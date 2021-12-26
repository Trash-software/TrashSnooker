package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.CuePlayParams;
import trashsoftware.trashSnooker.core.numberedGames.sidePocket.SidePocketBallPlayer;
import trashsoftware.trashSnooker.core.numberedGames.sidePocket.SidePocketGame;

public class SidePocketAiCue extends AiCue<SidePocketGame, SidePocketBallPlayer> {
    public SidePocketAiCue(SidePocketGame game, SidePocketBallPlayer aiPlayer) {
        super(game, aiPlayer);
    }

    @Override
    public AiCueResult makeCue() {
        return null;
    }
}
