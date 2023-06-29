package trashsoftware.trashSnooker.core;

import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.metrics.Pocket;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.phy.TableCloth;
import trashsoftware.trashSnooker.fxml.drawing.BallModel;

import java.util.Arrays;
import java.util.Random;

public abstract class Ball extends ObjectOnTable implements Comparable<Ball>, Cloneable {
    public static final double MAX_GEAR_EFFECT = 0.25;  // 齿轮效应造成的最严重分离角损耗
    public static final double GEAR_EFFECT_MAX_POWER = 0.32;  // 大于这个球速就没有齿轮效应了
    public static final double CUSHION_COLLISION_SPIN_FACTOR = 0.5;
    public static final double CUSHION_DIRECT_SPIN_APPLY = 0.4;
    public static final double SUCK_CUSHION_FACTOR = 0.75;
    public static final double MAXIMUM_SPIN_PASS = 0.2;  // 齿轮效应传递旋转的上限
    private static final Random ERROR_GENERATOR = new Random();
    public static final double[] LEFT_CUSHION_VEC = {0.0, -1.0};  // 视觉上的顺时针方向。注意y是反的
    public static final double[] RIGHT_CUSHION_VEC = {0.0, 1.0};
    public static final double[] TOP_CUSHION_VEC = {1.0, 0.0};
    public static final double[] BOT_CUSHION_VEC = {-1.0, 0.0};
    private static final double[] LEFT_CUSHION_VEC_NORM = {1.0, 0.0};  // 都指向球桌内侧
    private static final double[] RIGHT_CUSHION_VEC_NORM = {-1.0, 0.0};
    private static final double[] TOP_CUSHION_VEC_NORM = {0.0, 1.0};
    private static final double[] BOT_CUSHION_VEC_NORM = {0.0, -1.0};
    private static boolean gearOffsetEnabled = true;  // 齿轮/投掷效应造成的球线路偏差
    private static int idCounter = 0;
    public final BallModel model;
    protected final int value;
    private final Color color;
    private final Color colorWithOpa;
    private final Color colorTransparent;
    private final int identifier;  // 即使是分值一样的球identifier也不一样，但是clone之后identifier保持不变
    protected double xSpin, ySpin;
    protected double sideSpin;
    protected double axisX, axisY, axisZ,
            frameDegChange;  // 全部都是针对一个动画帧
    private boolean potted;
    private long msSinceCue;
    private long msRemainInPocket;
    private Pocket pottedPocket;
//    private Ball justHit;
    int pocketHitCount = 0;  // 本杆撞击袋角的次数

    private double lastCollisionX, lastCollisionY;  // 记录一下上次碰撞所在的位置

    protected Ball(int value, boolean initPotted, GameValues values) {
        super(values, values.ball.ballRadius);

        identifier = idCounter++;

        this.value = value;
        this.color = generateColor(value);
        this.colorWithOpa = this.color.deriveColor(0, 1, 1.6, 0.5);
        this.colorTransparent = colorWithOpa.deriveColor(0, 1, 1, 0);

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

//    private static Paint makeGradientColor(Color ballColor) {
//        Stop[] stops = new Stop[]{
//                new Stop()
//        };
//        LinearGradient gradient = new LinearGradient()
//    }

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

    public static boolean isGearOffsetEnabled() {
        return gearOffsetEnabled;
    }

    public static void enableGearOffset() {
        gearOffsetEnabled = true;
    }

    public static void disableGearOffset() {
        gearOffsetEnabled = false;
    }

    static void threeBallHits(Ball ball1, Ball ball2, Ball ball3, GameValues values, Phy phy) {
        // 老子今天就非要研究出来到底谁先撞谁！
        // 算了，研究不出来

        double dia = values.ball.ballDiameter;

        double x1 = ball1.x, x2 = ball2.x, x3 = ball3.x;
        double y1 = ball1.y, y2 = ball2.y, y3 = ball3.y;

        double dt12, dt13, dt23;

        double tolerance = 0.05;

        while (
                (dt12 = Algebra.distanceToPoint(x1, y1, x2, y2)) > dia
                        || (dt13 = Algebra.distanceToPoint(x1, y1, x3, y3)) > dia
                        || (dt23 = Algebra.distanceToPoint(x2, y2, x3, y3)) > dia
        ) {
            x1 -= ball1.vx;
            x2 -= ball2.vx;
            x3 -= ball3.vx;
            y1 -= ball1.vy;
            y2 -= ball2.vy;
            y3 -= ball3.vy;
        }

        double relSpeed12 = Math.hypot(ball1.vx - ball2.vx, ball1.vy - ball2.vy);
        double relSpeed13 = Math.hypot(ball1.vx - ball3.vx, ball1.vy - ball3.vy);
        double relSpeed23 = Math.hypot(ball2.vx - ball3.vx, ball2.vy - ball3.vy);

        double time12 = dt12 / relSpeed12;
        double time13 = dt13 / relSpeed13;
        double time23 = dt23 / relSpeed23;

        if (time12 < time13) {
            if (time12 < time23) {
                // 1，2最先撞
                ball1.tryHitBall(ball2, true, false, phy);

            }
        }
    }

    @Override
    public Ball clone() {
        try {
            return (Ball) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract Color generateColor(int value);

    public boolean isPotted() {
        return potted;
    }

    public void setPotted(boolean potted) {
        this.potted = potted;
    }

    public boolean isOutOfTable() {
        return !potted && (x < 0 || x >= values.table.outerWidth ||
                y < 0 || y >= values.table.outerHeight);
    }

    public void pickup() {
        setPotted(false);
        clearMovement();
    }

    public boolean isRed() {
        return value == 1;
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

    public boolean isNotMoving(Phy phy) {
        return getSpeed() < phy.speedReducer;
    }

    public boolean isLikelyStopped(Phy phy) {
        if (getSpeed() < phy.speedReducer   // todo: 写不明白，旋转停
                &&
                getSpinTargetSpeed() < phy.spinReducer * 2
//                &&
//                (!phy.isPrediction && Math.abs(sideSpin) < phy.sideSpinReducer)
        ) {
            vx = 0.0;
            vy = 0.0;
//            if (phy.isPrediction) {
//                sideSpin = 0.0;  // todo: 同上
//            }
            xSpin = 0.0;
            ySpin = 0.0;
            return true;
        }
        return false;
    }

    private double getSpinTargetSpeed() {
        return Math.hypot(xSpin, ySpin);
    }

    public void calculateAxis(Phy phy, double animationFrameMs) {
        // 每个动画帧算一次，而不是物理帧
        axisX = ySpin;
        axisY = -xSpin;
        double ss = sideSpin * Math.PI * 1.2;  // 不要误会，没有这么神，只是3.14刚好看起来差不多
//        if (isWhite()) System.out.println(xSpin + " " + ySpin + " " + sideSpin);
        if (Double.isNaN(ss)) ss = 0.0;
        axisZ = -ss;

        frameDegChange =
                Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)
                        * phy.calculationsPerSec * animationFrameMs / 800.0;
    }

    /**
     * 原地转
     */
    protected boolean sideSpinAtPosition(Phy phy) {
        msSinceCue++;
        if (sideSpin >= phy.sideSpinReducer) {
            sideSpin -= phy.sideSpinReducer;
            return true;
        } else if (sideSpin <= -phy.sideSpinReducer) {
            sideSpin += phy.sideSpinReducer;
            return true;
        } else {
            return false;
        }
    }

    protected void transformPhy(Phy srcPhy, Phy dstPhy) {
        double vMul = dstPhy.calculateMs / srcPhy.calculateMs;
        double accMul = vMul * vMul;

        vx *= vMul;
        vy *= vMul;
        xSpin *= vMul;
        ySpin *= vMul;
        sideSpin *= vMul;
    }

    protected void simpleMove(Phy phy) {
        double vMul = phy.accelerationMultiplier();

        double rvx = vx / vMul;
        double rvy = vy / vMul;

        distance += Math.hypot(rvx, rvy);
        nextX = x + rvx;
        nextY = y + rvy;
        setX(nextX);
        setY(nextY);

        msSinceCue += 1 / vMul;  // 啊？
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
            double xErr = ERROR_GENERATOR.nextGaussian() * 
                    phy.cloth.goodness.errorFactor / phy.calculationsPerSec * TableCloth.RANDOM_ERROR_FACTOR +
                    phy.cloth.goodness.fixedErrorFactor / phy.calculationsPerSec * TableCloth.FIXED_ERROR_FACTOR;
            double yErr = ERROR_GENERATOR.nextGaussian() * 
                    phy.cloth.goodness.errorFactor / phy.calculationsPerSec * TableCloth.RANDOM_ERROR_FACTOR;
            vx += xErr / values.ball.ballWeightRatio;  // 重球相对稳定
            vy += yErr / values.ball.ballWeightRatio;
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
        double spinReduceRatio = phy.spinReducer / spinDiffTotal * dynamicDragFactor
                * table.speedReduceMultiplier;  // fixme: 可能是平方
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

    public boolean canDraw() {
        return !isPotted() || msRemainInPocket > 0;
    }

    private void oneFrameInPocket(Phy phy) {
        double pocketRange = pottedPocket.graphicalRadius - values.ball.ballRadius;
        double nextDt = predictedDtToPoint(pottedPocket.graphicalCenter);
        double curDt = currentDtToPoint(pottedPocket.graphicalCenter);
//        System.out.printf("%f, %f");
        if (nextDt > pocketRange
                && nextDt > curDt) {
            innerBounce(pottedPocket.graphicalCenter, 0.6);
        }
//        tryEnterGravityArea(phy, pottedPocket.graphicalCenter, pottedPocket.isMid);
        x = nextX;
        y = nextY;
        nextX = x + vx;
        nextY = y + vy;
    }

    private void innerBounce(double[] center, double factor) {
        double[] normal = new double[]{
                nextX - center[0],
                nextY - center[1]
        };
        normal = Algebra.normalVector(normal);
        double[] bounce = Algebra.symmetricVector(vx, vy, normal[0], normal[1]);
        vx = bounce[0] * factor;
        vy = bounce[1] * factor;
//        nextX = x + vx;
//        nextY = y + vy;
    }

    public boolean tryFrameInPocket(Phy phy) {
        if (isPotted()) {
            if (msRemainInPocket > 0) {
                msRemainInPocket -= phy.calculateMs;

                oneFrameInPocket(phy);
                if (getSpeedPerSecond(phy) < 100) {
                    // 球已经停了，别放了
                    msRemainInPocket = 0;
                    pot();
                    return false;
                }

                return true;
            } else {
                msRemainInPocket = 0;
                pot();
                return false;
            }
        } else {
            return false;
        }
    }

    public void naturalPot(long remainMs) {
        msRemainInPocket = remainMs;
        setPotted(true);

        for (Pocket pocket : table.pockets) {
            double dt = pocket.fallRadius;
            if (currentDtToPoint(pocket.fallCenter) < dt || predictedDtToPoint(pocket.fallCenter) < dt) {
                pottedPocket = pocket;
                break;
            }
        }
        if (pottedPocket == null) {
            System.err.println("Cannot find pot pocket");
            msRemainInPocket = 0;  // 不搞了
        }
    }

    public void pot() {
        setPotted(true);
        msRemainInPocket = 0;
        pottedPocket = null;
        x = 0.0;
        y = 0.0;
        clearMovement();
    }

    protected boolean tryHitPocketsBack(Phy phy) {
        if (nextX > table.leftX && nextX < table.rightX && nextY > table.topY && nextY < table.botY) {
            return false;
        }

        double cornerBackRadius = table.cornerPocketBackInnerRadius();
        double midBackRadius = table.midPocketBackInnerRadius();
        return tryHitPocketBack(
                table.topLeft.graphicalCenter,
                cornerBackRadius,
                136,
                314
        ) ||
                tryHitPocketBack(
                        table.topRight.graphicalCenter,
                        cornerBackRadius,
                        226,
                        44
                ) ||
                tryHitPocketBack(
                        table.botLeft.graphicalCenter,
                        cornerBackRadius,
                        46,
                        224
                ) ||
                tryHitPocketBack(
                        table.botRight.graphicalCenter,
                        cornerBackRadius,
                        316,
                        134
                ) ||
                tryHitPocketBack(
                        table.topMid.graphicalCenter,
                        midBackRadius,
                        181,
                        359
                ) ||
                tryHitPocketBack(
                        table.botMid.graphicalCenter,
                        midBackRadius,
                        1,
                        179
                );
    }

    protected boolean tryHitPocketBack(double[] center, double radius,
                                       double startDeg, double endDeg) {
        double dt = currentDtToPoint(center);
        if (dt < radius) {
            double nextDt = predictedDtToPoint(center);
            if (nextDt >= radius) {
                double biSector = Algebra.angularBisector(Math.toRadians(startDeg), Math.toRadians(endDeg));
                double direction = Algebra.thetaOf(vx, vy);
                double theta = Algebra.angleBetweenTwoAngles(biSector, direction);
                if (theta < Algebra.HALF_PI) {
//                    pot();
//                    System.out.println("Hit pocket back");
//                    double[] normal = new double[]{
//                            nextX - center[0],
//                            nextY - center[1]
//                    };
//                    normal = Algebra.normalVector(normal);
//                    double[] bounce = Algebra.symmetricVector(vx, vy, normal[0], normal[1]);
//                    vx = bounce[0] * 0.1;
//                    vy = bounce[1] * 0.1;
//                    nextX = x + vx;
//                    nextY = y + vy;

                    return true;
                }
            }
        }

        return false;
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

    protected void hitHoleArcArea(double[] arcXY, Phy phy, double arcRadius) {
        vx *= table.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor;
        vy *= table.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor;

        double[] collisionNormal = new double[]{arcXY[0] - x, arcXY[1] - y};  // 切线的法向量
        double[] tangentUnitVec = Algebra.unitVector(Algebra.normalVector(collisionNormal));
        tangentUnitVec[0] = -tangentUnitVec[0];  // 切线，从UI上看指向右侧，因为y反了
        tangentUnitVec[1] = -tangentUnitVec[1];

        applySpin(collisionNormal, tangentUnitVec, phy, 1.0);

        super.hitHoleArcArea(arcXY, phy, arcRadius);

        // 撞出塞
        double sideSpinChangeFactor = Algebra.projectionLengthOn(tangentUnitVec, new double[]{vx, vy});
        double sideSpinChange = sideSpinChangeFactor * values.table.cushionPowerSpinFactor * CUSHION_COLLISION_SPIN_FACTOR;

        if (currentBounce instanceof ArcBounce) {
            ((ArcBounce) currentBounce).setDesiredLeaveSideSpin(sideSpin + sideSpinChange);
        }
        
        pocketHitCount++;
    }

    protected void hitHoleLineArea(double[][] line, double[] lineNormalVec, Phy phy) {
        vx *= table.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor;
        vy *= table.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor;
        
        double[] tanUnitVec = Algebra.unitVector(new double[]{line[1][0] - line[0][0], line[1][1] - line[0][1]});
        applySpin(lineNormalVec, tanUnitVec, phy, 0.8);
        super.hitHoleLineArea(line, lineNormalVec, phy);
        
        pocketHitCount++;

        // 袋角直线撞得出来个屁的塞，反正我是没见过
    }

    private double calculateEffectiveSideSpin(Phy phy, double[] cushionNormalVec) {
        double[] vec = new double[]{vx, vy};
        // 库的切线方向的投影长度，意思是砸的深度，越深效果越强
        double vertical = Algebra.projectionLengthOn(cushionNormalVec, vec);
        double mag = Math.abs(vertical) * phy.calculationsPerSec;
//        double mag = Math.hypot(vx, vy) * phy.calculationsPerSec;
//        double sideSpinEffectMul = mag / Values.MAX_POWER_SPEED;

        double sideSpinEffectMul = Math.pow(mag / Values.MAX_POWER_SPEED, 0.8);
//        System.out.println(mag + " " + sideSpinEffectMul);
        return sideSpin * sideSpinEffectMul;
    }

    private void applySpin(double[] collisionNormal, double[] tangentVec, Phy phy, double factor) {
        double sideSpinStrength = calculateEffectiveSideSpin(phy, collisionNormal);
//        double sideSpinStrength = Math.abs(sideSpin) * phy.calculationsPerSecSqr;
        double effectiveSideSpin = sideSpinStrength * table.wallSpinEffectRatio * factor;

        // 也是那个思路，换底，在新的底处理旋转，再换回来
        double theta = -Algebra.rawThetaOf(tangentVec);
        double[][] cob = Algebra.changeOfBasisMatrix(theta);
        double[][] cobInverse = Algebra.changeOfBasisMatrix(-theta);

        double[] vv = new double[]{vx, vy};
        double[] vCob = Algebra.matrixMultiplyVector(cob, vv);
        vCob[0] += effectiveSideSpin;  // 侧旋的效果
        vCob[1] += Math.abs(effectiveSideSpin * 0.36);  // 避免加速过度
//        System.err.println("fuck");

        double[] spins = new double[]{xSpin, ySpin};
        double[] spinCob = Algebra.matrixMultiplyVector(cob, spins);
        
        vCob[1] += spinCob[1] * (1 - table.wallSpinPreserveRatio) * CUSHION_DIRECT_SPIN_APPLY;  // 一部分高低杆旋转直接生效了
        
        spinCob[0] *= 1 - (1 - table.wallSpinPreserveRatio) * 0.5;
        spinCob[1] *= table.wallSpinPreserveRatio * SUCK_CUSHION_FACTOR;
        
        double[] inverse = Algebra.matrixMultiplyVector(cobInverse, vCob);

        vx = inverse[0];
        vy = inverse[1];

        double[] spinInverse = Algebra.matrixMultiplyVector(cobInverse, spinCob);
        xSpin = spinInverse[0];
        ySpin = spinInverse[1];

//        if (!phy.isPrediction) System.out.println("Spin before " + sideSpin);
        sideSpin -= effectiveSideSpin;  // 一定同号
//        if (!phy.isPrediction) System.out.println("Spin after " + sideSpin);
        sideSpin *= table.wallSpinPreserveRatio;
    }

    /**
     * 碰库时的旋转:
     * 1. 施加侧旋的效果
     * 2. 更新所有旋转效果
     * <p>
     * 需在更新了vx和vy之后调用
     * <p>
     * 该方法仅在撞标准库时调用，不负责袋角
     *
     * @param phy              物理
     * @param cushionNormalVec 撞的库的法向量。比如说边库应是(1,0)或(-1,0)
     */
    private void applySpinsWhenHitCushion(Phy phy, double[] cushionNormalVec) {
        double sideSpinStrength = calculateEffectiveSideSpin(phy, cushionNormalVec);
        double effectiveSideSpin = sideSpinStrength * table.wallSpinEffectRatio;

        if (Arrays.equals(cushionNormalVec, LEFT_CUSHION_VEC_NORM) || Arrays.equals(cushionNormalVec, RIGHT_CUSHION_VEC_NORM)) {
            if (vx < 0) {
                vy -= effectiveSideSpin;
            } else {
                vy += effectiveSideSpin;
            }
            xSpin *= table.wallSpinPreserveRatio * SUCK_CUSHION_FACTOR;
            ySpin *= 1 - (1 - table.wallSpinPreserveRatio) * 0.5;
        } else if (Arrays.equals(cushionNormalVec, TOP_CUSHION_VEC_NORM) || Arrays.equals(cushionNormalVec, BOT_CUSHION_VEC_NORM)) {
            if (vy < 0) {
                vx += effectiveSideSpin;
            } else {
                vx -= effectiveSideSpin;
            }
            xSpin *= 1 - (1 - table.wallSpinPreserveRatio) * 0.5;  // 比如ratio是0.8，这里就取0.9
            ySpin *= table.wallSpinPreserveRatio * SUCK_CUSHION_FACTOR;
        }

        sideSpin -= effectiveSideSpin;
        sideSpin *= table.wallSpinPreserveRatio;
//        sideSpin *= table.wallSpinPreserveRatio / sideSpinEffectMul;
    }

    /**
     * 该方法不检测袋口
     */
    protected boolean tryHitWall(Phy phy) {
        if (nextX < values.ball.ballRadius + table.leftX ||
                nextX >= table.rightX - values.ball.ballRadius) {
            // 顶库(屏幕两边)

            // 先减速，算是对时间复杂度的一种妥协
            vx *= table.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor;
            vy *= table.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor;
//            vy += ySpin * (1 - table.wallSpinPreserveRatio) * CUSHION_DIRECT_SPIN_APPLY;  // 一部分旋转直接生效了

            boolean isLeft = nextX < values.table.midX;
            double[] cushionVec = isLeft ? LEFT_CUSHION_VEC : RIGHT_CUSHION_VEC;
            double[] normalVec = isLeft ? LEFT_CUSHION_VEC_NORM : RIGHT_CUSHION_VEC_NORM;
            double[][] cushionLine = isLeft ? values.table.leftCushion : values.table.rightCushion;
//            applySpinsWhenHitCushion(phy, normalVec);
            applySpin(normalVec, cushionVec, phy, 1.0);

            double effectiveAcc = -bounceAcc(phy, vx);
            double nFrames = getNFramesInCushion(vx, effectiveAcc);

            double[] hitCushionPos = getCushionHitPos(cushionLine);

            double leaveY = hitCushionPos[1] + nFrames * vy;
            double hSpeedLoss = values.table.cushionPowerSpinFactor * (getSpeedPerSecond(phy) / Values.MAX_POWER_SPEED) * 0.25;
            // 撞库撞出来的塞
            double sideSpinChangeFactor = Algebra.projectionLengthOn(cushionVec, new double[]{vx, vy});
            double sideSpinChange = sideSpinChangeFactor * values.table.cushionPowerSpinFactor * CUSHION_COLLISION_SPIN_FACTOR;
            currentBounce = new CushionBounce(
                    effectiveAcc,
                    0,
                    phy.accelerationMultiplier());
            ((CushionBounce) currentBounce).setDesiredLeavePos(
                    hitCushionPos[0],
                    leaveY,
                    -vx,
                    vy * (1 - hSpeedLoss),
                    sideSpin + sideSpinChange);
            return true;
        }
        if (nextY < values.ball.ballRadius + table.topY ||
                nextY >= table.botY - values.ball.ballRadius) {
            // 边库
            vx *= table.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor;
            vy *= table.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor;
//            vx += xSpin * (1 - table.wallSpinPreserveRatio) * CUSHION_DIRECT_SPIN_APPLY;  // 一部分旋转直接生效了

            boolean isTop = nextY < table.midY;
            double[] cushionVec = isTop ? TOP_CUSHION_VEC : BOT_CUSHION_VEC;
            double[] normalVec = isTop ? TOP_CUSHION_VEC_NORM : BOT_CUSHION_VEC_NORM;
            boolean isLeft = nextX < values.table.midX;
            double[][] cushionLine = isTop ? (
                    isLeft ? values.table.topLeftCushion : values.table.topRightCushion)
                    : (isLeft ? values.table.botLeftCushion : values.table.botRightCushion);
//            applySpinsWhenHitCushion(phy, normalVec);
            applySpin(normalVec, cushionVec, phy, 1.0);

            double effectiveAcc = -bounceAcc(phy, vy);
            double nFrames = getNFramesInCushion(vy, effectiveAcc);

            double[] hitCushionPos = getCushionHitPos(cushionLine);

            double leaveX = hitCushionPos[0] + nFrames * vx;
            double hSpeedLoss = values.table.cushionPowerSpinFactor * (getSpeedPerSecond(phy) / Values.MAX_POWER_SPEED) * 0.25;
            // 撞库撞出来的塞
            double sideSpinChangeFactor = Algebra.projectionLengthOn(cushionVec, new double[]{vx, vy});
            double sideSpinChange = sideSpinChangeFactor * values.table.cushionPowerSpinFactor * CUSHION_COLLISION_SPIN_FACTOR;
            currentBounce = new CushionBounce(
                    0,
                    effectiveAcc,
                    phy.accelerationMultiplier());
            ((CushionBounce) currentBounce).setDesiredLeavePos(
                    leaveX,
                    hitCushionPos[1],
                    vx * (1 - hSpeedLoss),
                    -vy,
                    sideSpin + sideSpinChange);
            return true;
        }
        return false;
    }

    void threeBallHit(Ball ball2, Ball ball3, Phy phy) {
        double dt12 = predictedDtTo(ball2);  // 12，13都小于直径
        double dt13 = predictedDtTo(ball3);
//        double dt23 = ball2.predictedDtTo(ball3);

        if (dt12 < dt13) {
            tryHitBall(ball2, true, false, phy);
            if (isHitting(ball3, phy)) tryHitBall(ball3, true, false, phy);
        } else {
            tryHitBall(ball3, true, false, phy);
            if (isHitting(ball2, phy)) tryHitBall(ball2, true, false, phy);
        }
    }

    int tryHitTwoBalls2(Ball ball1, Ball ball2, Phy phy) {
        if (isHitting(ball1, phy)) {
            // 0撞1
            if (isHitting(ball2, phy)) {
                // 0撞12
                threeBallHit(ball1, ball2, phy);
                return 1;
            }
        }
        if (isHitting(ball2, phy)) {
            // 0撞2
            if (ball2.isHitting(ball1, phy)) {
                // 2撞01
                ball2.threeBallHit(this, ball1, phy);
                return 1;
            }
        }
        if (ball1.isHitting(ball2, phy)) {
            if (ball1.isHitting(this, phy)) {
                // 1撞02
                ball1.threeBallHit(this, ball2, phy);
                return 1;
            }
        }
        return 0;
    }

    /**
     * 返回:
     * 0: 真的没有三颗球撞一起（包括没有球碰撞和纯二球碰撞）
     * 1: 发生了可以处理的三球碰撞
     * 2: 发生了无法处理的三球碰撞
     */
    int tryHitTwoBalls(Ball ball1, Ball ball2, Phy phy) {
        if (this.isNotMoving()) {
            if (ball1.isNotMoving()) {
                if (ball2.isNotMoving()) {
                    return 0;  // 三颗球都没动
                } else {
                    return ball2.tryHitTwoBalls(this, ball1, phy);
                }
            } else {
                if (ball2.isNotMoving()) {
                    return ball1.tryHitTwoBalls(this, ball2, phy);
                } else {
//                    System.err.println("Both balls are moving");
//                    return 2;  // ball1、ball2 都在动，无法处理
                    if (isHitting(ball1, phy)) {
                        if (isHitting(ball2, phy)) {
                            threeBallHitCore(ball1, ball2, phy);
                            return 1;
                        }
                    }
                    return 0;
                }
            }
        } else {
            if (ball1.isNotMoving() && ball2.isNotMoving()) {
                // this 去撞另外两颗
//                double dt1 = currentDtTo(ball1), dt2 = currentDtTo(ball2);
//                double nextDt1 = predictedDtTo(ball1), nextDt2 = predictedDtTo(ball2);
//                if (((dt1 = predictedDtTo(ball1)) < values.ball.ballDiameter && currentDtTo(ball1) > dt1
//                        && justHit != ball1 && ball1.justHit != this
//                ) && ((dt2 = predictedDtTo(ball2)) < values.ball.ballDiameter && currentDtTo(ball2) > dt2
//                        && justHit != ball2 && ball2.justHit != this
//                )) {
//                if ((nextDt1 < values.ball.ballDiameter && nextDt1 < dt1)
//                        && (nextDt2 < values.ball.ballDiameter && nextDt2 < dt2)
////                        && justHit != ball1 && ball1.justHit != this && justHit != ball2 && ball2.justHit != this
//                ) {
//                    System.out.println("Hit two static balls!=====================");
                if (isHitting(ball1, phy) && isHitting(ball2, phy)) {

                    threeBallHitCore(ball1, ball2, phy);

                    return 1;
                } else {
                    return 0;  // 这三颗球没有贴在一起
                }
            } else {
//                System.err.println("Both balls are moving");
//                return 2;  // this 和 ball1、ball2 中的至少一颗都在动，无法处理
                if (isHitting(ball1, phy)) {
                    if (isHitting(ball2, phy)) {
                        threeBallHitCore(ball1, ball2, phy);
                        return 1;
                    }
                }
                return 0;
            }
        }
    }

    private void threeBallHitCore(Ball ball1, Ball ball2, Phy phy) {
        double xPos = x;
        double yPos = y;
        double dx = vx / Values.DETAILED_PHYSICAL;
        double dy = vy / Values.DETAILED_PHYSICAL;

        boolean ball1First = true;

        for (int i = 0; i < Values.DETAILED_PHYSICAL; ++i) {
            if (Algebra.distanceToPoint(xPos + dx, yPos + dy, ball1.x, ball1.y) < values.ball.ballDiameter) {
                break;
            }
            if (Algebra.distanceToPoint(xPos + dx, yPos + dy, ball2.x, ball2.y) < values.ball.ballDiameter) {
                ball1First = false;
                break;
            }
            xPos += dx;
            yPos += dy;
        }

        if (ball1First) {
            tryHitBall(ball1, false, false, phy);
            if (isHitting(ball2, phy)) tryHitBall(ball2, false, false, phy);
        } else {
            tryHitBall(ball2, false, false, phy);
            if (isHitting(ball1, phy)) tryHitBall(ball1, false, false, phy);
        }
    }

    private double msNeededToHit(Ball other, Phy phy) {
        // 可以返回负数
        double precisionMm = 0.00001 / phy.calculationsPerSecSqr;

        double x1 = x;
        double y1 = y;
        double x2 = other.x;
        double y2 = other.y;

        double result;
        double high = phy.calculateMs * 1;
        double low = -high;

        double dt;

        int count = 0;
        while (count < 100) {
            result = (high + low) / 2;
            dt = Algebra.distanceToPoint(
                    x1 + vx * result,
                    y1 + vy * result,
                    x2 + other.vx * result,
                    y2 + other.vy * result
            ) - values.ball.ballDiameter;
            if (Math.abs(dt) < precisionMm) {
                System.out.println("Count: " + count + ", dt: " + dt);
                return result;
            }

            if (dt < 0) {  // 过了
                low = result;
            } else {  // 没到
                high = result;
            }

            count++;
        }
        throw new RuntimeException("Cannot find collision point!");
//        System.err.println("Cannot find collision point!");
//        return 0.0;
    }

    boolean isHitting(Ball other, Phy phy) {
        double lastDt = currentDtTo(other);
        double nextDt = predictedDtTo(other);
        return nextDt < values.ball.ballDiameter && nextDt < lastDt;
    }

    void twoMovingBallsHitCore(Ball ball, Phy phy, boolean considerGearSpin) {
        double x1 = x;
        double y1 = y;
        double x2 = ball.x;
        double y2 = ball.y;
        if (Math.hypot(vx, vy) + Math.hypot(ball.vx, ball.vy) > values.ball.ballRadius) {
            // 怕球速太快，它跑过了
            x1 -= vx;
            y1 -= vy;
            x2 -= ball.vx;
            y2 -= ball.vy;
        }

        if (Algebra.distanceToPoint(x1, y1, x2, y2) <=
                Algebra.distanceToPoint(x1 + vx, y1 + vy, x2 + ball.vx, y2 + ball.vy)) {
            if (!phy.isPrediction) {
                System.err.println("Will not even collide!");
            }
//            throw new RuntimeException();
            return;
        }

        // 离岸位置发生改变，又懒得重新算了
        clearBounceDesiredLeavePos();
        ball.clearBounceDesiredLeavePos();

        int maxRound = 100;
        int phyRounds = 0;
        double tickDt;
        double curDivider = 1.0;

        double allowedDev = 0.01;  // 0.01 mm

        // 类似二分搜索，找碰撞点
        while (phyRounds < maxRound) {
            tickDt = Algebra.distanceToPoint(x1, y1, x2, y2) - values.ball.ballDiameter;
            if (Math.abs(tickDt) < allowedDev) break;
            double mul;
            if (tickDt > 0) {
                mul = 1;
            } else {
                mul = -1;
            }

            x1 += vx / curDivider * mul;
            y1 += vy / curDivider * mul;
            x2 += ball.vx / curDivider * mul;
            y2 += ball.vy / curDivider * mul;

            curDivider *= 2;
            phyRounds++;
        }
//        System.out.println("Rounds: " + phyRounds);

        if (!phy.isPrediction) {
            // AI考虑进攻时并不会clone目标球
            // 因此我们不希望AI在模拟时触发任何移动目标球的行为
            this.x = x1;
            this.y = y1;
            ball.x = x2;
            ball.y = y2;
        }

        this.lastCollisionX = x1;
        this.lastCollisionY = y1;
        ball.lastCollisionX = x2;
        ball.lastCollisionY = y2;

        double[] thisV = new double[]{vx, vy};
        double[] ballV = new double[]{ball.vx, ball.vy};

        double[] normVec = new double[]{x1 - x2, y1 - y2};  // 两球连线=法线
        double[] tangentVec = Algebra.normalVector(normVec);  // 切线

        double rotate = -Algebra.rawThetaOf(tangentVec);
//        System.out.println(Math.toDegrees(rotate));
        double[][] changeOfBasis = Algebra.changeOfBasisMatrix(rotate);
        double[][] inverseCob = Algebra.changeOfBasisMatrix(-rotate);

        // 为齿轮效应计算做准备
        double collisionThickness = Algebra.thetaBetweenVectors(thisV, tangentVec);  // 90度是正撞，0度是球1擦球2的右边，180度是擦左边
        collisionThickness -= Algebra.HALF_PI;  // 减去90度，正撞为0，擦右边为-90
//        System.out.println("Thick: " + Math.toDegrees(collisionThickness));
        double relSpeed = Math.hypot(this.vx - ball.vx, this.vy - ball.vy);
        double totalSpeed = (Math.hypot(this.vx, this.vy) + Math.hypot(ball.vx, ball.vy)) * phy.calculationsPerSec;

        double[] thisVCob = Algebra.matrixMultiplyVector(changeOfBasis, thisV);
        double[] ballVCob = Algebra.matrixMultiplyVector(changeOfBasis, ballV);
        double thisVerV = thisVCob[1];  // 垂直于切线的速率
        double thisHorV = thisVCob[0];  // 平行于切线的速率
        double ballVerV = ballVCob[1];
        double ballHorV = ballVCob[0];

//        System.out.println(Arrays.toString(thisVCob) + " " + Arrays.toString(ballVCob));

//        System.out.printf("(%f, %f), (%f, %f)\n", thisHorV, thisVerV, ballHorV, ballVerV);
//        System.out.print("Ball 1 " + this + " ");

        // todo: 看看有没有问题。应该没问题，以前是因为atan引起的，现在atan2应该就好了
        // todo: 我错了，去掉之后三天两头卡bug
        if (thisHorV == 0) thisHorV = 0.0000000001;
        if (thisVerV == 0) thisVerV = 0.0000000001;
        if (ballHorV == 0) ballHorV = 0.0000000001;
        if (ballVerV == 0) ballVerV = 0.0000000001;

        double thisOutHor = thisHorV;
        double thisOutVer = ballVerV;
        double ballOutHor = ballHorV;
        double ballOutVer = thisVerV;
        if (ball.vx == 0 && ball.vy == 0) {  // 两颗动球碰撞考虑齿轮效应太麻烦了
            // 实为投掷效应
            double powerGear = Math.min(1.0,
                    totalSpeed / GEAR_EFFECT_MAX_POWER / Values.MAX_POWER_SPEED * values.ball.ballWeightRatio);  // 32的力就没有效应了(高低杆要打出35的球速，起码要45的力)
            double throwEffect = (1 - powerGear) * MAX_GEAR_EFFECT;
            double gearEffect = 1 - throwEffect;

            double spinProj = Algebra.projectionLengthOn(thisV,
                    new double[]{this.xSpin, this.ySpin}) * phy.calculationsPerSec / 1500;  // 旋转方向在这颗球原本前进方向上的投影

            double gearRemain = throwEffect * spinProj;

            ballOutVer *= gearEffect;
            thisOutVer = thisVerV * gearRemain;
            
            if (gearOffsetEnabled) {
                double ratio = thisHorV / thisVerV;
                int sign = ratio < 0 ? -1 : 1;
                double offsetRatio = Math.sqrt(Math.abs(ratio * sign)) * sign;
                ballOutHor = ballOutVer * offsetRatio * (gearRemain * 0.2);  // 目标球身上的投掷效应。AI不会算，所以我们只给玩家搞这个
            }
        }

        // 碰撞后，两球平行于切线的速率不变，垂直于切线的速率互换
        double[] thisOutAtRelAxis = new double[]{
                thisOutHor, thisOutVer
        };
        double[] ballOutAtRelAxis = new double[]{
                ballOutHor, ballOutVer
        };
        double[] thisOut = Algebra.matrixMultiplyVector(inverseCob, thisOutAtRelAxis);
        double[] ballOut = Algebra.matrixMultiplyVector(inverseCob, ballOutAtRelAxis);

        this.vx = thisOut[0];
        this.vy = thisOut[1];
        ball.vx = ballOut[0];
        ball.vy = ballOut[1];

        // 齿轮效应的旋转传递
        double thisOutSpeed = Math.hypot(this.vx, this.vy);
        double ballOutSpeed = Math.hypot(ball.vx, ball.vy);
        if (considerGearSpin && relSpeed != 0.0 && (thisOutSpeed != 0.0 || ballOutSpeed != 0.0)) {
            double gearPassFactor = 0.18;
            double passRate = Math.cos(Math.abs(collisionThickness));  // 越厚传得越多。
            double speedPassRate = Math.abs(this.sideSpin) / relSpeed;  // 球速越慢，塞越大，传得越多
            double passPercentage = gearPassFactor * passRate * speedPassRate;
            passPercentage = Math.min(passPercentage, MAXIMUM_SPIN_PASS);  // 最多传1/4
            double passed = this.sideSpin * passPercentage;

            this.sideSpin -= passed;  // 自己的塞会减少，动量守恒嘛
            ball.sideSpin -= passed;  // 右塞传到球上就是左塞了

            if (gearOffsetEnabled) {
                // 相当于整个坐标系往一个方向扭一点点
                double angularRate = 0.18;
                double thisOutAng = Algebra.thetaOf(this.vx, this.vy);
                double deviation = passed * angularRate / thisOutSpeed;
                // 弱化大力的效果
                deviation *= Algebra.shiftRangeSafe(0, phy.maxPowerSpeed(), 1, 0.5, relSpeed);
                double[] thisOutGeared = Algebra.unitVectorOfAngle(thisOutAng - deviation);
                this.vx = thisOutSpeed * thisOutGeared[0];
                this.vy = thisOutSpeed * thisOutGeared[1];
                
                double ballOutAng = Algebra.thetaOf(ball.vx, ball.vy);
                double deviation2 = passed * angularRate / ballOutSpeed;
                deviation2 *= Algebra.shiftRangeSafe(0, phy.maxPowerSpeed(), 1, 0.5, relSpeed);
                double[] ballOutGeared = Algebra.unitVectorOfAngle(ballOutAng - deviation2);
                ball.vx = ballOutSpeed * ballOutGeared[0];
                ball.vy = ballOutSpeed * ballOutGeared[1];
            }

            // 薄边造成的旋转
            double gearStrengthFactor = 0.15;
            double spinChange = gearStrengthFactor * Math.cos(Algebra.HALF_PI - collisionThickness) * relSpeed;
            this.sideSpin += spinChange;
            ball.sideSpin -= spinChange;
        }

        // update
        nextX = x1 + vx;
        nextY = y1 + vy;
        ball.nextX = x2 + ball.vx;
        ball.nextY = y2 + ball.vy;

        if (Algebra.distanceToPoint(nextX, nextY, ball.nextX, ball.nextY) < Algebra.distanceToPoint(x, y, ball.x, ball.y)) {
            if (!phy.isPrediction)
                System.err.printf("Ball %d@%f,%f->%f,%f and ball %d@%f,%f->%f,%f not collide properly\n",
                        getValue(), x, y, nextX, nextY, ball.getValue(), ball.x, ball.y, ball.nextX, ball.nextY);
            if (!phy.isPrediction) System.err.printf("%d rounds, Last dt: %f, new dt: %f\n",
                    phyRounds,
                    Algebra.distanceToPoint(x, y, ball.x, ball.y),
                    Algebra.distanceToPoint(nextX, nextY, ball.nextX, ball.nextY));
        }
    }

    boolean tryHitBall(Ball ball, boolean checkMovingBall, boolean applyGearSpin, Phy phy) {
        if (this.isNotMoving(phy)) {
            if (ball.isNotMoving(phy)) return false;  // 两球都没动，怎么可能撞
        }

        double dt = predictedDtTo(ball);
        if (dt < values.ball.ballDiameter
                && currentDtTo(ball) > dt
//                && justHit != ball && ball.justHit != this
        ) {
//            if (this.isNotMoving()) {
//                if (ball.isNotMoving()) {
//                    if (checkMovingBall) {
//                        throw new RuntimeException("他妈的两颗静止的球拿头撞？");
//                    } else {
//                        System.err.println("Complex collision scenario");
//                        return false;
//                    }
//                } else {
//                    return ball.tryHitBall(this, checkMovingBall, applyGearSpin, phy);
//                }
//            }
//            if (!ball.isNotMoving()) {
//                if (!checkMovingBall) return false;
//            }

            twoMovingBallsHitCore(ball, phy, applyGearSpin);

//            justHit = ball;
//            ball.justHit = this;
            return true;
        }
        return false;
    }

    @Override
    public void clearMovement() {
        super.clearMovement();
        xSpin = 0.0;
        ySpin = 0.0;
        sideSpin = 0.0;
//        justHit = null;
        frameDegChange = 0.0;
        pocketHitCount = 0;
    }

    protected void prepareMove(Phy phy) {
        super.prepareMove(phy);
//        justHit = null;
    }

    public Color getColorTransparent() {
        return colorTransparent;
    }

    public Color getColorWithOpa() {
        return colorWithOpa;
    }

    public Color getColor() {
        return color;
    }

    public int getValue() {
        return value;
    }

    public double getAxisX() {
        return axisX;
    }

    public double getAxisY() {
        return axisY;
    }

    public double getAxisZ() {
        return axisZ;
    }

    public double getFrameDegChange() {
        return frameDegChange;
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
        return this == o || (o instanceof Ball && this.identifier == ((Ball) o).identifier);
    }

    /**
     * @return ball实例的hashcode，clone之后应保持相同。注意不要用value来生成hashcode，因为斯诺克有15颗value一样的红球
     */
    @Override
    public int hashCode() {
        return identifier;
    }

    public boolean checkEnterBreakArea(double breakLineX) {
        return x >= breakLineX && nextX < breakLineX;
    }

    public double getLastCollisionX() {
        return lastCollisionX;
    }

    public double getLastCollisionY() {
        return lastCollisionY;
    }
}
