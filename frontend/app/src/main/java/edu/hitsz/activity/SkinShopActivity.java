package edu.hitsz.activity;

/*
 * 说明：本文件为“讲解注释版”。
 * 注释只解释页面职责、核心流程和关键方法，不改变任何业务逻辑。
 */

import android.graphics.Color;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import edu.hitsz.R;
import edu.hitsz.manager.MenuBgmManager;
import edu.hitsz.manager.ImageManager;
import edu.hitsz.manager.SessionManager;
import edu.hitsz.manager.ShopStateManager;
import edu.hitsz.config.SkinConfig;
import edu.hitsz.manager.BackgroundScrollManager;
import edu.hitsz.util.FullScreenUtil;
import edu.hitsz.util.PressEffectUtil;
import edu.hitsz.widget.DigitTextView;
import okhttp3.OkHttpClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import edu.hitsz.config.ApiConfig;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 飞机皮肤商店页面。
 *
 * 作用：
 * 1. 展示当前金币、皮肤价格和当前预览皮肤；
 * 2. 支持左右切换不同皮肤；
 * 3. 根据皮肤是否拥有、是否已选中显示 BUY / SELECT / SELECTED 状态；
 * 4. 未登录时使用本地数据完成购买和选择；
 * 5. 登录时通过后端同步金币、已拥有皮肤和当前选中皮肤。
 *
 * 这个页面把游戏奖励金币和可解锁皮肤结合起来，属于扩展玩法模块。
 */
public class SkinShopActivity extends AppCompatActivity {
    // currentIndex 表示当前正在预览的皮肤下标。
    // ShopStateManager 管理本地金币、已拥有皮肤、当前选中皮肤；登录时会与后端同步。


    private DigitTextView digitCoinsValue;
    private DigitTextView digitPriceValue;

    private FrameLayout layoutPriceBox;
    private ImageView ivPricePossessed;
    private ImageView ivSkin;
    private ImageView ivArrowLeft;
    private ImageView ivArrowRight;
    private ImageView ivSelectAction;

    private int currentIndex = 0;

    private final OkHttpClient client = new OkHttpClient();

    /**
     * 初始化皮肤商店页面，读取当前选中皮肤，绑定左右切换和购买 / 选择按钮。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FullScreenUtil.hideSystemBars(this);

        BackgroundScrollManager.getInstance().ensureInitialized();
        setContentView(R.layout.activity_skin_shop);

        digitCoinsValue = findViewById(R.id.digitCoinsValue);
        digitPriceValue = findViewById(R.id.digitPriceValue);
        layoutPriceBox = findViewById(R.id.layoutPriceBox);
        ivPricePossessed = findViewById(R.id.ivPricePossessed);
        ivSkin = findViewById(R.id.ivSkin);
        ivArrowLeft = findViewById(R.id.ivArrowLeft);
        ivArrowRight = findViewById(R.id.ivArrowRight);
        ivSelectAction = findViewById(R.id.ivSelectAction);

        digitCoinsValue.setDigitTintColor(Color.WHITE);
        digitPriceValue.setDigitTintColor(Color.BLACK);

        currentIndex = SkinConfig.indexOfSkinId(
                ShopStateManager.getSelectedSkinId(this)
        );

        PressEffectUtil.apply(ivArrowLeft, ivArrowRight, ivSelectAction);

        ivArrowLeft.setOnClickListener(v -> {
            currentIndex--;
            if (currentIndex < 0) {
                currentIndex = SkinConfig.getCount() - 1;
            }
            renderCurrentSkin();
        });

        ivArrowRight.setOnClickListener(v -> {
            currentIndex++;
            if (currentIndex >= SkinConfig.getCount()) {
                currentIndex = 0;
            }
            renderCurrentSkin();
        });

        ivSelectAction.setOnClickListener(v -> handleSelectOrBuy());

        renderCurrentSkin();

        if (SessionManager.isLoggedIn(this)) {
            loadShopStatusFromServer();
        }
    }

    /**
     * 根据当前皮肤状态刷新金币、价格、皮肤图片和操作按钮。
     */
    private void renderCurrentSkin() {
        SkinConfig.SkinItem item = SkinConfig.getByIndex(currentIndex);

        int coins = ShopStateManager.getCoins(this);
        int selectedSkinId = ShopStateManager.getSelectedSkinId(this);

        boolean owned = ShopStateManager.isSkinOwned(this, item.skinId);
        boolean selected = owned && selectedSkinId == item.skinId;

        digitCoinsValue.setNumber(coins);
        digitPriceValue.setNumber(item.price);
        ivSkin.setImageResource(item.drawableResId);

        if (owned) {
            layoutPriceBox.setVisibility(android.view.View.GONE);
            ivPricePossessed.setVisibility(android.view.View.VISIBLE);
        } else {
            layoutPriceBox.setVisibility(android.view.View.VISIBLE);
            ivPricePossessed.setVisibility(android.view.View.GONE);
        }

        if (owned && selected) {
            ivSelectAction.setImageResource(R.drawable.img_shop_selected);
        } else if (owned) {
            ivSelectAction.setImageResource(R.drawable.img_shop_select);
        } else {
            ivSelectAction.setImageResource(R.drawable.img_shop_buy);
        }
    }

    /**
     * 登录状态下调用后端购买皮肤接口，并同步金币和拥有状态。
     */
    private void buySkinOnServer(int skinId, int price) {
        int userId = SessionManager.getUserId(this);

        HttpUrl baseUrl = HttpUrl.parse(ApiConfig.BASE_URL + "/shop/buy");
        if (baseUrl == null) {
            Toast.makeText(this, "服务器地址错误", Toast.LENGTH_SHORT).show();
            return;
        }

        HttpUrl url = baseUrl.newBuilder()
                .addQueryParameter("userId", String.valueOf(userId))
                .addQueryParameter("skinId", String.valueOf(skinId))
                .addQueryParameter("price", String.valueOf(price))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@androidx.annotation.NonNull Call call,
                                  @androidx.annotation.NonNull IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(SkinShopActivity.this,
                                "购买失败，请检查网络",
                                Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@androidx.annotation.NonNull Call call,
                                   @androidx.annotation.NonNull Response response) throws IOException {
                String body = response.body() == null
                        ? ""
                        : response.body().string().trim();

                try {
                    JSONObject obj = new JSONObject(body);
                    String status = obj.optString("status", "fail");

                    if ("not_enough".equals(status)) {
                        int coins = obj.optInt("coins", ShopStateManager.getCoins(SkinShopActivity.this));
                        ShopStateManager.setCoins(SkinShopActivity.this, coins);

                        runOnUiThread(() -> {
                            Toast.makeText(SkinShopActivity.this,
                                    "金币不够",
                                    Toast.LENGTH_SHORT).show();
                            renderCurrentSkin();
                        });
                        return;
                    }

                    if (!"success".equals(status)) {
                        runOnUiThread(() ->
                                Toast.makeText(SkinShopActivity.this,
                                        "购买失败",
                                        Toast.LENGTH_SHORT).show()
                        );
                        return;
                    }

                    int coins = obj.optInt("coins", ShopStateManager.getCoins(SkinShopActivity.this));
                    ShopStateManager.setCoins(SkinShopActivity.this, coins);
                    ShopStateManager.markSkinOwned(SkinShopActivity.this, skinId);

                    runOnUiThread(() -> renderCurrentSkin());

                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(SkinShopActivity.this,
                                    "购买结果格式错误",
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    /**
     * 登录状态下调用后端选择皮肤接口，并刷新英雄机图片资源。
     */
    private void selectSkinOnServer(int skinId) {
        int userId = SessionManager.getUserId(this);

        HttpUrl baseUrl = HttpUrl.parse(ApiConfig.BASE_URL + "/shop/select");
        if (baseUrl == null) {
            Toast.makeText(this, "服务器地址错误", Toast.LENGTH_SHORT).show();
            return;
        }

        HttpUrl url = baseUrl.newBuilder()
                .addQueryParameter("userId", String.valueOf(userId))
                .addQueryParameter("skinId", String.valueOf(skinId))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@androidx.annotation.NonNull Call call,
                                  @androidx.annotation.NonNull IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(SkinShopActivity.this,
                                "选择失败，请检查网络",
                                Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@androidx.annotation.NonNull Call call,
                                   @androidx.annotation.NonNull Response response) throws IOException {
                String body = response.body() == null
                        ? ""
                        : response.body().string().trim();

                try {
                    JSONObject obj = new JSONObject(body);
                    String status = obj.optString("status", "fail");

                    if (!"success".equals(status)) {
                        runOnUiThread(() ->
                                Toast.makeText(SkinShopActivity.this,
                                        "选择失败",
                                        Toast.LENGTH_SHORT).show()
                        );
                        return;
                    }

                    int selectedSkinId = obj.optInt("selectedSkinId", skinId);
                    ShopStateManager.setSelectedSkinId(SkinShopActivity.this, selectedSkinId);

                    runOnUiThread(() -> {
                        ImageManager.reloadHeroImage(SkinShopActivity.this);
                        renderCurrentSkin();
                    });

                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(SkinShopActivity.this,
                                    "选择结果格式错误",
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    /**
     * 游客或未登录状态下使用本地数据完成购买或选择。
     */
    private void handleSelectOrBuyLocal(SkinConfig.SkinItem item,
                                        boolean owned,
                                        int selectedSkinId) {
        if (owned && selectedSkinId == item.skinId) {
            return;
        }

        if (owned) {
            ShopStateManager.setSelectedSkinId(this, item.skinId);
            ImageManager.reloadHeroImage(this);
            renderCurrentSkin();
            return;
        }

        int coins = ShopStateManager.getCoins(this);
        if (coins < item.price) {
            Toast.makeText(this, "金币不够", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = ShopStateManager.spendCoins(this, item.price);
        if (!success) {
            Toast.makeText(this, "金币不够", Toast.LENGTH_SHORT).show();
            return;
        }

        ShopStateManager.markSkinOwned(this, item.skinId);
        renderCurrentSkin();
    }

    /**
     * 根据登录状态、是否已拥有和是否已选中，决定执行购买、选择或不处理。
     */
    private void handleSelectOrBuy() {
        SkinConfig.SkinItem item = SkinConfig.getByIndex(currentIndex);

        boolean owned = ShopStateManager.isSkinOwned(this, item.skinId);
        int selectedSkinId = ShopStateManager.getSelectedSkinId(this);

        if (owned && selectedSkinId == item.skinId) {
            return;
        }

        if (!SessionManager.isLoggedIn(this)) {
            handleSelectOrBuyLocal(item, owned, selectedSkinId);
            return;
        }

        if (owned) {
            selectSkinOnServer(item.skinId);
        } else {
            buySkinOnServer(item.skinId, item.price);
        }
    }

    /**
     * 回到商店页面时恢复全屏、播放菜单音乐并刷新当前皮肤状态。
     */
    @Override
    protected void onResume() {
        super.onResume();

        FullScreenUtil.hideSystemBars(this);

        MenuBgmManager.play(this);
        renderCurrentSkin();
    }

    /**
     * 离开商店页面时暂停菜单音乐。
     */
    @Override
    protected void onPause() {
        super.onPause();
        MenuBgmManager.pause();
    }

    /**
     * 登录用户进入商店时，从后端同步金币、已拥有皮肤和当前选中皮肤。
     */
    private void loadShopStatusFromServer() {
        int userId = SessionManager.getUserId(this);

        HttpUrl baseUrl = HttpUrl.parse(ApiConfig.BASE_URL + "/shop/status");
        if (baseUrl == null) {
            Toast.makeText(this, "服务器地址错误", Toast.LENGTH_SHORT).show();
            return;
        }

        HttpUrl url = baseUrl.newBuilder()
                .addQueryParameter("userId", String.valueOf(userId))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@androidx.annotation.NonNull Call call,
                                  @androidx.annotation.NonNull IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(SkinShopActivity.this,
                                "商店数据同步失败，暂用本地数据",
                                Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@androidx.annotation.NonNull Call call,
                                   @androidx.annotation.NonNull Response response) throws IOException {
                String body = response.body() == null
                        ? ""
                        : response.body().string().trim();

                try {
                    JSONObject obj = new JSONObject(body);
                    String status = obj.optString("status", "fail");

                    if (!"success".equals(status)) {
                        runOnUiThread(() ->
                                Toast.makeText(SkinShopActivity.this,
                                        "商店数据同步失败",
                                        Toast.LENGTH_SHORT).show()
                        );
                        return;
                    }

                    int coins = obj.optInt("coins", 0);
                    int selectedSkinId = obj.optInt("selectedSkinId", 1);

                    Set<Integer> ownedSet = new HashSet<>();
                    JSONArray array = obj.optJSONArray("ownedSkinIds");
                    if (array != null) {
                        for (int i = 0; i < array.length(); i++) {
                            ownedSet.add(array.optInt(i, 1));
                        }
                    }

                    ShopStateManager.saveShopState(
                            SkinShopActivity.this,
                            coins,
                            selectedSkinId,
                            ownedSet
                    );

                    currentIndex = SkinConfig.indexOfSkinId(selectedSkinId);

                    runOnUiThread(() -> {
                        ImageManager.reloadHeroImage(SkinShopActivity.this);
                        renderCurrentSkin();
                    });

                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(SkinShopActivity.this,
                                    "商店数据格式错误",
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }
}