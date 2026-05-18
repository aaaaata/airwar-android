package edu.hitsz.manager;

import android.content.Context;
import android.media.MediaPlayer;

import edu.hitsz.R;

/*
 * MenuBgmManager 管理登录、主菜单、排行榜、商店、结算页等非战斗界面的循环背景音乐。
 * 战斗界面的 BGM 由 SoundManager 管理，因此进入战斗前需要停止菜单音乐，避免两种 BGM 同时播放。
 */
public class MenuBgmManager {

    // 静态播放器让多个菜单类页面共用同一首 BGM，避免切页时重复创建。
    private static MediaPlayer menuBgmPlayer;

    // 菜单音乐太大就调小，比如 0.15f、0.10f
    // 菜单音乐音量单独控制，避免 menu_bgm 相对其他音乐过响。
    private static final float MENU_BGM_VOLUME = 0.20f;

    // 播放菜单音乐：先检查全局音效开关，关闭时直接释放播放器。
    public static void play(Context context) {
        boolean soundEnabled = SoundSettingManager.isSoundEnabled(context);

        if (!soundEnabled) {
            stopAndRelease();
            return;
        }

        if (menuBgmPlayer == null) {
            menuBgmPlayer = MediaPlayer.create(
                    context.getApplicationContext(),
                    R.raw.menu_bgm
            );

            if (menuBgmPlayer == null) {
                return;
            }

            menuBgmPlayer.setLooping(true);
        }

        menuBgmPlayer.setVolume(MENU_BGM_VOLUME, MENU_BGM_VOLUME);

        if (!menuBgmPlayer.isPlaying()) {
            menuBgmPlayer.start();
        }
    }

    // 页面暂停时只暂停音乐，不释放，返回时可以继续播放。
    public static void pause() {
        if (menuBgmPlayer != null && menuBgmPlayer.isPlaying()) {
            menuBgmPlayer.pause();
        }
    }

    // 进入战斗或关闭音效时彻底停止并释放播放器，避免和战斗 BGM 重叠。
    public static void stopAndRelease() {
        if (menuBgmPlayer == null) {
            return;
        }

        if (menuBgmPlayer.isPlaying()) {
            menuBgmPlayer.stop();
        }

        menuBgmPlayer.release();
        menuBgmPlayer = null;
    }
}
