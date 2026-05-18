package com.example.myserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.util.Map;

/**
 * PK 房间状态查询接口处理器，对应 /pk/roomStatus。
 *
 * 功能：
 * 1. 根据 roomId 查询当前房间状态；
 * 2. 返回双方用户信息、ready 状态、分数、finished 状态和 winnerUserId；
 * 3. 前端 OnlineRoomActivity 和 PkWaitingResultActivity 通过轮询调用该接口。
 */
public class PkRoomStatusHandler implements HttpHandler {

    @Override
    /**
     * 处理房间状态查询请求。
     */
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> params =
                HttpUtil.parseQuery(exchange.getRequestURI().getQuery());

        long roomId = HttpUtil.getLong(params, "roomId", -1);

        if (roomId <= 0) {
            HttpUtil.sendJson(exchange, "{\"status\":\"fail\",\"message\":\"bad_room_id\"}");
            return;
        }

        try (Connection conn = DbUtil.getConnection()) {
            String json = PkRoomJsonUtil.buildRoomStatusJson(conn, roomId);
            HttpUtil.sendJson(exchange, json);
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendJson(exchange, "{\"status\":\"fail\",\"message\":\"server_error\"}");
        }
    }
}