package trashsoftware.trashSnooker.core.table;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;
import trashsoftware.trashSnooker.core.Values;
import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.fxml.widgets.GamePane;

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
        drawBallBase(canvasX, canvasY, ballCanvasDiameter, color, Values.BALL_CONTOUR, graphicsContext, false);
    }

    protected static void drawBallBase(
            double canvasX,
            double canvasY,
            double canvasBallDiameter,
            Color color,
            Color contourColor,
            GraphicsContext graphicsContext,
            boolean drawWhiteBorder) {
        double ballRadius = canvasBallDiameter / 2;
        graphicsContext.setStroke(contourColor);
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

    public abstract void drawTableMarks(GamePane view, GraphicsContext graphicsContext, double scale);

    public abstract int nBalls();

    /**
     * 在指定的位置强行绘制一颗球，无论球是否已经落袋
     */
    public void forceDrawBall(GamePane container,
                              Ball ball,
                              double absoluteX,
                              double absoluteY,
                              double xAxis,
                              double yAxis,
                              double zAxis,
                              double frameDegChange) {
        ball.model.setX(container, absoluteX);
        ball.model.setY(container, absoluteY);
        if (frameDegChange != 0) {
            // 不转的球就别来浪费计算资源了
            // 旋转矩阵挺贵的
            ball.model.rotateBy(
                    xAxis,
                    yAxis,
                    zAxis,
                    frameDegChange
            );
        }
    }

    public void forceDrawBallInHand(GamePane container, 
                                    Ball ball,
                                    double realX,
                                    double realY) {
        ball.model.sphere.setVisible(true);
        forceDrawBall(container, ball, realX, realY, 0, 0, 0, 0);
    }

    public void drawStoppedBalls(GamePane container,
                                 Ball[] allBalls,
                                 HashMap<Ball, double[]> positionsPot) {
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
            boolean hide = pot || (x == 0.0 && y == 0.0);  // todo: 没起作用
            ball.model.sphere.setVisible(!hide);
            if (!hide) {
                forceDrawBall(container, ball, x, y, ax, ay, az, degChange);
            }
        }
    }

    public abstract double breakLineX();
}
