package trashsoftware.trashSnooker.core.game;

import org.jetbrains.annotations.NotNull;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.table.Table;

public abstract class VariableCueBallGame<B extends Ball, P extends Player, T extends Table> 
        extends Game<B, P, T> {
    protected B cueBall;
    
    protected VariableCueBallGame(EntireGame entireGame, GameSettings gameSettings, GameValues gameValues, T table, int frameIndex, int frameNumber) {
        super(entireGame, gameSettings, gameValues, table, frameIndex, frameNumber);
    }
    
    @NotNull
    protected B findFirstAvailableBall() {
        for (B ball : getAllBalls()) {
            if (!ball.isPotted()) {
                return ball;
            }
        }
        if (cueBall == null) {
            System.err.println("No ball alive?");
        }
        return getAllBalls()[0];
    }

    @Override
    public B getCueBall() {
        if (cueBall == null) {
            cueBall = findFirstAvailableBall();
        }
        return cueBall;
    }

    @Override
    public void setCueBall(Ball cueBall) {
        this.cueBall = (B) cueBall;
    }
}
