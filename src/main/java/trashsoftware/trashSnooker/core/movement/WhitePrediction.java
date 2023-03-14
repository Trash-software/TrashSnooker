package trashsoftware.trashSnooker.core.movement;

import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.Ball;

import java.util.ArrayList;
import java.util.List;

public class WhitePrediction {
    public final Ball cueBall;
    public final double whiteX;
    public final double whiteY;
    private final List<double[]> whitePath = new ArrayList<>();
    private Ball firstCollide;
    // 目标球碰撞后的单位行进方向
    private double ballDirectionX;
    private double ballDirectionY;
    // 目标球碰撞后的速度
    private double ballInitSpeed;
    // 白球碰到目标球后的单位行进方向
    private double whiteDirectionXBeforeCollision;
    private double whiteDirectionYBeforeCollision;
    private double whiteCollisionX;
    private double whiteCollisionY;
    private int whiteCushionCountBefore;  // 碰第一颗球之前的库数
    private int whiteCushionCountAfter;  // 碰第一颗球之后的库数
    // 第一颗碰到的球的初始位置
    private double firstBallX;
    private double firstBallY;
    private boolean firstBallWillPot;
    private boolean firstBallCollidesOther;
    private int firstBallCushionCount;
    
    private boolean hitWallBeforeHitBall;
    private boolean cueBallWillPot;
    
    // 非必选项
    private Ball whiteSecondCollide;
    private double whiteSpeedWhenHitSecondBall;
    private boolean whiteCollidesHoleArcs = false;  // 是否碰撞了袋角
    private double pathLength;
    private double distanceTravelledBeforeCollision;  // 白球在碰目标球之前跑了多远
    private double distanceTravelledAfterCollision;

    public WhitePrediction(Ball whiteBall) {
        this.cueBall = whiteBall;
        whiteX = whiteBall.getX();
        whiteY = whiteBall.getY();
    }

    public void resetToInit() {
        cueBall.setX(whiteX);
        cueBall.setY(whiteY);
        cueBall.pickup();
        
        if (firstCollide != null) {
            firstCollide.setX(firstBallX);
            firstCollide.setY(firstBallY);
            firstCollide.pickup();
        }
    }

    public List<double[]> getWhitePath() {
        return whitePath;
    }
    
    public double[] lastVector() {
        if (whitePath.size() < 2) return new double[]{1, 1};
        double[] last = whitePath.get(whitePath.size() - 1);
        double[] secondLast = whitePath.get(whitePath.size() - 2);
        
        return new double[]{last[0] - secondLast[0], last[1] - secondLast[1]};
    }
    
    public double[] stopPoint() {
        return whitePath.get(whitePath.size() - 1); 
    }
    
    public void addPointInPath(double[] point) {
        if (!whitePath.isEmpty()) {
            double[] lastPoint = whitePath.get(whitePath.size() - 1);
            double dt = Algebra.distanceToPoint(point[0], point[1], lastPoint[0], lastPoint[1]);
            pathLength += dt;
            if (firstCollide == null) {
                distanceTravelledBeforeCollision += dt;
            } else {
                distanceTravelledAfterCollision += dt;
            }
        }
        whitePath.add(point);
    }

    public double getPathLength() {
        return pathLength;
    }

    public double getDistanceTravelledBeforeCollision() {
        return distanceTravelledBeforeCollision;
    }

    public double getDistanceTravelledAfterCollision() {
        return distanceTravelledAfterCollision;
    }

    public void setFirstCollide(Ball firstCollide, boolean hitWallBeforeHitBall,
                                double ballDirectionX, double ballDirectionY,
                                double ballInitSpeed,
                                double whiteDirectionXBeforeCollision,
                                double whiteDirectionYBeforeCollision,
                                double whiteCollisionX, double whiteCollisionY) {
        this.firstCollide = firstCollide;
        this.hitWallBeforeHitBall = hitWallBeforeHitBall;
        this.ballDirectionX = ballDirectionX;
        this.ballDirectionY = ballDirectionY;
        this.ballInitSpeed = ballInitSpeed;
        this.whiteDirectionXBeforeCollision = whiteDirectionXBeforeCollision;
        this.whiteDirectionYBeforeCollision = whiteDirectionYBeforeCollision;
        this.whiteCollisionX = whiteCollisionX;
        this.whiteCollisionY = whiteCollisionY;
        this.firstBallX = firstCollide.getX();
        this.firstBallY = firstCollide.getY();
    }

    public void potCueBall() {
        this.cueBallWillPot = true;
    }
    
    public void potFirstBall() {
        this.firstBallWillPot = true;
    }
    
    public void setFirstBallCollidesOther() {
        this.firstBallCollidesOther = true;
    }

    public boolean isFirstBallCollidesOther() {
        return firstBallCollidesOther;
    }

    public boolean willCueBallPot() {
        return cueBallWillPot;
    }

    public boolean willFirstBallPot() {
        return firstBallWillPot;
    }

    public double getFirstBallX() {
        return firstBallX;
    }

    public double getFirstBallY() {
        return firstBallY;
    }

    public int getWhiteCushionCountBefore() {
        return whiteCushionCountBefore;
    }

    public int getWhiteCushionCountAfter() {
        return whiteCushionCountAfter;
    }

    public int getFirstBallCushionCount() {
        return firstBallCushionCount;
    }
    
    public void whiteHitCushion() {
        if (firstCollide == null) {
            whiteCushionCountBefore++;
        } else {
            whiteCushionCountAfter++;
        }
    }
    
    public void firstBallHitCushion() {
        firstBallCushionCount++;
    }

    /**
     * 白球撞上第二颗球时的速度，如果有的话。单位mm/s
     */
    public void setSecondCollide(Ball secondCollide, double whiteSpeedWhenCollision) {
        this.whiteSecondCollide = secondCollide;
        this.whiteSpeedWhenHitSecondBall = whiteSpeedWhenCollision;
    }

    public void whiteCollidesHoleArcs() {
        this.whiteCollidesHoleArcs = true;
    }

    public boolean isWhiteCollidesHoleArcs() {
        return whiteCollidesHoleArcs;
    }

    public double getWhiteSpeedWhenHitSecondBall() {
        return whiteSpeedWhenHitSecondBall;
    }

    public Ball getSecondCollide() {
        return whiteSecondCollide;
    }

    public double getBallInitSpeed() {
        return ballInitSpeed;
    }

    public Ball getFirstCollide() {
        return firstCollide;
    }

    public boolean isHitWallBeforeHitBall() {
        return hitWallBeforeHitBall;
    }

    public double getBallDirectionX() {
        return ballDirectionX;
    }

    public double getBallDirectionY() {
        return ballDirectionY;
    }

    public double getWhiteDirectionXBeforeCollision() {
        return whiteDirectionXBeforeCollision;
    }

    public double getWhiteDirectionYBeforeCollision() {
        return whiteDirectionYBeforeCollision;
    }

    public double getWhiteCollisionX() {
        return whiteCollisionX;
    }

    public double getWhiteCollisionY() {
        return whiteCollisionY;
    }
}
