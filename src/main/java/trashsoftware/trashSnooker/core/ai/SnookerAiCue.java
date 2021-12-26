package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.CuePlayParams;
import trashsoftware.trashSnooker.core.snooker.AbstractSnookerGame;
import trashsoftware.trashSnooker.core.snooker.SnookerPlayer;

import java.util.List;

public class SnookerAiCue extends AiCue<AbstractSnookerGame, SnookerPlayer> {

    public SnookerAiCue(AbstractSnookerGame game, SnookerPlayer aiPlayer) {
        super(game, aiPlayer);
    }

    @Override
    public AiCueResult makeCue() {
        if (-game.getScoreDiff(aiPlayer) > game.getRemainingScore()) {
            // 超分了，认输
            return null;
        }
        return regularCueDecision();
    }

}
