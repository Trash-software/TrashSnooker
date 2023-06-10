package trashsoftware.trashSnooker.core.table;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.core.Values;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;
import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.fxml.widgets.GamePane;
import trashsoftware.trashSnooker.util.OpacityColor;

import java.util.List;

public abstract class NumberedBallTable extends Table {

    protected NumberedBallTable(TableMetrics tableMetrics) {
        super(tableMetrics);
    }

    public static void drawPoolBallEssential(
            double canvasX,
            double canvasY,
            double canvasBallDiameter,
            Color baseColor,
            int ballNumber,
            GraphicsContext graphicsContext) {
        drawPoolBallEssential(
                canvasX, canvasY, canvasBallDiameter, baseColor, ballNumber, graphicsContext, false);
    }

    public static void drawPoolBallEssential(
            double canvasX,
            double canvasY,
            double canvasBallDiameter,
            Color baseColor,
            int ballNumber,
            GraphicsContext graphicsContext,
            boolean greyOut) {

        double opacity = greyOut ? 0.12 : 1.0;
        Color drawCol = OpacityColor.getInstance().getByOpacity(baseColor, opacity);
        Color contourCol = OpacityColor.getInstance().getByOpacity(Values.BALL_CONTOUR, opacity);
        drawBallBase(canvasX, canvasY, canvasBallDiameter, drawCol, contourCol,
                graphicsContext, ballNumber >= 9 && ballNumber != 16);

        if (ballNumber == 0) return;  // 母球
        if (ballNumber > 15) return;  // 仅用于表示目标球

        // 号码区域
        double textAreaRadius = canvasBallDiameter * 0.25;
        graphicsContext.setFill(Values.WHITE);
        graphicsContext.fillOval(
                canvasX - textAreaRadius,
                canvasY - textAreaRadius,
                textAreaRadius * 2,
                textAreaRadius * 2);

        // 号码
        graphicsContext.setFont(GameView.POOL_NUMBER_FONT);
        double textDown = GameView.POOL_NUMBER_FONT.getSize() * 0.36;
        Color textFill = OpacityColor.getInstance().getByOpacity(GameView.BLACK, opacity);
        graphicsContext.setFill(textFill);
        graphicsContext.fillText(
                String.valueOf(ballNumber),
                canvasX,
                canvasY + textDown);
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
        
        // 开球点位和置球点位
        double pointYGap = tableMetrics.innerHeight / 4;
        double[][] points = new double[][]{
                {breakLineX(), tableMetrics.topY + pointYGap},
                {breakLineX(), tableMetrics.topY + pointYGap * 2},
                {breakLineX(), tableMetrics.topY + pointYGap * 3},
                {firstBallPlacementX(), tableMetrics.midY}
        };
        
        graphicsContext.setFill(GameView.WHITE);
        double radius1 = 3.0 * scale;
        for (double[] point : points) {
            graphicsContext.fillOval(
                    view.canvasX(point[0]) - radius1,
                    view.canvasY(point[1]) - radius1,
                    radius1 * 2, 
                    radius1 * 2);
        }

        // 颗星标记
        List<double[]> stars = tableMetrics.tableStars;
        graphicsContext.setFill(GameView.WHITE);

        double radius2 = 12.0 * scale;
        for (double[] star : stars) {
            graphicsContext.fillOval(
                    view.canvasX(star[0]) - radius2,
                    view.canvasY(star[1]) - radius2,
                    radius2,
                    radius2);
        }
    }

    public double firstBallPlacementX() {
        return tableMetrics.leftX + (tableMetrics.innerWidth * 0.75);
    }

    public double breakLineX() {
        return tableMetrics.leftX + (tableMetrics.innerWidth * 0.25);
    }
}
