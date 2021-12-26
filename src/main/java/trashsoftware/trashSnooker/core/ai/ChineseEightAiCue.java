package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.CuePlayParams;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallPlayer;

import java.util.List;

public class ChineseEightAiCue extends AiCue<ChineseEightBallGame, ChineseEightBallPlayer> {
    
    public ChineseEightAiCue(ChineseEightBallGame game, ChineseEightBallPlayer aiPlayer) {
        super(game, aiPlayer);
    }

    @Override
    public AiCueResult makeCue() {
        return regularCueDecision();
    }
}
