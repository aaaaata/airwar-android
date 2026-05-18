package edu.hitsz.aircraft;

import edu.hitsz.manager.ImageManager;
import edu.hitsz.application.Main;
import edu.hitsz.factory.*;
import edu.hitsz.prop.AbstractProp;
import edu.hitsz.strategy.ScatterShoot;
import edu.hitsz.observer.BombObserver;

import java.util.LinkedList;
import java.util.List;

/**
 - 超级精英敌机
 - 每隔一定周期随机产生
 - 向屏幕下方左右移动
 - 散射弹道，同时发射3颗子弹，呈扇形
 - 坠毁后随机掉落 <= 1个道具
 */
public class SuperEliteEnemy extends AbstractEnemy implements BombObserver{

    /**
     - 子弹一次发射数量（散射3颗）
     */
    private int shootNum = 3;

    /**
     - 子弹伤害
     */
    private int power = 30;

    /**
     - 子弹射击方向 (向下发射：1)
     */
    private int direction = 1;

    /**
     - 水平移动方向 (1向右，-1向左)
     */
    private int horizontalDirection = 1;

    public SuperEliteEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        // 随机初始水平移动方向
        this.horizontalDirection = Math.random() < 0.5 ? 1 : -1;
        this.shootStrategy = new ScatterShoot(30, 1);
    }

    @Override
    public int update() {
        this.decreaseHp(50); // 血量减少50
        if (this.getHp() <= 0) {
            this.vanish();
            return 50; // 返回超级精英敌机的分数
        } else {
            return 0; // 未坠毁，不给分
        }
    }

    @Override
    public void forward() {
        locationX += speedX * horizontalDirection;
        locationY += speedY;

        int halfWidth = ImageManager.SUPER_ELITE_ENEMY_IMAGE.getWidth() / 2;
        int halfHeight = ImageManager.SUPER_ELITE_ENEMY_IMAGE.getHeight() / 2;

        // 碰到左右边界则反向，并把位置拉回合法范围
        if (locationX <= halfWidth || locationX >= Main.WINDOW_WIDTH - halfWidth) {
            horizontalDirection = -horizontalDirection;
            locationX = Math.max(halfWidth, Math.min(Main.WINDOW_WIDTH - halfWidth, locationX));
        }

        // 完全飞出屏幕底部再消失
        if (locationY >= Main.WINDOW_HEIGHT + halfHeight) {
            vanish();
        }
    }


    /**
     - 超级精英敌机被击毁后产生道具
     - 随机掉落 <= 1个道具（概率更低）
     - @return 产生的道具List
     */
    public List<AbstractProp> produceProps() {
        List<AbstractProp> props = new LinkedList<>();

        double random = Math.random();
        int propX = this.getLocationX();
        int propY = this.getLocationY();

        PropFactory propFactory;

        if (random < 0.20) {
            // 20%概率产生加血道具
            propFactory = new BloodFactory();
        } else if (random < 0.40) {
            // 20%概率产生火力道具
            propFactory = new BulletFactory();
        } else if (random < 0.60) {
            // 20%概率产生炸弹道具
            propFactory = new BombFactory();
        } else if (random < 0.80) {
            // 20%概率产生炸弹道具
            propFactory = new BulletPlusFactory();
        } else {
            // 20%概率不产生道具
            return props;
        }

        props.add(propFactory.createProp(propX, propY));
        return props;
    }
}

