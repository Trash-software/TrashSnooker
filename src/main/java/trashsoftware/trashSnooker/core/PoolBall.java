package trashsoftware.trashSnooker.core;

import javafx.scene.paint.Color;

public class PoolBall extends Ball {

    PoolBall(int number, boolean initPotted, GameValues values) {
        super(number, initPotted, values);
    }

    @Override
    protected Color generateColor(int value) {
        return poolBallBaseColor(value);
    }
}
