package edu.hitsz.config;

/*
 * 这个类集中保存普通 HTTP 接口的基础地址。登录、注册、排行榜上传、商店同步等请求都会间接依赖这里的 BASE_URL。
 * 做成单独配置类的好处是：本地测试地址和云端地址可以快速切换，不需要在每个 Activity 里逐个修改。
 */
public class ApiConfig {

    //云端连接配置
    // 服务器基础地址：切换云端/本地调试时只需要改这一处。
    public static final String BASE_URL = "http://120.77.207.97:8081";
    //自己部署云端时，BASE_URL = "http://云服务器公网IP:8081"

    //本地连接配置
    //public static final String BASE_URL = "http://10.0.2.2:8081";
    //自己进行本地测试时，BASE_URL = "http://本地局域网IP:8081"
}
