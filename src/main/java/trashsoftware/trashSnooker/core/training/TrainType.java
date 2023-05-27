package trashsoftware.trashSnooker.core.training;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

public enum TrainType {
    SNAKE_FULL,
    SNAKE_HALF,
    SNAKE_FULL_DENSE,
    SNAKE_CROSS,
    SNAKE_X,
    SNAKE_FULL_ORDERED,
    SNAKE_HALF_ORDERED,
    CLEAR_COLOR;

    @Override
    public String toString() {
        String key = Util.toLowerCamelCase("TRAINING_" + name());
        return App.getStrings().getString(key);
    }
}
