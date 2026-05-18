package com.example.myserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.Random;

/**
 * 创建 PK 房间接口处理器，对应 /pk/createRoom。
 *
 * 功能：
 * 1. 接收创建者的 userId、username、avatarId；
 * 2. 清理过期房间；
 * 3. 生成六位房间号；
 * 4. 在 pk_room 表中插入 WAITING 状态的新房间；
 * 5. 返回完整房间状态 JSON 给前端。
 */
public class PkCreateRoomHandler implements HttpHandler {

    @Override
    /**
     * 处理创建房间请求。
     */
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> params =
                HttpUtil.parseQuery(exchange.getRequestURI().getQuery());

        long userId = HttpUtil.getLong(params, "userId", -1);
        String username = params.get("username");

        int avatarId = HttpUtil.getInt(params, "avatarId", 1);
        avatarId = HttpUtil.normalizeAvatarId(avatarId);

        if (userId <= 0 || username == null || username.trim().isEmpty()) {
            HttpUtil.sendJson(exchange, "{\"status\":\"fail\",\"message\":\"bad_user\"}");
            return;
        }

        try (Connection conn = DbUtil.getConnection()) {
            PkRoomCleanupUtil.cleanupExpiredRooms(conn);

            String roomCode = generateRoomCode(conn);

            String sql =
                    "INSERT INTO pk_room(room_code, room_status, " +
                            "player1_user_id, player1_username, player1_avatar_id) " +
                            "VALUES (?, 'WAITING', ?, ?, ?)";

            try (PreparedStatement ps =
                         conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, roomCode);
                ps.setLong(2, userId);
                ps.setString(3, username.trim());
                ps.setInt(4, avatarId);

                ps.executeUpdate();

                long roomId = -1;

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        roomId = rs.getLong(1);
                    }
                }

                String json = PkRoomJsonUtil.buildRoomStatusJson(conn, roomId);
                HttpUtil.sendJson(exchange, json);
            }

        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendJson(exchange, "{\"status\":\"fail\",\"message\":\"server_error\"}");
        }
    }

    /**
     * 随机生成六位房间号，并检查数据库中是否已存在。
     */
    private String generateRoomCode(Connection conn) throws Exception {
        Random random = new Random();

        for (int i = 0; i < 100; i++) {
            int code = 100000 + random.nextInt(900000);
            String roomCode = String.valueOf(code);

            String sql = "SELECT room_id FROM pk_room WHERE room_code = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, roomCode);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return roomCode;
                    }
                }
            }
        }

        throw new RuntimeException("生成房间号失败");
    }
}