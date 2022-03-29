package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.CuePlayParams;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallPlayer;

public class ChineseEightAiCue extends AiCue<ChineseEightBallGame, ChineseEightBallPlayer> {

    public ChineseEightAiCue(ChineseEightBallGame game, ChineseEightBallPlayer aiPlayer) {
        super(game, aiPlayer);
    }
    
    public static boolean isCenterBreak(ChineseEightBallPlayer player) {
        return player.getPlayerPerson().getControllablePowerPercentage() >= 80.0;
    }

    @Override
    protected DefenseChoice breakCue() {
        // todo: 小力开球和大力开球
//        if (aiPlayer.getPlayerPerson().getControllablePowerPercentage() < 80.0) {
//            
//        } else {
//            
//        }
        return centerBreak();
    }
    
    private DefenseChoice centerBreak() {
        double dirX = game.breakPointX() - game.getCueBall().getX();
        double dirY = game.getGameValues().midY - game.getCueBall().getY();
        double[] unitXY = Algebra.unitVector(dirX, dirY);
        double selectedPower = aiPlayer.getPlayerPerson().getMaxPowerPercentage();
        CuePlayParams cpp = CuePlayParams.makeIdealParams(
                unitXY[0],
                unitXY[1],
                0.0,
                0.0,
                5.0,
                selectedPowerToActualPower(selectedPower)
        );
        return new DefenseChoice(unitXY, selectedPower, cpp);
    }

    @Override
    public AiCueResult makeCue() {
        return regularCueDecision();
    }
}
