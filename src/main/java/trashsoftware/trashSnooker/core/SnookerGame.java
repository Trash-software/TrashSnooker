package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.fxml.GameView;

public class SnookerGame extends AbstractSnookerGame {

    public SnookerGame(GameView parent, GameSettings gameSettings) {
        super(parent, gameSettings, GameValues.SNOOKER_VALUES);
    }

    @Override
    protected double breakLineX() {
        return gameValues.leftX + 737.0;
    }

    @Override
    protected double breakArcRadius() {
        return 292.0;
    }

    @Override
    protected double[] blackBallPos() {
        return new double[]{gameValues.rightX - 324.0, gameValues.midY};
    }
}
