package com.example.myserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * PK 房间 JSON 构造工具类。
 *
 * 1. 将 pk_room 表中的房间状态统一转换为 JSON；
 * 2. createRoom、joinRoom、ready、roomStatus、finish 等接口复用同一种返回格式；
 * 3. 支持 extraFields，用于额外返回 resultStatus 等上下文字段；
 * 4. 对可空字段进行默认值处理，降低前端解析复杂度。
 */
public class PkRoomJsonUtil {

    /**
     * 构造标准房间状态 JSON。
     */
    public static String buildRoomStatusJson(Connection conn, long roomId) throws Exception {
        return buildRoomStatusJson(conn, roomId, "");
    }

    /**
     * 构造房间状态 JSON，并允许调用方插入额外字段。
     */
    public static String buildRoomStatusJson(Connection conn, long roomId, String extraFields) throws Exception {
        String sql = "SELECT * FROM pk_room WHERE room_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, roomId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return "{\"status\":\"fail\",\"message\":\"room_not_found\"}";
                }

                Long player2UserId = getNullableLong(rs, "player2_user_id");
                Long winnerUserId = getNullableLong(rs, "winner_user_id");

                String player2Username = rs.getString("player2_username");
                int player2AvatarId = getIntDefault(rs, "player2_avatar_id", 1);

                StringBuilder sb = new StringBuilder();
                sb.append("{");

                sb.append("\"status\":\"success\",");

                if (extraFields != null && !extraFields.isEmpty()) {
                    sb.append(extraFields);
                }

                sb.append("\"roomId\":").append(rs.getLong("room_id")).append(",");
                sb.append("\"roomCode\":\"").append(HttpUtil.escapeJson(rs.getString("room_code"))).append("\",");
                sb.append("\"roomStatus\":\"").append(HttpUtil.escapeJson(rs.getString("room_status"))).append("\",");

                sb.append("\"player1UserId\":").append(rs.getLong("player1_user_id")).append(",");
                sb.append("\"player1Username\":\"").append(HttpUtil.escapeJson(rs.getString("player1_username"))).append("\",");
                sb.append("\"player1AvatarId\":").append(rs.getInt("player1_avatar_id")).append(",");
                sb.append("\"player1Ready\":").append(rs.getInt("player1_ready") == 1).append(",");
                sb.append("\"player1Score\":").append(rs.getInt("player1_score")).append(",");
                sb.append("\"player1Finished\":").append(rs.getInt("player1_finished") == 1).append(",");

                sb.append("\"player2UserId\":").append(player2UserId == null ? -1 : player2UserId).append(",");
                sb.append("\"player2Username\":\"").append(HttpUtil.escapeJson(player2Username)).append("\",");
                sb.append("\"player2AvatarId\":").append(player2AvatarId).append(",");
                sb.append("\"player2Ready\":").append(rs.getInt("player2_ready") == 1).append(",");
                sb.append("\"player2Score\":").append(rs.getInt("player2_score")).append(",");
                sb.append("\"player2Finished\":").append(rs.getInt("player2_finished") == 1).append(",");

                sb.append("\"winnerUserId\":").append(winnerUserId == null ? -1 : winnerUserId);

                sb.append("}");
                return sb.toString();
            }
        }
    }

    /**
     * 读取数据库中可能为 NULL 的 long 字段。
     */
    private static Long getNullableLong(ResultSet rs, String column) throws Exception {
        long value = rs.getLong(column);
        if (rs.wasNull()) {
            return null;
        }
        return value;
    }

    private static int getIntDefault(ResultSet rs, String column, int defaultValue) throws Exception {
        int value = rs.getInt(column);
        if (rs.wasNull()) {
            return defaultValue;
        }
        return value;
    }
}