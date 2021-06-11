package trashsoftware.trashSnooker.core;

import javafx.scene.paint.Color;

public abstract class Ball implements Comparable<Ball> {
    private final int value;
    private final Color color;
    private final GameValues values;
    protected double x, y;
    protected double nextX, nextY;
    protected double vx, vy;  // unit: mm/(sec/frameRate)
    protected double xSpin, ySpin;
    protected double sideSpin;
    private boolean potted;
    private boolean fmx, fmy;
    private long msSinceCue;
    private Ball justHit;

    protected Ball(int value, boolean initPotted, GameValues values) {
        this.value = value;
        this.potted = initPotted;
        this.values = values;
        this.color = generateColor(value);
    }

    protected Ball(int value, double[] xy, GameValues values) {
        this(value, false, values);

        setX(xy[0]);
        setY(xy[1]);
    }

    protected Ball(int value, GameValues values) {
        this(value, true, values);
    }

    protected abstract Color generateColor(int value);

    public static Color snookerColor(int value) {
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

    public static Color poolBallBaseColor(int number) {
        switch (number) {
            case 0:
                return Values.WHITE;
            case 1:
            case 9:
            case 16:
            case 17:
                return Values.YELLOW;
            case 2:
            case 10:
                return Values.BLUE;
            case 3:
            case 11:
                return Values.RED;
            case 4:
            case 12:
                return Values.PURPLE;
            case 5:
            case 13:
                return Values.ORANGE;
            case 6:
            case 14:
                return Values.GREEN;
            case 7:
            case 15:
                return Values.DARK_RED;
            case 8:
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
        if (getSpeed() < Game.speedReducer && getSpinTargetSpeed() < Game.spinReducer) {
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

    void normalMove() {
        x = nextX;
        y = nextY;
        msSinceCue++;
//        if (isWhite()) System.out.printf("%f %f %f %f\n", vx, vy, xSpin, ySpin);
        if (sideSpin >= Game.sideSpinReducer) {
            sideSpin -= Game.sideSpinReducer;
        } else if (sideSpin <= -Game.sideSpinReducer) {
            sideSpin += Game.sideSpinReducer;
        }
//        else {
//            if (isWhite()) System.out.println("Side spin end in " + msSince);
//        }

        double speed = getSpeed();
        double reducedSpeed = speed - Game.speedReducer;
        double ratio = reducedSpeed / speed;
        vx *= ratio;
        vy *= ratio;

        double xSpinDiff = xSpin - vx;
        double ySpinDiff = ySpin - vy;

        double spinDiffTotal = Math.hypot(xSpinDiff, ySpinDiff);
        double spinRatio = Game.spinReducer / spinDiffTotal;
        double xSpinReducer = Math.abs(xSpinDiff * spinRatio);  // todo
        double ySpinReducer = Math.abs(ySpinDiff * spinRatio);

//        if (isWhite()) System.out.printf("vx: %f, vy: %f, xr: %f, yr: %f, spin: %f\n", vx, vy, xSpinReducer, ySpinReducer, SnookerGame.spinReducer);

//        double spinEffect = 3000.0;  // 越小影响越大

        if (xSpinDiff < -xSpinReducer) {
            vx += xSpinDiff / Game.spinEffect;
            xSpin += xSpinReducer;
        } else if (xSpinDiff >= xSpinReducer) {
            vx += xSpinDiff / Game.spinEffect;
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
            vy += ySpinDiff / Game.spinEffect;
            ySpin += ySpinReducer;
        } else if (ySpinDiff >= ySpinReducer) {
            vy += ySpinDiff / Game.spinEffect;
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

    public void pot() {
        potted = true;
        x = 0.0;
        y = 0.0;
        clearMovement();
    }

    /**
     * 众所周知，中袋大力容易打不进
     *
     * @return (0.6, 1.2)之间的一个值
     */
    private double midHolePowerFactor() {
        return 1.2 - (getSpeed() * Game.calculationsPerSec / Values.MAX_POWER_SPEED) * 0.6;
    }

    boolean willPot() {
        double midHoleFactor = midHolePowerFactor();
        return predictedDtToPoint(values.topLeftHoleXY) < values.cornerHoleRadius ||
                predictedDtToPoint(values.botLeftHoleXY) < values.cornerHoleRadius ||
                predictedDtToPoint(values.topRightHoleXY) < values.cornerHoleRadius ||
                predictedDtToPoint(values.botRightHoleXY) < values.cornerHoleRadius ||
                predictedDtToPoint(values.topMidHoleXY) < values.midHoleRadius * midHoleFactor ||
                predictedDtToPoint(values.botMidHoleXY) < values.midHoleRadius * midHoleFactor;
    }

    /**
     * 检测是否撞击袋角或进入袋角区域。如果撞击袋角，返回{@code 2}且处理撞击。如果进入袋角区域但未发生撞击，返回{@code 1}。如未进入，返回{@code 0}
     */
    int tryHitHoleArea() {
        boolean enteredCorner = false;
        if (nextY < values.ballRadius + values.topY) {
            if (nextX < values.midHoleAreaRightX && nextX >= values.midHoleAreaLeftX) {
                // 上方中袋在袋角范围内
                if (predictedDtToPoint(values.topMidHoleLeftArcXy) < values.midArcRadius + values.ballRadius &&
                        currentDtToPoint(values.topMidHoleLeftArcXy) >= values.midArcRadius + values.ballRadius) {
                    // 击中上方中袋左侧
                    hitHoleArcArea(values.topMidHoleLeftArcXy);
                } else if (predictedDtToPoint(values.topMidHoleRightArcXy) < values.midArcRadius + values.ballRadius &&
                        currentDtToPoint(values.topMidHoleRightArcXy) >= values.midArcRadius + values.ballRadius) {
                    // 击中上方中袋右侧
                    hitHoleArcArea(values.topMidHoleRightArcXy);
                } else {
                    normalMove();
                    prepareMove();
                    return 1;
                }
                return 2;
            }
        } else if (nextY >= values.botY - values.ballRadius) {
            if (nextX < values.midHoleAreaRightX && nextX >= values.midHoleAreaLeftX) {
                // 下方中袋袋角范围内
                if (predictedDtToPoint(values.botMidHoleLeftArcXy) < values.midArcRadius + values.ballRadius &&
                        currentDtToPoint(values.botMidHoleLeftArcXy) >= values.midArcRadius + values.ballRadius) {
                    // 击中下方中袋左侧
                    hitHoleArcArea(values.botMidHoleLeftArcXy);
                } else if (predictedDtToPoint(values.botMidHoleRightArcXy) < values.midArcRadius + values.ballRadius &&
                        currentDtToPoint(values.botMidHoleRightArcXy) >= values.midArcRadius + values.ballRadius) {
                    // 击中下方中袋右侧
                    hitHoleArcArea(values.botMidHoleRightArcXy);
                } else {
                    normalMove();
                    prepareMove();
                    return 1;
                }
                return 2;
            }
        }
        if (nextY < values.topCornerHoleAreaDownY) {
            if (nextX < values.leftCornerHoleAreaRightX) enteredCorner = true;  // 左上底袋
            else if (nextX >= values.rightCornerHoleAreaLeftX) enteredCorner = true;  // 右上底袋
        } else if (nextY >= values.botCornerHoleAreaUpY) {
            if (nextX < values.leftCornerHoleAreaRightX) enteredCorner = true;  // 左下底袋
            else if (nextX >= values.rightCornerHoleAreaLeftX) enteredCorner = true;  // 右下底袋
        }

        if (enteredCorner) {
            for (int i = 0; i < values.allCornerLines.length; ++i) {
                double[][] line = values.allCornerLines[i];
                double[] normVec = i < 4 ? Values.NORMAL_315 : Values.NORMAL_45;
                if (predictedDtToLine(line) < values.ballRadius && currentDtToLine(line) >= values.ballRadius) {
                    hitCornerHoleLineArea(normVec);
                    return 2;
                }
            }
            for (double[] cornerArc : values.allCornerArcs) {
                if (predictedDtToPoint(cornerArc) < values.cornerArcRadius + values.ballRadius &&
                        currentDtToPoint(cornerArc) >= values.cornerArcRadius + values.ballRadius) {
                    hitHoleArcArea(cornerArc);
                    return 2;
                }
            }
            normalMove();
            prepareMove();
            return 1;
        }
        return 0;
    }

    private void hitHoleArcArea(double[] arcXY) {
        double axisX = arcXY[0] - x;  // 切线的法向量
        double axisY = arcXY[1] - y;
        double[] reflect = Algebra.symmetricVector(vx, vy, axisX, axisY);
        vx = -reflect[0] * Values.WALL_BOUNCE_RATIO;
        vy = -reflect[1] * Values.WALL_BOUNCE_RATIO;
        xSpin *= (Values.WALL_SPIN_PRESERVE_RATIO * 0.8);
        ySpin *= (Values.WALL_SPIN_PRESERVE_RATIO * 0.8);
        sideSpin *= Values.WALL_SPIN_PRESERVE_RATIO;
    }

    private void hitCornerHoleLineArea(double[] lineNormalVec) {
        double[] reflect = Algebra.symmetricVector(vx, vy, lineNormalVec[0], lineNormalVec[1]);
        vx = -reflect[0] * Values.WALL_BOUNCE_RATIO;
        vy = -reflect[1] * Values.WALL_BOUNCE_RATIO;
        xSpin *= (Values.WALL_SPIN_PRESERVE_RATIO * 0.8);
        ySpin *= (Values.WALL_SPIN_PRESERVE_RATIO * 0.8);
        sideSpin *= Values.WALL_SPIN_PRESERVE_RATIO;
    }

    /**
     * 该方法不检测袋口
     */
    boolean tryHitWall() {
        if (nextX < values.ballRadius + values.leftX ||
                nextX >= values.rightX - values.ballRadius) {
            // 顶库
            vx = -vx * Values.WALL_BOUNCE_RATIO;
            vy *= Values.WALL_BOUNCE_RATIO;
            if (nextX < values.ballRadius + values.leftX) {
                vy -= sideSpin;
            } else {
                vy += sideSpin;
            }
            xSpin *= (Values.WALL_SPIN_PRESERVE_RATIO * 0.7);
            ySpin *= Values.WALL_SPIN_PRESERVE_RATIO;
            sideSpin *= Values.WALL_SPIN_PRESERVE_RATIO;
            return true;
        }
        if (nextY < values.ballRadius + values.topY ||
                nextY >= values.botY - values.ballRadius) {
            // 边库
            vx *= Values.WALL_BOUNCE_RATIO;
            vy = -vy * Values.WALL_BOUNCE_RATIO;
            if (nextY < values.ballRadius + values.topY) {
                vx += sideSpin;
            } else {
                vx -= sideSpin;
            }
            xSpin *= Values.WALL_SPIN_PRESERVE_RATIO;
            ySpin *= (Values.WALL_SPIN_PRESERVE_RATIO * 0.7);
            sideSpin *= Values.WALL_SPIN_PRESERVE_RATIO;
//            System.out.println("Hit wall!======================");
            return true;
        }
        return false;
    }

    boolean tryHitBall(Ball ball) {
        double dt = predictedDtTo(ball);
        if (dt < values.ballDiameter
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
                    if (Algebra.distanceToPoint(xPos + dx, yPos + dy, ball.x, ball.y) < values.ballDiameter) {
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
