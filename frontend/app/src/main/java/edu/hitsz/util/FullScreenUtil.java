package edu.hitsz.util;

import android.app.Activity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/*
 * 讲解重点：
 * FullScreenUtil 统一隐藏系统状态栏，让游戏界面获得更完整的沉浸式显示空间。
 * 所有 Activity 在 onCreate / onResume 中调用它，避免切换页面后状态栏重新出现。
 */
public final class FullScreenUtil {

    private FullScreenUtil() {
    }

    // 隐藏状态栏：先判空保证工具方法在异常场景下不会导致闪退。
    public static void hideSystemBars(Activity activity) {
        if (activity == null) {
            return;
        }

        Window window = activity.getWindow();
        if (window == null) {
            return;
        }

        // 设置 FLAG_FULLSCREEN，隐藏顶部状态栏。
        window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        View decorView = window.getDecorView();
        if (decorView != null) {
            // 配合系统 UI 标志，让内容区域保持稳定布局。
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }
}
