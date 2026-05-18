package edu.hitsz.factory;

import edu.hitsz.aircraft.AbstractEnemy;
import edu.hitsz.aircraft.MobEnemy;
import edu.hitsz.manager.ImageManager;
import edu.hitsz.application.Main;

/**
 - 普通敌机工厂
 - 负责创建普通敌机实例
 */
public class MobFactory implements EnemyFactory {

    @Override
    public AbstractEnemy createEnemy() {
        int halfWidth = ImageManager.MOB_ENEMY_IMAGE.getWidth() / 2;
        int halfHeight = ImageManager.MOB_ENEMY_IMAGE.getHeight() / 2;

        // 保证整张敌机图片都在屏幕内
        int enemyX = halfWidth + (int)(Math.random() * (Main.WINDOW_WIDTH - 2 * halfWidth));

        // 在屏幕顶部生成
        int enemyY = halfHeight;

        int speedX = 0;
        int speedY = 5;
        int hp = 60;

        return new MobEnemy(enemyX, enemyY, speedX, speedY, hp);
    }
}
