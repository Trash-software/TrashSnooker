package trashsoftware.trashSnooker.core.career.achievement;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

import java.util.MissingResourceException;

public enum Achievement {
    POT_A_BALL(AchCat.GENERAL),  // 已完成
    POT_EIGHT_BALLS(AchCat.GENERAL, 8),
    THREE_BALL_IN_A_ROW(AchCat.GENERAL),
    WIN_A_FRAME(AchCat.GENERAL),  // 已完成
    WIN_A_MATCH(AchCat.GENERAL),  // 已完成
    GAIN_BY_SNOOKER(AchCat.GENERAL),  // 已完成
    SOLVE_SNOOKER_SUCCESS(AchCat.GENERAL),  // 已完成
    PASS_POT(AchCat.GENERAL),  // 传球 未完成
    COMPLETE_LOSS(AchCat.GENERAL_HIDDEN),  // 已完成
    MISCUED(AchCat.GENERAL_HIDDEN),  // 已完成
    SHAKE_POCKET(AchCat.GENERAL_HIDDEN),  // 已完成
    WIN_ALL_MATCHES(AchCat.GENERAL_HIDDEN),  // 已完成
    LOST_ALL_MATCHES(AchCat.GENERAL_HIDDEN),  // 已完成
    BIG_HEART(AchCat.GENERAL_HIDDEN, 3),  // 已完成
    ONE_ROUND_TOUR(AchCat.GENERAL_HIDDEN),  // 未完成
    CUE_BALL_POT(AchCat.FOUL),  // 已完成
    MISSED_SHOT(AchCat.FOUL),  // 已完成
    SNOOKER_BREAK_100(AchCat.SNOOKER),
    SNOOKER_BREAK_147(AchCat.SNOOKER),
    THREE_MISS_LOST(AchCat.SNOOKER),  // 已完成
    COME_BACK_BEHIND_70(AchCat.SNOOKER),  // 似乎未完成
    COME_BACK_BEHIND_OVER_SCORE(AchCat.SNOOKER),  // 似乎未完成
    POOL_BREAK_POT(AchCat.POOL_GENERAL),  // 已完成
    POOL_CLEAR(AchCat.POOL_GENERAL),
    POOL_BREAK_CLEAR(AchCat.POOL_GENERAL),
    SUICIDE(AchCat.POOL_GENERAL),  // 已完成
    BLIND_SHOT(AchCat.CHINESE_EIGHT),  // 已完成
    GOLDEN_NINE(AchCat.AMERICAN_NINE),  // 已完成
    PASS_NINE(AchCat.AMERICAN_NINE);  // 已完成

    public final AchCat category;
    public final int requiredTimes;

    Achievement(AchCat category, int requiredTimes) {
        this.category = category;
        this.requiredTimes = requiredTimes;
    }
    
    Achievement(AchCat category) {
        this(category, 1);
    }

    public static Achievement fromKey(String key) {
        return valueOf(Util.toAllCapsUnderscoreCase(key));
    }
    
    public boolean isComplete(AchCompletion completion) {
        return completion != null && completion.getTimes() >= requiredTimes;
    }
    
    public boolean repeatable() {
        return requiredTimes != 1;
    }

    public String toKey() {
        return Util.toLowerCamelCase(name());
    }

    public String title() {
        try {
            return App.getStrings().getString(Util.toLowerCamelCase("ACH_" + name()));
        } catch (MissingResourceException e) {
            return name();
        }
    }

    public String description() {
        try {
            return App.getStrings().getString(Util.toLowerCamelCase("ACH_DES_" + name()));
        } catch (MissingResourceException e) {
            return name();
        }
    }
}
