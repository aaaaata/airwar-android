package edu.hitsz.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import edu.hitsz.R;
import edu.hitsz.application.EasyGame;
import edu.hitsz.application.Game;
import edu.hitsz.config.GameConfig;
import edu.hitsz.application.HardGame;
import edu.hitsz.application.NormalGame;
import edu.hitsz.manager.BackgroundScrollManager;
import edu.hitsz.util.FullScreenUtil;
import edu.hitsz.widget.DigitTextView;

/**
 * 单人游戏页面。
 *
 * 作用：
 * 1. 根据上一页传入的难度创建 EasyGame / NormalGame / HardGame；
 * 2. 把 Game 作为自定义 View 加入游戏容器并启动游戏主循环；
 * 3. 定时刷新分数、生命值、金币等 HUD 信息；
 * 4. 接收 Game 发送的游戏结束消息，并跳转到结算排行榜页面。
 *
 * 这个类是 Android Activity 生命周期和原 Java 游戏主循环之间的桥梁。
 */
public class GameActivity extends AppCompatActivity {
    // difficulty：当前游戏难度；soundEnabled：音效开关；gameView：真正承载游戏逻辑的自定义 View。
    // digitScoreValue / digitLifeValue / digitCoinsGotValue：HUD 数字显示控件。
    // ivModeValue：HUD 中用于展示当前难度的图片。


    private String difficulty;
    private boolean soundEnabled;
    private Game gameView;

    private DigitTextView digitScoreValue;
    private DigitTextView digitLifeValue;
    private DigitTextView digitCoinsGotValue;
    private ImageView ivModeValue;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    /**
     * 定时刷新 HUD 数值。这里每 100ms 从 Game 对象读取分数、生命值和金币，并同步到界面。
     */
    private final Runnable infoUpdateTask = new Runnable() {
        @Override
        public void run() {
            if (gameView != null) {
                if (digitScoreValue != null) {
                    digitScoreValue.setNumber(gameView.getScore());
                }
                if (digitLifeValue != null) {
                    digitLifeValue.setNumber(gameView.getLife());
                }
                if (digitCoinsGotValue != null) {
                    digitCoinsGotValue.setNumber(gameView.getCoinsGot());
                }
                uiHandler.postDelayed(this, 100);
            }
        }
    };

    /**
     * 接收 Game 内部发来的消息。游戏结束时，Game 会通过 Handler 通知 Activity 跳转到结算页。
     */
    private final Handler gameHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            if (msg.what == GameConfig.MSG_GAME_OVER) {
                Bundle data = msg.getData();
                String mode = data.getString(GameConfig.EXTRA_DIFFICULTY);
                int score = data.getInt(GameConfig.EXTRA_SCORE);
                int coinsGot = data.getInt(GameConfig.EXTRA_COINS_GOT, 0);

                Intent intent = new Intent(GameActivity.this, GameResultRankActivity.class);
                intent.putExtra(GameConfig.EXTRA_DIFFICULTY, mode);
                intent.putExtra(GameConfig.EXTRA_SCORE, score);
                intent.putExtra(GameConfig.EXTRA_COINS_GOT, coinsGot);
                intent.putExtra(GameConfig.EXTRA_FROM_GAME_OVER, true);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }
        }
    };

    /**
     * 创建游戏页面：读取难度参数、创建对应 Game 对象、加入容器并启动游戏主循环。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FullScreenUtil.hideSystemBars(this);

        BackgroundScrollManager.getInstance().ensureInitialized();
        setContentView(R.layout.activity_game);

        digitScoreValue = findViewById(R.id.digitScoreValue);
        digitLifeValue = findViewById(R.id.digitLifeValue);
        digitCoinsGotValue = findViewById(R.id.digitCoinsGotValue);
        ivModeValue = findViewById(R.id.ivModeValue);

        difficulty = getIntent().getStringExtra(GameConfig.EXTRA_DIFFICULTY);
        soundEnabled = getIntent().getBooleanExtra(GameConfig.EXTRA_SOUND_ENABLED, true);

        if (difficulty == null) {
            difficulty = GameConfig.NORMAL;
        }

        setModeImage(difficulty);

        FrameLayout container = findViewById(R.id.gameContainer);

        switch (difficulty) {
            case GameConfig.EASY:
                gameView = new EasyGame(this, soundEnabled);
                break;
            case GameConfig.NORMAL:
                gameView = new NormalGame(this, soundEnabled);
                break;
            case GameConfig.HARD:
                gameView = new HardGame(this, soundEnabled);
                break;
            default:
                gameView = new NormalGame(this, soundEnabled);
                break;
        }

        gameView.setMainHandler(gameHandler);

        container.removeAllViews();
        container.addView(gameView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        gameView.action();

        if (digitScoreValue != null) {
            digitScoreValue.setDigitTintColor(Color.WHITE);
            digitScoreValue.setNumber(0);
        }
        if (digitLifeValue != null) {
            digitLifeValue.setDigitTintColor(Color.WHITE);
            digitLifeValue.setNumber(1000);
        }
        if (digitCoinsGotValue != null) {
            digitCoinsGotValue.setDigitTintColor(Color.WHITE);
            digitCoinsGotValue.setNumber(0);
        }

        uiHandler.post(infoUpdateTask);
    }

    /**
     * 根据当前难度设置 HUD 上的模式图片。
     */
    private void setModeImage(String difficulty) {
        if (ivModeValue == null) {
            return;
        }

        switch (difficulty) {
            case GameConfig.EASY:
                ivModeValue.setImageResource(R.drawable.img_mode_easy);
                break;
            case GameConfig.HARD:
                ivModeValue.setImageResource(R.drawable.img_mode_hard);
                break;
            case GameConfig.NORMAL:
            default:
                ivModeValue.setImageResource(R.drawable.img_mode_normal);
                break;
        }
    }

    /**
     * Activity 销毁时停止 HUD 刷新并释放 Game 资源，防止线程或回调泄漏。
     */
    @Override
    protected void onDestroy() {
        uiHandler.removeCallbacks(infoUpdateTask);

        if (gameView != null) {
            gameView.onGameDestroy();
            gameView = null;
        }

        super.onDestroy();
    }

    /**
     * 页面暂停时通知 Game 暂停，避免退到后台后游戏继续运行。
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.onGamePause();
        }
    }

    /**
     * 页面恢复时重新隐藏系统栏，并通知 Game 继续运行。
     */
    @Override
    protected void onResume() {
        super.onResume();

        FullScreenUtil.hideSystemBars(this);

        if (gameView != null) {
            gameView.onGameResume();
        }
    }
}