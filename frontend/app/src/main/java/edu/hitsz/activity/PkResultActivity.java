package edu.hitsz.activity;

/*
 * 说明：本文件为“讲解注释版”。
 * 注释只解释页面职责、核心流程和关键方法，不改变任何业务逻辑。
 */

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import edu.hitsz.R;
import edu.hitsz.manager.MenuBgmManager;
import edu.hitsz.util.AvatarUtil;
import edu.hitsz.manager.BackgroundScrollManager;
import edu.hitsz.util.FullScreenUtil;
import edu.hitsz.util.PressEffectUtil;

/**
 * 联机对战结果页面。
 *
 * 作用：
 * 1. 接收双方最终分数、头像和用户名；
 * 2. 根据最终分数判断胜者；
 * 3. 展示胜者头像和名称，平局时显示 DRAW；
 * 4. 支持返回主菜单。
 *
 * 给老师讲解时可以强调：结果页不再运行游戏逻辑，只负责最终结果展示。
 */
public class PkResultActivity extends AppCompatActivity {
    // 结果页只负责展示胜者，不再处理游戏运行和网络同步。


    private ImageView ivWinnerAvatar;
    private ImageView imgWin;
    private TextView tvWinnerName;
    private ImageButton btnBackToMenu;

    /**
     * 初始化联机结果页，绑定控件、显示胜者并设置返回按钮。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FullScreenUtil.hideSystemBars(this);

        BackgroundScrollManager.getInstance().ensureInitialized();
        setContentView(R.layout.activity_pk_result);

        bindViews();
        initWinnerInfo();
        initClickEvents();
    }

    /**
     * 绑定结果页需要使用的头像、胜利图片、名称和返回按钮。
     */
    private void bindViews() {
        ivWinnerAvatar = findViewById(R.id.ivWinnerAvatar);
        imgWin = findViewById(R.id.imgWin);
        tvWinnerName = findViewById(R.id.tvWinnerName);
        btnBackToMenu = findViewById(R.id.btnBackToMenu);
    }

    /**
     * 读取双方最终分数并根据分数判断胜者或平局。
     */
    private void initWinnerInfo() {
        Intent intent = getIntent();

        long p1UserId = intent.getLongExtra("player1UserId", -1);
        String p1Username = intent.getStringExtra("player1Username");
        int p1AvatarId = intent.getIntExtra("player1AvatarId", 1);
        int p1Score = intent.getIntExtra("player1Score", 0);

        long p2UserId = intent.getLongExtra("player2UserId", -1);
        String p2Username = intent.getStringExtra("player2Username");
        int p2AvatarId = intent.getIntExtra("player2AvatarId", 1);
        int p2Score = intent.getIntExtra("player2Score", 0);

        // 不再优先相信 winnerUserId，直接按最终分数判断
        if (p1Score > p2Score) {
            tvWinnerName.setText(p1Username == null ? "USER1" : p1Username);
            ivWinnerAvatar.setImageResource(AvatarUtil.getAvatarResId(this, p1AvatarId));
            imgWin.setVisibility(View.VISIBLE);
            return;
        }

        if (p2Score > p1Score) {
            tvWinnerName.setText(p2Username == null ? "USER2" : p2Username);
            ivWinnerAvatar.setImageResource(AvatarUtil.getAvatarResId(this, p2AvatarId));
            imgWin.setVisibility(View.VISIBLE);
            return;
        }

        // 平局
        tvWinnerName.setText("DRAW");
        ivWinnerAvatar.setImageResource(R.drawable.img_avatar_placeholder);
        imgWin.setVisibility(View.GONE);
    }

    /**
     * 绑定返回主菜单按钮。
     */
    private void initClickEvents() {
        PressEffectUtil.apply(btnBackToMenu);

        btnBackToMenu.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
        });
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