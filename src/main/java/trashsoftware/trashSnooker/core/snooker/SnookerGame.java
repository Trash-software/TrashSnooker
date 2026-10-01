package trashsoftware.trashSnooker.core.snooker;

import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.GameSettings;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.table.AbstractSnookerTable;
import trashsoftware.trashSnooker.core.table.SnookerTable;

public class SnookerGame extends AbstractSnookerGame<SnookerTable> {

    public SnookerGame(EntireGame entireGame, GameSettings gameSettings, GameValues gameValues, int frameIndex, int frameRestartIndex) {
        super(entireGame, gameSettings, gameValues, new SnookerTable(gameValues.table), frameIndex, frameRestartIndex);
    }

    @Override
    public GameRule getGameType() {
        return GameRule.SNOOKER;
    }

    @Override
    protected int numRedBalls() {
        return 15;
    }
}
