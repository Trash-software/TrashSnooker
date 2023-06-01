package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;
import trashsoftware.trashSnooker.core.phy.Phy;

public abstract class ObjectOnTable implements Cloneable {
    protected static final double GENERAL_BOUNCE_ACC = 0.5;
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

    public void setVx(double vx) {
        this.vx = vx;
    }

    public void setVy(double vy) {
        this.vy = vy;
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
            System.out.println("Cleared!");
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
        
        double cornerRoom = table.cornerHoleRadius - values.ball.ballRadius;
        double midRoom = table.midHoleRadius - values.ball.ballRadius;

        return predictedDtToPoint(table.topLeftHoleXY) < cornerRoom ||
                predictedDtToPoint(table.botLeftHoleXY) < cornerRoom ||
                predictedDtToPoint(table.topRightHoleXY) < cornerRoom ||
                predictedDtToPoint(table.botRightHoleXY) < cornerRoom ||
                predictedDtToPoint(table.topMidHoleXY) < midRoom ||
                predictedDtToPoint(table.botMidHoleXY) < midRoom;
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
                speed * table.wallBounceRatio * 0.9,
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
                Math.hypot(vx, vy) * table.wallBounceRatio * 0.9,
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

        double holeRadius = isMidHole ? table.midHoleRadius : table.cornerHoleRadius;
        double holeAndSlopeRadius = holeRadius +
                (isMidHole ? 
                        table.midPocketGravityRadius : 
                        table.cornerPocketGravityRadius);
        
        double gravityRadius = isMidHole ? table.midPocketGravityRadius : table.cornerPocketGravityRadius;

        if (dt < holeAndSlopeRadius) {
            // dt应该不会小于 holeRadius - ballRadius 太多
            double accMag;
            if (dt < holeRadius) {
                accMag = 1;
            } else {
                accMag = (gravityRadius - dt + holeRadius) / gravityRadius;
            }

            accMag *= 4800;
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
    protected int tryHitHoleArea(Phy phy) {
        if (nextY < radius + table.topY) {
            if (nextX < table.midHoleAreaRightX && nextX >= table.midHoleAreaLeftX) {
                // 上方中袋在袋角范围内
                if (predictedDtToPoint(table.topMidHoleLeftArcXy) < table.midArcRadius + radius &&
                        currentDtToPoint(table.topMidHoleLeftArcXy) >= table.midArcRadius + radius) {
                    // 击中上方中袋左侧
                    hitHoleArcArea(table.topMidHoleLeftArcXy, phy, table.midArcRadius);
                    return 2;
                } else if (predictedDtToPoint(table.topMidHoleRightArcXy) < table.midArcRadius + radius &&
                        currentDtToPoint(table.topMidHoleRightArcXy) >= table.midArcRadius + radius) {
                    // 击中上方中袋右侧
                    hitHoleArcArea(table.topMidHoleRightArcXy, phy, table.midArcRadius);
                    return 2;
                } else if (nextX >= table.midHoleLineLeftX && nextX < table.midHoleLineRightX) {
                    // 疑似上方中袋直线
                    double[][] line = table.topMidHoleLeftLine;
                    if (predictedDtToLine(line) < radius &&
                            currentDtToLine(line) >= radius) {
                        hitHoleLineArea(
                                line,
                                Algebra.normalVector(new double[]{line[0][0] - line[1][0], line[0][1] - line[1][1]}),
                                phy);
                        return 2;
                    }
                    line = table.topMidHoleRightLine;
                    if (predictedDtToLine(line) < radius &&
                            currentDtToLine(line) >= radius) {
                        hitHoleLineArea(
                                line,
                                Algebra.normalVector(new double[]{line[0][0] - line[1][0], line[0][1] - line[1][1]}),
                                phy);
                        return 2;
                    }

                    tryEnterGravityArea(phy, table.topMidHoleXY, true);
                    normalMove(phy);
                    prepareMove(phy);
                    return 1;
                } else {

                    tryEnterGravityArea(phy, table.topMidHoleXY, true);
                    normalMove(phy);
                    prepareMove(phy);
                    return 1;
                }
            }
        } else if (nextY >= table.botY - radius) {
            if (nextX < table.midHoleAreaRightX && nextX >= table.midHoleAreaLeftX) {
                // 下方中袋袋角范围内
                if (predictedDtToPoint(table.botMidHoleLeftArcXy) < table.midArcRadius + radius &&
                        currentDtToPoint(table.botMidHoleLeftArcXy) >= table.midArcRadius + radius) {
                    // 击中下方中袋左侧
                    hitHoleArcArea(table.botMidHoleLeftArcXy, phy, table.midArcRadius);
                    return 2;
                } else if (predictedDtToPoint(table.botMidHoleRightArcXy) < table.midArcRadius + radius &&
                        currentDtToPoint(table.botMidHoleRightArcXy) >= table.midArcRadius + radius) {
                    // 击中下方中袋右侧
                    hitHoleArcArea(table.botMidHoleRightArcXy, phy, table.midArcRadius);
                    return 2;
                } else if (nextX >= table.midHoleLineLeftX && nextX < table.midHoleLineRightX) {
                    // 疑似下方中袋直线
                    double[][] line = table.botMidHoleLeftLine;
                    if (predictedDtToLine(line) < radius &&
                            currentDtToLine(line) >= radius) {
                        hitHoleLineArea(
                                line,
                                Algebra.normalVector(new double[]{line[0][0] - line[1][0], line[0][1] - line[1][1]}),
                                phy);
                        return 2;
                    }
                    line = table.botMidHoleRightLine;
                    if (predictedDtToLine(line) < radius &&
                            currentDtToLine(line) >= radius) {
                        hitHoleLineArea(
                                line,
                                Algebra.normalVector(new double[]{line[0][0] - line[1][0], line[0][1] - line[1][1]}),
                                phy);
                        return 2;
                    }

                    tryEnterGravityArea(phy, table.botMidHoleXY, true);
                    normalMove(phy);
                    prepareMove(phy);
                    return 1;
                } else {

                    tryEnterGravityArea(phy, table.botMidHoleXY, true);
                    normalMove(phy);
                    prepareMove(phy);
                    return 1;
                }
            }
        }

        // 底袋
        double[] probHole = null;
        if (nextY < table.topCornerHoleAreaDownY) {
            if (nextX < table.leftCornerHoleAreaRightX) probHole = table.topLeftHoleXY;  // 左上底袋
            else if (nextX >= table.rightCornerHoleAreaLeftX)
                probHole = table.topRightHoleXY;  // 右上底袋
        } else if (nextY >= table.botCornerHoleAreaUpY) {
            if (nextX < table.leftCornerHoleAreaRightX) probHole = table.botLeftHoleXY;  // 左下底袋
            else if (nextX >= table.rightCornerHoleAreaLeftX)
                probHole = table.botRightHoleXY;  // 右下底袋
        }

        if (probHole != null) {
            for (int i = 0; i < table.allCornerLines.length; ++i) {
                double[][] line = table.allCornerLines[i];

                if (predictedDtToLine(line) < radius && currentDtToLine(line) >= radius) {
                    hitHoleLineArea(
                            line,
                            Algebra.normalVector(new double[]{line[0][0] - line[1][0], line[0][1] - line[1][1]}),
                            phy);
                    return 2;
                }
            }
//            if (!table.isStraightHole()) {
                for (double[] cornerArc : table.allCornerArcs) {
                    if (predictedDtToPoint(cornerArc) < table.cornerArcRadius + radius &&
                            currentDtToPoint(cornerArc) >= table.cornerArcRadius + radius) {
                        hitHoleArcArea(cornerArc, phy, table.cornerArcRadius);
                        return 2;
                    }
                }
//            }

            tryEnterGravityArea(phy, probHole, false);
            normalMove(phy);
            prepareMove(phy);
            return 1;
        }
        return 0;
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
                System.out.println("Bounce alive for frames " + framesCount);
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
        }
    }

    class ArcBounce extends Bounce {
        double[] holeArcCenter;
        double verticalAcc;
        double desiredLeaveSpeed;
        double injectAngle;
        
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
        }
    }
}
