package edu.hitsz.application;

import android.view.MotionEvent;
import android.view.View;

import edu.hitsz.aircraft.HeroAircraft;

/*
 * 讲解重点：
 * HeroController 负责把 Android 触摸事件转换成英雄机坐标更新。
 * 原桌面端通常使用鼠标监听，移植到 Android 后改为 OnTouchListener，并用 clamp 限制英雄机不超出屏幕范围。
 */
/**
 * 英雄机控制类。
 *
 * 将原来的鼠标拖拽监听改成触屏监听。
 */
public class HeroController implements View.OnTouchListener {

    private final Game game;
    private final HeroAircraft heroAircraft;

    // 构造时直接把自己注册成 Game 视图的触摸监听器。
    public HeroController(Game game, HeroAircraft heroAircraft) {
        this.game = game;
        this.heroAircraft = heroAircraft;
        game.setOnTouchListener(this);
    }
    // ACTION_DOWN / ACTION_MOVE 都更新英雄机位置，实现手指按下和拖动控制。
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                int maxWidth = game.getScreenWidth() > 0 ? game.getScreenWidth() : Main.WINDOW_WIDTH;
                int maxHeight = game.getScreenHeight() > 0 ? game.getScreenHeight() : Main.WINDOW_HEIGHT;

                int x = clamp((int) event.getX(), 0, maxWidth);
                int y = clamp((int) event.getY(), 0, maxHeight);
                heroAircraft.setLocation(x, y);
                return true;
            default:
                return true;
        }
    }

    // 边界限制：防止触摸坐标越界导致飞机跑出屏幕。
    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
