package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.numberedGames.nineBall.AmericanNineBallGame;
import trashsoftware.trashSnooker.core.numberedGames.nineBall.AmericanNineBallPlayer;

import java.util.ArrayList;
import java.util.List;

public class SidePocketAiCueBallPlacer extends 
        NumberedGameAiCueBallPlacer<AmericanNineBallGame, AmericanNineBallPlayer> {
    
    public SidePocketAiCueBallPlacer(AmericanNineBallGame game, AmericanNineBallPlayer player) {
        super(game, player);
    }

    @Override
    protected List<double[]> legalPositions(double smallSep) {
        GameValues values = game.getGameValues();
        double xLimit = values.table.rightX - values.ball.ballRadius;
        if (game.isBreaking()) {
            xLimit = game.getTable().breakLineX();
        }

        List<double[]> posList = new ArrayList<>();
        double xTick = (values.table.innerWidth - values.ball.ballDiameter) / smallSep / 2;
        double yTick = (values.table.innerHeight - values.ball.ballDiameter) / smallSep;
        for (double x = values.table.leftX + values.ball.ballRadius; x <= xLimit; x += xTick) {
            for (double y = values.table.topY + values.ball.ballRadius; y < values.table.botY; y += yTick) {
                if (game.canPlaceWhite(x, y)) {
                    posList.add(new double[]{x, y});
                }
            }
        }
        return posList;
    }

    @Override
    protected double[] breakPosition() {
        return new double[]{game.getTable().breakLineX(),
                game.getGameValues().table.botY - game.getGameValues().ball.ballDiameter * 1.5};
    }
}
