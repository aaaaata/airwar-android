package edu.hitsz.aircraft;

import edu.hitsz.manager.ImageManager;
import edu.hitsz.strategy.DirectShoot;

/**
 * 英雄飞机，游戏玩家操控
 * 采用DCL实现单例模式
 */
public class HeroAircraft extends AbstractAircraft {

    /**
     - 单例对象
     */
    private volatile static HeroAircraft instance;

    /**攻击方式 */

    /**
     * 子弹一次发射数量
     */
    private int shootNum = 1;

    /**
     * 子弹伤害
     */
    private int power = 30;

    /**
     * 子弹射击方向 (向上发射：1，向下发射：-1)
     */
    private int direction = -1;

    /**
     * @param locationX 英雄机位置x坐标
     * @param locationY 英雄机位置y坐标
     * @param speedX 英雄机射出的子弹的基准速度（英雄机无特定速度）
     * @param speedY 英雄机射出的子弹的基准速度（英雄机无特定速度）
     * @param hp    初始生命值
     */
    private HeroAircraft(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        this.shootStrategy = new DirectShoot(30, -1); // shootStrategy为类属性
    }


    /**
     - 获取英雄机单例对象
     - 使用默认参数创建
     - @return 英雄机单例对象
     */
    public static HeroAircraft getInstance() {
        if (instance == null) {
            synchronized (HeroAircraft.class) {
                if (instance == null) {
                    // 使用默认参数创建英雄机
                    instance = new HeroAircraft(
                            edu.hitsz.application.Main.WINDOW_WIDTH / 2,
                            edu.hitsz.application.Main.WINDOW_HEIGHT -
                                    ImageManager.HERO_IMAGE.getHeight(),
                            0,
                            0,
                            1000
                    );
                }
            }
        }
        return instance;
    }

    @Override
    public void forward() {
        // 英雄机由鼠标控制，不通过forward函数移动
    }


    /**
     * 增加血量，用于血包道具
     * @param increase 增加的血量
     */
    public void increaseHp(int increase) {
        // 血量不能超过最大血量
        this.hp = Math.min(this.hp + increase, this.maxHp);
    }

}
