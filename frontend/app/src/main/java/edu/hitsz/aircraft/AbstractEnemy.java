package edu.hitsz.aircraft;

import edu.hitsz.bullet.BaseBullet;

import java.util.List;

/**
 * 敌机抽象父类：
 * 包括普通敌机和精英敌机
 *
 * @author hitsz
 */
public abstract class AbstractEnemy extends AbstractAircraft {

    public AbstractEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
    }

    @Override
    public void forward() {
        super.forward();
    }

    /**
     * 敌机射击方法
     * 普通敌机不可射击，精英敌机可射击
     * @return 射击出的子弹List
     */
//    @Override
//    public abstract List<BaseBullet> shoot();
}