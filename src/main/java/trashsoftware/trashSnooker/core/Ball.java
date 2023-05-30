package trashsoftware.trashSnooker.core;

import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.drawing.BallModel;

import java.util.Arrays;
import java.util.Random;

public abstract class Ball extends ObjectOnTable implements Comparable<Ball>, Cloneable {
    public static final double MAX_GEAR_EFFECT = 0.2;  // 齿轮效应造成的最严重分离角损耗
    private static boolean enableGearOffset = true;  // 齿轮效应造成的球线路偏差
    private static final Random ERROR_GENERATOR = new Random();
    private static final double[] SIDE_CUSHION_VEC = {1.0, 0.0};
    private static final double[] TOP_BOT_CUSHION_VEC = {0.0, 1.0};
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
    private Ball justHit;
    
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
        double ss = sideSpin * 5.0;
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

    protected void hitHoleArcArea(double[] arcXY, Phy phy, double arcRadius) {

        // 一般来说袋角的弹性没库边好
//        vx *= table.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor * 0.9;
//        vy *= table.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor * 0.9;

        double[] collisionNormal = new double[]{arcXY[0] - x, arcXY[1] - y};  // 切线的法向量
        applySpinsWhenHitCushion(phy, collisionNormal);
        super.hitHoleArcArea(arcXY, phy, arcRadius);
    }

    protected void hitHoleLineArea(double[] lineNormalVec, Phy phy) {
        applySpinsWhenHitCushion(phy, lineNormalVec);
        super.hitHoleLineArea(lineNormalVec, phy);
//        vx *= table.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor * 0.95;
//        vy *= table.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor * 0.95;
    }
    
    private double calculateEffectiveSideSpin(Phy phy, double[] cushionNormalVec) {
        double[] vec = new double[]{vx, vy};
        // 库的切线方向的投影长度，意思是砸的深度，越深效果越强
        double vertical = Algebra.projectionLengthOn(cushionNormalVec, vec);
        double mag = Math.abs(vertical) * phy.calculationsPerSec;

        double sideSpinEffectMul = Math.pow(mag / Values.MAX_POWER_SPEED, 0.7);
//        System.out.println(mag + " " + sideSpinEffectMul);
        return sideSpin * sideSpinEffectMul;
    }

    /**
     * 碰库时的旋转:
     * 1. 施加侧旋的效果
     * 2. 更新所有旋转效果
     * <p>
     * 需在更新了vx和vy之后调用
     *
     * @param phy              物理
     * @param cushionNormalVec 撞的库的法向量。比如说边库应是(1,0)或(-1,0)
     */
    private void applySpinsWhenHitCushion(Phy phy, double[] cushionNormalVec) {
        double sideSpinStrength = calculateEffectiveSideSpin(phy, cushionNormalVec);
        double effectiveSideSpin = sideSpinStrength * table.wallSpinEffectRatio;

        // todo: 暂时修不好，大概还是因为左手/右手定理的问题
//        double[] cushionVec = Algebra.normalVector(cushionNormalVec);
//        double horizontal = Algebra.projectionLengthOn(cushionVec, vec);
//        
//        double[] afterSpin = new double[]{horizontal + effectiveSideSpin, vertical};
//        double newVx = Algebra.projectionLengthOn(Algebra.X_AXIS, afterSpin);
//        double newVy = Algebra.projectionLengthOn(Algebra.Y_AXIS, afterSpin);
//        
//        vx = newVx;
//        vy = newVy;

        // todo: 真的算，而不是用if。目前没考虑袋角
        if (Arrays.equals(cushionNormalVec, SIDE_CUSHION_VEC)) {
            if (vx < 0) {
                vy -= effectiveSideSpin;
            } else {
                vy += effectiveSideSpin;
            }
            xSpin *= (table.wallSpinPreserveRatio * 0.8);
            ySpin *= table.wallSpinPreserveRatio;
        } else if (Arrays.equals(cushionNormalVec, TOP_BOT_CUSHION_VEC)) {
            if (vy < 0) {
                vx += effectiveSideSpin;
            } else {
                vx -= effectiveSideSpin;
            }
            xSpin *= table.wallSpinPreserveRatio;
            ySpin *= (table.wallSpinPreserveRatio * 0.8);
        } else {
            xSpin *= (table.wallSpinPreserveRatio * 0.9);
            ySpin *= (table.wallSpinPreserveRatio * 0.9);
        }

        sideSpin -= effectiveSideSpin;
        sideSpin *= table.wallSpinPreserveRatio;
//        sideSpin *= table.wallSpinPreserveRatio / sideSpinEffectMul;

        // todo: 吃库之后侧旋
    }

    /**
     * 该方法不检测袋口
     */
    protected boolean tryHitWall(Phy phy) {
        if (nextX < values.ball.ballRadius + table.leftX ||
                nextX >= table.rightX - values.ball.ballRadius) {
            // 顶库

            // 先减速，算是对时间复杂度的一种妥协
            vx *= table.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor;
            vy *= table.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor;
            applySpinsWhenHitCushion(phy, SIDE_CUSHION_VEC);

            double effectiveAcc = -bounceAcc(phy, vx);
            double nFrames = vx / -effectiveAcc * 2;
            if (!phy.isPrediction && App.PRINT_DEBUG) System.out.println("predict " + nFrames);
            currentBounce = new CushionBounce(
                    effectiveAcc,
                    0);
            double leaveY = y + nFrames * vy;
            ((CushionBounce) currentBounce).setDesiredLeavePos(x, leaveY, -vx, vy);

            // todo: 以后这个可以集成到bounce里，甚至还更真实
//            applySpinsWhenHitCushion(phy, SIDE_CUSHION_VEC);
//            rotateDeg = 0.0;
            return true;
        }
        if (nextY < values.ball.ballRadius + table.topY ||
                nextY >= table.botY - values.ball.ballRadius) {
            // 边库
            vx *= table.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor;
            vy *= table.wallBounceRatio * phy.cloth.smoothness.cushionBounceFactor;
            applySpinsWhenHitCushion(phy, TOP_BOT_CUSHION_VEC);

            double effectiveAcc = -bounceAcc(phy, vy);
            double nFrames = vy / -effectiveAcc * 2;
            currentBounce = new CushionBounce(
                    0,
                    effectiveAcc);
            double leaveX = x + nFrames * vx;
            ((CushionBounce) currentBounce).setDesiredLeavePos(leaveX, y, vx, -vy);
//            if (!phy.isPrediction) {
//                System.out.println(currentBounce.accX + " " + currentBounce.accY);
//            }
//            System.out.println("Hit wall!======================");
//            rotateDeg = 0.0;
            return true;
        }
        return false;
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
                    return 2;  // ball1、ball2 都在动，无法处理
                }
            }
        } else {
            if (ball1.isNotMoving() && ball2.isNotMoving()) {
                // this 去撞另外两颗
                double dt1, dt2;
                if (((dt1 = predictedDtTo(ball1)) < values.ball.ballDiameter && currentDtTo(ball1) > dt1
                        && justHit != ball1 && ball1.justHit != this
                ) && ((dt2 = predictedDtTo(ball2)) < values.ball.ballDiameter && currentDtTo(ball2) > dt2
                        && justHit != ball2 && ball2.justHit != this
                )) {
                    System.out.println("Hit two static balls!=====================");
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
                        tryHitBall(ball2, false, false, phy);
                    } else {
                        tryHitBall(ball2, false, false, phy);
                        tryHitBall(ball1, false, false, phy);
                    }

                    return 1;
                } else {
                    return 0;  // 这三颗球没有贴在一起
                }
            } else {
//                System.err.println("Both balls are moving");
                return 2;  // this 和 ball1、ball2 中的至少一颗都在动，无法处理
            }
        }
    }

    void twoMovingBallsHitCore(Ball ball, Phy phy, boolean considerGearSpin) {
        // 离岸位置发生改变，又懒得重新算了
        clearBounceDesiredLeavePos();
        ball.clearBounceDesiredLeavePos();

        // 提高精确度
        double x1 = x;
        double y1 = y;
        double dx1 = vx / Values.DETAILED_PHYSICAL;
        double dy1 = vy / Values.DETAILED_PHYSICAL;

        double x2 = ball.x;
        double y2 = ball.y;
        double dx2 = ball.vx / Values.DETAILED_PHYSICAL;
        double dy2 = ball.vy / Values.DETAILED_PHYSICAL;

        for (int i = 0; i < Values.DETAILED_PHYSICAL; ++i) {
            if (Algebra.distanceToPoint(x1, y1, x2, y2) < values.ball.ballDiameter) {
                break;
            }
            x1 += dx1;
            y1 += dy1;
            x2 += dx2;
            y2 += dy2;
        }
        
        this.x = x1;
        this.y = y1;
        ball.x = x2;
        ball.y = y2;
        
        this.lastCollisionX = x1;
        this.lastCollisionY = y1;
        ball.lastCollisionX = x2;
        ball.lastCollisionY = y2;

        double[] thisV = new double[]{vx, vy};
        double[] ballV = new double[]{ball.vx, ball.vy};

        double[] normVec = new double[]{x1 - x2, y1 - y2};  // 两球连线=法线
        double[] tangentVec = Algebra.normalVector(normVec);  // 切线
        
        // 为齿轮效应计算做准备
        double collisionThickness = Algebra.thetaBetweenVectors(thisV, tangentVec);  // 90度是正撞，0度是球1擦球2的右边，180度是擦左边
        collisionThickness -= Algebra.HALF_PI;  // 减去90度，正撞为0，擦右边为-90
//        System.out.println("Thick: " + Math.toDegrees(collisionThickness));
        double relSpeed = Math.hypot(this.vx - ball.vx, this.vy - ball.vy);
        double totalSpeed = (Math.hypot(this.vx, this.vy) + Math.hypot(ball.vx, ball.vy)) * phy.calculationsPerSec;

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

        double thisOutHor = thisHorV;
        double thisOutVer = ballVerV;
        double ballOutHor = ballHorV;
        double ballOutVer = thisVerV;
        if (ball.vx == 0 && ball.vy == 0) {  // 两颗动球碰撞考虑齿轮效应太麻烦了
            double powerGear = Math.min(1.0, totalSpeed / 0.30 / Values.MAX_POWER_SPEED * values.ball.ballWeightRatio);  // 30的力就没有效应了(高低杆要打出30的球速，起码要45的力)
            double gearRemain = (1 - powerGear) * MAX_GEAR_EFFECT;
            double gearEffect = 1 - gearRemain;

            double spinProj = Algebra.projectionLengthOn(thisV,
                    new double[]{this.xSpin, this.ySpin}) * phy.calculationsPerSec / 1500;  // 旋转方向在这颗球原本前进方向上的投影

            gearRemain *= spinProj;

            // todo: 1.还没考虑旋转 2.目标球的偏移由于AI算不了而取消了
//            double transformed = thisOutHor * (1 - gearEffect);

            ballOutVer *= gearEffect;
            thisOutVer = thisVerV * gearRemain;
        }

        // 碰撞后，两球平行于切线的速率不变，垂直于切线的速率互换
        double[] thisOut = Algebra.antiProjection(tangentVec,
                new double[]{thisOutHor, thisOutVer});
        double[] ballOut = Algebra.antiProjection(tangentVec,
                new double[]{ballOutHor, ballOutVer});

//        this.vx = thisOut[0] * values.ball.ballBounceRatio;
//        this.vy = thisOut[1] * values.ball.ballBounceRatio;
//        ball.vx = ballOut[0] * values.ball.ballBounceRatio;
//        ball.vy = ballOut[1] * values.ball.ballBounceRatio;
        this.vx = thisOut[0];
        this.vy = thisOut[1];
        ball.vx = ballOut[0];
        ball.vy = ballOut[1];

        // 齿轮效应的旋转传递
        double thisOutSpeed = Math.hypot(this.vx, this.vy);
        double ballOutSpeed = Math.hypot(ball.vx, ball.vy);
        if (considerGearSpin && relSpeed != 0.0 && (thisOutSpeed != 0.0 || ballOutSpeed != 0.0)) {
            double gearPassFactor = 0.2;
            double passRate = Math.cos(Math.abs(collisionThickness));  // 越厚传得越多。
            double speedPassRate = Math.abs(this.sideSpin) / relSpeed;  // 球速越慢，塞越大，传得越多
            double passed = this.sideSpin * gearPassFactor * passRate * speedPassRate;
            this.sideSpin -= passed;  // 自己的塞会减少，动量守恒嘛
            ball.sideSpin -= passed;  // 右塞传到球上就是左塞了

            if (enableGearOffset) {
                // 相当于整个坐标系往一个方向扭一点点
                double angularRate = 0.18;
                double thisOutAng = Algebra.thetaOf(this.vx, this.vy);
                double[] thisOutGeared = Algebra.unitVectorOfAngle(thisOutAng - passed * angularRate / thisOutSpeed);
                this.vx = thisOutSpeed * thisOutGeared[0];
                this.vy = thisOutSpeed * thisOutGeared[1];
                
                double ballOutAng = Algebra.thetaOf(ball.vx, ball.vy);
                double[] ballOutGeared = Algebra.unitVectorOfAngle(ballOutAng - passed * angularRate / ballOutSpeed);
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
        
        if (Algebra.distanceToPoint(nextX, nextY, ball.nextX, ball.nextY) < values.ball.ballDiameter) {
            System.err.printf("Ball %d@%f,%f->%f,%f and ball %d@%f,%f->%f,%f not collide properly\n",
                    getValue(), x, y, nextX, nextY, ball.getValue(), ball.x, ball.y, ball.nextX, ball.nextY);
            System.err.printf("Last dt: %f, new dt: %f\n",
                    Algebra.distanceToPoint(x, y, ball.x, ball.y),
                    Algebra.distanceToPoint(nextX, nextY, ball.nextX, ball.nextY));
        }
    }

    boolean tryHitBall(Ball ball, boolean checkMovingBall, boolean applyGearSpin, Phy phy) {
        double dt = predictedDtTo(ball);
        if (dt < values.ball.ballDiameter
                && currentDtTo(ball) > dt
                && justHit != ball && ball.justHit != this
        ) {
            if (this.isNotMoving()) {
                if (ball.isNotMoving()) {
                    if (checkMovingBall) {
                        throw new RuntimeException("他妈的两颗静止的球拿头撞？");
                    } else {
                        System.err.println("复杂情况，不管了，有bug就有bug吧");
                        return false;
                    }
                } else {
                    return ball.tryHitBall(this, checkMovingBall, applyGearSpin, phy);
                }
            }
            if (!ball.isNotMoving()) {
                if (!checkMovingBall) return false;
            }
            twoMovingBallsHitCore(ball, phy, applyGearSpin);

            justHit = ball;
            ball.justHit = this;
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
        justHit = null;
        frameDegChange = 0.0;
    }

    protected void prepareMove(Phy phy) {
        super.prepareMove(phy);
        justHit = null;
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

    public static boolean isEnableGearOffset() {
        return enableGearOffset;
    }
    
    public static void enableGearOffset() {
        enableGearOffset = true;
    }

    public static void disableGearOffset() {
        enableGearOffset = false;
    }

    public double getLastCollisionX() {
        return lastCollisionX;
    }

    public double getLastCollisionY() {
        return lastCollisionY;
    }
}
