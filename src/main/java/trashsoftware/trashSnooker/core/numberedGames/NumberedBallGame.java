package trashsoftware.trashSnooker.core.numberedGames;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.table.Table;
import trashsoftware.trashSnooker.fxml.GameView;

public abstract class NumberedBallGame<P extends NumberedBallPlayer>
        extends Game<PoolBall, P> {

    protected NumberedBallGame(GameView parent, EntireGame entireGame, GameSettings gameSettings,
                               Table table,
                               int frameIndex) {
        super(parent, entireGame, gameSettings, entireGame.gameValues, table, frameIndex);
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
}
