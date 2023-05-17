package trashsoftware.trashSnooker.core.table;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;
import trashsoftware.trashSnooker.core.Values;
import trashsoftware.trashSnooker.fxml.GameView;

import java.util.Arrays;
import java.util.List;

public abstract class NumberedBallTable extends Table {

    protected NumberedBallTable(TableMetrics tableMetrics) {
        super(tableMetrics);
    }

    @Override
    public void drawTableMarks(GameView view, GraphicsContext graphicsContext, double scale) {
        // 开球线
        double breakLineX = GameView.canvasX(breakLineX());
        graphicsContext.setStroke(GameView.WHITE);
        graphicsContext.strokeLine(
                breakLineX,
                GameView.canvasY(tableMetrics.topY),
                breakLineX,
                GameView.canvasY(tableMetrics.topY + tableMetrics.innerHeight));

        // 颗星标记
        List<double[]> stars = tableMetrics.tableStars;
        graphicsContext.setFill(GameView.WHITE);

        for (double[] star : stars) {
            graphicsContext.fillOval(
                    GameView.canvasX(star[0]) - 2,
                    GameView.canvasY(star[1]) - 2,
                    5,
                    5);
        }
    }

    public static void drawPoolBallEssential(
            double canvasX,
            double canvasY,
            double canvasBallDiameter,
            Color baseColor,
            int ballNumber,
            GraphicsContext graphicsContext) {

        drawBallBase(canvasX, canvasY, canvasBallDiameter, baseColor, graphicsContext,
                ballNumber >= 9 && ballNumber != 16);

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
        graphicsContext.setFill(GameView.BLACK);
        graphicsContext.fillText(
                String.valueOf(ballNumber),
                canvasX,
                canvasY + textDown);
    }

    public double firstBallPlacementX() {
        return tableMetrics.leftX + (tableMetrics.innerWidth * 0.75);
    }

    public double breakLineX() {
        return tableMetrics.leftX + 635.0;
    }

//    @Override
//    public void forceDrawBall(GameView view,
//                              Ball ball,
//                              double absoluteX,
//                              double absoluteY,
//                              GraphicsContext graphicsContext,
//                              double scale) {
//        drawPoolBallEssential(
//                view.canvasX(absoluteX),
//                view.canvasY(absoluteY),
//                gameValues.ballDiameter * scale,
//                ball.getColor(),
//                ball.getValue(),
//                graphicsContext);
//    }
}
