package trashsoftware.trashSnooker.core.numberedGames;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.table.Table;
import trashsoftware.trashSnooker.fxml.GameView;

import java.util.HashMap;
import java.util.Map;

public abstract class NumberedBallGame<P extends NumberedBallPlayer> 
        extends Game<PoolBall, P> {

    protected NumberedBallGame(GameView parent, GameSettings gameSettings, 
                               GameValues gameValues, int frameIndex) {
        super(parent, gameSettings, gameValues, frameIndex);
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
