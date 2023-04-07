package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;
import trashsoftware.trashSnooker.core.phy.Phy;

public abstract class ObjectOnTable {
    protected final GameValues gameValues;
    protected final TableMetrics table;
    protected final double radius;
    protected double distance;
    protected double x, y;
    protected double nextX, nextY;
    protected double vx, vy;  // unit: mm/(sec/frameRate)

    public ObjectOnTable(GameValues gameValues, double radius) {
        this.gameValues = gameValues;
        this.table = gameValues.table;
        this.radius = radius;
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

    protected  double predictedDtTo(Ball ball) {
        return Algebra.distanceToPoint(nextX, nextY, ball.nextX, ball.nextY);
    }

    protected  double currentDtToLine(double[][] line) {
        return Algebra.distanceToLine(x, y, line[0], line[1]);
    }

    protected  double predictedDtToLine(double[][] line) {
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
    
    protected boolean willPot(Phy phy) {
        double cornerRoom = table.cornerHoleRadius - gameValues.ball.ballRadius;
        double midRoom = table.midHoleRadius - gameValues.ball.ballRadius;
        
        return predictedDtToPoint(table.topLeftHoleXY) < cornerRoom ||
                predictedDtToPoint(table.botLeftHoleXY) < cornerRoom ||
                predictedDtToPoint(table.topRightHoleXY) < cornerRoom ||
                predictedDtToPoint(table.botRightHoleXY) < cornerRoom ||
                predictedDtToPoint(table.topMidHoleXY) < midRoom ||
                predictedDtToPoint(table.botMidHoleXY) < midRoom;
    }

    protected double[] hitHoleArcArea(double[] arcXY, Phy phy) {
        double axisX = arcXY[0] - x;  // 切线的法向量
        double axisY = arcXY[1] - y;
        double[] reflect = Algebra.symmetricVector(vx, vy, axisX, axisY);
        vx = -reflect[0];
        vy = -reflect[1];
        
        return new double[]{axisX, axisY};  // 返回切线的法向量
    }

    protected void hitHoleLineArea(double[] lineNormalVec, Phy phy) {
        double[] reflect = Algebra.symmetricVector(vx, vy, lineNormalVec[0], lineNormalVec[1]);
        vx = -reflect[0];
        vy = -reflect[1];
    }
    
    protected void tryEnterGravityArea(Phy phy, double[] holeXy, boolean isMidHole) {
        double xDiff = holeXy[0] - nextX;
        double yDiff = holeXy[1] - nextY;
        double dt = Math.hypot(xDiff, yDiff);
        
        double holeRadius = isMidHole ? table.midHoleRadius : table.cornerHoleRadius;
        double holeAndSlopeRadius = holeRadius + table.holeGravityAreaWidth;
        
        if (dt < holeAndSlopeRadius) {
            // dt应该不会小于 holeRadius - ballRadius 太多
            double accMag;
            if (dt < holeRadius) {
                accMag = 1;
            } else {
                accMag = (table.holeGravityAreaWidth - dt + holeRadius) / table.holeGravityAreaWidth;
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
     * 检测是否撞击袋角或进入袋角区域。如果撞击袋角，返回{@code 2}且处理撞击。如果进入袋角区域但未发生撞击，返回{@code 1}。如未进入，返回{@code 0}
     */
    protected int tryHitHoleArea(Phy phy) {
        if (nextY < radius + table.topY) {
            if (nextX < table.midHoleAreaRightX && nextX >= table.midHoleAreaLeftX) {
                // 上方中袋在袋角范围内
                if (predictedDtToPoint(table.topMidHoleLeftArcXy) < table.midArcRadius + radius &&
                        currentDtToPoint(table.topMidHoleLeftArcXy) >= table.midArcRadius + radius) {
                    // 击中上方中袋左侧
                    hitHoleArcArea(table.topMidHoleLeftArcXy, phy);
                } else if (predictedDtToPoint(table.topMidHoleRightArcXy) < table.midArcRadius + radius &&
                        currentDtToPoint(table.topMidHoleRightArcXy) >= table.midArcRadius + radius) {
                    // 击中上方中袋右侧
                    hitHoleArcArea(table.topMidHoleRightArcXy, phy);
                } else if (table.isStraightHole() &&
                        nextX >= table.midHoleLineLeftX && nextX < table.midHoleLineRightX) {
                    // 疑似上方中袋直线
                    double[][] line = table.topMidHoleLeftLine;
                    if (predictedDtToLine(line) < radius &&
                            currentDtToLine(line) >= radius) {
                        hitHoleLineArea(
                                Algebra.normalVector(new double[]{line[0][0] - line[1][0], line[0][1] - line[1][1]}), 
                                phy);
                        return 2;
                    }
                    line = table.topMidHoleRightLine;
                    if (predictedDtToLine(line) < radius &&
                            currentDtToLine(line) >= radius) {
                        hitHoleLineArea(
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
                return 2;
            }
        } else if (nextY >= table.botY - radius) {
            if (nextX < table.midHoleAreaRightX && nextX >= table.midHoleAreaLeftX) {
                // 下方中袋袋角范围内
                if (predictedDtToPoint(table.botMidHoleLeftArcXy) < table.midArcRadius + radius &&
                        currentDtToPoint(table.botMidHoleLeftArcXy) >= table.midArcRadius + radius) {
                    // 击中下方中袋左侧
                    hitHoleArcArea(table.botMidHoleLeftArcXy, phy);
                } else if (predictedDtToPoint(table.botMidHoleRightArcXy) < table.midArcRadius + radius &&
                        currentDtToPoint(table.botMidHoleRightArcXy) >= table.midArcRadius + radius) {
                    // 击中下方中袋右侧
                    hitHoleArcArea(table.botMidHoleRightArcXy, phy);
                } else if (table.isStraightHole() &&
                        nextX >= table.midHoleLineLeftX && nextX < table.midHoleLineRightX) {
                    // 疑似下方中袋直线
                    double[][] line = table.botMidHoleLeftLine;
                    if (predictedDtToLine(line) < radius &&
                            currentDtToLine(line) >= radius) {
                        hitHoleLineArea(
                                Algebra.normalVector(new double[]{line[0][0] - line[1][0], line[0][1] - line[1][1]}),
                                phy);
                        return 2;
                    }
                    line = table.botMidHoleRightLine;
                    if (predictedDtToLine(line) < radius &&
                            currentDtToLine(line) >= radius) {
                        hitHoleLineArea(
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
                return 2;
            }
        }
        double[] probHole = null;
        if (nextY < table.topCornerHoleAreaDownY) {
            if (nextX < table.leftCornerHoleAreaRightX) probHole = table.topLeftHoleXY;  // 左上底袋
            else if (nextX >= table.rightCornerHoleAreaLeftX) probHole = table.topRightHoleXY;  // 右上底袋
        } else if (nextY >= table.botCornerHoleAreaUpY) {
            if (nextX < table.leftCornerHoleAreaRightX) probHole = table.botLeftHoleXY;  // 左下底袋
            else if (nextX >= table.rightCornerHoleAreaLeftX) probHole = table.botRightHoleXY;  // 右下底袋
        }

        if (probHole != null) {
            for (int i = 0; i < table.allCornerLines.length; ++i) {
                double[][] line = table.allCornerLines[i];

                if (predictedDtToLine(line) < radius && currentDtToLine(line) >= radius) {
                    hitHoleLineArea(
                            Algebra.normalVector(new double[]{line[0][0] - line[1][0], line[0][1] - line[1][1]}),
                            phy);
                    return 2;
                }
            }
            if (!table.isStraightHole()) {
                for (double[] cornerArc : table.allCornerArcs) {
                    if (predictedDtToPoint(cornerArc) < table.cornerArcRadius + radius &&
                            currentDtToPoint(cornerArc) >= table.cornerArcRadius + radius) {
                        hitHoleArcArea(cornerArc, phy);
                        return 2;
                    }
                }
            }
            
            tryEnterGravityArea(phy, probHole, false);
            normalMove(phy);
            prepareMove(phy);
            return 1;
        }
        return 0;
    }
}
