package trashsoftware.trashSnooker.core;

public enum GameType {
    SNOOKER(true, GameValues.SNOOKER_VALUES),
    MINI_SNOOKER(true, GameValues.MINI_SNOOKER_VALUES),
    CHINESE_EIGHT(false, GameValues.CHINESE_EIGHT_VALUES),
    SIDE_POCKET(false, GameValues.SIDE_POCKET);

    public final boolean scoredGame;
    public final GameValues gameValues;

    GameType(boolean scoredGame, GameValues gameValues) {
        this.scoredGame = scoredGame;
        this.gameValues = gameValues;
    }
}
