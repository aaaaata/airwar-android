// PropBullet.java - 火力道具
package edu.hitsz.prop;

import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.strategy.ScatterShoot;

/**
 * 火力道具
 * 暂时只实现控制台输出
 *
 * @author hitsz
 */
public class PropBullet extends AbstractProp {

    public PropBullet(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public void takeEffect(HeroAircraft heroAircraft,boolean soundEnabled) {
        System.out.println("FireSupply active!");
        // 切换为散射策略
        heroAircraft.setShootStrategy(new ScatterShoot(30, -1));


        // 启动道具效果线程
        Thread effectThread = new Thread(new PropEffect(heroAircraft, "scatter"));
        effectThread.start();
    }
}
