package trashsoftware.trashSnooker.core.table;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;
import trashsoftware.trashSnooker.core.Values;
import trashsoftware.trashSnooker.fxml.GameView;

import java.util.HashMap;

/**
 * 重要！！！
 * 
 * 这个Table和真实的table尺寸布局这些没什么关系，只是一个游戏的载体
 */
public abstract class Table {

    protected TableMetrics tableMetrics;

    protected Table(TableMetrics tableMetrics) {
        this.tableMetrics = tableMetrics;
    }

    public static void drawBallBase(double canvasX,
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

    public abstract void drawTableMarks(GameView view, GraphicsContext graphicsContext, double scale);

    public abstract int nBalls();

    /**
     * 在指定的位置强行绘制一颗球，无论球是否已经落袋
     */
    public void forceDrawBall(GameView view,
                              Ball ball,
                              double absoluteX,
                              double absoluteY,
                              double xAxis,
                              double yAxis,
                              double zAxis,
                              double frameDegChange,
                              GraphicsContext graphicsContext,
                              double scale) {
        ball.model.setX(absoluteX);
        ball.model.setY(absoluteY);
        ball.model.rotateBy(
                xAxis,
                yAxis,
                zAxis,
                frameDegChange
        );
//        ball.model.rx.setAngle(Math.toDegrees(xAngle) / 10);
//        ball.model.ry.setAngle(Math.toDegrees(yAngle) / 10);
//        ball.model.rz.setAngle(Math.toDegrees(zAngle) / 10);
    }

    public void forceDrawBallInHand(GameView view,
                                    Ball ball,
                                    double realX,
                                    double realY,
                                    GraphicsContext graphicsContext,
                                    double scale) {
        ball.model.sphere.setVisible(true);
        forceDrawBall(view, ball, realX, realY, 0, 0, 0, 0, graphicsContext, scale);
//        drawBallBase(
//                view.canvasX(realX),
//                view.canvasY(realY),
//                gameValues.ballDiameter * scale,
//                ball.getColor(),
//                graphicsContext);
    }

    public void drawStoppedBalls(GameView view,
                                 Ball[] allBalls,
                                 HashMap<Ball, double[]> positionsPot,
                                 GraphicsContext graphicsContext,
                                 double scale) {
        for (Ball ball : allBalls) {
            boolean pot;
            double x, y, ax, ay, az, degChange;
            if (positionsPot == null) {
                pot = ball.isPotted();
                x = ball.getX();
                y = ball.getY();
                ax = ball.getAxisX();
                ay = ball.getAxisY();
                az = ball.getAxisZ();
                degChange = ball.getFrameDegChange();
            } else {
                double[] xyp = positionsPot.get(ball);
                pot = xyp[6] == 1;
                x = xyp[0];
                y = xyp[1];
                ax = xyp[2];
                ay = xyp[3];
                az = xyp[4];
                degChange = xyp[5];
            }
//            System.out.println(pot + " " + x + ", " + y + ", " + ball.model.sphere.getMaterial() + " ");
            ball.model.sphere.setVisible(!pot);
            if (!pot) {
                forceDrawBall(view, ball, x, y, ax, ay, az, degChange, graphicsContext, scale);
            }
        }
    }
}
