package edu.hitsz.factory;

import edu.hitsz.prop.AbstractProp;
import edu.hitsz.prop.PropBomb;

/**
 - 炸弹道具工厂
 - 负责创建炸弹道具实例
 */
public class BombFactory implements PropFactory {

    @Override
    public AbstractProp createProp(int locationX, int locationY) {
        // 道具参数设置
        int speedX = 0;
        int speedY = 5;

        return new PropBomb(locationX, locationY, speedX, speedY);

    }
}