package com.example.myserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

/**
 * PK 准备接口处理器，对应 /pk/ready。
 *
 * 功能：
 * 1. 玩家点击 READY 后设置自己的 ready 字段；
 * 2. 检查双方是否都已准备；
 * 3. 双方都准备后，将房间状态改为 PLAYING 并写入 started_at；
 * 4. 前端轮询到 PLAYING 后进入 PkGameActivity。
 */
public class PkReadyHandler implements HttpHandler {

    @Override
    /**
     * 处理玩家准备请求。
     */
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> params =
                HttpUtil.parseQuery(exchange.getRequestURI().getQuery());

        long roomId = HttpUtil.getLong(params, "roomId", -1);
        long userId = HttpUtil.getLong(params, "userId", -1);

        if (roomId <= 0 || userId <= 0) {
            HttpUtil.sendJson(exchange, "{\"status\":\"fail\",\"message\":\"bad_params\"}");
            return;
        }

        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);

            long player1UserId;
            Long player2UserId;

            String query =
                    "SELECT player1_user_id, player2_user_id " +
                            "FROM pk_room WHERE room_id = ? FOR UPDATE";

            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setLong(1, roomId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        HttpUtil.sendJson(exchange, "{\"status\":\"fail\",\"message\":\"room_not_found\"}");
                        return;
                    }

                    player1UserId = rs.getLong("player1_user_id");

                    long tmp = rs.getLong("player2_user_id");
                    player2UserId = rs.wasNull() ? null : tmp;
                }
            }

            if (player2UserId == null) {
                conn.rollback();
                HttpUtil.sendJson(exchange, "{\"status\":\"fail\",\"message\":\"opponent_not_joined\"}");
                return;
            }

            if (userId == player1UserId) {
                updateReady(conn, roomId, "player1_ready");
            } else if (userId == player2UserId) {
                updateReady(conn, roomId, "player2_ready");
            } else {
                conn.rollback();
                HttpUtil.sendJson(exchange, "{\"status\":\"fail\",\"message\":\"not_room_player\"}");
                return;
            }

            if (checkBothReady(conn, roomId)) {
                String update =
                        "UPDATE pk_room SET room_status = 'PLAYING', started_at = NOW() " +
                                "WHERE room_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(update)) {
                    ps.setLong(1, roomId);
                    ps.executeUpdate();
                }
            }

            String json = PkRoomJsonUtil.buildRoomStatusJson(conn, roomId);

            conn.commit();
            HttpUtil.sendJson(exchange, json);

        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendJson(exchange, "{\"status\":\"fail\",\"message\":\"server_error\"}");
        }
    }

    /**
     * 根据玩家身份更新 player1_ready 或 player2_ready。
     */
    private void updateReady(Connection conn, long roomId, String column) throws Exception {
        String sql = "UPDATE pk_room SET " + column + " = 1 WHERE room_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            ps.executeUpdate();
        }
    }

    /**
     * 查询双方是否都已经准备。
     */
    private boolean checkBothReady(Connection conn, long roomId) throws Exception {
        String sql =
                "SELECT player1_ready, player2_ready " +
                        "FROM pk_room WHERE room_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, roomId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("player1_ready") == 1
                            && rs.getInt("player2_ready") == 1;
                }
            }
        }

        return false;
    }
}