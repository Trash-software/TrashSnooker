package trashsoftware.trashSnooker.core;

import org.json.JSONArray;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
    
    public static Collection<SubRule> defaultSubRule(GameRule rule) {
        return List.of(switch (rule) {
            case SNOOKER -> SNOOKER_STD;
            case CHINESE_EIGHT -> CHINESE_EIGHT_STD;
            default -> RAW_STD;
        });
    }
    
    public static JSONArray subRulesToJson(Collection<SubRule> subRules) {
        JSONArray array = new JSONArray();
        for (SubRule rule : subRules) {
            array.put(rule.name());
        }
        return array;
    }

    public static Collection<SubRule> jsonToSubRules(JSONArray jsonArray) {
        Collection<SubRule> subRules = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            subRules.add(SubRule.valueOf(jsonArray.getString(i)));
        }
        return subRules;
    }

    public static String subRulesToCommaString(Collection<SubRule> subRules) {
        return subRules.stream().map(SubRule::name).collect(Collectors.joining(","));
    }

    public static Collection<SubRule> commaStringToSubRules(String commaString) {
        String[] arr = commaString.split(",");
        return Arrays.stream(arr).map(SubRule::valueOf).collect(Collectors.toList());
    }
}
