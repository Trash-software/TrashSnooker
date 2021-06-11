package trashsoftware.trashSnooker.core.snooker;

import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.GameValues;

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
