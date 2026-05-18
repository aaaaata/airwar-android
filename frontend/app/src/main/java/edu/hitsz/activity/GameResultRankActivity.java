package edu.hitsz.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.hitsz.R;
import edu.hitsz.config.ApiConfig;
import edu.hitsz.config.GameConfig;
import edu.hitsz.manager.MenuBgmManager;
import edu.hitsz.application.RankRecord;
import edu.hitsz.manager.SessionManager;
import edu.hitsz.manager.ShopStateManager;
import edu.hitsz.util.AvatarUtil;
import edu.hitsz.manager.BackgroundScrollManager;
import edu.hitsz.util.FullScreenUtil;
import edu.hitsz.util.GrayPressEffectUtil;
import edu.hitsz.widget.DigitTextView;
import edu.hitsz.widget.RoundCornerImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 单局游戏结束后的结算排行榜页面。
 *
 * 作用：
 * 1. 接收本局难度、分数和金币；
 * 2. 未登录时先把金币保存到本地；
 * 3. 将成绩上传到后端，并拉取当前难度下的排行榜结果；
 * 4. 渲染 Top 10 以及当前玩家本局成绩行；
 * 5. 支持返回主菜单并重置菜单背景状态。
 *
 * 这个页面把“游戏结果、后端上传、排行榜展示、商店金币”几个模块连接起来。
 */
public class GameResultRankActivity extends AppCompatActivity {
    // 页面核心状态：difficulty 表示本局难度，score 表示本局得分，coinsGot 表示本局获得金币。
    // OkHttpClient 用于访问云端后端；Handler 用于把网络线程结果切回主线程刷新 UI。


    private ImageView ivModeHeader;
    private ImageView ivBackToMenu;
    private LinearLayout recordContainer;

    private String difficulty;
    private int score;
    private int coinsGot;

    private final OkHttpClient client = new OkHttpClient();
    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * 初始化结算排行榜页面，读取本局成绩并启动上传和排行榜加载流程。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FullScreenUtil.hideSystemBars(this);

        BackgroundScrollManager.getInstance().ensureInitialized();
        setContentView(R.layout.activity_game_result_rank);

        ivModeHeader = findViewById(R.id.ivModeHeader);
        ivBackToMenu = findViewById(R.id.ivBackToMenu);
        recordContainer = findViewById(R.id.recordContainer);

        difficulty = getIntent().getStringExtra(GameConfig.EXTRA_DIFFICULTY);
        score = getIntent().getIntExtra(GameConfig.EXTRA_SCORE, 0);

        coinsGot = getIntent().getIntExtra(GameConfig.EXTRA_COINS_GOT, 0);

        if (savedInstanceState == null && !SessionManager.isLoggedIn(this)) {
            saveCoinsGotLocally();
        }

        if (difficulty == null) {
            difficulty = GameConfig.NORMAL;
        }

        updateHeaderImage();
        renderRecords(buildLoadingRecords());

        uploadScoreAndLoadRanking();

        GrayPressEffectUtil.apply(ivBackToMenu);

        ivBackToMenu.setOnClickListener(v -> backToMenu());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                backToMenu();
            }
        });
    }

    /**
     * 根据本局难度切换排行榜顶部标题图片。
     */
    private void updateHeaderImage() {
        switch (difficulty) {
            case GameConfig.EASY:
                ivModeHeader.setImageResource(R.drawable.img_rank_header_easy);
                break;
            case GameConfig.HARD:
                ivModeHeader.setImageResource(R.drawable.img_rank_header_hard);
                break;
            case GameConfig.NORMAL:
            default:
                ivModeHeader.setImageResource(R.drawable.img_rank_header_normal);
                break;
        }
    }

    /**
     * 未登录玩家没有云端账号，因此本局获得的金币先保存到本地。
     */
    private void saveCoinsGotLocally() {
        if (coinsGot <= 0) {
            return;
        }

        ShopStateManager.addCoins(this, coinsGot);
    }
    /**
     * 把本局成绩上传给后端，并请求后端返回结算页要展示的排行榜数据。
     */
    private void uploadScoreAndLoadRanking() {
        int userId = SessionManager.getUserId(this);
        String username = SessionManager.getUsername(this);
        int avatarId = SessionManager.getAvatarId(this);

        if (!SessionManager.isLoggedIn(this)) {
            userId = -1;
            username = "GUEST";
            avatarId = 1;
        }

        HttpUrl baseUrl = HttpUrl.parse(ApiConfig.BASE_URL + "/score/uploadResult");
        if (baseUrl == null) {
            Toast.makeText(this, "服务器地址错误", Toast.LENGTH_SHORT).show();
            renderRecords(buildFallbackRecords());
            return;
        }

        HttpUrl url = baseUrl.newBuilder()
                .addQueryParameter("userId", String.valueOf(userId))
                .addQueryParameter("username", username)
                .addQueryParameter("avatarId", String.valueOf(avatarId))
                .addQueryParameter("difficulty", difficulty)
                .addQueryParameter("score", String.valueOf(score))
                .addQueryParameter("coinsGot", String.valueOf(coinsGot))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@androidx.annotation.NonNull Call call,
                                  @androidx.annotation.NonNull IOException e) {
                handler.post(() -> {
                    Toast.makeText(GameResultRankActivity.this,
                            "成绩上传失败，显示本地临时结果",
                            Toast.LENGTH_SHORT).show();
                    renderRecords(buildFallbackRecords());
                });
            }

            @Override
            public void onResponse(@androidx.annotation.NonNull Call call,
                                   @androidx.annotation.NonNull Response response) throws IOException {
                String body = response.body() == null
                        ? ""
                        : response.body().string().trim();

                try {
                    List<RankRecord> records = parseResultRanking(body);
                    handler.post(() -> renderRecords(records));
                } catch (Exception e) {
                    handler.post(() -> {
                        Toast.makeText(GameResultRankActivity.this,
                                "排行榜数据格式错误",
                                Toast.LENGTH_SHORT).show();
                        renderRecords(buildFallbackRecords());
                    });
                }
            }
        });
    }

    /**
     * 解析后端返回的 JSON 数组，把每条记录转换为界面层使用的 RankRecord。
     */
    private List<RankRecord> parseResultRanking(String body) throws Exception {
        JSONArray array = new JSONArray(body);
        List<RankRecord> records = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);

            String rankLabel = obj.optString("rankLabel", String.valueOf(i + 1));
            String username = obj.optString("username", "---");
            int score = obj.optInt("score", 0);
            int avatarId = obj.optInt("avatarId", obj.optInt("avatar_id", 1));
            boolean current = obj.optBoolean("current", false);

            records.add(new RankRecord(
                    rankLabel,
                    username,
                    score,
                    AvatarUtil.getAvatarResId(this, avatarId),
                    current
            ));
        }

        if (records.isEmpty()) {
            return buildFallbackRecords();
        }

        return records;
    }

    /**
     * 构造加载中的占位数据，避免网络请求完成前界面为空。
     */
    private List<RankRecord> buildLoadingRecords() {
        List<RankRecord> list = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            list.add(new RankRecord(
                    String.valueOf(i),
                    "LOADING",
                    0,
                    R.drawable.img_avatar_placeholder,
                    false
            ));
        }

        list.add(new RankRecord(
                "-",
                "YOU",
                score,
                R.drawable.img_avatar_placeholder,
                true
        ));

        return list;
    }

    /**
     * 网络失败或数据异常时使用的兜底排行榜数据。
     */
    private List<RankRecord> buildFallbackRecords() {
        List<RankRecord> list = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            list.add(new RankRecord(
                    String.valueOf(i),
                    "---",
                    0,
                    R.drawable.img_avatar_placeholder,
                    false
            ));
        }

        list.add(new RankRecord(
                "-",
                SessionManager.getUsername(this),
                score,
                AvatarUtil.getAvatarResId(this, SessionManager.getAvatarId(this)),
                true
        ));

        return list;
    }

    /**
     * 把排行榜数据逐条转换成 item_rank_record 布局并添加到容器中。
     */
    private void renderRecords(List<RankRecord> records) {
        recordContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (RankRecord item : records) {
            View row = inflater.inflate(R.layout.item_rank_record, recordContainer, false);

            DigitTextView digitRank = row.findViewById(R.id.digitRank);
            TextView tvName = row.findViewById(R.id.tvName);
            DigitTextView digitScore = row.findViewById(R.id.digitScore);
            RoundCornerImageView ivAvatar = row.findViewById(R.id.ivAvatar);
            ImageView ivContentBg = row.findViewById(R.id.ivContentBg);
            ImageView ivRankBg = row.findViewById(R.id.ivRankBg);

            digitRank.setTextValue(item.getRankLabel());
            tvName.setText(item.getName());

            if (item.getScore() <= 0) {
                digitScore.setTextValue("---");
            } else {
                digitScore.setTextValue(String.valueOf(item.getScore()));
            }

            ivAvatar.setImageResource(item.getAvatarResId());
            ivAvatar.setCornerRadius(dp(4));

            if (item.isCurrentPlayerRow()) {
                ivContentBg.setImageDrawable(makeSolidDrawable("#6C8C9F"));
                ivRankBg.setImageDrawable(makeSolidDrawable("#6C8C9F"));
            } else {
                ivContentBg.setImageResource(R.drawable.img_rank_content_bg);
                ivRankBg.setImageResource(R.drawable.img_rank_index_bg);
            }

            recordContainer.addView(row);
        }
    }

    /**
     * 生成当前玩家成绩行使用的纯色圆角背景。
     */
    private GradientDrawable makeSolidDrawable(String color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(color));
        drawable.setCornerRadius(dp(3));
        return drawable;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /**
     * 返回主菜单，同时重置菜单背景滚动状态。
     */
    private void backToMenu() {
        BackgroundScrollManager.getInstance().resetToFirstMenu();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        FullScreenUtil.hideSystemBars(this);

        MenuBgmManager.play(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        MenuBgmManager.pause();
    }

}