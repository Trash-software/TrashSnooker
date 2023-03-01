package trashsoftware.trashSnooker.core.snooker;

import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.metrics.GameValues;

public class SnookerBall extends Ball {
    public SnookerBall(int value, GameValues values) {
        super(value, values);
    }

    public SnookerBall(int value, double[] pos, GameValues values) {
        super(value, pos, values);
    }

    @Override
    protected Color generateColor(int value) {
        return snookerColor(value);
    }
}
