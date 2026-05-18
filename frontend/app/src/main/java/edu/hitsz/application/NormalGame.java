package edu.hitsz.application;

import android.content.Context;
import android.util.Log;

/*
 * NormalGame 是普通难度的配置类。有 Boss，难度会随时间逐步提升，但提升速度比 HardGame 更温和。
 * 讲解时可以和 EasyGame、HardGame 对比，说明三种难度实际只是重写 Game 中的抽象配置方法。
 */
/**
 * 普通模式：
 * 1. 有 Boss
 * 2. 难度随时间提升
 * 3. Boss 每次出现血量固定
 */
public class NormalGame extends Game {

    private static final String TAG = "NormalGame";

    public NormalGame(Context context) {
        this(context, false);
    }

    public NormalGame(Context context, boolean soundEnabled) {
        super(context, "NORMAL", soundEnabled);
    }
    // 普通模式初始参数：有 Boss，难度提升速度介于 Easy 和 Hard 之间。
    @Override
    protected void initDifficulty() {
        this.enemyMaxNumber = 5;
        this.bossScoreThreshold = 300;
        this.cycleDuration = 600;
        this.eliteCycleDuration = 1200;
        this.eliteProbability = 0.20;
        this.superEliteProbability = 0.15;
        this.hpMultiplier = 1.0;
        this.bossBaseHp = 200;
    }

    @Override
    protected boolean hasBoss() {
        return true;
    }

    @Override
    protected boolean increaseDifficulty() {
        return true;
    }
    // 普通模式的动态增难：逐步增加敌机上限、精英概率并缩短刷新间隔。
    @Override
    protected void doIncreaseDifficulty() {
        enemyMaxNumber = Math.min(enemyMaxNumber + 1, 8);
        eliteProbability = Math.min(eliteProbability + 0.02, 0.30);
        cycleDuration = Math.max((int) (cycleDuration * 0.98), 400);
        eliteCycleDuration = Math.max((int) (eliteCycleDuration * 0.98), 800);
        hpMultiplier += 0.02;

        Log.d(TAG, "提高难度: enemyMaxNumber=" + enemyMaxNumber
                + ", eliteProbability=" + eliteProbability
                + ", cycleDuration=" + cycleDuration
                + ", eliteCycleDuration=" + eliteCycleDuration
                + ", hpMultiplier=" + hpMultiplier);
    }
    // 普通模式 Boss 血量固定，避免难度增长过快。
    @Override
    protected int getBossHp() {
        return bossBaseHp;
    }
}
