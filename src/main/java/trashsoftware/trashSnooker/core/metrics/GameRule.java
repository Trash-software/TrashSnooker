package trashsoftware.trashSnooker.core.metrics;

import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.Game;

/**
 * 一个比赛类型。
 * <p>
 * 一文厘清这几个的关系:
 * GameType指这个游系，如斯诺克，与GameValues其实是完全对应的，只是因为初期设计原因分开了。
 *
 * @see TableMetrics 已经说了
 * @see trashsoftware.trashSnooker.core.table.Table 与GameType还是一一对应的，只不过主要功能是绘图
 * @see EntireGame 一场比赛的实例
 * @see Game 一局游戏的实例
 */
public enum GameRule {
    SNOOKER(true, 22, "Snooker"),
    MINI_SNOOKER(true, 13, "MiniSnooker"),
    CHINESE_EIGHT(false, 16, "ChineseEight"),
    SIDE_POCKET(false, 10, "SidePocket");

    public final boolean snookerLike;
    public final String sqlKey;
    public final int nBalls;

    GameRule(boolean snookerLike, int nBalls, String sqlKey) {
        this.snookerLike = snookerLike;
        this.nBalls = nBalls;
        this.sqlKey = sqlKey;
    }

    public static GameRule fromSqlKey(String sqlKey) {
        for (GameRule gameRule : values()) {
            if (gameRule.sqlKey.equalsIgnoreCase(sqlKey)) return gameRule;
        }
        throw new EnumConstantNotPresentException(GameRule.class, sqlKey);
    }

    public static String toReadable(GameRule gameRule) {
        if (gameRule == GameRule.SNOOKER) {
            return "斯诺克";
        } else if (gameRule == GameRule.MINI_SNOOKER) {
            return "小斯诺克";
        } else if (gameRule == GameRule.CHINESE_EIGHT) {
            return "中式八球";
        } else if (gameRule == GameRule.SIDE_POCKET) {
            return "美式九球";
        } else {
            return "";
        }
    }

    @Override
    public String toString() {
        return toReadable(this);
    }

    public String toSqlKey() {
        return sqlKey;
    }
}
