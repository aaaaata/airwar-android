package edu.hitsz.config;

import edu.hitsz.R;

/*
 * SkinConfig 是商店皮肤配置表，保存每个皮肤的 id、价格和图片资源。
 * 商店页面、皮肤购买逻辑、ImageManager 重新加载英雄机图片时都会读取这里的配置。
 */
public final class SkinConfig {

    private SkinConfig() {
    }

    // 单个皮肤配置项：id 用于存储和后端同步，price 用于商店购买，drawableResId 用于显示和战斗绘制。
    public static class SkinItem {
        public final int skinId;
        public final int price;
        public final int drawableResId;

        public SkinItem(int skinId, int price, int drawableResId) {
            this.skinId = skinId;
            this.price = price;
            this.drawableResId = drawableResId;
        }
    }

    // 所有可购买/可选择皮肤的配置表，新增皮肤时主要在这里追加一项。
    public static final SkinItem[] SKINS = new SkinItem[] {
            new SkinItem(1, 0, R.drawable.hero),
            new SkinItem(2, 487, R.drawable.img_plane_skin_2),
            new SkinItem(3, 33, R.drawable.img_plane_skin_3),
            new SkinItem(4, 8, R.drawable.img_plane_skin_4),
            new SkinItem(5, 6, R.drawable.img_plane_skin_5),
            new SkinItem(6, 88, R.drawable.img_plane_skin_6),
            new SkinItem(7, 6, R.drawable.img_plane_skin_7),
            new SkinItem(8, 8, R.drawable.img_plane_skin_8),
            new SkinItem(9, 6, R.drawable.img_plane_skin_9)
    };

    // 商店左右切换时需要知道皮肤总数。
    public static int getCount() {
        return SKINS.length;
    }

    // 根据索引获取皮肤，并处理左右循环越界。
    public static SkinItem getByIndex(int index) {
        int safeIndex = index;
        if (safeIndex < 0) {
            safeIndex = SKINS.length - 1;
        }
        if (safeIndex >= SKINS.length) {
            safeIndex = 0;
        }
        return SKINS[safeIndex];
    }

    // 根据皮肤 id 找到它在数组中的位置，用于打开商店时定位当前皮肤。
    public static int indexOfSkinId(int skinId) {
        for (int i = 0; i < SKINS.length; i++) {
            if (SKINS[i].skinId == skinId) {
                return i;
            }
        }
        return 0;
    }

    // 校验皮肤 id 是否存在，避免本地或服务器返回异常数据。
    public static boolean containsSkinId(int skinId) {
        for (SkinItem item : SKINS) {
            if (item.skinId == skinId) {
                return true;
            }
        }
        return false;
    }

    // 根据皮肤 id 找到实际 drawable，找不到时回退默认英雄机。
    public static int getSkinDrawableResId(int skinId) {
        for (SkinItem item : SKINS) {
            if (item.skinId == skinId) {
                return item.drawableResId;
            }
        }
        return R.drawable.hero;
    }
}
