package trashsoftware.trashSnooker.core.table;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.ArcType;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;
import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.fxml.widgets.GamePane;

public class RussianTable extends Table {
    public RussianTable(TableMetrics tableMetrics) {
        super(tableMetrics);
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

        // 置球点
        drawBallPoints(view, graphicsContext);
    }

    private void drawBallPoints(GamePane view, GraphicsContext graphicsContext) {
        graphicsContext.setFill(GameView.WHITE);
        double pointRadius = 2.0;
        double pointDiameter = pointRadius * 2;
        
        double[] forePoint = new double[]{breakLineX(), tableMetrics.midY};
        double[] backPoint = new double[]{rackFirstBallX(null), tableMetrics.midY};

        for (double[] xy : new double[][]{forePoint, backPoint}) {
            graphicsContext.fillOval(view.canvasX(xy[0]) - pointRadius,
                    view.canvasY(xy[1]) - pointRadius,
                    pointDiameter,
                    pointDiameter);
        }
        
    }

    @Override
    public double breakLineX() {
        return tableMetrics.leftX + tableMetrics.innerWidth * 0.25;
    }

    @Override
    public double rackFirstBallX(GameValues gameValues) {
        return tableMetrics.leftX + (tableMetrics.innerWidth * 0.75);
    }
}
