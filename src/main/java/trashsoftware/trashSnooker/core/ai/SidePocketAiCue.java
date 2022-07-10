package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.numberedGames.sidePocket.SidePocketPlayer;
import trashsoftware.trashSnooker.core.numberedGames.sidePocket.SidePocketGame;

public class SidePocketAiCue extends AiCue<SidePocketGame, SidePocketPlayer> {
    public SidePocketAiCue(SidePocketGame game, SidePocketPlayer aiPlayer) {
        super(game, aiPlayer);
    }

    @Override
    protected DefenseChoice breakCue() {
        return null;
    }

    @Override
    public AiCueResult makeCue() {
        return null;
    }
}
