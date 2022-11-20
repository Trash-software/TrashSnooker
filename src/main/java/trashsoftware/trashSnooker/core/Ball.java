package trashsoftware.trashSnooker.core;

import javafx.geometry.Point3D;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.fxml.ballDrawing.BallModel;

import java.util.Arrays;
import java.util.Random;

public abstract class Ball extends ObjectOnTable implements Comparable<Ball> {
    private static final Random ERROR_GENERATOR = new Random();
    private static final Random randomGenerator = new Random();
    private static final double[] SIDE_CUSHION_VEC = {1.0, 0.0};
    private static final double[] TOP_BOT_CUSHION_VEC = {0.0, 1.0};
    public final BallModel model;
    protected final int value;
    private final Color color;
    protected double xSpin, ySpin;
    protected double sideSpin;
    //    protected double axisX, axisY, axisZ, rotateDeg;
    protected Rotate rotation = new Rotate();
    protected double xAngle, yAngle, zAngle;
    private boolean potted;
    private long msSinceCue;
    private Ball justHit;
    private double currentXError;
    private double currentYError;

    protected Ball(int value, boolean initPotted, GameValues values) {
        super(values, values.ballRadius);

        this.value = value;
        this.color = generateColor(value);

//        this.axisX = randomGenerator.nextDouble();
//        this.axisY = randomGenerator.nextDouble();
//        this.axisZ = randomGenerator.nextDouble();
//        this.rotateDeg = randomGenerator.nextDouble() * 360.0 * 1000;

        model = BallModel.createModel(this);
        setPotted(initPotted);
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

    public static double midHolePowerFactor(double speed) {
        return 1.2 - (speed / Values.MAX_POWER_SPEED) * 0.6;
    }

    protected abstract Color generateColor(int value);

    public boolean isPotted() {
        return potted;
    }

    public void setPotted(boolean potted) {
        this.potted = potted;
    }

    public void pickup() {
        setPotted(false);
        vx = 0.0;
        vy = 0.0;
        sideSpin = 0.0;
        xSpin = 0.0;
        ySpin = 0.0;
        distance = 0.0;
    }

    public double getXAngle() {
        return xAngle;
    }

    public double getYAngle() {
        return yAngle;
    }

    public double getZAngle() {
        return zAngle;
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

    public boolean isLikelyStopped(Phy phy) {
        if (getSpeed() < phy.speedReducer &&
                getSpinTargetSpeed() < phy.spinReducer) {
            vx = 0.0;
            vy = 0.0;
            if (phy.isPrediction) {
                sideSpin = 0.0;
            }
            xSpin = 0.0;
            ySpin = 0.0;
            return true;
        }
        return false;
    }

    private double getSpinTargetSpeed() {
        return Math.hypot(xSpin, ySpin);
    }

    public void calculateAxis(Phy phy) {
        double axisX = ySpin;
        double axisY = -xSpin;
        double ss = sideSpin * 8.0;
//        if (isWhite()) System.out.println(xSpin + " " + ySpin + " " + sideSpin);
        if (Double.isNaN(ss)) ss = 0.0;
        double axisZ = -ss;
        double degChange =
                Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ) * phy.calculationsPerSec;

        rotation.setAxis(new Point3D(axisX, axisY, axisZ));
        rotation.setAngle(degChange);

        double theta = -Math.asin(rotation.getMzx());
        double cosTheta = Math.cos(theta);
        double psi = Math.atan2(rotation.getMzy() / cosTheta, rotation.getMzz() / cosTheta);
        double phi = Math.atan2(rotation.getMyx() / cosTheta, rotation.getMxx() / cosTheta);

        xAngle += psi;
        yAngle += theta;
        zAngle += phi;
    }

    protected boolean sideSpinStopped(Phy phy) {
        msSinceCue++;
        if (sideSpin >= phy.sideSpinReducer) {
            sideSpin -= phy.sideSpinReducer;
        } else if (sideSpin <= -phy.sideSpinReducer) {
            sideSpin += phy.sideSpinReducer;
        } else {
            return true;
        }
        return false;
    }

    protected void normalMove(Phy phy) {
        distance += Math.hypot(vx, vy);
        setX(nextX);
        setY(nextY);

//        if (!phy.isPrediction) calculateAxis(phy);

        msSinceCue++;
        if (sideSpin >= phy.sideSpinReducer) {
            sideSpin -= phy.sideSpinReducer;
        } else if (sideSpin <= -phy.sideSpinReducer) {
            sideSpin += phy.sideSpinReducer;
        }

        double speed = getSpeed();
        double reducedSpeed = speed - values.speedReducerPerInterval(phy);
        double ratio = reducedSpeed / speed;
        vx *= ratio;
        vy *= ratio;

        if (!phy.isPrediction) {
            // 这部分是台泥造成的线路偏差
            double xErr = ERROR_GENERATOR.nextGaussian() * phy.cloth.goodness.errorFactor / phy.calculationsPerSec / 1.2 +
                    phy.cloth.goodness.fixedErrorFactor / phy.calculationsPerSec / 180;
            double yErr = ERROR_GENERATOR.nextGaussian() * phy.cloth.goodness.errorFactor / phy.calculationsPerSec / 1.2;
            vx += xErr;
            vy += yErr;
        }

        double xSpinDiff = xSpin - vx;
        double ySpinDiff = ySpin - vy;

        // 对于滑的桌子，转速差越大，旋转reducer和effect反而越小
        double spinDiffTotal = Math.hypot(xSpinDiff, ySpinDiff);
        double spinDiffFactor = spinDiffTotal / Values.MAX_SPIN_DIFF * phy.calculationsPerSec;
        spinDiffFactor = Math.min(1.0, spinDiffFactor);
        double dynamicDragFactor = 1 - phy.cloth.smoothness.tailSpeedFactor * spinDiffFactor;

//        if (isWhite() && !phy.isPrediction) System.out.println(dynamicDragFactor);

        // 乘和除抵了，所以第一部分是线性的
        double spinReduceRatio = phy.spinReducer / spinDiffTotal * dynamicDragFactor;
        double xSpinReducer = Math.abs(xSpinDiff * spinReduceRatio);
        double ySpinReducer = Math.abs(ySpinDiff * spinReduceRatio);

//        if (isWhite()) System.out.printf("vx: %f, vy: %f, xr: %f, yr: %f, spin: %f\n", vx, vy, xSpinReducer, ySpinReducer, SnookerGame.spinReducer);

        if (xSpinDiff < -xSpinReducer) {
            vx += xSpinDiff / phy.spinEffect * dynamicDragFactor;
            xSpin += xSpinReducer * dynamicDragFactor;
        } else if (xSpinDiff >= xSpinReducer) {
            vx += xSpinDiff / phy.spinEffect * dynamicDragFactor;
            xSpin -= xSpinReducer * dynamicDragFactor;
        } else {
            xSpin = vx;
        }

        if (ySpinDiff < -ySpinReducer) {
            vy += ySpinDiff / phy.spinEffect * dynamicDragFactor;
            ySpin += ySpinReducer * dynamicDragFactor;
        } else if (ySpinDiff >= ySpinReducer) {
            vy += ySpinDiff / phy.spinEffect * dynamicDragFactor;
            ySpin -= ySpinReducer * dynamicDragFactor;
        } else {
            ySpin = vy;
        }
    }

    public void pot() {
        setPotted(true);
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
    protected double midHolePowerFactor(Phy phy) {
        return midHolePowerFactor(getSpeed() * phy.calculationsPerSec);
    }

    protected double[] hitHoleArcArea(double[] arcXY, Phy phy) {
        double[] normalVec = super.hitHoleArcArea(arcXY, phy);  // 碰撞点切线的法向量
        // 一般来说袋角的弹性没库边好
        vx *= values.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor * 0.9;
        vy *= values.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor * 0.9;
        applySpinsWhenHitCushion(phy, normalVec);
        
        return normalVec;
    }

    protected void hitHoleLineArea(double[] lineNormalVec, Phy phy) {
        super.hitHoleLineArea(lineNormalVec, phy);
        vx *= values.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor * 0.95;
        vy *= values.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor * 0.95;
        applySpinsWhenHitCushion(phy, lineNormalVec);
    }

    /**
     * 碰库时的旋转:
     * 1. 施加侧旋的效果
     * 2. 更新所有旋转效果
     * 
     * 需在更新了vx和vy之后调用
     *
     * @param phy 物理
     * @param cushionNormalVec 撞的库的法向量。比如说边库应是(1,0)或(-1,0)
     */
    private void applySpinsWhenHitCushion(Phy phy, double[] cushionNormalVec) {
        // 库的切线方向的投影长度，意思是砸的深度，越深效果越强
        double mag = Math.abs(Algebra.projectionLengthOn(cushionNormalVec, new double[]{vx, vy})) *
                phy.calculationsPerSec;
        
        double sideSpinEffectMul = Math.pow(mag / Values.MAX_POWER_SPEED, 0.62);
//        System.out.println(mag + " " + sideSpinEffectMul);
        double effectiveSideSpin = sideSpin * sideSpinEffectMul;
        
        // todo: 真的算，而不是用if。目前没考虑袋角
        if (Arrays.equals(cushionNormalVec, SIDE_CUSHION_VEC)) {
            if (vx < 0) {
                vy += effectiveSideSpin;
            } else {
                vy -= effectiveSideSpin;
            }
            xSpin *= (values.wallSpinPreserveRatio * 0.8);
            ySpin *= values.wallSpinPreserveRatio;
        } else if (Arrays.equals(cushionNormalVec, TOP_BOT_CUSHION_VEC)) {
            if (vy < 0) {
                vx -= effectiveSideSpin;
            } else {
                vx += effectiveSideSpin;
            }
            xSpin *= values.wallSpinPreserveRatio;
            ySpin *= (values.wallSpinPreserveRatio * 0.8);
        } else {
            xSpin *= (values.wallSpinPreserveRatio * 0.9);
            ySpin *= (values.wallSpinPreserveRatio * 0.9);
        }

        sideSpin *= values.wallSpinPreserveRatio * sideSpinEffectMul;
    }

    /**
     * 该方法不检测袋口
     */
    protected boolean tryHitWall(Phy phy) {
        if (nextX < values.ballRadius + values.leftX ||
                nextX >= values.rightX - values.ballRadius) {
            // 顶库
            vx = -vx * values.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor;
            vy *= values.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor;
            
            applySpinsWhenHitCushion(phy, SIDE_CUSHION_VEC);
//            rotateDeg = 0.0;
            return true;
        }
        if (nextY < values.ballRadius + values.topY ||
                nextY >= values.botY - values.ballRadius) {
            // 边库
            vx *= values.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor;
            vy = -vy * values.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor;
            
            applySpinsWhenHitCushion(phy, TOP_BOT_CUSHION_VEC);
//            System.out.println("Hit wall!======================");
//            rotateDeg = 0.0;
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

        ball.vy = ballVY * values.ballBounceRatio;
        ball.vx = ballVX * values.ballBounceRatio;

        this.vx = (this.vx - ballVX) * values.ballBounceRatio;
        this.vy = (this.vy - ballVY) * values.ballBounceRatio;

        nextX = x + vx;
        nextY = y + vy;
        ball.nextX = ball.x + ball.vx;
        ball.nextY = ball.y + ball.vy;
    }

    void twoMovingBallsHitCore(Ball ball) {
        // 提高精确度
        double x1 = x;
        double y1 = y;
        double dx1 = vx / Values.DETAILED_PHYSICAL;
        double dy1 = vy / Values.DETAILED_PHYSICAL;

        double x2 = ball.x;
        ;
        double y2 = ball.y;
        double dx2 = ball.vx / Values.DETAILED_PHYSICAL;
        double dy2 = ball.vy / Values.DETAILED_PHYSICAL;

        for (int i = 0; i < Values.DETAILED_PHYSICAL; ++i) {
            if (Algebra.distanceToPoint(x1, y1, x2, y2) < values.ballDiameter) {
                break;
            }
            x1 += dx1;
            y1 += dy1;
            x2 += dx2;
            y2 += dy2;
        }

        double[] thisV = new double[]{vx, vy};
        double[] ballV = new double[]{ball.vx, ball.vy};

        double[] normVec = new double[]{x1 - x2, y1 - y2};  // 两球连线=法线
        double[] tangentVec = Algebra.normalVector(normVec);  // 切线

        double thisVerV = Algebra.projectionLengthOn(normVec, thisV);  // 垂直于切线的速率
        double thisHorV = Algebra.projectionLengthOn(tangentVec, thisV);  // 平行于切线的速率
        double ballVerV = Algebra.projectionLengthOn(normVec, ballV);
        double ballHorV = Algebra.projectionLengthOn(tangentVec, ballV);
//        System.out.printf("(%f, %f), (%f, %f)\n", thisHorV, thisVerV, ballHorV, ballVerV);
//        System.out.print("Ball 1 " + this + " ");

        if (thisHorV == 0) thisHorV = 0.0000000001;
        if (thisVerV == 0) thisVerV = 0.0000000001;
        if (ballHorV == 0) ballHorV = 0.0000000001;
        if (ballVerV == 0) ballVerV = 0.0000000001;

        // 碰撞后，两球平行于切线的速率不变，垂直于切线的速率互换
        double[] thisOut = Algebra.antiProjection(tangentVec,
                new double[]{thisHorV, ballVerV});
//        System.out.println("Ball 1 out " + Arrays.toString(thisOut));
//        System.out.print("Ball 2 " + ball + " ");
        double[] ballOut = Algebra.antiProjection(tangentVec,
                new double[]{ballHorV, thisVerV});
//        System.out.println("Ball 2 out " + Arrays.toString(ballOut));

        this.vx = thisOut[0] * values.ballBounceRatio;
        this.vy = thisOut[1] * values.ballBounceRatio;
        ball.vx = ballOut[0] * values.ballBounceRatio;
        ball.vy = ballOut[1] * values.ballBounceRatio;

        nextX = x + vx;
        nextY = y + vy;
        ball.nextX = ball.x + ball.vx;
        ball.nextY = ball.y + ball.vy;
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
//                if (checkMovingBall) System.out.println("Hit static ball!=====================");4
                twoMovingBallsHitCore(ball);
//                hitStaticBallCore(ball);
            } else {
                if (!checkMovingBall) return false;
//                System.out.println("Hit moving ball!=====================");

                twoMovingBallsHitCore(ball);
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
        distance = 0.0;
        justHit = null;
    }

    protected void prepareMove(Phy phy) {
        super.prepareMove(phy);
        justHit = null;
        currentXError = phy.cloth.goodness.fixedErrorFactor * phy.calculationsPerSec;
        currentYError = 0.0;
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

    /**
     * @return ball实例的原生hashcode。注意不要用value来生成hashcode，因为斯诺克有15颗value一样的红球
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
