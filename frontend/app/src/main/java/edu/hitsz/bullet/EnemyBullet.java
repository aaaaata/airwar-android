package edu.hitsz.bullet;
import edu.hitsz.observer.BombObserver;

/**
 * @Author hitsz
 */
public class EnemyBullet extends BaseBullet implements BombObserver {

    public EnemyBullet(int locationX, int locationY, int speedX, int speedY, int power) {
        super(locationX, locationY, speedX, speedY, power);
    }

    @Override
    public int update() {
        this.vanish();
        return 0;
    }

}
