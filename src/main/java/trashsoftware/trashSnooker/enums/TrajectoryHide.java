package trashsoftware.trashSnooker.enums;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

import java.util.ResourceBundle;

public enum TrajectoryHide {
    INSTANT,
    OPERATION,
    NEXT_CUE;
    
    @Override
    public String toString() {
        ResourceBundle strings = App.getStrings();
        String key = Util.toLowerCamelCase("EFFECT_TRAJECTORY_HIDE_" + name());
        if (strings.containsKey(key)) return strings.getString(key);
        else return name();
    }

    public static TrajectoryHide fromKey(String key) {
        return valueOf(Util.toAllCapsUnderscoreCase(key));
    }

    public String toKey() {
        return Util.toLowerCamelCase(name());
    }
}
