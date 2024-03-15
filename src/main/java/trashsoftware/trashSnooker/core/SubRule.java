package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

import java.util.Collection;

public enum SubRule {
    RAW_STD(0),  // 仅作占位用
    CHINESE_EIGHT_STD(1),
    CHINESE_EIGHT_JOE(1),
    SNOOKER_STD(2),
    SNOOKER_GOLDEN(2);
    
    private final int typeId;  // 同typeId的SubRule只能存在最多一个
    
    SubRule(int typeId) {
        this.typeId = typeId;
    }
    
    public boolean isRepellent(SubRule subRule) {
        return this.typeId == subRule.typeId;
    }

    @Override
    public String toString() {
        String key = Util.toLowerCamelCase(name());
        return App.getStrings().getString(key);
    }
}
