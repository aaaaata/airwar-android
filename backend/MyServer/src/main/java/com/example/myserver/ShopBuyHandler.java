package com.example.myserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

/**
 * 商店购买皮肤接口处理器，对应 /shop/buy。
 *
 * 功能：
 * 1. 接收 userId、skinId、price；
 * 2. 检查玩家是否已经拥有该皮肤；
 * 3. 检查金币是否足够；
 * 4. 金币足够时扣除金币并写入 user_skins；
 * 5. 返回购买后的金币数量和 owned 状态。
 */
public class ShopBuyHandler implements HttpHandler {

    @Override
    /**
     * 处理购买皮肤请求。
     */
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> params =
                HttpUtil.parseQuery(exchange.getRequestURI().getQuery());

        long userId = HttpUtil.getLong(params, "userId", -1);
        int skinId = HttpUtil.getInt(params, "skinId", 1);
        int price = HttpUtil.getInt(params, "price", 0);

        if (userId <= 0 || skinId <= 0 || price < 0) {
            HttpUtil.sendJson(exchange,
                    "{\"status\":\"fail\",\"message\":\"invalid params\"}");
            return;
        }

        String json = buySkin(userId, skinId, price);
        HttpUtil.sendJson(exchange, json);
    }

    /**
     * 使用事务完成购买，确保扣金币和添加皮肤要么同时成功，要么同时失败。
     */
    private String buySkin(long userId, int skinId, int price) {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);

            try {
                ensureDefaultSkin(conn, userId);

                if (isSkinOwned(conn, userId, skinId)) {
                    int coins = getCoins(conn, userId);
                    conn.commit();

                    return "{"
                            + "\"status\":\"success\","
                            + "\"coins\":" + coins + ","
                            + "\"owned\":true"
                            + "}";
                }

                int coins = getCoins(conn, userId);

                if (coins < price) {
                    conn.rollback();

                    return "{"
                            + "\"status\":\"not_enough\","
                            + "\"coins\":" + coins
                            + "}";
                }

                String updateSql =
                        "UPDATE users SET coins = coins - ? WHERE user_id = ? AND coins >= ?";

                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, price);
                    ps.setLong(2, userId);
                    ps.setInt(3, price);

                    int updated = ps.executeUpdate();
                    if (updated <= 0) {
                        conn.rollback();
                        return "{\"status\":\"not_enough\"}";
                    }
                }

                String insertSkinSql =
                        "INSERT IGNORE INTO user_skins(user_id, skin_id) VALUES (?, ?)";

                try (PreparedStatement ps = conn.prepareStatement(insertSkinSql)) {
                    ps.setLong(1, userId);
                    ps.setInt(2, skinId);
                    ps.executeUpdate();
                }

                int newCoins = getCoins(conn, userId);

                conn.commit();

                return "{"
                        + "\"status\":\"success\","
                        + "\"coins\":" + newCoins + ","
                        + "\"owned\":true"
                        + "}";

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"status\":\"fail\",\"message\":\"server error\"}";
        }
    }

    private void ensureDefaultSkin(Connection conn, long userId) throws Exception {
        String sql =
                "INSERT IGNORE INTO user_skins(user_id, skin_id) VALUES (?, 1)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    /**
     * 查询用户是否已经拥有某个皮肤。
     */
    private boolean isSkinOwned(Connection conn, long userId, int skinId) throws Exception {
        String sql =
                "SELECT id FROM user_skins WHERE user_id = ? AND skin_id = ? LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, skinId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int getCoins(Connection conn, long userId) throws Exception {
        String sql = "SELECT coins FROM users WHERE user_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("coins");
                }
            }
        }

        return 0;
    }
}