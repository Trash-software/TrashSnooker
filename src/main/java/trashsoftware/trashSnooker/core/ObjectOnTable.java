package trashsoftware.trashSnooker.core;

public abstract class ObjectOnTable {
    protected final GameValues values;
    protected final double radius;
    protected double distance;
    protected double x, y;
    protected double nextX, nextY;
    protected double vx, vy;  // unit: mm/(sec/frameRate)

    public ObjectOnTable(GameValues gameValues, double radius) {
        this.values = gameValues;
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

    protected void prepareMove() {
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
        double midHoleFactor = midHolePowerFactor(phy);
        return predictedDtToPoint(values.topLeftHoleXY) < values.cornerHoleRadius ||
                predictedDtToPoint(values.botLeftHoleXY) < values.cornerHoleRadius ||
                predictedDtToPoint(values.topRightHoleXY) < values.cornerHoleRadius ||
                predictedDtToPoint(values.botRightHoleXY) < values.cornerHoleRadius ||
                predictedDtToPoint(values.topMidHoleXY) < values.midHoleRadius * midHoleFactor ||
                predictedDtToPoint(values.botMidHoleXY) < values.midHoleRadius * midHoleFactor;
    }

    protected void hitHoleArcArea(double[] arcXY) {
        double axisX = arcXY[0] - x;  // 切线的法向量
        double axisY = arcXY[1] - y;
        double[] reflect = Algebra.symmetricVector(vx, vy, axisX, axisY);
        vx = -reflect[0];
        vy = -reflect[1];
    }

    protected void hitHoleLineArea(double[] lineNormalVec) {
        double[] reflect = Algebra.symmetricVector(vx, vy, lineNormalVec[0], lineNormalVec[1]);
        vx = -reflect[0];
        vy = -reflect[1];
    }

    /**
     * 检测是否撞击袋角或进入袋角区域。如果撞击袋角，返回{@code 2}且处理撞击。如果进入袋角区域但未发生撞击，返回{@code 1}。如未进入，返回{@code 0}
     */
    protected int tryHitHoleArea(Phy phy) {
        boolean enteredCorner = false;
        if (nextY < radius + values.topY) {
            if (nextX < values.midHoleAreaRightX && nextX >= values.midHoleAreaLeftX) {
                // 上方中袋在袋角范围内
                if (predictedDtToPoint(values.topMidHoleLeftArcXy) < values.midArcRadius + radius &&
                        currentDtToPoint(values.topMidHoleLeftArcXy) >= values.midArcRadius + radius) {
                    // 击中上方中袋左侧
                    hitHoleArcArea(values.topMidHoleLeftArcXy);
                } else if (predictedDtToPoint(values.topMidHoleRightArcXy) < values.midArcRadius + radius &&
                        currentDtToPoint(values.topMidHoleRightArcXy) >= values.midArcRadius + radius) {
                    // 击中上方中袋右侧
                    hitHoleArcArea(values.topMidHoleRightArcXy);
                } else if (values.isStraightHole() &&
                        nextX >= values.midHoleLineLeftX && nextX < values.midHoleLineRightX) {
                    // 疑似上方中袋直线
                    double[][] line = values.topMidHoleLeftLine;
                    if (predictedDtToLine(line) < radius &&
                            currentDtToLine(line) >= radius) {
                        hitHoleLineArea(
                                Algebra.normalVector(new double[]{line[0][0] - line[1][0], line[0][1] - line[1][1]}));
                        return 2;
                    }
                    line = values.topMidHoleRightLine;
                    if (predictedDtToLine(line) < radius &&
                            currentDtToLine(line) >= radius) {
                        hitHoleLineArea(
                                Algebra.normalVector(new double[]{line[0][0] - line[1][0], line[0][1] - line[1][1]}));
                        return 2;
                    }
                    normalMove(phy);
                    prepareMove();
                    return 1;
                } else {
                    normalMove(phy);
                    prepareMove();
                    return 1;
                }
                return 2;
            }
        } else if (nextY >= values.botY - radius) {
            if (nextX < values.midHoleAreaRightX && nextX >= values.midHoleAreaLeftX) {
                // 下方中袋袋角范围内
                if (predictedDtToPoint(values.botMidHoleLeftArcXy) < values.midArcRadius + radius &&
                        currentDtToPoint(values.botMidHoleLeftArcXy) >= values.midArcRadius + radius) {
                    // 击中下方中袋左侧
                    hitHoleArcArea(values.botMidHoleLeftArcXy);
                } else if (predictedDtToPoint(values.botMidHoleRightArcXy) < values.midArcRadius + radius &&
                        currentDtToPoint(values.botMidHoleRightArcXy) >= values.midArcRadius + radius) {
                    // 击中下方中袋右侧
                    hitHoleArcArea(values.botMidHoleRightArcXy);
                } else if (values.isStraightHole() &&
                        nextX >= values.midHoleLineLeftX && nextX < values.midHoleLineRightX) {
                    // 疑似下方中袋直线
                    double[][] line = values.botMidHoleLeftLine;
                    if (predictedDtToLine(line) < radius &&
                            currentDtToLine(line) >= radius) {
                        hitHoleLineArea(
                                Algebra.normalVector(new double[]{line[0][0] - line[1][0], line[0][1] - line[1][1]}));
                        return 2;
                    }
                    line = values.botMidHoleRightLine;
                    if (predictedDtToLine(line) < radius &&
                            currentDtToLine(line) >= radius) {
                        hitHoleLineArea(
                                Algebra.normalVector(new double[]{line[0][0] - line[1][0], line[0][1] - line[1][1]}));
                        return 2;
                    }
                    normalMove(phy);
                    prepareMove();
                    return 1;
                } else {
                    normalMove(phy);
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

                if (predictedDtToLine(line) < radius && currentDtToLine(line) >= radius) {
                    hitHoleLineArea(
                            Algebra.normalVector(new double[]{line[0][0] - line[1][0], line[0][1] - line[1][1]}));
                    return 2;
                }
            }
            if (!values.isStraightHole()) {
                for (double[] cornerArc : values.allCornerArcs) {
                    if (predictedDtToPoint(cornerArc) < values.cornerArcRadius + radius &&
                            currentDtToPoint(cornerArc) >= values.cornerArcRadius + radius) {
                        hitHoleArcArea(cornerArc);
                        return 2;
                    }
                }
            }
            normalMove(phy);
            prepareMove();
            return 1;
        }
        return 0;
    }
}
