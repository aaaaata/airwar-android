package edu.hitsz.manager;

import android.content.Context;

/*
 * SoundSettingManager 保存全局音效开关状态。主菜单开关修改后，其他页面读取同一个 SharedPreferences 值。
 * 这样可以保证菜单音乐、战斗 BGM 和战斗音效使用同一套开关逻辑。
 */
public class SoundSettingManager {

    // 音效设置保存位置，独立于账号和商店状态。
    private static final String PREF_NAME = "game_settings";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";

    // 读取全局音效开关，默认开启。
    public static boolean isSoundEnabled(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SOUND_ENABLED, true);
    }

    // 主菜单开关变化时写入本地，后续所有页面都会读取这个值。
    public static void setSoundEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SOUND_ENABLED, enabled)
                .apply();
    }
}
