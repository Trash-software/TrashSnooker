package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.numberedGames.sidePocket.SidePocketPlayer;
import trashsoftware.trashSnooker.core.numberedGames.sidePocket.SidePocketGame;
import trashsoftware.trashSnooker.core.phy.Phy;

public class SidePocketAiCue extends AiCue<SidePocketGame, SidePocketPlayer> {
    public SidePocketAiCue(SidePocketGame game, SidePocketPlayer aiPlayer) {
        super(game, aiPlayer);
    }

    @Override
    protected DefenseChoice standardDefense() {
        return null;
    }

    @Override
    protected DefenseChoice solveSnooker() {
        return null;
    }

    @Override
    protected DefenseChoice breakCue() {
        return null;
    }

    @Override
    public AiCueResult makeCue(Phy phy) {
        return null;
    }
}
