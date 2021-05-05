package trashsoftware.trashSnooker.core;

import javafx.scene.paint.Color;

import java.util.Objects;

public class Ball implements Comparable<Ball> {
    private final int value;
    private final Color color;
    private final SnookerGame game;
    double x, y;
    double nextX, nextY;
    double vx, vy;  // unit: mm/(sec/frameRate)
    double xSpin, ySpin;
    double sideSpin;
    private boolean potted;
    private boolean fmx, fmy;
    private long msSinceCue;
    private Ball justHit;

    Ball(int value, boolean initPotted, SnookerGame game) {
        this.value = value;
        this.potted = initPotted;
        this.color = generateColor(value);
        this.game = game;
    }

    Ball(int value, double[] xy, SnookerGame game) {
        this(value, false, game);

        setX(xy[0]);
        setY(xy[1]);
    }

    Ball(int value, SnookerGame game) {
        this(value, true, game);
    }

    public static Color generateColor(int value) {
        switch (value) {
            case 0:
                return Values.WHITE;
            case 1:
                return Values.RED;
            case 2:
                return Values.YELLOW;
            case 3:
                return Values.GREEN;
            case 4:
                return Values.BROWN;
            case 5:
                return Values.BLUE;
            case 6:
                return Values.PINK;
            case 7:
                return Values.BLACK;
            default:
                throw new RuntimeException("Unexpected ball.");
        }
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public boolean isPotted() {
        return potted;
    }

    public void pickup() {
        potted = false;
        vx = 0.0;
        vy = 0.0;
        sideSpin = 0.0;
        xSpin = 0.0;
        ySpin = 0.0;
    }

    public boolean isRed() {
        return value == 1;
    }

    public boolean isColored() {
        return value > 1 && value <= 7;
    }

    public boolean isWhite() {
        return value == 0;
    }

    public void setVx(double vx) {
        this.vx = vx;
    }

    public void setVy(double vy) {
        this.vy = vy;
    }

    public void setSpin(double xSpin, double ySpin, double sideSpin) {
        msSinceCue = 0;
        fmy = false;
        fmx = false;
        this.xSpin = xSpin;
        this.ySpin = ySpin;
        this.sideSpin = sideSpin;
    }

    public boolean isLikelyStopped() {
        if (getSpeed() < SnookerGame.speedReducer && getSpinTargetSpeed() < SnookerGame.spinReducer) {
            vx = 0.0;
            vy = 0.0;
            sideSpin = 0.0;
            xSpin = 0.0;
            ySpin = 0.0;
            return true;
        }
        return false;
    }

    private double getSpeed() {
        return Math.hypot(vx, vy);
    }

    private double getSpinTargetSpeed() {
        return Math.hypot(xSpin, ySpin);
    }

//    /**
//     * @return this ball is still moving
//     */
//    boolean move() {
//        if (isPotted()) return false;
//        if (!isLikelyStopped()) {
//            if (willPot()) {
//                pot();
//                return true;
//            }
//            if (tryHitHoleArea()) {
//                // 袋口区域
//                tryHitOtherBalls();
//                return true;
//            }
//            if (tryHitWall()) {
//                // 库边
//                return true;
//            }
//
//            boolean noHit = !tryHitOtherBalls();
//            if (noHit) normalMove();
////            normalMove();
//
//            return true;
//        }
//        return false;
//    }

//    private boolean tryHitOtherBalls() {
//        boolean result = false;
//        for (Ball ball : game.getAllBalls()) {
//            if (ball != this && !ball.potted) {
//                if (tryHitBall(ball)) {
//                    ball.move();
//                    result = true;
//                }
//            }
//        }
//        return result;
//    }

    void normalMove() {
        x = nextX;
        y = nextY;
        msSinceCue++;
//        if (isWhite()) System.out.printf("%f %f %f %f\n", vx, vy, xSpin, ySpin);
        if (sideSpin >= SnookerGame.sideSpinReducer) {
            sideSpin -= SnookerGame.sideSpinReducer;
        } else if (sideSpin <= -SnookerGame.sideSpinReducer) {
            sideSpin += SnookerGame.sideSpinReducer;
        }
//        else {
//            if (isWhite()) System.out.println("Side spin end in " + msSince);
//        }

        double speed = getSpeed();
        double reducedSpeed = speed - SnookerGame.speedReducer;
        double ratio = reducedSpeed / speed;
        vx *= ratio;
        vy *= ratio;

        double xSpinDiff = xSpin - vx;
        double ySpinDiff = ySpin - vy;

        double spinDiffTotal = Math.hypot(xSpinDiff, ySpinDiff);
        double spinRatio = SnookerGame.spinReducer / spinDiffTotal;
        double xSpinReducer = Math.abs(xSpinDiff * spinRatio);
        double ySpinReducer = Math.abs(ySpinDiff * spinRatio);

//        if (isWhite()) System.out.printf("vx: %f, vy: %f, xr: %f, yr: %f, spin: %f\n", vx, vy, xSpinReducer, ySpinReducer, SnookerGame.spinReducer);

//        double spinEffect = 3000.0;  // 越小影响越大

        if (xSpinDiff < -xSpinReducer) {
            vx += xSpinDiff / SnookerGame.spinEffect;
            xSpin += xSpinReducer;
        } else if (xSpinDiff >= xSpinReducer) {
            vx += xSpinDiff / SnookerGame.spinEffect;
            xSpin -= xSpinReducer;
        } else {
            xSpin = vx;
//            if (!fmx) {
//                fmx = true;
//                if (isWhite()) System.out.println("X matched in " + msSince);
//            }
//            if (isWhite()) System.out.println("X Matched!");
        }

        if (ySpinDiff < -ySpinReducer) {
            vy += ySpinDiff / SnookerGame.spinEffect;
            ySpin += ySpinReducer;
        } else if (ySpinDiff >= ySpinReducer) {
            vy += ySpinDiff / SnookerGame.spinEffect;
            ySpin -= ySpinReducer;
        } else {
            ySpin = vy;
//            if (isWhite()) System.out.println("Y Matched!");
//            if (!fmy) {
//                fmy = true;
//                if (isWhite()) System.out.println("Y matched in " + msSince);
//            }
        }
    }

    void pot() {
        potted = true;
        x = 0.0;
        y = 0.0;
        clearMovement();
    }

    /**
     * 众所周知，中袋大力容易打不进
     *
     * @return (0.5, 1.0)之间的一个值
     */
    private double midHolePowerFactor() {
        return 1.0 - (getSpeed() * SnookerGame.calculationsPerSec / Values.MAX_POWER_SPEED) * 0.5;
    }

    boolean willPot() {
        double midHoleFactor = midHolePowerFactor();
        return predictedDtToPoint(Values.TOP_LEFT_HOLE_XY) < Values.CORNER_HOLE_RADIUS ||
                predictedDtToPoint(Values.BOT_LEFT_HOLE_XY) < Values.CORNER_HOLE_RADIUS ||
                predictedDtToPoint(Values.TOP_RIGHT_HOLE_XY) < Values.CORNER_HOLE_RADIUS ||
                predictedDtToPoint(Values.BOT_RIGHT_HOLE_XY) < Values.CORNER_HOLE_RADIUS ||
                predictedDtToPoint(Values.TOP_MID_HOLE_XY) < Values.MID_HOLE_RADIUS * midHoleFactor ||
                predictedDtToPoint(Values.BOT_MID_HOLE_XY) < Values.MID_HOLE_RADIUS * midHoleFactor;
    }

    /**
     * 检测是否撞击袋角或进入袋角区域。如果撞击袋角，返回{@code true}且处理撞击。如果进入袋角区域但未发生撞击，同样返回{@code true}
     */
    boolean tryHitHoleArea() {
        boolean enteredCorner = false;
        if (nextY < Values.BALL_RADIUS + Values.TOP_Y) {
            if (nextX < Values.MID_HOLE_AREA_RIGHT_X && nextX >= Values.MID_HOLE_AREA_LEFT_X) {
                // 上方中袋在袋角范围内
                if (predictedDtToPoint(Values.TOP_MID_HOLE_LEFT_ARC_XY) < Values.MID_ARC_RADIUS + Values.BALL_RADIUS &&
                        currentDtToPoint(Values.TOP_MID_HOLE_LEFT_ARC_XY) >= Values.MID_ARC_RADIUS + Values.BALL_RADIUS) {
                    // 击中上方中袋左侧
                    hitHoleArcArea(Values.TOP_MID_HOLE_LEFT_ARC_XY);
                } else if (predictedDtToPoint(Values.TOP_MID_HOLE_RIGHT_ARC_XY) < Values.MID_ARC_RADIUS + Values.BALL_RADIUS &&
                        currentDtToPoint(Values.TOP_MID_HOLE_RIGHT_ARC_XY) >= Values.MID_ARC_RADIUS + Values.BALL_RADIUS) {
                    // 击中上方中袋右侧
                    hitHoleArcArea(Values.TOP_MID_HOLE_RIGHT_ARC_XY);
                } else {
                    normalMove();
                    prepareMove();
                }
                return true;
            }
        } else if (nextY >= Values.BOT_Y - Values.BALL_RADIUS) {
            if (nextX < Values.MID_HOLE_AREA_RIGHT_X && nextX >= Values.MID_HOLE_AREA_LEFT_X) {
                // 下方中袋袋角范围内
                if (predictedDtToPoint(Values.BOT_MID_HOLE_LEFT_ARC_XY) < Values.MID_ARC_RADIUS + Values.BALL_RADIUS &&
                        currentDtToPoint(Values.BOT_MID_HOLE_LEFT_ARC_XY) >= Values.MID_ARC_RADIUS + Values.BALL_RADIUS) {
                    // 击中下方中袋左侧
                    hitHoleArcArea(Values.BOT_MID_HOLE_LEFT_ARC_XY);
                } else if (predictedDtToPoint(Values.BOT_MID_HOLE_RIGHT_ARC_XY) < Values.MID_ARC_RADIUS + Values.BALL_RADIUS &&
                        currentDtToPoint(Values.BOT_MID_HOLE_RIGHT_ARC_XY) >= Values.MID_ARC_RADIUS + Values.BALL_RADIUS) {
                    // 击中下方中袋右侧
                    hitHoleArcArea(Values.BOT_MID_HOLE_RIGHT_ARC_XY);
                } else {
                    normalMove();
                    prepareMove();
                }
                return true;
            }
        }
        if (nextY < Values.TOP_CORNER_HOLE_AREA_DOWN_Y) {
            if (nextX < Values.LEFT_CORNER_HOLE_AREA_RIGHT_X) enteredCorner = true;  // 左上底袋
            else if (nextX >= Values.RIGHT_CORNER_HOLE_AREA_LEFT_X) enteredCorner = true;  // 右上底袋
        } else if (nextY >= Values.BOT_CORNER_HOLE_AREA_UP_Y) {
            if (nextX < Values.LEFT_CORNER_HOLE_AREA_RIGHT_X) enteredCorner = true;  // 左下底袋
            else if (nextX >= Values.RIGHT_CORNER_HOLE_AREA_LEFT_X) enteredCorner = true;  // 右下底袋
        }

        if (enteredCorner) {
            for (int i = 0; i < Values.ALL_CORNER_LINES.length; ++i) {
                double[][] line = Values.ALL_CORNER_LINES[i];
                double[] normVec = i < 4 ? Values.NORMAL_315 : Values.NORMAL_45;
                if (predictedDtToLine(line) < Values.BALL_RADIUS && currentDtToLine(line) >= Values.BALL_RADIUS) {
                    hitCornerHoleLineArea(normVec);
                    return true;
                }
            }
            for (double[] cornerArc : Values.ALL_CORNER_ARCS) {
                if (predictedDtToPoint(cornerArc) < Values.CORNER_ARC_RADIUS + Values.BALL_RADIUS &&
                        currentDtToPoint(cornerArc) >= Values.CORNER_ARC_RADIUS + Values.BALL_RADIUS) {
                    hitHoleArcArea(cornerArc);
                    return true;
                }
            }
            normalMove();
            prepareMove();
            return true;
        }
        return false;
    }

    private void hitHoleArcArea(double[] arcXY) {
        double axisX = arcXY[0] - x;  // 切线的法向量
        double axisY = arcXY[1] - y;
        double[] reflect = Algebra.symmetricVector(vx, vy, axisX, axisY);
        vx = -reflect[0] * Values.WALL_BOUNCE_RATIO;
        vy = -reflect[1] * Values.WALL_BOUNCE_RATIO;
        xSpin *= Values.WALL_SPIN_PRESERVE_RATIO;
        ySpin *= Values.WALL_SPIN_PRESERVE_RATIO;
        sideSpin *= Values.WALL_SPIN_PRESERVE_RATIO;
    }

    private void hitCornerHoleLineArea(double[] lineNormalVec) {
        double[] reflect = Algebra.symmetricVector(vx, vy, lineNormalVec[0], lineNormalVec[1]);
        vx = -reflect[0] * Values.WALL_BOUNCE_RATIO;
        vy = -reflect[1] * Values.WALL_BOUNCE_RATIO;
        xSpin *= (Values.WALL_SPIN_PRESERVE_RATIO * 0.75);
        ySpin *= (Values.WALL_SPIN_PRESERVE_RATIO * 0.75);
        sideSpin *= Values.WALL_SPIN_PRESERVE_RATIO;
    }

    /**
     * 该方法不检测袋口
     */
    boolean tryHitWall() {
        if (nextX < Values.BALL_RADIUS + Values.LEFT_X ||
                nextX >= Values.RIGHT_X - Values.BALL_RADIUS) {
            // 顶库
            vx = -vx * Values.WALL_BOUNCE_RATIO;
            vy *= Values.WALL_BOUNCE_RATIO;
            if (nextX < Values.BALL_RADIUS + Values.LEFT_X) {
                vy -= sideSpin;
            } else {
                vy += sideSpin;
            }
            xSpin *= Values.WALL_SPIN_PRESERVE_RATIO;
            ySpin *= (Values.WALL_SPIN_PRESERVE_RATIO * 0.5);
            sideSpin *= Values.WALL_SPIN_PRESERVE_RATIO;
            return true;
        }
        if (nextY < Values.BALL_RADIUS + Values.TOP_Y ||
                nextY >= Values.BOT_Y - Values.BALL_RADIUS) {
            // 边库
            vx *= Values.WALL_BOUNCE_RATIO;
            vy = -vy * Values.WALL_BOUNCE_RATIO;
            if (nextY < Values.BALL_RADIUS + Values.TOP_Y) {
                vx += sideSpin;
            } else {
                vx -= sideSpin;
            }
            xSpin *= (Values.WALL_SPIN_PRESERVE_RATIO * 0.5);
            ySpin *= Values.WALL_SPIN_PRESERVE_RATIO;
            sideSpin *= Values.WALL_SPIN_PRESERVE_RATIO;
//            System.out.println("Hit wall!======================");
            return true;
        }
        return false;
    }

    boolean tryHitBall(Ball ball) {
        double dt = predictedDtTo(ball);
        if (dt < Values.BALL_DIAMETER
                && currentDtTo(ball) > dt
                && justHit != ball && ball.justHit != this) {

            if (this.vx == 0.0 && this.vy == 0.0) {
                if (ball.vx == 0.0 && ball.vy == 0.0) {
                    throw new RuntimeException("他妈的两颗静止的球拿头撞？");
                } else {
                    return ball.tryHitBall(this);
                }
            }
            if (ball.vx == 0.0 && ball.vy == 0.0) {
                System.out.println("Hit static ball!=====================");
                double xPos = x;
                double yPos = y;
                double dx = vx / Values.DETAILED_PHYSICAL;
                double dy = vy / Values.DETAILED_PHYSICAL;

                for (int i = 0; i < Values.DETAILED_PHYSICAL; ++i) {
                    if (Algebra.distanceToPoint(xPos + dx, yPos + dy, ball.x, ball.y) < Values.BALL_DIAMETER) {
                        break;
                    }
                    xPos += dx;
                    yPos += dy;
                }

                double ang = (xPos - ball.x) / (yPos - ball.y);

                double ballVY = (ang * this.vx + this.vy) / (ang * ang + 1);
                double ballVX = ang * ballVY;
                ball.vy = ballVY * Values.BALL_BOUNCE_RATIO;
                ball.vx = ballVX * Values.BALL_BOUNCE_RATIO;

                this.vx = (this.vx - ballVX) * Values.BALL_BOUNCE_RATIO;
                this.vy = (this.vy - ballVY) * Values.BALL_BOUNCE_RATIO;
            } else {
                System.out.println("Hit moving ball!=====================");

                double[] thisV = new double[]{vx, vy};
                double[] ballV = new double[]{ball.vx, ball.vy};

                double[] normVec = new double[]{this.x - ball.x, this.y - ball.y};  // 两球连线=法线
                double[] tangentVec = Algebra.normalVector(normVec);  // 切线

                double thisVerV = Algebra.projectionLengthOn(normVec, thisV);  // 垂直于切线的速率
                double thisHorV = Algebra.projectionLengthOn(tangentVec, thisV);  // 平行于切线的速率
                double ballVerV = Algebra.projectionLengthOn(normVec, ballV);
                double ballHorV = Algebra.projectionLengthOn(tangentVec, ballV);
//            System.out.printf("(%f, %f), (%f, %f)\n", thisHorV, thisVerV, ballHorV, ballVerV);
                System.out.print("Ball 1 " + this + " ");

                // 碰撞后，两球平行于切线的速率不变，垂直于切线的速率互换
                double[] thisOut = Algebra.antiProjection(tangentVec, new double[]{thisHorV, ballVerV});
                System.out.print("Ball 2 " + ball + " ");
                double[] ballOut = Algebra.antiProjection(tangentVec, new double[]{ballHorV, thisVerV});

                this.vx = thisOut[0] * Values.BALL_BOUNCE_RATIO;
                this.vy = thisOut[1] * Values.BALL_BOUNCE_RATIO;
                ball.vx = ballOut[0] * Values.BALL_BOUNCE_RATIO;
                ball.vy = ballOut[1] * Values.BALL_BOUNCE_RATIO;
            }

            justHit = ball;
            ball.justHit = this;
            return true;
        }
        return false;
    }

    void clearMovement() {
        vx = 0.0;
        vy = 0.0;
        xSpin = 0.0;
        ySpin = 0.0;
        sideSpin = 0.0;
        justHit = null;
    }

    void prepareMove() {
        nextX = x + vx;
        nextY = y + vy;
        justHit = null;
    }

    private double currentDtTo(Ball ball) {
        return Algebra.distanceToPoint(x, y, ball.x, ball.y);
    }

    private double predictedDtTo(Ball ball) {
        return Algebra.distanceToPoint(nextX, nextY, ball.nextX, ball.nextY);
    }

    private double currentDtToLine(double[][] line) {
        return Algebra.distanceToLine(x, y, line[0], line[1]);
    }

    private double predictedDtToLine(double[][] line) {
        return Algebra.distanceToLine(nextX, nextY, line[0], line[1]);
    }

    double currentDtToPoint(double[] point) {
        return Algebra.distanceToPoint(x, y, point[0], point[1]);
    }

    private double predictedDtToPoint(double[] point) {
        return Algebra.distanceToPoint(nextX, nextY, point[0], point[1]);
    }

    public Color getColor() {
        return color;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("Ball{%d at (%f, %f)}", value, x, y);
    }

    @Override
    public int compareTo(Ball o) {
        return Integer.compare(value, o.value);
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
