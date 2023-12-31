package trashsoftware.trashSnooker.core.table;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.ArcType;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;
import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.fxml.widgets.GamePane;

public abstract class AbstractSnookerTable extends Table {
    
    public final double[][] pointsRankHighToLow = new double[6][];
    
    public AbstractSnookerTable(TableMetrics tableMetrics) {
        super(tableMetrics);

        pointsRankHighToLow[0] = blackBallPos();
        pointsRankHighToLow[1] = pinkBallPos();
        pointsRankHighToLow[2] = blueBallPos();
        pointsRankHighToLow[3] = brownBallPos();
        pointsRankHighToLow[4] = greenBallPos();
        pointsRankHighToLow[5] = yellowBallPos();
    }

    @Override
    public void drawTableMarks(GamePane view, GraphicsContext graphicsContext, double scale) {
        // 开球线
        double breakLineX = view.canvasX(breakLineX());
        graphicsContext.setStroke(GameView.WHITE);
        graphicsContext.strokeLine(
                breakLineX,
                view.canvasY(tableMetrics.topY),
                breakLineX,
                view.canvasY(tableMetrics.topY + tableMetrics.innerHeight));

        // 开球半圆
        double breakArcRadius = breakArcRadius() * scale;
        graphicsContext.strokeArc(
                breakLineX - breakArcRadius,
                view.canvasY(tableMetrics.midY) - breakArcRadius,
                breakArcRadius * 2,
                breakArcRadius * 2,
                90.0,
                180.0,
                ArcType.OPEN);

        // 置球点
        drawBallPoints(view, graphicsContext);
    }

    private void drawBallPoints(GamePane view, GraphicsContext graphicsContext) {
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
    public double breakLineX() {
        return tableMetrics.leftX + tableMetrics.innerWidth * 0.2065;
    }
    
    public double breakArcRadius() {
        return tableMetrics.tableName.equals(TableMetrics.SNOOKER) ? 292.0 : 219.0;
    }
    
    public double[] blackBallPos() {
        return new double[]{tableMetrics.rightX - tableMetrics.innerWidth * 0.09078, tableMetrics.midY};
    }

    public double[] yellowBallPos() {
        return new double[]{breakLineX(), tableMetrics.midY + breakArcRadius()};
    }

    public double[] greenBallPos() {
        return new double[]{breakLineX(), tableMetrics.midY - breakArcRadius()};
    }

    public double[] brownBallPos() {
        return new double[]{breakLineX(), tableMetrics.midY};
    }

    public double[] blueBallPos() {
        return new double[]{tableMetrics.midX, tableMetrics.midY};
    }

    public double[] pinkBallPos() {
        return new double[]{tableMetrics.leftX + tableMetrics.innerWidth * 0.75, tableMetrics.midY};
    }
}
