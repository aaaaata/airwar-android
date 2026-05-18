package edu.hitsz.prop;

import edu.hitsz.aircraft.HeroAircraft;

public class PropCoin extends AbstractProp {

    private static final int COIN_VALUE = 1;

    public PropCoin(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    public int getCoinValue() {
        return COIN_VALUE;
    }

    @Override
    public void takeEffect(HeroAircraft heroAircraft, boolean soundEnabled) {
        // 金币不直接改变英雄机属性，真正加金币在 Game.checkPropsHitHero() 里处理。
    }
}