package edu.hitsz.activity;

/*
 * 说明：本文件为“讲解注释版”。
 * 注释只解释页面职责、核心流程和关键方法，不改变任何业务逻辑。
 */

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import edu.hitsz.R;
import edu.hitsz.config.GameConfig;
import edu.hitsz.manager.MenuBgmManager;
import edu.hitsz.application.UserSession;
import edu.hitsz.util.FullScreenUtil;
import edu.hitsz.util.PressEffectUtil;

/**
 * 游戏模式选择页面。
 *
 * 作用：
 * 1. 在主菜单点击 START 后进入该页面；
 * 2. 单机模式跳转到 DifficultySelectActivity；
 * 3. 联机模式会先检查登录状态，未登录则提示登录；
 * 4. 已登录时把用户信息传给 OnlineRoomActivity，用于创建或加入房间。
 *
 * 该页面是单机流程和联机流程的分叉点。
 */
public class ModeSelectActivity extends AppCompatActivity {
    // soundEnabled 保存从主菜单传来的音效开关状态，继续传给后续单机或联机页面。
    // ivOffline / ivOnline 分别对应单机模式和联机模式入口。


    private boolean soundEnabled;

    private ImageView ivOffline;
    private ImageView ivOnline;

    /**
     * 初始化模式选择页，绑定单机和联机两个入口。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FullScreenUtil.hideSystemBars(this);

        setContentView(R.layout.activity_mode_select);

        soundEnabled = getIntent().getBooleanExtra(GameConfig.EXTRA_SOUND_ENABLED, true);

        ivOffline = findViewById(R.id.ivOffline);
        ivOnline = findViewById(R.id.ivOnline);

        PressEffectUtil.apply(ivOffline, ivOnline);

        ivOffline.setOnClickListener(v -> {
            Intent intent = new Intent(ModeSelectActivity.this, DifficultySelectActivity.class);
            intent.putExtra(GameConfig.EXTRA_SOUND_ENABLED, soundEnabled);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        ivOnline.setOnClickListener(v -> {
            if (!UserSession.isLoggedIn()) {
                Toast.makeText(
                        ModeSelectActivity.this,
                        "请先登录账号再进入联机模式",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Intent intent = new Intent(ModeSelectActivity.this, OnlineRoomActivity.class);
            intent.putExtra(GameConfig.EXTRA_SOUND_ENABLED, soundEnabled);
            intent.putExtra("userId", UserSession.getUserId());
            intent.putExtra("username", UserSession.getUsername());
            intent.putExtra("avatarId", UserSession.getAvatarId());

            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }

    /**
     * 页面恢复时隐藏系统栏并播放菜单音乐。
     */
    @Override
    protected void onResume() {
        super.onResume();

        FullScreenUtil.hideSystemBars(this);

        MenuBgmManager.play(this);
    }

    /**
     * 页面暂停时暂停菜单音乐。
     */
    @Override
    protected void onPause() {
        super.onPause();

        MenuBgmManager.pause();
    }

}