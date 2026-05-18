package edu.hitsz.util;

import android.graphics.PorterDuff;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

//按压变灰效果工具类
/*
 * 讲解重点：
 * GrayPressEffectUtil 给图片按钮添加“按下变灰”的反馈效果，适合不希望按钮缩放、只希望颜色变化的界面元素。
 * 它通过 ImageView 的 ColorFilter 实现，不改变控件实际尺寸。
 */
public class GrayPressEffectUtil {

    // 给一个或多个 ImageView 批量绑定按压变灰效果。
    public static void apply(ImageView... views) {
        if (views == null) {
            return;
        }

        for (ImageView view : views) {
            if (view == null) {
                continue;
            }

            view.setOnTouchListener((v, event) -> {
                ImageView imageView = (ImageView) v;

                switch (event.getAction()) {
                    // 手指按下时叠加半透明黑色滤镜，形成变灰反馈。
                    case MotionEvent.ACTION_DOWN:
                        imageView.setColorFilter(0x44000000, PorterDuff.Mode.SRC_ATOP);
                        break;

                    // 手指松开或事件取消时清除滤镜，恢复原图。
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        imageView.clearColorFilter();
                        break;
                }

                return false;
            });
        }
    }
}
