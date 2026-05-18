package edu.hitsz.factory;

import edu.hitsz.aircraft.AbstractEnemy;

/**
 * 敌机工厂接口
 */
public interface EnemyFactory {
    /**
     * 创建敌机对象
     * @return 敌机对象
     */
    AbstractEnemy createEnemy();
}