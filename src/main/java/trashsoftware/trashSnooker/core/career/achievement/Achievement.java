package trashsoftware.trashSnooker.core.career.achievement;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

import java.util.MissingResourceException;

/**
 * 一个成就
 * 
 * 成就分为三种：
 * 单次成就：完成了就完成了
 * 累积型成就：累计完成次数，达到一定次数后判断成就达成
 * 纪录型成就：类似最高记录那种
 */
public enum Achievement {
    POT_A_BALL(AchCat.GENERAL_TABLE),  // 已完成
    POT_EIGHT_BALLS(AchCat.GENERAL_TABLE, 8, false),  // 已完成
    THREE_BALLS_IN_A_ROW(AchCat.GENERAL_TABLE),  // 已完成
    POSITIONING_MASTER(AchCat.GENERAL_TABLE, 7, true),  // 已完成
    GAIN_BY_SNOOKER(AchCat.GENERAL_TABLE),  // 已完成
    SOLVE_SNOOKER_SUCCESS(AchCat.GENERAL_TABLE),  // 已完成
    ACCURACY_WIN(AchCat.GENERAL_TABLE),  // 已完成
    CONTINUOUS_LONG_POT(AchCat.GENERAL_TABLE),
//    PASS_POT(AchCat.GENERAL),  // todo 传球 完成不了

    WIN_A_FRAME(AchCat.GENERAL_MATCH),  // 已完成
    WIN_A_MATCH(AchCat.GENERAL_MATCH),  // 已完成
    WIN_ALL_MATCHES(AchCat.GENERAL_MATCH),  // 已完成
    LEGENDARY_REVENGE(AchCat.GENERAL_MATCH),  // 已完成
    
    LONG_THINK(AchCat.GENERAL_HIDDEN),  // 已完成
    COMPLETE_LOSS(AchCat.GENERAL_HIDDEN),  // 已完成
    MISCUED(AchCat.GENERAL_HIDDEN),  // 已完成
    SHAKE_POCKET(AchCat.GENERAL_HIDDEN),  // 已完成
    FRAME_NO_ATTACK(AchCat.GENERAL_HIDDEN),  // 已完成
    KEY_BALL_FAIL(AchCat.GENERAL_HIDDEN),  // 已完成
    POT_FAIL_THREE(AchCat.GENERAL_HIDDEN),  // 已完成
    LOST_ALL_MATCHES(AchCat.GENERAL_HIDDEN),  // 已完成
    BIG_HEART(AchCat.GENERAL_HIDDEN, 3, false),  // 已完成
    ONE_ROUND_TOUR(AchCat.GENERAL_HIDDEN),  // 已完成
    LEGENDARY_REVENGED(AchCat.GENERAL_HIDDEN),  // 已完成
    
    CUE_BALL_POT(AchCat.FOUL),  // 已完成
    MISSED_SHOT(AchCat.FOUL),  // 已完成
    
    SNOOKER_BREAK_50(AchCat.SNOOKER),  // 已完成
    SNOOKER_BREAK_100(AchCat.SNOOKER),  // 已完成
    SNOOKER_BREAK_147(AchCat.GENERAL_HIDDEN),  // 已完成。还是不把147放明面成就上，太难了
    THREE_MISS_LOST(AchCat.SNOOKER),  // 已完成
    COME_BACK_BEHIND_65(AchCat.SNOOKER),  // 已完成
    COME_BACK_BEHIND_OVER_SCORE(AchCat.SNOOKER),  // 已完成
    
    POOL_BREAK_POT(AchCat.POOL_GENERAL),  // 已完成
    POOL_CLEAR(AchCat.POOL_GENERAL),  // 已完成
    POOL_BREAK_CLEAR(AchCat.POOL_GENERAL),  // 已完成
    SUICIDE(AchCat.POOL_GENERAL),  // 已完成
    
    BLIND_SHOT(AchCat.CHINESE_EIGHT),  // 已完成
    REMAIN_ONE_MUST_LOSE(AchCat.CHINESE_EIGHT),  // 已完成
    
    GOLDEN_NINE(AchCat.AMERICAN_NINE),  // 已完成
    BALL_WORKER(AchCat.AMERICAN_NINE),  // 已完成
    PASS_NINE(AchCat.AMERICAN_NINE);  // 已完成

    public final AchCat category;
    public final int requiredTimes;
    public boolean recordLike;

    Achievement(AchCat category, int requiredTimes, boolean recordLike) {
        this.category = category;
        this.requiredTimes = requiredTimes;
        this.recordLike = recordLike;
    }
    
    Achievement(AchCat category) {
        this(category, 1, false);
    }

    public static Achievement fromKey(String key) {
        return valueOf(Util.toAllCapsUnderscoreCase(key));
    }
    
    public boolean isComplete(AchCompletion completion) {
        return completion != null && completion.getTimes() >= requiredTimes;
    }
    
    public boolean countLikeRepeatable() {
        return !recordLike && requiredTimes != 1;
    }

    public boolean isRecordLike() {
        return recordLike;
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
