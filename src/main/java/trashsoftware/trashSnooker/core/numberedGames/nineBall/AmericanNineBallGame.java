package trashsoftware.trashSnooker.core.numberedGames.nineBall;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.ai.AiCue;
import trashsoftware.trashSnooker.core.ai.AmericanNineAiCue;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;
import trashsoftware.trashSnooker.core.career.achievement.CareerAchManager;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallGame;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.scoreResult.NineBallScoreResult;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;
import trashsoftware.trashSnooker.core.table.SidePocketTable;

import java.util.*;
import java.util.stream.Collectors;

public class AmericanNineBallGame extends NumberedBallGame<AmericanNineBallPlayer>
        implements NeedBigBreak {

    private final LinkedHashMap<PoolBall, Boolean> pottedRecord = new LinkedHashMap<>();  // 缓存用，仅用于GameView画目标球
    protected NineBallScoreResult curResult;
    protected boolean pushingOut;
    private boolean pushedOut;
    private boolean wasIllegalBreak;

    public AmericanNineBallGame(EntireGame entireGame, GameSettings gameSettings, GameValues gameValues, int frameIndex, int frameRestartIndex) {
        super(entireGame, gameSettings, gameValues, new SidePocketTable(gameValues.table), frameIndex, frameRestartIndex);

        initBalls();
        currentTarget = 1;
    }
    
    public boolean currentlyCanPushOut() {
        return finishedCuesCount == 1;
    }
    
    public boolean isPushingOut() {
        return pushingOut;
    }

    public boolean lastCueWasPushOut() {
        return finishedCuesCount == 2 && pushedOut;
    }

    public void pushOut() {
        pushingOut = true;
        currentTarget = 0;
    }

    @Override
    public int getNumBallsTotal() {
        return 10;
    }

    public Map<PoolBall, Boolean> getBalls() {
        for (int i = 1; i < getNumBallsTotal(); i++) {
            pottedRecord.put(allBalls[i], allBalls[i].isPotted());
        }
        return pottedRecord;
    }

    @Override
    public GameRule getGameType() {
        return GameRule.AMERICAN_NINE;
    }

    public Movement cue(CuePlayParams params, Phy phy) {
        createScoreResult();
        return super.cue(params, phy);
    }

    private void initBalls() {
        allBalls = new PoolBall[getNumBallsTotal()];

        allBalls[0] = cueBall;
        for (int i = 1; i < getNumBallsTotal(); ++i) {
            allBalls[i] = new PoolBall(i, false, gameValues);
            pottedRecord.put(allBalls[i], false);
        }
        List<PoolBall> placeOrder = new ArrayList<>(List.of(allBalls).subList(1, 10));
        Collections.shuffle(placeOrder);
        for (int i = 0; i < 9; ++i) {
            PoolBall cur = placeOrder.get(i);
            if (i != 0 && cur.getValue() == 1) {
                placeOrder.set(i, placeOrder.get(0));
                placeOrder.set(0, cur);
            }
            if (i != 4 && cur.getValue() == 9) {
                placeOrder.set(i, placeOrder.get(4));
                placeOrder.set(4, cur);
            }
        }

        double curX = getTable().firstBallPlacementX();
        double rowOccupyX = gameValues.ball.ballDiameter * Math.sin(Math.toRadians(60.0))
                + Game.MIN_PLACE_DISTANCE * 0.6;

        int[] numBallsRow = new int[]{1, 2, 3, 2, 1};
        int index = 0;
        for (int row = 0; row < 5; ++row) {
            double rowBalls = numBallsRow[row];
            double y = gameValues.table.midY - (gameValues.ball.ballRadius + Game.MIN_PLACE_DISTANCE) * (rowBalls - 1);
            for (int col = 0; col < rowBalls; ++col) {
                PoolBall ball = placeOrder.get(index++);
                ball.setXY(curX, y);
                y += gameValues.ball.ballDiameter + Game.MIN_PLACE_DISTANCE;
            }

            curX += rowOccupyX;
        }
    }

    @Override
    public ScoreResult makeScoreResult(Player justCuedPlayer) {
        return curResult;
    }

    private void createScoreResult() {
        curResult = new NineBallScoreResult(
                thinkTime,
                getCuingPlayer().getInGamePlayer().getPlayerNumber(),
                getBalls(),
                getCuingPlayer().getSinglePoleCount());
    }

    @Override
    protected AiCue<?, ?> createAiCue(AmericanNineBallPlayer aiPlayer) {
        return new AmericanNineAiCue(this, aiPlayer);
    }

    @Override
    public boolean isLegalBall(Ball ball, int targetRep, boolean isSnookerFreeBall, boolean isInLineHandBall) {
        return ball.getValue() == targetRep;
    }

    @Override
    public int getTargetAfterPotSuccess(Ball pottingBall, boolean isSnookerFreeBall) {
        if (pottingBall.getValue() == 9) return END_REP;

        Ball minNext = getMinimumBallOnTable(pottingBall.getValue());
        return minNext.getValue();
    }

    @Override
    public int getTargetAfterPotFailed() {
        return currentTarget;
    }

    @Override
    public double priceOfTarget(int targetRep, Ball ball, Player attackingPlayer,
                                Ball lastPotting) {
        return 1.0;
    }

    @Override
    public SidePocketTable getTable() {
        return (SidePocketTable) super.getTable();
    }

    @Override
    protected void initPlayers() {
        player1 = new AmericanNineBallPlayer(gameSettings.getPlayer1());
        player2 = new AmericanNineBallPlayer(gameSettings.getPlayer2());
    }

    @Override
    protected boolean canPlaceWhiteInTable(double x, double y) {
        if (isBreaking()) {
            return x <= getTable().breakLineX() && !isOccupied(x, y);
        } else {
            return !isOccupied(x, y);
        }
    }

    @Override
    public Player getWiningPlayer() {
        return winingPlayer;
    }

    @Override
    protected void endMoveAndUpdate() {
        updateScore(newPotted);
    }

    private void updateScore(Set<PoolBall> pottedBalls) {
        boolean baseFoul = checkStandardFouls(() -> 1);

        PoolBall nineBall = getNineBall();
        if (baseFoul && nineBall.isNotOnTable()) {  // 白球九号一起进
            if (isBreaking) {
                AchManager.getInstance().addAchievement(Achievement.WHITE_GOLDEN_NINE, getCuingIgp());
                pickupCriticalBall(getNineBall());
                setBallInHand();
                switchPlayer();
                return;
            }
            winingPlayer = getAnotherPlayer();
            end();
            AchManager.getInstance().addAchievement(Achievement.SUICIDE, getCuingIgp());
            return;
        }

        if (isPushingOut()) {
            thisCueFoul.removeFoul(strings.getString("emptyCue"));
            thisCueFoul.removeFoul(strings.getString("noBallHitCushion"));
            AchManager.getInstance().removePendingAch(Achievement.MISSED_SHOT);
            thisCueFoul.setMiss(false);
            
            pushedOut = true;
            pushingOut = false;

            if (pottedBalls.contains(getNineBall())) {
                // 推球时进了九号
                pickupCriticalBall(getNineBall());
                thisCueFoul.addFoul("potNineWhilePushing");
            }
        } else if (whiteFirstCollide == null) {
            thisCueFoul.addFoul(strings.getString("emptyCue"), true);
        } else {
            int actualHit = whiteFirstCollide.getValue();
            if (actualHit != currentTarget) {
                thisCueFoul.addFoul(String.format(
                                strings.getString("targetXHitYNumbered"), currentTarget, actualHit),
                        true);
            }
        }

        if (isBreaking) {
            updateBreakStats(newPotted);
            if (isBreakFoul()) {
                thisCueFoul.addFoul(String.format(
                        strings.getString("breakFoulDes"),
                        breakStats.nBallsHitCushion
                ));
            } else if (isIllegalBreak()) {
                if (pottedBalls.contains(getNineBall())) {
                    // 非法开球但进了九号
                    pickupCriticalBall(getNineBall());
                }
                thisCueFoul.setIllegal(true);
                thisCueFoul.setHeaderReason(strings.getString("illegalBreak"));
                thisCueFoul.addFoul(String.format(
                        strings.getString("breakLoseChanceDes"),
                        breakStats.nBallsPot,
                        breakStats.nBallTimesEnterBreakArea
                ));
                switchPlayer();
                forceUpdateTarget();
                // 不是自由球
                return;
            }
        }

        if (thisCueFoul.isFoul()) {
            setBallInHand();

            if (nineBall.isPotted()) {
                // 在cueBall.pot之后
                pickupCriticalBall(getNineBall());
            }
            
            switchPlayer();
            forceUpdateTarget();
            System.out.println(thisCueFoul.getAllReasons());
            return;
        }

        // 已经是没犯规的前提了
        if (!pottedBalls.isEmpty()) {
            lastPotSuccess = true;
            currentPlayer.correctPotBalls(this, pottedBalls);
            if (isBreaking) {
                currentPlayer.setBreakSuccess(pottedBalls.size());
            } else {
                // 成就的触发条件是非开球
                if (pottedBalls.size() == 2) {
                    AchManager.getInstance().addAchievement(Achievement.POT_TWO_LEGAL, getCuingIgp());
                } else if (pottedBalls.size() >= 3) {
                    AchManager.getInstance().addAchievement(Achievement.POT_THREE_LEGAL, getCuingIgp());
                }
            }
            
            
            if (pottedBalls.contains(nineBall)) {
                if (isBreaking) {
                    currentPlayer.setGoldNine();
                    AchManager.getInstance().addAchievement(Achievement.GOLDEN_NINE, getCuingIgp());
                }
                if (currentTarget != 9) {
                    AchManager.getInstance().addAchievement(Achievement.PASS_NINE, getCuingIgp());
                }
                winingPlayer = currentPlayer;
                
                AmericanNineBallPlayer loser = getAnotherPlayer(currentPlayer);
                Set<Integer> loserPotted = loser.getAllPotted().stream().map(Ball::getValue).collect(Collectors.toSet());
                if (!loserPotted.contains(9) && loserPotted.size() == 8) {
                    CareerAchManager.getInstance().addAchievement(Achievement.BALL_WORKER, loser.getInGamePlayer());
                }
                
                end();
                return;
            }
        }
        if (lastPotSuccess) {
            potSuccess();
        } else {
            switchPlayer();
        }
        forceUpdateTarget();
    }

    public PoolBall getNineBall() {
        return getBallByValue(9);
    }

    private boolean isBreakFoul() {
        // 开球犯规
        if (breakStats.nBallsPot > 0) return false;
        return breakStats.nBallsHitCushion < 4;
    }

    private boolean isIllegalBreak() {
        // 开球违例，同中八开球失机
        wasIllegalBreak = breakStats.nBallsPot + breakStats.nBallTimesEnterBreakArea < 3;
        return wasIllegalBreak;
    }

    @Override
    protected void updateTargetPotSuccess(boolean isSnookerFreeBall) {
        if (currentTarget == 9) {
            currentTarget = END_REP;
            return;
        }
        forceUpdateTarget();
    }

    @Override
    protected void updateTargetPotFailed() {
        forceUpdateTarget();
    }

    private void forceUpdateTarget() {
        Ball minNext = getMinimumBallOnTable(0);  // 就是找桌面上最小的球
        currentTarget = minNext.getValue();
    }

    @Override
    public GamePlayStage getGamePlayStage(Ball predictedTargetBall, boolean printPlayStage) {
        if (isBreaking()) return GamePlayStage.BREAK;
        if (currentTarget == 8) return GamePlayStage.NEXT_BALL_WIN;
        if (currentTarget == 9) return GamePlayStage.THIS_BALL_WIN;
        return GamePlayStage.NORMAL;
    }

    protected Ball getMinimumBallOnTable(int base) {
        for (int i = base + 1; i < allBalls.length; i++) {
            PoolBall ball = allBalls[i];
            if (!ball.isPotted()) return ball;
        }
        return null;
    }

    @Override
    public boolean wasIllegalBreak() {
        return wasIllegalBreak;
    }

    @Override
    protected double criticalBallX() {
        return getTable().firstBallPlacementX();
    }
}
