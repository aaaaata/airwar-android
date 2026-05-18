package edu.hitsz.strategy;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.HeroBullet;
import edu.hitsz.bullet.EnemyBullet;

import java.util.LinkedList;
import java.util.List;

/**

 - 散射策略
 - 同时发射3颗子弹，呈扇形分布
 - @author hitsz
 */
public class ScatterShoot implements ShootStrategy {

    private int shootNum = 3;      // 子弹数量
    private int power = 30;        // 子弹伤害
    private int direction;         // 射击方向（-1向上，1向下）

    public ScatterShoot(int power, int direction) {
        this.power = power;
        this.direction = direction;
    }

    @Override
    public List<BaseBullet> shoot(AbstractAircraft aircraft) {
        List<BaseBullet> res = new LinkedList<>();


        int x = aircraft.getLocationX();
        int y = aircraft.getLocationY() + direction * 2;
        int speedY = aircraft.getSpeedY() + direction * 5;

        BaseBullet bullet;

        // 中间子弹：垂直飞行
        if (direction == -1) {
            bullet = new HeroBullet(x, y, 0, speedY, power);
        } else {
            bullet = new EnemyBullet(x, y, 0, speedY, power);
        }
        res.add(bullet);

        // 左侧子弹：向左倾斜
        if (direction == -1) {
            bullet = new HeroBullet(x - 20, y, -3, speedY, power);
        } else {
            bullet = new EnemyBullet(x - 20, y, -3, speedY, power);
        }
        res.add(bullet);

        // 右侧子弹：向右倾斜
        if (direction == -1) {
            bullet = new HeroBullet(x + 20, y, 3, speedY, power);
        } else {
            bullet = new EnemyBullet(x + 20, y, 3, speedY, power);
        }
        res.add(bullet);

        return res;


    }
}
