package edu.hitsz.application;

//记录类
/*
 * RankRecord 是排行榜页面使用的数据模型，统一封装名次、玩家名、分数、头像资源和是否为当前玩家行。
 * 有了这个模型，排行榜页面只需要根据列表渲染 UI，不需要关心数据来自服务器还是本地兜底。
 */
public class RankRecord {

    // 排行榜一行所需的所有展示数据，字段设为 final 保证创建后不被随意修改。
    private final String rankLabel;
    private final String name;
    private final int score;
    private final int avatarResId;
    private final boolean currentPlayerRow;

    // 构造一条排行榜记录，currentPlayerRow 用于游戏结束页高亮本局玩家成绩。
    public RankRecord(String rankLabel, String name, int score, int avatarResId, boolean currentPlayerRow) {
        this.rankLabel = rankLabel;
        this.name = name;
        this.score = score;
        this.avatarResId = avatarResId;
        this.currentPlayerRow = currentPlayerRow;
    }

    public String getRankLabel() {
        return rankLabel;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public int getAvatarResId() {
        return avatarResId;
    }

    public boolean isCurrentPlayerRow() {
        return currentPlayerRow;
    }
}
