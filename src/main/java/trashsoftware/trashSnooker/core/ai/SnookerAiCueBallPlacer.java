package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.GameValues;
import trashsoftware.trashSnooker.core.TableMetrics;
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
        double yOff;
        switch (player.getPlayerPerson().getAiPlayStyle().snookerBreakMethod) {
            case LEFT:
                yOff = -105.0;
                break;
            case RIGHT:
                yOff = 105.0;
                break;
            case BACK:
                yOff = 230.0;
                break;
            default:
                throw new EnumConstantNotPresentException(AiPlayStyle.SnookerBreakMethod.class, "Java编译器是不是脑壳出问题了");
        }
        return new double[]{game.getTable().breakLineX(), 
                game.getGameValues().table.midY + yOff};
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
                double[] xy = Algebra.angleToUnitVector(theta + Math.PI / 2);
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
