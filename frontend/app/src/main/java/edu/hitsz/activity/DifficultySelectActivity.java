package edu.hitsz.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import edu.hitsz.R;
import edu.hitsz.config.GameConfig;
import edu.hitsz.manager.MenuBgmManager;
import edu.hitsz.manager.SoundSettingManager;
import edu.hitsz.manager.BackgroundScrollManager;
import edu.hitsz.util.FullScreenUtil;
import edu.hitsz.util.PressEffectUtil;

/**
 * 难度选择页面。
 *
 * 作用：
 * 1. 展示 Easy / Normal / Hard 三个单机难度入口；
 * 2. 根据玩家选择把难度参数通过 Intent 传给 GameActivity；
 * 3. 在进入战斗前切换背景滚动配置，并停止菜单 BGM，避免与战斗 BGM 重叠。
 *
 * 这个 Activity 是“模式选择之后、真正开始游戏之前”的参数分发页。
 */
public class DifficultySelectActivity extends AppCompatActivity {
    // 当前音效开关状态。进入游戏时会继续传递给 GameActivity。
    // 三个难度按钮分别对应 Easy、Normal、Hard。


    private boolean soundEnabled;

    private ImageView ivEasy;
    private ImageView ivNormal;
    private ImageView ivHard;

    /**
     * Activity 创建时初始化全屏、布局、音效状态和三个难度按钮。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FullScreenUtil.hideSystemBars(this);

        setContentView(R.layout.activity_difficulty_select);

        soundEnabled = getIntent().getBooleanExtra(GameConfig.EXTRA_SOUND_ENABLED, true);

        ivEasy = findViewById(R.id.ivEasy);
        ivNormal = findViewById(R.id.ivNormal);
        ivHard = findViewById(R.id.ivHard);

        PressEffectUtil.apply(ivEasy, ivNormal, ivHard);

        ivEasy.setOnClickListener(v -> startGame(GameConfig.EASY));
        ivNormal.setOnClickListener(v -> startGame(GameConfig.NORMAL));
        ivHard.setOnClickListener(v -> startGame(GameConfig.HARD));
    }

    /**
     * 根据玩家选择的难度准备背景配置、传递游戏参数，并启动 GameActivity。
     */
    private void startGame(String difficulty) {
        BackgroundScrollManager manager = BackgroundScrollManager.getInstance();

        if (GameConfig.NORMAL.equals(difficulty)) {
            manager.useNormalLoopKeepCurrent();
        } else if (GameConfig.HARD.equals(difficulty)) {
            manager.useHardLoopKeepCurrent();
        }
        // EASY 不改，继续默认 13 系列

        boolean currentSoundEnabled = SoundSettingManager.isSoundEnabled(this);

        Intent intent = new Intent(DifficultySelectActivity.this, GameActivity.class);
        intent.putExtra(GameConfig.EXTRA_DIFFICULTY, difficulty);
        intent.putExtra(GameConfig.EXTRA_SOUND_ENABLED, currentSoundEnabled);

// 进入战斗前停止菜单音乐，避免和战斗 BGM 重叠
        MenuBgmManager.stopAndRelease();

        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    /**
     * 页面重新可见时恢复沉浸式全屏，并播放菜单背景音乐。
     */
    @Override
    protected void onResume() {
        super.onResume();

        FullScreenUtil.hideSystemBars(this);

        MenuBgmManager.play(this);
    }

    /**
     * 页面进入后台时暂停菜单音乐，避免多个页面音乐叠加。
     */
    @Override
    protected void onPause() {
        super.onPause();

        MenuBgmManager.pause();
    }
}