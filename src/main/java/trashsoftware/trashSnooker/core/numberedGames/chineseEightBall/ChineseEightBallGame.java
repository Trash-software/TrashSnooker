package trashsoftware.trashSnooker.core.numberedGames.chineseEightBall;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.ai.AiCue;
import trashsoftware.trashSnooker.core.ai.ChineseEightAiCue;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;
import trashsoftware.trashSnooker.core.career.achievement.CareerAchManager;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallGame;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.scoreResult.ChineseEightScoreResult;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;
import trashsoftware.trashSnooker.core.table.ChineseEightTable;
import trashsoftware.trashSnooker.util.Util;

import java.util.*;
import java.util.stream.Collectors;

public class ChineseEightBallGame extends NumberedBallGame<ChineseEightBallPlayer>
        implements NeedBigBreak {

    public static final int NOT_SELECTED_REP = 0;
    public static final int FULL_BALL_REP = 16;
    public static final int HALF_BALL_REP = 17;
    private static final int[] FULL_BALL_SLOTS = {0, 2, 3, 7, 9, 10, 12};
    private static final int[] HALF_BALL_SLOTS = {1, 5, 6, 7, 11, 13, 14};
    protected double eightBallPosX;
    protected ChineseEightScoreResult curResult;
    private boolean wasBreakLoseChance;

    public ChineseEightBallGame(EntireGame entireGame, GameSettings gameSettings, GameValues gameValues, int frameIndex) {
        super(entireGame, gameSettings, gameValues, new ChineseEightTable(gameValues.table), frameIndex);

        initBalls();
    }

    @Override
    public int getNumBallsTotal() {
        return 16;
    }

    @Override
    public GameRule getGameType() {
        return GameRule.CHINESE_EIGHT;
    }

    protected PoolBall getEightBall() {
        return allBalls[8];
    }

    private void initBalls() {
        allBalls = new PoolBall[16];

        List<PoolBall> fullBalls = new ArrayList<>();
        List<PoolBall> halfBalls = new ArrayList<>();
        for (int i = 0; i < 7; ++i) {
            fullBalls.add(new PoolBall(i + 1, false, gameValues));
        }
        for (int i = 0; i < 7; ++i) {
            halfBalls.add(new PoolBall(i + 9, false, gameValues));
        }

        allBalls[0] = cueBall;
        for (int i = 0; i < 7; ++i) {
            allBalls[i + 1] = fullBalls.get(i);
        }
        PoolBall eightBall = new PoolBall(8, false, gameValues);
        allBalls[8] = eightBall;
        for (int i = 0; i < 7; ++i) {
            allBalls[i + 9] = halfBalls.get(i);
        }

        Collections.shuffle(fullBalls);
        Collections.shuffle(halfBalls);

        double curX = getTable().firstBallPlacementX();
        double rowStartY = gameValues.table.midY;
        double rowOccupyX = gameValues.ball.ballDiameter * Math.sin(Math.toRadians(60.0))
                + Game.MIN_PLACE_DISTANCE * 0.6;
        int ballCountInRow = 1;
        int index = 0;
        for (int row = 0; row < 5; ++row) {
            double y = rowStartY;
            for (int col = 0; col < ballCountInRow; ++col) {
                if (index == 4) {
                    eightBallPosX = curX;
                    eightBall.setX(curX);
                    eightBall.setY(y);
                } else if (Util.arrayContains(FULL_BALL_SLOTS, index)) {
                    PoolBall ball = fullBalls.remove(fullBalls.size() - 1);
                    ball.setX(curX);
                    ball.setY(y);
                } else {
                    PoolBall ball = halfBalls.remove(halfBalls.size() - 1);
                    ball.setX(curX);
                    ball.setY(y);
                }
                index++;
                y += gameValues.ball.ballDiameter + Game.MIN_PLACE_DISTANCE;
            }
            ballCountInRow++;
            rowStartY -= gameValues.ball.ballRadius + Game.MIN_PLACE_DISTANCE;
            curX += rowOccupyX;
        }
    }

    @Override
    public ScoreResult makeScoreResult(Player justCuedPlayer) {
        return curResult;
    }

    public Movement cue(CuePlayParams params, Phy phy) {
        createScoreResult();
        return super.cue(params, phy);
    }

    private void createScoreResult() {
        curResult = new ChineseEightScoreResult(
                thinkTime,
                getCuingPlayer().getInGamePlayer().getPlayerNumber(),
                ChineseEightTable.filterRemainingTargetOfPlayer(player1.getBallRange(), this),
                ChineseEightTable.filterRemainingTargetOfPlayer(player2.getBallRange(), this));
    }

    @Override
    protected AiCue<?, ?> createAiCue(ChineseEightBallPlayer aiPlayer) {
        return new ChineseEightAiCue(this, aiPlayer);
    }

    @Override
    public boolean isLegalBall(Ball ball, int targetRep, boolean isSnookerFreeBall, boolean isInLineHandBall) {
        if (!ball.isPotted() && !ball.isWhite()) {
            if (isInLineHandBall) {
                if (ball.getX() <= getTable().breakLineX()) {
                    return false;
                }
            }

            if (targetRep == NOT_SELECTED_REP) {
                return ball.getValue() != 8;
            } else if (targetRep == FULL_BALL_REP) {
                return ball.getValue() < 8;
            } else if (targetRep == HALF_BALL_REP) {
                return ball.getValue() > 8;
            } else if (targetRep == 8) {
                return ball.getValue() == 8;
            }
        }
        return false;
    }

    @Override
    public int getTargetAfterPotSuccess(Ball pottingBall, boolean isSnookerFreeBall) {
        ChineseEightBallPlayer player = getCuingPlayer();
        if (player.getBallRange() == NOT_SELECTED_REP) {
            if (isFullBall(pottingBall)) return FULL_BALL_REP;
            else if (isHalfBall(pottingBall)) return HALF_BALL_REP;
            else return 0;
        }
        if (player.getBallRange() == FULL_BALL_REP || player.getBallRange() == HALF_BALL_REP) {
            int backLet = player.getLettedBalls().get(LetBall.BACK);

            if (getRemRangedBallOnTable(player.getBallRange()) > backLet + 1)
                return player.getBallRange();
            else if (pottingBall.getValue() == 8) return END_REP;
            else return 8;
        }
        throw new RuntimeException("不可能");
    }

    @Override
    public int getTargetAfterPotFailed() {
        return getTargetOfPlayer(getAnotherPlayer());
    }

    @Override
    public double priceOfTarget(int targetRep, Ball ball, Player attackingPlayer,
                                Ball lastPotting) {
        return 1.0;
    }

    /**
     * 返回给定的球员还剩几颗球没打，含黑八
     */
    public int getRemainingBallsOfPlayer(Player player) {
        int target = getTargetOfPlayer(player);
        if (target == 8) return 1;
        else if (target == FULL_BALL_REP) {
            int rem = 1;
            for (int i = 1; i <= 8; i++) {
                Ball ball = getAllBalls()[i];
                if (!ball.isPotted()) rem++;
            }
            return rem;
        } else if (target == HALF_BALL_REP) {
            int rem = 1;
            for (int i = 8; i <= 15; i++) {
                Ball ball = getAllBalls()[i];
                if (!ball.isPotted()) rem++;
            }
            return rem;
        } else {
            int rem = 1;
            for (Ball ball : getAllBalls()) {
                if (!ball.isWhite() && !ball.isPotted()) rem++;
            }
            return rem;
        }
    }

    @Override
    public ChineseEightTable getTable() {
        return (ChineseEightTable) super.getTable();
    }

    @Override
    protected void initPlayers() {
        Map<LetBall, Integer> p1Letted = new HashMap<>();
        Map<LetBall, Integer> p2Letted = new HashMap<>();

        InGamePlayer p1 = gameSettings.getPlayer1();
        InGamePlayer p2 = gameSettings.getPlayer2();

        if (p1.getPlayerPerson().getSex() != p2.getPlayerPerson().getSex()) {
            if (p1.getPlayerPerson().getSex() == PlayerPerson.Sex.F) {
                p1Letted.put(LetBall.BACK, 1);
            } else {
                p2Letted.put(LetBall.BACK, 1);
            }
        }

        System.out.println("P1 letted balls: " + p1Letted);
        System.out.println("P2 letted balls: " + p2Letted);

        player1 = new ChineseEightBallPlayer(p1, p1Letted);
        player2 = new ChineseEightBallPlayer(p2, p2Letted);
    }

    @Override
    protected boolean canPlaceWhiteInTable(double x, double y) {
        if (isBreaking() || isJustAfterBreak()) {
            return x <= getTable().breakLineX() && !isOccupied(x, y);
        } else {
            return !isOccupied(x, y);
        }
    }

    private boolean isTargetSelected() {
        return player1.getBallRange() != 0;
    }

    @Override
    protected void endMoveAndUpdate() {
        backLet(player1);  // 让前和让中未实装
        backLet(player2);
        updateScore(newPotted);
    }

    @Override
    public Player getWiningPlayer() {
        return winingPlayer;
    }

    @Override
    public GamePlayStage getGamePlayStage(Ball predictedTargetBall, boolean printPlayStage) {
        if (isBreaking()) return GamePlayStage.BREAK;
        int rems = getRemainingBallsOfPlayer(getCuingPlayer());
        if (rems == 1) {
            if (printPlayStage) System.out.println("This ball win!");
            return GamePlayStage.THIS_BALL_WIN;
        } else if (rems == 2) {
            if (printPlayStage) System.out.println("Next ball win!");
            return GamePlayStage.NEXT_BALL_WIN;
        }
        return GamePlayStage.NORMAL;
    }

    protected boolean isFullBall(Ball ball) {
        return ball.getValue() >= 1 && ball.getValue() <= 7;
    }

    protected boolean isHalfBall(Ball ball) {
        return ball.getValue() >= 9 && ball.getValue() <= 15;
    }

    protected int getRemRangedBallOnTable(int ballRange) {
        if (ballRange == FULL_BALL_REP) return getRemFullBallOnTable();
        else if (ballRange == HALF_BALL_REP) return getRemHalfBallOnTable();
        else return 1;
    }

    private int getRemFullBallOnTable() {
        int count = 0;
        for (Ball ball : getAllBalls()) {
            if (!ball.isPotted() && isFullBall(ball)) count++;
        }
        return count;
    }

    private int getRemHalfBallOnTable() {
        int count = 0;
        for (Ball ball : getAllBalls()) {
            if (!ball.isPotted() && isHalfBall(ball)) count++;
        }
        return count;
    }

    private boolean hasFullBallOnTable() {
        return getRemFullBallOnTable() > 0;
    }

    private boolean hasHalfBallOnTable() {
        return getRemHalfBallOnTable() > 0;
    }

    protected boolean allFullBalls(Set<PoolBall> balls) {
        for (PoolBall ball : balls) {
            if (isHalfBall(ball)) return false;
        }
        return true;
    }

    protected boolean allHalfBalls(Set<PoolBall> balls) {
        for (PoolBall ball : balls) {
            if (isFullBall(ball)) return false;
        }
        return true;
    }

    protected Collection<PoolBall> fullBallsOf(Set<PoolBall> balls) {
        return balls.stream().filter(this::isFullBall).collect(Collectors.toSet());
    }

    protected Collection<PoolBall> halfBallsOf(Set<PoolBall> balls) {
        return balls.stream().filter(this::isHalfBall).collect(Collectors.toSet());
    }
    
    protected int countFullBalls(Set<PoolBall> balls){
        int count = 0;
        for (PoolBall ball : balls) {
            if (isFullBall(ball)) count++;
        }
        return count;
    }

    protected int countHalfBalls(Set<PoolBall> balls){
        int count = 0;
        for (PoolBall ball : balls) {
            if (isHalfBall(ball)) count++;
        }
        return count;
    }

    protected boolean hasFullBalls(Set<PoolBall> balls) {
        return countFullBalls(balls) > 0;
    }

    protected boolean hasHalfBalls(Set<PoolBall> balls) {
        return countHalfBalls(balls) > 0;
    }

    protected int getTargetOfPlayer(Player playerX) {
        ChineseEightBallPlayer player = (ChineseEightBallPlayer) playerX;
        if (player.getBallRange() == NOT_SELECTED_REP) return NOT_SELECTED_REP;
        if (player.getBallRange() == FULL_BALL_REP) {
            if (hasFullBallOnTable()) return FULL_BALL_REP;
            else return 8;
        }
        if (player.getBallRange() == HALF_BALL_REP) {
            if (hasHalfBallOnTable()) return HALF_BALL_REP;
            else return 8;
        }
        throw new RuntimeException("不可能");
    }

    @Override
    public boolean isInLineHandBall() {
//        System.out.println(isJustAfterBreak() + " " + lastCueFoul);
        return isJustAfterBreak() && lastCueFoul.isFoul();
    }

    @Override
    public boolean isInLineHandBallForAi() {
        return isJustAfterBreak() && thisCueFoul.isFoul();  // 主要区别就是，ai计算的时候是在lastCueFoul=thisCueFoul之前
    }

    private void backLet(ChineseEightBallPlayer player) {
        if (player.getBallRange() == NOT_SELECTED_REP) return;

        int backLet = player.getLettedBalls().get(LetBall.BACK);
        int remBalls = getRemRangedBallOnTable(player.getBallRange());

        if (backLet >= remBalls) {
            if (player.getBallRange() == FULL_BALL_REP) {
                for (Ball ball : getAllBalls()) {
                    if (!ball.isPotted() && isFullBall(ball)) {
                        ball.pot();
                    }
                }
            } else if (player.getBallRange() == HALF_BALL_REP) {
                for (Ball ball : getAllBalls()) {
                    if (!ball.isPotted() && isHalfBall(ball)) {
                        ball.pot();
                    }
                }
            }
        }
    }

    private void updateScore(Set<PoolBall> pottedBalls) {
        boolean baseFoul = checkStandardFouls(() -> 1);

        if (baseFoul && getEightBall().isNotOnTable()) {  // 白球黑八一起进
            if (isBreaking) {
                // 开球犯规但进黑八不算输
                pickupCriticalBall(getEightBall());
                cueBall.pot();
                ballInHand = true;
                switchPlayer();
                return;
            }
            
            AchManager.getInstance().addAchievement(Achievement.SUICIDE, getCuingIgp());
            winingPlayer = getAnotherPlayer();
            end();
            return;
        }

        if (!baseFoul) {
            if (isInLineHandBall()) {
                System.out.println("In line hand ball");
                // 开球后直接造成的自由球
                double[] origPos = recordedPositions.get((PoolBall) whiteFirstCollide);
                if (origPos[0] <= getTable().breakLineX()) {
                    // 可以压线
                    System.out.println(Arrays.toString(origPos));
                    thisCueFoul.addFoul(strings.getString("breakFreeMustOut"));
                }
            }

            if (currentTarget == FULL_BALL_REP) {
                if (whiteFirstCollide.getValue() == 8) {
                    thisCueFoul.addFoul(strings.getString("targetFullHitBlack"), true);
                    AchManager.getInstance().addAchievement(Achievement.BLIND_SHOT, getCuingIgp());
                } else if (!isFullBall(whiteFirstCollide)) {
                    thisCueFoul.addFoul(strings.getString("targetFullHitHalf"), true);
                    AchManager.getInstance().addAchievement(Achievement.BLIND_SHOT, getCuingIgp());
                }
            } else if (currentTarget == HALF_BALL_REP) {
                if (whiteFirstCollide.getValue() == 8) {
                    thisCueFoul.addFoul(strings.getString("targetHalfHitBlack"), true);
                    AchManager.getInstance().addAchievement(Achievement.BLIND_SHOT, getCuingIgp());
                } else if (!isHalfBall(whiteFirstCollide)) {
                    thisCueFoul.addFoul(strings.getString("targetHalfHitFull"), true);
                    AchManager.getInstance().addAchievement(Achievement.BLIND_SHOT, getCuingIgp());
                }
            } else if (currentTarget == 8) {
                if (whiteFirstCollide.getValue() != 8) {
                    thisCueFoul.addFoul(strings.getString("targetBlackHitOther"), true);
                    AchManager.getInstance().addAchievement(Achievement.BLIND_SHOT, getCuingIgp());
                }
            }
        }

        if (thisCueFoul.isFoul() && !getEightBall().isNotOnTable()) {
            cueBall.pot();
            ballInHand = true;
            switchPlayer();
            currentTarget = getTargetOfPlayer(currentPlayer);  // 在switchPlayer之后
            System.out.println(thisCueFoul.getAllReasons());
            return;
        }

        if (isBreaking) {
            updateBreakStats(newPotted);
            int playerNum = getPlayerNum(currentPlayer);
            if (isBreakLoseChance(currentPlayer.getPlayerPerson())) {
                getEntireGame().addBreakLoseChance(playerNum);
                
                thisCueFoul.setHeaderReason(strings.getString("breakLoseChance"));
                thisCueFoul.addFoul(String.format(
                        strings.getString("breakLoseChanceDes"),
                        getBreakStats().nBallsPot,
                        getBreakStats().nBallTimesEnterBreakArea) + "\n" +
                        String.format(
                                strings.getString("cumulatedLoseChance"),
                                getEntireGame().getBreakLoseChance(playerNum)
                        ));
                if (getEntireGame().getBreakLoseChance(playerNum) >= 3) {
                    // 累计失机三次，判负一局
                    getEntireGame().clearBreakLoseChance(playerNum);
                    winingPlayer = getAnotherPlayer();
                    end();
                }

                if (pottedBalls.contains(getEightBall())) {
                    // 开球失机但进了黑八
                    pickupCriticalBall(getEightBall());
                }
                switchPlayer();
                return;
            }
        }

        if (!pottedBalls.isEmpty()) {
            if (pottedBalls.contains(getEightBall())) {
                if (currentTarget == 8) {
                    winingPlayer = currentPlayer;
                    if (currentPlayer.getInGamePlayer().isHuman()) {
                        if (currentPlayer.getSinglePoleCount() >= 4) {
                            ChineseEightBallPlayer opponent = getAnotherPlayer(currentPlayer);
                            if (getRemainingBallsOfPlayer(opponent) <= 2) {
                                // 剩一必输
                                CareerAchManager.getInstance().addAchievement(Achievement.REMAIN_ONE_MUST_LOSE, 
                                        currentPlayer.getInGamePlayer());
                            }
                        }
                    }
                    currentPlayer.correctPotBalls(this, Set.of(getEightBall()));
                    end();
                } else if (currentTarget == NOT_SELECTED_REP) {
                    if (isBreaking) {
                        pickupCriticalBall(getEightBall());
                        if (thisCueFoul.isFoul()) {
                            cueBall.pot();
                            ballInHand = true;
                            switchPlayer();
                            System.out.println(thisCueFoul.getAllReasons());
                        } else {
                            currentPlayer.setBreakSuccess(pottedBalls.size());
                            System.out.println("Break pot black not calculated");
                        }
                    } else {
                        winingPlayer = getAnotherPlayer();
                        end();
                    }
                } else {  // 误进黑八
                    winingPlayer = getAnotherPlayer();
                    end();
                    AchManager.getInstance().addAchievement(Achievement.SUICIDE, getCuingIgp());
                }
                return;
            }
            if (currentTarget == NOT_SELECTED_REP) {  // 未选球
                if (isBreaking) {  // 开球进袋不算选球
                    currentPlayer.setBreakSuccess(pottedBalls.size());
                    System.out.println("开球进球不选球");
                    return;
                }
                assert whiteFirstCollide != null;
                PoolBall firstCollide = (PoolBall) whiteFirstCollide;
                if (isFullBall(firstCollide)) {
                    if (hasFullBalls(pottedBalls)) {
                        currentPlayer.setBallRange(FULL_BALL_REP);
                        getAnotherPlayer().setBallRange(HALF_BALL_REP);
                        currentTarget = getTargetOfPlayer(currentPlayer);
                        lastPotSuccess = true;
                        currentPlayer.correctPotBalls(this, fullBallsOf(pottedBalls));
                    }
                }
                if (isHalfBall(firstCollide)) {
                    if (hasHalfBalls(pottedBalls)) {
                        currentPlayer.setBallRange(HALF_BALL_REP);
                        getAnotherPlayer().setBallRange(FULL_BALL_REP);
                        currentTarget = getTargetOfPlayer(currentPlayer);
                        lastPotSuccess = true;
                        currentPlayer.correctPotBalls(this, fullBallsOf(pottedBalls));
                    }
                }
            } else {
                int sucCount = 0;
                int potOppoCount = 0;
                if (currentTarget == FULL_BALL_REP) {
                    sucCount = countFullBalls(pottedBalls);
                    potOppoCount = countHalfBalls(pottedBalls);
                    if (sucCount > 0) {
                        lastPotSuccess = true;
                    }
                    currentPlayer.correctPotBalls(this, fullBallsOf(pottedBalls));
                } else if (currentTarget == HALF_BALL_REP) {
                    sucCount = countHalfBalls(pottedBalls);
                    potOppoCount = countFullBalls(pottedBalls);
                    if (sucCount > 0) {
                        lastPotSuccess = true;
                    }
                    currentPlayer.correctPotBalls(this, halfBallsOf(pottedBalls));
                }
                // 触发成就，且开球不算
                if (sucCount == 2) {
                    AchManager.getInstance().addAchievement(Achievement.POT_TWO_LEGAL, getCuingIgp());
                } else if (sucCount >= 3) {
                    AchManager.getInstance().addAchievement(Achievement.POT_THREE_LEGAL, getCuingIgp());
                }
                if (potOppoCount > 0) {
                    AchManager.getInstance().addAchievement(Achievement.POT_OPPONENT_BALL, getCuingIgp());
                }
            }
        }
        if (lastPotSuccess) {
            potSuccess();
        } else {
            switchPlayer();
        }
        currentTarget = getTargetOfPlayer(currentPlayer);  // 在switchPlayer之后
    }

    private boolean isBreakLoseChance(PlayerPerson breakPlayer) {
        int limit = breakPlayer.getSex() == PlayerPerson.Sex.M ? 4 : 3;

        wasBreakLoseChance = breakStats.nBallsPot + breakStats.nBallTimesEnterBreakArea < limit;
        return wasBreakLoseChance;
    }

    @Override
    public boolean wasIllegalBreak() {
        return wasBreakLoseChance;
    }

    @Override
    protected void updateTargetPotSuccess(boolean isSnookerFreeBall) {

    }

    @Override
    protected void updateTargetPotFailed() {

    }

    @Override
    protected double criticalBallX() {
        return eightBallPosX;
    }
}
