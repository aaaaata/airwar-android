package edu.hitsz.factory;

import edu.hitsz.prop.AbstractProp;
import edu.hitsz.prop.PropBlood;

/**
 - 加血道具工厂
 - 负责创建加血道具实例
 */
public class BloodFactory implements PropFactory {

    @Override
    public AbstractProp createProp(int locationX, int locationY) {
        // 道具参数设置
        int speedX = 0;
        int speedY = 5;

        return new PropBlood(locationX, locationY, speedX, speedY);

    }
}
