package edu.hitsz.prop;

import edu.hitsz.aircraft.HeroAircraft;
/**
 * 加血道具
 *
 * @author hitsz
 */
public class PropBlood extends AbstractProp {

    /**
     * 恢复的血量
     */
    private int healAmount = 200;

    public PropBlood(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public void takeEffect(HeroAircraft heroAircraft,boolean soundEnabled) {
        heroAircraft.increaseHp(healAmount);

    }
}