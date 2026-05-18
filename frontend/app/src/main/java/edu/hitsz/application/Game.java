package edu.hitsz.application;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.AbstractEnemy;
import edu.hitsz.aircraft.BossEnemy;
import edu.hitsz.aircraft.EliteEnemy;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.aircraft.MobEnemy;
import edu.hitsz.aircraft.SuperEliteEnemy;
import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.config.GameConfig;
import edu.hitsz.factory.BossFactory;
import edu.hitsz.factory.EliteFactory;
import edu.hitsz.factory.EnemyFactory;
import edu.hitsz.factory.MobFactory;
import edu.hitsz.factory.SuperEliteFactory;
import edu.hitsz.manager.ImageManager;
import edu.hitsz.manager.SoundManager;
import edu.hitsz.observer.BombObserver;
import edu.hitsz.prop.AbstractProp;
import edu.hitsz.prop.PropBomb;
import edu.hitsz.prop.PropCoin;
import edu.hitsz.manager.BackgroundScrollManager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/*
 * 讲解重点：
 * Game 是安卓端战斗场景的核心类，负责游戏循环、SurfaceView 绘制、敌机/子弹/道具更新、碰撞检测、得分结算和游戏结束回调。
 * 它把原 Java/Swing 版本的绘制与鼠标交互改造成 Android 的 SurfaceView + Canvas + 触屏控制，属于移植工作中最核心的代码。
 * Activity 不直接写游戏循环，而是把 Game 作为一个自定义 View 放到容器中运行。
 */
/**
 * 安卓版游戏主视图。
 *
 * 1. BaseGame 重构为 SurfaceView；
 * 2. 输入交互改为触屏；
 * 3. 图片改为 Bitmap；
 * 4. 暂不实现 Swing 排行榜/难度选择页面；
 * 5. 不引入 GameConfig 和 GameOverListener。
 */
public abstract class Game extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    // 全局背景管理器：战斗时负责以更快速度绘制滚动背景。
    private final BackgroundScrollManager backgroundScrollManager = BackgroundScrollManager.getInstance();
    private static final String TAG = "Game";

    private final Context context;
    private final SurfaceHolder holder;

    // 渲染线程：SurfaceView 通常在独立线程中循环更新和绘制，避免阻塞主线程。
    private Thread renderThread;
    private volatile boolean isDrawing = false;

    // 当前游戏难度，用于游戏结束时传回 Activity 和排行榜页面。
    private String difficulty;
    private boolean soundEnabled;

    private boolean bossExists = false;
    private final int timeInterval = 20;

    // 游戏对象集合：英雄机、敌机、子弹和道具都由 Game 主循环统一更新。
    private final HeroAircraft heroAircraft;
    private final List<AbstractAircraft> enemyAircrafts;
    private final List<BaseBullet> heroBullets;
    private final List<BaseBullet> enemyBullets;
    private final List<AbstractProp> props;

    // 下面这些 protected 参数由 Easy/Normal/Hard 子类在 initDifficulty 中设置。
    protected int enemyMaxNumber;
    protected int bossScoreThreshold;
    protected int cycleDuration;
    protected int eliteCycleDuration;
    protected double eliteProbability;
    protected double superEliteProbability;
    protected double hpMultiplier = 1.0;
    protected int bossBaseHp;

    // 运行状态数据：分数、金币、计时器、游戏是否结束等。
    private int score = 0;
    private int coinsGot = 0;
    private boolean coinEnabled = true;
    private int time = 0;
    private int cycleTime = 0;
    private int eliteCycleTime = 0;
    private boolean gameOverFlag = false;

    private int screenWidth = Main.WINDOW_WIDTH;
    private int screenHeight = Main.WINDOW_HEIGHT;
    private final Paint gameOverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gameOverBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap gameOverBitmap;

    // 战斗音频管理器，负责游戏 BGM、Boss BGM 和各类短音效。
    private final SoundManager soundManager;
    private boolean gameOverSoundPlayed = false;

    // Activity 传入的 Handler。Game 结束时通过它把分数和金币回传给页面层。
    private Handler mainHandler;
    private boolean gameOverMessageSent = false;

    /**
     * 空间网格大小，用于优化英雄子弹与敌机的碰撞检测。
     * 数值越小，网格越密，筛选越精细，但维护成本更高；
     * 数值越大，网格越粗，筛选效果下降。
     */
    private static final int GRID_SIZE = 120;

    private List<AbstractAircraft>[] enemyGrid;
    private int gridCols;
    private int gridRows;

    // 构造函数完成 SurfaceView、资源、英雄机、集合、触摸控制和难度参数初始化。
    public Game(Context context, String difficulty, boolean soundEnabled) {
        super(context);
        this.context = context;
        this.difficulty = difficulty;
        this.soundEnabled = soundEnabled;
        this.soundManager = new SoundManager(context, soundEnabled);

        this.holder = getHolder();
        this.holder.addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setKeepScreenOn(true);

        ImageManager.init(context);
        loadGameOverBitmap();
        backgroundScrollManager.ensureInitialized();

        heroAircraft = HeroAircraft.getInstance();
        heroAircraft.setHp(1000);
        heroAircraft.setLocation(Main.WINDOW_WIDTH / 2.0, Main.WINDOW_HEIGHT * 0.8);

        enemyAircrafts = new ArrayList<>(16);
        heroBullets = new ArrayList<>(128);
        enemyBullets = new ArrayList<>(128);
        props = new ArrayList<>(32);

        new HeroController(this, heroAircraft);
        initDifficulty();
        initPaints();
    }

    // 加载 GAME OVER 图片；如果资源不存在，绘制阶段会使用文字兜底。
    private void loadGameOverBitmap() {
        int resId = getResources().getIdentifier(
                "img_game_over",
                "drawable",
                context.getPackageName()
        );

        if (resId != 0) {
            gameOverBitmap = BitmapFactory.decodeResource(getResources(), resId);
        }
    }

    // 初始化 GAME OVER 兜底文字和图片绘制参数。
    private void initPaints() {

        gameOverPaint.setColor(Color.RED);
        gameOverPaint.setTextSize(72f);
        gameOverPaint.setFakeBoldText(true);
        gameOverPaint.setTextAlign(Paint.Align.CENTER);
        gameOverBitmapPaint.setFilterBitmap(false);
        gameOverBitmapPaint.setDither(false);
    }

    // 模板方法：具体难度由子类提供，父类只负责统一调用。
    protected abstract void initDifficulty();

    protected abstract boolean hasBoss();

    protected abstract boolean increaseDifficulty();

    protected abstract void doIncreaseDifficulty();

    protected abstract int getBossHp();

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    // Activity 调用的启动入口：Surface 已可用时启动游戏线程。
    public void action() {
        if (!isDrawing && holder.getSurface().isValid()) {
            surfaceCreated(holder);
        }
    }
    // Surface 创建后启动渲染线程、设置战斗背景速度并播放战斗 BGM。
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (renderThread != null && renderThread.isAlive()) {
            return;
        }

        backgroundScrollManager.setGameSpeed();
        backgroundScrollManager.rebaseClock(System.nanoTime());
        soundManager.playGameBgm();

        isDrawing = true;
        renderThread = new Thread(this, "aircraft-war-render");
        renderThread.start();
    }
    // Surface 尺寸变化时更新游戏宽高，并同步到 Main 兼容常量。
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenWidth = width > 0 ? width : Main.WINDOW_WIDTH;
        screenHeight = height > 0 ? height : Main.WINDOW_HEIGHT;

        Main.WINDOW_WIDTH = screenWidth;
        Main.WINDOW_HEIGHT = screenHeight;

        heroAircraft.setLocation(screenWidth / 2.0, screenHeight * 0.8);
    }
    // Surface 销毁时停止绘制线程并暂停音频，避免页面切换后继续运行。
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isDrawing = false;
        if (renderThread != null) {
            try {
                renderThread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 这里只暂停，不释放
        soundManager.pauseAll();
    }
    // 游戏主循环：固定时间间隔更新逻辑并绘制一帧。
    @Override
    public void run() {
        while (isDrawing) {
            long frameStart = System.currentTimeMillis();

            if (!gameOverFlag) {
                updateGame();
            }
            drawFrame();

            if (gameOverFlag) {
                SystemClock.sleep(1000);
                isDrawing = false;
                continue;
            }

            long cost = System.currentTimeMillis() - frameStart;
            long sleepTime = timeInterval - cost;
            if (sleepTime > 0) {
                SystemClock.sleep(sleepTime);
            }
        }
    }

    // 单帧逻辑更新：增难、生成敌机、射击、移动、碰撞、清理和游戏结束判断都在这里串起来。
    private void updateGame() {
        time += timeInterval;

        if (increaseDifficulty() && time % 10000 == 0) {
            doIncreaseDifficulty();
        }

        if (timeCountAndNewCycleJudge()) {
            if (enemyAircrafts.size() < enemyMaxNumber) {
                createRandomEnemy();
            }
            shootAction();
        }

        eliteCycleTime += timeInterval;
        if (eliteCycleTime >= eliteCycleDuration) {
            eliteCycleTime %= eliteCycleDuration;
            eliteShootAction();
        }

        bulletsMoveAction();
        aircraftsMoveAction();
        propsMoveAction();
        crashCheckAction();
        postProcessAction();

        if (heroAircraft.getHp() <= 0) {
            gameOverFlag = true;

            if (!gameOverSoundPlayed) {
                soundManager.stopAllBgm();
                soundManager.playGameOver();
                gameOverSoundPlayed = true;
            }

            Log.i(TAG, "Game Over, difficulty=" + difficulty + ", score=" + score);

            if (!gameOverMessageSent && mainHandler != null) {
                Message msg = Message.obtain();
                msg.what = GameConfig.MSG_GAME_OVER;

                Bundle bundle = new Bundle();
                bundle.putString(GameConfig.EXTRA_DIFFICULTY, difficulty);
                bundle.putInt(GameConfig.EXTRA_SCORE, score);
                bundle.putInt(GameConfig.EXTRA_COINS_GOT, coinsGot);
                msg.setData(bundle);

                // 稍微延迟一点，先让 GAME OVER 和结束音效出来
                mainHandler.sendMessageDelayed(msg, 1200);

                gameOverMessageSent = true;
            }
        }
    }

    // 根据 cycleDuration 判断是否进入新的生成/射击周期。
    private boolean timeCountAndNewCycleJudge() {
        cycleTime += timeInterval;
        if (cycleTime >= cycleDuration) {
            cycleTime %= cycleDuration;
            return true;
        }
        return false;
    }

    // 按概率创建普通敌机、精英敌机或超级精英敌机；达到分数阈值时优先创建 Boss。
    private void createRandomEnemy() {
        if (hasBoss() && score >= bossScoreThreshold && !bossExists) {
            createBoss();
            return;
        }

        EnemyFactory enemyFactory;
        AbstractEnemy enemy;
        double random = Math.random();
        double mobProbability = 1.0 - eliteProbability - superEliteProbability;

        if (random < mobProbability) {
            enemyFactory = new MobFactory();
            enemy = enemyFactory.createEnemy();
        } else if (random < mobProbability + eliteProbability) {
            enemyFactory = new EliteFactory();
            enemy = enemyFactory.createEnemy();
        } else {
            enemyFactory = new SuperEliteFactory();
            enemy = enemyFactory.createEnemy();
        }

        enemy.setHp((int) (enemy.getHp() * hpMultiplier));
        enemyAircrafts.add(enemy);
    }

    // 创建 Boss，设置血量，提高下一次 Boss 出现阈值，并切换 Boss BGM。
    private void createBoss() {
        EnemyFactory bossFactory = new BossFactory();
        AbstractEnemy boss = bossFactory.createEnemy();
        boss.setHp(getBossHp());
        enemyAircrafts.add(boss);
        bossExists = true;
        bossScoreThreshold += 500;

        soundManager.playBossBgm();
    }

    // 英雄机发射子弹，具体射击策略由英雄机内部策略对象决定。
    private void shootAction() {
        heroBullets.addAll(heroAircraft.exeShootStrategy());
    }

    // 精英敌机、超级精英和 Boss 才会发射敌方子弹。
    private void eliteShootAction() {
        for (AbstractAircraft enemyAircraft : enemyAircrafts) {
            if ((enemyAircraft instanceof EliteEnemy
                    || enemyAircraft instanceof SuperEliteEnemy
                    || enemyAircraft instanceof BossEnemy)
                    && enemyAircraft.getHp() > 0) {
                enemyBullets.addAll(enemyAircraft.exeShootStrategy());
            }
        }
    }

    // 更新所有子弹坐标。
    private void bulletsMoveAction() {
        for (BaseBullet bullet : heroBullets) {
            bullet.forward();
        }
        for (BaseBullet bullet : enemyBullets) {
            bullet.forward();
        }
    }

    // 更新所有敌机坐标。
    private void aircraftsMoveAction() {
        for (AbstractAircraft enemyAircraft : enemyAircrafts) {
            enemyAircraft.forward();
        }
    }

    // 更新所有道具坐标。
    private void propsMoveAction() {
        for (AbstractProp prop : props) {
            prop.forward();
        }
    }

    // 碰撞检测总入口，拆分成四类检测便于讲解和维护。
    private void crashCheckAction() {
        checkEnemyBulletsHitHero();     // 敌方子弹打英雄机
        checkHeroBulletsHitEnemies();   // 英雄子弹打敌机
        checkEnemiesHitHero();          // 敌机撞英雄机
        checkPropsHitHero();            // 道具碰英雄机
    }

    // 检测敌方子弹是否击中英雄机。
    private void checkEnemyBulletsHitHero() {
        for (BaseBullet bullet : enemyBullets) {
            if (bullet.notValid()) {
                continue;
            }

            if (bullet.crash(heroAircraft)) {
                heroAircraft.decreaseHp(bullet.getPower());
                bullet.vanish();
            }
        }
    }

    // 检测英雄子弹是否击中敌机；这里使用空间网格减少无效碰撞判断。
    private void checkHeroBulletsHitEnemies() {
        buildEnemyGrid();

        for (BaseBullet bullet : heroBullets) {
            if (bullet.notValid()) {
                continue;
            }

            int bulletCol = clampGridCol(bullet.getLocationX() / GRID_SIZE);
            int bulletRow = clampGridRow(bullet.getLocationY() / GRID_SIZE);

            boolean hit = false;

            int startRow = Math.max(0, bulletRow - 1);
            int endRow = Math.min(gridRows - 1, bulletRow + 1);
            int startCol = Math.max(0, bulletCol - 1);
            int endCol = Math.min(gridCols - 1, bulletCol + 1);

            for (int row = startRow; row <= endRow; row++) {
                for (int col = startCol; col <= endCol; col++) {
                    List<AbstractAircraft> nearbyEnemies = enemyGrid[getGridIndex(col, row)];

                    for (AbstractAircraft enemyAircraft : nearbyEnemies) {
                        if (enemyAircraft.notValid()) {
                            continue;
                        }

                        if (enemyAircraft.crash(bullet)) {
                            enemyAircraft.decreaseHp(bullet.getPower());
                            bullet.vanish();

                            soundManager.playBulletHit();

                            if (enemyAircraft.notValid()) {
                                handleEnemyKilled(enemyAircraft);
                            }

                            hit = true;
                            break;
                        }
                    }

                    if (hit) {
                        break;
                    }
                }

                if (hit) {
                    break;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    // 根据屏幕大小创建或调整空间网格。
    private void ensureEnemyGrid() {
        int width = screenWidth > 0 ? screenWidth : Main.WINDOW_WIDTH;
        int height = screenHeight > 0 ? screenHeight : Main.WINDOW_HEIGHT;

        int newCols = Math.max(1, (width + GRID_SIZE - 1) / GRID_SIZE);
        int newRows = Math.max(1, (height + GRID_SIZE - 1) / GRID_SIZE);

        if (enemyGrid != null && newCols == gridCols && newRows == gridRows) {
            return;
        }

        gridCols = newCols;
        gridRows = newRows;
        enemyGrid = new ArrayList[gridCols * gridRows];

        for (int i = 0; i < enemyGrid.length; i++) {
            enemyGrid[i] = new ArrayList<>();
        }
    }

    // 每帧重建网格前先清空旧数据。
    private void clearEnemyGrid() {
        if (enemyGrid == null) {
            return;
        }

        for (List<AbstractAircraft> cell : enemyGrid) {
            cell.clear();
        }
    }

    private int getGridIndex(int col, int row) {
        return row * gridCols + col;
    }

    private int clampGridCol(int col) {
        if (col < 0) {
            return 0;
        }
        if (col >= gridCols) {
            return gridCols - 1;
        }
        return col;
    }

    private int clampGridRow(int row) {
        if (row < 0) {
            return 0;
        }
        if (row >= gridRows) {
            return gridRows - 1;
        }
        return row;
    }

    // 把每个敌机登记到它覆盖的网格中，子弹只检查附近网格。
    private void buildEnemyGrid() {
        ensureEnemyGrid();
        clearEnemyGrid();

        for (AbstractAircraft enemy : enemyAircrafts) {
            if (enemy.notValid()) {
                continue;
            }

            int halfWidth = enemy.getWidth() / 2;
            int halfHeight = enemy.getHeight() / 2;

            int left = enemy.getLocationX() - halfWidth;
            int right = enemy.getLocationX() + halfWidth;
            int top = enemy.getLocationY() - halfHeight;
            int bottom = enemy.getLocationY() + halfHeight;

            int leftCol = clampGridCol(left / GRID_SIZE);
            int rightCol = clampGridCol(right / GRID_SIZE);
            int topRow = clampGridRow(top / GRID_SIZE);
            int bottomRow = clampGridRow(bottom / GRID_SIZE);

            for (int row = topRow; row <= bottomRow; row++) {
                for (int col = leftCol; col <= rightCol; col++) {
                    enemyGrid[getGridIndex(col, row)].add(enemy);
                }
            }
        }
    }

    // 检测敌机与英雄机相撞；撞到后直接判定英雄机死亡。
    private void checkEnemiesHitHero() {
        for (AbstractAircraft enemyAircraft : enemyAircrafts) {
            if (enemyAircraft.notValid()) {
                continue;
            }

            if (enemyAircraft.crash(heroAircraft) || heroAircraft.crash(enemyAircraft)) {
                enemyAircraft.vanish();
                heroAircraft.decreaseHp(Integer.MAX_VALUE);
                break;
            }
        }
    }

    // 检测英雄机拾取道具，并根据道具类型执行回血、炸弹、火力、金币等效果。
    private void checkPropsHitHero() {
        for (AbstractProp prop : props) {
            if (prop.notValid()) {
                continue;
            }

            if (prop.crash(heroAircraft)) {
                if (prop instanceof PropBomb) {
                    PropBomb bomb = (PropBomb) prop;

                    for (AbstractAircraft enemy : enemyAircrafts) {
                        if (enemy instanceof BombObserver) {
                            bomb.addObserver((BombObserver) enemy);
                        }
                    }

                    for (BaseBullet bullet : enemyBullets) {
                        if (bullet instanceof BombObserver) {
                            bomb.addObserver((BombObserver) bullet);
                        }
                    }

                    bomb.takeEffect(heroAircraft, soundEnabled);
                    soundManager.playBombExplosion();
                    score += bomb.getTotalScore();
                    bomb.vanish();
                } else if (prop instanceof PropCoin) {
                    if (coinEnabled) {
                        PropCoin coin = (PropCoin) prop;
                        coinsGot += coin.getCoinValue();
                        soundManager.playPropEffect();
                    }
                    prop.vanish();
                } else {
                    prop.takeEffect(heroAircraft, soundEnabled);
                    soundManager.playPropEffect();
                    prop.vanish();
                }
            }
        }
    }

    // 敌机死亡后的统一处理：加分、掉落道具/金币、Boss 死亡后恢复普通 BGM。
    private void handleEnemyKilled(AbstractAircraft enemyAircraft) {
        if (enemyAircraft instanceof MobEnemy) {
            score += 10;
            dropProps(enemyAircraft, new ArrayList<>(), 1, 0.25);
        } else if (enemyAircraft instanceof EliteEnemy) {
            score += 30;
            List<AbstractProp> generatedProps = ((EliteEnemy) enemyAircraft).produceProps();
            dropProps(enemyAircraft, generatedProps, 1, 0.60);
        } else if (enemyAircraft instanceof SuperEliteEnemy) {
            score += 50;
            List<AbstractProp> generatedProps = ((SuperEliteEnemy) enemyAircraft).produceProps();
            dropProps(enemyAircraft, generatedProps, 2, 0.75);
        } else if (enemyAircraft instanceof BossEnemy) {
            score += 100;
            List<AbstractProp> generatedProps = ((BossEnemy) enemyAircraft).produceProps();
            dropProps(enemyAircraft, generatedProps, 5, 1.00);
            bossExists = false;
            soundManager.resumeGameBgmAfterBoss();
        }
    }

    /**
     * 在敌机死亡位置掉落道具
     * 不同种道具避免重叠（按 Bitmap 矩形区域避开），同类道具允许轻微偏移
     */
    private void dropProps(AbstractAircraft enemyAircraft, List<AbstractProp> generatedProps, int coinCount, double coinProbability) {
        if (enemyAircraft == null) return;

        int centerX = enemyAircraft.getLocationX();
        int centerY = enemyAircraft.getLocationY();

        // 保存已生成道具矩形区域，单位为屏幕坐标
        List<RectF> occupiedRects = new ArrayList<>();

        // 生成非金币道具（血包、火力、炸弹等）
        for (AbstractProp prop : generatedProps) {
            Bitmap image = prop.getImage();
            if (image == null) continue;

            int offsetX = (int)(Math.random() * 40 - 20);
            int offsetY = (int)(Math.random() * 40 - 20);

            float x = centerX + offsetX - image.getWidth() / 2f;
            float y = centerY + offsetY - image.getHeight() / 2f;

            // 限制在屏幕范围内
            x = Math.max(0, Math.min(screenWidth - image.getWidth(), x));
            y = Math.max(0, Math.min(screenHeight - image.getHeight(), y));

            prop.setLocation((int)(x + image.getWidth()/2f), (int)(y + image.getHeight()/2f));
            props.add(prop);

            // 记录实际矩形区域
            occupiedRects.add(new RectF(x, y, x + image.getWidth(), y + image.getHeight()));
        }

        // 生成金币
        if (!coinEnabled || coinCount <= 0) return;

        Bitmap coinImage = ImageManager.COIN_PROP_IMAGE;
        if (coinImage == null) return;

        double angleStep = 360.0 / coinCount;
        int coinRadius = 30;

        for (int i = 0; i < coinCount; i++) {
            if (Math.random() > coinProbability) continue;

            float x, y;
            int tries = 0;

            do {
                double angle = Math.toRadians(i * angleStep);
                x = centerX + (float)(coinRadius * Math.cos(angle) + Math.random() * 8 - 4) - coinImage.getWidth() / 2f;
                y = centerY + (float)(coinRadius * Math.sin(angle) + Math.random() * 8 - 4) - coinImage.getHeight() / 2f;

                x = Math.max(0, Math.min(screenWidth - coinImage.getWidth(), x));
                y = Math.max(0, Math.min(screenHeight - coinImage.getHeight(), y));

                tries++;
            } while (isRectOverlap(x, y, coinImage.getWidth(), coinImage.getHeight(), occupiedRects) && tries < 10);

            props.add(new PropCoin((int)(x + coinImage.getWidth()/2f), (int)(y + coinImage.getHeight()/2f), 0, 5));
            occupiedRects.add(new RectF(x, y, x + coinImage.getWidth(), y + coinImage.getHeight()));
        }
    }

    /** 判断给定矩形是否和已有矩形重叠 */
    private boolean isRectOverlap(float x, float y, float width, float height, List<RectF> rects) {
        RectF rect = new RectF(x, y, x + width, y + height);
        for (RectF r : rects) {
            if (RectF.intersects(rect, r)) return true;
        }
        return false;
    }

    /** 判断 (x,y) 是否和已有道具位置太近 */
    private boolean isTooClose(int x, int y, List<int[]> positions) {
        int minDistance = 30; // 最小间距
        for (int[] pos : positions) {
            int dx = x - pos[0];
            int dy = y - pos[1];
            if (dx * dx + dy * dy < minDistance * minDistance) {
                return true;
            }
        }
        return false;
    }
    // 按概率在敌机死亡位置附近掉落金币，并限制金币不超出屏幕左右边界。

    // 清理已失效对象，避免集合不断增大导致性能下降。
    private void postProcessAction() {
        enemyBullets.removeIf(AbstractFlyingObject::notValid);
        heroBullets.removeIf(AbstractFlyingObject::notValid);
        enemyAircrafts.removeIf(AbstractFlyingObject::notValid);
        props.removeIf(AbstractFlyingObject::notValid);
    }

    // 绘制一帧：锁定 Canvas，按背景、子弹、敌机、道具、英雄机、结束提示的顺序绘制。
    private void drawFrame() {
        Canvas canvas = null;
        try {
            canvas = holder.lockCanvas();
            if (canvas == null) {
                return;
            }
            drawBackground(canvas);
            paintObjects(canvas, enemyBullets);
            paintObjects(canvas, heroBullets);
            paintObjects(canvas, enemyAircrafts);
            paintObjects(canvas, props);
            paintHero(canvas);
            if (gameOverFlag) {
                paintGameOver(canvas);
            }
        } catch (Exception e) {
            Log.e(TAG, "drawFrame error", e);
        } finally {
            if (canvas != null) {
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    // 绘制滚动背景，具体图片切换和偏移由 BackgroundScrollManager 负责。
    private void drawBackground(Canvas canvas) {
        int width = screenWidth > 0 ? screenWidth : getWidth();
        int height = screenHeight > 0 ? screenHeight : getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        backgroundScrollManager.step(System.nanoTime(), height);
        backgroundScrollManager.draw(canvas, getContext(), width, height);
    }

    // 批量绘制敌机、子弹、道具等飞行对象。
    private void paintObjects(Canvas canvas, List<? extends AbstractFlyingObject> objects) {
        if (objects == null || objects.isEmpty()) {
            return;
        }
        for (AbstractFlyingObject object : objects) {
            Bitmap image = object.getImage();
            if (image == null) {
                continue;
            }
            float left = object.getLocationX() - image.getWidth() / 2f;
            float top = object.getLocationY() - image.getHeight() / 2f;
            canvas.drawBitmap(image, left, top, null);
        }
    }

    // 单独绘制英雄机，方便商店换皮肤后统一从 ImageManager 取最新图片。
    private void paintHero(Canvas canvas) {
        Bitmap heroImage = ImageManager.HERO_IMAGE;
        if (heroImage == null) {
            return;
        }
        float left = heroAircraft.getLocationX() - heroImage.getWidth() / 2f;
        float top = heroAircraft.getLocationY() - heroImage.getHeight() / 2f;
        canvas.drawBitmap(heroImage, left, top, null);
    }
    // 游戏结束提示：优先绘制图片，没有图片时用红色文字兜底。
    private void paintGameOver(Canvas canvas) {
        if (gameOverBitmap != null && !gameOverBitmap.isRecycled()) {
            float maxWidth = screenWidth * 0.25f;
            float maxHeight = screenHeight * 0.08f;

            float imageWidth = gameOverBitmap.getWidth();
            float imageHeight = gameOverBitmap.getHeight();

            if (imageWidth > 0 && imageHeight > 0) {
                float scale = Math.min(maxWidth / imageWidth, maxHeight / imageHeight);
                float targetWidth = imageWidth * scale;
                float targetHeight = imageHeight * scale;

                float left = (screenWidth - targetWidth) / 2f;
                float top = (screenHeight - targetHeight) / 2f;

                RectF dst = new RectF(left, top, left + targetWidth, top + targetHeight);
                canvas.drawBitmap(gameOverBitmap, null, dst, gameOverBitmapPaint);
                return;
            }
        }

        // 如果 img_game_over.png 暂时不存在，保留旧文字作为兜底，避免黑屏无提示。
        canvas.drawText("GAME OVER", screenWidth / 2f, screenHeight / 2f, gameOverPaint);
    }

    // Activity 暂停时暂停游戏音频。
    public void onGamePause() {
        soundManager.pauseAll();
    }

    // Activity 恢复时根据游戏状态恢复音频。
    public void onGameResume() {
        if (!gameOverFlag) {
            soundManager.resumeByState();
        }
    }

    // Activity 销毁时停止游戏线程并释放音频资源。
    public void onGameDestroy() {
        isDrawing = false;
        soundManager.stopAllBgm();
        soundManager.release();
    }

    // 绑定 Activity 的 Handler，让 Game 能在结束时通知页面跳转。
    public void setMainHandler(Handler handler) {
        this.mainHandler = handler;
    }

    public int getScore() {
        return score;
    }

    public int getLife() {
        return heroAircraft.getHp();
    }

    public int getCoinsGot() {
        return coinsGot;
    }

    // PK 模式不需要金币时关闭金币掉落和累计。
    public void setCoinEnabled(boolean coinEnabled) {
        this.coinEnabled = coinEnabled;

        if (!coinEnabled) {
            this.coinsGot = 0;
        }
    }
}
