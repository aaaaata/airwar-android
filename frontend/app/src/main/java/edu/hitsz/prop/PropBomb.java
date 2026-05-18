// PropBomb.java - 炸弹道具
package edu.hitsz.prop;

import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.observer.BombObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * 炸弹道具
 * 充当观察者模式的发布者
 */
public class PropBomb extends AbstractProp {
    /**
     - 观察者列表
     */
    private List<BombObserver> observers = new ArrayList<>();

    /**
     - 炸弹爆炸后获得的总分数
     */
    private int totalScore = 0;

    public PropBomb(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    /**
     - 增加观察者
     */
    public void addObserver(BombObserver observer) {
        observers.add(observer);
    }

    /**
     - 删除观察者
     */
    public void removeObserver(BombObserver observer) {
        observers.remove(observer);
    }

    /**
     - 通知所有观察者
     */
    public void notifyAllObserver() {
        for (BombObserver observer : observers) {
            int score = observer.update();
            totalScore += score;
        }
    }


    @Override
    public void takeEffect(HeroAircraft heroAircraft,boolean soundEnabled) {
        System.out.println("BombSupply active!");

        // TODO: 实现炸弹效果（清除所有敌机）
        notifyAllObserver();
    }

    /**
     - 获取炸弹爆炸获得的总分数
     */
    public int getTotalScore() {
        return totalScore;
    }
}