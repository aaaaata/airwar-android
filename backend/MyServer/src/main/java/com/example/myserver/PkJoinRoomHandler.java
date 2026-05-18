package com.example.myserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

/**
 * 加入 PK 房间接口处理器，对应 /pk/joinRoom。
 *
 * 功能：
 * 1. 根据房间号查找 WAITING 状态房间；
 * 2. 校验不能加入自己的房间、房间不能已满；
 * 3. 将加入者写入 player2 字段；
 * 4. 房间状态由 WAITING 更新为 READY；
 * 5. 返回房间完整状态，供前端刷新等待界面。
 */
public class PkJoinRoomHandler implements HttpHandler {

    @Override
    /**
     * 处理加入房间请求。
     * 使用 FOR UPDATE 锁定房间，避免两个玩家同时加入导致超员。
     */
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> params =
                HttpUtil.parseQuery(exchange.getRequestURI().getQuery());

        String roomCode = params.get("roomCode");
        long userId = HttpUtil.getLong(params, "userId", -1);
        String username = params.get("username");

        int avatarId = HttpUtil.getInt(params, "avatarId", 1);
        avatarId = HttpUtil.normalizeAvatarId(avatarId);

        if (roomCode == null || roomCode.trim().isEmpty()
                || userId <= 0
                || username == null || username.trim().isEmpty()) {
            HttpUtil.sendJson(exchange, "{\"status\":\"fail\",\"message\":\"bad_params\"}");
            return;
        }

        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);

            long roomId;
            long player1UserId;
            String roomStatus;
            Long player2UserId;

            String query =
                    "SELECT room_id, room_status, player1_user_id, player2_user_id " +
                            "FROM pk_room WHERE room_code = ? FOR UPDATE";

            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, roomCode.trim());

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        HttpUtil.sendJson(exchange, "{\"status\":\"fail\",\"message\":\"room_not_found\"}");
                        return;
                    }

                    roomId = rs.getLong("room_id");
                    roomStatus = rs.getString("room_status");
                    player1UserId = rs.getLong("player1_user_id");

                    long tmp = rs.getLong("player2_user_id");
                    player2UserId = rs.wasNull() ? null : tmp;
                }
            }

            if (player1UserId == userId) {
                conn.rollback();
                HttpUtil.sendJson(exchange, "{\"status\":\"fail\",\"message\":\"cannot_join_self\"}");
                return;
            }

            if (player2UserId != null) {
                conn.rollback();
                HttpUtil.sendJson(exchange, "{\"status\":\"fail\",\"message\":\"room_full\"}");
                return;
            }

            if (!"WAITING".equals(roomStatus)) {
                conn.rollback();
                HttpUtil.sendJson(exchange, "{\"status\":\"fail\",\"message\":\"bad_room_status\"}");
                return;
            }

            String update =
                    "UPDATE pk_room SET " +
                            "player2_user_id = ?, " +
                            "player2_username = ?, " +
                            "player2_avatar_id = ?, " +
                            "room_status = 'READY' " +
                            "WHERE room_id = ?";

            try (PreparedStatement ps = conn.prepareStatement(update)) {
                ps.setLong(1, userId);
                ps.setString(2, username.trim());
                ps.setInt(3, avatarId);
                ps.setLong(4, roomId);
                ps.executeUpdate();
            }

            String json = PkRoomJsonUtil.buildRoomStatusJson(conn, roomId);

            conn.commit();
            HttpUtil.sendJson(exchange, json);

        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendJson(exchange, "{\"status\":\"fail\",\"message\":\"server_error\"}");
        }
    }
}