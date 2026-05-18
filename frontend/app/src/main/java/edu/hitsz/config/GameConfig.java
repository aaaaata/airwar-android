package edu.hitsz.config;

/*
 * GameConfig 集中保存 Activity 之间传递数据用的 key、游戏结束消息码，以及三种难度的字符串常量。
 * 这样可以避免多个 Activity 中重复写字符串，减少 Intent 传参时因拼写错误导致的数据丢失问题。
 */
public final class GameConfig {

    private GameConfig() {}

    // Intent 参数 key：用于在页面之间传递游戏难度。
    public static final String EXTRA_DIFFICULTY = "difficulty";
    public static final String EXTRA_SOUND_ENABLED = "soundEnabled";
    public static final String EXTRA_SCORE = "score";
    public static final String EXTRA_FROM_GAME_OVER = "fromGameOver";

    public static final String EXTRA_COINS_GOT = "coinsGot";

    // Handler 消息码：Game 通过它通知 Activity 游戏结束。
    public static final int MSG_GAME_OVER = 1001;

    // 三种难度统一使用常量，避免多个类中硬编码字符串。
    public static final String EASY = "EASY";
    public static final String NORMAL = "NORMAL";
    public static final String HARD = "HARD";
}
