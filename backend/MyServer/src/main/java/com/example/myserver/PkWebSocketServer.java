package com.example.myserver;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PK WebSocket 实时同步服务器。
 *
 * 1. 运行在 8082 端口；
 * 2. 每个连接通过 roomId 和 userId 绑定玩家身份；
 * 3. 使用 roomSockets 保存每个房间内的玩家 WebSocket；
 * 4. 收到某玩家的 score/life/state 后转发给同房间对手；
 * 5. 收到 finished/关闭连接时通知对手。
 */
public class PkWebSocketServer extends WebSocketServer {

    private final Map<Long, Map<Long, WebSocket>> roomSockets = new ConcurrentHashMap<>();

    public PkWebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    /**
     * 新 WebSocket 连接建立时，从 URL 参数解析 roomId/userId 并登记到 roomSockets。
     */
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        try {
            String resource = handshake.getResourceDescriptor();
            Map<String, String> params = parseQueryFromResource(resource);

            long roomId = Long.parseLong(params.getOrDefault("roomId", "-1"));
            long userId = Long.parseLong(params.getOrDefault("userId", "-1"));

            if (roomId <= 0 || userId <= 0) {
                conn.close(1008, "bad params");
                return;
            }

            ClientInfo info = new ClientInfo(roomId, userId);
            conn.setAttachment(info);

            roomSockets
                    .computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                    .put(userId, conn);

            System.out.println("WebSocket open: roomId=" + roomId + ", userId=" + userId);

        } catch (Exception e) {
            e.printStackTrace();
            conn.close(1011, "server error");
        }
    }

    @Override
    /**
     * 收到客户端实时消息后，根据 type 决定转发分数/生命值或结束通知。
     */
    public void onMessage(WebSocket conn, String message) {
        try {
            ClientInfo info = conn.getAttachment();
            if (info == null) {
                return;
            }

            JSONObject obj = new JSONObject(message);
            String type = obj.optString("type", "");

            if ("state".equals(type)) {
                int score = obj.optInt("score", 0);
                int life = obj.optInt("life", -1);

                JSONObject forward = new JSONObject();
                forward.put("type", "opponent_state");
                forward.put("userId", info.userId);
                forward.put("score", Math.max(score, 0));
                forward.put("life", Math.max(life, 0));

                sendToOpponent(info.roomId, info.userId, forward.toString());
                return;
            }

            // 兼容旧客户端：旧客户端只发送 score
            if ("score".equals(type)) {
                int score = obj.optInt("score", 0);

                JSONObject forward = new JSONObject();
                forward.put("type", "opponent_state");
                forward.put("userId", info.userId);
                forward.put("score", Math.max(score, 0));
                forward.put("life", -1);

                sendToOpponent(info.roomId, info.userId, forward.toString());
                return;
            }

            if ("finished".equals(type)) {
                JSONObject forward = new JSONObject();
                forward.put("type", "opponent_finished");
                forward.put("userId", info.userId);

                sendToOpponent(info.roomId, info.userId, forward.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    /**
     * 客户端断开时从房间连接表移除，并通知同房间对手。
     */
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        ClientInfo info = conn.getAttachment();

        if (info != null) {
            Map<Long, WebSocket> room = roomSockets.get(info.roomId);

            if (room != null) {
                room.remove(info.userId);

                try {
                    JSONObject msg = new JSONObject();
                    msg.put("type", "opponent_left");
                    msg.put("userId", info.userId);

                    sendToOpponent(info.roomId, info.userId, msg.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (room.isEmpty()) {
                    roomSockets.remove(info.roomId);
                }
            }

            System.out.println("WebSocket close: roomId=" + info.roomId
                    + ", userId=" + info.userId
                    + ", reason=" + reason);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("PkWebSocketServer started on port " + getPort());
    }

    /**
     * 将消息只发送给同一房间内除自己以外的连接。
     */
    private void sendToOpponent(long roomId, long selfUserId, String message) {
        Map<Long, WebSocket> room = roomSockets.get(roomId);

        if (room == null) {
            return;
        }

        for (Map.Entry<Long, WebSocket> entry : room.entrySet()) {
            long userId = entry.getKey();
            WebSocket socket = entry.getValue();

            if (userId != selfUserId && socket != null && socket.isOpen()) {
                socket.send(message);
            }
        }
    }

    /**
     * 从 WebSocket 连接地址中解析 query 参数。
     */
    private Map<String, String> parseQueryFromResource(String resource) {
        Map<String, String> map = new ConcurrentHashMap<>();

        if (resource == null) {
            return map;
        }

        int questionIndex = resource.indexOf('?');
        if (questionIndex < 0 || questionIndex >= resource.length() - 1) {
            return map;
        }

        String query = resource.substring(questionIndex + 1);
        String[] pairs = query.split("&");

        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);

            if (kv.length == 2) {
                String key = decode(kv[0]);
                String value = decode(kv[1]);
                map.put(key, value);
            }
        }

        return map;
    }

    private String decode(String text) {
        try {
            return URLDecoder.decode(text, "UTF-8");
        } catch (Exception e) {
            return text;
        }
    }

    private static class ClientInfo {
        final long roomId;
        final long userId;

        ClientInfo(long roomId, long userId) {
            this.roomId = roomId;
            this.userId = userId;
        }
    }
}