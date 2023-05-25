package trashsoftware.trashSnooker.core.training;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallGame;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;

public class PoolTraining extends ChineseEightBallGame implements Training {

    protected final TrainType trainType;
    protected boolean ordered;
    protected Challenge challenge;

    public PoolTraining(EntireGame entireGame, GameSettings gameSettings, TrainType trainType, Challenge challenge) {
        super(entireGame, gameSettings, 1);

        this.trainType = trainType;
        this.challenge = challenge;
        this.ordered = trainType == TrainType.SNAKE_FULL_ORDERED || trainType == TrainType.SNAKE_HALF_ORDERED;
        if (ordered) currentTarget = 1;
        else currentTarget = NOT_SELECTED_REP;

        placeSnake();

        isBreaking = false;
    }

    protected void placeSnake() {
        switch (trainType) {
            case SNAKE_FULL:
            case SNAKE_FULL_ORDERED:
                placeFromBottom(getColoredBalls(15));
                break;
            case SNAKE_HALF:
            case SNAKE_HALF_ORDERED:
                placeFromBottom(getColoredBalls(8));
                hideOtherBalls(8);
                break;
        }
    }

    private PoolBall[] getColoredBalls(int nBalls) {
        PoolBall[] res = new PoolBall[nBalls];
        System.arraycopy(getAllBalls(), 1, res, 0, nBalls);
        return res;
    }

    private void hideOtherBalls(int nBallsOnTable) {
        PoolBall[] balls = getAllBalls();
        for (int i = nBallsOnTable + 1; i < balls.length; i++) {
            balls[i].pot();
        }
    }

    protected void placeFromBottom(PoolBall[] ballOrder) {
        double gap = gameValues.table.innerWidth / 16;
        double baseX = gameValues.table.rightX - gap;
        for (int i = 0; i < ballOrder.length; i++) {
            ballOrder[i].setXY(baseX - i * gap, gameValues.table.midY);
        }
    }

    @Override
    public int getTargetAfterPotSuccess(Ball pottingBall, boolean isSnookerFreeBall) {
        if (ordered) {
            return currentTarget + 1;
        } else {
            return NOT_SELECTED_REP;
        }
    }

    @Override
    public int getTargetAfterPotFailed() {
        return currentTarget;
    }

    @Override
    protected void endMoveAndUpdate() {
        if (cueBall.isPotted()) {
            thisCueFoul.addFoul(strings.getString("cueBallPot"));
        }

        if (whiteFirstCollide == null) {
            thisCueFoul.addFoul(strings.getString("emptyCue"), 1, true);
        } else {
            if (ordered && whiteFirstCollide.getValue() != currentTarget) {
                thisCueFoul.addFoul(String.format(
                                strings.getString("targetXHitYNumbered"), currentTarget, whiteFirstCollide),
                        true);
            } else if (!thisCueFoul.isFoul() && newPotted.size() == 1) {
                PoolBall onlyPot = null;
                for (PoolBall b : newPotted) onlyPot = b;
                
                if (onlyPot == whiteFirstCollide) {
                    lastPotSuccess = true;
                }
            }
        }
        
        if (getChallenge() != null && !lastPotSuccess) {
            // challenge failed
            winingPlayer = getAnotherPlayer();
            end();
        }
        
        if (getRemTargetBallsOnTable() == 0) {
            winingPlayer = getPlayer1();
            end();
            return;
        }
        
        currentTarget = getTargetOfPlayer(getPlayer1());
    }

    @Override
    public GamePlayStage getGamePlayStage(Ball predictedTargetBall, boolean printPlayStage) {
        return GamePlayStage.NORMAL;
    }

    @Override
    public boolean isLegalBall(Ball ball, int targetRep, boolean isSnookerFreeBall, boolean isInLineHandBall) {
        if (ordered) {
            return targetRep == ball.getValue();
        } else {
            return true;
        }
    }

    @Override
    protected int getTargetOfPlayer(Player playerX) {
        if (ordered) {
            return getMinimumBallOnTable(0).getValue();
        } else return NOT_SELECTED_REP;
    }

    private int getRemTargetBallsOnTable() {
        int rem = 0;
        for (Ball ball : getAllBalls()) {
            if (!ball.isWhite() && !ball.isPotted()) rem++;
        }
        return rem;
    }

    protected Ball getMinimumBallOnTable(int base) {
        for (int i = base + 1; i < allBalls.length; i++) {
            PoolBall ball = allBalls[i];
            if (!ball.isPotted()) return ball;
        }
        return null;
    }

    @Override
    protected boolean canPlaceWhiteInTable(double x, double y) {
        return !isOccupied(x, y);
    }

    @Override
    public int getNumBallsTotal() {
        return 16;
    }

    @Override
    public TrainType getTrainType() {
        return trainType;
    }

    @Override
    public Challenge getChallenge() {
        return challenge;
    }
}
