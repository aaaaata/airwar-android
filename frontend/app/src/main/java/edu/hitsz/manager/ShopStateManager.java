package edu.hitsz.manager;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

import edu.hitsz.config.SkinConfig;

/*
 * ShopStateManager 负责本地保存商店状态，包括金币数量、当前选择的皮肤、已拥有皮肤集合。
 * 它按用户 id 做 key 前缀，因此不同账号和游客状态之间的金币/皮肤数据不会混在一起。
 */
public final class ShopStateManager {

    // 商店状态单独存一个 SharedPreferences 文件，和登录信息分离。
    private static final String PREF_NAME = "airwar_shop_state";

    private static final String KEY_COINS = "_coins";
    private static final String KEY_SELECTED_SKIN = "_selected_skin";
    private static final String KEY_OWNED_SKINS = "_owned_skins";

    private static final int DEFAULT_SKIN_ID = 1;

    private ShopStateManager() {
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // 不同账号使用不同 key 前缀；未登录时使用 guest，避免数据互相覆盖。
    private static String getUserPrefix(Context context) {
        if (SessionManager.isLoggedIn(context)) {
            return "user_" + SessionManager.getUserId(context);
        }
        return "guest";
    }

    // 读取当前用户金币数。
    public static int getCoins(Context context) {
        String prefix = getUserPrefix(context);
        return getPrefs(context).getInt(prefix + KEY_COINS, 0);
    }

    // 游戏结束获得金币后调用，游客模式也会本地累加。
    public static void addCoins(Context context, int amount) {
        if (amount <= 0) {
            return;
        }

        String prefix = getUserPrefix(context);
        int oldCoins = getCoins(context);
        getPrefs(context).edit()
                .putInt(prefix + KEY_COINS, oldCoins + amount)
                .apply();
    }

    // 本地扣除金币；金币不足时返回 false。
    public static boolean spendCoins(Context context, int amount) {
        if (amount <= 0) {
            return true;
        }

        String prefix = getUserPrefix(context);
        int oldCoins = getCoins(context);

        if (oldCoins < amount) {
            return false;
        }

        getPrefs(context).edit()
                .putInt(prefix + KEY_COINS, oldCoins - amount)
                .apply();

        return true;
    }

    // 读取当前选中的皮肤，如果本地数据异常则回退到默认皮肤。
    public static int getSelectedSkinId(Context context) {
        String prefix = getUserPrefix(context);
        int skinId = getPrefs(context).getInt(prefix + KEY_SELECTED_SKIN, DEFAULT_SKIN_ID);

        if (!SkinConfig.containsSkinId(skinId)) {
            return DEFAULT_SKIN_ID;
        }

        return skinId;
    }

    // 切换皮肤前先确认该皮肤已拥有，防止选择未购买皮肤。
    public static void setSelectedSkinId(Context context, int skinId) {
        if (!SkinConfig.containsSkinId(skinId)) {
            skinId = DEFAULT_SKIN_ID;
        }

        if (!isSkinOwned(context, skinId)) {
            return;
        }

        String prefix = getUserPrefix(context);
        getPrefs(context).edit()
                .putInt(prefix + KEY_SELECTED_SKIN, skinId)
                .apply();
    }

    // 默认皮肤永远视为已拥有，其余皮肤从拥有集合中判断。
    public static boolean isSkinOwned(Context context, int skinId) {
        if (skinId == DEFAULT_SKIN_ID) {
            return true;
        }

        Set<Integer> ownedSet = getOwnedSkinSet(context);
        return ownedSet.contains(skinId);
    }

    // 购买成功后把皮肤加入本地拥有集合。
    public static void markSkinOwned(Context context, int skinId) {
        if (!SkinConfig.containsSkinId(skinId)) {
            return;
        }

        Set<Integer> ownedSet = getOwnedSkinSet(context);
        ownedSet.add(DEFAULT_SKIN_ID);
        ownedSet.add(skinId);
        saveOwnedSkinSet(context, ownedSet);
    }

    // 把 SharedPreferences 中的逗号字符串解析成皮肤 id 集合。
    private static Set<Integer> getOwnedSkinSet(Context context) {
        String prefix = getUserPrefix(context);
        String value = getPrefs(context).getString(prefix + KEY_OWNED_SKINS, String.valueOf(DEFAULT_SKIN_ID));

        Set<Integer> set = new HashSet<>();
        set.add(DEFAULT_SKIN_ID);

        if (value == null || value.trim().isEmpty()) {
            return set;
        }

        String[] parts = value.split(",");
        for (String part : parts) {
            try {
                int skinId = Integer.parseInt(part.trim());
                if (SkinConfig.containsSkinId(skinId)) {
                    set.add(skinId);
                }
            } catch (Exception ignored) {
            }
        }

        return set;
    }

    // 把皮肤 id 集合重新序列化为逗号字符串保存。
    private static void saveOwnedSkinSet(Context context, Set<Integer> set) {
        StringBuilder builder = new StringBuilder();

        boolean first = true;
        for (Integer skinId : set) {
            if (skinId == null || !SkinConfig.containsSkinId(skinId)) {
                continue;
            }

            if (!first) {
                builder.append(",");
            }

            builder.append(skinId);
            first = false;
        }

        String prefix = getUserPrefix(context);
        getPrefs(context).edit()
                .putString(prefix + KEY_OWNED_SKINS, builder.toString())
                .apply();
    }

    public static void setCoins(Context context, int coins) {
        if (coins < 0) {
            coins = 0;
        }

        String prefix = getUserPrefix(context);
        getPrefs(context).edit()
                .putInt(prefix + KEY_COINS, coins)
                .apply();
    }

    public static void saveOwnedSkinIds(Context context, Set<Integer> ownedSet) {
        if (ownedSet == null) {
            ownedSet = new HashSet<>();
        }

        ownedSet.add(DEFAULT_SKIN_ID);
        saveOwnedSkinSet(context, ownedSet);
    }

    // 服务器同步商店状态后，一次性覆盖本地金币、选中皮肤和拥有皮肤集合。
    public static void saveShopState(Context context,
                                     int coins,
                                     int selectedSkinId,
                                     Set<Integer> ownedSet) {
        if (coins < 0) {
            coins = 0;
        }

        if (!SkinConfig.containsSkinId(selectedSkinId)) {
            selectedSkinId = DEFAULT_SKIN_ID;
        }

        if (ownedSet == null) {
            ownedSet = new HashSet<>();
        }

        ownedSet.add(DEFAULT_SKIN_ID);

        String prefix = getUserPrefix(context);

        getPrefs(context).edit()
                .putInt(prefix + KEY_COINS, coins)
                .putInt(prefix + KEY_SELECTED_SKIN, selectedSkinId)
                .apply();

        saveOwnedSkinSet(context, ownedSet);
    }
}
