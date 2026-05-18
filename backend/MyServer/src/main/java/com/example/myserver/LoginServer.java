package com.example.myserver;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * 后端服务启动入口。
 *
 * 1. 使用 JDK 自带 HttpServer 开启 8081 端口；
 * 2. 为登录、注册、排行榜、商店、PK 房间等功能注册 HTTP 路由；
 * 3. 同时启动 8082 端口的 WebSocket 服务，用于联机对战实时同步；
 * 4. HTTP 负责状态型请求，WebSocket 负责实时消息转发。
 */
public class LoginServer {

    /**
     * 后端主入口。
     * 启动后会同时监听 HTTP 8081 和 WebSocket 8082。
     */
    public static void main(String[] args) throws Exception {

        HttpServer server =
                HttpServer.create(new InetSocketAddress(8081), 0);

        server.createContext("/login", new LoginHandler());
        server.createContext("/register", new RegisterHandler());

        server.createContext("/ranking/menu", new RankingMenuHandler());
        server.createContext("/score/uploadResult", new ScoreUploadResultHandler());

        server.createContext("/shop/status", new ShopStatusHandler());
        server.createContext("/shop/buy", new ShopBuyHandler());
        server.createContext("/shop/select", new ShopSelectHandler());

        server.createContext("/pk/createRoom", new PkCreateRoomHandler());
        server.createContext("/pk/joinRoom", new PkJoinRoomHandler());
        server.createContext("/pk/roomStatus", new PkRoomStatusHandler());
        server.createContext("/pk/ready", new PkReadyHandler());
        server.createContext("/pk/finish", new PkFinishHandler());

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        PkWebSocketServer pkWebSocketServer = new PkWebSocketServer(8082);
        pkWebSocketServer.start();

        System.out.println("AircraftWar HTTP server started successfully on port 8081");
        System.out.println("AircraftWar WebSocket server started successfully on port 8082");
    }
}