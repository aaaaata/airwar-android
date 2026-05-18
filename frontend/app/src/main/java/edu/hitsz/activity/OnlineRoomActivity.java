package edu.hitsz.activity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import edu.hitsz.R;
import edu.hitsz.config.GameConfig;
import edu.hitsz.manager.MenuBgmManager;
import edu.hitsz.application.PkApiClient;
import edu.hitsz.application.UserSession;
import edu.hitsz.util.AvatarUtil;
import edu.hitsz.manager.BackgroundScrollManager;
import edu.hitsz.util.FullScreenUtil;
import edu.hitsz.util.PressEffectUtil;
import edu.hitsz.widget.DigitTextView;
import edu.hitsz.widget.RoundCornerImageView;

/**
 * 联机房间页面。
 *
 * 作用：
 * 1. 支持创建房间和输入房间号加入房间；
 * 2. 展示房间号、自己与对手的头像和用户名；
 * 3. 通过 HTTP 轮询后端房间状态，等待对手加入和双方准备；
 * 4. 当房间状态变为 PLAYING 时，携带双方信息进入 PkGameActivity。
 *
 * 这里使用 HTTP 做“房间管理和准备状态同步”，真正游戏中的实时分数同步交给 WebSocket。
 */
public class OnlineRoomActivity extends AppCompatActivity {
    // panelSelect：创建 / 加入房间面板；panelWaiting：等待对手和准备开始面板。
    // selfXXX 保存当前玩家信息，opponentXXX 保存对手信息。
    // pollHandler 用于轮询房间状态，dotHandler 用于等待动画。

    private View innerScreen;
    private FrameLayout panelSelect;
    private FrameLayout panelWaiting;

    private EditText etRoomCode;

    private ImageButton btnJoinRoom;
    private ImageButton btnCreateRoom;
    private ImageButton btnStart;

    private DigitTextView digitRoomCode;
    private TextView tvSelfName;
    private TextView tvOpponentName;

    private RoundCornerImageView ivSelfAvatar;
    private RoundCornerImageView ivOpponentAvatar;

    private final PkApiClient apiClient = new PkApiClient();

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Handler dotHandler = new Handler(Looper.getMainLooper());

    private boolean soundEnabled;

    private long selfUserId;
    private String selfUsername;
    private int selfAvatarId;

    private long roomId = -1;
    private String roomCode = "";

    private long opponentUserId = -1;
    private String opponentUsername = "";
    private int opponentAvatarId = 1;

    private int dotCount = 0;
    private boolean enteringGame = false;
    private boolean selfReady = false;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (roomId > 0 && !enteringGame) {
                requestRoomStatus();
                pollHandler.postDelayed(this, 1000);
            }
        }
    };

    private final Runnable waitingDotsRunnable = new Runnable() {
        @Override
        public void run() {
            if (opponentUserId > 0) {
                return;
            }

            dotCount = dotCount % 3 + 1;
            String dots = dotCount == 1 ? "." : dotCount == 2 ? ".." : "...";
            tvOpponentName.setText("WAITING" + dots);
            dotHandler.postDelayed(this, 500);
        }
    };

    /**
     * 初始化联机房间页，读取用户信息、绑定控件、设置点击事件并调整等待面板尺寸。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FullScreenUtil.hideSystemBars(this);

        BackgroundScrollManager.getInstance().ensureInitialized();
        setContentView(R.layout.activity_online_room);

        readIntent();
        bindViews();
        initViews();
        initClickEvents();
        adjustWaitingPanelSize();
    }

    /**
     * 从上一页 Intent 中读取音效状态和当前玩家账号信息。
     */
    private void readIntent() {
        Intent intent = getIntent();

        soundEnabled = intent.getBooleanExtra(GameConfig.EXTRA_SOUND_ENABLED, true);

        selfUserId = intent.getLongExtra("userId", UserSession.getUserId());
        selfUsername = intent.getStringExtra("username");
        selfAvatarId = intent.getIntExtra("avatarId", UserSession.getAvatarId());

        if (selfUsername == null || selfUsername.trim().isEmpty()) {
            selfUsername = UserSession.getUsername();
        }
        if (selfUsername == null || selfUsername.trim().isEmpty()) {
            selfUsername = "GUEST";
        }
    }

    /**
     * 集中绑定布局中的所有控件，便于后续方法直接使用。
     */
    private void bindViews() {
        innerScreen = findViewById(R.id.innerScreen);
        panelSelect = findViewById(R.id.panelSelect);
        panelWaiting = findViewById(R.id.panelWaiting);

        etRoomCode = findViewById(R.id.etRoomCode);

        btnJoinRoom = findViewById(R.id.btnJoinRoom);
        btnCreateRoom = findViewById(R.id.btnCreateRoom);
        btnStart = findViewById(R.id.btnStart);

        digitRoomCode = findViewById(R.id.digitRoomCode);
        tvSelfName = findViewById(R.id.tvSelfName);
        tvOpponentName = findViewById(R.id.tvOpponentName);

        ivSelfAvatar = findViewById(R.id.ivSelfAvatar);
        ivOpponentAvatar = findViewById(R.id.ivOpponentAvatar);
    }

    /**
     * 设置页面初始状态：显示房间选择面板，隐藏等待面板。
     */
    private void initViews() {
        panelSelect.setVisibility(View.VISIBLE);
        panelWaiting.setVisibility(View.GONE);

        tvSelfName.setText(selfUsername);
        ivSelfAvatar.setImageResource(AvatarUtil.getAvatarResId(this, selfAvatarId));

        setReadyVisual(false);
        setStartEnabled(false);
    }

    /**
     * 绑定创建房间、加入房间和准备按钮的点击逻辑。
     */
    private void initClickEvents() {
        PressEffectUtil.apply(btnJoinRoom, btnCreateRoom);

        btnCreateRoom.setOnClickListener(v -> createRoom());

        btnJoinRoom.setOnClickListener(v -> {
            String inputCode = etRoomCode.getText().toString().trim();
            if (inputCode.isEmpty()) {
                Toast.makeText(this, "请输入房间号", Toast.LENGTH_SHORT).show();
                return;
            }
            joinRoom(inputCode);
        });

        btnStart.setOnClickListener(v -> ready());
    }

    /**
     * 根据屏幕宽高动态调整等待房间面板、头像和 READY 按钮大小。
     */
    private void adjustWaitingPanelSize() {
        innerScreen.post(() -> {
            int screenW = innerScreen.getWidth();
            int screenH = innerScreen.getHeight();
            if (screenW <= 0 || screenH <= 0) {
                return;
            }

            int panelW = Math.round(screenW * 0.78f);
            int titleH = clamp(Math.round(panelW * 0.11f), dp(28), dp(42));
            int roomRowH = clamp(Math.round(panelW * 0.085f), dp(24), dp(34));
            int avatarAreaH = clamp(Math.round(panelW * 0.34f), dp(92), dp(120));
            int readyH = clamp(getDrawableHeightForWidth(R.drawable.btn_ready_normal, panelW), dp(32), dp(58));

            int panelH = dp(16) + titleH + dp(8) + roomRowH + dp(8) + avatarAreaH + readyH;
            panelH = Math.min(panelH, Math.round(screenH * 0.82f));

            ViewGroup.LayoutParams lp = panelWaiting.getLayoutParams();
            lp.width = panelW;
            lp.height = panelH;
            panelWaiting.setLayoutParams(lp);

            setHeight(findViewById(R.id.ivLobbyTitle), titleH);
            setHeight(findViewById(R.id.roomIdRow), roomRowH);
            setHeight(findViewById(R.id.versusArea), avatarAreaH);
            setSize(btnStart, panelW, readyH);

            int avatarSize = clamp(Math.round(panelW * 0.225f), dp(58), dp(78));
            setSize(ivSelfAvatar, avatarSize, avatarSize);
            setSize(ivOpponentAvatar, avatarSize, avatarSize);
            ivSelfAvatar.setCornerRadius(Math.round(avatarSize * 0.16f));
            ivOpponentAvatar.setCornerRadius(Math.round(avatarSize * 0.16f));
        });
    }

    /**
     * 调用后端创建房间接口，成功后进入等待面板并开始轮询房间状态。
     */
    private void createRoom() {
        if (!checkOnlineLogin()) {
            return;
        }

        etRoomCode.setText("");
        Toast.makeText(this, "系统将自动生成房间号", Toast.LENGTH_SHORT).show();
        btnCreateRoom.setEnabled(false);

        apiClient.createRoom(selfUserId, selfUsername, selfAvatarId, new PkApiClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                btnCreateRoom.setEnabled(true);
                if (!"success".equals(json.optString("status"))) {
                    Toast.makeText(OnlineRoomActivity.this, "创建房间失败：" + json.optString("message"), Toast.LENGTH_SHORT).show();
                    return;
                }
                updateRoomFromJson(json);
                showWaitingPanel();
                startPollingRoom();
            }

            @Override
            public void onFail(String message) {
                btnCreateRoom.setEnabled(true);
                Toast.makeText(OnlineRoomActivity.this, "网络错误：" + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 调用后端加入房间接口，成功后进入等待面板并开始轮询房间状态。
     */
    private void joinRoom(String inputRoomCode) {
        if (!checkOnlineLogin()) {
            return;
        }

        btnJoinRoom.setEnabled(false);

        apiClient.joinRoom(inputRoomCode, selfUserId, selfUsername, selfAvatarId, new PkApiClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                btnJoinRoom.setEnabled(true);
                if (!"success".equals(json.optString("status"))) {
                    Toast.makeText(OnlineRoomActivity.this, "加入失败：" + json.optString("message"), Toast.LENGTH_SHORT).show();
                    return;
                }
                updateRoomFromJson(json);
                showWaitingPanel();
                startPollingRoom();
            }

            @Override
            public void onFail(String message) {
                btnJoinRoom.setEnabled(true);
                Toast.makeText(OnlineRoomActivity.this, "网络错误：" + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 玩家点击 READY 后通知后端，若双方都准备则进入 PK 游戏。
     */
    private void ready() {
        if (roomId <= 0) {
            return;
        }
        if (opponentUserId <= 0) {
            Toast.makeText(this, "等待对方加入", Toast.LENGTH_SHORT).show();
            return;
        }

        selfReady = true;
        setReadyVisual(true);
        btnStart.setEnabled(false);

        apiClient.ready(roomId, selfUserId, new PkApiClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                updateRoomFromJson(json);
                String roomStatus = json.optString("roomStatus");
                if ("PLAYING".equals(roomStatus)) {
                    enterPkGame();
                } else {
                    Toast.makeText(OnlineRoomActivity.this, "READY，等待对方准备", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFail(String message) {
                selfReady = false;
                setReadyVisual(false);
                btnStart.setEnabled(true);
                Toast.makeText(OnlineRoomActivity.this, "准备失败：" + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 定时请求后端房间状态，用于发现对手加入或游戏开始。
     */
    private void requestRoomStatus() {
        apiClient.roomStatus(roomId, new PkApiClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                if (!"success".equals(json.optString("status"))) {
                    return;
                }
                updateRoomFromJson(json);
                if ("PLAYING".equals(json.optString("roomStatus"))) {
                    enterPkGame();
                }
            }

            @Override
            public void onFail(String message) {
            }
        });
    }

    /**
     * 把后端返回的房间 JSON 更新到本地变量和界面显示。
     */
    private void updateRoomFromJson(JSONObject json) {
        roomId = json.optLong("roomId", roomId);
        roomCode = json.optString("roomCode", roomCode);

        long p1UserId = json.optLong("player1UserId", -1);
        String p1Username = json.optString("player1Username", "");
        int p1AvatarId = json.optInt("player1AvatarId", 1);

        long p2UserId = json.optLong("player2UserId", -1);
        String p2Username = json.optString("player2Username", "");
        int p2AvatarId = json.optInt("player2AvatarId", 1);

        if (p1UserId == selfUserId) {
            opponentUserId = p2UserId;
            opponentUsername = p2Username;
            opponentAvatarId = p2AvatarId;
        } else {
            opponentUserId = p1UserId;
            opponentUsername = p1Username;
            opponentAvatarId = p1AvatarId;
        }

        digitRoomCode.setTextValue(roomCode);
        tvSelfName.setText(selfUsername);
        ivSelfAvatar.setImageResource(AvatarUtil.getAvatarResId(this, selfAvatarId));

        if (opponentUserId > 0) {
            stopWaitingDots();
            tvOpponentName.setText(opponentUsername);
            ivOpponentAvatar.setImageResource(AvatarUtil.getAvatarResId(this, opponentAvatarId));
            setStartEnabled(!selfReady);
        } else {
            ivOpponentAvatar.setImageResource(R.drawable.img_avatar_placeholder);
            setStartEnabled(false);
            startWaitingDots();
        }
    }

    /**
     * 进入联机相关操作前检查是否已登录。
     */
    private boolean checkOnlineLogin() {
        if (selfUserId <= 0 || !UserSession.isLoggedIn()) {
            Toast.makeText(this, "请先登录账号再进入联机模式", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * 从创建 / 加入房间界面切换到等待房间界面。
     */
    private void showWaitingPanel() {
        panelSelect.setVisibility(View.GONE);
        panelWaiting.setVisibility(View.VISIBLE);
        adjustWaitingPanelSize();
    }

    /**
     * 控制 READY 按钮能否点击，并通过透明度给玩家反馈。
     */
    private void setStartEnabled(boolean enabled) {
        btnStart.setEnabled(enabled);
        btnStart.setAlpha(enabled ? 1.0f : 0.45f);
    }

    /**
     * 根据是否已准备切换 READY 按钮图片。
     */
    private void setReadyVisual(boolean ready) {
        btnStart.setImageResource(ready ? R.drawable.btn_ready_pressed : R.drawable.btn_ready_normal);
    }

    /**
     * 启动房间状态轮询。
     */
    private void startPollingRoom() {
        pollHandler.removeCallbacks(pollRunnable);
        pollHandler.post(pollRunnable);
    }

    /**
     * 启动等待对手加入的点点点动画。
     */
    private void startWaitingDots() {
        dotHandler.removeCallbacks(waitingDotsRunnable);
        dotHandler.post(waitingDotsRunnable);
    }

    /**
     * 停止等待动画。
     */
    private void stopWaitingDots() {
        dotHandler.removeCallbacks(waitingDotsRunnable);
    }

    /**
     * 携带房间和双方玩家信息进入 PkGameActivity。
     */
    private void enterPkGame() {
        if (enteringGame) {
            return;
        }

        enteringGame = true;
        pollHandler.removeCallbacks(pollRunnable);
        dotHandler.removeCallbacks(waitingDotsRunnable);

        Intent intent = new Intent(this, PkGameActivity.class);
        intent.putExtra(GameConfig.EXTRA_SOUND_ENABLED, soundEnabled);
        intent.putExtra("roomId", roomId);
        intent.putExtra("roomCode", roomCode);
        intent.putExtra("selfUserId", selfUserId);
        intent.putExtra("selfUsername", selfUsername);
        intent.putExtra("selfAvatarId", selfAvatarId);
        intent.putExtra("opponentUserId", opponentUserId);
        intent.putExtra("opponentUsername", opponentUsername);
        intent.putExtra("opponentAvatarId", opponentAvatarId);

        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }

    /**
     * 页面销毁时移除轮询和动画回调，避免内存泄漏。
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        pollHandler.removeCallbacks(pollRunnable);
        dotHandler.removeCallbacks(waitingDotsRunnable);
    }


    private int getDrawableHeightForWidth(int resId, int targetWidth) {
        Drawable drawable = getResources().getDrawable(resId, getTheme());
        if (drawable == null || drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            return Math.round(targetWidth * 0.12f);
        }
        return Math.round(targetWidth * drawable.getIntrinsicHeight() / (float) drawable.getIntrinsicWidth());
    }

    private void setHeight(View view, int height) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.height = height;
        view.setLayoutParams(lp);
    }

    private void setSize(View view, int width, int height) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.width = width;
        lp.height = height;
        view.setLayoutParams(lp);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

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
