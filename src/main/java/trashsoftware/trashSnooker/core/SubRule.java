package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

public enum SubRule {
    CHINESE_EIGHT_JOE,
    CHINESE_EIGHT_STD;

    @Override
    public String toString() {
        String key = Util.toLowerCamelCase(name());
        return App.getStrings().getString(key);
    }
}
