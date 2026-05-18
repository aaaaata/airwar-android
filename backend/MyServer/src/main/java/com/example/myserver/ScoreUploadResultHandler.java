package com.example.myserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Map;

/**
 * 单机游戏成绩上传与结算排行榜接口，对应 /score/uploadResult。
 *
 * 功能：
 * 1. 接收用户信息、难度、分数和本局金币；
 * 2. 将成绩写入 scores 表；
 * 3. 登录用户会把本局金币累加到 users.coins；
 * 4. 返回前 10 名排行榜，如果当前成绩未进入前 10，也额外返回当前玩家排名；
 * 5. 每个难度只保留前 100 条有效成绩，控制数据库规模。
 */
public class ScoreUploadResultHandler implements HttpHandler {

    private static final int RESULT_TOP_COUNT = 10;
    private static final int MAX_SAVED_COUNT_PER_DIFFICULTY = 100;

    @Override
    /**
     * 处理游戏结束后的成绩上传请求。
     */
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> params =
                HttpUtil.parseQuery(exchange.getRequestURI().getQuery());

        long userId = HttpUtil.getLong(params, "userId", -1);

        String username = params.get("username");
        if (username == null || username.trim().isEmpty()) {
            username = "GUEST";
        }

        int avatarId = HttpUtil.getInt(params, "avatarId", 1);
        avatarId = HttpUtil.normalizeAvatarId(avatarId);

        String difficulty = HttpUtil.normalizeDifficulty(params.get("difficulty"));

        int score = HttpUtil.getInt(params, "score", 0);
        if (score < 0) {
            score = 0;
        }

        int coinsGot = HttpUtil.getInt(params, "coinsGot", 0);
        if (coinsGot < 0) {
            coinsGot = 0;
        }

        String json = uploadAndGetRanking(
                userId,
                username.trim(),
                avatarId,
                difficulty,
                score,
                coinsGot
        );

        HttpUtil.sendJson(exchange, json);
    }

    /**
     * 写入成绩、增加金币、构造结算排行榜，并清理多余历史成绩。
     */
    private String uploadAndGetRanking(long userId,
                                       String username,
                                       int avatarId,
                                       String difficulty,
                                       int score,
                                       int coinsGot) {
        try (Connection conn = DbUtil.getConnection()) {
            long currentScoreId = insertScore(conn, userId, username, avatarId, difficulty, score);

            if (userId > 0 && coinsGot > 0) {
                addCoinsToUser(conn, userId, coinsGot);
            }

            String json = buildResultRankingJson(
                    conn,
                    difficulty,
                    currentScoreId,
                    username,
                    avatarId,
                    score
            );

            cleanupScoresBeyondTop100(conn, difficulty);

            return json;

        } catch (Exception e) {
            e.printStackTrace();

            return "[{"
                    + "\"rankLabel\":\"-\","
                    + "\"username\":\"" + HttpUtil.escapeJson(username) + "\","
                    + "\"score\":" + score + ","
                    + "\"avatarId\":" + avatarId + ","
                    + "\"current\":true"
                    + "}]";
        }
    }

    /**
     * 将本局成绩写入 scores 表。
     */
    private long insertScore(Connection conn,
                             long userId,
                             String username,
                             int avatarId,
                             String difficulty,
                             int score) throws Exception {

        String sql =
                "INSERT INTO scores(user_id, username, avatar_id, difficulty, score) " +
                        "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps =
                     conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (userId <= 0) {
                ps.setNull(1, Types.BIGINT);
            } else {
                ps.setLong(1, userId);
            }

            ps.setString(2, username);
            ps.setInt(3, avatarId);
            ps.setString(4, difficulty);
            ps.setInt(5, score);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        return -1;
    }

    /**
     * 登录用户获得的金币会同步累加到 users 表。
     */
    private void addCoinsToUser(Connection conn, long userId, int coinsGot) throws Exception {
        String sql =
                "UPDATE users SET coins = coins + ? WHERE user_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, coinsGot);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    private String buildResultRankingJson(Connection conn,
                                          String difficulty,
                                          long currentScoreId,
                                          String fallbackUsername,
                                          int fallbackAvatarId,
                                          int fallbackScore) throws Exception {

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        boolean currentAlreadyInTop = appendTopRecords(conn, difficulty, currentScoreId, sb);

        if (!currentAlreadyInTop) {
            if (currentScoreId > 0) {
                appendCurrentScore(conn, difficulty, currentScoreId, sb);
            } else {
                appendFallbackCurrentScore(sb, fallbackUsername, fallbackAvatarId, fallbackScore);
            }
        }

        sb.append("]");
        return sb.toString();
    }

    /**
     * 添加当前难度前 10 名记录，同时判断当前成绩是否已经在前 10。
     */
    private boolean appendTopRecords(Connection conn,
                                     String difficulty,
                                     long currentScoreId,
                                     StringBuilder sb) throws Exception {

        String sql =
                "SELECT score_id, username, score, avatar_id " +
                        "FROM scores " +
                        "WHERE difficulty = ? " +
                        "ORDER BY score DESC, created_at ASC, score_id ASC " +
                        "LIMIT ?";

        boolean currentAlreadyInTop = false;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, difficulty);
            ps.setInt(2, RESULT_TOP_COUNT);

            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                boolean first = true;

                while (rs.next()) {
                    long scoreId = rs.getLong("score_id");
                    boolean current = scoreId == currentScoreId;

                    if (current) {
                        currentAlreadyInTop = true;
                    }

                    if (!first) {
                        sb.append(",");
                    }

                    first = false;

                    String username = rs.getString("username");
                    int score = rs.getInt("score");
                    int avatarId = rs.getInt("avatar_id");

                    sb.append("{")
                            .append("\"rankLabel\":\"").append(rank).append("\",")
                            .append("\"username\":\"").append(HttpUtil.escapeJson(username)).append("\",")
                            .append("\"score\":").append(score).append(",")
                            .append("\"avatarId\":").append(avatarId).append(",")
                            .append("\"current\":").append(current)
                            .append("}");

                    rank++;
                }
            }
        }

        return currentAlreadyInTop;
    }

    private void appendCurrentScore(Connection conn,
                                    String difficulty,
                                    long currentScoreId,
                                    StringBuilder sb) throws Exception {

        CurrentScore current = queryCurrentScore(conn, currentScoreId);
        if (current == null) {
            return;
        }

        int rank = queryRank(conn, difficulty, current);

        if (sb.length() > 1) {
            sb.append(",");
        }

        sb.append("{")
                .append("\"rankLabel\":\"").append(rank).append("\",")
                .append("\"username\":\"").append(HttpUtil.escapeJson(current.username)).append("\",")
                .append("\"score\":").append(current.score).append(",")
                .append("\"avatarId\":").append(current.avatarId).append(",")
                .append("\"current\":true")
                .append("}");
    }

    private CurrentScore queryCurrentScore(Connection conn, long currentScoreId) throws Exception {
        String sql =
                "SELECT score_id, username, score, avatar_id, created_at " +
                        "FROM scores " +
                        "WHERE score_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, currentScoreId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                CurrentScore current = new CurrentScore();
                current.scoreId = rs.getLong("score_id");
                current.username = rs.getString("username");
                current.score = rs.getInt("score");
                current.avatarId = rs.getInt("avatar_id");
                current.createdAt = rs.getTimestamp("created_at");
                return current;
            }
        }
    }

    /**
     * 计算当前成绩的真实排名。
     * 与排行榜排序规则保持一致：更高分数优先，同分时更早提交优先。
     */
    private int queryRank(Connection conn, String difficulty, CurrentScore current) throws Exception {
        String sql =
                "SELECT COUNT(*) + 1 AS rank_value " +
                        "FROM scores " +
                        "WHERE difficulty = ? " +
                        "AND (" +
                        "    score > ? " +
                        "    OR (" +
                        "        score = ? " +
                        "        AND (" +
                        "            created_at < ? " +
                        "            OR (created_at = ? AND score_id < ?)" +
                        "        )" +
                        "    )" +
                        ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, difficulty);
            ps.setInt(2, current.score);
            ps.setInt(3, current.score);
            ps.setTimestamp(4, current.createdAt);
            ps.setTimestamp(5, current.createdAt);
            ps.setLong(6, current.scoreId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("rank_value");
                }
            }
        }

        return 1;
    }

    /**
     * 每个难度最多保留前 100 条，防止 scores 表无限增长。
     */
    private void cleanupScoresBeyondTop100(Connection conn, String difficulty) throws Exception {
        String sql =
                "DELETE FROM scores " +
                        "WHERE difficulty = ? " +
                        "AND score_id NOT IN (" +
                        "    SELECT score_id FROM (" +
                        "        SELECT score_id " +
                        "        FROM scores " +
                        "        WHERE difficulty = ? " +
                        "        ORDER BY score DESC, created_at ASC, score_id ASC " +
                        "        LIMIT ?" +
                        "    ) AS keep_rows" +
                        ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, difficulty);
            ps.setString(2, difficulty);
            ps.setInt(3, MAX_SAVED_COUNT_PER_DIFFICULTY);
            ps.executeUpdate();
        }
    }

    private void appendFallbackCurrentScore(StringBuilder sb,
                                            String username,
                                            int avatarId,
                                            int score) {

        if (sb.length() > 1) {
            sb.append(",");
        }

        sb.append("{")
                .append("\"rankLabel\":\"-\",")
                .append("\"username\":\"").append(HttpUtil.escapeJson(username)).append("\",")
                .append("\"score\":").append(score).append(",")
                .append("\"avatarId\":").append(avatarId).append(",")
                .append("\"current\":true")
                .append("}");
    }

    private static class CurrentScore {
        long scoreId;
        String username;
        int score;
        int avatarId;
        Timestamp createdAt;
    }
}