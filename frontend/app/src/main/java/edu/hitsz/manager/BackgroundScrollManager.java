package edu.hitsz.manager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.SparseArray;

import edu.hitsz.R;

/*
 * BackgroundScrollManager 是全局滚动背景管理器，用单例保存背景当前滚动位置、当前图片、上方衔接图片以及循环序列。
 * 这样多个 Activity 切换时可以共享同一套滚动状态，避免返回菜单时背景突然从头开始。
 * 它还区分 UI 场景速度和游戏场景速度：菜单/排行榜较慢，战斗界面较快。
 */
public final class BackgroundScrollManager {

    // 单例：所有页面共用同一个背景滚动状态，保证切页时背景连续。
    private static final BackgroundScrollManager INSTANCE = new BackgroundScrollManager();

    public static BackgroundScrollManager getInstance() {
        return INSTANCE;
    }

    // Bitmap 缓存：避免每一帧重复解码图片，降低卡顿和内存抖动。
    private final SparseArray<Bitmap> bitmapCache = new SparseArray<>();

    // 当前屏幕中的那张图
    // 背景滚动由两张图拼接实现：current 在屏幕内，upper 从上方衔接进入。
    private int currentResId;
    // 当前屏幕上方、正在衔接进入的那张图
    private int upperResId;

    // 当前正在使用的循环队列
    private int[] loopResIds = new int[0];
    private int nextIndex = 0;

    // 延迟切换到新循环：必须等 current 变成 bg_13_2 后才真正切
    // 延迟切换标记：用于保证不同难度背景在指定节点后平滑切换，不会突然跳图。
    private boolean pendingLoopSwitch = false;
    private int pendingUpperResId = 0;
    private int[] pendingLoopResIds = null;

    // 当前滚动偏移：current 图顶部距离屏幕顶部的距离
    private float offsetPx = 0f;

    // 速度，可自己调
    private float speedPxPerSec = 100f;

    // UI 页面使用较慢速度，让菜单、排行榜背景更平缓。
    public synchronized void setUiSpeed() {
        this.speedPxPerSec = 100f;
    }

    // 战斗页面使用较快速度，增强游戏进行中的速度感。
    public synchronized void setGameSpeed() {
        this.speedPxPerSec = 400f;
    }

    private long lastFrameNanos = 0L;
    private boolean initialized = false;

    private BackgroundScrollManager() {
    }

    public synchronized boolean isInitialized() {
        return initialized;
    }

    // 初始菜单：屏幕里是第一张图，上方衔接 13_1
    // 初始化菜单背景状态：从第一张初始图开始，后续进入默认 13 系列循环。
    public synchronized void initForFirstMenu() {
        currentResId = R.drawable.bg_menu_initial;
        upperResId = R.drawable.bg_13_1;
        loopResIds = new int[]{
                R.drawable.bg_13_2,
                R.drawable.bg_13_1
        };
        nextIndex = 0;

        pendingLoopSwitch = false;
        pendingUpperResId = 0;
        pendingLoopResIds = null;

        offsetPx = 0f;
        lastFrameNanos = 0L;
        initialized = true;
    }

    public synchronized void ensureInitialized() {
        if (!initialized) {
            initForFirstMenu();
        }
    }

    // 默认循环：13_1 / 13_2
    public synchronized void useDefaultLoopKeepCurrent() {
        ensureInitialized();

        pendingLoopSwitch = false;
        pendingUpperResId = 0;
        pendingLoopResIds = null;

        upperResId = R.drawable.bg_13_1;
        loopResIds = new int[]{
                R.drawable.bg_13_2,
                R.drawable.bg_13_1
        };
        nextIndex = 0;
    }

    // 常规难度：
    // 不立刻切图，必须等到 bg_13_2 成为 current 后，
    // 才让它的下一张变成 bg_5_1，然后 5_2/5_1 循环
    // 普通难度背景切换：不立即换图，而是等当前 13 系列走到指定节点后切入 5 系列。
    public synchronized void useNormalLoopKeepCurrent() {
        ensureInitialized();
        scheduleLoopSwitchAfterBg13_2(
                R.drawable.bg_5_1,
                new int[]{
                        R.drawable.bg_5_2,
                        R.drawable.bg_5_1
                }
        );
    }

    // 困难难度：
    // 不立刻切图，必须等到 bg_13_2 成为 current 后，
    // 才让它的下一张变成 bg_2_1，然后 2_2/2_1 循环
    // 困难难度背景切换：同样延迟切换，但切入 2 系列背景。
    public synchronized void useHardLoopKeepCurrent() {
        ensureInitialized();
        scheduleLoopSwitchAfterBg13_2(
                R.drawable.bg_2_1,
                new int[]{
                        R.drawable.bg_2_2,
                        R.drawable.bg_2_1
                }
        );
    }

    private void scheduleLoopSwitchAfterBg13_2(int nextUpperResId, int[] nextLoopResIds) {
        pendingLoopSwitch = true;
        pendingUpperResId = nextUpperResId;
        pendingLoopResIds = nextLoopResIds;
    }

    public synchronized void resetToFirstMenu() {
        initForFirstMenu();
    }

    public synchronized void setSpeedPxPerSec(float speedPxPerSec) {
        this.speedPxPerSec = speedPxPerSec;
    }

    public synchronized void rebaseClock(long nowNanos) {
        lastFrameNanos = nowNanos;
    }

    // 根据时间差更新滚动偏移；当偏移超过屏幕高度时，完成一次图片交接。
    public synchronized void step(long nowNanos, int screenHeight) {
        if (!initialized || screenHeight <= 0) {
            return;
        }

        if (lastFrameNanos == 0L) {
            lastFrameNanos = nowNanos;
            return;
        }

        float dt = (nowNanos - lastFrameNanos) / 1_000_000_000f;
        lastFrameNanos = nowNanos;

        if (dt < 0f) dt = 0f;
        if (dt > 0.05f) dt = 0.05f; // 防止切页时突然跳太多

        offsetPx += speedPxPerSec * dt;

        while (offsetPx >= screenHeight) {
            offsetPx -= screenHeight;

            // 上方图片进入主屏
            currentResId = upperResId;

            // 只有当 bg_13_2 真正进入主屏之后，才开始切新素材循环
            if (pendingLoopSwitch && currentResId == R.drawable.bg_13_2) {
                upperResId = pendingUpperResId;
                loopResIds = pendingLoopResIds != null ? pendingLoopResIds : new int[0];
                nextIndex = 0;

                pendingLoopSwitch = false;
                pendingUpperResId = 0;
                pendingLoopResIds = null;
            } else {
                if (loopResIds.length > 0) {
                    upperResId = loopResIds[nextIndex];
                    nextIndex = (nextIndex + 1) % loopResIds.length;
                }
            }
        }
    }

    // 把 current 和 upper 两张图分别画到屏幕内和屏幕上方，实现无缝向下滚动。
    public synchronized void draw(Canvas canvas, Context context, int width, int height) {
        if (!initialized || width <= 0 || height <= 0) {
            return;
        }

        Bitmap current = getBitmap(context, currentResId);
        Bitmap upper = getBitmap(context, upperResId);

        int currentTop = Math.round(offsetPx);

        Rect dstCurrent = new Rect(0, currentTop, width, currentTop + height);
        Rect dstUpper = new Rect(0, currentTop - height, width, currentTop);

        canvas.drawBitmap(current, null, dstCurrent, null);
        canvas.drawBitmap(upper, null, dstUpper, null);
    }

    // 从缓存取图；没有缓存时才解码资源，避免频繁创建 Bitmap。
    private Bitmap getBitmap(Context context, int resId) {
        Bitmap bmp = bitmapCache.get(resId);
        if (bmp == null || bmp.isRecycled()) {
            bmp = BitmapFactory.decodeResource(context.getResources(), resId);
            bitmapCache.put(resId, bmp);
        }
        return bmp;
    }


}
