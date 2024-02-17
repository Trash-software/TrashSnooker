package trashsoftware.trashSnooker.core.snooker;

import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.GameSettings;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.table.SnookerTenTable;
import trashsoftware.trashSnooker.core.table.Table;

public class SnookerTenGame extends AbstractSnookerGame {
    
    public SnookerTenGame(EntireGame entireGame, GameSettings gameSettings, GameValues gameValues, int frameIndex) {
        super(entireGame, gameSettings, gameValues, new SnookerTenTable(gameValues.table), frameIndex);
    }

    @Override
    public GameRule getGameType() {
        return GameRule.SNOOKER_TEN;
    }

    @Override
    protected int numRedBalls() {
        return 10;
    }
}
