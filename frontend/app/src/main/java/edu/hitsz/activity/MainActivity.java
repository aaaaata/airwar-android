package edu.hitsz.activity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import edu.hitsz.R;
import edu.hitsz.config.GameConfig;
import edu.hitsz.manager.MenuBgmManager;
import edu.hitsz.manager.SoundSettingManager;
import edu.hitsz.manager.SessionManager;
import edu.hitsz.util.AvatarUtil;
import edu.hitsz.manager.BackgroundScrollManager;
import edu.hitsz.util.FullScreenUtil;
import edu.hitsz.util.PressEffectUtil;
import edu.hitsz.widget.DigitTextView;
import edu.hitsz.widget.RoundCornerImageView;

/**
 * 游戏主菜单页面。
 *
 * 作用：
 * 1. 作为登录后的主入口，展示账号信息、音效开关和主要功能按钮；
 * 2. 管理菜单背景音乐开关，并把音效状态传递给后续页面；
 * 3. 根据用户登录状态刷新头像、用户名、用户 ID 和登录天数；
 * 4. 跳转到模式选择、排行榜、商店等功能页面；
 * 5. 对顶部账号栏和设置按钮做程序化缩放，适配不同屏幕宽度。
 *
 * 主菜单负责全局入口导航和用户状态展示。
 */
public class MainActivity extends AppCompatActivity {
    // 主菜单包含：账号区域、音效开关、设置按钮、开始按钮、排行榜按钮、商店按钮等。
    // 账号区域内部的头像、用户名、用户 ID 和登录天数会根据 SessionManager 动态刷新。

    private View innerScreen;
    private SwitchCompat switchSound;

    private View accountPanel;
    private ImageView ivAccountPanelBg;
    private RoundCornerImageView ivAccountAvatar;
    private LinearLayout accountInfoLayout;
    private View rowAccountName;
    private View rowAccountId;
    private View rowLoginDays;
    private ImageView ivLabelUname;
    private ImageView ivLabelUid;
    private ImageView ivLabelLoginDays;
    private TextView tvAccountNameValue;
    private DigitTextView digitAccountId;
    private DigitTextView digitLoginDays;

    private ImageView ivSettings;
    private ImageView ivStart;
    private ImageView ivRanking;
    private ImageView ivShop;
    private ImageView ivWaiting;

    /**
     * 初始化主菜单控件、音效开关、账号栏、按钮点击事件和页面跳转。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FullScreenUtil.hideSystemBars(this);

        BackgroundScrollManager.getInstance().ensureInitialized();
        setContentView(R.layout.activity_main_menu);

        innerScreen = findViewById(R.id.innerScreen);
        switchSound = findViewById(R.id.switchSound);

        accountPanel = findViewById(R.id.accountPanel);
        ivAccountPanelBg = findViewById(R.id.ivAccountPanelBg);
        ivAccountAvatar = findViewById(R.id.ivAccountAvatar);
        accountInfoLayout = findViewById(R.id.accountInfoLayout);
        rowAccountName = findViewById(R.id.rowAccountName);
        rowAccountId = findViewById(R.id.rowAccountId);
        rowLoginDays = findViewById(R.id.rowLoginDays);
        ivLabelUname = findViewById(R.id.ivLabelUname);
        ivLabelUid = findViewById(R.id.ivLabelUid);
        ivLabelLoginDays = findViewById(R.id.ivLabelLoginDays);
        tvAccountNameValue = findViewById(R.id.tvAccountNameValue);
        digitAccountId = findViewById(R.id.digitAccountId);
        digitLoginDays = findViewById(R.id.digitLoginDays);

        ivSettings = findViewById(R.id.ivSettings);
        ivStart = findViewById(R.id.ivStart);
        ivRanking = findViewById(R.id.ivRanking);
        ivShop = findViewById(R.id.ivShop);
        ivWaiting = findViewById(R.id.ivWaiting);

        switchSound.setChecked(SoundSettingManager.isSoundEnabled(this));

        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SoundSettingManager.setSoundEnabled(MainActivity.this, isChecked);

            if (isChecked) {
                MenuBgmManager.play(MainActivity.this);
            } else {
                MenuBgmManager.stopAndRelease();
            }
        });

        adjustTopBarSize();
        renderAccountInfo();

        PressEffectUtil.apply(ivSettings, ivStart, ivRanking, ivShop, ivWaiting);

        ivStart.setOnClickListener(v -> {
            boolean soundEnabled = SoundSettingManager.isSoundEnabled(MainActivity.this);

            Intent intent = new Intent(MainActivity.this, ModeSelectActivity.class);
            intent.putExtra(GameConfig.EXTRA_SOUND_ENABLED, soundEnabled);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        ivRanking.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MenuRankingActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        ivSettings.setOnClickListener(v ->
                Toast.makeText(MainActivity.this, "设置功能待开发", Toast.LENGTH_SHORT).show()
        );

        ivShop.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SkinShopActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        ivWaiting.setOnClickListener(v ->
                Toast.makeText(MainActivity.this, "该功能待开发", Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * 每次回到主菜单时刷新账号信息、音效开关状态，并恢复菜单音乐。
     */
    @Override
    protected void onResume() {
        super.onResume();

        FullScreenUtil.hideSystemBars(this);

        renderAccountInfo();

        switchSound.setChecked(SoundSettingManager.isSoundEnabled(this));
        MenuBgmManager.play(this);
    }

    /**
     * 主菜单不可见时暂停菜单音乐。
     */
    @Override
    protected void onPause() {
        super.onPause();

        MenuBgmManager.pause();
    }

    /**
     * 根据是否登录展示真实账号信息或游客默认信息。
     */
    private void renderAccountInfo() {
        if (SessionManager.isLoggedIn(this)) {
            String username = SessionManager.getUsername(this);
            int userId = SessionManager.getUserId(this);
            int avatarId = SessionManager.getAvatarId(this);
            int loginDays = SessionManager.getLoginDays(this);

            tvAccountNameValue.setText(username == null || username.trim().isEmpty() ? "USER" : username);
            digitAccountId.setLongNumber(userId);
            digitLoginDays.setNumber(loginDays);
            ivAccountAvatar.setImageResource(AvatarUtil.getAvatarResId(avatarId));
        } else {
            tvAccountNameValue.setText("GUEST");
            digitAccountId.setTextValue("----");
            digitLoginDays.setNumber(0);
            ivAccountAvatar.setImageResource(R.drawable.img_avatar_placeholder);
        }
    }

    /**
     * 根据账号栏和设置按钮素材比例，计算顶部区域的实际显示宽高。
     */
    private void adjustTopBarSize() {
        innerScreen.post(() -> {
            Drawable accountDrawable = ivAccountPanelBg.getDrawable();
            Drawable settingsDrawable = ivSettings.getDrawable();
            if (accountDrawable == null || settingsDrawable == null) {
                return;
            }

            int innerWidth = innerScreen.getWidth();
            if (innerWidth <= 0) {
                return;
            }

            float accountRatio = accountDrawable.getIntrinsicWidth()
                    / (float) accountDrawable.getIntrinsicHeight();
            float settingsRatio = settingsDrawable.getIntrinsicWidth()
                    / (float) settingsDrawable.getIntrinsicHeight();

            int targetHeight = Math.round(innerWidth / (accountRatio + settingsRatio));
            int settingsWidth = Math.round(targetHeight * settingsRatio);
            int accountWidth = innerWidth - settingsWidth;

            accountPanel.setLayoutParams(new LinearLayout.LayoutParams(accountWidth, targetHeight));
            ivSettings.setLayoutParams(new LinearLayout.LayoutParams(settingsWidth, targetHeight));

            adjustAccountContent(accountWidth, targetHeight);
        });
    }

    /**
     * 在账号面板内部重新计算头像、文字标签和数字控件的尺寸与位置。
     */
    private void adjustAccountContent(int accountWidth, int accountHeight) {
        int avatarSize = Math.round(accountHeight * 0.80f);
        int avatarTop = Math.round(accountHeight * 0.10f);
        int avatarStart = Math.round(accountHeight * 0.12f);

        FrameLayout.LayoutParams avatarLp = new FrameLayout.LayoutParams(avatarSize, avatarSize);
        avatarLp.leftMargin = avatarStart;
        avatarLp.topMargin = avatarTop;
        avatarLp.gravity = Gravity.START | Gravity.TOP;
        ivAccountAvatar.setLayoutParams(avatarLp);
        ivAccountAvatar.setCornerRadius(Math.round(avatarSize * 0.14f));

        int gap = Math.round(accountHeight * 0.12f);
        int infoStart = avatarStart + avatarSize + gap;
        int infoEndMargin = Math.round(accountHeight * 0.10f);
        int infoWidth = Math.max(1, accountWidth - infoStart - infoEndMargin);

        FrameLayout.LayoutParams infoLp = new FrameLayout.LayoutParams(infoWidth, avatarSize);
        infoLp.leftMargin = infoStart;
        infoLp.topMargin = avatarTop;
        infoLp.gravity = Gravity.START | Gravity.TOP;
        accountInfoLayout.setLayoutParams(infoLp);

        int nameRowH = Math.round(avatarSize * 0.30f);
        int smallRowH = Math.round(avatarSize * 0.22f);
        int rowGap = Math.max(1, (avatarSize - nameRowH - smallRowH * 2) / 2);

        setRowHeightAndBottomMargin(rowAccountName, nameRowH, rowGap);
        setRowHeightAndBottomMargin(rowAccountId, smallRowH, rowGap);
        setRowHeightAndBottomMargin(rowLoginDays, smallRowH, 0);

        setImageHeight(ivLabelUname, Math.round(nameRowH * 0.58f));
        setImageHeight(ivLabelUid, Math.round(smallRowH * 0.62f));
        setImageHeight(ivLabelLoginDays, Math.round(smallRowH * 0.62f));

        tvAccountNameValue.setTextSize(TypedValue.COMPLEX_UNIT_PX, nameRowH * 0.58f);
        setViewHeight(digitAccountId, Math.round(smallRowH * 0.62f));
        setViewHeight(digitLoginDays, Math.round(smallRowH * 0.62f));
        digitAccountId.setCharSpacingPx(dp(1));
        digitLoginDays.setCharSpacingPx(dp(1));
    }

    /**
     * 设置账号信息某一行的高度和下边距。
     */
    private void setRowHeightAndBottomMargin(View row, int height, int bottomMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height
        );
        lp.bottomMargin = bottomMargin;
        row.setLayoutParams(lp);
    }

    /**
     * 只调整图片高度，宽度交给 ImageView 按比例自适应。
     */
    private void setImageHeight(ImageView imageView, int height) {
        ViewGroup.LayoutParams lp = imageView.getLayoutParams();
        lp.height = Math.max(1, height);
        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        imageView.setLayoutParams(lp);
    }

    /**
     * 通用高度设置方法。
     */
    private void setViewHeight(View view, int height) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.height = Math.max(1, height);
        view.setLayoutParams(lp);
    }

    /**
     * 把 dp 转换成像素，用于跨屏幕密度适配。
     */
    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        ));
    }
}