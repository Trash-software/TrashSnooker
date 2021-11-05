package trashsoftware.trashSnooker.core.snooker;

import trashsoftware.trashSnooker.core.GameSettings;
import trashsoftware.trashSnooker.core.GameValues;
import trashsoftware.trashSnooker.fxml.GameView;

public class SnookerGame extends AbstractSnookerGame {

    public SnookerGame(GameView parent, GameSettings gameSettings, int frameIndex) {
        super(parent, gameSettings, GameValues.SNOOKER_VALUES, frameIndex);
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
