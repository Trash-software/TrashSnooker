package trashsoftware.trashSnooker.core.game;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.table.Table;

public abstract class FixedCueBallGame<B extends Ball, P extends Player> extends Game<B, P> {

    protected B cueBall;
    
    protected FixedCueBallGame(EntireGame entireGame, GameSettings gameSettings, GameValues gameValues, Table table, int frameIndex, int frameNumber) {
        super(entireGame, gameSettings, gameValues, table, frameIndex, frameNumber);
    }

    @Override
    public boolean cueBallSwitchable() {
        return false;
    }

    @Override
    public void setCueBall(Ball cueBall) {
        this.cueBall = (B) cueBall;
    }

    @Override
    public B getCueBall() {
        return cueBall;
    }
}
