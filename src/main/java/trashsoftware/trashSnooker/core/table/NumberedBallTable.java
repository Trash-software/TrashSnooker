package trashsoftware.trashSnooker.core.table;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.GameValues;
import trashsoftware.trashSnooker.core.Values;
import trashsoftware.trashSnooker.fxml.GameView;

public abstract class NumberedBallTable extends Table {

    protected NumberedBallTable(GameValues gameValues) {
        super(gameValues);
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

    public double breakPointX() {
        return gameValues.leftX + (gameValues.innerWidth * 0.75);
    }

    public double breakLineX() {
        return gameValues.leftX + 635.0;
    }

    @Override
    public void forceDrawBall(GameView view,
                              Ball ball,
                              double absoluteX,
                              double absoluteY,
                              GraphicsContext graphicsContext,
                              double scale) {
        drawPoolBallEssential(
                view.canvasX(absoluteX),
                view.canvasY(absoluteY),
                gameValues.ballDiameter * scale,
                ball.getColor(),
                ball.getValue(),
                graphicsContext);
    }
}
