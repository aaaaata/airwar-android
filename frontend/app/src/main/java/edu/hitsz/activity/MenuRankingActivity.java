package edu.hitsz.activity;

/*
 * 说明：本文件为“讲解注释版”。
 * 注释只解释页面职责、核心流程和关键方法，不改变任何业务逻辑。
 */

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
 * 主菜单入口的排行榜页面。
 *
 * 作用：
 * 1. 提供 Easy / Normal / Hard 三个难度排行榜切换；
 * 2. 根据当前难度向后端请求排行榜数据；
 * 3. 将后端 JSON 数据转换为 RankRecord 并渲染到列表中；
 * 4. 网络异常或数据为空时显示占位记录，避免界面空白。
 *
 * 这个页面是“查看排行榜”，不负责上传成绩。
 */
public class MenuRankingActivity extends AppCompatActivity {
    // currentDifficulty 记录当前选中的排行榜难度。
    // recordContainer 是排行榜记录容器，后端返回的数据会被逐条 inflate 成记录行。


    private ImageView ivTabEasy;
    private ImageView ivTabNormal;
    private ImageView ivTabHard;
    private ImageView ivBackToMenu;
    private LinearLayout recordContainer;

    private String currentDifficulty = GameConfig.EASY;

    private final OkHttpClient client = new OkHttpClient();
    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * 初始化排行榜页面、难度标签按钮、返回按钮，并默认加载 Easy 榜单。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FullScreenUtil.hideSystemBars(this);

        BackgroundScrollManager.getInstance().ensureInitialized();
        setContentView(R.layout.activity_menu_ranking);

        ivTabEasy = findViewById(R.id.ivTabEasy);
        ivTabNormal = findViewById(R.id.ivTabNormal);
        ivTabHard = findViewById(R.id.ivTabHard);
        ivBackToMenu = findViewById(R.id.ivBackToMenu);
        recordContainer = findViewById(R.id.recordContainer);

        GrayPressEffectUtil.apply(ivBackToMenu);

        ivTabEasy.setOnClickListener(v -> switchDifficulty(GameConfig.EASY));
        ivTabNormal.setOnClickListener(v -> switchDifficulty(GameConfig.NORMAL));
        ivTabHard.setOnClickListener(v -> switchDifficulty(GameConfig.HARD));
        ivBackToMenu.setOnClickListener(v -> backToMenu());

        switchDifficulty(GameConfig.EASY);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                backToMenu();
            }
        });
    }

    /**
     * 切换当前难度，并重新加载对应难度排行榜。
     */
    private void switchDifficulty(String difficulty) {
        currentDifficulty = difficulty;
        updateTabImages();
        loadRankingFromServer(difficulty);
    }

    /**
     * 根据当前选中难度更新三个标签按钮的选中 / 未选中图片。
     */
    private void updateTabImages() {
        ivTabEasy.setImageResource(GameConfig.EASY.equals(currentDifficulty)
                ? R.drawable.img_rank_tab_easy_selected
                : R.drawable.img_rank_tab_easy_normal);

        ivTabNormal.setImageResource(GameConfig.NORMAL.equals(currentDifficulty)
                ? R.drawable.img_rank_tab_normal_selected
                : R.drawable.img_rank_tab_normal_normal);

        ivTabHard.setImageResource(GameConfig.HARD.equals(currentDifficulty)
                ? R.drawable.img_rank_tab_hard_selected
                : R.drawable.img_rank_tab_hard_normal);
    }

    /**
     * 向后端 /ranking/menu 接口请求指定难度的排行榜数据。
     */
    private void loadRankingFromServer(String difficulty) {
        HttpUrl baseUrl = HttpUrl.parse(ApiConfig.BASE_URL + "/ranking/menu");
        if (baseUrl == null) {
            renderRecords(buildEmptyRecords(12));
            return;
        }

        HttpUrl url = baseUrl.newBuilder()
                .addQueryParameter("difficulty", difficulty)
                .addQueryParameter("limit", "12")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(() -> {
                    Toast.makeText(MenuRankingActivity.this,
                            "排行榜加载失败",
                            Toast.LENGTH_SHORT).show();
                    renderRecords(buildEmptyRecords(12));
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() == null
                        ? ""
                        : response.body().string().trim();

                try {
                    List<RankRecord> records = parseMenuRanking(body);
                    handler.post(() -> renderRecords(records));
                } catch (Exception e) {
                    handler.post(() -> {
                        Toast.makeText(MenuRankingActivity.this,
                                "排行榜数据格式错误",
                                Toast.LENGTH_SHORT).show();
                        renderRecords(buildEmptyRecords(12));
                    });
                }
            }
        });
    }

    /**
     * 解析菜单排行榜 JSON，最多保留 12 条，不足时用占位数据补齐。
     */
    private List<RankRecord> parseMenuRanking(String body) throws Exception {
        JSONArray array = new JSONArray(body);
        List<RankRecord> records = new ArrayList<>();

        for (int i = 0; i < array.length() && i < 12; i++) {
            JSONObject obj = array.getJSONObject(i);

            String rankLabel = obj.optString("rankLabel", String.valueOf(i + 1));
            String username = obj.optString("username", "---");
            int score = obj.optInt("score", 0);
            int avatarId = obj.optInt("avatarId", obj.optInt("avatar_id", 1));

            records.add(new RankRecord(
                    rankLabel,
                    username,
                    score,
                    AvatarUtil.getAvatarResId(avatarId),
                    false
            ));
        }

        while (records.size() < 12) {
            int rank = records.size() + 1;
            records.add(new RankRecord(
                    String.valueOf(rank),
                    "---",
                    0,
                    R.drawable.img_avatar_placeholder,
                    false
            ));
        }

        return records;
    }

    /**
     * 构造空榜单占位数据，用于网络失败或暂无数据的情况。
     */
    private List<RankRecord> buildEmptyRecords(int count) {
        List<RankRecord> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(new RankRecord(
                    String.valueOf(i),
                    "---",
                    0,
                    R.drawable.img_avatar_placeholder,
                    false
            ));
        }
        return list;
    }

    /**
     * 把 RankRecord 列表渲染为排行榜记录行。
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

            ivContentBg.setImageResource(R.drawable.img_rank_content_bg);
            ivRankBg.setImageResource(R.drawable.img_rank_index_bg);

            recordContainer.addView(row);
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /**
     * 关闭当前排行榜页，回到主菜单且取消页面切换动画。
     */
    private void backToMenu() {
        finish();
        overridePendingTransition(0, 0);
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