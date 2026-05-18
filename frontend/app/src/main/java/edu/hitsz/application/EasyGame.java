package edu.hitsz.application;

import android.content.Context;

/*
 * 讲解重点：
 * EasyGame 是简单难度的参数配置类。它继承 Game，只负责给父类提供简单模式下的敌机数量、生成周期、精英敌机概率等参数。
 * 简单模式没有 Boss，也不会随时间自动提升难度，因此适合用来说明“模板方法”：核心循环在 Game，具体难度参数由子类决定。
 */
/**
 * 简单模式：
 * 1. 无 Boss
 * 2. 难度不随时间提升
 */
public class EasyGame extends Game {

    // 兼容旧调用方式：默认关闭声音参数，实际游戏通常调用下面带 soundEnabled 的构造方法。
    public EasyGame(Context context) {
        this(context, false);
    }

    // 把难度标识和音效开关传给父类 Game，由父类完成核心初始化。
    public EasyGame(Context context, boolean soundEnabled) {
        super(context, "EASY", soundEnabled);
    }
    // 设置简单模式的核心参数：敌机少、生成慢、无 Boss、难度不增长。
    @Override
    protected void initDifficulty() {
        this.enemyMaxNumber = 3;
        this.bossScoreThreshold = Integer.MAX_VALUE;
        this.cycleDuration = 800;
        this.eliteCycleDuration = 1500;
        this.eliteProbability = 0.15;
        this.superEliteProbability = 0.05;
        this.hpMultiplier = 1.0;
        this.bossBaseHp = 0;
    }
    // 简单模式没有 Boss，因此父类创建敌机时不会进入 Boss 创建逻辑。
    @Override
    protected boolean hasBoss() {
        return false;
    }
    // 返回 false 表示 updateGame 中不会执行动态增难。
    @Override
    protected boolean increaseDifficulty() {
        return false;
    }

    @Override
    protected void doIncreaseDifficulty() {
        // 简单模式不增难
    }

    @Override
    protected int getBossHp() {
        return 0;
    }
}
