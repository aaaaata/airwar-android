package edu.hitsz.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

//圆角工具类
/*
 * 讲解重点：
 * RoundCornerImageView 是支持圆角裁剪的 ImageView，用于头像、玩家图标等需要圆角显示的图片。
 * 它通过 clipPath 在绘制时裁剪出圆角区域，不需要为每张头像单独制作圆角图片。
 */
public class RoundCornerImageView extends AppCompatImageView {

    // path 保存圆角裁剪区域，rectF 保存控件当前矩形范围。
    private final Path path = new Path();
    private final RectF rectF = new RectF();
    private float cornerRadius = 18f;

    public RoundCornerImageView(Context context) {
        super(context);
        init();
    }

    public RoundCornerImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RoundCornerImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    // CENTER_CROP 保证头像铺满控件，同时超出部分会被裁剪。
    private void init() {
        setScaleType(ScaleType.CENTER_CROP);
    }

    // 外部可动态设置圆角半径，例如根据头像尺寸按比例设置。
    public void setCornerRadius(float radius) {
        this.cornerRadius = radius;
        rebuildPath();
        invalidate();
    }
    // 控件尺寸变化后重新计算圆角裁剪路径。
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rebuildPath();
    }

    // 根据当前宽高和圆角半径重建裁剪 Path。
    private void rebuildPath() {
        path.reset();
        rectF.set(0, 0, getWidth(), getHeight());
        path.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW);
        path.close();
    }
    // 先裁剪画布为圆角区域，再调用父类正常绘制图片。
    @Override
    protected void onDraw(Canvas canvas) {
        int saveCount = canvas.save();
        canvas.clipPath(path);
        super.onDraw(canvas);
        canvas.restoreToCount(saveCount);
    }
}
