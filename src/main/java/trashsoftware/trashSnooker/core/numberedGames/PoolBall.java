package trashsoftware.trashSnooker.core.numberedGames;

import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.Values;
import trashsoftware.trashSnooker.core.metrics.GameValues;

public class PoolBall extends Ball {

    public PoolBall(int number, boolean initPotted, GameValues values) {
        super(number, initPotted, values);
    }

    @Override
    protected Color generateColor(int value) {
        return poolBallBaseColor(value);
    }

    public static Color poolBallBaseColor(int number) {
        return switch (number) {
            case 0 -> Values.WHITE;
            case 1, 9, 16, 17 -> Values.YELLOW;
            case 2, 10 -> Values.BLUE;
            case 3, 11 -> Values.RED;
            case 4, 12 -> Values.PURPLE;
            case 5, 13 -> Values.ORANGE;
            case 6, 14 -> Values.GREEN;
            case 7, 15 -> Values.DARK_RED;
            case 8 -> Values.BLACK;
            default -> throw new RuntimeException("Unexpected ball.");
        };
    }
}
