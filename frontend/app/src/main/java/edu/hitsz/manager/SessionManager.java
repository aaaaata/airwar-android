package edu.hitsz.manager;

import android.content.Context;
import android.content.SharedPreferences;

/*
 * SessionManager 使用 SharedPreferences 持久化登录信息，包括登录状态、用户 id、用户名、头像和登录天数。
 * 它解决的是“关闭页面或重启后仍能读取账号信息”的问题，与只保存在内存中的 UserSession 相互补充。
 */
public class SessionManager {

    // SharedPreferences 文件名，专门保存账号会话信息。
    private static final String SP_NAME = "aircraft_war_session";

    // 登录成功后持久化用户信息，之后主菜单/排行榜/商店都从这里读取。
    public static void saveUser(Context context,
                                int userId,
                                String username,
                                int avatarId,
                                int loginDays) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit()
                .putBoolean("logged_in", true)
                .putInt("user_id", userId)
                .putString("username", username)
                .putInt("avatar_id", avatarId)
                .putInt("login_days", loginDays)
                .apply();
    }

    // 判断本地是否有有效登录状态。
    public static boolean isLoggedIn(Context context) {
        return context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .getBoolean("logged_in", false);
    }

    public static int getUserId(Context context) {
        return context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .getInt("user_id", -1);
    }

    public static String getUsername(Context context) {
        return context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .getString("username", "GUEST");
    }

    public static int getAvatarId(Context context) {
        return context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .getInt("avatar_id", 1);
    }

    public static int getLoginDays(Context context) {
        return context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .getInt("login_days", 0);
    }

    // 清除本地会话，用于后续扩展退出登录功能。
    public static void clear(Context context) {
        context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }
}
