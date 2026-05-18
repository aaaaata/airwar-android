package edu.hitsz.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/*
 * 讲解重点：
 * DigitTextView 是自定义数字控件，用 0~9 的 PNG 图片绘制数字，保持像素风 UI 风格。
 * 它不是每个数字创建一个 ImageView，而是在一个 View 的 onDraw 中直接画 Bitmap，因此性能更稳定、布局更简单。
 */
/**
 * 用 0~9 的 PNG 显示数字。
 *
 * 资源命名要求：
 * drawable/img_digit_0.png ... drawable/img_digit_9.png
 * 可选：drawable/img_digit_dash.png，用于显示 --- 或负号。
 *
 * 本控件只占一个 View，不会给每个数字创建 ImageView。
 * 为了避免像素风数字被放大后发糊，drawBitmap 时关闭了线性过滤。
 */
public class DigitTextView extends View {

    // bitmapPaint 用于绘制数字图片；fallbackPaint 用于缺少资源时绘制普通文字兜底。
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fallbackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Map<Character, Bitmap> bitmapCache = new HashMap<>();

    // 当前要显示的内容，可以是数字，也可以是 --- 这类占位符。
    private String textValue = "";
    private int charSpacingPx;
    private Integer digitTintColor = null;

    public DigitTextView(Context context) {
        super(context);
        init();
    }

    public DigitTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DigitTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    // 初始化绘制参数：关闭图片过滤以保留像素风锐利边缘。
    private void init() {
        charSpacingPx = dp(1.5f);

        bitmapPaint.setFilterBitmap(false);
        bitmapPaint.setDither(false);

        fallbackPaint.setColor(0xFFE1EAF0);
        fallbackPaint.setTextAlign(Paint.Align.LEFT);
        fallbackPaint.setFakeBoldText(true);
    }

    // 显示普通 int 数字，负数统一按 0 处理。
    public void setNumber(int number) {
        setTextValue(String.valueOf(Math.max(number, 0)));
    }

    public void setLongNumber(long number) {
        setTextValue(String.valueOf(Math.max(number, 0)));
    }

    // 设置显示文本后重新测量和重绘。
    public void setTextValue(String value) {
        if (value == null) {
            value = "";
        }
        textValue = value;
        requestLayout();
        invalidate();
    }

    public String getTextValue() {
        return textValue;
    }

    public void setCharSpacingPx(int charSpacingPx) {
        this.charSpacingPx = Math.max(0, charSpacingPx);
        requestLayout();
        invalidate();
    }

    /**
     * 给数字 PNG 统一染色。
     * 适合黑色数字透明底的素材，例如单机游戏 HUD 需要显示白色数字。
     * 如果调用 clearDigitTintColor()，则恢复 PNG 原始颜色。
     */
    // 给数字图片染色，例如 HUD 中把黑色数字统一转成白色。
    public void setDigitTintColor(int color) {
        this.digitTintColor = color;
        bitmapPaint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        fallbackPaint.setColor(color);
        invalidate();
    }

    public void clearDigitTintColor() {
        this.digitTintColor = null;
        bitmapPaint.setColorFilter(null);
        fallbackPaint.setColor(0xFFE1EAF0);
        invalidate();
    }
    // 根据高度和每个数字图片的宽高比，计算控件自然宽度。
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int desiredHeight;
        if (heightMode == MeasureSpec.EXACTLY) {
            desiredHeight = heightSize;
        } else {
            desiredHeight = dp(20);
            if (heightMode == MeasureSpec.AT_MOST) {
                desiredHeight = Math.min(desiredHeight, heightSize);
            }
        }

        int desiredWidth = Math.round(calculateNaturalWidth(desiredHeight))
                + getPaddingStart() + getPaddingEnd();

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int finalWidth;
        if (widthMode == MeasureSpec.EXACTLY) {
            finalWidth = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            finalWidth = Math.min(desiredWidth, widthSize);
        } else {
            finalWidth = desiredWidth;
        }

        setMeasuredDimension(finalWidth, desiredHeight);
    }
    // 逐字符绘制数字图片；空间不足时会整体按比例缩小以适配宽度。
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (textValue == null || textValue.length() == 0) {
            return;
        }

        int availableWidth = Math.max(0, getWidth() - getPaddingStart() - getPaddingEnd());
        int availableHeight = Math.max(0, getHeight() - getPaddingTop() - getPaddingBottom());
        if (availableWidth <= 0 || availableHeight <= 0) {
            return;
        }

        float naturalWidth = calculateNaturalWidth(availableHeight);
        float scale = naturalWidth > availableWidth ? availableWidth / naturalWidth : 1f;
        float drawHeight = availableHeight * scale;
        float startX = getPaddingStart();
        float startY = getPaddingTop() + (availableHeight - drawHeight) / 2f;

        float x = startX;
        for (int i = 0; i < textValue.length(); i++) {
            char c = textValue.charAt(i);

            if (c == ' ') {
                x += drawHeight * 0.35f;
                continue;
            }

            Bitmap bitmap = getBitmapForChar(c);
            if (bitmap != null && bitmap.getHeight() > 0) {
                float ratio = bitmap.getWidth() / (float) bitmap.getHeight();
                float charWidth = drawHeight * ratio;
                RectF dst = new RectF(x, startY, x + charWidth, startY + drawHeight);
                canvas.drawBitmap(bitmap, null, dst, bitmapPaint);
                x += charWidth + charSpacingPx * scale;
            } else {
                fallbackPaint.setTextSize(drawHeight * 0.86f);
                canvas.drawText(String.valueOf(c), x, startY + drawHeight * 0.84f, fallbackPaint);
                x += fallbackPaint.measureText(String.valueOf(c)) + charSpacingPx * scale;
            }
        }
    }

    // 预估指定高度下整串字符所需宽度，用于测量和缩放。
    private float calculateNaturalWidth(float height) {
        if (textValue == null || textValue.length() == 0 || height <= 0) {
            return 0;
        }

        float width = 0;
        int charCount = 0;

        for (int i = 0; i < textValue.length(); i++) {
            char c = textValue.charAt(i);
            if (c == ' ') {
                width += height * 0.35f;
                continue;
            }

            Bitmap bitmap = getBitmapForChar(c);
            if (bitmap != null && bitmap.getHeight() > 0) {
                width += height * bitmap.getWidth() / (float) bitmap.getHeight();
            } else {
                width += height * 0.58f;
            }
            charCount++;
        }

        if (charCount > 1) {
            width += charSpacingPx * (charCount - 1);
        }
        return width;
    }

    @Nullable
    // 根据字符找到对应图片资源，并缓存到 Map 中避免重复解码。
    private Bitmap getBitmapForChar(char c) {
        if (bitmapCache.containsKey(c)) {
            return bitmapCache.get(c);
        }

        String name;
        if (c >= '0' && c <= '9') {
            name = "img_digit_" + c;
        } else if (c == '-') {
            name = "img_digit_dash";
        } else {
            bitmapCache.put(c, null);
            return null;
        }

        int resId = getResources().getIdentifier(name, "drawable", getContext().getPackageName());
        if (resId == 0) {
            bitmapCache.put(c, null);
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId, options);
        bitmapCache.put(c, bitmap);
        return bitmap;
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        ));
    }
}
