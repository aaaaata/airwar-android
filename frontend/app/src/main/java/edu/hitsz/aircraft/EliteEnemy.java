package edu.hitsz.aircraft;

import edu.hitsz.application.Main;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.factory.*;
import edu.hitsz.prop.AbstractProp;
import edu.hitsz.strategy.DirectShoot;
import edu.hitsz.observer.BombObserver;

import java.util.LinkedList;
import java.util.List;

/**
 * 精英敌机
 * 可射击
 * 被击毁后可产生道具
 */

public class EliteEnemy extends AbstractEnemy implements BombObserver{

    /**
     * 子弹一次发射数量
     */
    private int shootNum = 1;

    /**
     * 子弹伤害
     */
    private int power = 30;

    /**
     * 子弹射击方向 (向下发射：1)
     */
    private int direction = 1;

    private PropFactory propFactory;
    private AbstractProp prop;

    public EliteEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        this.shootStrategy = new DirectShoot(30, 1);
    }

    @Override
    public int update() {
        this.vanish();
        return 30; // 返回精英敌机的分数
    }

    @Override
    public void forward() {
        super.forward();
        // 判定 y 轴向下飞行出界
        if (locationY >= Main.WINDOW_HEIGHT) {
            vanish();
        }
    }

    /**
     * 精英敌机被击毁后产生道具
     * @return 产生的道具List
     */
    public List<AbstractProp> produceProps() {
        List<AbstractProp> props = new LinkedList<>();

        // 随机产生道具，总概率约90%
        double random = Math.random();
        int propX = this.getLocationX();
        int propY = this.getLocationY();

        if (random < 0.2) {
            // 20%概率产生加血道具
            propFactory = new BloodFactory();
            prop = propFactory.createProp(propX, propY);
            props.add(prop);
        } else if (random < 0.4) {
            // 20%概率产生火力道具
            propFactory = new BulletFactory();
            prop = propFactory.createProp(propX, propY);
            props.add(prop);
        } else if (random < 0.6) {
            // 20%概率产生炸弹道具
            propFactory = new BombFactory();
            prop = propFactory.createProp(propX, propY);
            props.add(prop);
        } else if (random < 0.8) {
            // 20%概率产生炸弹道具
            propFactory = new BulletPlusFactory();
            prop = propFactory.createProp(propX, propY);
            props.add(prop);
        }
        // 其余20%概率不产生道具


        return props;
    }
}