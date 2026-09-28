package trashsoftware.trashSnooker.core.russian;

import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.metrics.GameValues;

public class RussianBall extends Ball {
    public RussianBall(int value, boolean initPotted, GameValues values) {
        super(value, initPotted, values);
    }

    @Override
    protected Color generateColor(int value) {
        return russianColor(value);
    }
    
    public static Color russianColor(int value) {
        if (value == 0) return Color.RED;
        else return Color.SNOW;
    }
}
