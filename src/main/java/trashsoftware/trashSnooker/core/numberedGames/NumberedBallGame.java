package trashsoftware.trashSnooker.core.numberedGames;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.snooker.SnookerBall;
import trashsoftware.trashSnooker.core.table.NumberedBallTable;
import trashsoftware.trashSnooker.core.table.Table;
import trashsoftware.trashSnooker.fxml.GameView;

public abstract class NumberedBallGame<P extends NumberedBallPlayer>
        extends Game<PoolBall, P> {

    protected NumberedBallGame(EntireGame entireGame, GameSettings gameSettings,
                               Table table,
                               int frameIndex) {
        super(entireGame, gameSettings, entireGame.gameValues, table, frameIndex);
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
}
