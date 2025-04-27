package trashsoftware.trashSnooker.core;

import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.core.metrics.BallMetrics;
import trashsoftware.trashSnooker.core.metrics.Cushion;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.metrics.Pocket;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.phy.TableCloth;
import trashsoftware.trashSnooker.fxml.drawing.BallModel;

import java.util.Random;

public abstract class Ball extends ObjectOnTable implements Comparable<Ball>, Cloneable {
    public static final double MAX_GEAR_EFFECT = 0.25;  // 齿轮效应造成的最严重分离角损耗
    public static final double GEAR_EFFECT_MAX_POWER = 0.3;  // 大于这个球速就没有齿轮效应了
    public static final double CUSHION_COLLISION_SPIN_FACTOR = 0.5;
    public static final double CUSHION_DIRECT_SPIN_APPLY = 0.4;
    public static final double SUCK_CUSHION_FACTOR = 0.7;
    public static final double MAXIMUM_SPIN_PASS = 0.2;  // 齿轮效应传递旋转的上限
    public static final double NEAR_CUSHION_AREA = 2.5;
    public static final double NEAR_CUSHION_ACC = 10.0;
    private static final Random ERROR_GENERATOR = new Random();
    private static boolean gearOffsetEnabled = true;  // 齿轮/投掷效应造成的球线路偏差
    private static int idCounter = 0;
    public final BallModel model;
    protected final int value;
    private final Color color;
    private final Color colorWithOpa;
    private final Color colorTransparent;
    private final Color traceColor;
    private final int identifier;  // 即使是分值一样的球identifier也不一样，但是clone之后identifier保持不变
    protected double xSpin, ySpin;
    protected double sideSpin;
    protected double axisX, axisY, axisZ,
            frameDegChange;  // 全部都是针对一个动画帧
    private boolean potted;
    private long phyFramesSinceCue;
    private long msRemainInPocket;
    private Pocket pottedPocket;
    private double maxInPocketSpeed;  // 本杆在袋内的最大速度，m/s
    //    private Ball justHit;
    int pocketHitCount = 0;  // 本杆撞击袋角的次数

    private double lastCollisionX, lastCollisionY;  // 记录一下上次碰撞所在的位置
    private double lastCollisionRelSpeed;

    protected Ball(int value, boolean initPotted, GameValues values) {
        super(values, values.ball.ballRadius);

        identifier = idCounter++;

        this.value = value;
        this.color = generateColor(value);
        this.colorWithOpa = this.color.deriveColor(0, 1, 1.6, 0.5);
        this.colorTransparent = colorWithOpa.deriveColor(0, 1, 1, 0);
        this.traceColor = this.color.deriveColor(0, 1, 0.75, 1);

        model = BallModel.createModel(this, values.getBallsGroupPreset());
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

    public boolean isNotOnTable() {
        return isPotted() || isOutOfTable();
    }

    public void pickup() {
        setPotted(false);
        clearMovement();
    }

    public boolean isWhite() {
        return value == 0;
    }

    public void setSpin(double xSpin, double ySpin, double sideSpin) {
        phyFramesSinceCue = 0;
        this.xSpin = xSpin;
        this.ySpin = ySpin;
        this.sideSpin = sideSpin;
    }

    public boolean isNotMoving(Phy phy) {
        double slippingFriction = values.ball.frictionRatio * phy.slippingFrictionTimed;
        return getSpeed() < slippingFriction;
    }

    public boolean isMovingTowards(Ball other) {
        double dx = other.x - this.x;
        double dy = other.y - this.y;
        double dot = dx * this.vx + dy * this.vy;
        return dot > 0;
    }

    public boolean isLikelyStopped(Phy phy) {
        double slippingFriction = values.ball.frictionRatio * phy.slippingFrictionTimed;
        if (getSpeed() < slippingFriction   // todo: 写不明白，旋转停
                &&
                getSpinTargetSpeed() < slippingFriction * 2
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
        double ss = sideSpin * Math.PI;  // 不要误会，没有这么神，只是3.14刚好看起来差不多
//        if (isWhite()) System.out.println(xSpin + " " + ySpin + " " + sideSpin);
        if (Double.isNaN(ss)) ss = 0.0;
        axisZ = -ss;

        frameDegChange =
                Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)
                        * phy.calculationsPerSec * animationFrameMs * 0.00145;
    }

    /**
     * 原地转
     */
    protected boolean sideSpinAtPosition(Phy phy) {
        phyFramesSinceCue++;
        double sideSpinReducer = values.ball.frictionRatio * phy.sideSpinFrictionTimed;

        if (Math.abs(sideSpin) < sideSpinReducer) {
            sideSpin = 0;
            return false;
        } else if (sideSpin > 0) {
            sideSpin -= sideSpinReducer;
        } else {
            sideSpin += sideSpinReducer;
        }
        return true;
    }

    protected void normalMove(Phy phy) {
        distance += Math.hypot(vx, vy);
        setX(nextX);
        setY(nextY);
        phyFramesSinceCue++;

        // 球的质量会抵消
        double slippingFriction = values.ball.frictionRatio * phy.slippingFrictionTimed;
        double sideSpinReducer = values.ball.frictionRatio * phy.sideSpinFrictionTimed;

        double rollingFriction = values.ball.frictionRatio * phy.rollingFrictionTimed;

        if (Math.abs(sideSpin) < sideSpinReducer) {
            sideSpin = 0;
        } else if (sideSpin > 0) {
            sideSpin -= sideSpinReducer;
        } else {
            sideSpin += sideSpinReducer;
        }

        // Compute slip vector
        double slipX = vx - xSpin;
        double slipY = vy - ySpin;

        // Compute slip magnitude
        double slipMag = Math.hypot(slipX, slipY);

        if (slipMag < slippingFriction) {
            // 滚动
            double speed = Math.hypot(vx, vy);
            if (speed > rollingFriction) {
                double directionX = vx / speed;
                double directionY = vy / speed;

                // Apply friction against velocity
                vx -= directionX * rollingFriction;
                vy -= directionY * rollingFriction;

                // Reduce spin in the same direction (rolling implies match)
                xSpin = vx;
                ySpin = vy;
            } else {
                vx = 0;
                vy = 0;
                xSpin = 0;
                ySpin = 0;
            }
        } else {
            // 滑动
            // Normalize slip direction
            double slipDirX = slipX / slipMag;
            double slipDirY = slipY / slipMag;

            double spinEffect = slippingFriction * TableCloth.SLIP_ACCELERATE_EFFICIENCY;  // 这个乘数是旋转的转化成动力的效率
            // Apply friction against velocity (reduce slipping)
            vx -= slipDirX * spinEffect;
            vy -= slipDirY * spinEffect;

            // Apply friction to spin in opposite direction (spin catches up)
            xSpin += slipDirX * slippingFriction;
            ySpin += slipDirY * slippingFriction;
        }

        if (!phy.isPrediction) {
            // 这部分是台泥造成的线路偏差
            clothPathChange(phy);
        }
    }

//    protected void normalMove2(Phy phy) {
//        distance += Math.hypot(vx, vy);
//        setX(nextX);
//        setY(nextY);
//
////        if (!phy.isPrediction) calculateAxis(phy);
//
//        phyFramesSinceCue++;
//        if (sideSpin >= phy.sideSpinReducer) {
//            sideSpin -= phy.sideSpinReducer;
//        } else if (sideSpin <= -phy.sideSpinReducer) {
//            sideSpin += phy.sideSpinReducer;
//        }
//
//        double speed = getSpeed();
//        double reducedSpeed = speed - values.speedReducerPerInterval(phy);
//        double ratio = reducedSpeed / speed;
//        vx *= ratio;
//        vy *= ratio;
//
//        if (!phy.isPrediction) {
//            // 这部分是台泥造成的线路偏差
//            clothPathChange(phy);
//        }
//
//        double xSpinDiff = xSpin - vx;
//        double ySpinDiff = ySpin - vy;
//
//        // 对于滑的桌子，转速差越大，旋转reducer和effect反而越小
//        double spinDiffTotal = Math.hypot(xSpinDiff, ySpinDiff);
//        double spinDiffFactor = spinDiffTotal / Values.MAX_SPIN_DIFF * phy.calculationsPerSec;
//        spinDiffFactor = Math.min(1.0, spinDiffFactor);
//        double dynamicDragFactor = 1 - phy.cloth.smoothness.tailSpeedFactor * spinDiffFactor;
//
////        if (isWhite() && !phy.isPrediction) System.out.println(dynamicDragFactor);
//
//        // 乘和除抵了，所以第一部分是线性的
//        double spinReduceRatio = phy.spinReducer / spinDiffTotal * dynamicDragFactor
//                * table.speedReduceMultiplier;  // fixme: 可能是平方
//        double xSpinReducer = Math.abs(xSpinDiff * spinReduceRatio);
//        double ySpinReducer = Math.abs(ySpinDiff * spinReduceRatio);
//
////        if (isWhite()) System.out.printf("vx: %f, vy: %f, xr: %f, yr: %f, spin: %f\n", vx, vy, xSpinReducer, ySpinReducer, SnookerGame.spinReducer);
//
//        if (xSpinDiff < -xSpinReducer) {
//            vx += xSpinDiff / phy.spinEffect * dynamicDragFactor;
//            xSpin += xSpinReducer * dynamicDragFactor;
//        } else if (xSpinDiff >= xSpinReducer) {
//            vx += xSpinDiff / phy.spinEffect * dynamicDragFactor;
//            xSpin -= xSpinReducer * dynamicDragFactor;
//        } else {
//            xSpin = vx;
//        }
//
//        if (ySpinDiff < -ySpinReducer) {
//            vy += ySpinDiff / phy.spinEffect * dynamicDragFactor;
//            ySpin += ySpinReducer * dynamicDragFactor;
//        } else if (ySpinDiff >= ySpinReducer) {
//            vy += ySpinDiff / phy.spinEffect * dynamicDragFactor;
//            ySpin -= ySpinReducer * dynamicDragFactor;
//        } else {
//            ySpin = vy;
//        }
//
//        if (!phy.isPrediction) {
//            nearCushionChangePath(phy);
//        }
//    }

    private void clothPathChange(Phy phy) {
        // 逆毛效应
        double[] direction = Algebra.unitVector(vx, vy);
        double fixedError = Math.max(-direction[0], 0);  // 从右到左的球(vx<0的)才有逆毛效应
        fixedError *= Math.abs(direction[1]);  // 希望在斜45度时逆毛效应达到最大
        fixedError *= phy.cloth.goodness.fixedErrorFactor / phy.calculationsPerSec *
                TableCloth.FIXED_ERROR_FACTOR * table.getClothType().backNylonEffect;
//            System.out.println("Fixed error: " + fixedError);

        double xErr = ERROR_GENERATOR.nextGaussian() *
                phy.cloth.goodness.errorFactor / phy.calculationsPerSec * TableCloth.RANDOM_ERROR_FACTOR +
                fixedError;
        double yErr = ERROR_GENERATOR.nextGaussian() *
                phy.cloth.goodness.errorFactor / phy.calculationsPerSec * TableCloth.RANDOM_ERROR_FACTOR;
        vx += xErr / values.ball.ballWeightRatio;  // 重球相对稳定
        vy += yErr / values.ball.ballWeightRatio;
    }

    private void nearCushionChangePath(Phy phy) {
        BallMetrics ballMetrics = values.ball;
        if (x >= table.leftX + ballMetrics.ballRadius
                && x < table.leftX + ballMetrics.ballRadius + NEAR_CUSHION_AREA
                && vx > 0) {
            vx -= NEAR_CUSHION_ACC / phy.calculationsPerSecSqr;
        } else if (x < table.rightX - ballMetrics.ballRadius
                && x >= table.rightX - ballMetrics.ballRadius - NEAR_CUSHION_AREA
                && vx < 0) {
            vx += NEAR_CUSHION_ACC / phy.calculationsPerSecSqr;
        } else if (y >= table.topY + ballMetrics.ballRadius
                && y < table.topY + ballMetrics.ballRadius + NEAR_CUSHION_AREA
                && vy > 0) {
            vy -= NEAR_CUSHION_ACC / phy.calculationsPerSecSqr;
        } else if (y < table.botY - ballMetrics.ballRadius
                && y >= table.botY - ballMetrics.ballRadius - NEAR_CUSHION_AREA
                && vy < 0) {
            vy += NEAR_CUSHION_ACC / phy.calculationsPerSecSqr;
        }
    }

    public boolean canDraw() {
        return !isPotted() || msRemainInPocket > 0;
    }

    /**
     * @return 未碰撞0，碰撞但不是最大的一次1，最大碰撞2
     */
    private int oneFrameInPocket(Phy phy) {
        double pocketRange = pottedPocket.graphicalRadius - values.ball.ballRadius;
        double nextDt = predictedDtToPoint(pottedPocket.graphicalCenter);
        double curDt = currentDtToPoint(pottedPocket.graphicalCenter);
        int rtn = 0;
//        System.out.printf("%f, %f");
        if (nextDt > pocketRange
                && nextDt > curDt) {
            rtn = 1;
            double[] ballDir = new double[]{vx, vy};
            double[] ballAwayFromPocketCenter = new double[]{
                    nextX - pottedPocket.graphicalCenter[0],
                    nextY - pottedPocket.graphicalCenter[1]
            };
//            
//            double[] pocketBottomDir = new double[]{
//                    -pottedPocket.facingDir[0], 
//                    -pottedPocket.facingDir[1]
//            };
//            double proj = Algebra.projectionLengthOn(pocketBottomDir, ballDir);  // 球的方向往袋底方向的投影
            double proj = Algebra.projectionLengthOn(ballAwayFromPocketCenter, ballDir);  // 球的方向往袋底方向的投影
            double hitSpeed = proj * phy.calculationsPerSec;
            if (hitSpeed > maxInPocketSpeed) {
                maxInPocketSpeed = hitSpeed;
                rtn = 2;
            }

            innerBounce(pottedPocket.graphicalCenter, 0.6);
        }
//        tryEnterGravityArea(phy, pottedPocket.graphicalCenter, pottedPocket.isMid);
        x = nextX;
        y = nextY;
        nextX = x + vx;
        nextY = y + vy;
        return rtn;
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

    public int tryFrameInPocket(Phy phy) {
        if (isPotted()) {
            if (msRemainInPocket > 0) {
                msRemainInPocket -= phy.calculateMs;

                int stat = oneFrameInPocket(phy);
                if (getSpeedPerSecond(phy) < 100) {
                    // 球已经停了，别放了
                    msRemainInPocket = 0;
                    pot();
                    return 0;
                }

                return stat;
            } else {
                msRemainInPocket = 0;
                pot();
                return 0;
            }
        } else {
            return 0;
        }
    }

    public void naturalPot(long remainMs) {
        msRemainInPocket = remainMs;
        setPotted(true);

        for (Pocket pocket : table.pockets) {
            double dt = pocket.fallRadius - values.ball.ballRadius;
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

        double sideSpinEffectMul = Math.pow(mag / Values.MAX_POWER_SPEED, 0.8) * 1.25;
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

    protected double cushionBounceFactor(Phy phy, double[] direction, boolean isEndCushion) {
        double base = phy.cloth.smoothness.cushionBounceFactor;

        double variable;
        if (isEndCushion) {
            variable = Math.abs(direction[0]);
        } else {
            variable = Math.abs(direction[1]);
        }
        return Algebra.shiftRangeSafe(0, 1, 1, base, variable);
    }

    /**
     * 该方法不检测袋口
     */
    protected Cushion tryHitWall(Phy phy) {
        if (nextX < values.ball.ballRadius + table.leftX ||
                nextX >= table.rightX - values.ball.ballRadius) {
            // 顶库(屏幕两边)

            double[] direction = Algebra.unitVector(vx, vy);
            // 先减速，算是对时间复杂度的一种妥协
            double bounceFactor = cushionBounceFactor(phy, direction, true);
            vx *= table.wallBounceRatio * bounceFactor;
            vy *= table.wallBounceRatio * bounceFactor;

            boolean isLeft = nextX < values.table.midX;
            Cushion.EdgeCushion cushion = isLeft ? table.leftCushion : table.rightCushion;
            applySpin(cushion.getNormal(), cushion.getVector(), phy, 1.0);

            double effectiveAcc = -bounceAcc(phy, vx);
            double nFrames = getNFramesInCushion(vx, effectiveAcc);

            double[] hitCushionPos = getCushionHitPos(cushion.getPosition());

            double leaveY = hitCushionPos[1] + nFrames * vy;
            double hSpeedLoss = values.table.cushionPowerSpinFactor *
                    Math.abs(direction[0]) *
                    (getSpeedPerSecond(phy) / Values.MAX_POWER_SPEED) * 0.5;
            // 撞库撞出来的塞
            double sideSpinChangeFactor = Algebra.projectionLengthOn(cushion.getVector(), new double[]{vx, vy});
            double sideSpinChange = sideSpinChangeFactor * values.table.cushionPowerSpinFactor * CUSHION_COLLISION_SPIN_FACTOR;
            double bouncedSideSpin;
            if (sideSpin * 3.14 > sideSpinChangeFactor) {
                bouncedSideSpin = sideSpin;
            } else {
                bouncedSideSpin = sideSpin + sideSpinChange;
            }

            currentBounce = new CushionBounce(
                    effectiveAcc,
                    0,
                    phy.accelerationMultiplier());
            ((CushionBounce) currentBounce).setDesiredLeavePos(
                    hitCushionPos[0],
                    leaveY,
                    -vx,
                    vy * (1 - hSpeedLoss),
//                    vy,
                    bouncedSideSpin);
            return cushion;
        }
        if (nextY < values.ball.ballRadius + table.topY ||
                nextY >= table.botY - values.ball.ballRadius) {
            // 边库
            double[] direction = Algebra.unitVector(vx, vy);
            double bounceFactor = cushionBounceFactor(phy, direction, false);
            vx *= table.wallBounceRatio * bounceFactor;
            vy *= table.wallBounceRatio * bounceFactor;

            boolean isTop = nextY < table.midY;
            boolean isLeft = nextX < values.table.midX;
            Cushion.EdgeCushion cushion = isTop ? (
                    isLeft ? values.table.topLeftCushion : values.table.topRightCushion)
                    : (isLeft ? values.table.botLeftCushion : values.table.botRightCushion);
            applySpin(cushion.getNormal(), cushion.getVector(), phy, 1.0);

            double effectiveAcc = -bounceAcc(phy, vy);
            double nFrames = getNFramesInCushion(vy, effectiveAcc);

            double[] hitCushionPos = getCushionHitPos(cushion.getPosition());

            double leaveX = hitCushionPos[0] + nFrames * vx;
            double hSpeedLoss = values.table.cushionPowerSpinFactor *
                    Math.abs(direction[1]) *
                    (getSpeedPerSecond(phy) / Values.MAX_POWER_SPEED) * 0.5;
            // 撞库撞出来的塞
            double sideSpinChangeFactor = Algebra.projectionLengthOn(cushion.getVector(), new double[]{vx, vy});
            double sideSpinChange = sideSpinChangeFactor * values.table.cushionPowerSpinFactor * CUSHION_COLLISION_SPIN_FACTOR;
            double bouncedSideSpin;
            if (sideSpin * 3.14 > sideSpinChangeFactor) {
                bouncedSideSpin = sideSpin;
            } else {
                bouncedSideSpin = sideSpin + sideSpinChange;
            }

            currentBounce = new CushionBounce(
                    0,
                    effectiveAcc,
                    phy.accelerationMultiplier());
            ((CushionBounce) currentBounce).setDesiredLeavePos(
                    leaveX,
                    hitCushionPos[1],
                    vx * (1 - hSpeedLoss),
//                    vx,
                    -vy,
                    bouncedSideSpin);
            return cushion;
        }
        return null;
    }

    /**
     * 返回:
     * 0: 真的没有三颗球撞一起（包括没有球碰撞和纯二球碰撞）
     * 1: 发生了可以处理的三球碰撞
     * 2: 发生了无法处理的三球碰撞
     */
    int tryHitTwoBalls(Game<?, ?> game, Ball ball1, Ball ball2, Phy phy) {
        if (this.isNotMoving()) {
            if (ball1.isNotMoving()) {
                if (ball2.isNotMoving()) {
                    return 0;  // 三颗球都没动
                } else {
                    return ball2.tryHitTwoBalls(game, this, ball1, phy);
                }
            } else {
                if (ball2.isNotMoving()) {
                    return ball1.tryHitTwoBalls(game, this, ball2, phy);
                } else {
                    if (isHitting(ball1, phy)) {
                        if (isHitting(ball2, phy)) {
                            threeBallHitCore(game, ball1, ball2, phy);
                            return 1;
                        }
                    }
                    return 0;
                }
            }
        } else {
            if (ball1.isNotMoving() && ball2.isNotMoving()) {
                // this 去撞另外两颗
                if (isHitting(ball1, phy) && isHitting(ball2, phy)) {

                    threeBallHitCore(game, ball1, ball2, phy);

                    return 1;
                } else {
                    return 0;  // 这三颗球没有贴在一起
                }
            } else {
                if (isHitting(ball1, phy)) {
                    if (isHitting(ball2, phy)) {
                        threeBallHitCore(game, ball1, ball2, phy);
                        return 1;
                    }
                }
                return 0;
            }
        }
    }

    private void threeBallHitCore(Game<?, ?> game, Ball ball1, Ball ball2, Phy phy) {
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
            tryHitBall(game, ball1, false, false, phy);
            if (isHitting(ball2, phy)) tryHitBall(game, ball2, false, false, phy);
        } else {
            tryHitBall(game, ball2, false, false, phy);
            if (isHitting(ball1, phy)) tryHitBall(game, ball1, false, false, phy);
        }
    }

    boolean isHitting(Ball other, Phy phy) {
        double lastDt = currentDtTo(other);
        double nextDt = predictedDtTo(other);
        return nextDt < values.ball.ballDiameter && nextDt < lastDt;
    }

    private static double[] getExactCollisionPoint(Ball a, double x1, double y1,
                                                   Ball b, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dvx = a.vx - b.vx;
        double dvy = a.vy - b.vy;
        double r = a.radius + b.radius;

        double aCoeff = dvx * dvx + dvy * dvy;
        double bCoeff = 2 * (dx * dvx + dy * dvy);
        double cCoeff = dx * dx + dy * dy - r * r;

        double discriminant = bCoeff * bCoeff - 4 * aCoeff * cCoeff;

        if (discriminant < 0 || aCoeff == 0) {
            return null; // No collision or invalid motion
        }

        double sqrtD = Math.sqrt(discriminant);
        double t1 = (-bCoeff - sqrtD) / (2 * aCoeff);
        double t2 = (-bCoeff + sqrtD) / (2 * aCoeff);
        double t = Math.min(t1, t2);

        if (t < 0) return null; // Collision happens in the past

        double ax = x1 + a.vx * t;
        double ay = y1 + a.vy * t;
        double bx = x2 + b.vx * t;
        double by = y2 + b.vy * t;

        return new double[]{ax, ay, bx, by, t}; // Positions at collision and time
    }

    private double[] findApproxCollisionPoint(double x1, double y1,
                                              Ball ball,
                                              double x2, double y2) {

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
        return new double[]{x1, y1, x2, y2};
    }

    void twoMovingBallsHitCore(Game<?, ?> game, Ball ball, Phy phy, boolean considerGearSpin) {
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

        if (Algebra.distanceToPoint(x1, y1, x2, y2) <= values.ball.ballDiameter) {
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
            return;
        }

        this.lastCollisionRelSpeed = Math.hypot(vx - ball.vx, vy - ball.vy);
        ball.lastCollisionRelSpeed = this.lastCollisionRelSpeed;

        // 离岸位置发生改变，又懒得重新算了
        clearBounceDesiredLeavePos();
        ball.clearBounceDesiredLeavePos();

        double[] exactColPoint = getExactCollisionPoint(this, x1, y1,
                ball, x2, y2);
        if (exactColPoint == null) {
            System.err.println("Cannot find exact collision point!");
            double[] approxColPoint = findApproxCollisionPoint(x1, y1, ball, x2, y2);
            x1 = approxColPoint[0];
            y1 = approxColPoint[1];
            x2 = approxColPoint[2];
            y2 = approxColPoint[3];
        } else {
            x1 = exactColPoint[0];
            y1 = exactColPoint[1];
            x2 = exactColPoint[2];
            y2 = exactColPoint[3];
        }

        if (!phy.isPrediction) {
            // AI考虑进攻时并不会clone目标球
            // 因此我们不希望AI在模拟时触发任何移动目标球的行为
            this.x = x1;
            this.y = y1;
            ball.x = x2;
            ball.y = y2;
        }

        // fixme: 固定开球，目前有bug不能用
//        if (game != null && !phy.isPrediction && game.ballHeapIntact &&
//                game.getGameValues().hasSubRuleDetail(SubRule.Detail.PAPER_BREAK) &&
//                (game.isBallPlacedInHeap(this) || game.isBallPlacedInHeap(ball))) {
//            game.paperBreak(phy);
//            System.err.println("Paper Break!");
//            return;
//        }

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
        double thinnessCos = Math.cos(Algebra.HALF_PI - collisionThickness);
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

        double frictionStrength = values.ball.frictionRatio;
        double spinProj = 0.0;
        boolean wasBallStopped = false;
        if (ball.vx == 0 && ball.vy == 0) {
            wasBallStopped = true;
            spinProj = Algebra.projectionLengthOn(thisV,
                    new double[]{this.xSpin, this.ySpin}) * phy.calculationsPerSec / 1500;  // 旋转方向在这颗球原本前进方向上的投影
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
            // 侧旋的传递
            double gearPassFactor = 0.15 * frictionStrength;
            double passRate = Math.cos(Math.abs(collisionThickness));  // 越厚传得越多。
//            double passRate = 1 - Math.abs(thinnessCos);  // 越厚传得越多，而且初始衰减要多
            double speedPassRate = Math.abs(this.sideSpin) / relSpeed;  // 球速越慢，塞越大，传得越多
            double passPercentage = gearPassFactor * passRate * speedPassRate;
            passPercentage = Math.min(passPercentage, MAXIMUM_SPIN_PASS);  // 最多传1/4
            double passed = this.sideSpin * passPercentage;

            this.sideSpin -= passed;  // 自己的塞会减少，动量守恒嘛
            ball.sideSpin -= passed;  // 右塞传到球上就是左塞了

            // 前后旋转的传递
            // 这里并没有考虑自身的旋转损失，因为可以理解为已经在其他地方实现了这个效果了
            double factor = spinProj >= 0.0 ? 0.18 : 0.36;  // 只希望强烈的前向传递
//            System.out.println("Fact: " + factor + ", proj: " + spinProj);
            factor *= frictionStrength;
            double effSpinMag = Algebra.projectionLengthOn(thisOut, new double[]{xSpin, ySpin});
            double passedSpin = effSpinMag * factor * passRate;
            ball.xSpin -= ballOut[0] * passedSpin;
            ball.ySpin -= ballOut[1] * passedSpin;

//            double xSpinPassed = this.xSpin * factor * passRate;
//            double ySpinPassed = this.ySpin * factor * passRate;
//            ball.xSpin -= xSpinPassed;
//            ball.ySpin -= ySpinPassed;

            if (gearOffsetEnabled) {
                // 相当于整个坐标系往一个方向扭一点点
                double angularRate = 0.15 * frictionStrength;
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
                if (wasBallStopped) {
                    // 投掷效应
                    double powerGear = Math.min(1.0,
                            totalSpeed / GEAR_EFFECT_MAX_POWER / Values.MAX_POWER_SPEED *
                                    values.ball.ballWeightRatio);  // 30的力就没有效应了(高低杆要打出30的球速，起码要40的力)
                    double throwEffect = (1 - powerGear) * frictionStrength * 0.05;
//                    System.out.println("power gear: " + powerGear);
                    double sideSpinFactor = -this.sideSpin * 0.5;
                    // 理解为加顺赛抵消投掷效应
                    double throwStrength;
                    if (thinnessCos * sideSpinFactor > 0) {
                        // 顺赛
                        throwStrength = Math.max(0, Math.abs(thinnessCos - sideSpinFactor));
                    } else {
                        throwStrength = Math.abs(thinnessCos);
                    }
                    throwStrength *= throwEffect;
//                    System.out.println("Throw: " + throwStrength + ", thick cos: " + thinnessCos + ", side spin: " + this.sideSpin);
                    double[] directionOffset = Algebra.vectorScale(thisV, throwStrength);

                    ball.vx += directionOffset[0];
                    ball.vy += directionOffset[1];
                }
            }

            // 薄边造成的侧旋
            double gearStrengthFactor = 0.15 * frictionStrength;
            double angleSpinChange = gearStrengthFactor * thinnessCos;
            double spinChange = angleSpinChange * relSpeed;
            this.sideSpin += spinChange;
            ball.sideSpin -= spinChange;

//            // 薄边造成的纵向旋转
//            double[] thisOrth = new double[]{-thisOut[1], thisOut[0]};
//            // todo: phy.calculationsPerSec
//            double thisXySpinChange = angleSpinChange *
//                    (Values.MAX_SPIN_SPEED / Values.MAX_SIDE_SPIN_SPEED) * 0.5;
//            double thisSpinXChange = thisXySpinChange * thisOrth[0];
//            double thisSpinYChange = thisXySpinChange * thisOrth[1];
//            this.xSpin += thisSpinXChange;
//            this.ySpin += thisSpinYChange;
//            // ball也该变，但会影响目标球走势，AI不会瞄，辅助瞄准线也做不了
        }

        // update
        nextX = x1 + vx;
        nextY = y1 + vy;
        ball.nextX = x2 + ball.vx;
        ball.nextY = y2 + ball.vy;

        // 弹走了来再减速
        vx *= values.ball.ballBounceRatio;
        vy *= values.ball.ballBounceRatio;
        ball.vx *= values.ball.ballBounceRatio;
        ball.vy *= values.ball.ballBounceRatio;

        if (Algebra.distanceToPoint(nextX, nextY, ball.nextX, ball.nextY) < Algebra.distanceToPoint(x, y, ball.x, ball.y)) {
            if (!phy.isPrediction)
                System.err.printf("Ball %d@%f,%f->%f,%f and ball %d@%f,%f->%f,%f not collide properly\n",
                        getValue(), x, y, nextX, nextY, ball.getValue(), ball.x, ball.y, ball.nextX, ball.nextY);
            if (!phy.isPrediction) System.err.printf("Last dt: %f, new dt: %f\n",
//                    phyRounds,
                    Algebra.distanceToPoint(x, y, ball.x, ball.y),
                    Algebra.distanceToPoint(nextX, nextY, ball.nextX, ball.nextY));
        }
    }

    boolean tryHitBall(Game<?, ?> game,
                       Ball ball, boolean checkMovingBall, boolean applyGearSpin, Phy phy) {
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

            twoMovingBallsHitCore(game, ball, phy, applyGearSpin);

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
        maxInPocketSpeed = 0.0;
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

    public Color getTraceColor() {
        return traceColor;
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

    public double getLastCollisionRelSpeed() {
        return lastCollisionRelSpeed;
    }

    public double getMaxInPocketSpeed() {
        return maxInPocketSpeed;
    }
}
