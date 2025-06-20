package trashsoftware.trashSnooker.core.numberedGames;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallPlayer;
import trashsoftware.trashSnooker.core.snooker.SnookerBall;
import trashsoftware.trashSnooker.core.table.NumberedBallTable;
import trashsoftware.trashSnooker.core.table.Table;
import trashsoftware.trashSnooker.fxml.GameView;

import java.util.Map;
import java.util.Set;

public abstract class NumberedBallGame<P extends NumberedBallPlayer>
        extends Game<PoolBall, P> {
    
    protected P winingPlayer;

    protected NumberedBallGame(EntireGame entireGame, GameSettings gameSettings,
                               GameValues gameValues,
                               Table table,
                               int frameIndex,
                               int frameNumber) {
        super(entireGame, gameSettings, gameValues, table, frameIndex, frameNumber);
    }

    @Override
    protected void cloneBalls(PoolBall[] allBalls) {
        PoolBall[] allBallsCopy = new PoolBall[allBalls.length];
        for (int i = 0; i < allBalls.length; i++) {
            allBallsCopy[i] = (PoolBall) allBalls[i].clone();
        }
        this.allBalls = allBallsCopy;
    }

    @Override
    public void withdraw(Player player) {
        winingPlayer = getAnotherPlayer(player);
        super.withdraw(player);
    }

    protected void pickupCriticalBall(PoolBall ball) {
        double y = gameValues.table.midY;
        for (double x = criticalBallX(); x < gameValues.table.rightX - gameValues.ball.ballRadius; x += 1.0) {
            if (!isOccupied(x, y)) {
                ball.setX(x);
                ball.setY(y);
                ball.pickup();
                return;
            }
        }
        throw new RuntimeException("Cannot place eight ball");
    }

    @Override
    protected void setBreakingPlayer(Player breakingPlayer) {
        super.setBreakingPlayer(breakingPlayer);

        ((NumberedBallPlayer) breakingPlayer).setBreakingPlayer();
    }

    @Override
    public void switchPlayer() {
        super.switchPlayer();
        currentPlayer.incrementPlayTimes();
    }

    @Override
    protected PoolBall createWhiteBall() {
        return new PoolBall(0, true, gameValues);
    }

    @Override
    protected boolean isBallPlacedInHeap(Ball ball) {
        return !ball.isWhite();
    }

    @Override
    public NumberedBallTable getTable() {
        return (NumberedBallTable) table;
    }
    
    public abstract int getNumBallsTotal();

    /**
     * @return 致胜球（8或9）的置球点x
     */
    protected abstract double criticalBallX();
}
