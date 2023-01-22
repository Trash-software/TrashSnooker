package trashsoftware.trashSnooker.core.snooker;

import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.GameSettings;
import trashsoftware.trashSnooker.core.GameRule;
import trashsoftware.trashSnooker.core.TableMetrics;
import trashsoftware.trashSnooker.core.table.AbstractSnookerTable;
import trashsoftware.trashSnooker.core.table.SnookerTable;
import trashsoftware.trashSnooker.core.table.Tables;
import trashsoftware.trashSnooker.fxml.GameView;

public class SnookerGame extends AbstractSnookerGame {

    public SnookerGame(GameView parent, EntireGame entireGame, GameSettings gameSettings, int frameIndex) {
        super(parent, entireGame, gameSettings, new SnookerTable(entireGame.gameValues.table), frameIndex);
    }

    @Override
    public AbstractSnookerTable getTable() {
        return (AbstractSnookerTable) super.getTable();
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
