package edu.hitsz.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import edu.hitsz.manager.BackgroundScrollManager;

/*
 * 讲解重点：
 * FlowBackgroundView 是 XML 页面中使用的滚动背景 View。
 * 它内部调用 BackgroundScrollManager，让菜单、排行榜、商店等非战斗页面共享同一套背景滚动状态。
 */
public class FlowBackgroundView extends View {

    // 使用全局背景管理器，保证不同页面的滚动背景状态连续。
    private final BackgroundScrollManager manager = BackgroundScrollManager.getInstance();
    private boolean running = false;

    public FlowBackgroundView(Context context) {
        super(context);
        init();
    }

    public FlowBackgroundView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FlowBackgroundView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    // 告诉 View 系统本控件需要执行 onDraw。
    private void init() {
        setWillNotDraw(false);
    }
    // View 加入窗口后开始刷新背景，并切换为 UI 场景速度。
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        running = true;
        manager.ensureInitialized();
        manager.setUiSpeed();
        manager.rebaseClock(System.nanoTime());
        postInvalidateOnAnimation();
    }
    // View 离开窗口后停止刷新，避免无意义重绘。
    @Override
    protected void onDetachedFromWindow() {
        running = false;
        super.onDetachedFromWindow();
    }
    // 每一帧更新背景位置并绘制，然后请求下一帧动画。
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        manager.step(System.nanoTime(), getHeight());
        manager.draw(canvas, getContext(), getWidth(), getHeight());

        if (running) {
            postInvalidateOnAnimation();
        }
    }
}
