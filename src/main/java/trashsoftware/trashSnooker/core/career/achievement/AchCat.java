package trashsoftware.trashSnooker.core.career.achievement;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

public enum AchCat {
    GENERAL_HIDDEN,
    GENERAL_TABLE,
    GENERAL_MATCH,
    FOUL,
    SNOOKER,
    POOL_GENERAL,
    CHINESE_EIGHT,
    AMERICAN_NINE,
    TOUR,
    SNOOKER_TOUR,
    CHINESE_EIGHT_TOUR,
    AMERICAN_NINE_TOUR;
    
    private Achievement[] allAchInCat;
    
    public Achievement[] getAll() {
        if (allAchInCat == null) {
            int count = 0;
            for (Achievement ach : Achievement.values()) {
                if (ach.category == this) count++;
            }
            allAchInCat = new Achievement[count];
            int index = 0;
            for (Achievement ach : Achievement.values()) {
                if (ach.category == this) allAchInCat[index++] = ach;
            }
        }
        return allAchInCat;
    }
    
    public String shown() {
        return App.getStrings().getString(Util.toLowerCamelCase("ACH_CAT_" + name()));
    }
}
