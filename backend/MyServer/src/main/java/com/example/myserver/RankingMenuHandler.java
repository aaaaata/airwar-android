package com.example.myserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

/**
 * 主菜单排行榜接口处理器，对应 /ranking/menu。
 *
 * 功能：
 * 1. 接收 difficulty 和 limit；
 * 2. 从 scores 表按分数倒序、提交时间正序查询排行榜；
 * 3. 返回前端排行榜页面需要的 rankLabel、username、score、avatarId。
 */
public class RankingMenuHandler implements HttpHandler {

    private static final int DEFAULT_LIMIT = 12;
    private static final int MAX_LIMIT = 100;

    @Override
    /**
     * 处理主菜单排行榜查询请求。
     */
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> params =
                HttpUtil.parseQuery(exchange.getRequestURI().getQuery());

        String difficulty = HttpUtil.normalizeDifficulty(params.get("difficulty"));
        int limit = HttpUtil.getInt(params, "limit", DEFAULT_LIMIT);

        if (limit <= 0) {
            limit = DEFAULT_LIMIT;
        }

        if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }

        String json = getRanking(difficulty, limit);
        HttpUtil.sendJson(exchange, json);
    }

    /**
     * 根据难度查询排行榜。
     * 排序规则：分数高优先；同分时先提交的成绩靠前。
     */
    private String getRanking(String difficulty, int limit) {
        String sql =
                "SELECT username, score, avatar_id " +
                        "FROM scores " +
                        "WHERE difficulty = ? " +
                        "ORDER BY score DESC, created_at ASC, score_id ASC " +
                        "LIMIT ?";

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, difficulty);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                int rank = 1;

                while (rs.next()) {
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
                            .append("\"avatarId\":").append(avatarId)
                            .append("}");

                    rank++;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        sb.append("]");
        return sb.toString();
    }
}