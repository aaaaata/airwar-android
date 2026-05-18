package com.example.myserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Map;

/**
 * PK 对战结束接口处理器，对应 /pk/finish。
 *
 * 功能：
 * 1. 玩家游戏结束后提交最终分数；
 * 2. 使用事务和 SELECT ... FOR UPDATE 锁定房间，避免双方同时提交造成数据覆盖；
 * 3. 记录 player1/player2 的最终分数和 finished 状态；
 * 4. 双方都结束后计算胜者并将房间状态改为 FINISHED；
 * 5. 返回 waiting_result 或 final_result，指导前端进入等待页或结果页。
 */
public class PkFinishHandler implements HttpHandler {

    @Override
    /**
     * 处理玩家提交最终分数请求。
     */
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> params =
                HttpUtil.parseQuery(exchange.getRequestURI().getQuery());

        long roomId = HttpUtil.getLong(params, "roomId", -1);
        long userId = HttpUtil.getLong(params, "userId", -1);
        int score = HttpUtil.getInt(params, "score", 0);

        if (score < 0) {
            score = 0;
        }

        if (roomId <= 0 || userId <= 0) {
            HttpUtil.sendJson(exchange, "{\"status\":\"fail\",\"message\":\"bad_params\"}");
            return;
        }

        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);

            RoomSnapshot room = loadRoomForUpdate(conn, roomId);

            if (room == null) {
                conn.rollback();
                HttpUtil.sendJson(exchange, "{\"status\":\"fail\",\"message\":\"room_not_found\"}");
                return;
            }

            /*
             * 关键保护 1：
             * 如果房间已经 FINISHED，不允许再次覆盖 player1_score / player2_score / winner_user_id。
             * 直接返回最终结果即可。
             */
            if ("FINISHED".equals(room.roomStatus)) {
                String extra = "\"resultStatus\":\"final_result\",";
                String json = PkRoomJsonUtil.buildRoomStatusJson(conn, roomId, extra);

                conn.commit();
                HttpUtil.sendJson(exchange, json);
                return;
            }

            /*
             * 判断提交结果的用户属于 player1 还是 player2。
             * 关键保护 2：
             * 如果该玩家已经 finished，不再覆盖他第一次提交的分数。
             */
            if (userId == room.player1UserId) {
                if (!room.player1Finished) {
                    updatePlayerFinish(
                            conn,
                            roomId,
                            "player1_score",
                            "player1_finished",
                            score
                    );
                }

            } else if (room.player2UserId != null && userId == room.player2UserId) {
                if (!room.player2Finished) {
                    updatePlayerFinish(
                            conn,
                            roomId,
                            "player2_score",
                            "player2_finished",
                            score
                    );
                }

            } else {
                conn.rollback();
                HttpUtil.sendJson(exchange, "{\"status\":\"fail\",\"message\":\"not_room_player\"}");
                return;
            }

            RoomSnapshot updated = loadRoomForUpdate(conn, roomId);

            if (updated == null) {
                conn.rollback();
                HttpUtil.sendJson(exchange, "{\"status\":\"fail\",\"message\":\"room_not_found\"}");
                return;
            }

            String resultStatus = "waiting_result";

            /*
             * 双方都结束后，计算胜者并把房间状态改成 FINISHED。
             */
            if (updated.player1Finished && updated.player2Finished) {
                Long winnerUserId = calculateWinner(updated);

                String finishSql =
                        "UPDATE pk_room SET " +
                                "room_status = 'FINISHED', " +
                                "winner_user_id = ?, " +
                                "ended_at = NOW() " +
                                "WHERE room_id = ?";

                try (PreparedStatement ps = conn.prepareStatement(finishSql)) {
                    if (winnerUserId == null) {
                        ps.setNull(1, Types.BIGINT);
                    } else {
                        ps.setLong(1, winnerUserId);
                    }

                    ps.setLong(2, roomId);
                    ps.executeUpdate();
                }

                resultStatus = "final_result";
            }

            String extra = "\"resultStatus\":\"" + resultStatus + "\",";
            String json = PkRoomJsonUtil.buildRoomStatusJson(conn, roomId, extra);

            conn.commit();
            HttpUtil.sendJson(exchange, json);

        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendJson(exchange, "{\"status\":\"fail\",\"message\":\"server_error\"}");
        }
    }

    /**
     * 写入某一方的最终分数，并标记该玩家已经结束。
     */
    private void updatePlayerFinish(Connection conn,
                                    long roomId,
                                    String scoreColumn,
                                    String finishedColumn,
                                    int score) throws Exception {

        String sql =
                "UPDATE pk_room SET " +
                        scoreColumn + " = ?, " +
                        finishedColumn + " = 1 " +
                        "WHERE room_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, score);
            ps.setLong(2, roomId);
            ps.executeUpdate();
        }
    }

    /**
     * 根据双方最终分数计算胜者；平局返回 null。
     */
    private Long calculateWinner(RoomSnapshot room) {
        if (room.player1Score > room.player2Score) {
            return room.player1UserId;
        }

        if (room.player2Score > room.player1Score) {
            return room.player2UserId;
        }

        // 平局时 winner_user_id 置空，客户端显示 DRAW
        return null;
    }

    /**
     * 查询并锁定房间快照，保证一次结算过程中数据不会被其他请求并发修改。
     */
    private RoomSnapshot loadRoomForUpdate(Connection conn, long roomId) throws Exception {
        String sql = "SELECT * FROM pk_room WHERE room_id = ? FOR UPDATE";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, roomId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                RoomSnapshot room = new RoomSnapshot();

                room.roomId = rs.getLong("room_id");
                room.roomStatus = rs.getString("room_status");

                room.player1UserId = rs.getLong("player1_user_id");

                long p2 = rs.getLong("player2_user_id");
                room.player2UserId = rs.wasNull() ? null : p2;

                room.player1Score = rs.getInt("player1_score");
                room.player2Score = rs.getInt("player2_score");

                room.player1Finished = rs.getInt("player1_finished") == 1;
                room.player2Finished = rs.getInt("player2_finished") == 1;

                long winner = rs.getLong("winner_user_id");
                room.winnerUserId = rs.wasNull() ? null : winner;

                return room;
            }
        }
    }

    private static class RoomSnapshot {
        long roomId;
        String roomStatus;

        long player1UserId;
        Long player2UserId;

        int player1Score;
        int player2Score;

        boolean player1Finished;
        boolean player2Finished;

        Long winnerUserId;
    }
}