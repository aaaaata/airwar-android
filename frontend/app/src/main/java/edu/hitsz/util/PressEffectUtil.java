package edu.hitsz.util;

import android.view.MotionEvent;
import android.view.View;

//按压缩放效果工具类
/*
 * 讲解重点：
 * PressEffectUtil 给按钮添加按压缩放和透明度变化，让图片按钮更像真实可点击控件。
 * 返回 false 是为了不拦截原来的 click 事件，OnClickListener 仍然会正常触发。
 */
public final class PressEffectUtil {

    private PressEffectUtil() {
    }

    // 批量给按钮/图片绑定按压动画。
    public static void apply(View... views) {
        if (views == null) return;

        for (View view : views) {
            if (view == null) continue;

            view.setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    // 按下时缩小并降低透明度，模拟按钮被按下。
                    case MotionEvent.ACTION_DOWN:
                        v.animate()
                                .scaleX(0.94f)
                                .scaleY(0.94f)
                                .alpha(0.85f)
                                .setDuration(70)
                                .start();
                        break;

                    // 松开或取消时恢复原尺寸和透明度。
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .alpha(1f)
                                .setDuration(90)
                                .start();
                        break;
                }
                return false;
            });
        }
    }
}
