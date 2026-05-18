package edu.hitsz.application;

import android.content.Context;
import android.util.Log;

/*
 * 讲解重点：
 * HardGame 是困难难度的配置类。它的敌机上限更高、生成速度更快、精英敌机概率更高，并且 Boss 血量会随着出现次数递增。
 * 这个类体现了在不改 Game 主循环的情况下，通过子类覆盖参数和增难逻辑实现不同游戏难度。
 */
/**
 * 困难模式：
 * 1. 有 Boss
 * 2. 难度提升更快
 * 3. Boss 每次出现血量递增
 */
public class HardGame extends Game {

    private static final String TAG = "HardGame";

    // 记录 Boss 出现次数，用来让困难模式 Boss 血量逐次递增。
    private int bossSpawnCount = 0;

    public HardGame(Context context) {
        this(context, false);
    }

    public HardGame(Context context, boolean soundEnabled) {
        super(context, "HARD", soundEnabled);
    }
    // 困难模式初始参数：敌机更多、刷新更快、精英敌机概率更高。
    @Override
    protected void initDifficulty() {
        this.enemyMaxNumber = 7;
        this.bossScoreThreshold = 300;
        this.cycleDuration = 500;
        this.eliteCycleDuration = 1000;
        this.eliteProbability = 0.25;
        this.superEliteProbability = 0.20;
        this.hpMultiplier = 1.0;
        this.bossBaseHp = 300;
    }

    @Override
    protected boolean hasBoss() {
        return true;
    }

    @Override
    protected boolean increaseDifficulty() {
        return true;
    }
    // 每隔一段时间逐步提高敌机数量、生成频率和血量倍率，形成持续压迫感。
    @Override
    protected void doIncreaseDifficulty() {
        enemyMaxNumber = Math.min(enemyMaxNumber + 1, 10);
        eliteProbability = Math.min(eliteProbability + 0.03, 0.40);
        cycleDuration = Math.max((int) (cycleDuration * 0.95), 300);
        eliteCycleDuration = Math.max((int) (eliteCycleDuration * 0.95), 600);
        hpMultiplier += 0.05;

        Log.d(TAG, "提高难度: enemyMaxNumber=" + enemyMaxNumber
                + ", eliteProbability=" + eliteProbability
                + ", cycleDuration=" + cycleDuration
                + ", eliteCycleDuration=" + eliteCycleDuration
                + ", hpMultiplier=" + hpMultiplier);
    }
    // 困难模式 Boss 血量随出现次数递增，越到后期越难打。
    @Override
    protected int getBossHp() {
        bossSpawnCount++;
        int currentBossHp = bossBaseHp + (bossSpawnCount - 1) * 100;
        Log.d(TAG, "Boss 第 " + bossSpawnCount + " 次出现, 血量=" + currentBossHp);
        return currentBossHp;
    }
}
