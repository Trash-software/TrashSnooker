package trashsoftware.trashSnooker.core.snooker;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.ai.AiCue;
import trashsoftware.trashSnooker.core.ai.SnookerAiCue;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;
import trashsoftware.trashSnooker.core.scoreResult.SnookerScoreResult;
import trashsoftware.trashSnooker.core.table.AbstractSnookerTable;
import trashsoftware.trashSnooker.fxml.GameView;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractSnookerGame extends Game<SnookerBall, SnookerPlayer> {

    public static final int RAW_COLORED_REP = 0;  // 代表任意彩球
    public static final int END_REP = 8;
    public final double redRowOccupyX;
    public final double redGapDt;
    private final SnookerBall yellowBall;
    private final SnookerBall greenBall;
    private final SnookerBall brownBall;
    private final SnookerBall blueBall;
    private final SnookerBall pinkBall;
    private final SnookerBall blackBall;
    private final SnookerBall[] redBalls = new SnookerBall[numRedBalls()];
    private final SnookerBall[] allBalls;
    private final SnookerBall[] coloredBalls = new SnookerBall[6];
    private boolean doingFreeBall = false;  // 正在击打自由球
    private boolean blackBattle = false;
    private int lastFoulPoints = 0;
    
    private int repositionCount;
    private boolean willLoseBecauseThisFoul;

    AbstractSnookerGame(GameView parent, EntireGame entireGame,
                        GameSettings gameSettings, GameValues gameValues,
                        int frameIndex) {
        super(parent, entireGame, gameSettings, gameValues, frameIndex);

        redRowOccupyX = gameValues.ballDiameter * Math.sin(Math.toRadians(60.0)) +
                Game.MIN_PLACE_DISTANCE * 0.8;
        redGapDt = Game.MIN_PLACE_DISTANCE;

        currentTarget = 1;

        yellowBall = new SnookerBall(2, getTable().yellowBallPos(), gameValues);
        greenBall = new SnookerBall(3, getTable().greenBallPos(), gameValues);
        brownBall = new SnookerBall(4, getTable().brownBallPos(), gameValues);
        blueBall = new SnookerBall(5, getTable().blueBallPos(), gameValues);
        pinkBall = new SnookerBall(6, getTable().pinkBallPos(), gameValues);
        blackBall = new SnookerBall(7, getTable().blackBallPos(), gameValues);

        initRedBalls();

        allBalls = new SnookerBall[redBalls.length + 7];
        System.arraycopy(redBalls, 0, allBalls, 0, redBalls.length);
        allBalls[redBalls.length] = yellowBall;
        allBalls[redBalls.length + 1] = greenBall;
        allBalls[redBalls.length + 2] = brownBall;
        allBalls[redBalls.length + 3] = blueBall;
        allBalls[redBalls.length + 4] = pinkBall;
        allBalls[redBalls.length + 5] = blackBall;
        allBalls[redBalls.length + 6] = cueBall;

        System.arraycopy(allBalls, redBalls.length, coloredBalls, 0, 6);
    }

    public static String ballValueToColorName(int ballValue) {
        switch (ballValue) {
            case 1:
                return "红球";
            case 2:
                return "黄球";
            case 3:
                return "绿球";
            case 4:
                return "咖啡球";
            case 5:
                return "蓝球";
            case 6:
                return "粉球";
            case 7:
                return "黑球";
            case 0:
                return "彩球";
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public abstract AbstractSnookerTable getTable();

    protected abstract int numRedBalls();

    @Override
    protected void initPlayers() {
        player1 = new SnookerPlayer(gameSettings.getPlayer1(), this);
        player2 = new SnookerPlayer(gameSettings.getPlayer2(), this);
    }

    @Override
    protected SnookerBall createWhiteBall() {
        return new SnookerBall(0, gameValues);
    }

    @Override
    public SnookerBall[] getAllBalls() {
        return allBalls;
    }

    public SnookerBall getBallOfValue(int score) {
        switch (score) {
            case 1:
                return redBalls[0];
            case 2:
                return yellowBall;
            case 3:
                return greenBall;
            case 4:
                return brownBall;
            case 5:
                return blueBall;
            case 6:
                return pinkBall;
            case 7:
                return blackBall;
            default:
                throw new RuntimeException("没有这种彩球。");
        }
    }

    @Override
    protected AiCue<?, ?> createAiCue(SnookerPlayer aiPlayer) {
        return new SnookerAiCue(this, aiPlayer);
    }

    public int getScoreDiff() {
        return Math.abs(player1.getScore() - player2.getScore());
    }

    public int getScoreDiff(SnookerPlayer player) {
        SnookerPlayer another = player == player1 ? player2 : player1;
        return player.getScore() - another.getScore();
    }

    /**
     * 返回当前的台面剩余，非负
     */
    public int getRemainingScore() {
        if (currentTarget == 1) {
            return remainingRedCount() * 8 + 27;
        } else if (currentTarget == RAW_COLORED_REP) {
            return remainingRedCount() * 8 + 34;
        } else {
            int scoreSum = 0;
            for (int sc = currentTarget; sc <= 7; sc++) {
                scoreSum += sc;
            }
            return scoreSum;
        }
    }

    public boolean hasFreeBall() {
        if (lastCueFoul) {
            // 当从白球处无法看到任何一颗目标球的最薄边时
            List<Ball> currentTarBalls = getAllLegalBalls(currentTarget, false);
            int canSeeBallCount = countSeeAbleTargetBalls(cueBall.getX(), cueBall.getY(),
                    currentTarBalls, 3).seeAbleTargets;
            System.out.println("Target: " + currentTarget + ", Free ball check: " +
                    canSeeBallCount + ", n targets: " + currentTarBalls.size());
            return canSeeBallCount == 0;

//            // 使用预测击球线的方法：如瞄准最薄边时，预测线显示打到的就是这颗球（不会碰到其他球），则没有自由球。
//            double simulateBallDiameter = gameValues.ballDiameter - Values.PREDICTION_INTERVAL;
//            for (Ball ball : currentTarBalls) {
//                // 两球连线、预测的最薄击球点构成两个直角三角形，斜边为连线，其中一个直角边为球直的径（理想状况下）
//                double xDiff = ball.getX() - cueBall.getX();
//                double yDiff = ball.getY() - cueBall.getY();
//                double[] vec = new double[]{xDiff, yDiff};
//                double[] unitVec = Algebra.unitVector(vec);
//                double dt = Math.hypot(xDiff, yDiff);  // 两球球心距离
//                double theta = Math.asin(simulateBallDiameter / dt);  // 连线与预测线的夹角
//                double alpha = Algebra.thetaOf(unitVec);  // 两球连线与X轴的夹角
//
//                double leftAng = Algebra.normalizeAngle(alpha + theta);
//                double rightAng = Algebra.normalizeAngle(alpha - theta);
//
//                double[] leftUnitVec = Algebra.unitVectorOfAngle(leftAng);
//                double[] rightUnitVec = Algebra.unitVectorOfAngle(rightAng);
//
//                PredictedPos leftPP = getPredictedHitBall(cueBall.getX(), cueBall.getY(),
//                        leftUnitVec[0], leftUnitVec[1]);
//                PredictedPos rightPP = getPredictedHitBall(cueBall.getX(), cueBall.getY(),
//                        rightUnitVec[0], rightUnitVec[1]);
//
//                if ((leftPP == null || leftPP.getTargetBall().getValue() == ball.getValue()) &&
//                        (rightPP == null || rightPP.getTargetBall().getValue() == ball.getValue()))
//                    // 对于红球而言，能看到任意一颗红球的左侧与任意（可为另一颗）的右侧，则没有自由球
//                    return false;  // 两侧都看得到或者看得穿，没有自由球
//            }
//            return true;
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
        else if (currentTarget == RAW_COLORED_REP) return 2;  // 最后一颗红球附带的彩球进攻失败
        else return currentTarget;  // 其他情况目标球不变
    }

    protected void updateTargetPotSuccess(boolean isFreeBall) {
        int nextTarget = getTargetAfterPotSuccess(null, isFreeBall);
        if (nextTarget == END_REP) {
            if (player1.getScore() != player2.getScore()) end();
            else {
                // 延分，争黑球
                blackBattle = true;
                cueBall.pot();
                currentTarget = 7;
                pickupColorBall(blackBall);
                System.out.println("Black battle!");
                ballInHand = true;
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

        if (currentTarget == target.getValue()) {  // 必须选择非当前真正目标球的球作为自由球，todo: 规则存疑
            return new int[]{0, getDefaultFoulValue()};
        }

        if (pottedBalls.size() == 0) {
            return new int[2];
        } else if (pottedBalls.size() == 1) {
            for (Ball onlyBall : pottedBalls) {
                if (onlyBall == target) return new int[]{currentTarget, 0};
            }
            return new int[2];
        } else {
            int foul = getDefaultFoulValue();
            for (Ball ball : pottedBalls) {
                if (ball != target) {  // 第一颗碰到的球视为目标球
                    if (ball.getValue() > foul) foul = ball.getValue();
                }
            }
            return new int[]{0, foul};
        }
    }

    private int getMaxFoul(Set<SnookerBall> pottedBalls) {
        int foul = 0;
        for (SnookerBall ball : pottedBalls) {
            if (ball.isColored() && ball.getValue() > foul) foul = ball.getValue();
            else foul = getDefaultFoulValue();
        }
        return foul;
    }

    public int getDefaultFoulValue() {
        if (currentTarget == RAW_COLORED_REP || currentTarget < 4) return 4;
        else return currentTarget;
    }

    protected void updateScoreAndTarget(Set<SnookerBall> pottedBalls, boolean isFreeBall) {
        int score = 0;
        int foul = 0;
        if (whiteFirstCollide == null) {
            foul = getDefaultFoulValue();  // 没打到球，除了白球也不可能有球进，白球进不进也无所谓，分都一样
            foulReason = "空杆";
            if (cueBall.isPotted()) ballInHand = true;
        } else if (cueBall.isPotted()) {
            foul = Math.max(getDefaultFoulValue(), getMaxFoul(pottedBalls));
            foulReason = "白球落袋";
            ballInHand = true;
        } else if (isFreeBall) {
            int[] scoreFoul = scoreFoulOfFreeBall(pottedBalls);
            score = scoreFoul[0];
            foul = scoreFoul[1];
            if (foul > 0) {
                foulReason = "击打了错误的自由球";
            }
        } else if (currentTarget == 1) {
            if (whiteFirstCollide.isRed()) {
                for (SnookerBall ball : pottedBalls) {
                    if (ball.isRed()) {
                        score++;  // 进了颗红球
                    } else {
                        foul = getMaxFoul(pottedBalls);  // 进了颗彩球
                        foulReason = "目标球为红球，但有彩球落袋";
                    }
                }
            } else {  // 该打红球时打了彩球
                foul = Math.max(4, whiteFirstCollide.getValue());
                foulReason = "目标球为红球，但击打了彩球";
            }
        } else {
            if (whiteFirstCollide.getValue() == 1) {  // 该打彩球时打了红球
                foul = Math.max(4, getMaxFoul(pottedBalls));
                foulReason = "目标球为" + ballValueToColorName(currentTarget) + "，但击打了红球";
            } else {
                if (currentTarget != 0 && whiteFirstCollide.getValue() != currentTarget) {  // 打了非目标球的彩球
                    foul = Math.max(4, Math.max(whiteFirstCollide.getValue(), currentTarget));
                    foulReason = "目标球为" + ballValueToColorName(currentTarget) + "，但击打了其他球";
                }
                if (pottedBalls.size() == 1) {
                    if (currentTarget == RAW_COLORED_REP) {  // 任意彩球
                        for (Ball onlyBall : pottedBalls) {
                            if (onlyBall == whiteFirstCollide) score = onlyBall.getValue();
                            else {
                                foul = Math.max(4, onlyBall.getValue());
                                foulReason = "目标球为彩球，但击打了红球";
                            }
                        }
                    } else {  // 非任意彩球
                        for (Ball onlyBall : pottedBalls) {
                            if (onlyBall.getValue() == currentTarget) {
                                score = currentTarget;
                            } else {
                                foul = Math.max(foul, getMaxFoul(pottedBalls));
                                foulReason = "目标球为" + ballValueToColorName(currentTarget) +
                                        "，但击打了" + ballValueToColorName(onlyBall.getValue());
                            }
                        }
                    }
                } else if (!pottedBalls.isEmpty()) {
                    foul = getMaxFoul(pottedBalls);
                    foulReason = "击打彩球时有非目标球落袋";
                }
            }
        }
        lastFoulPoints = foul;
        if (foul > 0) {
            if (willLoseBecauseThisFoul) {
                // 三次瞎打判负
                getAnotherPlayer().addScore(foul);
                getCuingPlayer().withdraw();
                end();
                return;
            }
            
            if (blackBattle) {
                // 抢黑时犯规就直接判负
                getAnotherPlayer().addScore(foul);
                end();
                return;
            }

            getAnotherPlayer().addScore(foul);
            updateTargetPotFailed();
            switchPlayer();
            lastCueFoul = true;
            if (!cueBall.isPotted() && hasFreeBall()) {
                doingFreeBall = true;
                System.out.println("Free ball!");
            }
        } else {
            if (score > 0) {
                if (isFreeBall) {
                    if (pottedBalls.size() != 1) throw new RuntimeException("为什么进了这么多自由球？？？");
                    currentPlayer.potFreeBall(score);
                } else currentPlayer.correctPotBalls(pottedBalls);
                potSuccess(isFreeBall);
            } else {
                updateTargetPotFailed();
                switchPlayer();
            }
            repositionCount = 0;
            willLoseBecauseThisFoul = false;
            lastCueFoul = false;
        }
//        System.out.println("Potted: " + pottedBalls + ", first: " + whiteFirstCollide + " score: " + score + ", foul: " + foul);
    }

    public boolean isDoingFreeBall() {
        return doingFreeBall;
    }

    public boolean canReposition() {
        if (lastCueFoul) {
            int minScore = Math.min(player1.getScore(), player2.getScore());
            int maxScore = Math.max(player1.getScore(), player2.getScore());
            return minScore + getRemainingScore() > maxScore;  // 超分或延分不能复位
        } else {
            return false;
        }
    }

    public boolean aiConsiderReposition(Phy phy) {
        SnookerAiCue sac = new SnookerAiCue(this, getCuingPlayer());
        return sac.considerReposition(phy, recordedPositions);
    }

    public void tieTest() {
        for (Ball ball : redBalls) ball.pot();
        yellowBall.pot();
        greenBall.pot();
        brownBall.pot();
        blueBall.pot();
        pinkBall.pot();
        blackBall.pickup();
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
    
    public void notReposition() {
        repositionCount = 0;
        willLoseBecauseThisFoul = false;
    }

    public void reposition() {
        System.out.println("Reposition!");
        lastCueFoul = false;
        ballInHand = false;
        doingFreeBall = false;
        for (Map.Entry<SnookerBall, double[]> entry : recordedPositions.entrySet()) {
            SnookerBall ball = entry.getKey();
            ball.setX(entry.getValue()[0]);
            ball.setY(entry.getValue()[1]);
            if (ball.isPotted()) ball.pickup();
        }
        switchPlayer();
        currentTarget = recordedTarget;
        repositionCount++;
    }

    /**
     * 在复位之后再call这个。
     */
    public boolean isNoHitThreeWarning() {
        willLoseBecauseThisFoul = isAnyFullBallVisible() && repositionCount == 2;
        return willLoseBecauseThisFoul;
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

    public void pickupPottedBalls(int lastCueTarget) {
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
                while (x < gameValues.rightX - gameValues.ballRadius) {
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

    @Override
    protected void endMoveAndUpdate() {
        boolean isFreeBall = doingFreeBall;
        doingFreeBall = false;

        updateScoreAndTarget(newPotted, isFreeBall);
        if (!isEnded())
            pickupPottedBalls(recordedTarget);
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

    @Override
    public boolean isLegalBall(Ball ball, int targetRep, boolean isSnookerFreeBall) {
        if (!ball.isPotted() && !ball.isWhite()) {
            if (targetRep == RAW_COLORED_REP) {
                return ball.isColored();
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
        if (targetRep == 1) {
            return 1.0;  // 目标球是红球，有可能是自由球
        } else if (targetRep == AbstractSnookerGame.RAW_COLORED_REP) {
            int scoreBehind;
            int remReds = remainingRedCount();
            int remMax;

            if (lastPotting == null) {
                scoreBehind = -getScoreDiff((SnookerPlayer) attackingPlayer);
                remMax = remReds * 8 + 34;
            } else {
                scoreBehind = -getScoreDiff((SnookerPlayer) attackingPlayer) - 1;
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

            int curSinglePole = attackingPlayer.getSinglePoleScore();
            if (lastPotting != null) curSinglePole += 1;
//            System.out.println(curSinglePole + " " + remMax);
            if (curSinglePole > 24 && curSinglePole + remMax == 147) {
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
    public GamePlayStage getGamePlayStage(Ball predictedTargetBall, boolean printPlayStage) {
        if (isBreaking()) return GamePlayStage.BREAK;
        int targetValue = getCurrentTarget();
        int singlePoleScore = getCuingPlayer().getSinglePoleScore();
        if (singlePoleScore >= 140 && targetValue == 7) {
            // 打进黑球就是147
            if (printPlayStage) System.out.println("打进就是147！");
            return GamePlayStage.THIS_BALL_WIN;
        }
        if (singlePoleScore >= 134 && targetValue == 6) {
            // 打进粉球再打进黑球就是147
            if (printPlayStage) System.out.println("冲击147！");
            return GamePlayStage.NEXT_BALL_WIN;
        }

        int ahead = getScoreDiff(getCuingPlayer());
        int remaining = getRemainingScore();
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
            if (printPlayStage) System.out.println("打进超分！");
            return GamePlayStage.THIS_BALL_WIN;
        }
        if (targetValue != 7 &&
                aheadAfter < remainingAfter &&
                aheadAfter2 >= remainingAfter2) {
            if (printPlayStage) System.out.println("准备超分！");
            return GamePlayStage.NEXT_BALL_WIN;
        }
        if (ahead >= remaining && ahead - remaining <= 8) {
            if (printPlayStage) System.out.println("接近锁定胜局！");
            return GamePlayStage.ENHANCE_WIN;
        } else if (ahead > remaining) {
            // todo: 需检查
            if (singlePoleScore + remaining >= 147) {
                if (printPlayStage) System.out.println("147路上");
                return GamePlayStage.NORMAL;
            }

            if (printPlayStage) System.out.println("超分了，瞎JB打都行");
            return GamePlayStage.NO_PRESSURE;
        }
        return GamePlayStage.NORMAL;
    }

    private void initRedBalls() {
        double curX = getTable().firstRedX();
        double rowStartY = gameValues.midY;

        int index = 0;
        for (int row = 0; row < 5; ++row) {
            double y = rowStartY;
            for (int col = 0; col < row + 1; ++col) {
                redBalls[index++] = new SnookerBall(1, new double[]{curX, y}, gameValues);
                y += gameValues.ballDiameter + redGapDt;
            }
            rowStartY -= gameValues.ballRadius + redGapDt * 0.6;
            curX += redRowOccupyX;
            if (index >= numRedBalls()) break;
        }
    }

    @Override
    public String getFoulReason() {
        return foulReason == null ? null : (foulReason + "，罚" + lastFoulPoints + "分");
    }
}
