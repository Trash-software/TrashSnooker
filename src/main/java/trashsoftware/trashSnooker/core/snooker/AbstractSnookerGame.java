package trashsoftware.trashSnooker.core.snooker;

import org.jetbrains.annotations.Nullable;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.ai.AiCue;
import trashsoftware.trashSnooker.core.ai.SnookerAiCue;
import trashsoftware.trashSnooker.core.attempt.PotAttempt;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.metrics.Rule;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.movement.WhitePrediction;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;
import trashsoftware.trashSnooker.core.scoreResult.SnookerScoreResult;
import trashsoftware.trashSnooker.core.table.AbstractSnookerTable;
import trashsoftware.trashSnooker.core.table.Table;

import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

public abstract class AbstractSnookerGame extends Game<SnookerBall, SnookerPlayer> {

    public static final int RAW_COLORED_REP = 0;  // 代表任意彩球
    public final double redRowOccupyX;
    public final double redGapDt;
    protected final SnookerBall[] redBalls = new SnookerBall[numRedBalls()];
    protected SnookerBall[] coloredBalls;
    protected SnookerBall goldenBall;
    /*
    球堆两侧的倒数第一颗球
     */
    protected final Set<Ball> suggestedRegularBreakBalls = new HashSet<>();
    /*
     红球队最下方一排左右两侧红球球心的位置
     
     从黑球方向看，左边一颗在前，右边一颗在后
     */
    protected double[][] cornerRedBallPoses = new double[2][];
    //    private int lastFoulPoints = 0;
    private boolean doingFreeBall = false;  // 正在击打自由球
    private boolean blackBattle = false;
    private int repositionCount;
    private int continuousFoulAndMiss;
    private boolean willLoseBecauseThisFoul;
    private boolean isSolvable;  // 球形是否有解
    private int indicatedTarget;
    private boolean targetManualIndicated = false;

    private int maxScoreDiff;  // 最大分差，p1的分减p2的分
    private boolean p1EverOver;  // p1 p2分别是否曾经超了分
    private boolean p2EverOver;
    private boolean isOnMaximum = true;  // 是否还有机会冲击147

    protected AbstractSnookerGame(EntireGame entireGame,
                                  GameSettings gameSettings,
                                  GameValues gameValues,
                                  Table table,
                                  int frameIndex,
                                  int frameRestartIndex) {
        super(entireGame, gameSettings, gameValues, table, frameIndex, frameRestartIndex);

//        subRules.add(SubRule.SNOOKER_STD);

        redRowOccupyX = gameValues.ball.ballDiameter * Math.sin(Math.toRadians(60.0)) +
                Game.MIN_PLACE_DISTANCE * 0.8;
        redGapDt = Game.MIN_PLACE_DISTANCE;

        currentTarget = 1;

        SnookerBall yellowBall = new SnookerBall(2, getTable().yellowBallPos(), gameValues);
        SnookerBall greenBall = new SnookerBall(3, getTable().greenBallPos(), gameValues);
        SnookerBall brownBall = new SnookerBall(4, getTable().brownBallPos(), gameValues);
        SnookerBall blueBall = new SnookerBall(5, getTable().blueBallPos(), gameValues);
        SnookerBall pinkBall = new SnookerBall(6, getTable().pinkBallPos(), gameValues);
        SnookerBall blackBall = new SnookerBall(7, getTable().blackBallPos(), gameValues);

        initRedBalls();

        int nColorBalls = 6;
        if (gameValues.hasSubRule(SubRule.SNOOKER_GOLDEN)) {
            nColorBalls = 7;
            goldenBall = new SnookerBall(20,
                    new double[]{gameValues.table.leftX + gameValues.ball.ballRadius + 1.0, gameValues.table.midY},
                    gameValues);
        }

        coloredBalls = new SnookerBall[6];
        allBalls = new SnookerBall[redBalls.length + nColorBalls + 1];
        allBalls[0] = cueBall;
        System.arraycopy(redBalls, 0, allBalls, 1, redBalls.length);
        allBalls[redBalls.length + 1] = yellowBall;
        allBalls[redBalls.length + 2] = greenBall;
        allBalls[redBalls.length + 3] = brownBall;
        allBalls[redBalls.length + 4] = blueBall;
        allBalls[redBalls.length + 5] = pinkBall;
        allBalls[redBalls.length + 6] = blackBall;

        System.arraycopy(allBalls, redBalls.length + 1, coloredBalls, 0, 6);

        if (goldenBall != null) {
            allBalls[redBalls.length + 7] = goldenBall;
        }
    }

    public static String ballValueToColorName(int ballValue, ResourceBundle strings) {
        return switch (ballValue) {
            case 1 -> strings.getString("redBall");
            case 2 -> strings.getString("yellowBall");
            case 3 -> strings.getString("greenBall");
            case 4 -> strings.getString("brownBall");
            case 5 -> strings.getString("blueBall");
            case 6 -> strings.getString("pinkBall");
            case 7 -> strings.getString("blackBall");
            case 20 -> strings.getString("goldBall");
            case 0 -> strings.getString("coloredBall");
            default -> throw new RuntimeException();
        };
    }

    /**
     * 返回单杆最大的球数，特殊规则如金球不计算在内
     */
    public static int maxBreakCueCount(GameRule gameRule) {
        return switch (gameRule) {
            case SNOOKER -> 36;
            case SNOOKER_TEN -> 26;
            case MINI_SNOOKER -> 18;
            default -> throw new IllegalArgumentException("Game rule " + gameRule.name() + " is not snooker-like.");
        };
    }

    /**
     * 返回总共的球数，白球以及特殊规则如金球不计算在内
     */
    public static int nBalls(GameRule gameRule) {
        return switch (gameRule) {
            case SNOOKER -> 21;
            case SNOOKER_TEN -> 16;
            case MINI_SNOOKER -> 12;
            default -> throw new IllegalArgumentException("Game rule " + gameRule.name() + " is not snooker-like.");
        };
    }

    @Override
    public AbstractSnookerTable getTable() {
        return (AbstractSnookerTable) super.getTable();
    }

    protected abstract int numRedBalls();

    protected int numRedRows() {
        int nRedBalls = numRedBalls();
        int cum = 0;
        int rows = 0;
        while (cum < nRedBalls) {
            rows++;
            cum += rows;
        }
        return rows;
    }

    @Override
    protected void cloneBalls(SnookerBall[] allBalls) {
        SnookerBall[] allBallsCopy = new SnookerBall[allBalls.length];
        for (int i = 0; i < allBalls.length; i++) {
            allBallsCopy[i] = (SnookerBall) allBalls[i].clone();
        }
        this.allBalls = allBallsCopy;
        // fixme: 这两行启用之后，彩球进了就捡不出来了，西八
//        System.arraycopy(allBallsCopy, 0, this.redBalls, 0, table.nBalls() - 7);
//        System.arraycopy(allBallsCopy, table.nBalls() - 7, this.coloredBalls, 0, 6);
    }

    @Override
    protected void initPlayers() {
        player1 = new SnookerPlayer(gameSettings.getPlayer1(), this);
        player2 = new SnookerPlayer(gameSettings.getPlayer2(), this);
    }

    @Override
    protected SnookerBall createWhiteBall() {
        return new SnookerBall(0, gameValues);
    }

    public SnookerBall getBallOfValue(int score) {
        return switch (score) {
            case 1 -> {
                for (int i = 0; i < numRedBalls(); i++) {
                    if (!redBalls[i].isPotted()) yield redBalls[i];
                }
                yield redBalls[0];  // all reds potted
            }
            case 2 -> allBalls[numRedBalls() + 1];
            case 3 -> allBalls[numRedBalls() + 2];
            case 4 -> allBalls[numRedBalls() + 3];
            case 5 -> allBalls[numRedBalls() + 4];
            case 6 -> allBalls[numRedBalls() + 5];
            case 7 -> allBalls[numRedBalls() + 6];
            case 20 -> allBalls[numRedBalls() + 7];  // 金球
            default -> throw new RuntimeException("没有这种彩球。");
        };
    }

    @Override
    protected AiCue<?, ?> createAiCue(SnookerPlayer aiPlayer) {
        return new SnookerAiCue(this, aiPlayer);
    }

    /**
     * 斯诺克击打彩球时（target=0），需要指定目标球
     */
    public void setIndicatedTarget(int indicatedTarget, boolean manualIndication) {
           if ((!this.targetManualIndicated) || manualIndication) {
            this.indicatedTarget = indicatedTarget;
        }
        this.targetManualIndicated |= manualIndication;
    }

    public int getIndicatedTarget() {
        return indicatedTarget;
    }

    public int getScoreDiffAbs() {
        return Math.abs(player1.getScore() - player2.getScore());
    }

    /**
     * 领先正，落后负
     */
    public int getScoreDiff(SnookerPlayer player) {
        SnookerPlayer another = player == player1 ? player2 : player1;
        return player.getScore() - another.getScore();
    }

    /**
     * 返回当前的台面剩余，非负
     */
    public int getRemainingScore(boolean isSnookerFreeBall) {
        // fixme: 考虑斯诺克自由球
        if (currentTarget == 1) {
            return remainingRedCount() * 8 + 27;
        } else if (currentTarget == RAW_COLORED_REP) {
            return remainingRedCount() * 8 + 34;
        } else if (currentTarget == END_REP) {
            return 0;
        } else {
            int scoreSum = 0;
            for (int sc = currentTarget; sc <= 7; sc++) {
                scoreSum += sc;
            }
            return scoreSum;
        }
    }

    public boolean hasFreeBall() {
        if (thisCueFoul.isFoul()) {
            // 当从白球处无法看到任何一颗目标球的最薄边时
            List<Ball> currentTarBalls = getAllLegalBalls(currentTarget, false, false);
            int canSeeBallCount = countSeeAbleTargetBalls(cueBall.getX(), cueBall.getY(),
                    currentTarBalls, 3).seeAbleTargets;
            System.out.println("Target: " + currentTarget + ", Free ball check: " +
                    canSeeBallCount + ", n targets: " + currentTarBalls.size());
            return canSeeBallCount == 0;
        } else {
            return false;
        }
    }

    /**
     * 对于斯诺克，pottingBall没有任何作用
     */
    @Override
    public int getTargetAfterPotSuccess(Ball pottingBall, boolean isFreeBall) {
        if (currentTarget == 1) {
            return RAW_COLORED_REP;
        } else if (currentTarget == RAW_COLORED_REP) {
            if (hasRed()) return 1;
            else return 2;  // 最后一颗红球附带的彩球打完
        } else if (currentTarget == 7) {  // 黑球进了
            if (goldenBall != null && !goldenBall.isPotted()) {
                return 20;
            } else {
                return END_REP;
            }
        } else if (currentTarget == 20) {
            return END_REP;
        } else if (!isFreeBall) {
            return currentTarget + 1;
        }
        // 其余情况：清彩球阶段的自由球，目标球不变
        return currentTarget;
    }

    @Override
    public int getTargetAfterPotFailed() {
        if (hasRed()) return 1;  // 剩余红球在台面时，无论打的是红还是彩，打不进之后都应该打红
        else if (currentTarget == RAW_COLORED_REP  // 最后一颗红球附带的彩球进攻失败
                || currentTarget == 1)  // 进攻失败或犯规，但最后一颗红球进了
            return 2;
        else return currentTarget;  // 其他情况目标球不变
    }

    @Override
    public Movement cue(CuePlayParams params, Phy phy) {
        isSolvable = isSolvable(params);
        if (!isSolvable) {
            AchManager.getInstance().addAchievement(Achievement.UNSOLVABLE_SNOOKER, getP1().isHuman() ? getP1() : getP2());
        }
        SnookerPlayer cuing = getCuingPlayer();
        if (cuing.getInGamePlayer().isHuman()) {
            int scoreDiff = getScoreDiff(cuing);
            if (scoreDiff < 0) {
                int rem = getRemainingScore(isDoingFreeBall());
                if (-scoreDiff >= rem + 20) {
                    AchManager.getInstance().addAchievement(Achievement.NOT_CONCEDE,
                            cuing.getInGamePlayer());
                }
            }
        }
        return super.cue(params, phy);
    }

    protected void updateTargetPotSuccess(boolean isFreeBall) {
        int nextTarget = getTargetAfterPotSuccess(null, isFreeBall);
        if (nextTarget == END_REP) {
            if (player1.getScore() != player2.getScore()) {
                currentTarget = nextTarget;
                end();
            } else {
                // 延分，争黑球
                blackBattle = true;
                cueBall.pot();
                currentTarget = 7;
                pickupColorBall(getBallOfValue(7));
                System.out.println("Black battle!");
                AchManager.getInstance().addAchievement(Achievement.BLACK_BATTLE, null);
                setBallInHand();
                if (Math.random() < 0.5) {
                    currentPlayer = player1;
                } else {
                    currentPlayer = player2;
                }
            }
        } else {
            currentTarget = nextTarget;
            if (nextTarget == 7 && currentPlayer.getScore() + 7 < getAnotherPlayer().getScore()) {
                end();  // 已经被超分了你还把粉球打了？傻逼
            }
        }
    }

    protected void updateTargetPotFailed() {
        currentTarget = getTargetAfterPotFailed();
        int scoreDiff = getScoreDiff(player1);
        if (currentTarget == 7 && Math.abs(scoreDiff) > 7) {
            // 已超分且只剩黑球，强制结束
            end();
        }
    }

    private int[] scoreFoulOfFreeBall(Set<SnookerBall> pottedBalls) {
//        System.out.println("Free ball of " + currentTarget);
        if (currentTarget == RAW_COLORED_REP) throw new RuntimeException("不可能自由球打任意彩球");
        Ball target = whiteFirstCollide;  // not null

//        if (currentTarget == target.getValue()) {
//            return new int[]{0, getDefaultFoulValue()};
//        }

        if (pottedBalls.isEmpty()) {
            return new int[2];
        } else if (pottedBalls.size() == 1) {
            for (Ball onlyBall : pottedBalls) {
                if (onlyBall == target) return new int[]{currentTarget, 0};
            }
            return new int[2];
        } else {
            // 自由球进多颗球，只有在一种情况：指定的球和原本目标球都进了才不犯规
            boolean foul = false;
            int foulScore = getDefaultFoulValue();
            int score = 0;
            for (Ball ball : pottedBalls) {
                if (ball == target || ball.getValue() == currentTarget) {
                    score += currentTarget;
                } else {
                    foul = true;
                    if (ball.getValue() > foulScore) foulScore = ball.getValue();
                }
            }
            if (foul) return new int[]{0, foulScore};
            else return new int[]{score, 0};
        }
    }

    private int getFoulScore(Set<SnookerBall> pottedBalls) {
        int foul = getDefaultFoulValue();
        for (SnookerBall ball : pottedBalls) {
            int ballFoulScore = getBallFoulScore(ball);
            if (ballFoulScore > foul) foul = ballFoulScore;
        }
//        if (foul == 0) throw new RuntimeException("No foul, why call this method");
        return foul;
    }

    public int getDefaultFoulValue() {
        if (currentTarget == RAW_COLORED_REP) return Math.max(4, indicatedTarget);
        else return Math.max(currentTarget, 4);
    }

    protected void updateScoreAndTarget(Set<SnookerBall> pottedBalls, boolean isFreeBall) {
        int score = 0;
        boolean baseFoul = checkStandardFouls(() -> getFoulScore(pottedBalls));
        if (baseFoul) {
            // 不同的分支
            if (whiteFirstCollide != null) {
                // == null的分支一定在baseFoul里面
                // 这里是处理多重犯规下，还有miss的情况
                if (!wasLegalBall(whiteFirstCollide,
                        currentTarget,
                        isFreeBall)) {
                    thisCueFoul.addFoul(
                            String.format(strings.getString("targetXOtherHit"),
                                    ballValueToColorName(currentTarget, strings)),
                            Math.max(4, Math.max(currentTarget, whiteFirstCollide.getValue())),
                            true
                    );
                }
            }
        } else if (isFreeBall) {
            int[] scoreFoul = scoreFoulOfFreeBall(pottedBalls);
            score = scoreFoul[0];
            int foul = scoreFoul[1];
            if (foul > 0) {
                thisCueFoul.addFoul(strings.getString("wrongFreeBall"), foul, true);
            } else if (score == 0) {
                // 没犯规也没进球，检查是不是直接用自由球做障碍了
                int opponentTarget = getTargetAfterPotFailed();
                List<Ball> oppoBalls = getAllLegalBalls(opponentTarget, false, false);

                Ball[] freeBall = new Ball[]{whiteFirstCollide};
                SeeAble seeAble = getAllSeeAbleBalls(cueBall.getX(),
                        cueBall.getY(),
                        oppoBalls, 1, null, freeBall);
                if (seeAble.seeAbleTargets == 0) {
                    foul = Math.max(4, currentTarget);  // 除非是在清彩阶段大于咖啡球的自由球，否则都是4分
                    thisCueFoul.addFoul(strings.getString("freeBallCannotSnooker"), foul, false);
                }
            }
        } else if (currentTarget == 1) {
            if (whiteFirstCollide.getValue() == 1) {
                boolean wrongPot = false;
                for (SnookerBall ball : pottedBalls) {
                    if (ball.isRed()) {
                        score++;  // 进了颗红球
                    } else {
                        // 进了颗彩球
                        thisCueFoul.addFoul(strings.getString("targetRedColorPots"),
                                getFoulScore(pottedBalls), false);
                        wrongPot = true;
                    }
                }
                if (wrongPot && score > 0) {
                    AchManager.getInstance().addAchievement(Achievement.POT_WRONG_COLOR, getCuingIgp());
                } else if (!wrongPot) {
                    if (score == 2) {
                        noHopeMaximum();
                        AchManager.getInstance().addAchievement(Achievement.POT_TWO_LEGAL, getCuingIgp());
                    } else if (score >= 3) {
                        noHopeMaximum();
                        AchManager.getInstance().addAchievement(Achievement.POT_THREE_LEGAL, getCuingIgp());
                    }
                }
            } else {  // 该打红球时打了彩球
                int foul = getBallFoulScore(whiteFirstCollide);
                thisCueFoul.addFoul(String.format(strings.getString("targetRedHitX"),
                                ballValueToColorName(whiteFirstCollide.getValue(), strings)),
                        foul,
                        true);
            }
            if (cueBall.isPotted()) {
                System.err.println("Should not enter this branch");
                thisCueFoul.addFoul(strings.getString("cueBallPot"), getFoulScore(pottedBalls), false);
                setBallInHand();
            }
        } else {
            int realTarget = whiteFirstCollide.getValue();
            if (realTarget == 1) {  // 该打彩球时打了红球
                int foul = Math.max(4, getFoulScore(pottedBalls));
                thisCueFoul.addFoul(String.format(strings.getString("targetXRedHit"),
                                ballValueToColorName(currentTarget, strings)),
                        foul,
                        true);
            } else {
                if (currentTarget == RAW_COLORED_REP) {
                    // 任意彩球
                    if (indicatedTarget == RAW_COLORED_REP) {
                        System.err.println("Program error");
                    } else if (indicatedTarget == 20 || realTarget == 20) {
                        int foul = 4;
                        thisCueFoul.addFoul(String.format(strings.getString("targetColorXHit"),
                                        ballValueToColorName(20, strings)),
                                foul,
                                true);
                    } else if (indicatedTarget != realTarget) {
                        int foul = Math.max(4, Math.max(realTarget, indicatedTarget));
                        thisCueFoul.addFoul(String.format(strings.getString("indicateXHitY"),
                                        ballValueToColorName(indicatedTarget, strings),
                                        ballValueToColorName(realTarget, strings)),
                                foul,
                                true);
                    }
                    if (indicatedTarget != 7) noHopeMaximum();
                }
                if (currentTarget != RAW_COLORED_REP && realTarget != currentTarget) {  // 打了非目标球的彩球
                    int realTarFoul = realTarget == 20 ? 4 : realTarget;
                    int curTarFoul = currentTarget == 20 ? 4 : currentTarget;

                    int foul = Math.max(4, Math.max(realTarFoul, curTarFoul));
                    thisCueFoul.addFoul(String.format(strings.getString("targetXOtherHit"),
                                    ballValueToColorName(currentTarget, strings)),
                            foul,
                            true);
                }
                if (pottedBalls.size() == 1) {
                    if (currentTarget == RAW_COLORED_REP) {  // 任意彩球
                        for (Ball onlyBall : pottedBalls) {
                            if (onlyBall == whiteFirstCollide) score = onlyBall.getValue();
                            else {
                                int foul = getBallFoulScore(onlyBall);
                                thisCueFoul.addFoul(strings.getString("targetColorOtherPot"),
                                        foul,
                                        false);
                            }
                        }
                    } else {  // 非任意彩球
                        for (Ball onlyBall : pottedBalls) {
                            if (onlyBall.getValue() == currentTarget) {
                                score = currentTarget;
                            } else {
                                thisCueFoul.addFoul(
                                        String.format(strings.getString("targetXPotY"),
                                                ballValueToColorName(currentTarget, strings),
                                                ballValueToColorName(onlyBall.getValue(), strings)),
                                        getFoulScore(pottedBalls),
                                        false
                                );
                            }
                        }
                    }
                } else if (!pottedBalls.isEmpty()) {
                    thisCueFoul.addFoul(
                            strings.getString("targetColorOtherPot"),
                            Math.max(indicatedTarget, getFoulScore(pottedBalls)),
                            false
                    );
                    for (SnookerBall potted : pottedBalls) {
                        // 进了彩球同时也误进其他彩球
                        if (potted.getValue() == indicatedTarget || potted.getValue() == currentTarget) {
                            AchManager.getInstance().addAchievement(Achievement.POT_WRONG_COLOR, getCuingIgp());
                        }
                    }
                    noHopeMaximum();
                }
            }
            if (cueBall.isPotted()) {
                System.err.println("Should not enter this branch");
                thisCueFoul.addFoul(strings.getString("cueBallPot"), 
                        Math.max(indicatedTarget, getFoulScore(pottedBalls)), 
                        false);
                setBallInHand();
            }
        }

        if (isBreaking()) {
            updateBreakStats(newPotted);
            if (!newPotted.isEmpty() && !thisCueFoul.isFoul()) {
                AchManager.getInstance().addAchievement(Achievement.SNOOKER_BREAK_POT,
                        newPotted.size(), getCuingIgp());
            }
            if (breakStats.nBallsHitCushion >= 10) {
                AchManager.getInstance().addAchievement(Achievement.SNOOKER_BIG_POWER_BREAK,
                        breakStats.nBallsHitCushion, getCuingIgp());
            }
        }

        if (thisCueFoul.isFoul()) {
            if (thisCueFoul.isMiss()) {
                // 看有没有解，如果无解，那就不算miss
                if (!isSolvable()) {
                    System.out.println("Unsolvable snooker, not miss");
                    thisCueFoul.setMiss(false);
                }
            }

            if (thisCueFoul.isMiss()) {
                continuousFoulAndMiss++;
            } else {
                continuousFoulAndMiss = 0;
                willLoseBecauseThisFoul = false;
            }

            if (willLoseBecauseThisFoul) {
                // 三次瞎打判负
                if (getCuingPlayer().getInGamePlayer().isHuman())
                    AchManager.getInstance().addAchievement(Achievement.THREE_MISS_LOST, getCuingIgp());
                addFoulScore();
                getCuingPlayer().withdraw();
                end();
                return;
            }

            if (blackBattle) {
                // 抢黑时犯规就直接判负
                addFoulScore();

                end();
                return;
            }

            addFoulScore();
            updateTargetPotFailed();
            switchPlayer();
            if (gameValues.rule.hasRule(Rule.FOUL_BALL_IN_HAND)) {
                setBallInHand();
            } else if (!cueBall.isPotted() && hasFreeBall()) {
                // todo: 要在pickup colored之后检查
                doingFreeBall = true;
                System.out.println("Free ball!");
            }
        } else {
            if (score > 0) {
                if (isFreeBall) {
                    if (pottedBalls.size() != 1)
                        throw new RuntimeException("为什么进了这么多自由球？？？");
                    currentPlayer.potFreeBall(score);
                } else {
                    currentPlayer.correctPotBalls(this, pottedBalls);
                }
                lastPotSuccess = true;
                potSuccess(isFreeBall);
            } else {
                updateTargetPotFailed();
                switchPlayer();
            }
            repositionCount = 0;
            continuousFoulAndMiss = 0;
            willLoseBecauseThisFoul = false;
//            thisCueFoul = false;
        }
//        System.out.println("Potted: " + pottedBalls + ", first: " + whiteFirstCollide + " score: " + score + ", foul: " + foul);
    }

    protected int getBallFoulScore(Ball ball) {
        if (ball.getValue() > 7) return 4;
        else return Math.max(4, ball.getValue());
    }

    protected void noHopeMaximum() {
        isOnMaximum = false;
        if (goldenBall != null && !goldenBall.isPotted()) {
            goldenBall.pot();
        }
    }

    @Override
    public void switchPlayer() {
        super.switchPlayer();

        int nReds = numRedBalls();
        int remReds = remainingRedCount();
        if (nReds != remReds) noHopeMaximum();
    }

    protected void addFoulScore() {
        int score = thisCueFoul.getFoulScore();
        getAnotherPlayer().addScore(score);
        AchManager.getInstance().cumulateAchievement(Achievement.SNOOKER_CUMULATE_FOUL_GAIN,
                score,
                getAnotherPlayer().getInGamePlayer());
    }

    @Override
    protected void end() {
        super.end();

        if (!gameValues.isTraining()) {
            checkScoreSumAchievement();
        }
    }

    private void checkScoreSumAchievement() {
        AchManager achManager = AchManager.getInstance();
        InGamePlayer human = getP1().isHuman() ? getP1() : getP2();
        if (getScoreSum(false) > 147) {
            if (getScoreDiffAbs() <= 50) {
                achManager.addAchievement(Achievement.SUM_OVER_147, human);
            }
        } else if (getScoreSum(true) < 75) {
            achManager.addAchievement(Achievement.SUM_BELOW, human);
        } else if (getScoreSum(true) < 50) {
            achManager.addAchievement(Achievement.SUM_BELOW_2, human);
        }

        boolean humanWin = getWiningPlayer().getInGamePlayer().isHuman();
        if (humanWin) {
            int maxAhead = getMaxScoreDiff(human.getPlayerNumber());
            if (maxAhead <= -65) {
                achManager.addAchievement(Achievement.COME_BACK_BEHIND_65, human);
            }
            if ((human.getPlayerNumber() == 1 && isP2EverOver()) ||
                    (human.getPlayerNumber() == 2 && isP1EverOver())) {
                // 从被超分逆转胜利
                achManager.addAchievement(Achievement.COME_BACK_BEHIND_OVER_SCORE, human);
            }
            if (getRemainingScore(false) == 0) {
                // 是打完了，不是有人认输了
                int scoreDiff = getScoreDiffAbs();
                if (scoreDiff <= 13) {
                    // 1. human必定领先，因为是human赢了
                    // 2. 在打进黑球之前，如领先6分及以下，则黑球为决定胜负的球
                    //    如果正好是领先7分，那对手打进就是延分，不算决定胜负的球
                    achManager.addAchievement(Achievement.LAST_GASP_WIN, human);
                }
            }
        } else {
            if (getRemainingScore(false) == 0) {
                // 是打完了，不是有人认输了
                int scoreDiff = getScoreDiffAbs();
                if (scoreDiff <= 13) {
                    // 同上
                    achManager.addAchievement(Achievement.LAST_GASP_LOST, human);
                }
            }
        }
    }

    private int getScoreSum(boolean includeRemain) {
        return player1.getScore() + player2.getScore() +
                (includeRemain ? getRemainingScore(false) : 0);
    }

    public boolean isDoingFreeBall() {
        return doingFreeBall;
    }

    public void cancelFreeBall() {
        doingFreeBall = false;
    }

    @Override
    public boolean canReposition() {
        // 不需要再去检查有没有解了，因为无解的球不会判miss
        int minScore = Math.min(player1.getScore(), player2.getScore());
        int maxScore = Math.max(player1.getScore(), player2.getScore());
        boolean notOverScore = minScore + getRemainingScore(false) > maxScore;  // 超分或延分不能复位
        return notOverScore && super.canReposition();
    }

    public boolean aiConsiderReposition(Phy phy, PotAttempt lastPotAttempt) {
        SnookerAiCue sac = new SnookerAiCue(this, getCuingPlayer());
        return sac.considerReposition(phy, recordedPositions, lastPotAttempt, isDoingFreeBall());
    }

    public void tieTest() {
        for (Ball ball : redBalls) ball.pot();

        for (Ball ball : coloredBalls) {
            ball.pot();
        }
        pickupColorBall(getBallOfValue(7));

        player2.addScore(-player2.getScore() + 7);
        player1.addScore(-player1.getScore());
        currentPlayer = player1;
        currentTarget = 7;
    }

    @Override
    public void clearRedBallsTest() {
        for (int i = 0; i < 14; ++i) {
            redBalls[i].pot();
        }
    }

    @Override
    public void notReposition() {
        super.notReposition();

        repositionCount = 0;
    }

    @Override
    public void reposition(boolean isSimulate) {
        super.reposition(isSimulate);

        doingFreeBall = wasDoingFreeBall;
        if (!isSimulate) repositionCount++;
    }

    /**
     * 在复位之后再call这个。
     */
    public boolean isNoHitThreeWarning() {
//        willLoseBecauseThisFoul = isAnyFullBallVisible() && repositionCount == 2;
//        return willLoseBecauseThisFoul;
        return willLoseBecauseThisFoul();
    }

    /**
     * 在复位之后再call这个。
     */
    public boolean willLoseBecauseThisFoul() {
        willLoseBecauseThisFoul = loseBecauseFoulAndMiss(repositionCount, continuousFoulAndMiss + 1);
        return willLoseBecauseThisFoul;
    }

    public int getRepositionCount() {
        return repositionCount;
    }

    private boolean loseBecauseFoulAndMiss(int repCount, int contFMCount) {
        return isAnyFullBallVisible() && (repCount >= 2) && (contFMCount >= 3);
    }

    private boolean isSolvable(CuePlayParams aimingParam) {
        long st = System.currentTimeMillis();
        List<Ball> legals = getAllLegalBalls(currentTarget, isDoingSnookerFreeBll(), false);
        Set<Ball> legalSet = new HashSet<>(legals);

        // 从出杆的方向开始算，希望更早遇到能碰到的角度，相当于优化。但对真的解不到的球没有帮助。
        double aimingDeg = Math.toDegrees(Algebra.thetaOf(aimingParam.vx, aimingParam.vy));

        double degTick = 1.0;
        for (double t = 0.0; t < 180.0; t += degTick) {
            for (int i = -1; i <= 1; i += 2) {
                if (t == 0.0 && i == -1) continue;  // 0度不要检查两次
                // 左一下右一下
                double deg = aimingDeg + t * i;
                if (deg < 0) deg += 360.0;
                else if (deg >= 360) deg -= 360.0;

                double[] unitXy = Algebra.unitVectorOfAngle(Math.toRadians(deg));
                CueParams cueParams = CueParams.createBySelected(
                        50.0,
                        0.0,
                        0.0,
                        5.0,
                        this,
                        getCuingIgp(),
                        null
                );
                CuePlayParams cpp = CuePlayParams.makeIdealParams(
                        unitXy[0],
                        unitXy[1],
                        cueParams
                );
                WhitePrediction wp = predictWhite(cpp, entireGame.predictPhy, 10000.0,
                        false,
                        false,
                        false, 
                        true,
                        true, false);
                if (legalSet.contains(wp.getFirstCollide())) {
                    System.out.println("Solvable! Check foul and miss in " + (System.currentTimeMillis() - st) + " ms, " +
                            "solve angle: " + deg);
                    return true;
                }
            }
        }
        System.out.println("Unsolvable! Check foul and miss in " + (System.currentTimeMillis() - st) + " ms");
        return false;
    }

    /**
     * 是不是无意识救球，前提是已经犯规且没有击中目标球
     */
    private boolean isSolvable() {
        return isSolvable;
    }

    @Override
    public int getContinuousFoulAndMiss() {
        return continuousFoulAndMiss;
    }

    public int remainingRedCount() {
        int count = 0;
        for (Ball ball : redBalls) {
            if (!ball.isPotted()) count++;
        }
        return count;
    }

    private boolean hasRed() {
        for (Ball redBall : redBalls) if (!redBall.isPotted()) return true;
        return false;
    }

    public void pickupPottedBallsLast(int lastCueTarget) {
        if (lastCueTarget == RAW_COLORED_REP || lastCueTarget == 1) {
            for (int i = 5; i >= 0; i--) {
                pickupColorBall(coloredBalls[i]);
            }
        } else {
            for (int i = 5; i >= lastCueTarget - 1; i--) {
                pickupColorBall(coloredBalls[i]);
            }
        }
    }

    public void pickupPottedBalls(int updatedTarget) {
        if (updatedTarget == RAW_COLORED_REP || updatedTarget == 1) {
            for (int i = 5; i >= 0; i--) {
                pickupColorBall(coloredBalls[i]);
            }
        } else if (updatedTarget == 2) {
            // 下个目标是黄球
            for (int i = 5; i >= 0; i--) {
                pickupColorBall(coloredBalls[i]);
            }
        } else {
            for (int i = 5; i >= updatedTarget - 2; i--) {
                pickupColorBall(coloredBalls[i]);
            }
        }
    }

    private void pickupColorBall(Ball ball) {
        if (!ball.isPotted()) return;
        double[] placePoint = getTable().pointsRankHighToLow[7 - ball.getValue()];
        if (isOccupied(placePoint[0], placePoint[1])) {
            boolean placed = false;
            for (double[] otherPoint : getTable().pointsRankHighToLow) {
                if (!isOccupied(otherPoint[0], otherPoint[1])) {
                    ball.setX(otherPoint[0]);
                    ball.setY(otherPoint[1]);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                double x = placePoint[0] + MIN_GAP_DISTANCE;
                while (x < gameValues.table.rightX - gameValues.ball.ballRadius) {
                    if (!isOccupied(x, placePoint[1])) {
                        ball.setX(x);
                        ball.setY(placePoint[1]);
                        placed = true;
                        break;
                    }
                    x += MIN_GAP_DISTANCE;
                }
                if (!placed) {
                    System.out.println("Failed to place!" + ball);
                }
            }
        } else {
            ball.setX(placePoint[0]);
            ball.setY(placePoint[1]);
        }

        ball.pickup();
    }

    public int getMaxScoreDiff(int playerNum) {
        return playerNum == 1 ? maxScoreDiff : -maxScoreDiff;
    }

    public boolean isP1EverOver() {
        return p1EverOver;
    }

    public boolean isP2EverOver() {
        return p2EverOver;
    }
    
    public void letOtherPlay() {
        super.letOtherPlay();
        
        cancelFreeBall();  // 让杆了你还打自由球？
    }

    @Override
    protected void endMoveAndUpdate() {
        boolean isFreeBall = doingFreeBall;
        doingFreeBall = false;

        updateScoreAndTarget(newPotted, isFreeBall);
        int scoreDiff = player1.getScore() - player2.getScore();
        int rem = getRemainingScore(isFreeBall);
        if (Math.abs(scoreDiff) > Math.abs(maxScoreDiff)) {
            maxScoreDiff = scoreDiff;
        }
        if (scoreDiff > rem) {
            p1EverOver = true;
        }
        if (-scoreDiff > rem) {
            p2EverOver = true;
        }
        indicatedTarget = 0;
        targetManualIndicated = false;
        playingRepositionBall = false;
        if (!isEnded())
            pickupPottedBalls(currentTarget);
    }

    @Override
    public ScoreResult makeScoreResult(Player justCuedPlayer) {
        return new SnookerScoreResult(
                thinkTime,
                player1.getScore(),
                player2.getScore(),
                player1.getLastAddedScore(),
                player2.getLastAddedScore(),
                justCuedPlayer.getInGamePlayer().getPlayerNumber(),
                justCuedPlayer.getSinglePole()
        );
    }

    public int getPossibleBreak(int alreadySinglePole) {
        // todo: 这里有个bug
        // todo: bug成因未知，但大概是因为这个method在更新target之前就被call了
        int rem = getRemainingScore(false);
        return alreadySinglePole + rem;
    }

    @Override
    public SnookerPlayer getWiningPlayer() {
        if (player1.isWithdrawn()) return player2;
        else if (player2.isWithdrawn()) return player1;

        if (player1.getScore() > player2.getScore()) return player1;
        else if (player2.getScore() > player1.getScore()) return player2;
        else throw new RuntimeException("延分时不会结束");
    }

    protected boolean canPlaceWhiteInTable(double x, double y) {
        return x <= getTable().breakLineX() &&
                Algebra.distanceToPoint(
                        x,
                        y,
                        getTable().brownBallPos()[0],
                        getTable().brownBallPos()[1]
                ) <= getTable().breakArcRadius() &&
                !isOccupied(x, y);
    }

    public boolean wasLegalBall(Ball ball, int targetRep, boolean isSnookerFreeBall) {
        if (!ball.isWhite()) {
            if (targetRep == RAW_COLORED_REP) {
                return ball.getValue() > 1 && ball.getValue() <= 7;
            } else {
                if (isSnookerFreeBall) {
                    return true;
                } else return ball.getValue() == targetRep;
            }
        }
        return false;
    }

    @Override
    public boolean isLegalBall(Ball ball, int targetRep, boolean isSnookerFreeBall, boolean isInLineHandBall) {
        if (!ball.isPotted() && !ball.isWhite()) {
            if (targetRep == RAW_COLORED_REP) {
                return ball.getValue() > 1 && ball.getValue() <= 7;
            } else {
                if (isSnookerFreeBall) {
                    return true;
                } else return ball.getValue() == targetRep;
            }
        }
        return false;
    }

    @Override
    public boolean isDoingSnookerFreeBll() {
        return isDoingFreeBall();
    }

    @Override
    public double priceOfTarget(int targetRep, Ball ball, Player attackingPlayer,
                                Ball lastPotting) {
        SnookerPlayer snookerPlayer = (SnookerPlayer) attackingPlayer;
        if (targetRep == 1) {
            return 1.0;  // 目标球是红球，有可能是自由球
        } else if (targetRep == AbstractSnookerGame.RAW_COLORED_REP) {
            int scoreBehind;
            int remReds = remainingRedCount();
            int remMax;

            if (lastPotting == null) {
                scoreBehind = -getScoreDiff(snookerPlayer);
                remMax = remReds * 8 + 34;
            } else {
                scoreBehind = -getScoreDiff(snookerPlayer) - 1;
                remMax = remReds * 8 + 26;
            }

//            System.out.println(scoreBehind + ", rem: " + remMax);
            if (remMax == scoreBehind) {
                // 即使清完黑球也是延分
                if (ball.getValue() == 7) return 1.0;
                else return ball.getValue() / 50.0;
            } else if (remMax > scoreBehind && remMax - 7 < scoreBehind) {
                // 没被超分，但必须打高分彩球
                int minScoreReq = scoreBehind - remMax + 8;
                if (ball.getValue() >= minScoreReq) return ball.getValue() / 7.0;
                else return ball.getValue() / 50.0;
            }

            int curSinglePole = snookerPlayer.getSinglePoleScore();
            if (lastPotting != null) curSinglePole += 1;
//            System.out.println(curSinglePole + " " + remMax);
//            if (curSinglePole > 24 && curSinglePole + remMax == 147) {
            if (curSinglePole > 24 && isOnMaximum) {
                // 有机会冲击147
                if (ball.getValue() == 7) return 1.0;
                if (curSinglePole > -scoreBehind) {
                    // 已经超分
                    return ball.getValue() / 50.0;
                } else {
                    return ball.getValue() / 21.0;
                }
            }

            return ball.getValue() / 7.0;
        } else {  // 清彩球阶段
            return 1.0;
        }
    }

    @Override
    public GamePlayStage getGamePlayStage(@Nullable Ball predictedTargetBall, boolean printPlayStage) {
//        printPlayStage = true;
        if (isBreaking()) return GamePlayStage.BREAK;
        int targetValue = getCurrentTarget();
//        int singlePoleScore = getCuingPlayer().getSinglePoleScore();
        boolean isGolden = gameValues.hasSubRule(SubRule.SNOOKER_GOLDEN);
        if (isOnMaximum) {
            if (isGolden) {
                if (targetValue == 20) {
                    // 打进金球就是167
                    if (printPlayStage) System.out.println("This ball 167!");
                    return GamePlayStage.THIS_BALL_WIN;
                } else if (targetValue == 7) {
                    // 打进黑球再打进金球就是167
                    if (printPlayStage) System.out.println("Going 167!");
                    return GamePlayStage.NEXT_BALL_WIN;
                }
            } else {
                if (targetValue == 7) {
                    // 打进黑球就是147
                    if (printPlayStage) System.out.println("This ball 147!");
                    return GamePlayStage.THIS_BALL_WIN;
                } else if (targetValue == 6) {
                    // 打进粉球再打进黑球就是147
                    if (printPlayStage) System.out.println("Going 147!");
                    return GamePlayStage.NEXT_BALL_WIN;
                }
            }
        }


        int ahead = getScoreDiff(getCuingPlayer());
        int remaining = getRemainingScore(isDoingSnookerFreeBll());
        int aheadAfter;  // 打进后的领先
        int remainingAfter;  // 打进后的剩余
        int aheadAfter2;  // 打进再下一颗球后的领先
        int remainingAfter2;  // 打进再下一颗球后的剩余
        if (targetValue == 1) {
            // 打红球
            aheadAfter = ahead + 1;
            remainingAfter = remaining - 8;
            aheadAfter2 = aheadAfter + 7;
            remainingAfter2 = remainingAfter;
        } else if (targetValue == RAW_COLORED_REP) {
            if (predictedTargetBall != null && predictedTargetBall.getValue() != 1) {
                aheadAfter = ahead + predictedTargetBall.getValue();
            } else {
                aheadAfter = ahead + 7;
            }
            remainingAfter = remaining;  // 打进彩球不改变
            if (remainingRedCount() == 0) {
                // 打完现在的彩球后该打黄球了
                aheadAfter2 = aheadAfter + 2;
                remainingAfter2 = remainingAfter - 2;
            } else {
                // 打完打红球
                aheadAfter2 = aheadAfter + 1;
                remainingAfter2 = remainingAfter - 8;
            }
        } else {
            // 清彩球阶段
            aheadAfter = ahead + targetValue;
            remainingAfter = remaining - targetValue;
            aheadAfter2 = aheadAfter + targetValue + 1;
            remainingAfter2 = remainingAfter - targetValue - 1;
        }
        if (ahead < remaining && aheadAfter >= remainingAfter) {
            // 打进目标球超分或延分
            if (printPlayStage) System.out.println("This ball over score!");
            return GamePlayStage.THIS_BALL_WIN;
        }
        if (targetValue != 7 &&
                aheadAfter < remainingAfter &&
                aheadAfter2 >= remainingAfter2) {
            if (printPlayStage) System.out.println("Prepared to over score!");
            return GamePlayStage.NEXT_BALL_WIN;
        }
        if (ahead >= remaining && ahead - remaining <= 8) {
            if (printPlayStage) System.out.println("Close to win!");
            return GamePlayStage.ENHANCE_WIN;
        } else if (ahead > remaining && ahead - remaining < 15) {
            if (printPlayStage) System.out.println("Won, but opponent may stand again");
            return GamePlayStage.NORMAL;
        } else if (ahead > remaining) {
            // todo: 需检查
//            if (singlePoleScore + remaining >= 147) {
            if (isOnMaximum) {
                if (printPlayStage) System.out.println("Way on 147");
                return GamePlayStage.NORMAL;
            }

            if (printPlayStage) System.out.println("Overed score, blind chicken eight play");
            return GamePlayStage.NO_PRESSURE;
        }
        return GamePlayStage.NORMAL;
    }

    protected void initRedBalls() {
        double curX = firstRedX();
        double rowStartY = gameValues.table.midY;

        int nRows = numRedRows();

        int index = 0;
        for (int row = 0; row < nRows; ++row) {
            int nCols = row + 1;
            double y = rowStartY;
            for (int col = 0; col < nCols; ++col) {
                SnookerBall ball = new SnookerBall(1, new double[]{curX, y}, gameValues);
                redBalls[index++] = ball;

                if (col == 0) {
                    if (row >= nRows - 1) {
                        suggestedRegularBreakBalls.add(ball);
                        if (row == nRows - 1) {
                            cornerRedBallPoses[1] = new double[]{curX, y};
                        }
                    }
                } else if (col == nCols - 1) {
                    if (row >= nRows - 1) {
                        suggestedRegularBreakBalls.add(ball);
                        if (row == nRows - 1) {
                            cornerRedBallPoses[0] = new double[]{curX, y};
                        }
                    }
                }
                y += gameValues.ball.ballDiameter + redGapDt;
            }
            rowStartY -= gameValues.ball.ballRadius + redGapDt * 0.6;
            curX += redRowOccupyX;
        }
    }

    public double firstRedX() {
        return getTable().pinkBallPos()[0] + gameValues.ball.ballDiameter + Game.MIN_GAP_DISTANCE;  // 粉球与红球堆空隙
    }

    @Override
    public String getFoulReason() {
        return thisCueFoul.isFoul() ?
                String.format(strings.getString("foulReasonFormat"),
                        thisCueFoul.getAllReasons(), thisCueFoul.getFoulScore()) :
                null;
    }

    @Override
    protected boolean isBallPlacedInHeap(Ball ball) {
        return ((SnookerBall) ball).isRed();
    }

    public Set<Ball> getSuggestedRegularBreakBalls() {
        return suggestedRegularBreakBalls;
    }

    public double[] getCornerRedBallPosYellowSide() {
        return cornerRedBallPoses[0];
    }

    public double[] getCornerRedBallPosGreenSide() {
        return cornerRedBallPoses[1];
    }
}
