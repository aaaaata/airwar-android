package edu.hitsz.activity;

/*
 * 说明：本文件为“讲解注释版”。
 * 注释只解释页面职责、核心流程和关键方法，不改变任何业务逻辑。
 */

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import edu.hitsz.R;
import edu.hitsz.config.GameConfig;
import edu.hitsz.application.NormalGame;
import edu.hitsz.application.PkApiClient;
import edu.hitsz.application.PkWebSocketClient;
import edu.hitsz.application.UserSession;
import edu.hitsz.util.AvatarUtil;
import edu.hitsz.manager.BackgroundScrollManager;
import edu.hitsz.util.FullScreenUtil;
import edu.hitsz.widget.DigitTextView;
import edu.hitsz.widget.RoundCornerImageView;

/**
 * 联机对战游戏页面。
 *
 * 作用：
 * 1. 读取房间信息、双方用户信息，并初始化 PK 顶部信息栏；
 * 2. 使用 NormalGame 作为联机对战的本地游戏逻辑；
 * 3. 通过 WebSocket 定期发送自己的分数和生命值，并接收对手状态；
 * 4. 游戏结束后通过 HTTP 上传最终成绩；
 * 5. 如果双方结果都已生成，则进入 PkResultActivity，否则进入等待结果页面。
 *
 * 本类把“本地游戏运行”和“网络实时同步”结合起来，是联机功能的核心页面。
 */
public class PkGameActivity extends AppCompatActivity {
    // PK 页面同时维护“本地游戏状态”和“网络同步状态”。
    // currentScore/currentLife 是自己的实时状态；enemyScore/enemyLife 是 WebSocket 收到的对手状态。
    // pkApiClient 负责 HTTP 结果提交；pkWebSocketClient 负责实时状态同步。


    private View innerScreen;
    private FrameLayout pkBarRoot;
    private FrameLayout pkGameContainer;
    private android.widget.ImageView ivPkBarBg;

    private View myInfoRows;
    private View enemyInfoRows;

    private RoundCornerImageView ivMyAvatar;
    private RoundCornerImageView ivEnemyAvatar;

    private TextView tvMyName;
    private TextView tvEnemyName;
    private DigitTextView digitMyScore;
    private DigitTextView digitEnemyScore;
    private DigitTextView digitMyLife;
    private DigitTextView digitEnemyLife;

    private boolean soundEnabled;

    private long roomId;
    private String roomCode;

    private long myUserId;
    private String myUsername;
    private int myAvatarId;

    private long enemyUserId;
    private String enemyUsername;
    private int enemyAvatarId;

    private NormalGame gameView;

    private int currentScore = 0;
    private int currentLife = 1000;
    private int enemyScore = 0;
    private int enemyLife = -1;

    private boolean gameFinished = false;

    private PkApiClient pkApiClient;
    private PkWebSocketClient pkWebSocketClient;

    private int lastSentScore = -1;
    private int lastSentLife = -1;
    private long lastSendStateTime = 0L;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private final Runnable infoUpdateTask = new Runnable() {
        @Override
        public void run() {
            if (gameView != null && !gameFinished) {
                int score = gameView.getScore();
                int life = gameView.getLife();

                updateMyScore(score);
                updateMyLife(life);
                sendStateIfNeeded(score, life);

                uiHandler.postDelayed(this, 100);
            }
        }
    };

    private final Handler gameHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == GameConfig.MSG_GAME_OVER) {
                Bundle data = msg.getData();
                int finalScore = data.getInt(GameConfig.EXTRA_SCORE, currentScore);
                handlePkGameOver(finalScore);
            }
        }
    };

    /**
     * 初始化 PK 游戏页：读取房间信息、布局适配、连接 WebSocket 并启动本地游戏。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FullScreenUtil.hideSystemBars(this);

        BackgroundScrollManager.getInstance().ensureInitialized();
        setContentView(R.layout.activity_pk_game);

        readIntent();
        bindViews();

        pkApiClient = new PkApiClient();

        adjustPkBarSize();
        bindPkUserInfo();
        initPkWebSocket();
        startNormalGame();
    }

    /**
     * 从 Intent 中读取房间号、双方用户信息和音效状态。
     */
    private void readIntent() {
        Intent intent = getIntent();

        soundEnabled = intent.getBooleanExtra(GameConfig.EXTRA_SOUND_ENABLED, true);

        roomId = intent.getLongExtra("roomId", -1);
        roomCode = intent.getStringExtra("roomCode");

        myUserId = intent.getLongExtra("selfUserId", UserSession.getUserId());
        myUsername = intent.getStringExtra("selfUsername");
        myAvatarId = intent.getIntExtra("selfAvatarId", UserSession.getAvatarId());

        enemyUserId = intent.getLongExtra("opponentUserId", -1);
        enemyUsername = intent.getStringExtra("opponentUsername");
        enemyAvatarId = intent.getIntExtra("opponentAvatarId", 1);

        if (myUsername == null || myUsername.trim().isEmpty()) {
            myUsername = UserSession.getUsername();
        }
        if (myUsername == null || myUsername.trim().isEmpty()) {
            myUsername = "USER";
        }
        if (enemyUsername == null || enemyUsername.trim().isEmpty()) {
            enemyUsername = "OPPONENT";
        }
    }

    /**
     * 绑定 PK 顶部栏和游戏容器相关控件。
     */
    private void bindViews() {
        innerScreen = findViewById(R.id.innerScreen);

        pkBarRoot = findViewById(R.id.pkBarRoot);
        pkGameContainer = findViewById(R.id.pkGameContainer);
        ivPkBarBg = findViewById(R.id.ivPkBarBg);

        myInfoRows = findViewById(R.id.myInfoRows);
        enemyInfoRows = findViewById(R.id.enemyInfoRows);

        ivMyAvatar = findViewById(R.id.ivMyAvatar);
        ivEnemyAvatar = findViewById(R.id.ivEnemyAvatar);

        tvMyName = findViewById(R.id.tvMyName);
        tvEnemyName = findViewById(R.id.tvEnemyName);

        digitMyScore = findViewById(R.id.digitMyScore);
        digitEnemyScore = findViewById(R.id.digitEnemyScore);
        digitMyLife = findViewById(R.id.digitMyLife);
        digitEnemyLife = findViewById(R.id.digitEnemyLife);
    }

    /**
     * 根据顶部 PK 栏素材比例动态计算高度，并适配头像和数字控件。
     */
    private void adjustPkBarSize() {
        innerScreen.post(() -> {
            Drawable drawable = ivPkBarBg.getDrawable();
            if (drawable == null) {
                return;
            }

            int innerWidth = innerScreen.getWidth();
            if (innerWidth <= 0) {
                return;
            }

            int intrinsicWidth = drawable.getIntrinsicWidth();
            int intrinsicHeight = drawable.getIntrinsicHeight();
            if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
                return;
            }

            int targetHeight = Math.round(innerWidth * (intrinsicHeight / (float) intrinsicWidth));
            ViewGroup.LayoutParams lp = pkBarRoot.getLayoutParams();
            lp.height = targetHeight;
            pkBarRoot.setLayoutParams(lp);

            int avatarSize = Math.max(1, Math.round(targetHeight * 0.46f));
            setSize(ivMyAvatar, avatarSize, avatarSize);
            setSize(ivEnemyAvatar, avatarSize, avatarSize);
            ivMyAvatar.setCornerRadius(Math.round(avatarSize * 0.14f));
            ivEnemyAvatar.setCornerRadius(Math.round(avatarSize * 0.14f));

            setHeight(myInfoRows, avatarSize);
            setHeight(enemyInfoRows, avatarSize);

            int rowHeight = Math.max(1, avatarSize / 3);

            float nameTextSize = rowHeight * 0.58f;
            tvMyName.setTextSize(TypedValue.COMPLEX_UNIT_PX, nameTextSize);
            tvEnemyName.setTextSize(TypedValue.COMPLEX_UNIT_PX, nameTextSize);

            int digitHeight = Math.round(rowHeight * 0.64f);
            setHeight(digitMyScore, digitHeight);
            setHeight(digitEnemyScore, digitHeight);
            setHeight(digitMyLife, digitHeight);
            setHeight(digitEnemyLife, digitHeight);

            digitMyScore.setCharSpacingPx(dp(1));
            digitEnemyScore.setCharSpacingPx(dp(1));
            digitMyLife.setCharSpacingPx(dp(1));
            digitEnemyLife.setCharSpacingPx(dp(1));
        });
    }

    /**
     * 把双方头像、用户名、初始分数和生命值显示到 PK 信息栏。
     */
    private void bindPkUserInfo() {
        ivMyAvatar.setImageResource(AvatarUtil.getAvatarResId(this, myAvatarId));
        ivEnemyAvatar.setImageResource(AvatarUtil.getAvatarResId(this, enemyAvatarId));

        tvMyName.setText(myUsername);
        tvEnemyName.setText(enemyUsername);

        updateMyScore(0);
        updateEnemyScore(0);
        updateMyLife(1000);
        updateEnemyLife(-1);
    }

    /**
     * 建立 WebSocket 连接，用于收发双方实时分数和生命值。
     */
    private void initPkWebSocket() {
        if (roomId <= 0 || myUserId <= 0) {
            return;
        }

        pkWebSocketClient = new PkWebSocketClient(roomId, myUserId, new PkWebSocketClient.Listener() {
            @Override
            public void onOpponentState(int score, int life) {
                runOnUiThread(() -> {
                    updateEnemyScore(score);
                    if (life >= 0) {
                        updateEnemyLife(life);
                    }
                });
            }

            @Override
            public void onOpponentFinished(long opponentUserId) {
            }

            @Override
            public void onOpponentLeft() {
                runOnUiThread(() ->
                        Toast.makeText(PkGameActivity.this, "对方已离开", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() ->
                        Toast.makeText(PkGameActivity.this, "联机连接异常：" + message, Toast.LENGTH_SHORT).show()
                );
            }
        });

        pkWebSocketClient.connect();
    }

    /**
     * 创建联机模式下使用的 NormalGame，并把它加入 PK 游戏容器。
     */
    private void startNormalGame() {
        pkGameContainer.removeAllViews();

        gameView = new NormalGame(this, soundEnabled);
        gameView.setCoinEnabled(false);
        gameView.setMainHandler(gameHandler);

        pkGameContainer.addView(gameView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        gameView.action();
        uiHandler.post(infoUpdateTask);
    }

    /**
     * 更新自己的当前分数和界面显示。
     */
    private void updateMyScore(int score) {
        currentScore = Math.max(score, 0);
        if (digitMyScore != null) {
            digitMyScore.setNumber(currentScore);
        }
    }

    /**
     * 更新对手分数和界面显示。
     */
    private void updateEnemyScore(int score) {
        enemyScore = Math.max(score, 0);
        if (digitEnemyScore != null) {
            digitEnemyScore.setNumber(enemyScore);
        }
    }

    /**
     * 更新自己的生命值和界面显示。
     */
    private void updateMyLife(int life) {
        currentLife = Math.max(life, 0);
        if (digitMyLife != null) {
            digitMyLife.setNumber(currentLife);
        }
    }

    /**
     * 更新对手生命值；未知时用 --- 占位。
     */
    private void updateEnemyLife(int life) {
        enemyLife = life;
        if (digitEnemyLife == null) {
            return;
        }

        if (life < 0) {
            digitEnemyLife.setTextValue("---");
        } else {
            digitEnemyLife.setNumber(life);
        }
    }

    /**
     * 在状态变化且达到发送间隔时，通过 WebSocket 发送自己的状态。
     */
    private void sendStateIfNeeded(int score, int life) {
        if (pkWebSocketClient == null) {
            return;
        }

        long now = System.currentTimeMillis();
        boolean stateChanged = score != lastSentScore || life != lastSentLife;
        boolean intervalReached = now - lastSendStateTime >= 200;

        if (stateChanged && intervalReached) {
            lastSentScore = score;
            lastSentLife = life;
            lastSendStateTime = now;
            pkWebSocketClient.sendState(score, life);
        }
    }

    /**
     * 处理自己游戏结束：停止刷新、发送最终状态并进入结果提交流程。
     */
    private void handlePkGameOver(int finalScore) {
        if (gameFinished) {
            return;
        }

        gameFinished = true;
        uiHandler.removeCallbacks(infoUpdateTask);

        updateMyScore(finalScore);

        if (pkWebSocketClient != null) {
            pkWebSocketClient.sendState(finalScore, currentLife);
            pkWebSocketClient.sendFinished();
        }

        uploadPkFinishAndEnterResultFlow(finalScore);
    }

    /**
     * 向后端提交本方最终成绩，并根据后端返回决定进入结果页还是等待页。
     */
    private void uploadPkFinishAndEnterResultFlow(int finalScore) {
        if (roomId <= 0 || myUserId <= 0) {
            enterLocalFallbackResult();
            return;
        }

        pkApiClient.finish(roomId, myUserId, finalScore, new PkApiClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                String resultStatus = json.optString("resultStatus");
                if ("final_result".equals(resultStatus)) {
                    enterPkResultActivity(json);
                } else {
                    enterWaitingResultActivity();
                }
            }

            @Override
            public void onFail(String message) {
                Toast.makeText(
                        PkGameActivity.this,
                        "上传联机结果失败：" + message,
                        Toast.LENGTH_SHORT
                ).show();
                enterWaitingResultActivity();
            }
        });
    }

    /**
     * 自己先结束、对手未结束时进入等待结果页。
     */
    private void enterWaitingResultActivity() {
        Intent intent = new Intent(this, PkWaitingResultActivity.class);
        intent.putExtra("roomId", roomId);
        intent.putExtra("selfUserId", myUserId);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }

    /**
     * 后端已生成最终结果时，直接进入 PK 结果页。
     */
    private void enterPkResultActivity(JSONObject json) {
        Intent intent = new Intent(this, PkResultActivity.class);

        intent.putExtra("roomId", json.optLong("roomId", roomId));
        intent.putExtra("roomCode", json.optString("roomCode", roomCode));

        intent.putExtra("player1UserId", json.optLong("player1UserId", -1));
        intent.putExtra("player1Username", json.optString("player1Username", ""));
        intent.putExtra("player1AvatarId", json.optInt("player1AvatarId", 1));
        intent.putExtra("player1Score", json.optInt("player1Score", 0));

        intent.putExtra("player2UserId", json.optLong("player2UserId", -1));
        intent.putExtra("player2Username", json.optString("player2Username", ""));
        intent.putExtra("player2AvatarId", json.optInt("player2AvatarId", 1));
        intent.putExtra("player2Score", json.optInt("player2Score", 0));

        intent.putExtra("winnerUserId", json.optLong("winnerUserId", -1));

        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }

    /**
     * 房间或用户信息异常时，用本地已有分数生成兜底结果。
     */
    private void enterLocalFallbackResult() {
        Intent intent = new Intent(this, PkResultActivity.class);

        intent.putExtra("player1UserId", myUserId);
        intent.putExtra("player1Username", myUsername);
        intent.putExtra("player1AvatarId", myAvatarId);
        intent.putExtra("player1Score", currentScore);

        intent.putExtra("player2UserId", enemyUserId);
        intent.putExtra("player2Username", enemyUsername);
        intent.putExtra("player2AvatarId", enemyAvatarId);
        intent.putExtra("player2Score", enemyScore);

        long winnerUserId;
        if (currentScore > enemyScore) {
            winnerUserId = myUserId;
        } else if (enemyScore > currentScore) {
            winnerUserId = enemyUserId;
        } else {
            winnerUserId = -1;
        }

        intent.putExtra("winnerUserId", winnerUserId);

        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        FullScreenUtil.hideSystemBars(this);

        BackgroundScrollManager.getInstance().setGameSpeed();
        BackgroundScrollManager.getInstance().rebaseClock(System.nanoTime());
        if (gameView != null) {
            gameView.onGameResume();
        }
    }

    @Override
    protected void onPause() {
        if (gameView != null) {
            gameView.onGamePause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        uiHandler.removeCallbacks(infoUpdateTask);
        if (pkWebSocketClient != null) {
            pkWebSocketClient.close();
        }
        super.onDestroy();
    }

    private void setSize(View view, int width, int height) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.width = width;
        lp.height = height;
        view.setLayoutParams(lp);
    }

    private void setHeight(View view, int height) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.height = Math.max(1, height);
        view.setLayoutParams(lp);
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        ));
    }
}