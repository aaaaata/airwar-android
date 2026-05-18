package edu.hitsz.application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import edu.hitsz.config.ServerConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/*
 * PkWebSocketClient 封装联机对战中的 WebSocket 长连接。
 * 它用于实时发送本方分数/生命值，并接收对方状态、结束、离开等消息，是 PK 模式实时同步的核心。
 */
public class PkWebSocketClient {

    private final long roomId;
    private final long userId;
    private final Listener listener;

    // WebSocket 长连接对象，连接建立后用于实时收发 PK 状态。
    private WebSocket webSocket;

    // 监听接口把网络消息回调给 Activity，由 Activity 决定如何更新界面。
    public interface Listener {
        void onOpponentState(int score, int life);

        void onOpponentFinished(long opponentUserId);

        void onOpponentLeft();

        void onError(String message);
    }

    public PkWebSocketClient(long roomId, long userId, Listener listener) {
        this.roomId = roomId;
        this.userId = userId;
        this.listener = listener;
    }

    // 建立 WebSocket 连接：把 roomId 和 userId 放到 URL 参数中，让服务器识别玩家所在房间。
    public void connect() {
        OkHttpClient client = new OkHttpClient();

        String url = ServerConfig.WS_BASE_URL
                + "?roomId=" + roomId
                + "&userId=" + userId;

        Request request = new Request.Builder()
                .url(url)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                System.out.println("PK WebSocket connected");
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                handleMessage(text);
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket,
                                  @NonNull Throwable t,
                                  @Nullable Response response) {
                if (listener != null) {
                    listener.onError(t.getMessage() == null ? "WebSocket 连接失败" : t.getMessage());
                }
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                System.out.println("PK WebSocket closed: " + reason);
            }
        });
    }

    // 发送本方实时状态：包括分数和生命值，服务器再转发给对手。
    public void sendState(int score, int life) {
        if (webSocket == null) {
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("type", "state");
            json.put("roomId", roomId);
            json.put("userId", userId);
            json.put("score", Math.max(score, 0));
            json.put("life", Math.max(life, 0));

            webSocket.send(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 兼容旧接口：只发分数时复用 sendState。
    public void sendScore(int score) {
        sendState(score, 0);
    }

    // 通知服务器本方战斗已结束，用于对手端或服务器结果流程判断。
    public void sendFinished() {
        if (webSocket == null) {
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("type", "finished");
            json.put("roomId", roomId);
            json.put("userId", userId);

            webSocket.send(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 页面销毁时主动关闭连接，避免内存泄漏和无效消息继续发送。
    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "exit");
            webSocket = null;
        }
    }

    // 解析服务器推送的 JSON 消息，根据 type 分发给不同回调。
    private void handleMessage(String text) {
        try {
            JSONObject json = new JSONObject(text);
            String type = json.optString("type", "");

            if ("opponent_state".equals(type)) {
                int score = json.optInt("score", 0);
                int life = json.optInt("life", -1);

                if (listener != null) {
                    listener.onOpponentState(score, life);
                }
                return;
            }

            if ("opponent_score".equals(type)) {
                int score = json.optInt("score", 0);

                if (listener != null) {
                    listener.onOpponentState(score, -1);
                }
                return;
            }

            if ("opponent_finished".equals(type)) {
                long opponentUserId = json.optLong("userId", -1);

                if (listener != null) {
                    listener.onOpponentFinished(opponentUserId);
                }
                return;
            }

            if ("opponent_left".equals(type)) {
                if (listener != null) {
                    listener.onOpponentLeft();
                }
            }

        } catch (Exception e) {
            if (listener != null) {
                listener.onError(e.getMessage() == null ? "WebSocket 消息解析失败" : e.getMessage());
            }
        }
    }
}
