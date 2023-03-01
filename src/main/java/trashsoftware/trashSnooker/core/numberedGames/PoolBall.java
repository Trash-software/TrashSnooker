package trashsoftware.trashSnooker.core.numberedGames;

import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.metrics.GameValues;

public class PoolBall extends Ball {

    public PoolBall(int number, boolean initPotted, GameValues values) {
        super(number, initPotted, values);
    }

    @Override
    protected Color generateColor(int value) {
        return poolBallBaseColor(value);
    }
}
