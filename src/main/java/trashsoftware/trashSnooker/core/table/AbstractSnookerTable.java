package trashsoftware.trashSnooker.core.table;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.ArcType;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.Game;
import trashsoftware.trashSnooker.core.GameValues;
import trashsoftware.trashSnooker.fxml.GameView;

public abstract class AbstractSnookerTable extends Table {
    
    public final double[][] pointsRankHighToLow = new double[6][];
    
    public AbstractSnookerTable(GameValues gameValues) {
        super(gameValues);

        pointsRankHighToLow[0] = blackBallPos();
        pointsRankHighToLow[1] = pinkBallPos();
        pointsRankHighToLow[2] = blueBallPos();
        pointsRankHighToLow[3] = brownBallPos();
        pointsRankHighToLow[4] = greenBallPos();
        pointsRankHighToLow[5] = yellowBallPos();
    }

    @Override
    public void drawTableMarks(GameView view, GraphicsContext graphicsContext, double scale) {
        // 开球线
        double breakLineX = view.canvasX(breakLineX());
        graphicsContext.setStroke(GameView.WHITE);
        graphicsContext.strokeLine(
                breakLineX,
                view.canvasY(gameValues.topY),
                breakLineX,
                view.canvasY(gameValues.topY + gameValues.innerHeight));

        // 开球半圆
        double breakArcRadius = breakArcRadius() * scale;
        graphicsContext.strokeArc(
                breakLineX - breakArcRadius,
                view.canvasY(gameValues.midY) - breakArcRadius,
                breakArcRadius * 2,
                breakArcRadius * 2,
                90.0,
                180.0,
                ArcType.OPEN);

        // 置球点
        drawBallPoints(view, graphicsContext);
    }

    private void drawBallPoints(GameView view, GraphicsContext graphicsContext) {
        graphicsContext.setFill(GameView.WHITE);
        double pointRadius = 2.0;
        double pointDiameter = pointRadius * 2;
        for (double[] xy : pointsRankHighToLow) {
            graphicsContext.fillOval(view.canvasX(xy[0]) - pointRadius,
                    view.canvasY(xy[1]) - pointRadius,
                    pointDiameter,
                    pointDiameter);
        }
    }

    @Override
    public void forceDrawBall(GameView view,
                              Ball ball,
                              double absoluteX,
                              double absoluteY,
                              GraphicsContext graphicsContext,
                              double scale) {
        drawBallBase(
                view.canvasX(absoluteX),
                view.canvasY(absoluteY),
                gameValues.ballDiameter * scale,
                ball.getColor(),
                graphicsContext);
    }
    
    public abstract double breakLineX();
    
    public abstract double breakArcRadius();

    public double firstRedX() {
        return pinkBallPos()[0] + gameValues.ballDiameter + Game.MIN_GAP_DISTANCE;  // 粉球与红球堆空隙
    }

    public double[] yellowBallPos() {
        return new double[]{breakLineX(), gameValues.midY + breakArcRadius()};
    }

    public double[] greenBallPos() {
        return new double[]{breakLineX(), gameValues.midY - breakArcRadius()};
    }

    public double[] brownBallPos() {
        return new double[]{breakLineX(), gameValues.midY};
    }

    public double[] blueBallPos() {
        return new double[]{gameValues.midX, gameValues.midY};
    }

    public double[] pinkBallPos() {
        return new double[]{(blackBallPos()[0] + blueBallPos()[0]) / 2, gameValues.midY};
    }

    public abstract double[] blackBallPos();
}
