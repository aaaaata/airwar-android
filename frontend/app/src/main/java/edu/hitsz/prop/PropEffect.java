package edu.hitsz.prop;

import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.strategy.DirectShoot;
import edu.hitsz.strategy.ScatterShoot;
import edu.hitsz.strategy.CircleShoot;

/**
 - 道具效果线程
 - 实现Runnable接口，火力道具持续3秒后恢复
 */
public class PropEffect implements Runnable {

    private HeroAircraft heroAircraft;
    private String propType;

    public PropEffect(HeroAircraft heroAircraft, String propType) {
        this.heroAircraft = heroAircraft;
        this.propType = propType;
    }

    @Override
    public void run() {
        try {
            // 持续3秒
            Thread.sleep(3000);

            // 恢复为直射
            heroAircraft.setShootStrategy(new DirectShoot(30, -1));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }
}