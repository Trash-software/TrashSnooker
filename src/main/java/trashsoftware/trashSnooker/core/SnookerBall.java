package trashsoftware.trashSnooker.core;

import javafx.scene.paint.Color;

public class SnookerBall extends Ball {
    SnookerBall(int value, GameValues values) {
        super(value, values);
    }

    SnookerBall(int value, double[] pos, GameValues values) {
        super(value, pos, values);
    }

    @Override
    protected Color generateColor(int value) {
        return snookerColor(value);
    }
}
