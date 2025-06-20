package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.core.metrics.Cushion;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.metrics.Pocket;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;
import trashsoftware.trashSnooker.core.phy.Phy;

import java.util.Objects;

public abstract class ObjectOnTable implements Cloneable {
    protected static final double GENERAL_BOUNCE_ACC = 0.35;
    protected final GameValues values;
    protected final TableMetrics table;
    protected final double radius;
    protected double distance;
    protected double x, y;
    protected double nextX, nextY;
    protected double vx, vy;  // unit: mm/(sec/frameRate)

    protected Bounce currentBounce;

    public ObjectOnTable(GameValues values, double radius) {
        this.values = values;
        this.table = values.table;
        this.radius = radius;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        ObjectOnTable oot = (ObjectOnTable) super.clone();
        if (currentBounce != null) {
            oot.currentBounce = (Bounce) currentBounce.clone();
        }
        return oot;
    }

    public double getRadius() {
        return radius;
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

    public void setXY(double x, double y) {
        setX(x);
        setY(y);
    }
    
    public double[] getPositionArray() {
        return new double[]{x, y};
    }

    public void setVx(double vx) {
        this.vx = vx;
    }

    public void setVy(double vy) {
        this.vy = vy;
    }
    
    public void setVelocity(double[] vel) {
        setVx(vel[0]);
        setVy(vel[1]);
    }
    
    public void setPosition(double[] pos) {
        setX(pos[0]);
        setY(pos[1]);
    }

    protected double getSpeed() {
        return Math.hypot(vx, vy);
    }

    public double getSpeedPerSecond(Phy phy) {
        return getSpeed() * phy.calculationsPerSec;
    }

    protected double getDistanceMoved() {
        return distance;
    }

    protected void prepareMove(Phy phy) {
        nextX = x + vx;
        nextY = y + vy;
    }

    protected abstract void normalMove(Phy phy);

    protected double currentDtTo(Ball ball) {
        return Algebra.distanceToPoint(x, y, ball.x, ball.y);
    }

    protected double predictedDtTo(Ball ball) {
        return Algebra.distanceToPoint(nextX, nextY, ball.nextX, ball.nextY);
    }

    protected double currentDtToLine(double[][] line) {
        return Algebra.distanceToLine(x, y, line[0], line[1]);
    }

    protected double predictedDtToLine(double[][] line) {
        return Algebra.distanceToLine(nextX, nextY, line[0], line[1]);
    }

    protected double currentDtToPoint(double[] point) {
        return Algebra.distanceToPoint(x, y, point[0], point[1]);
    }

    protected double predictedDtToPoint(double[] point) {
        return predictedDtToPoint(point[0], point[1]);
    }

    protected double predictedDtToPoint(double px, double py) {
        return Algebra.distanceToPoint(nextX, nextY, px, py);
    }

    protected double midHolePowerFactor(Phy phy) {
        return 1;
    }

    public void clearMovement() {
        vx = 0.0;
        vy = 0.0;
        distance = 0.0;
        currentBounce = null;
    }

    protected void clearBounceDesiredLeavePos() {
        if (currentBounce != null) {
//            System.out.println("Cleared bounce desired leave pos!");
            currentBounce.clearDesireLeavePos();
        }
    }

    protected void processBounce(boolean print) {
        boolean notDestroy = currentBounce.oneFrame();

        if (!notDestroy) {
            if (print)
                System.out.println(print + " Bounce lasts for " + currentBounce.framesCount + " frames");
            currentBounce.leave();
            currentBounce = null;
        }
    }

    protected boolean willPot(Phy phy) {
//        if (x < table.leftClothX || 
//                x >= table.rightClothX || 
//                y < table.topClothY || 
//                y >= table.botClothY) {
//            // 出台了
//            return true;
//        }
        
        for (Pocket pocket : table.pockets) {
            double room = pocket.fallRadius - values.ball.ballRadius;
            if (predictedDtToPoint(pocket.fallCenter) < room) return true;
        }
        return false;
    }

    protected void hitHoleArcArea(double[] arcXY, Phy phy, double arcRadius) {
        if (currentBounce != null) {
            System.err.println("Current is bouncing!");
        }

        double[] hitPos = getArcHitPos(arcXY, arcRadius);

        // todo: 把难的袋口硬度加大，使大力更不容易zang进
        double speed = Math.hypot(vx, vy);
        double ballAngle = Algebra.thetaOf(vx, vy);  // 入射角与垂线的夹角
        double verticalAngle = Algebra.thetaOf(arcXY[0] - hitPos[0], arcXY[1] - hitPos[1]);
        double injectAngle = Algebra.normalizeAngle(ballAngle - verticalAngle);
        currentBounce = new ArcBounce(
                arcXY,
                bounceAcc(phy, speed),
                speed * 0.8,
                injectAngle,
                phy.accelerationMultiplier()
        );

        x += vx;
        y += vy;
    }

    protected void hitHoleLineArea(double[][] line, double[] lineNormalVec, Phy phy) {
//        double[] reflect = Algebra.symmetricVector(vx, vy, lineNormalVec[0], lineNormalVec[1]);
//        vx = -reflect[0];
//        vy = -reflect[1];

        double[] vv = new double[]{vx, vy};

        double[] unitNormal = Algebra.unitVector(lineNormalVec);
        double verticalSpeed = Algebra.projectionLengthOn(unitNormal, vv);

        currentBounce = new LineBounce(
                -unitNormal[0] *
                        verticalSpeed * phy.cloth.smoothness.cushionBounceFactor * GENERAL_BOUNCE_ACC,
                -unitNormal[1] *
                        verticalSpeed * phy.cloth.smoothness.cushionBounceFactor * GENERAL_BOUNCE_ACC,
                Math.hypot(vx, vy) * 0.9,
                phy.accelerationMultiplier()
        );
    }

    protected double getNFramesInCushion(double verticalSpeed, double acc) {
        return verticalSpeed / -acc * 2;
    }

    protected void tryEnterGravityArea(Phy phy, double[] holeXy, boolean isMidHole) {
        double xDiff = holeXy[0] - nextX;
        double yDiff = holeXy[1] - nextY;
        double dt = Math.hypot(xDiff, yDiff);

        double holeRadius = isMidHole ? 
                table.pocketDifficulty.midPocketFallRadius : 
                table.pocketDifficulty.cornerPocketFallRadius;
        double holeAndSlopeRadius = holeRadius +
                (isMidHole ?
                        table.midPocketGravityRadius :
                        table.cornerPocketGravityRadius);

        if (dt < holeAndSlopeRadius) {
            double pureHoleRadius = holeRadius - values.ball.ballRadius;
            
            double gravity = 9800;
            double[] supporter;
            if (dt <= pureHoleRadius) {
                // 已经完全进袋了，但是我们当袋底也有点角度
                supporter = new double[]{Algebra.HALF_SQRT2, Algebra.HALF_SQRT2};
            } else {
                double enteredDt = holeAndSlopeRadius - dt;
                double enterRatio = enteredDt / (holeAndSlopeRadius - pureHoleRadius);
                double angle = Math.acos(enterRatio);  // 球心与弧心连线 与 水平面 的夹角
                supporter = Algebra.unitVectorOfAngle(angle);
            }

            double accMag = supporter[0] * gravity;
            double resist = 0.0;  // 摩擦力
            accMag *= (1 - resist);
            
            accMag /= phy.calculationsPerSecSqr;

            double[] accVec = Algebra.unitVector(xDiff, yDiff);
            accVec[0] *= accMag;
            accVec[1] *= accMag;

            vx += accVec[0];
            vy += accVec[1];
        }
    }

    /**
     * 返回PLAY_MS下的反弹加速度
     */
    protected double bounceAcc(Phy phy, double verticalSpeed) {
        return verticalSpeed * phy.cloth.smoothness.cushionBounceFactor * GENERAL_BOUNCE_ACC;
    }

    /**
     * 检测是否撞击袋角或进入袋角区域。
     * 如果处于弹性中，返回{@code 3}。
     * 如果撞击袋角，返回{@code 2}且处理撞击。
     * 如果进入袋角区域但未发生撞击，返回{@code 1}。
     * 如未进入，返回{@code 0}
     */
    protected CushionHitResult tryHitHoleArea(Phy phy) {
        if (nextY < radius + table.topY) {
            if (nextX < table.midHoleAreaRightX && nextX >= table.midHoleAreaLeftX) {
                // 上方中袋在袋角范围内
                if (nextY > table.topMidArcMinY &&  // 进入袋里面的不算
                        predictedDtToPoint(table.topMidHoleLeftArcXy.getCenter()) < table.midArcRadius + radius &&
                        currentDtToPoint(table.topMidHoleLeftArcXy.getCenter()) >= table.midArcRadius + radius) {
                    // 击中上方中袋左侧
                    hitHoleArcArea(table.topMidHoleLeftArcXy.getCenter(), phy, table.midArcRadius);
                    return new CushionHitResult(table.topMidHoleLeftArcXy, 2);
                } else if (nextY > table.topMidArcMinY &&
                        predictedDtToPoint(table.topMidHoleRightArcXy.getCenter()) < table.midArcRadius + radius &&
                        currentDtToPoint(table.topMidHoleRightArcXy.getCenter()) >= table.midArcRadius + radius) {
                    // 击中上方中袋右侧
                    hitHoleArcArea(table.topMidHoleRightArcXy.getCenter(), phy, table.midArcRadius);
                    return new CushionHitResult(table.topMidHoleRightArcXy, 2);
                } else if (nextX >= table.midHoleLineLeftX && nextX < table.midHoleLineRightX) {
                    // 疑似上方中袋直线
                    Cushion.CushionLine line = table.topMidHoleLeftLine;
                    if (predictedDtToLine(line.getPosition()) < radius &&
                            currentDtToLine(line.getPosition()) >= radius) {
                        hitHoleLineArea(
                                line.getPosition(),
                                line.getNormal(),
                                phy);
                        return new CushionHitResult(line, 2);
                    }
                    line = table.topMidHoleRightLine;
                    if (predictedDtToLine(line.getPosition()) < radius &&
                            currentDtToLine(line.getPosition()) >= radius) {
                        hitHoleLineArea(
                                line.getPosition(),
                                line.getNormal(),
                                phy);
                        return new CushionHitResult(line, 2);
                    }

                    tryEnterGravityArea(phy, table.topMid.fallCenter, true);
                    normalMove(phy);
                    prepareMove(phy);
                    return new CushionHitResult(1);
                } else {

                    tryEnterGravityArea(phy, table.topMid.fallCenter, true);
                    normalMove(phy);
                    prepareMove(phy);
                    return new CushionHitResult(1);
                }
            }
        } else if (nextY >= table.botY - radius) {
            if (nextX < table.midHoleAreaRightX && nextX >= table.midHoleAreaLeftX) {
                // 下方中袋袋角范围内
                if (nextY <= table.botMidArcMaxY && 
                        predictedDtToPoint(table.botMidHoleLeftArcXy.getCenter()) < table.midArcRadius + radius &&
                        currentDtToPoint(table.botMidHoleLeftArcXy.getCenter()) >= table.midArcRadius + radius) {
                    // 击中下方中袋左侧
                    hitHoleArcArea(table.botMidHoleLeftArcXy.getCenter(), phy, table.midArcRadius);
                    return new CushionHitResult(table.botMidHoleLeftArcXy, 2);
                } else if (nextY <= table.botMidArcMaxY && 
                        predictedDtToPoint(table.botMidHoleRightArcXy.getCenter()) < table.midArcRadius + radius &&
                        currentDtToPoint(table.botMidHoleRightArcXy.getCenter()) >= table.midArcRadius + radius) {
                    // 击中下方中袋右侧
                    hitHoleArcArea(table.botMidHoleRightArcXy.getCenter(), phy, table.midArcRadius);
                    return new CushionHitResult(table.botMidHoleRightArcXy, 2);
                } else if (nextX >= table.midHoleLineLeftX && nextX < table.midHoleLineRightX) {
                    // 疑似下方中袋直线
                    Cushion.CushionLine line = table.botMidHoleLeftLine;
                    if (predictedDtToLine(line.getPosition()) < radius &&
                            currentDtToLine(line.getPosition()) >= radius) {
                        hitHoleLineArea(
                                line.getPosition(),
                                line.getNormal(),
                                phy);
                        return new CushionHitResult(line, 2);
                    }
                    line = table.botMidHoleRightLine;
                    if (predictedDtToLine(line.getPosition()) < radius &&
                            currentDtToLine(line.getPosition()) >= radius) {
                        hitHoleLineArea(
                                line.getPosition(),
                                line.getNormal(),
                                phy);
                        return new CushionHitResult(line, 2);
                    }

                    tryEnterGravityArea(phy, table.botMid.fallCenter, true);
                    normalMove(phy);
                    prepareMove(phy);
                    return new CushionHitResult(1);
                } else {

                    tryEnterGravityArea(phy, table.botMid.fallCenter, true);
                    normalMove(phy);
                    prepareMove(phy);
                    return new CushionHitResult(1);
                }
            }
        }

        // 底袋
        double[] probHole = null;
        double[] probPocketFallCenter = null;
        if (nextY < table.topCornerHoleAreaDownY) {
            if (nextX < table.leftCornerHoleAreaRightX) {
                // 左上底袋
                probHole = table.topLeft.fallCenter;
                probPocketFallCenter = table.topLeft.fallCenter;
            } else if (nextX >= table.rightCornerHoleAreaLeftX) {
                // 右上底袋
                probHole = table.topRight.fallCenter;
                probPocketFallCenter = table.topRight.fallCenter;
            }
        } else if (nextY >= table.botCornerHoleAreaUpY) {
            if (nextX < table.leftCornerHoleAreaRightX) {
                // 左下底袋}
                probHole = table.botLeft.fallCenter;
                probPocketFallCenter = table.botLeft.fallCenter;
            } else if (nextX >= table.rightCornerHoleAreaLeftX) {
                probHole = table.botRight.fallCenter;  // 右下底袋
                probPocketFallCenter = table.botRight.fallCenter;
            }
        }

        if (probHole != null) {
            for (int i = 0; i < table.allCornerLines.length; ++i) {
                Cushion.CushionLine line = table.allCornerLines[i];

                if (predictedDtToLine(line.getPosition()) < radius && currentDtToLine(line.getPosition()) >= radius) {
                    hitHoleLineArea(
                            line.getPosition(),
                            line.getNormal(),
                            phy);
                    return new CushionHitResult(line, 2);
                }
            }
//            if (!table.isStraightHole()) {
            for (Cushion.CushionArc cornerArc : table.allCornerArcs) {
                if (predictedDtToPoint(cornerArc.getCenter()) < table.cornerArcRadius + radius &&
                        currentDtToPoint(cornerArc.getCenter()) >= table.cornerArcRadius + radius) {
                    hitHoleArcArea(cornerArc.getCenter(), phy, table.cornerArcRadius);
                    return new CushionHitResult(cornerArc, 2);
                }
            }
//            }

            tryEnterGravityArea(phy, probPocketFallCenter, false);
            normalMove(phy);
            prepareMove(phy);
            return new CushionHitResult(1);
        }
        return new CushionHitResult(0);
    }

    /**
     * 返回较为精确的碰库位置
     */
    protected double[] getCushionHitPos(double[][] cushionLine) {
        double x1 = x;
        double y1 = y;
        int maxRound = 100;
        int phyRounds = 0;
        double tickDt;
        double curDivider = 1.0;

        double allowedDev = 0.01;  // 0.01 mm

        // 类似二分搜索，找碰撞点
        while (phyRounds < maxRound) {
            tickDt = Algebra.distanceToLine(x1, y1, cushionLine[0], cushionLine[1]) - values.ball.ballRadius;
            if (Math.abs(tickDt) < allowedDev) break;
            double mul;
            if (tickDt > 0) {
                mul = 1;
            } else {
                mul = -1;
            }

            x1 += vx / curDivider * mul;
            y1 += vy / curDivider * mul;

            curDivider *= 2;
            phyRounds++;
        }
        return new double[]{x1, y1};
    }

    /**
     * 返回较为精确的碰库位置
     */
    protected double[] getArcHitPos(double[] arcCenter, double arcRadius) {
        double x1 = x;
        double y1 = y;
        int maxRound = 100;
        int phyRounds = 0;
        double tickDt;
        double curDivider = 1.0;

        double allowedDev = 0.01;  // 0.01 mm

        // 类似二分搜索，找碰撞点
        while (phyRounds < maxRound) {
            tickDt = Algebra.distanceToPoint(x1, y1, arcCenter[0], arcCenter[1]) - values.ball.ballRadius - arcRadius;
            if (Math.abs(tickDt) < allowedDev) break;
            double mul;
            if (tickDt > 0) {
                mul = 1;
            } else {
                mul = -1;
            }

            x1 += vx / curDivider * mul;
            y1 += vy / curDivider * mul;

            curDivider *= 2;
            phyRounds++;
        }
        return new double[]{x1, y1};
    }

    abstract class Bounce implements Cloneable {

//        /*
//        1: 上边库 
//        2: 下边库 
//        3: 左底库 
//        4: 右底库 
//        5: 袋角弧线 
//        6: 袋角直线
//        */
//        int scenario;
//
//        // 仅有情况5时需要

//        double holeArcRadius;

        int accMul;  // 加速度的倍率，用于处理Phy帧时间的问题
        boolean everEnter = false;  // 是否进入过库边区域
        int framesCount = 0;

        protected Bounce(double accMul) {
            this.accMul = (int) Math.round(accMul);
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        abstract void processOneFrame();

        /**
         * @return 如果还在bounce过程则true。如返回false，则销毁该bounce
         */
        final boolean oneFrame() {
            if (framesCount > 30) {
//                System.out.println("Bounce alive for frames " + framesCount + " ");
                if (everEnter) {
//                    System.out.println("force leave");
                    leave();
                    return false;
                }
            }
            if (!everEnter) {
                if (!values.isInTable(x, y, values.ball.ballRadius)) {
                    everEnter = true;
                }
            }

            if (everEnter) {
                processOneFrame();
                framesCount++;
                return !isLeaving(x, y);
            } else {
                framesCount++;
                if (framesCount > 3) {
                    System.out.println("Frame " + framesCount + " not entered");
                    return false;
                }

                return true;
            }
        }

//        boolean isHoleArea() {
//            return scenario == 5 || scenario == 6;
//        }

        boolean isLeaving(double curX, double curY) {
//            return framesCount > 30 || !values.isInTable(curX, curY, values.ball.ballRadius) &&
//                    values.isInTable(curX + vx, curY + vy, values.ball.ballRadius);
            return everEnter && values.isInTable(curX + vx, curY + vy, values.ball.ballRadius);
        }

        abstract void leave();

        void clearDesireLeavePos() {
        }
    }

    class CushionBounce extends Bounce {
        double accX, accY;  // 反弹力的加速度，在PLAY_MS的条件下
        // 如果一切顺利，会在什么地方离开库
        double desiredX;
        double desiredY;
        double desiredVx;
        double desiredVy;
        double desiredSideSpin;

        CushionBounce(double accX, double accY, double accMul) {
            super(accMul);
            this.accX = accX;
            this.accY = accY;
        }

        @Override
        void processOneFrame() {
            vx += accX;
            vy += accY;
        }

        @Override
        void leave() {
            if (desiredX != 0) {
                x = desiredX;
                y = desiredY;
                vx = desiredVx;
                vy = desiredVy;
                if (ObjectOnTable.this instanceof Ball) {
                    ((Ball) ObjectOnTable.this).sideSpin = desiredSideSpin;
                }
            }
        }

        /**
         * 如果在弹库的过程中又被其他球撞了，更新加速度
         */
        void updateAcceleration() {
            // todo: 实现这个，虽然说影响应该不大
        }

        @Override
        void clearDesireLeavePos() {
            setDesiredLeavePos(0, 0, 0, 0, 0);
        }

        void setDesiredLeavePos(double desiredX, double desiredY,
                                double desiredVx, double desiredVy,
                                double desiredSideSpin) {
            this.desiredX = desiredX;
            this.desiredY = desiredY;
            this.desiredVx = desiredVx;
            this.desiredVy = desiredVy;
            this.desiredSideSpin = desiredSideSpin;
        }
    }

    class LineBounce extends CushionBounce {
        double desiredLeaveSpeed;

        LineBounce(double accX, double accY, double desiredLeaveSpeed, double accelerationMul) {
            super(accX, accY, accelerationMul);

            this.accX = accX;
            this.desiredLeaveSpeed = desiredLeaveSpeed;
        }

        @Override
        void leave() {
            if (desiredLeaveSpeed != 0) {
//                System.out.println("speed ratio: " + Math.hypot(vx, vy) / desiredLeaveSpeed / table.wallBounceRatio);
                double speed = Math.hypot(vx, vy);
                double ratio = speed / desiredLeaveSpeed;
                vx /= ratio;
                vy /= ratio;
            }
        }

        @Override
        void clearDesireLeavePos() {
            desiredLeaveSpeed = 0.0;
            desiredSideSpin = 0.0;
        }
    }

    class ArcBounce extends Bounce {
        double[] holeArcCenter;
        double verticalAcc;
        double desiredLeaveSpeed;
        double injectAngle;

        double desiredLeaveSideSpin;

//        double[] lastUnitAcc;

        ArcBounce(double[] arcCenter, double verticalAcc, double desiredLeaveSpeed,
                  double injectAngle, double accMul) {
            super(accMul);
            this.holeArcCenter = arcCenter;
            this.verticalAcc = verticalAcc;
            this.desiredLeaveSpeed = desiredLeaveSpeed;
            this.injectAngle = injectAngle;

//            System.out.println(Math.toDegrees(injectAngle));
        }

        @Override
        void processOneFrame() {
            // 每一帧都得更新加速方向
            // 加速方向是球当前位置与圆心的连线
            // todo: 此处假设球永远砸不到圆的半径那么深
            double[] unitAcc = Algebra.unitVector(x - holeArcCenter[0], y - holeArcCenter[1]);
//            if (overshoot) unitAcc = lastUnitAcc;
//            else {
//                unitAcc = Algebra.unitVector(x - holeArcCenter[0], y - holeArcCenter[1]);
//                if (lastUnitAcc != null) {
//                    double unitAccChange = Algebra.thetaBetweenVectors(lastUnitAcc, unitAcc);
//                    if (unitAccChange >= Algebra.HALF_PI) {
//                        System.out.println("Overshoot!");
//                        unitAcc = lastUnitAcc;
//                        overshoot = true;
//                    }
//                }
//            }

            double accX = unitAcc[0] * verticalAcc;
            double accY = unitAcc[1] * verticalAcc;
            vx += accX;
            vy += accY;

//            lastUnitAcc = unitAcc;
        }

        public void setDesiredLeaveSideSpin(double desiredLeaveSideSpin) {
            this.desiredLeaveSideSpin = desiredLeaveSideSpin;
        }

        @Override
        void leave() {
            if (desiredLeaveSpeed != 0) {
//                System.out.println("speed ratio: " + Math.hypot(vx, vy) / desiredLeaveSpeed / table.wallBounceRatio);
//                double ejectAngle = Algebra.thetaOf(vx, vy);  // 入射角与垂线的夹角
                double verticalAngle = Algebra.thetaOf(x - holeArcCenter[0], y - holeArcCenter[1]);
                double ballAngle = Algebra.thetaOf(vx, vy);  // 当前球的射出角
                double ejectAngle = verticalAngle - injectAngle;  // 根据当年入射角算出来的反射角

                // 取平均值，魔法
                double realAngle = Algebra.angularBisector(ballAngle, ejectAngle);
//                System.out.printf("%.2f, %.2f, %.2f\n%n", Math.toDegrees(ballAngle), Math.toDegrees(ejectAngle), Math.toDegrees(realAngle));

                double[] vecOfAngle = Algebra.unitVectorOfAngle(realAngle);
                vx = vecOfAngle[0] * desiredLeaveSpeed;
                vy = vecOfAngle[1] * desiredLeaveSpeed;

                if (ObjectOnTable.this instanceof Ball && desiredLeaveSideSpin != 0.0) {
                    ((Ball) ObjectOnTable.this).sideSpin = desiredLeaveSideSpin;
                }

//                double speed = Math.hypot(vx, vy);
//                double ratio = speed / desiredLeaveSpeed;
//                vx /= ratio;
//                vy /= ratio;
            }
        }

        @Override
        void clearDesireLeavePos() {
            this.desiredLeaveSpeed = 0.0;
            this.injectAngle = 0.0;
            this.desiredLeaveSideSpin = 0.0;
        }
    }

    public static final class CushionHitResult {
        private final Cushion cushion;
        private final int result;

        public CushionHitResult(Cushion cushion, int result) {
            this.cushion = cushion;
            this.result = result;
            
            if (result == 2) {
                if (cushion == null) throw new RuntimeException();
            } else {
                if (cushion != null) throw new RuntimeException();
            }
        }
        
        public CushionHitResult(int result) {
            this(null, result);
        }

        public Cushion cushion() {
            return cushion;
        }

        public int result() {
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (CushionHitResult) obj;
            return Objects.equals(this.cushion, that.cushion) &&
                    this.result == that.result;
        }

        @Override
        public int hashCode() {
            return Objects.hash(cushion, result);
        }

        @Override
        public String toString() {
            return "CushionHitResult[" +
                    "cushion=" + cushion + ", " +
                    "result=" + result + ']';
        }
    
        }
}
