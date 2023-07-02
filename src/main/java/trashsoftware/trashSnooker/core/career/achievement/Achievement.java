package trashsoftware.trashSnooker.core.career.achievement;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

import java.util.MissingResourceException;

/**
 * 一个成就
 * <p>
 * 成就分为三种：
 * 单次成就：完成了就完成了
 * 累积型成就：累计完成次数，达到一定次数后判断成就达成
 * 纪录型成就：类似最高记录那种
 */
public enum Achievement {
    POT_A_BALL(AchCat.GENERAL_TABLE),  // 已完成
    POT_EIGHT_BALLS(AchCat.GENERAL_TABLE, 8, false),  // 已完成
    POT_HUNDRED_BALLS(AchCat.GENERAL_TABLE, 100, false),  // 已完成
    CUMULATIVE_LONG_POTS_1(AchCat.GENERAL_TABLE, 10, false),  // 已完成
    THREE_BALLS_IN_A_ROW(AchCat.GENERAL_TABLE),  // 已完成
    POSITIONING_MASTER(AchCat.GENERAL_TABLE, 7, true),  // 已完成
    GAIN_BY_SNOOKER(AchCat.GENERAL_TABLE),  // 已完成
    SOLVE_SNOOKER_SUCCESS(AchCat.GENERAL_TABLE),  // 已完成
    ACCURACY_WIN(AchCat.GENERAL_TABLE),  // 已完成
    ACCURACY_WIN_LONG(AchCat.GENERAL_TABLE),  // 已完成
    CONTINUOUS_LONG_POT(AchCat.GENERAL_TABLE),  // 已完成
    MULTIPLE_EASY_FAILS(AchCat.GENERAL_TABLE) {  // 已完成

        @Override
        public boolean isHidden() {
            return true;
        }
    },
//    PASS_POT(AchCat.GENERAL),  // todo 传球 完成不了

    WIN_A_FRAME(AchCat.GENERAL_MATCH),  // 已完成
    WIN_A_MATCH(AchCat.GENERAL_MATCH),  // 已完成
    WIN_ALL_MATCHES(AchCat.GENERAL_MATCH),  // 已完成
    SEMIFINAL_STAGE(AchCat.GENERAL_MATCH),  // 已完成
    FINAL_STAGE(AchCat.GENERAL_MATCH),  // 已完成
    FINAL_FRAME(AchCat.GENERAL_MATCH),  // 已完成
    FINAL_FRAME_USUAL_GUEST(AchCat.GENERAL_MATCH, 5, false),  // 已完成
    FINAL_STAGE_FINAL_FRAME(AchCat.GENERAL_MATCH),  // 已完成
    FINAL_STAGE_FINAL_FRAME_WIN(AchCat.GENERAL_HIDDEN),  // 已完成
    LEGENDARY_REVENGE(AchCat.GENERAL_MATCH),  // 已完成

    LONG_THINK(AchCat.GENERAL_HIDDEN),  // 已完成
    COMPLETE_LOSS(AchCat.GENERAL_HIDDEN),  // 已完成
    MISCUED(AchCat.GENERAL_HIDDEN),  // 已完成
    SHAKE_POCKET(AchCat.GENERAL_HIDDEN),  // 已完成

    CUE_BALL_POT(AchCat.GENERAL_HIDDEN),  // 已完成
    MISSED_SHOT(AchCat.GENERAL_HIDDEN),  // 已完成

    FRAME_NO_ATTACK(AchCat.GENERAL_HIDDEN),  // 已完成
    KEY_BALL_FAIL(AchCat.GENERAL_HIDDEN),  // 已完成
    POT_FAIL_THREE(AchCat.GENERAL_HIDDEN),  // 已完成
    LOST_ALL_MATCHES(AchCat.GENERAL_HIDDEN),  // 已完成
    BIG_HEART(AchCat.GENERAL_HIDDEN, 3, false),  // 已完成
    ONE_ROUND_TOUR(AchCat.GENERAL_HIDDEN),  // 已完成
    LEGENDARY_REVENGED(AchCat.GENERAL_HIDDEN),  // 已完成

    SNOOKER_BREAK_50(AchCat.SNOOKER),  // 已完成
    SNOOKER_BREAK_100(AchCat.SNOOKER),  // 已完成
    SNOOKER_BREAK_147(AchCat.SNOOKER) {  // 已完成
        @Override
        public boolean isHidden() {
            return true;  // 还是不把147放明面成就上，太难了
        }
    },
    THREE_MISS_LOST(AchCat.SNOOKER),  // 已完成
    HARD_SNOOKER_BY_OPPONENT(AchCat.SNOOKER),  // 已完成
    HARD_SNOOKER_BY_HUMAN(AchCat.SNOOKER),  // 已完成
    UNSOLVABLE_SNOOKER(AchCat.SNOOKER) {  // 已完成
        @Override
        public boolean isHidden() {
            return true;
        }
    },
    COME_BACK_BEHIND_65(AchCat.SNOOKER),  // 已完成
    COME_BACK_BEHIND_OVER_SCORE(AchCat.SNOOKER),  // 已完成
    SUM_BELOW(AchCat.SNOOKER),  // 已完成
    SUM_BELOW_2(AchCat.SNOOKER) {  // 已完成
        @Override
        public boolean isHidden() {
            return true;
        }
    },
    SNOOKER_NO_POT(AchCat.SNOOKER) {  // 已完成
        @Override
        public boolean isHidden() {
            return true;
        }
    },
    SUM_OVER_147(AchCat.SNOOKER),  // 已完成
    POT_WRONG_COLOR(AchCat.SNOOKER) {  // 已完成
        @Override
        public boolean isHidden() {
            return true;
        }
    },

    POOL_BREAK_POT(AchCat.POOL_GENERAL),  // 已完成
    POOL_CLEAR(AchCat.POOL_GENERAL),  // 已完成
    POOL_BREAK_CLEAR(AchCat.POOL_GENERAL),  // 已完成
    SUICIDE(AchCat.POOL_GENERAL),  // 已完成

    BLIND_SHOT(AchCat.CHINESE_EIGHT),  // 已完成
    REMAIN_ONE_MUST_LOSE(AchCat.CHINESE_EIGHT),  // 已完成
    CHINESE_EIGHT_NO_POT(AchCat.CHINESE_EIGHT) {  // 已完成
        @Override
        public boolean isHidden() {
            return true;
        }
    },

    GOLDEN_NINE(AchCat.AMERICAN_NINE),  // 已完成
    BALL_WORKER(AchCat.AMERICAN_NINE),  // 已完成
    PASS_NINE(AchCat.AMERICAN_NINE),  // 已完成
    AMERICAN_NINE_NO_POT(AchCat.AMERICAN_NINE) {  // 已完成
        @Override
        public boolean isHidden() {
            return true;
        }
    },

    // 巡回赛
    EARNED_MONEY(AchCat.TOUR),  // 已完成
    CHAMPION(AchCat.TOUR),  // 已完成
    SECOND_PLACE(AchCat.TOUR),  // 已完成
    BEST_FOUR(AchCat.TOUR),  // 已完成

    // 斯诺克巡回赛
    SNOOKER_TOP_16(AchCat.SNOOKER_TOUR),  // 已完成
    SNOOKER_TOP_1(AchCat.SNOOKER_TOUR),  // 已完成
    SNOOKER_RANKED_CHAMPION(AchCat.SNOOKER_TOUR),  // 已完成
    SNOOKER_WORLD_CHAMPION(AchCat.SNOOKER_TOUR),  // 已完成
    SNOOKER_TRIPLE_CROWN(AchCat.SNOOKER_TOUR),  // 已完成
    POTTING_MACHINE(AchCat.SNOOKER_TOUR),  // 已完成
    ;

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

    public boolean isHidden() {
        return false;
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
