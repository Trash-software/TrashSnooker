package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.snooker.AbstractSnookerGame;
import trashsoftware.trashSnooker.core.snooker.SnookerPlayer;

import java.util.ArrayList;
import java.util.List;

public class SnookerAiCueBallPlacer extends AiCueBallPlacer<AbstractSnookerGame, SnookerPlayer> {
    public SnookerAiCueBallPlacer(AbstractSnookerGame game, SnookerPlayer player) {
        super(game, player);
    }

    @Override
    protected double[] breakPosition() {
        GameValues gameValues = game.getGameValues();
        double yPos = switch (player.getPlayerPerson().getAiPlayStyle().snookerBreakMethod) {
            case LEFT -> game.getTable().brownBallPos()[1] - gameValues.ball.ballDiameter * 1.25;
            case RIGHT -> game.getTable().brownBallPos()[1] + gameValues.ball.ballDiameter * 1.25;
            case BACK -> game.getTable().yellowBallPos()[1] - gameValues.ball.ballDiameter * 1.25;
        };
        return new double[]{game.getTable().breakLineX(), yPos};
    }

    @Override
    protected List<double[]> legalPositions() {
        double breakLineX = game.getTable().breakLineX();
        double breakArcRadius = game.getTable().breakArcRadius();
        GameValues values = game.getGameValues();
        double centerY = values.table.midY;

        List<double[]> posList = new ArrayList<>();
//        System.out.println("X: " + breakLineX + ", Y: " + centerY);
        for (double radius = 0.0; radius < breakArcRadius; radius += values.ball.ballRadius) {
            int thisRadiusCapacity = (int) (radius * Math.PI / values.ball.ballRadius) + 1;
            double eachRad = Math.PI / thisRadiusCapacity;
            for (int n = 0; n < thisRadiusCapacity; n++) {
                double theta = n * eachRad;
                double[] xy = Algebra.unitVectorOfAngle(theta + Math.PI / 2);
                xy[0] = breakLineX + xy[0] * radius;
                xy[1] = centerY - xy[1] * radius;
//                System.out.println(xy[0] + ", " + xy[1]);
                if (game.canPlaceWhite(xy[0], xy[1])) {
                    posList.add(xy);
                }
            }
        }
        return posList;
    }
}
