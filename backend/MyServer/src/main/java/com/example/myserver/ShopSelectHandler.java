package com.example.myserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

/**
 * 商店选择皮肤接口处理器，对应 /shop/select。
 *
 * 功能：
 * 1. 接收 userId 和 skinId；
 * 2. 校验该用户是否拥有该皮肤；
 * 3. 拥有时更新 users.selected_skin_id；
 * 4. 返回 selectedSkinId，前端据此刷新英雄机皮肤。
 */
public class ShopSelectHandler implements HttpHandler {

    @Override
    /**
     * 处理选择皮肤请求。
     */
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> params =
                HttpUtil.parseQuery(exchange.getRequestURI().getQuery());

        long userId = HttpUtil.getLong(params, "userId", -1);
        int skinId = HttpUtil.getInt(params, "skinId", 1);

        if (userId <= 0 || skinId <= 0) {
            HttpUtil.sendJson(exchange,
                    "{\"status\":\"fail\",\"message\":\"invalid params\"}");
            return;
        }

        String json = selectSkin(userId, skinId);
        HttpUtil.sendJson(exchange, json);
    }

    /**
     * 只有已拥有的皮肤才允许设置为当前皮肤。
     */
    private String selectSkin(long userId, int skinId) {
        try (Connection conn = DbUtil.getConnection()) {
            ensureDefaultSkin(conn, userId);

            if (!isSkinOwned(conn, userId, skinId)) {
                return "{\"status\":\"not_owned\"}";
            }

            String sql =
                    "UPDATE users SET selected_skin_id = ? WHERE user_id = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, skinId);
                ps.setLong(2, userId);
                ps.executeUpdate();
            }

            return "{"
                    + "\"status\":\"success\","
                    + "\"selectedSkinId\":" + skinId
                    + "}";

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
}