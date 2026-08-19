package trashsoftware.trashSnooker.core.metrics;

import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.core.BreakRule;
import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.Game;
import trashsoftware.trashSnooker.core.cue.CueSize;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.core.snooker.SnookerBall;
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
    SNOOKER_TEN(17, "SnookerTen",
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
    //    public final int nBalls;
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
//        this.nBalls = nBalls;
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
        return switch (rule) {
            case SNOOKER, MINI_SNOOKER, SNOOKER_TEN -> BallMetrics.SNOOKER_BALL;
            default -> BallMetrics.POOL_BALL;
        };
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

    public Color ballBaseColor(int value) {
        if (snookerLike()) return SnookerBall.snookerColor(value);
        else if (poolLike()) return PoolBall.poolBallBaseColor(value);
        else throw new RuntimeException("Not implemented yet");
    }

    public int[] sessionDivision(int totalFrames) {
        return switch (this) {
            case SNOOKER -> {
                if (totalFrames >= 33) {
                    yield new int[]{8, 9, 8, totalFrames - 25};
                } else if (totalFrames >= 25) {
                    yield new int[]{8, 8, totalFrames - 16};
                } else if (totalFrames >= 19) {
                    yield new int[]{9, totalFrames - 9};
                } else if (totalFrames >= 17) {
                    yield new int[]{8, totalFrames - 8};
                } else {
                    yield new int[]{totalFrames};
                }
            }
            case CHINESE_EIGHT, MINI_SNOOKER, SNOOKER_TEN -> {
                if (totalFrames >= 29) {
                    int half = totalFrames / 2;
                    yield new int[]{half, totalFrames - half};
                } else yield new int[]{totalFrames};
            }
            default -> new int[]{totalFrames};
        };
    }
    
    private int[] divideSubsessionDefault(int sessionFrames, int desiredSubsessionLength) {
        int nRests = sessionFrames / desiredSubsessionLength;
        int nFinal = sessionFrames - nRests * desiredSubsessionLength;
        int[] sub;
        if (nFinal < desiredSubsessionLength / 2) {
            // 延长最后一个小session
            sub = new int[nRests];
            for (int i = 0; i < nRests - 1; i++) {
                sub[i] = desiredSubsessionLength;
            }
            sub[sub.length - 1] = desiredSubsessionLength + nFinal;
        } else {
            // 加一个session并缩短
            sub = new int[nRests + 1];
            for (int i = 0; i < nRests; i++) {
                sub[i] = desiredSubsessionLength;
            }
            sub[sub.length - 1] = nFinal;
        }
        return sub;
    }
    
    public int[] subSessionDivision(int sessionFrames) {
        return switch (this) {
            case SNOOKER -> {
                if (sessionFrames <= 6) {
                    yield new int[]{sessionFrames};
                } else if (sessionFrames <= 10) {
                    yield new int[]{4, sessionFrames - 4};
                } else {
                    yield divideSubsessionDefault(sessionFrames, 4);
                }
            }
            case SNOOKER_TEN -> divideSubsessionDefault(sessionFrames, 6);
            default -> divideSubsessionDefault(sessionFrames, 8);
        };
    }
}
