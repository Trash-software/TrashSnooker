package trashsoftware.trashSnooker.core.snooker;

import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.GameSettings;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.table.AbstractSnookerTable;
import trashsoftware.trashSnooker.core.table.SnookerTable;
import trashsoftware.trashSnooker.fxml.GameView;

public class SnookerGame extends AbstractSnookerGame {

    public SnookerGame(EntireGame entireGame, GameSettings gameSettings, GameValues gameValues, int frameIndex) {
        super(entireGame, gameSettings, gameValues, new SnookerTable(gameValues.table), frameIndex);
    }

    @Override
    public AbstractSnookerTable getTable() {
        return super.getTable();
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
