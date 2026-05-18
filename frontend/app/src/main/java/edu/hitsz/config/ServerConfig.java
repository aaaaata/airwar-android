package edu.hitsz.config;

/*
 * ServerConfig 保存 PK 联机功能用到的 HTTP 与 WebSocket 地址。
 * 它和 ApiConfig 的区别是：这里主要服务于联机房间和实时对战；ApiConfig 更多用于登录、排行榜、商店等普通接口。
 */
public class ServerConfig {

    //云端配置
    // PK 房间控制接口地址：创建房间、加入房间、准备、提交成绩等。
    public static final String HTTP_BASE_URL =  "http://120.77.207.97:8081";

    // PK 实时同步地址：用于 WebSocket 分数/生命值同步。
    public static final String WS_BASE_URL = "ws://120.77.207.97:8082/pk/ws";

    //本地测试配置
    //public static final String WS_BASE_URL = "ws://10.0.2.2:8082";

    //public static final String HTTP_BASE_URL = "http://10.0.2.2:8081";

    private ServerConfig() {
    }
}
