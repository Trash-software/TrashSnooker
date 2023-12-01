package trashsoftware.trashSnooker.core.metrics;

import trashsoftware.trashSnooker.core.BreakRule;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.Game;
import trashsoftware.trashSnooker.core.cue.CueSize;
import trashsoftware.trashSnooker.core.training.TrainType;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

import java.util.Set;

/**
 * 一个比赛类型。
 * <p>
 * 一文厘清这几个的关系:
 * GameType指这个游系，如斯诺克，与GameValues其实是完全对应的，只是因为初期设计原因分开了。
 *
 * @see TableMetrics 已经说了
 * @see trashsoftware.trashSnooker.core.table.Table 与GameType还是一一对应的，只不过主要功能是绘图
 * @see EntireGame 一场比赛的实例
 * @see Game 一局游戏的实例
 */
public enum GameRule {
    SNOOKER(22, "Snooker",
            new CueSize[]{CueSize.VERY_SMALL, CueSize.SMALL},
            new TrainType[]{TrainType.SNAKE_FULL, TrainType.SNAKE_FULL_DENSE,
                    TrainType.SNAKE_HALF, TrainType.SNAKE_CROSS, TrainType.SNAKE_X, TrainType.CLEAR_COLOR},
            BreakRule.ALTERNATE,
            Set.of(Rule.FOUL_AND_MISS, Rule.FOUL_LET_OTHER_PLAY)) {
        @Override
        public boolean snookerLike() {
            return true;
        }
    },
    MINI_SNOOKER(13, "MiniSnooker",
            new CueSize[]{CueSize.VERY_SMALL, CueSize.SMALL},
            new TrainType[]{TrainType.SNAKE_FULL, TrainType.SNAKE_HALF, TrainType.SNAKE_CROSS, TrainType.CLEAR_COLOR},
            BreakRule.ALTERNATE,
            Set.of(Rule.HIT_CUSHION, Rule.FOUL_BALL_IN_HAND)) {
        @Override
        public boolean snookerLike() {
            return true;
        }
    },
    CHINESE_EIGHT(16, "ChineseEight",
            new CueSize[]{CueSize.MEDIUM, CueSize.SMALL, CueSize.BIG},
            new TrainType[]{TrainType.SNAKE_FULL, TrainType.SNAKE_HALF,
                    TrainType.SNAKE_FULL_ORDERED, TrainType.SNAKE_HALF_ORDERED},
            BreakRule.WINNER,
            Set.of(Rule.HIT_CUSHION, Rule.FOUL_BALL_IN_HAND)) {
        @Override
        public boolean poolLike() {
            return true;
        }

        @Override
        public boolean eightBallLike() {
            return true;
        }
    },
    LIS_EIGHT(16, "LisEight",
            new CueSize[]{CueSize.MEDIUM, CueSize.SMALL, CueSize.BIG},
            new TrainType[]{TrainType.SNAKE_FULL, TrainType.SNAKE_HALF,
                    TrainType.SNAKE_FULL_ORDERED, TrainType.SNAKE_HALF_ORDERED},
            BreakRule.WINNER,
            Set.of(Rule.FOUL_AND_MISS, Rule.FOUL_LET_OTHER_PLAY)) {
        @Override
        public boolean poolLike() {
            return true;
        }

        @Override
        public boolean eightBallLike() {
            return true;
        }
    },
    AMERICAN_NINE(10, "AmericanNine",
            new CueSize[]{CueSize.BIG, CueSize.MEDIUM},
            new TrainType[]{TrainType.SNAKE_FULL,
                    TrainType.SNAKE_FULL_ORDERED, TrainType.SNAKE_HALF_ORDERED},
            BreakRule.WINNER,
            Set.of(Rule.HIT_CUSHION, Rule.FOUL_BALL_IN_HAND, Rule.POCKET_INDICATION, Rule.PUSH_OUT)) {
        @Override
        public boolean poolLike() {
            return true;
        }
    };

    public final String sqlKey;
    public final int nBalls;
    public final CueSize[] suggestedCues;
    public final TrainType[] supportedTrainings;
    public final BreakRule breakRule;
    public final Set<Rule> ruleSet;

    GameRule(int nBalls, 
             String sqlKey, 
             CueSize[] suggestedCues,
             TrainType[] supportedTrainings,
             BreakRule breakRule,
             Set<Rule> ruleSet) {
        this.nBalls = nBalls;
        this.sqlKey = sqlKey;
        this.suggestedCues = suggestedCues;
        this.supportedTrainings = supportedTrainings;
        this.breakRule = breakRule;
        this.ruleSet = ruleSet;
    }

    public static GameRule fromSqlKey(String sqlKey) {
        for (GameRule gameRule : values()) {
            if (gameRule.sqlKey.equalsIgnoreCase(sqlKey)) return gameRule;
        }
        throw new EnumConstantNotPresentException(GameRule.class, sqlKey);
    }

    public static String toReadable(GameRule gameRule) {
        String key = Util.toLowerCamelCase(gameRule.sqlKey);
        return App.getStrings().getString(key);
    }
    
    public static BallMetrics getDefaultBall(GameRule rule) {
        switch (rule) {
            case SNOOKER:
            case MINI_SNOOKER:
                return BallMetrics.SNOOKER_BALL;
            default:
                return BallMetrics.POOL_BALL;
        }
    }

    public boolean snookerLike() {
        return false;
    }

    public boolean poolLike() {
        return false;
    }

    public boolean eightBallLike() {
        return false;
    }
    
    public boolean hasRule(Rule rule) {
        return ruleSet.contains(rule);
    }

    @Override
    public String toString() {
        return toReadable(this);
    }

    public String toSqlKey() {
        return sqlKey;
    }
}
