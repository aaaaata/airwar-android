package edu.hitsz.application;

/*
 * Main 在安卓项目中不再作为程序入口，只保留窗口宽高常量，兼容旧代码中对 Main.WINDOW_WIDTH / WINDOW_HEIGHT 的引用。
 * 真正的入口已经迁移到 AndroidManifest 中声明的 Activity。
 */
/**
 * 安卓迁移后的兼容常量类。
 *
 * 注意：
 * 1. 这个类不再是程序入口；
 * 2. 真正入口是 MainActivity；
 * 3. 之所以保留 Main，是为了兼容 BossEnemy、AbstractFlyingObject 等旧类里
 *    对 Main.WINDOW_WIDTH / Main.WINDOW_HEIGHT 的依赖。
 */
public final class Main {

    // 旧代码兼容宽高：SurfaceView 真正尺寸确定后会在 Game.surfaceChanged 中更新。
    public static int WINDOW_WIDTH = 512;
    public static int WINDOW_HEIGHT = 768;

    private Main() {
    }
}
