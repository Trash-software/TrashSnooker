package trashsoftware.trashSnooker.core;

public enum GameType {
    SNOOKER(true, GameValues.SNOOKER_VALUES, "Snooker"),
    MINI_SNOOKER(true, GameValues.MINI_SNOOKER_VALUES, "MiniSnooker"),
    CHINESE_EIGHT(false, GameValues.CHINESE_EIGHT_VALUES, "ChineseEight"),
    SIDE_POCKET(false, GameValues.SIDE_POCKET, "SidePocket");

    public final boolean snookerLike;
    public final GameValues gameValues;
    public final String sqlKey;

    GameType(boolean snookerLike, GameValues gameValues, String sqlKey) {
        this.snookerLike = snookerLike;
        this.gameValues = gameValues;
        this.sqlKey = sqlKey;
    }
    
    public String toSqlKey() {
        return sqlKey;
    }
    
    public static GameType fromSqlKey(String sqlKey) {
        for (GameType gameType : values()) {
            if (gameType.sqlKey.equalsIgnoreCase(sqlKey)) return gameType;
        }
        throw new EnumConstantNotPresentException(GameType.class, sqlKey);
    }
}
