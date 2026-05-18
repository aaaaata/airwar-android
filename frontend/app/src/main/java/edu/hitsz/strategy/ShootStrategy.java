package edu.hitsz.strategy;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.bullet.BaseBullet;

import java.util.List;

/**
 - 射击策略接口
 */
public interface ShootStrategy {
    /**
     - 射击方法
     - @param aircraft 执行射击的飞机
     - @return 射出的子弹列表
     */
    List<BaseBullet> shoot(AbstractAircraft aircraft);
}
