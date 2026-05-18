package edu.hitsz.application;

/*
 * UserSession 是内存级的用户会话，保存当前运行期间的用户 id、用户名、头像和登录天数。
 * 它适合在多个页面之间快速读取当前用户信息；长期保存则由 SessionManager 通过 SharedPreferences 完成。
 */
public class UserSession {

    // 静态字段保存当前运行期间的登录状态；userId <= 0 表示未登录。
    private static long userId = -1;
    private static String username = "GUEST";
    private static int avatarId = 1;
    private static int loginDays = 0;

    private UserSession() {
    }

    // 登录成功后写入内存会话，供联机、主菜单等模块快速读取当前用户信息。
    public static void login(long id, String name, int avatar, int days) {
        userId = id;
        username = name == null || name.trim().isEmpty() ? "GUEST" : name.trim();
        avatarId = avatar <= 0 ? 1 : avatar;
        loginDays = days;
    }

    //后续开发游客模式接口
    // 清空内存会话，恢复游客状态。
    public static void logout() {
        userId = -1;
        username = "GUEST";
        avatarId = 1;
        loginDays = 0;
    }

    // 只要 userId 大于 0，就认为当前用户已经登录。
    public static boolean isLoggedIn() {
        return userId > 0;
    }

    public static long getUserId() {
        return userId;
    }

    public static String getUsername() {
        return username;
    }

    public static int getAvatarId() {
        return avatarId;
    }

    public static int getLoginDays() {
        return loginDays;
    }
}
