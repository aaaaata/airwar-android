package edu.hitsz.prop;

import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.application.Main;
import edu.hitsz.basic.AbstractFlyingObject;

/**
 * 道具抽象父类
 * 包括加血道具、火力道具、炸弹道具
 *
 * @author hitsz
 */
public abstract class AbstractProp extends AbstractFlyingObject {

    public AbstractProp(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
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
     * 道具生效方法
     * @param heroAircraft 英雄机对象
     */
    public abstract void takeEffect(HeroAircraft heroAircraft,boolean soundEnabled);
}
