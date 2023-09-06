package trashsoftware.trashSnooker.core.career.achievement;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.EventLogger;
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
    UNIQUE_DEFEAT(AchCat.UNIQUE_DEFEATS, Type.COLLECTIVE) {
        @Override
        public int getCompletedLevelIndex(AchCompletion completion) {
            if (completion == null) return -1;
            return 0;
        }

        @Override
        public int getNCompleted(AchCompletion completion) {
            if (completion instanceof AchCompletion.Collective collective) {
                return collective.getNCompleted();
            } else if (completion instanceof AchCompletion.Sub sub) {
                return sub.getNCompleted();
            } else {
                EventLogger.error("Achievement " + name() + " does not support " + completion);
                return 0;
            }
        }

        @Override
        public boolean isFullyComplete(AchCompletion completion) {
            return getNCompleted(completion) >= 1;
        }
    },
    
    POT_A_BALL(AchCat.GENERAL_TABLE),  // 已完成
    POT_BALLS(AchCat.GENERAL_TABLE, Type.CUMULATIVE, 10, 50, 500),  // 已完成
    POT_BALLS_REST(AchCat.GENERAL_TABLE, Type.CUMULATIVE, 3, 20, 100),  // 已完成
    POT_BALLS_ANTI(AchCat.GENERAL_TABLE, Type.CUMULATIVE, 1, 10, 30),  // 已完成
    CUMULATIVE_LONG_POTS_1(AchCat.GENERAL_TABLE, Type.CUMULATIVE, 5, 20, 100),  // 已完成
    THREE_BALLS_IN_A_ROW(AchCat.GENERAL_TABLE),  // 已完成
    POSITIONING_MASTER(AchCat.GENERAL_TABLE, Type.HIGH_RECORD, 7),  // 已完成
    GAIN_BY_SNOOKER(AchCat.GENERAL_TABLE),  // 已完成
    SOLVE_SNOOKER_SUCCESS(AchCat.GENERAL_TABLE),  // 已完成
    SOLVE_SNOOKER_SUCCESS_POT(AchCat.GENERAL_HIDDEN),  // 已完成
    ACCURACY_WIN(AchCat.GENERAL_TABLE),  // 已完成
    ACCURACY_WIN_LONG(AchCat.GENERAL_TABLE),  // 已完成
    CONTINUOUS_LONG_POT(AchCat.GENERAL_TABLE),  // 已完成
    POT_TWO_LEGAL(AchCat.GENERAL_TABLE),  // 已完成
    POT_THREE_LEGAL(AchCat.GENERAL_TABLE),  // 已完成
    DOUBLE_POT(AchCat.GENERAL_TABLE, Type.CUMULATIVE, 1, 10, 30),  // 已完成
    PASS_POT(AchCat.GENERAL_TABLE, Type.CUMULATIVE, 1, 10, 30),  // 已完成
    AROUND_TABLE_POSITION(AchCat.GENERAL_TABLE),  // 已完成
    MULTI_CUSHION_POSITION_1(AchCat.GENERAL_TABLE),  // 已完成
    MULTI_CUSHION_POSITION_2(AchCat.GENERAL_TABLE),  // 已完成
    MULTI_CUSHION_ESCAPE(AchCat.GENERAL_TABLE),  // 已完成
    MULTIPLE_EASY_FAILS(AchCat.GENERAL_TABLE, true),  // 已完成

    WIN_A_FRAME(AchCat.GENERAL_MATCH),  // 已完成
    WIN_FRAMES(AchCat.GENERAL_MATCH, Type.CUMULATIVE, 5, 20, 100),  // 已完成
    WIN_A_MATCH(AchCat.GENERAL_MATCH),  // 已完成
    WIN_MATCHES(AchCat.GENERAL_MATCH, Type.CUMULATIVE, 3, 10, 50),  // 已完成
    WIN_ALL_MATCHES(AchCat.GENERAL_MATCH),  // 已完成
    WIN_FRAMES_NORMAL_DIFFICULTY(AchCat.GENERAL_MATCH, Type.CUMULATIVE),  // 已完成
    SEMIFINAL_STAGE(AchCat.GENERAL_MATCH),  // 已完成
    FINAL_STAGE(AchCat.GENERAL_MATCH),  // 已完成
    FINAL_FRAME(AchCat.GENERAL_MATCH),  // 已完成
    FINAL_FRAME_USUAL_GUEST(AchCat.GENERAL_MATCH, Type.CUMULATIVE, 5),  // 已完成
    FINAL_STAGE_FINAL_FRAME(AchCat.GENERAL_MATCH),  // 已完成
    FINAL_STAGE_FINAL_FRAME_WIN(AchCat.GENERAL_HIDDEN),  // 已完成
    LEGENDARY_REVENGE(AchCat.GENERAL_MATCH),  // 已完成

    LONG_THINK(AchCat.GENERAL_HIDDEN),  // 已完成
    COMPLETE_LOSS(AchCat.GENERAL_HIDDEN),  // 已完成
    MISCUED(AchCat.GENERAL_HIDDEN),  // 已完成
    SHAKE_POCKET(AchCat.GENERAL_HIDDEN),  // 已完成
    FREE_BALL_FAIL(AchCat.GENERAL_HIDDEN),  // 已完成

    CUE_BALL_POT(AchCat.GENERAL_HIDDEN),  // 已完成
    MISSED_SHOT(AchCat.GENERAL_HIDDEN),  // 已完成

    FRAME_NO_ATTACK(AchCat.GENERAL_HIDDEN),  // 已完成
    LIE_DOWN_WIN(AchCat.GENERAL_HIDDEN),  // 已完成
    KEY_BALL_FAIL(AchCat.GENERAL_HIDDEN),  // 已完成
    POT_FAIL_THREE(AchCat.GENERAL_HIDDEN),  // 已完成
    LOST_ALL_MATCHES(AchCat.GENERAL_HIDDEN),  // 已完成
    BIG_HEART(AchCat.GENERAL_HIDDEN, Type.CUMULATIVE, 3),  // 已完成
    ONE_ROUND_TOUR(AchCat.GENERAL_HIDDEN),  // 已完成
    LEGENDARY_REVENGED(AchCat.GENERAL_HIDDEN),  // 已完成

    // 斯诺克
    SNOOKER_BREAK_50(AchCat.SNOOKER),  // 已完成
    SNOOKER_BREAK_100(AchCat.SNOOKER),  // 已完成
    SNOOKER_BREAK_100_BIG(AchCat.SNOOKER),  // 已完成
    SNOOKER_BREAK_147(AchCat.SNOOKER, true), // 已完成
    SNOOKER_CUMULATE_SCORE(AchCat.SNOOKER, Type.CUMULATIVE, 200, 1000, 10000),  // 已完成
    SNOOKER_CUMULATE_FOUL_GAIN(AchCat.SNOOKER, Type.CUMULATIVE, 10, 50, 200),  // 已完成
    THREE_MISS_LOST(AchCat.SNOOKER),  // 已完成
    HARD_SNOOKER_BY_OPPONENT(AchCat.SNOOKER),  // 已完成
    HARD_SNOOKER_BY_HUMAN(AchCat.SNOOKER),  // 已完成
    UNSOLVABLE_SNOOKER(AchCat.SNOOKER, true),  // 已完成 
    COME_BACK_BEHIND_65(AchCat.SNOOKER),  // 已完成
    COME_BACK_BEHIND_OVER_SCORE(AchCat.SNOOKER),  // 已完成
    NOT_RESPECT(AchCat.SNOOKER),  // 已完成
    SUM_BELOW(AchCat.SNOOKER),  // 已完成
    SUM_BELOW_2(AchCat.SNOOKER, true),  // 已完成
    SNOOKER_NO_POT(AchCat.SNOOKER, true),  // 已完成
    SUM_OVER_147(AchCat.SNOOKER),  // 已完成
    POT_WRONG_COLOR(AchCat.SNOOKER, true),  // 已完成

    // Pool general
    DOUBLE_POT_WIN(AchCat.POOL_GENERAL),  // 已完成
    POOL_BREAK_POT(AchCat.POOL_GENERAL),  // 已完成
    POOL_BREAK_MASTER(AchCat.POOL_GENERAL, Type.HIGH_RECORD, 2),  // 已完成
    POOL_CLEAR(AchCat.POOL_GENERAL),  // 已完成
    POOL_BREAK_CLEAR(AchCat.POOL_GENERAL),  // 已完成
    SUICIDE(AchCat.POOL_GENERAL),  // 已完成

    // 中八
    BLIND_SHOT(AchCat.CHINESE_EIGHT),  // 已完成
    POT_OPPONENT_BALL(AchCat.CHINESE_EIGHT),  // 已完成
    REMAIN_ONE_MUST_LOSE(AchCat.CHINESE_EIGHT),  // 已完成
    CEB_CUMULATIVE_CLEAR(AchCat.CHINESE_EIGHT, Type.CUMULATIVE, 5, 10, 20),  // 已完成
    CHINESE_EIGHT_NO_POT(AchCat.CHINESE_EIGHT, true),  // 已完成

    GOLDEN_NINE(AchCat.AMERICAN_NINE),  // 已完成
    WHITE_GOLDEN_NINE(AchCat.AMERICAN_NINE, true),  // 已完成
    BALL_WORKER(AchCat.AMERICAN_NINE),  // 已完成
    PASS_NINE(AchCat.AMERICAN_NINE),  // 已完成
    AMERICAN_NINE_NO_POT(AchCat.AMERICAN_NINE, true),  // 已完成

    // 巡回赛
    EARNED_MONEY(AchCat.TOUR),  // 已完成
    EARN_MONEY_CUMULATIVE(AchCat.TOUR, Type.CUMULATIVE, 50000, 200000, 1000000),  // 已完成
    CHAMPION(AchCat.TOUR),  // 已完成
    SECOND_PLACE(AchCat.TOUR),  // 已完成
    BEST_FOUR(AchCat.TOUR),  // 已完成
    DEFEAT_BY_CHAMPION(AchCat.TOUR, true),
    PLAY_ONE_YEAR(AchCat.TOUR),  // 已完成
    PLAY_TWO_YEARS(AchCat.TOUR),  // 已完成
    PLAY_FIVE_YEARS(AchCat.TOUR ,true),  // 已完成
    PLAY_TEN_YEARS(AchCat.TOUR, true),  // 已完成
    PLAY_TWENTY_YEARS(AchCat.TOUR, true),  // 已完成

    // 斯诺克巡回赛
    SNOOKER_TOP_64(AchCat.SNOOKER_TOUR),  // 已完成
    SNOOKER_TOP_16(AchCat.SNOOKER_TOUR),  // 已完成
    SNOOKER_TOP_1(AchCat.SNOOKER_TOUR),  // 已完成
    SNOOKER_RANKED_CHAMPION(AchCat.SNOOKER_TOUR),  // 已完成
    SNOOKER_WORLD_CHAMPION(AchCat.SNOOKER_TOUR),  // 已完成
    SNOOKER_TRIPLE_CROWN(AchCat.SNOOKER_TOUR),  // 已完成
    POTTING_MACHINE(AchCat.SNOOKER_TOUR),  // 已完成
    FIRST_YEAR_WORLD_CHAMP_SHIP(AchCat.SNOOKER_TOUR, true),  // todo: 目前的赛季开始日期不支持
    DEFEAT_UNIQUE_OPPONENTS_SNOOKER(AchCat.SNOOKER_TOUR, Type.CUMULATIVE, 5, 10, 25),
    DEFEAT_SAME_OPPONENT_CONTINUOUS_SNOOKER(AchCat.SNOOKER_TOUR),  // 已完成
    DEFEAT_SAME_OPPONENT_MULTI_SNOOKER_1(AchCat.SNOOKER_TOUR),  // 已完成
    DEFEAT_SAME_OPPONENT_MULTI_SNOOKER_2(AchCat.SNOOKER_TOUR),  // 已完成
    
    // 中式台球巡回赛
    DEFEAT_UNIQUE_OPPONENTS_CEB(AchCat.CHINESE_EIGHT_TOUR, Type.CUMULATIVE, 5, 10, 25),
    DEFEAT_SAME_OPPONENT_CONTINUOUS_CEB(AchCat.CHINESE_EIGHT_TOUR),  // 已完成
    DEFEAT_SAME_OPPONENT_MULTI_CEB_1(AchCat.CHINESE_EIGHT_TOUR),  // 已完成
    DEFEAT_SAME_OPPONENT_MULTI_CEB_2(AchCat.CHINESE_EIGHT_TOUR),  // 已完成

    // 美式九球巡回赛
    DEFEAT_UNIQUE_OPPONENTS_AMERICAN(AchCat.AMERICAN_NINE_TOUR, Type.CUMULATIVE, 5, 10, 25),
    DEFEAT_SAME_OPPONENT_CONTINUOUS_AMERICAN(AchCat.AMERICAN_NINE_TOUR),  // 已完成
    DEFEAT_SAME_OPPONENT_MULTI_AMERICAN_1(AchCat.AMERICAN_NINE_TOUR),  // 已完成
    DEFEAT_SAME_OPPONENT_MULTI_AMERICAN_2(AchCat.AMERICAN_NINE_TOUR),  // 已完成
    ;

    public final AchCat category;
    public final Type type;
    public final int[] requiredTimes;
    private final boolean hidden;

    Achievement(AchCat category, Type type, boolean hidden, int... requiredTimes) {
        this.category = category;
        this.type = type;
        this.requiredTimes = requiredTimes;
        this.hidden = hidden;
    }

    Achievement(AchCat category, Type type, int... requiredTimes) {
        this(category, type, false, requiredTimes);
    }

    Achievement(AchCat category) {
        this(category, false);
    }

    Achievement(AchCat category, boolean hidden) {
        this(category, Type.ONE_TIME, hidden, 1);
    }

    public static Achievement fromKey(String key) {
        return valueOf(Util.toAllCapsUnderscoreCase(key));
    }

    public boolean isHidden() {
        return hidden;
    }

    public AchCat getCategory() {
        return category;
    }

    public Type getType() {
        return type;
    }

    public boolean isFullyComplete(AchCompletion completion) {
        return completion != null && getNCompleted(completion) == getNLevels();
    }
    
    public int[] getLevels() {
        return requiredTimes;
    }
    
    public int getNLevels() {
        return requiredTimes.length;
    }

    public int getCompletedLevelIndex(AchCompletion completion) {
        if (completion == null) return -1;
        
        for (int i = requiredTimes.length - 1; i >= 0; i--) {
            if (requiredTimes[i] <= completion.getTimes()) {
                return i;
            }
        }
        return -1;
    }
    
    public int getNCompleted(AchCompletion completion) {
        return getCompletedLevelIndex(completion) + 1;
    }

    public String toKey() {
        return Util.toLowerCamelCase(name());
    }

    public String title() {
        String resName = Util.toLowerCamelCase("ACH_" + name());
        try {
            return App.getAchievementStrings().getString(resName);
        } catch (MissingResourceException e) {
            EventLogger.error(resName);
            return name();
        }
    }

    String description() {
        try {
            return App.getAchievementStrings().getString(Util.toLowerCamelCase("ACH_DES_" + name()));
        } catch (MissingResourceException e) {
            return name();
        }
    }
    
    public String getDescriptionOfLevel(int levelIndex) {
        String s = description();
        if (s.contains("%d")) {
            return String.format(s, getLevels()[levelIndex]);
        } else {
            return s;
        }
    }
    
    public enum Type {
        ONE_TIME,
        HIGH_RECORD,
        CUMULATIVE,
        COLLECTIVE
    }
}
