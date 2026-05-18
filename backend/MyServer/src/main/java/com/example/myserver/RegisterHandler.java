package com.example.myserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

/**
 * 注册接口处理器，对应前端 LoginActivity 中的 /register 请求。
 *
 * 功能：
 * 1. 校验用户名和密码；
 * 2. 检查用户名是否已存在；
 * 3. 新建用户并初始化金币和默认皮肤；
 * 4. 注册成功后返回 success，重名返回 exist。
 */
public class RegisterHandler implements HttpHandler {

    @Override
    /**
     * 处理一次注册请求，读取 username/password/avatarId。
     */
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("收到注册请求");

        Map<String, String> params =
                HttpUtil.parseQuery(exchange.getRequestURI().getQuery());

        String username = params.get("username");
        String password = params.get("password");
        int avatarId = HttpUtil.getInt(params, "avatarId", 1);
        avatarId = HttpUtil.normalizeAvatarId(avatarId);

        if (username == null || username.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            HttpUtil.sendText(exchange, "fail");
            return;
        }

        String result = register(username.trim(), password.trim(), avatarId);
        HttpUtil.sendText(exchange, result);
    }

    /**
     * 使用事务完成注册：
     * 1. 检查用户名是否重复；
     * 2. 插入 users；
     * 3. 给新用户插入默认皮肤。
     */
    private String register(String username, String password, int avatarId) {
        String checkSql = "SELECT user_id FROM users WHERE username = ?";

        String insertUserSql =
                "INSERT INTO users(username, password, avatar_id, coins, selected_skin_id) " +
                        "VALUES (?, ?, ?, 0, 1)";

        String insertDefaultSkinSql =
                "INSERT IGNORE INTO user_skins(user_id, skin_id) VALUES (?, 1)";

        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);

            try {
                try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                    check.setString(1, username);

                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next()) {
                            conn.rollback();
                            return "exist";
                        }
                    }
                }

                long newUserId = -1;

                try (PreparedStatement insertUser =
                             conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {

                    insertUser.setString(1, username);
                    insertUser.setString(2, password);
                    insertUser.setInt(3, avatarId);
                    insertUser.executeUpdate();

                    try (ResultSet keys = insertUser.getGeneratedKeys()) {
                        if (keys.next()) {
                            newUserId = keys.getLong(1);
                        }
                    }
                }

                if (newUserId <= 0) {
                    conn.rollback();
                    return "fail";
                }

                try (PreparedStatement insertSkin = conn.prepareStatement(insertDefaultSkinSql)) {
                    insertSkin.setLong(1, newUserId);
                    insertSkin.executeUpdate();
                }

                conn.commit();
                return "success";

            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                return "fail";
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "fail";
        }
    }
}