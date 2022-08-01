package trashsoftware.trashSnooker.core;

public enum GameType {
    SNOOKER(true, GameValues.SNOOKER_VALUES, 22, "Snooker"),
    MINI_SNOOKER(true, GameValues.MINI_SNOOKER_VALUES, 22, "MiniSnooker"),
    CHINESE_EIGHT(false, GameValues.CHINESE_EIGHT_VALUES, 16, "ChineseEight"),
    SIDE_POCKET(false, GameValues.SIDE_POCKET, 10, "SidePocket");

    public final boolean snookerLike;
    public final GameValues gameValues;
    public final String sqlKey;
    public final int nBalls;

    GameType(boolean snookerLike, GameValues gameValues, int nBalls, String sqlKey) {
        this.snookerLike = snookerLike;
        this.gameValues = gameValues;
        this.nBalls = nBalls;
        this.sqlKey = sqlKey;
    }

    public static GameType fromSqlKey(String sqlKey) {
        for (GameType gameType : values()) {
            if (gameType.sqlKey.equalsIgnoreCase(sqlKey)) return gameType;
        }
        throw new EnumConstantNotPresentException(GameType.class, sqlKey);
    }

    public static String toReadable(GameType gameType) {
        if (gameType == GameType.SNOOKER) {
            return "斯诺克";
        } else if (gameType == GameType.MINI_SNOOKER) {
            return "小斯诺克";
        } else if (gameType == GameType.CHINESE_EIGHT) {
            return "中式八球";
        } else if (gameType == GameType.SIDE_POCKET) {
            return "美式九球";
        } else {
            return "";
        }
    }

    public String toSqlKey() {
        return sqlKey;
    }
}
