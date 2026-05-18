package edu.hitsz.manager;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import java.util.HashSet;
import java.util.Set;

import edu.hitsz.R;

/*
 * SoundManager 管理战斗过程中的音频：MediaPlayer 播放较长的游戏 BGM / Boss BGM，SoundPool 播放较短的击中、炸弹、道具、游戏结束音效。
 * 这样区分后，长音频可以循环播放，短音效可以低延迟叠加播放，更适合游戏场景。
 */
/**
 * 游戏音频管理类
 * 1. MediaPlayer：长音频（游戏BGM、Boss BGM）
 * 2. SoundPool：短音效（击中、炸弹、道具、游戏结束）
 */
public class SoundManager {

    // 使用 applicationContext，避免持有 Activity 导致内存泄漏。
    private final Context context;
    private final boolean soundEnabled;

    // MediaPlayer 负责长音频：普通战斗 BGM 和 Boss BGM。
    private MediaPlayer gameBgmPlayer;
    private MediaPlayer bossBgmPlayer;

    // SoundPool 负责短音效：击中、炸弹、道具、游戏结束等。
    private SoundPool soundPool;
    private int bulletHitSoundId;
    private int bombExplosionSoundId;
    private int propEffectSoundId;
    private int gameOverSoundId;

    private boolean bossBgmPlaying = false;
    private boolean gameBgmPlaying = false;
    private final Set<Integer> loadedSoundIds = new HashSet<>();

    // 如果全局音效关闭，构造时直接返回，不创建播放器和音效池。
    public SoundManager(Context context, boolean soundEnabled) {
        this.context = context.getApplicationContext();
        this.soundEnabled = soundEnabled;

        if (!soundEnabled) {
            return;
        }

        initMediaPlayers();
        initSoundPool();
    }

    private static final float BGM_VOLUME = 1.0f;      // 背景音乐，尽量大
    private static final float EFFECT_VOLUME = 0.40f;  // 音效，调小

    // 初始化两类 BGM，并设置循环播放和音量。
    private void initMediaPlayers() {
        gameBgmPlayer = MediaPlayer.create(context, R.raw.bgm);
        if (gameBgmPlayer != null) {
            gameBgmPlayer.setLooping(true);
            gameBgmPlayer.setVolume(BGM_VOLUME, BGM_VOLUME);
        }

        bossBgmPlayer = MediaPlayer.create(context, R.raw.bgm_boss);
        if (bossBgmPlayer != null) {
            bossBgmPlayer.setLooping(true);
            bossBgmPlayer.setVolume(BGM_VOLUME, BGM_VOLUME);
        }
    }

    // 初始化短音效池；加载完成后才允许播放，避免音效未加载就触发。
    private void initSoundPool() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                .setMaxStreams(6)
                .build();

        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            if (status == 0) {
                loadedSoundIds.add(sampleId);
            }
        });

        bulletHitSoundId = soundPool.load(context, R.raw.bullet_hit, 1);
        bombExplosionSoundId = soundPool.load(context, R.raw.bomb_explosion, 1);
        propEffectSoundId = soundPool.load(context, R.raw.get_supply, 1);
        gameOverSoundId = soundPool.load(context, R.raw.game_over, 1);
    }

    // 播放普通战斗 BGM；Boss BGM 正在播放时不抢占。
    public void playGameBgm() {
        if (!soundEnabled || gameBgmPlayer == null) {
            return;
        }
        if (bossBgmPlaying) {
            return;
        }
        if (!gameBgmPlayer.isPlaying()) {
            gameBgmPlayer.start();
            gameBgmPlaying = true;
        }
    }

    public void pauseGameBgm() {
        if (!soundEnabled || gameBgmPlayer == null) {
            return;
        }
        if (gameBgmPlayer.isPlaying()) {
            gameBgmPlayer.pause();
            gameBgmPlaying = false;
        }
    }

    public void stopGameBgm() {
        if (!soundEnabled || gameBgmPlayer == null) {
            return;
        }
        if (gameBgmPlayer.isPlaying()) {
            gameBgmPlayer.pause();
            gameBgmPlayer.seekTo(0);
        }
        gameBgmPlaying = false;
    }

    // Boss 出现时暂停普通 BGM，切换到 Boss BGM。
    public void playBossBgm() {
        if (!soundEnabled || bossBgmPlayer == null) {
            return;
        }
        stopGameBgm();
        if (!bossBgmPlayer.isPlaying()) {
            bossBgmPlayer.start();
        }
        bossBgmPlaying = true;
    }

    public void stopBossBgm() {
        if (!soundEnabled || bossBgmPlayer == null) {
            return;
        }
        if (bossBgmPlayer.isPlaying()) {
            bossBgmPlayer.pause();
            bossBgmPlayer.seekTo(0);
        }
        bossBgmPlaying = false;
    }

    // Boss 被击败后停止 Boss BGM，恢复普通战斗 BGM。
    public void resumeGameBgmAfterBoss() {
        if (!soundEnabled) {
            return;
        }
        stopBossBgm();
        playGameBgm();
    }

    public void playBulletHit() {
        playEffect(bulletHitSoundId);
    }

    public void playBombExplosion() {
        playEffect(bombExplosionSoundId);
    }

    public void playPropEffect() {
        playEffect(propEffectSoundId);
    }

    public void playGameOver() {
        playEffect(gameOverSoundId);
    }

    // 播放短音效前检查开关、SoundPool 和加载状态，避免崩溃或无效播放。
    private void playEffect(int soundId) {
        if (!soundEnabled || soundPool == null || soundId == 0) {
            return;
        }

        if (!loadedSoundIds.contains(soundId)) {
            return;
        }

        soundPool.play(soundId, EFFECT_VOLUME, EFFECT_VOLUME, 1, 0, 1.0f);
    }

    // Activity 暂停或 Surface 销毁时暂停所有正在播放的 BGM。
    public void pauseAll() {
        if (!soundEnabled) {
            return;
        }
        if (gameBgmPlayer != null && gameBgmPlayer.isPlaying()) {
            gameBgmPlayer.pause();
        }
        if (bossBgmPlayer != null && bossBgmPlayer.isPlaying()) {
            bossBgmPlayer.pause();
        }
    }

    // 页面恢复时根据之前状态决定恢复 Boss BGM 还是普通 BGM。
    public void resumeByState() {
        if (!soundEnabled) {
            return;
        }
        if (bossBgmPlaying) {
            if (bossBgmPlayer != null && !bossBgmPlayer.isPlaying()) {
                bossBgmPlayer.start();
            }
        } else if (gameBgmPlaying) {
            if (gameBgmPlayer != null && !gameBgmPlayer.isPlaying()) {
                gameBgmPlayer.start();
            }
        } else {
            playGameBgm();
        }
    }

    public void stopAllBgm() {
        stopGameBgm();
        stopBossBgm();
    }

    // 游戏销毁时释放音频资源，防止播放器占用。
    public void release() {
        if (gameBgmPlayer != null) {
            gameBgmPlayer.release();
            gameBgmPlayer = null;
        }
        if (bossBgmPlayer != null) {
            bossBgmPlayer.release();
            bossBgmPlayer = null;
        }
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
