package edu.hitsz.factory;

import edu.hitsz.aircraft.AbstractEnemy;
import edu.hitsz.aircraft.SuperEliteEnemy;
import edu.hitsz.manager.ImageManager;
import edu.hitsz.application.Main;

/**
 - 超级精英敌机工厂
 - 负责创建超级精英敌机实例
 */
public class SuperEliteFactory implements EnemyFactory {

    @Override
    public AbstractEnemy createEnemy() {
        int enemyX = (int) (Math.random() * (Main.WINDOW_WIDTH - ImageManager.SUPER_ELITE_ENEMY_IMAGE.getWidth()));
        int enemyY = (int) (Math.random() * Main.WINDOW_HEIGHT * 0.05);

        // 超级精英敌机参数设置
        int speedX = 3;
        int speedY = 3;      // 比精英敌机慢
        int hp = 80;         // 比精英敌机更耐打

        return new SuperEliteEnemy(enemyX, enemyY, speedX, speedY, hp);


    }
}