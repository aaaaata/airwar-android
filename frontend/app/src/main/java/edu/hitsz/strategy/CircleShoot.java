package edu.hitsz.strategy;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.HeroBullet;
import edu.hitsz.bullet.EnemyBullet;

import java.util.LinkedList;
import java.util.List;

/**

 - 环射策略
 - 同时发射20颗子弹，呈环形分布（360度）
 - @author hitsz
 */
public class CircleShoot implements ShootStrategy {

    private int shootNum = 20;     // 子弹数量
    private int power = 30;        // 子弹伤害
    private int direction;         // 射击方向（-1向上，1向下）

    public CircleShoot(int power, int direction) {
        this.power = power;
        this.direction = direction;
    }

    @Override
    public List<BaseBullet> shoot(AbstractAircraft aircraft) {
        List<BaseBullet> res = new LinkedList<>();

        int x = aircraft.getLocationX();
        int y = aircraft.getLocationY();
        double baseSpeed = 8; // 子弹基准速度

        // 发射20颗子弹，呈环形分布
        double angleStep = 360.0 / shootNum;  // 每颗子弹间隔18度

        for (int i = 0; i < shootNum; i++) {
            double angle = Math.toRadians(i * angleStep);

            // 计算子弹的速度分量
            int bulletSpeedX = (int) (baseSpeed * Math.cos(angle));
            int bulletSpeedY = (int) (baseSpeed * Math.sin(angle));

            BaseBullet bullet;
            if (direction == -1) {
                // 英雄机子弹（向上射击的飞机）
                bullet = new HeroBullet(x, y, bulletSpeedX, bulletSpeedY, power);
            } else {
                // 敌机子弹（向下射击的飞机）
                bullet = new EnemyBullet(x, y, bulletSpeedX, bulletSpeedY, power);
            }
            res.add(bullet);
        }

        return res;

    }
}
