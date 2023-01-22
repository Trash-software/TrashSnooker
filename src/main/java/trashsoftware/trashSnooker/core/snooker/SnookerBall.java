package trashsoftware.trashSnooker.core.snooker;

import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.BallMetrics;
import trashsoftware.trashSnooker.core.GameValues;
import trashsoftware.trashSnooker.core.TableMetrics;

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
