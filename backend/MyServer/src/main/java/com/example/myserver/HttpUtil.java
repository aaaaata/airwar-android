package com.example.myserver;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 通用工具类。
 *
 * 1. 负责解析 URL query 参数；
 * 2. 统一返回 text/json/error 响应；
 * 3. 提供 JSON 字符串转义，避免用户名中包含特殊字符时破坏 JSON 格式；
 * 4. 提供参数安全读取和 difficulty/avatarId 规范化，减少各 Handler 重复代码。
 */
public class HttpUtil {

    /**
     * 解析 GET 请求中的 query 参数，例如 username=abc&password=123。
     */
    public static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();

        if (query == null || query.trim().isEmpty()) {
            return map;
        }

        String[] pairs = query.split("&");

        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);

            if (kv.length == 2) {
                String key = decode(kv[0]);
                String value = decode(kv[1]);
                map.put(key, value);
            }
        }

        return map;
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    public static void sendText(HttpExchange exchange, String text) throws IOException {
        send(exchange, 200, "text/plain;charset=utf-8", text);
    }

    /**
     * 统一发送 JSON 响应。
     */
    public static void sendJson(HttpExchange exchange, String json) throws IOException {
        send(exchange, 200, "application/json;charset=utf-8", json);
    }

    public static void sendError(HttpExchange exchange, String message) throws IOException {
        send(exchange, 500, "text/plain;charset=utf-8", message);
    }

    private static void send(HttpExchange exchange,
                             int code,
                             String contentType,
                             String body) throws IOException {

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(code, bytes.length);

        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    /**
     * 手写 JSON 字符串时必须转义特殊字符，防止用户名等字段破坏 JSON 格式。
     */
    public static String escapeJson(String s) {
        if (s == null) {
            return "";
        }

        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    public static int getInt(Map<String, String> params, String key, int defaultValue) {
        try {
            return Integer.parseInt(params.get(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static long getLong(Map<String, String> params, String key, long defaultValue) {
        try {
            return Long.parseLong(params.get(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static int normalizeAvatarId(int avatarId) {
        if (avatarId < 1 || avatarId > 6) {
            return 1;
        }
        return avatarId;
    }

    /**
     * 统一规范化难度字段，保证数据库中的 difficulty 只出现 EASY/NORMAL/HARD。
     */
    public static String normalizeDifficulty(String difficulty) {
        if (difficulty == null || difficulty.trim().isEmpty()) {
            return "NORMAL";
        }

        String d = difficulty.trim().toUpperCase();

        if ("EASY".equals(d)) {
            return "EASY";
        }

        if ("HARD".equals(d)) {
            return "HARD";
        }

        return "NORMAL";
    }
}