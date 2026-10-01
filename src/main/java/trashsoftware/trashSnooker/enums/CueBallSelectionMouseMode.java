package trashsoftware.trashSnooker.enums;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

import java.util.ResourceBundle;

public enum CueBallSelectionMouseMode {
    DOUBLE_CLICK,
    SECONDARY_CLICK;

    @Override
    public String toString() {
        ResourceBundle strings = App.getStrings();
        String key = Util.toLowerCamelCase("CUE_BALL_SELECTION_MOUSE_MODE_" + name());
        if (strings.containsKey(key)) return strings.getString(key);
        else return name();
    }

    public static CueBallSelectionMouseMode fromKey(String key) {
        return valueOf(Util.toAllCapsUnderscoreCase(key));
    }

    public String toKey() {
        return Util.toLowerCamelCase(name());
    }
}
