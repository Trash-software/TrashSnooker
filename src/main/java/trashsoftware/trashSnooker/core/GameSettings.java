package trashsoftware.trashSnooker.core;

public class GameSettings {

    private boolean player1Breaks;
    private InGamePlayer player1, player2;

    public GameSettings() {
    }

    public boolean isPlayer1Breaks() {
        return player1Breaks;
    }

    public InGamePlayer getPlayer1() {
        return player1;
    }

    public InGamePlayer getPlayer2() {
        return player2;
    }

    public static class Builder {

        private final GameSettings gameSettings = new GameSettings();

        public Builder player1Breaks(boolean player1Breaks) {
            gameSettings.player1Breaks = player1Breaks;
            return this;
        }

        public Builder players(InGamePlayer p1, InGamePlayer p2) {
            gameSettings.player1 = p1;
            gameSettings.player2 = p2;
            return this;
        }

        public GameSettings build() {
            return gameSettings;
        }
    }
}
