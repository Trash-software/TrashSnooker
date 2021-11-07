package trashsoftware.trashSnooker.core;

import javafx.scene.paint.Color;

public abstract class Ball extends ObjectOnTable implements Comparable<Ball> {
    private final int value;
    private final Color color;
    protected double xSpin, ySpin;
    protected double sideSpin;
    private boolean potted;
    private long msSinceCue;
    private Ball justHit;

    protected Ball(int value, boolean initPotted, GameValues values) {
        super(values, values.ballRadius);
        
        this.potted = initPotted;
        this.value = value;
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

    protected abstract Color generateColor(int value);

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

    public void setSpin(double xSpin, double ySpin, double sideSpin) {
        msSinceCue = 0;
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

    private double getSpinTargetSpeed() {
        return Math.hypot(xSpin, ySpin);
    }

    protected void normalMove() {
        x = nextX;
        y = nextY;
        msSinceCue++;
        if (sideSpin >= Game.sideSpinReducer) {
            sideSpin -= Game.sideSpinReducer;
        } else if (sideSpin <= -Game.sideSpinReducer) {
            sideSpin += Game.sideSpinReducer;
        }

        double speed = getSpeed();
        double reducedSpeed = speed - Game.speedReducer;
        double ratio = reducedSpeed / speed;
        vx *= ratio;
        vy *= ratio;

        double xSpinDiff = xSpin - vx;
        double ySpinDiff = ySpin - vy;

        // A linear reduce
        double spinDiffTotal = Math.hypot(xSpinDiff, ySpinDiff);
        double spinReduceRatio = Game.spinReducer / spinDiffTotal;
        double xSpinReducer = Math.abs(xSpinDiff * spinReduceRatio);
        double ySpinReducer = Math.abs(ySpinDiff * spinReduceRatio);

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
        }

        if (ySpinDiff < -ySpinReducer) {
            vy += ySpinDiff / Game.spinEffect;
            ySpin += ySpinReducer;
        } else if (ySpinDiff >= ySpinReducer) {
            vy += ySpinDiff / Game.spinEffect;
            ySpin -= ySpinReducer;
        } else {
            ySpin = vy;
        }
    }

    public void pot() {
        potted = true;
        x = 0.0;
        y = 0.0;
        clearMovement();
    }

    private boolean isNotMoving() {
        return vx == 0.0 && vy == 0.0;
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

    protected void hitHoleArcArea(double[] arcXY) {
        super.hitHoleArcArea(arcXY);
        vx *= Values.WALL_BOUNCE_RATIO;
        vy *= Values.WALL_BOUNCE_RATIO;
        xSpin *= (Values.WALL_SPIN_PRESERVE_RATIO * 0.8);
        ySpin *= (Values.WALL_SPIN_PRESERVE_RATIO * 0.8);
        sideSpin *= Values.WALL_SPIN_PRESERVE_RATIO;
    }

    protected void hitHoleLineArea(double[] lineNormalVec) {
        super.hitHoleLineArea(lineNormalVec);
        vx *= Values.WALL_BOUNCE_RATIO;
        vy *= Values.WALL_BOUNCE_RATIO;
        xSpin *= (Values.WALL_SPIN_PRESERVE_RATIO * 0.8);
        ySpin *= (Values.WALL_SPIN_PRESERVE_RATIO * 0.8);
        sideSpin *= Values.WALL_SPIN_PRESERVE_RATIO;
    }

    /**
     * 该方法不检测袋口
     */
    protected boolean tryHitWall() {
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

    boolean tryHitTwoBalls(Ball ball1, Ball ball2) {
        if (this.isNotMoving()) {
            if (ball1.isNotMoving()) {
                if (ball2.isNotMoving()) {
                    return false;  // 三颗球都没动
                } else {
                    return ball2.tryHitTwoBalls(this, ball1);
                }
            } else {
                if (ball2.isNotMoving()) {
                    return ball1.tryHitTwoBalls(this, ball2);
                } else {
                    return false;  // ball1、ball2 都在动，无法处理
                }
            }
        } else {
            if (ball1.isNotMoving() && ball2.isNotMoving()) {
                // this 去撞另外两颗
                double dt1, dt2, dt12;
                if (((dt1 = predictedDtTo(ball1)) < values.ballDiameter && currentDtTo(ball1) > dt1 &&
                        justHit != ball1 && ball1.justHit != this) &&
                        ((dt2 = predictedDtTo(ball2)) < values.ballDiameter && currentDtTo(ball2) > dt2 &&
                                justHit != ball2 && ball2.justHit != this)) {
                    System.out.println("Hit two static balls!=====================");
                    double xPos = x;
                    double yPos = y;
                    double dx = vx / Values.DETAILED_PHYSICAL;
                    double dy = vy / Values.DETAILED_PHYSICAL;

                    boolean ball1First = true;

                    for (int i = 0; i < Values.DETAILED_PHYSICAL; ++i) {
                        if (Algebra.distanceToPoint(xPos + dx, yPos + dy, ball1.x, ball1.y) < values.ballDiameter) {
                            break;
                        }
                        if (Algebra.distanceToPoint(xPos + dx, yPos + dy, ball2.x, ball2.y) < values.ballDiameter) {
                            ball1First = false;
                            break;
                        }
                        xPos += dx;
                        yPos += dy;
                    }

                    if (ball1First) {
                        tryHitBall(ball1, false);
                        tryHitBall(ball2, false);
                    } else {
                        tryHitBall(ball2, false);
                        tryHitBall(ball1, false);
                    }

                    return true;
                } else {
                    return false;  // 这三颗球没有贴在一起
                }
            } else {
                return false;  // this 和 ball1、ball2 中的至少一颗都在动，无法处理
            }
        }
    }

    boolean tryHitBall(Ball ball) {
        return tryHitBall(ball, true);
    }

    void hitStaticBallCore(Ball ball) {
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
    }

    boolean tryHitBall(Ball ball, boolean checkMovingBall) {
        double dt = predictedDtTo(ball);
        if (dt < values.ballDiameter
                && currentDtTo(ball) > dt
                && justHit != ball && ball.justHit != this) {

            if (this.isNotMoving()) {
                if (ball.isNotMoving()) {
                    if (checkMovingBall) {
                        throw new RuntimeException("他妈的两颗静止的球拿头撞？");
                    } else {
                        System.err.println("复杂情况，不管了，有bug就有bug吧");
                        return false;
                    }
                } else {
                    return ball.tryHitBall(this);
                }
            }
            if (ball.isNotMoving()) {
//                if (checkMovingBall) System.out.println("Hit static ball!=====================");
                hitStaticBallCore(ball);
            } else {
                if (!checkMovingBall) return false;
//                System.out.println("Hit moving ball!=====================");

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

    protected void prepareMove() {
        super.prepareMove();
        justHit = null;
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
