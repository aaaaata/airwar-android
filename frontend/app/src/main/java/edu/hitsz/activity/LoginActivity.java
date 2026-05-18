package edu.hitsz.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;

import edu.hitsz.R;
import edu.hitsz.config.ApiConfig;
import edu.hitsz.manager.MenuBgmManager;
import edu.hitsz.application.UserSession;
import edu.hitsz.manager.SessionManager;
import edu.hitsz.util.FullScreenUtil;
import edu.hitsz.util.GrayPressEffectUtil;
import edu.hitsz.widget.RoundCornerImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 登录与注册页面。
 *
 * 作用：
 * 1. 展示账号、密码、头像选择等登录注册 UI；
 * 2. 根据屏幕尺寸动态调整登录面板和头像区域，保证不同机型显示一致；
 * 3. 使用 OkHttp 向后端发送登录 / 注册请求；
 * 4. 登录成功后保存用户会话信息，并进入主菜单。
 *
 * 这个类体现了 Android UI 适配、网络请求和本地会话保存三部分逻辑。
 */
public class LoginActivity extends AppCompatActivity {
    // 登录页主要由登录面板、输入框、头像选择区、登录按钮和注册按钮组成。
    // selectedAvatarId 保存当前选中的头像编号，登录 / 注册时会提交或保存该信息。
    // OkHttpClient 负责网络请求，Handler 负责回到主线程更新 Toast 和页面跳转。


    private View innerScreen;
    private FrameLayout loginPanel;
    private LinearLayout loginContent;
    private LinearLayout loginButtonRow;
    private LinearLayout avatarContainer;
    private ImageView ivLoginTitle;
    private ImageView ivChooseAvatarLabel;

    private EditText etUsername;
    private EditText etPassword;
    private ImageView btnLogin;
    private ImageView btnRegister;
    private ImageView ivPasswordEye;
    private boolean passwordVisible = false;

    private ImageView[] avatarViews;
    private int selectedAvatarId = 1;

    private static final String TAG = "LoginActivity";

    private final OkHttpClient client = new OkHttpClient();
    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * 初始化登录页控件、头像选择、按钮点击事件和全屏显示。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        FullScreenUtil.hideSystemBars(this);

        setContentView(R.layout.activity_login);

        innerScreen = findViewById(R.id.innerScreen);
        loginPanel = findViewById(R.id.loginPanel);
        loginContent = findViewById(R.id.loginContent);
        loginButtonRow = findViewById(R.id.loginButtonRow);
        avatarContainer = findViewById(R.id.avatarContainer);
        ivLoginTitle = findViewById(R.id.ivLoginTitle);
        ivChooseAvatarLabel = findViewById(R.id.ivChooseAvatarLabel);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);
        ivPasswordEye = findViewById(R.id.ivPasswordEye);

        avatarViews = new ImageView[]{
                findViewById(R.id.ivAvatar1),
                findViewById(R.id.ivAvatar2),
                findViewById(R.id.ivAvatar3),
                findViewById(R.id.ivAvatar4),
                findViewById(R.id.ivAvatar5),
                findViewById(R.id.ivAvatar6)
        };

        adjustLoginPanelSize();
        bindAvatarEvents();

        GrayPressEffectUtil.apply(btnLogin, btnRegister);

        ivPasswordEye.setOnClickListener(v -> togglePasswordVisible());
        btnLogin.setOnClickListener(v -> login());
        btnRegister.setOnClickListener(v -> register());
    }

    /**
     * 根据 innerScreen 实际尺寸动态计算登录弹窗、输入框、头像和按钮大小。
     */
    private void adjustLoginPanelSize() {
        innerScreen.post(() -> {
            int screenW = innerScreen.getWidth();
            int screenH = innerScreen.getHeight();
            if (screenW <= 0 || screenH <= 0) {
                return;
            }

            int panelW = Math.round(screenW * 0.86f);
            int sideMargin = Math.round(panelW * 0.055f);

            int titleH = clamp(Math.round(panelW * 0.105f), dp(30), dp(44));
            int inputH = clamp(Math.round(panelW * 0.11f), dp(34), dp(44));
            int chooseLabelH = clamp(Math.round(panelW * 0.05f), dp(14), dp(22));
            int avatarAreaH = clamp(Math.round(panelW * 0.125f), dp(42), dp(54));
            int buttonH = clamp(Math.round(panelW * 0.145f), dp(44), dp(58));

            int top = dp(18);
            int titleToInput = dp(12);
            int inputGap = dp(10);
            int labelGap = dp(10);
            int avatarGap = dp(6);
            int contentBottomGap = dp(10);

            int contentH = top + titleH + titleToInput + inputH + inputGap + inputH
                    + labelGap + chooseLabelH + avatarGap + avatarAreaH + contentBottomGap;
            int panelH = contentH + buttonH;

            int maxPanelH = Math.round(screenH * 0.88f);
            if (panelH > maxPanelH) {
                int overflow = panelH - maxPanelH;
                inputH = Math.max(dp(30), inputH - overflow / 2);
                contentH = top + titleH + titleToInput + inputH + inputGap + inputH
                        + labelGap + chooseLabelH + avatarGap + avatarAreaH + contentBottomGap;
                panelH = Math.min(maxPanelH, contentH + buttonH);
            }

            ViewGroup.LayoutParams panelLp = loginPanel.getLayoutParams();
            panelLp.width = panelW;
            panelLp.height = panelH;
            loginPanel.setLayoutParams(panelLp);

            FrameLayout.LayoutParams contentLp = (FrameLayout.LayoutParams) loginContent.getLayoutParams();
            contentLp.leftMargin = sideMargin;
            contentLp.rightMargin = sideMargin;
            contentLp.topMargin = top;
            contentLp.bottomMargin = buttonH;
            loginContent.setLayoutParams(contentLp);

            setHeight(ivLoginTitle, titleH);
            setHeight(etUsername, inputH);
            setHeight(findViewById(R.id.passwordBox), inputH);
            setHeight(ivChooseAvatarLabel, chooseLabelH);
            setHeight(avatarContainer, avatarAreaH);
            setHeight(loginButtonRow, buttonH);

            int avatarSize = Math.round(avatarAreaH * 0.82f);
            for (ImageView avatarView : avatarViews) {
                ViewGroup.LayoutParams lp = avatarView.getLayoutParams();
                lp.width = avatarSize;
                lp.height = avatarSize;
                avatarView.setLayoutParams(lp);
                if (avatarView instanceof RoundCornerImageView) {
                    ((RoundCornerImageView) avatarView).setCornerRadius(Math.round(avatarSize * 0.16f));
                }
            }
        });
    }

    /**
     * 通用工具方法：只修改某个 View 的高度。
     */
    private void setHeight(View view, int height) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.height = height;
        view.setLayoutParams(lp);
    }

    /**
     * 给所有头像绑定点击事件，点击后更新当前选中的头像编号。
     */
    private void bindAvatarEvents() {
        for (int i = 0; i < avatarViews.length; i++) {
            final int avatarId = i + 1;
            avatarViews[i].setOnClickListener(v -> {
                selectedAvatarId = avatarId;
                updateAvatarSelection();
            });
        }
        updateAvatarSelection();
    }

    /**
     * 刷新头像选中状态，给选中的头像加选中边框。
     */
    private void updateAvatarSelection() {
        for (int i = 0; i < avatarViews.length; i++) {
            if (i + 1 == selectedAvatarId) {
                avatarViews[i].setBackgroundResource(R.drawable.bg_avatar_selected);
            } else {
                avatarViews[i].setBackgroundResource(R.drawable.bg_avatar_normal);
            }
        }
    }

    /**
     * 切换密码明文 / 密文显示，同时保持光标位置在文本末尾。
     */
    private void togglePasswordVisible() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    /**
     * 读取输入并向后端 /login 接口发送登录请求。
     */
    private void login() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        HttpUrl base = HttpUrl.parse(ApiConfig.BASE_URL + "/login");
        if (base == null) {
            Toast.makeText(this, "服务器地址错误", Toast.LENGTH_SHORT).show();
            return;
        }

        HttpUrl url = base.newBuilder()
                .addQueryParameter("username", username)
                .addQueryParameter("password", password)
                .build();

        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "login failed", e);
                handler.post(() -> Toast.makeText(LoginActivity.this, "连接服务器失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body() == null ? "" : response.body().string().trim();
                Log.d(TAG, "login response = " + result);
                handler.post(() -> handleLoginResult(username, result));
            }
        });
    }

    /**
     * 解析登录结果，成功后保存 Session 并进入主菜单。
     */
    private void handleLoginResult(String inputUsername, String result) {
        try {
            JSONObject obj = new JSONObject(result);
            String status = obj.optString("status");

            if ("success".equals(status)) {
                int userId = obj.optInt("userId", -1);
                String username = obj.optString("username", inputUsername);
                int avatarId = obj.optInt("avatarId", 1);
                int loginDays = obj.optInt("loginDays", 1);

                UserSession.login(userId, username, avatarId, loginDays);
                SessionManager.saveUser(this, userId, username, avatarId, loginDays);

                Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            } else {
                Toast.makeText(this, "账号或密码错误", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            if ("success".equals(result)) {
                SessionManager.saveUser(this, -1, inputUsername, selectedAvatarId, 1);
                Toast.makeText(this, "登录成功，但服务器未返回完整账号信息", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            } else {
                Toast.makeText(this, "账号或密码错误", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 读取输入和头像选择，向后端 /register 接口发送注册请求。
     */
    private void register() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        HttpUrl base = HttpUrl.parse(ApiConfig.BASE_URL + "/register");
        if (base == null) {
            Toast.makeText(this, "服务器地址错误", Toast.LENGTH_SHORT).show();
            return;
        }

        HttpUrl url = base.newBuilder()
                .addQueryParameter("username", username)
                .addQueryParameter("password", password)
                .addQueryParameter("avatarId", String.valueOf(selectedAvatarId))
                .build();

        Log.d(TAG, "register url = " + url);
        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "register failed", e);
                handler.post(() -> Toast.makeText(LoginActivity.this, "注册失败：" + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body() == null ? "" : response.body().string().trim();
                Log.d(TAG, "register response = " + result);

                handler.post(() -> {
                    if ("success".equals(result)) {
                        Toast.makeText(LoginActivity.this, "注册成功，请登录", Toast.LENGTH_SHORT).show();
                    } else if ("exist".equals(result)) {
                        Toast.makeText(LoginActivity.this, "用户名已存在", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "注册失败：" + result, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    /**
     * 把数值限制在指定范围内，避免控件尺寸过大或过小。
     */
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 把 dp 单位转换成当前屏幕下的像素值。
     */
    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        ));
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
