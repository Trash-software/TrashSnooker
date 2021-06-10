package trashsoftware.trashSnooker.core;

public class GameSettings {

    private boolean player1Breaks;

    public boolean isPlayer1Breaks() {
        return player1Breaks;
    }

    public static class  Builder {

        private final GameSettings gameSettings = new GameSettings();

        public Builder player1Breaks(boolean player1Breaks) {
            gameSettings.player1Breaks = player1Breaks;
            return this;
        }

        public GameSettings build() {
            return gameSettings;
        }
    }
}
