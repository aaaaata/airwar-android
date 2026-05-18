package edu.hitsz.prop;

import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.strategy.CircleShoot;

/**
 - 超级火力道具
 - 生效后英雄机切换为环射弹道
 */
public class PropBulletPlus extends AbstractProp {

    public PropBulletPlus(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public void takeEffect(HeroAircraft heroAircraft,boolean soundEnabled) {
        // 切换为环射策略
        heroAircraft.setShootStrategy(new CircleShoot(30, -1));


        // 启动道具效果线程
        Thread effectThread = new Thread(new PropEffect(heroAircraft, "circle"));
        effectThread.start();
    }
}