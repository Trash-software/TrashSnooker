package trashsoftware.trashSnooker.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.ArcType;
import trashsoftware.trashSnooker.fxml.GameView;

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
