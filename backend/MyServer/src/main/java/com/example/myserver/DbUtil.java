package com.example.myserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 数据库连接工具类。
 *
 * 重点：
 * 1. 后端所有 Handler 都通过 DbUtil.getConnection() 获取 MySQL 连接；
 * 2. 数据库 URL、账号、密码优先从环境变量读取，方便云服务器部署；
 * 3. 如果没有环境变量，则使用本地默认配置，便于开发调试；
 * 4. static 代码块提前加载 MySQL 驱动，避免运行时找不到驱动。
 */
public class DbUtil {

    private static final String URL = getEnv(
            "DB_URL",
            "jdbc:mysql://localhost:3306/airwar"
                    + "?useUnicode=true"
                    + "&characterEncoding=utf8"
                    + "&serverTimezone=Asia/Shanghai"
                    + "&useSSL=false"
                    + "&allowPublicKeyRetrieval=true"
    );

    private static final String USER = getEnv("DB_USER", "root");

    // 这里填你本地 MySQL 密码，方便本地继续运行
    private static final String PASSWORD = getEnv("DB_PASSWORD", "Wyq815336");

    /*
     * 类加载时注册 MySQL 驱动。
     * 如果驱动依赖没有正确引入，这里会直接抛出异常，便于尽早发现问题。
     */
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            throw new RuntimeException("MySQL 驱动加载失败", e);
        }
    }

    /**
     * 获取一个新的数据库连接。
     * 每个 Handler 使用 try-with-resources 自动关闭连接，避免连接泄漏。
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * 从环境变量读取配置。
     * 云服务器部署时可通过 DB_URL / DB_USER / DB_PASSWORD 覆盖本地默认值。
     */
    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }
}