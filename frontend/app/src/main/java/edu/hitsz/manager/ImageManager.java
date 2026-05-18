package edu.hitsz.manager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.HashMap;
import java.util.Map;

import edu.hitsz.R;
import edu.hitsz.aircraft.BossEnemy;
import edu.hitsz.aircraft.EliteEnemy;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.aircraft.MobEnemy;
import edu.hitsz.aircraft.SuperEliteEnemy;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.bullet.HeroBullet;
import edu.hitsz.prop.PropBlood;
import edu.hitsz.prop.PropBomb;
import edu.hitsz.prop.PropBullet;
import edu.hitsz.prop.PropBulletPlus;
import edu.hitsz.prop.PropCoin;
import edu.hitsz.config.SkinConfig;

/*
 * ImageManager 负责战斗层 Bitmap 资源的加载、统一缩放和按类名映射。
 * 因为战斗画面不是 XML 控件，而是 Canvas 手动画 Bitmap，所以必须在这里限制英雄机、敌机、子弹、道具的显示尺寸。
 * 商店换皮肤后，也通过 reloadHeroImage 重新加载英雄机素材。
 */
/**
 * 安卓版图片管理器。
 *
 * 注意：
 * 1. 游戏战斗层不是 XML 控件，而是在 Canvas 中直接绘制 Bitmap；
 * 2. 因此战斗资源必须在这里统一缩放；
 * 3. 以后即使上传更高清的 png，只要文件名不变，游戏内显示尺寸也不会失控。
 */
public final class ImageManager {

    // 通过类名映射 Bitmap，游戏对象绘制时可以根据自身 class 快速找到对应图片。
    private static final Map<String, Bitmap> CLASSNAME_IMAGE_MAP = new HashMap<>();

    private static boolean initialized = false;

    /*
     * 战斗资源统一目标高度，单位是 px，不是 dp。
     * 如果你觉得某类资源偏大或偏小，只改这里的数值。
     */
    // 统一高度配置：限制各类战斗素材显示尺寸，避免换高清图后飞机/道具变得异常巨大。
    private static final int HERO_TARGET_HEIGHT = 320;

    private static final int MOB_TARGET_HEIGHT = 200;
    private static final int ELITE_TARGET_HEIGHT = 300;
    private static final int SUPER_ELITE_TARGET_HEIGHT = 300;
    private static final int BOSS_TARGET_HEIGHT = 800;

    private static final int HERO_BULLET_TARGET_HEIGHT = 70;
    private static final int ENEMY_BULLET_TARGET_HEIGHT = 50;

    private static final int PROP_TARGET_HEIGHT = 120;
    private static final int COIN_PROP_TARGET_HEIGHT = 70;

    public static Bitmap BACKGROUND_IMAGE_EASY;
    public static Bitmap BACKGROUND_IMAGE_NORMAL;
    public static Bitmap BACKGROUND_IMAGE_HARD;

    public static Bitmap HERO_IMAGE;
    public static Bitmap HERO_BULLET_IMAGE;
    public static Bitmap ENEMY_BULLET_IMAGE;

    public static Bitmap MOB_ENEMY_IMAGE;
    public static Bitmap ELITE_ENEMY_IMAGE;
    public static Bitmap SUPER_ELITE_ENEMY_IMAGE;
    public static Bitmap BOSS_ENEMY_IMAGE;

    public static Bitmap BLOOD_PROP_IMAGE;
    public static Bitmap BULLET_PROP_IMAGE;
    public static Bitmap BOMB_PROP_IMAGE;
    public static Bitmap BULLET_PROP_PLUS_IMAGE;
    public static Bitmap COIN_PROP_IMAGE;

    private ImageManager() {
    }

    // 初始化所有战斗资源。只完整加载一次，后续进入游戏时主要刷新英雄机皮肤。
    public static synchronized void init(Context context) {
        Context appContext = context.getApplicationContext();

        if (initialized) {
            reloadHeroImage(appContext);
            return;
        }

        BACKGROUND_IMAGE_EASY = BitmapFactory.decodeResource(
                appContext.getResources(),
                R.drawable.bg_menu_initial
        );
        BACKGROUND_IMAGE_NORMAL = BitmapFactory.decodeResource(
                appContext.getResources(),
                R.drawable.bg_menu_initial
        );
        BACKGROUND_IMAGE_HARD = BitmapFactory.decodeResource(
                appContext.getResources(),
                R.drawable.bg_menu_initial
        );

        reloadHeroImage(appContext);

        MOB_ENEMY_IMAGE = loadScaledByHeight(appContext, R.drawable.mob, MOB_TARGET_HEIGHT);
        ELITE_ENEMY_IMAGE = loadScaledByHeight(appContext, R.drawable.elite, ELITE_TARGET_HEIGHT);
        SUPER_ELITE_ENEMY_IMAGE = loadScaledByHeight(appContext, R.drawable.elite_plus, SUPER_ELITE_TARGET_HEIGHT);
        BOSS_ENEMY_IMAGE = loadScaledByHeight(appContext, R.drawable.boss, BOSS_TARGET_HEIGHT);

        HERO_BULLET_IMAGE = loadScaledByHeight(appContext, R.drawable.bullet_hero, HERO_BULLET_TARGET_HEIGHT);
        ENEMY_BULLET_IMAGE = loadScaledByHeight(appContext, R.drawable.bullet_enemy, ENEMY_BULLET_TARGET_HEIGHT);

        BLOOD_PROP_IMAGE = loadScaledByHeight(appContext, R.drawable.prop_blood, PROP_TARGET_HEIGHT);
        BULLET_PROP_IMAGE = loadScaledByHeight(appContext, R.drawable.prop_bullet, PROP_TARGET_HEIGHT);
        BOMB_PROP_IMAGE = loadScaledByHeight(appContext, R.drawable.prop_bomb, PROP_TARGET_HEIGHT);
        BULLET_PROP_PLUS_IMAGE = loadScaledByHeight(appContext, R.drawable.prop_bullet_plus, PROP_TARGET_HEIGHT);
        COIN_PROP_IMAGE = loadScaledByHeight(appContext, R.drawable.prop_coin, COIN_PROP_TARGET_HEIGHT);

        refreshClassNameImageMap();

        initialized = true;
    }

    // 根据商店中选中的皮肤重新加载英雄机图片，换皮肤后立即影响战斗绘制。
    public static synchronized void reloadHeroImage(Context context) {
        Context appContext = context.getApplicationContext();

        int selectedSkinId = ShopStateManager.getSelectedSkinId(appContext);
        int heroResId = SkinConfig.getSkinDrawableResId(selectedSkinId);

        HERO_IMAGE = loadScaledByHeight(appContext, heroResId, HERO_TARGET_HEIGHT);
        CLASSNAME_IMAGE_MAP.put(HeroAircraft.class.getName(), HERO_IMAGE);
    }

    // 更新类名到图片的映射表，敌机、子弹、道具都会在这里登记。
    private static void refreshClassNameImageMap() {
        CLASSNAME_IMAGE_MAP.clear();

        CLASSNAME_IMAGE_MAP.put(HeroAircraft.class.getName(), HERO_IMAGE);

        CLASSNAME_IMAGE_MAP.put(MobEnemy.class.getName(), MOB_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EliteEnemy.class.getName(), ELITE_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(SuperEliteEnemy.class.getName(), SUPER_ELITE_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BossEnemy.class.getName(), BOSS_ENEMY_IMAGE);

        CLASSNAME_IMAGE_MAP.put(HeroBullet.class.getName(), HERO_BULLET_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EnemyBullet.class.getName(), ENEMY_BULLET_IMAGE);

        CLASSNAME_IMAGE_MAP.put(PropBlood.class.getName(), BLOOD_PROP_IMAGE);
        CLASSNAME_IMAGE_MAP.put(PropBullet.class.getName(), BULLET_PROP_IMAGE);
        CLASSNAME_IMAGE_MAP.put(PropBomb.class.getName(), BOMB_PROP_IMAGE);
        CLASSNAME_IMAGE_MAP.put(PropBulletPlus.class.getName(), BULLET_PROP_PLUS_IMAGE);
        CLASSNAME_IMAGE_MAP.put(PropCoin.class.getName(), COIN_PROP_IMAGE);
    }

    // 按目标高度加载并缩放图片，保证不同素材尺寸统一可控。
    private static Bitmap loadScaledByHeight(Context context, int resId, int targetHeight) {
        Bitmap raw = BitmapFactory.decodeResource(context.getResources(), resId);
        return scaleBitmapByHeight(raw, targetHeight);
    }

    // 按原图宽高比等比例缩放，避免图片被拉伸变形。
    private static Bitmap scaleBitmapByHeight(Bitmap source, int targetHeight) {
        if (source == null || targetHeight <= 0) {
            return source;
        }

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return source;
        }

        int targetWidth = Math.max(
                1,
                Math.round(sourceWidth * (targetHeight / (float) sourceHeight))
        );

        return Bitmap.createScaledBitmap(
                source,
                targetWidth,
                targetHeight,
                false
        );
    }

    // 根据类名获取图片，供 AbstractFlyingObject 等绘制逻辑调用。
    public static Bitmap get(String className) {
        return CLASSNAME_IMAGE_MAP.get(className);
    }

    public static Bitmap get(Object obj) {
        if (obj == null) {
            return null;
        }
        return get(obj.getClass().getName());
    }
}
