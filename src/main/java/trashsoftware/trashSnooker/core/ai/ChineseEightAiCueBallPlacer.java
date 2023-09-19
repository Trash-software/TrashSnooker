package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallPlayer;

import java.util.*;
import java.util.stream.Collectors;

public class ChineseEightAiCueBallPlacer extends 
        TargetOrientedCueBallPlacer<ChineseEightBallGame, ChineseEightBallPlayer> {
    
    public ChineseEightAiCueBallPlacer(ChineseEightBallGame game, ChineseEightBallPlayer player) {
        super(game, player);
    }

    @Override
    protected ALLOWED_POS currentAllowedPos() {
        return game.isBreaking() || game.isJustAfterBreak() ? ALLOWED_POS.BEFORE_LINE : ALLOWED_POS.FULL_TABLE;
    }

    @Override
    protected List<Ball> targetsSortedByPrivilege() {
        if (player.getBallRange() == 8) return List.of(game.getBallByValue(8));
        
        boolean inLineHandBall = currentAllowedPos() == ALLOWED_POS.BEFORE_LINE;
        boolean hardFirst;
        
        List<Ball> targets;
        if (player.getBallRange() == ChineseEightBallGame.NOT_SELECTED_REP) {
            // 选球不考虑是不是线后
            List<Ball> fullBalls = game.getAllLegalBalls(ChineseEightBallGame.FULL_BALL_REP, 
                    false, 
                    false);
            List<Ball> halfBalls = game.getAllLegalBalls(ChineseEightBallGame.HALF_BALL_REP, 
                    false, 
                    false);
            double fullPrice = ChineseEightAiCue.avgPriceOfSet(game, fullBalls);
            double halfPrice = ChineseEightAiCue.avgPriceOfSet(game, halfBalls);
            
            // 但是摆球得考虑是不是线后
            targets = fullPrice > halfPrice ?
                    game.getAllLegalBalls(ChineseEightBallGame.FULL_BALL_REP, false, inLineHandBall) :
                    game.getAllLegalBalls(ChineseEightBallGame.HALF_BALL_REP, false, inLineHandBall);
            hardFirst = false;
        } else if (player.getBallRange() == ChineseEightBallGame.FULL_BALL_REP || player.getBallRange() == ChineseEightBallGame.HALF_BALL_REP) {
            targets = game.getAllLegalBalls(player.getBallRange(), false, inLineHandBall);
            hardFirst = true;
        } else {
            throw new RuntimeException("Unexpected target rep: " + player.getBallRange());
        }
        
        Map<Ball, Double> priceMap = targets.stream()
                .collect(Collectors.toMap(ball -> ball, ball -> ChineseEightAiCue.ballAlivePrice(game, ball)));
        targets.removeIf(ball -> priceMap.get(ball) == 0);  // 完全死球就算了
        targets.sort(Comparator.comparingDouble(priceMap::get));  // price低的在前
        if (!hardFirst) {
            Collections.reverse(targets);
        }
        
        return targets;
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
    
    protected List<double[]> legalPositions(double smallSep) {
        GameValues values = game.getGameValues();
        double xLimit = values.table.rightX - values.ball.ballRadius;
        if (game.isBreaking() || game.isJustAfterBreak()) {
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
}
