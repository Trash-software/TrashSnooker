package trashsoftware.trashSnooker.core.snooker;

import trashsoftware.trashSnooker.core.GameSettings;
import trashsoftware.trashSnooker.core.GameType;
import trashsoftware.trashSnooker.core.GameValues;
import trashsoftware.trashSnooker.core.table.AbstractSnookerTable;
import trashsoftware.trashSnooker.core.table.Tables;
import trashsoftware.trashSnooker.fxml.GameView;

public class SnookerGame extends AbstractSnookerGame {

    public SnookerGame(GameView parent, GameSettings gameSettings, int frameIndex) {
        super(parent, gameSettings, GameValues.SNOOKER_VALUES, frameIndex);
    }

    @Override
    public AbstractSnookerTable getTable() {
        return Tables.SNOOKER_TABLE;
    }

    @Override
    public GameType getGameType() {
        return GameType.SNOOKER;
    }

    @Override
    protected int numRedBalls() {
        return 15;
    }
}
