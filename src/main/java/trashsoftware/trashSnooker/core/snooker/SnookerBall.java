package trashsoftware.trashSnooker.core.snooker;

import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.Values;
import trashsoftware.trashSnooker.core.metrics.GameValues;

public class SnookerBall extends Ball {
    public SnookerBall(int value, GameValues values) {
        super(value, values);
    }

    public SnookerBall(int value, double[] pos, GameValues values) {
        super(value, pos, values);
    }
    
    public boolean isRed() {
        return getValue() == 1;
    }
    
    public boolean isGold() {
        return getValue() == 20;
    }

    @Override
    protected Color generateColor(int value) {
        return snookerColor(value);
    }

    public static Color snookerColor(int value) {
        return switch (value) {
            case 0 -> Values.WHITE;
            case 1 -> Values.RED;
            case 2 -> Values.YELLOW;
            case 3 -> Values.GREEN;
            case 4 -> Values.BROWN;
            case 5 -> Values.BLUE;
            case 6 -> Values.PINK;
            case 7 -> Values.BLACK;
            case 20 -> Values.GOLD;
            default -> throw new RuntimeException("Unexpected ball.");
        };
    }
}
