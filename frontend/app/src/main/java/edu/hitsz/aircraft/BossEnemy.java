package edu.hitsz.aircraft;

import edu.hitsz.manager.ImageManager;
import edu.hitsz.application.Main;
import edu.hitsz.factory.*;
import edu.hitsz.prop.AbstractProp;
import edu.hitsz.strategy.CircleShoot;
import edu.hitsz.observer.BombObserver;

import java.util.LinkedList;
import java.util.List;

/**
 - Boss敌机
 - 分数达到设定阈值后出现，可多次出现
 - 悬浮于界面上方左右移动
 - 环射弹道，同时发射20颗子弹，呈环形
 - 坠毁后随机掉落 <= 3个道具
 */
public class BossEnemy extends AbstractEnemy implements BombObserver{

    /**
     - 子弹一次发射数量（环射20颗）
     */
    private int shootNum = 20;

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

    /**
     - Boss悬浮的固定y坐标
     */
    private int floatY;

    @Override
    public int update() {
        return 0; // Boss不受影响，不给分
    }

    public BossEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);

        // 直接使用工厂传入的y作为Boss悬浮高度
        this.floatY = locationY;
        this.locationY = locationY;

        this.horizontalDirection = Math.random() < 0.5 ? 1 : -1;
        this.shootStrategy = new CircleShoot(30, 1);
    }

    @Override
    public void forward() {
        locationX += speedX * horizontalDirection;
        locationY = floatY;

        int halfWidth = ImageManager.BOSS_ENEMY_IMAGE.getWidth() / 2;

        if (locationX <= halfWidth || locationX >= Main.WINDOW_WIDTH - halfWidth) {
            horizontalDirection = -horizontalDirection;
            locationX = Math.max(halfWidth, Math.min(Main.WINDOW_WIDTH - halfWidth, locationX));
        }
    }


    /**
     - Boss敌机被击毁后产生道具
     - 随机掉落 <= 3个道具
     - @return 产生的道具List
     */
    public List<AbstractProp> produceProps() {
        List<AbstractProp> props = new LinkedList<>();

        int propCount = 1 + (int) (Math.random() * 3); // 随机1-3个道具

        int propX = this.getLocationX();
        int propY = this.getLocationY();

        for (int i = 0; i < propCount; i++) {
            double random = Math.random();
            PropFactory propFactory;


            if (random < 0.25) {
                propFactory = new BloodFactory();
            } else if (random < 0.50) {
                propFactory = new BulletFactory();
            } else if (random < 0.75){
                propFactory = new BombFactory();
            } else {
                propFactory = new BulletPlusFactory();
            }

            // 多个道具位置稍微错开，避免重叠
            int offsetX = (i - propCount / 2) * 30;
            props.add(propFactory.createProp(propX + offsetX, propY));


        }

        return props;
    }
}
