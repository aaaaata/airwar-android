package com.example.myserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

/**
 * 登录接口处理器，对应前端 LoginActivity 中的 /login 请求。
 *
 * 功能：
 * 1. 读取 username/password；
 * 2. 查询 users 表判断账号密码是否匹配；
 * 3. 登录成功后返回 userId、username、avatarId、loginDays；
 * 4. 同时更新 last_login_date，记录最近登录日期。
 */
public class LoginHandler implements HttpHandler {

    @Override
    /**
     * 处理一次登录 HTTP 请求。
     */
    public void handle(HttpExchange exchange) throws IOException {

        Map<String, String> params =
                HttpUtil.parseQuery(exchange.getRequestURI().getQuery());

        String username = params.get("username");
        String password = params.get("password");

        System.out.println("收到登录请求 username = " + username);

        if (username == null || username.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            HttpUtil.sendJson(exchange, "{\"status\":\"fail\"}");
            return;
        }

        String result = checkLogin(username.trim(), password.trim());

        System.out.println("login result = " + result);

        HttpUtil.sendJson(exchange, result);
    }

    /**
     * 到 users 表中校验用户名和密码，并组装登录成功 JSON。
     */
    private String checkLogin(String username, String password) {
        String sql =
                "SELECT user_id, username, avatar_id, " +
                        "DATEDIFF(CURDATE(), DATE(created_at)) + 1 AS login_days " +
                        "FROM users " +
                        "WHERE username = ? AND password = ?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long userId = rs.getLong("user_id");
                    String dbUsername = rs.getString("username");
                    int avatarId = rs.getInt("avatar_id");
                    int loginDays = rs.getInt("login_days");

                    updateLastLoginDate(conn, userId);

                    return "{"
                            + "\"status\":\"success\","
                            + "\"userId\":" + userId + ","
                            + "\"username\":\"" + HttpUtil.escapeJson(dbUsername) + "\","
                            + "\"avatarId\":" + avatarId + ","
                            + "\"loginDays\":" + loginDays
                            + "}";
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "{\"status\":\"fail\"}";
    }

    /**
     * 登录成功后更新最后登录日期。
     */
    private void updateLastLoginDate(Connection conn, long userId) {
        String sql = "UPDATE users SET last_login_date = CURDATE() WHERE user_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}