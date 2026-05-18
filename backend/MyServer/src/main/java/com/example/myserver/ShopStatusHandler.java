package com.example.myserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

/**
 * 商店状态同步接口处理器，对应 /shop/status。
 *
 * 功能：
 * 1. 查询用户金币、当前选中皮肤；
 * 2. 查询用户已拥有的皮肤列表；
 * 3. 确保每个用户至少拥有默认皮肤 1；
 * 4. 前端进入商店时通过该接口同步云端商店状态。
 */
public class ShopStatusHandler implements HttpHandler {

    @Override
    /**
     * 处理商店状态同步请求。
     */
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> params =
                HttpUtil.parseQuery(exchange.getRequestURI().getQuery());

        long userId = HttpUtil.getLong(params, "userId", -1);

        if (userId <= 0) {
            HttpUtil.sendJson(exchange,
                    "{\"status\":\"fail\",\"message\":\"invalid userId\"}");
            return;
        }

        String json = getShopStatus(userId);
        HttpUtil.sendJson(exchange, json);
    }

    /**
     * 查询金币、当前皮肤和已拥有皮肤列表。
     */
    private String getShopStatus(long userId) {
        String userSql =
                "SELECT coins, selected_skin_id FROM users WHERE user_id = ?";

        String skinSql =
                "SELECT skin_id FROM user_skins WHERE user_id = ? ORDER BY skin_id ASC";

        try (Connection conn = DbUtil.getConnection()) {
            ensureDefaultSkin(conn, userId);

            int coins = 0;
            int selectedSkinId = 1;

            try (PreparedStatement ps = conn.prepareStatement(userSql)) {
                ps.setLong(1, userId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return "{\"status\":\"fail\",\"message\":\"user not found\"}";
                    }

                    coins = rs.getInt("coins");
                    selectedSkinId = rs.getInt("selected_skin_id");
                }
            }

            StringBuilder owned = new StringBuilder();
            owned.append("[");

            try (PreparedStatement ps = conn.prepareStatement(skinSql)) {
                ps.setLong(1, userId);

                try (ResultSet rs = ps.executeQuery()) {
                    boolean first = true;

                    while (rs.next()) {
                        if (!first) {
                            owned.append(",");
                        }

                        owned.append(rs.getInt("skin_id"));
                        first = false;
                    }
                }
            }

            owned.append("]");

            return "{"
                    + "\"status\":\"success\","
                    + "\"coins\":" + coins + ","
                    + "\"selectedSkinId\":" + selectedSkinId + ","
                    + "\"ownedSkinIds\":" + owned
                    + "}";

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"status\":\"fail\",\"message\":\"server error\"}";
        }
    }

    /**
     * 保证每个用户至少拥有默认皮肤，避免前端商店无可用皮肤。
     */
    private void ensureDefaultSkin(Connection conn, long userId) throws Exception {
        String sql =
                "INSERT IGNORE INTO user_skins(user_id, skin_id) VALUES (?, 1)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }
}