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
    
    protected BreakStats breakStats;
    protected P winingPlayer;

    protected NumberedBallGame(EntireGame entireGame, GameSettings gameSettings,
                               GameValues gameValues,
                               Table table,
                               int frameIndex) {
        super(entireGame, gameSettings, gameValues, table, frameIndex);
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

    protected void updateBreakStats(Set<PoolBall> newPotted) {
        int uniqueBallsHitCushion = 0;
        int acrossBreakLine = 0;
        for (PoolBall ball : getAllBalls()) {
            if (!ball.isWhite()) {
                int[] stats = computeCushionAndAcrossLineOfBall(ball);
                acrossBreakLine += stats[1];
                if (stats[0] > 0) {
                    uniqueBallsHitCushion++;
                }
            }
        }
        
        breakStats = new BreakStats(newPotted.size(), uniqueBallsHitCushion, acrossBreakLine);
    }

    public BreakStats getBreakStats() {
        return breakStats;
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
    public NumberedBallTable getTable() {
        return (NumberedBallTable) table;
    }
    
    public abstract int getNumBallsTotal();

    /**
     * @return 致胜球（8或9）的置球点x
     */
    protected abstract double criticalBallX();
}
