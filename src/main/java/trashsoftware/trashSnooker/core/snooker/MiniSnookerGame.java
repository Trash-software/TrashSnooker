package trashsoftware.trashSnooker.core.snooker;

import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.GameSettings;
import trashsoftware.trashSnooker.core.GameType;
import trashsoftware.trashSnooker.core.GameValues;
import trashsoftware.trashSnooker.core.table.AbstractSnookerTable;
import trashsoftware.trashSnooker.core.table.Tables;
import trashsoftware.trashSnooker.fxml.GameView;

public class MiniSnookerGame extends AbstractSnookerGame {

    public MiniSnookerGame(GameView parent, EntireGame entireGame, 
                           GameSettings gameSettings, int frameIndex) {
        super(parent, entireGame, gameSettings, GameValues.MINI_SNOOKER_VALUES, frameIndex);
    }

    @Override
    public AbstractSnookerTable getTable() {
        return Tables.MINI_SNOOKER_TABLE;
    }

    @Override
    public GameType getGameType() {
        return GameType.MINI_SNOOKER;
    }

    @Override
    protected int numRedBalls() {
        return 6;
    }
}
