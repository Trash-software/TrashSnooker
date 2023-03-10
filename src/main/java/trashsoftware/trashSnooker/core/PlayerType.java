package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

public enum PlayerType {
    PLAYER,
    COMPUTER;

    @Override
    public String toString() {
        return App.getStrings().getString(Util.toLowerCamelCase("TYPE_" + name()));
    }
}
