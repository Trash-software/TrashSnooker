package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.GameValues;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallPlayer;

import java.util.ArrayList;
import java.util.List;

public class ChineseEightAiCueBallPlacer extends 
        AiCueBallPlacer<ChineseEightBallGame, ChineseEightBallPlayer> {
    
    public ChineseEightAiCueBallPlacer(ChineseEightBallGame game, ChineseEightBallPlayer player) {
        super(game, player);
    }

    @Override
    protected double[] breakPosition() {
        if (ChineseEightAiCue.isCenterBreak(player)) {
            return new double[]{game.breakLineX(), game.getGameValues().midY};
        } else {
            return new double[]{game.breakLineX(), 
                    game.getGameValues().botY - game.getGameValues().ballDiameter * 1.5};
        }
    }

    @Override
    protected List<double[]> legalPositions() {
        GameValues values = game.getGameValues();
        double xLimit = values.rightX - values.ballRadius;
        if (game.isBreaking() || game.isJustAfterBreak()) {
            xLimit = game.breakLineX();
        }
        
        List<double[]> posList = new ArrayList<>();
        double xTick = (values.innerWidth - values.ballDiameter) / 48.0;
        double yTick = (values.innerHeight - values.ballDiameter) / 24.0;
        for (double x = values.leftX + values.ballRadius; x <= xLimit; x += xTick) {
            for (double y = values.topY + values.ballRadius; y < values.botY ; y += yTick) {
                if (game.canPlaceWhite(x, y)) {
                    posList.add(new double[]{x, y});
                }
            }
        }
        return posList;
    }
}
