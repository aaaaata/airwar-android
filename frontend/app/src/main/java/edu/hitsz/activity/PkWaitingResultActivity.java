package edu.hitsz.activity;

/*
 * 说明：本文件为“讲解注释版”。
 * 注释只解释页面职责、核心流程和关键方法，不改变任何业务逻辑。
 */

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import edu.hitsz.R;
import edu.hitsz.manager.MenuBgmManager;
import edu.hitsz.application.PkApiClient;
import edu.hitsz.manager.BackgroundScrollManager;
import edu.hitsz.util.FullScreenUtil;

/**
 * 联机对战等待结果页面。
 *
 * 作用：
 * 1. 当自己先结束游戏、对手尚未结束时进入该页面；
 * 2. 用简单的点点点动画提示正在等待；
 * 3. 定时轮询房间状态，直到后端返回 FINISHED；
 * 4. 获取双方最终数据后跳转到 PkResultActivity。
 *
 * 这个页面解决了“双人结束时间不同步”的问题。
 */
public class PkWaitingResultActivity extends AppCompatActivity {
    // roomId 用于向后端查询本房间结果；enteringResult 防止重复跳转结果页。


    private TextView tvWaitingResult;

    private long roomId;

    private final PkApiClient apiClient = new PkApiClient();

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Handler dotHandler = new Handler(Looper.getMainLooper());

    private int dotCount = 0;
    private boolean enteringResult = false;

    /**
     * 等待动画任务：循环显示不同数量的点，表示还在等待对手结束。
     */
    private final Runnable dotRunnable = new Runnable() {
        @Override
        public void run() {
            dotCount = dotCount % 3 + 1;

            String dots;
            if (dotCount == 1) {
                dots = ".";
            } else if (dotCount == 2) {
                dots = "..";
            } else {
                dots = "...";
            }

            tvWaitingResult.setText(dots);
            dotHandler.postDelayed(this, 500);
        }
    };

    /**
     * 结果轮询任务：定时向后端查询房间是否已经进入 FINISHED 状态。
     */
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!enteringResult) {
                requestRoomStatus();
                pollHandler.postDelayed(this, 1000);
            }
        }
    };

    /**
     * 初始化等待结果页，读取房间号并启动等待动画和状态轮询。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FullScreenUtil.hideSystemBars(this);

        BackgroundScrollManager.getInstance().ensureInitialized();
        setContentView(R.layout.activity_pk_waiting_result);

        tvWaitingResult = findViewById(R.id.tvWaitingResult);

        roomId = getIntent().getLongExtra("roomId", -1);

        dotHandler.post(dotRunnable);
        pollHandler.post(pollRunnable);
    }

    /**
     * 向后端查询房间状态，发现双方都结束后进入结果页。
     */
    private void requestRoomStatus() {
        apiClient.roomStatus(roomId, new PkApiClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                if (!"success".equals(json.optString("status"))) {
                    return;
                }

                if ("FINISHED".equals(json.optString("roomStatus"))) {
                    enterResultPage(json);
                }
            }

            @Override
            public void onFail(String message) {
            }
        });
    }

    /**
     * 把后端返回的最终比赛数据传递给 PkResultActivity。
     */
    private void enterResultPage(JSONObject json) {
        if (enteringResult) {
            return;
        }

        enteringResult = true;

        pollHandler.removeCallbacks(pollRunnable);
        dotHandler.removeCallbacks(dotRunnable);

        Intent intent = new Intent(this, PkResultActivity.class);

        intent.putExtra("roomId", json.optLong("roomId", roomId));
        intent.putExtra("roomCode", json.optString("roomCode", ""));

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
     * 页面销毁时停止动画和轮询回调。
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        pollHandler.removeCallbacks(pollRunnable);
        dotHandler.removeCallbacks(dotRunnable);
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