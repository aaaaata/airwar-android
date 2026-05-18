package edu.hitsz.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

/*
 * 讲解重点：
 * UiInsetUtil 用统一像素值设置四边 margin，解决 Android 中 mm 单位在不同方向换算不完全一致导致边框看起来不等宽的问题。
 * 这里通过 xdpi 和 ydpi 的平均值计算 px，保证视觉上的四边 inset 更统一。
 */
public final class UiInsetUtil {

    private UiInsetUtil() {
    }

    /**
     * 用同一个像素值设置四边 inset，避免 mm 在横纵方向换算不一致。
     */
    // 将 mm 转成统一 px 后设置到四边 margin，保证视觉边距一致。
    public static void applyUniformInsetMm(View target, Context context, float mm) {
        if (target == null || context == null) return;

        // DisplayMetrics 中包含屏幕横向/纵向 dpi，用于进行物理尺寸换算。
        DisplayMetrics dm = context.getResources().getDisplayMetrics();

        // 取 xdpi 和 ydpi 的平均值，算出一个统一 px
        float dpi = (dm.xdpi + dm.ydpi) / 2f;
        int px = Math.round(mm * dpi / 25.4f);

        ViewGroup.LayoutParams lp = target.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
            mlp.setMargins(px, px, px, px);
            target.setLayoutParams(mlp);
        }
    }
}
