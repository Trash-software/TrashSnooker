package trashsoftware.trashSnooker.util.config;

import javafx.scene.input.KeyCode;
import trashsoftware.trashSnooker.util.Util;

import java.util.ResourceBundle;

public enum KeyBehavior {
    SHOT(KeyCode.SPACE),
    MOVE_LEFT_MAJOR(KeyCode.LEFT),
    MOVE_RIGHT_MAJOR(KeyCode.RIGHT),
    MOVE_LEFT_MINOR(KeyCode.COMMA),
    MOVE_RIGHT_MINOR(KeyCode.PERIOD),
    POWER_INCREASE(KeyCode.UP),
    POWER_DECREASE(KeyCode.DOWN),
    SPIN_UP(KeyCode.W),
    SPIN_LEFT(KeyCode.A),
    SPIN_DOWN(KeyCode.S),
    SPIN_RIGHT(KeyCode.D),
    CUE_ANGLE_UP(KeyCode.Q),
    CUE_ANGLE_DOWN(KeyCode.E),
    ROTATE_CUE_CLOCKWISE(KeyCode.X),
    ROTATE_CUE_COUNTERCLOCKWISE(KeyCode.Z),
    CHANGE_CUE_MENU(KeyCode.TAB),
    CHANGE_HAND_LEFT(KeyCode.DIGIT1),
    CHANGE_HAND_RIGHT(KeyCode.DIGIT2),
    CHANGE_HAND_REST(KeyCode.DIGIT3),
    ALTER_AIMING_EXTENSION(KeyCode.I),
    ALTER_POT_INSPECTION(KeyCode.O),
    ALTER_SHADOW_INSPECTION(KeyCode.P);
    
    public final KeyCode defaultKey;
    
    KeyBehavior(KeyCode defaultKey) {
        this.defaultKey = defaultKey;
    }
    
    public String getShown(ResourceBundle strings) {
        String name = Util.toLowerCamelCase("KEY_" + name());
        if (strings.containsKey(name)) return strings.getString(name);
        else return name;
    }
}
