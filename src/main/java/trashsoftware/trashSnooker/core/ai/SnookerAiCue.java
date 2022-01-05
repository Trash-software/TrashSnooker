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
    protected DefenseChoice breakCue() {
        return getBestDefenseChoice(40, 41);
    }

    @Override
    public AiCueResult makeCue() {
        int behind = -game.getScoreDiff(aiPlayer);
        int rem = game.getRemainingScore();
        if (behind > rem) {
            if (behind > rem + 12) {
                // 超太多了，认输
                return null;
            } else {
                if (rem == 7) return null;  // 只剩一颗球还防个屁
                else if (behind > rem + 8 && rem <= 27) return null; // 清彩阶段，落后多了就认输
            }
            // 其他情况还可以挣扎
        }
        return regularCueDecision();
    }

}
