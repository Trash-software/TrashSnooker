package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.metrics.GameValues;
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
            return new double[]{game.getTable().breakLineX(), game.getGameValues().table.midY};
        } else {
            return new double[]{game.getTable().breakLineX(), 
                    game.getGameValues().table.botY - game.getGameValues().ball.ballDiameter * 1.5};
        }
    }

    @Override
    protected List<double[]> legalPositions() {
        List<double[]> legalPos;
        double sep = 24;
        do {
            legalPos = legalPositions(sep);
            sep *= 1.5;
        } while (legalPos.isEmpty() && sep < 1000);
        
        return legalPos;
    }

    private List<double[]> legalPositions(double smallSep) {
        GameValues values = game.getGameValues();
        double xLimit = values.table.rightX - values.ball.ballRadius;
        if (game.isBreaking() || game.isJustAfterBreak()) {
            xLimit = game.getTable().breakLineX();
        }

        List<double[]> posList = new ArrayList<>();
        double xTick = (values.table.innerWidth - values.ball.ballDiameter) / smallSep / 2;
        double yTick = (values.table.innerHeight - values.ball.ballDiameter) / smallSep;
        for (double x = values.table.leftX + values.ball.ballRadius; x <= xLimit; x += xTick) {
            for (double y = values.table.topY + values.ball.ballRadius; y < values.table.botY ; y += yTick) {
                if (game.canPlaceWhite(x, y)) {
                    posList.add(new double[]{x, y});
                }
            }
        }
        return posList;
    }
}
