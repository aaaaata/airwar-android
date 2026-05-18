package com.example.myserver;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * PK 房间清理工具类。
 *
 * 1. 清理已经结束较久的 FINISHED 房间；
 * 2. 清理长时间无人加入或无人开始的 WAITING/READY 房间；
 * 3. 清理异常长时间 PLAYING 的房间；
 * 4. 避免数据库中 pk_room 表长期堆积无效记录。
 */
public class PkRoomCleanupUtil {

    private PkRoomCleanupUtil() {
    }

    /**
     * 清理过期房间记录。
     */
    public static void cleanupExpiredRooms(Connection conn) {
        String sql =
                "DELETE FROM pk_room " +
                        "WHERE " +
                        "  (room_status = 'FINISHED' " +
                        "   AND ended_at IS NOT NULL " +
                        "   AND ended_at < DATE_SUB(NOW(), INTERVAL 1 HOUR)) " +
                        "OR " +
                        "  (room_status IN ('WAITING', 'READY') " +
                        "   AND created_at < DATE_SUB(NOW(), INTERVAL 2 HOUR)) " +
                        "OR " +
                        "  (room_status = 'PLAYING' " +
                        "   AND started_at IS NOT NULL " +
                        "   AND started_at < DATE_SUB(NOW(), INTERVAL 6 HOUR))";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int deleted = ps.executeUpdate();

            if (deleted > 0) {
                System.out.println("清理过期 PK 房间记录：" + deleted + " 条");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}