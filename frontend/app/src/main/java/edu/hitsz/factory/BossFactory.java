package edu.hitsz.factory;

import edu.hitsz.aircraft.AbstractEnemy;
import edu.hitsz.aircraft.BossEnemy;
import edu.hitsz.manager.ImageManager;
import edu.hitsz.application.Main;

public class BossFactory implements EnemyFactory {

    @Override
    public AbstractEnemy createEnemy() {
        int halfWidth = ImageManager.BOSS_ENEMY_IMAGE.getWidth() / 2;
        int halfHeight = ImageManager.BOSS_ENEMY_IMAGE.getHeight() / 2;

        // Boss从屏幕顶部居中生成，保证整张图不会超出顶部
        int enemyX = Main.WINDOW_WIDTH / 2;
        int enemyY = halfHeight + (int)(Main.WINDOW_HEIGHT * 0.01);

        int speedX = 5;
        int speedY = 0;
        int hp = 200;

        return new BossEnemy(enemyX, enemyY, speedX, speedY, hp);
    }
}