package trashsoftware.trashSnooker.core.numberedGames.sidePocket;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.ai.AiCue;
import trashsoftware.trashSnooker.core.ai.SidePocketAiCue;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallGame;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.scoreResult.NineBallScoreResult;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;
import trashsoftware.trashSnooker.core.table.SidePocketTable;

import java.util.*;

public class SidePocketGame extends NumberedBallGame<SidePocketPlayer>
        implements NeedBigBreak {
    
    private final LinkedHashMap<PoolBall, Boolean> pottedRecord = new LinkedHashMap<>();  // 缓存用，仅用于GameView画目标球
    protected SidePocketPlayer winingPlayer;
    protected NineBallScoreResult curResult;

    public SidePocketGame(EntireGame entireGame, GameSettings gameSettings, int frameIndex) {
        super(entireGame, gameSettings, new SidePocketTable(entireGame.gameValues.table), frameIndex);

        initBalls();
        currentTarget = 1;
    }

    public Map<PoolBall, Boolean> getBalls() {
        for (int i = 1; i < 10; i++) {
            pottedRecord.put(allBalls[i], allBalls[i].isPotted());
        }
        return pottedRecord;
    }

    @Override
    public GameRule getGameType() {
        return GameRule.SIDE_POCKET;
    }

    public Movement cue(CuePlayParams params, Phy phy) {
        createScoreResult();
        return super.cue(params, phy);
    }

    private void initBalls() {
        allBalls = new PoolBall[10];

        allBalls[0] = cueBall;
        for (int i = 1; i < 10; ++i) {
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
                placeOrder.get(index++).setXY(curX, y);
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
                getBalls());
    }

    @Override
    protected AiCue<?, ?> createAiCue(SidePocketPlayer aiPlayer) {
        return new SidePocketAiCue(this, aiPlayer);
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
        player1 = new SidePocketPlayer(gameSettings.getPlayer1());
        player2 = new SidePocketPlayer(gameSettings.getPlayer2());
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
        if (!collidesWall && pottedBalls.isEmpty()) {
            thisCueFoul.addFoul(strings.getString("noBallHitCushion"));
        }
        PoolBall nineBall = getNineBall();
        if (cueBall.isPotted()) {
            if (nineBall.isPotted()) {  // 白球九号一起进
                winingPlayer = getAnotherPlayer();
                end();
                return;
            }
            thisCueFoul.addFoul(strings.getString("cueBallPot"));
        }
        // todo: 开球后的推球规则
        if (whiteFirstCollide == null) {
            thisCueFoul.addFoul(strings.getString("emptyCue"), true);
        } else {
            int actualHit = whiteFirstCollide.getValue();
            if (actualHit != currentTarget) {
                thisCueFoul.addFoul(String.format(
                                strings.getString("targetXHitYNumbered"), currentTarget, actualHit),
                        true);
            }
        }

        if (thisCueFoul.isFoul()) {
            if (nineBall.isPotted()) {
                winingPlayer = getAnotherPlayer();
                end();
                return;
            }

            cueBall.pot();
            ballInHand = true;
            switchPlayer();
            forceUpdateTarget();
            System.out.println(thisCueFoul.getAllReasons());
            return;
        }

        // 已经是没犯规的前提了
        if (!pottedBalls.isEmpty()) {
            lastPotSuccess = true;
            currentPlayer.correctPotBalls(pottedBalls);
            if (isBreaking) {
                currentPlayer.setBreakSuccess();
            }
            if (pottedBalls.contains(nineBall)) {
                if (isBreaking) {
                    // todo: 记录黄金九
                }
                winingPlayer = currentPlayer;
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
        return GamePlayStage.NORMAL;
    }

    protected Ball getMinimumBallOnTable(int base) {
        for (int i = base + 1; i < allBalls.length; i++) {
            PoolBall ball = allBalls[i];
            if (!ball.isPotted()) return ball;
        }
        return null;
    }
}
