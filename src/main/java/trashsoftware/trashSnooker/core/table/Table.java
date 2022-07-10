package trashsoftware.trashSnooker.core.table;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.GameValues;
import trashsoftware.trashSnooker.core.Values;
import trashsoftware.trashSnooker.fxml.GameView;

import java.util.HashMap;

public abstract class Table {
    
    protected GameValues gameValues;
    
    protected Table(GameValues gameValues) {
        this.gameValues = gameValues;
    }
    
    public abstract void drawTableMarks(GameView view, GraphicsContext graphicsContext, double scale);
    
    /**
     * 在指定的位置强行绘制一颗球，无论球是否已经落袋
     */
    public abstract void forceDrawBall(GameView view,
                                       Ball ball,
                                       double absoluteX,
                                       double absoluteY,
                                       GraphicsContext graphicsContext,
                                       double scale);

    public void forcedDrawWhiteBall(GameView view,
                                    double realX,
                                    double realY,
                                    GraphicsContext graphicsContext,
                                    double scale) {
        drawBallBase(
                view.canvasX(realX),
                view.canvasY(realY),
                gameValues.ballDiameter * scale,
                Values.WHITE,
                graphicsContext);
    }

    public void drawStoppedBalls(GameView view, 
                                 Ball[] allBalls,
                                 HashMap<Ball, double[]> positionsPot,
                                 GraphicsContext graphicsContext, 
                                 double scale) {
        for (Ball ball : allBalls) {
            boolean pot;
            double x, y;
            if (positionsPot == null) {
                pot = ball.isPotted();
                x = ball.getX();
                y = ball.getY();
            } else {
                double[] xyp = positionsPot.get(ball);
                pot = xyp[2] == 1;
                x = xyp[0];
                y = xyp[1];
            }
            if (!pot) {
                forceDrawBall(view, ball, x, y, graphicsContext, scale);
            }
        }
    }

    protected static void drawBallBase(double canvasX,
                                       double canvasY,
                                       double ballCanvasDiameter,
                                       Color color,
                                       GraphicsContext graphicsContext) {
        drawBallBase(canvasX, canvasY, ballCanvasDiameter, color, graphicsContext, false);
    }

    protected static void drawBallBase(
            double canvasX,
            double canvasY,
            double canvasBallDiameter,
            Color color,
            GraphicsContext graphicsContext,
            boolean drawWhiteBorder) {
        double ballRadius = canvasBallDiameter / 2;
        graphicsContext.setStroke(Values.BALL_CONTOUR);
        graphicsContext.strokeOval(
                canvasX - ballRadius,
                canvasY - ballRadius,
                canvasBallDiameter,
                canvasBallDiameter
        );

        graphicsContext.setFill(color);
        graphicsContext.fillOval(
                canvasX - ballRadius,
                canvasY - ballRadius,
                canvasBallDiameter,
                canvasBallDiameter);

        // 16球9-15号球顶端和底端的白带
        if (drawWhiteBorder) {
            double angle = 50.0;
            graphicsContext.setFill(Values.WHITE);
            graphicsContext.fillArc(
                    canvasX - ballRadius,
                    canvasY - ballRadius,
                    canvasBallDiameter,
                    canvasBallDiameter,
                    90 - angle,
                    angle * 2,
                    ArcType.CHORD
            );
            graphicsContext.fillArc(
                    canvasX - ballRadius,
                    canvasY - ballRadius,
                    canvasBallDiameter,
                    canvasBallDiameter,
                    270 - angle,
                    angle * 2,
                    ArcType.CHORD
            );
        }
    }
}
