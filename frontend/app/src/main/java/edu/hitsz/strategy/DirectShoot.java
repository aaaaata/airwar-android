package edu.hitsz.strategy;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.HeroBullet;
import edu.hitsz.bullet.EnemyBullet;

import java.util.LinkedList;
import java.util.List;

/**
 - 直射策略
 - 发射1颗子弹，垂直飞行
 */
public class DirectShoot implements ShootStrategy {

    private int shootNum = 1;      // 子弹数量
    private int power = 30;        // 子弹伤害
    private int direction;         // 射击方向（-1向上，1向下）

    public DirectShoot(int power, int direction) {
        this.power = power;
        this.direction = direction;
    }

    @Override
    public List<BaseBullet> shoot(AbstractAircraft aircraft) {
        List<BaseBullet> res = new LinkedList<>();

        int x = aircraft.getLocationX();
        int y = aircraft.getLocationY() + direction * 2;
        int speedX = 0;
        int speedY = aircraft.getSpeedY() + direction * 5;

        BaseBullet bullet;
        if (direction == -1) {
            // 英雄机子弹（向上）
            bullet = new HeroBullet(x, y, speedX, speedY, power);
        } else {
            // 敌机子弹（向下）
            bullet = new EnemyBullet(x, y, speedX, speedY, power);
        }
        res.add(bullet);

        return res;

    }
}