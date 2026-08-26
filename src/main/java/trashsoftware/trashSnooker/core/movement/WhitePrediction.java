package trashsoftware.trashSnooker.core.movement;

import org.jetbrains.annotations.Nullable;
import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.CuePlayParams;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.phy.Phy;

import java.util.ArrayList;
import java.util.List;

public class WhitePrediction {
    public static final double TWICE_HIT_MINIMAL_GAP_MS = 3.001;
    
    public final Ball cueBall;
    public final double whiteX;  // 初始的位置
    public final double whiteY;
    private final List<double[]> whitePath = new ArrayList<>();
//    private final List<double[]> firstBallPath = new ArrayList<>();
//    private double[] firstBallStopPos;
    private Ball firstCollide;
    // 目标球碰撞后的单位行进方向
    private double ballDirectionX;
    private double ballDirectionY;
    // 目标球碰撞后的单位行进方向，但排除任何效应
    private double ballDirectionXRaw;
    private double ballDirectionYRaw;
    // 目标球碰撞后的速度
    private double ballInitSpeed;
    // 白球碰到目标球后的单位行进方向
    private double whiteDirectionXBeforeCollision;
    private double whiteDirectionYBeforeCollision;
    private double whiteCollisionX;
    private double whiteCollisionY;
    private int whiteCushionCountBefore;  // 碰第一颗球之前的库数
    private int whiteCushionCountAfter;  // 碰第一颗球之后的库数
    
    // 第一次碰库时的位置，都是大致位置，不完全精确
    private double[] whiteFirstCushionPos;
//    private double[] firstBallFirstCushionPos;
    
    // 第一颗碰到的球的初始位置
    private double firstBallX;
    private double firstBallY;
    private boolean firstBallWillPot;
    private Ball firstBallCollidesOther;
    private int firstBallCushionCount;
//    private double msWhenHitFirst;
    private boolean hitWallAfterFirstCollide;  // 白球或目标球在撞击之后又碰了库。用于二次碰撞检测
    
    private boolean hitWallBeforeHitBall;
    private boolean cueBallWillPot;
//    private boolean cueBallFirstBallTwiceColl;  // 二次碰撞
    
    // 非必选项
    private Ball whiteSecondCollide;
    
    private double whiteSpeedWhenHitFirstBall;
    private double[] whitePosWhenHitSecondBall;
    private double[] whiteVelocityWhenHitSecondBall;
    private double whiteSpeedWhenHitSecondBall;
    private boolean whiteHitsHoleArcs = false;  // 是否碰撞了袋角
    private double pathLength;
    private double distanceTravelledBeforeCollision;  // 白球在碰目标球之前跑了多远
    private double distanceTravelledAfterCollision;
    private double distanceTravelledAfter2ndCollision;  // 撞了第二颗球之后跑了多远

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
    
//    public double estimateTargetMoveDt(GameValues values, Phy phy) {
//        if (firstCollide != null) {
//            return values.estimatedMoveDistance(phy, getBallInitSpeed());
//        }
//        return -1;
//    }

    public List<double[]> getWhitePath() {
        return whitePath;
    }
    
//    public double[] lastVector() {
//        if (whitePath.size() < 2) return new double[]{1, 1};
//        double[] last = whitePath.get(whitePath.size() - 1);
//        double[] secondLast = whitePath.get(whitePath.size() - 2);
//        
//        return new double[]{last[0] - secondLast[0], last[1] - secondLast[1]};
//    }

    public List<double[]> getFirstBallPath() {
//        return firstBallPath;
        return null;
    }

    public double[] stopPoint() {
        if (whitePath.isEmpty()) return null;
        return whitePath.getLast(); 
    }
    
    public double @Nullable [] getFirstBallStopPoint() {
        return null;
//        if (firstBallPath.isEmpty()) return null;
//        return firstBallPath.getLast();
    }

//    public void setFirstBallStopPos(double[] firstBallStopPos) {
//        if (this.firstBallStopPos == null) {
//            this.firstBallStopPos = firstBallStopPos;
//        }
//    }
    
    public void addPointInFirstBallPath(double[] point) {
//        firstBallPath.add(point);
    }

    public void addPointInPath(double[] point) {
        if (!whitePath.isEmpty()) {
            double[] lastPoint = whitePath.getLast();
            double dt = Algebra.distanceToPoint(point[0], point[1], lastPoint[0], lastPoint[1]);
            pathLength += dt;
            if (firstCollide == null) {
                distanceTravelledBeforeCollision += dt;
            } else {
                distanceTravelledAfterCollision += dt;
                if (whiteSecondCollide != null) {
                    distanceTravelledAfter2ndCollision += dt;
                }
            }
        }
        whitePath.add(point);
    }
    
    public double[] whiteOrigPos() {
        return new double[]{whiteX, whiteY};
    }
    
    public double[] targetOrigPos() {
        return new double[]{firstBallX, firstBallY};
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

    public void setFirstCollide(Ball firstCollide, 
                                double whiteSpeedWhenHit,
                                boolean hitWallBeforeHitBall,
                                double ballDirectionX, 
                                double ballDirectionY,
                                double ballDirectionXRaw,  // 不考虑投掷/齿轮等效应时，目标球的方向
                                double ballDirectionYRaw,
                                double ballInitSpeed,
                                double whiteDirectionXBeforeCollision,
                                double whiteDirectionYBeforeCollision,
                                double whiteCollisionX, 
                                double whiteCollisionY) {
        this.firstCollide = firstCollide;
        this.whiteSpeedWhenHitFirstBall = whiteSpeedWhenHit;
        this.hitWallBeforeHitBall = hitWallBeforeHitBall;
        this.ballDirectionX = ballDirectionX;
        this.ballDirectionY = ballDirectionY;
        this.ballDirectionXRaw = ballDirectionXRaw;
        this.ballDirectionYRaw = ballDirectionYRaw;
        this.ballInitSpeed = ballInitSpeed;
        this.whiteDirectionXBeforeCollision = whiteDirectionXBeforeCollision;
        this.whiteDirectionYBeforeCollision = whiteDirectionYBeforeCollision;
        this.whiteCollisionX = whiteCollisionX;
        this.whiteCollisionY = whiteCollisionY;
        this.firstBallX = firstCollide.getX();
        this.firstBallY = firstCollide.getY();
//        this.msWhenHitFirst = happenMs;
        this.hitWallAfterFirstCollide = false;
    }

    public void potCueBall() {
        this.cueBallWillPot = true;
    }
    
    public void potFirstBall() {
        this.firstBallWillPot = true;
    }
    
    public void setFirstBallCollidesOther(Ball firstBallCollision) {
        this.firstBallCollidesOther = firstBallCollision;
        if (firstBallCollision.isWhite() 
                && whiteSecondCollide == null 
//                && happenMs - msWhenHitFirst >= TWICE_HIT_MINIMAL_GAP_MS
                && hitWallAfterFirstCollide
        ) {
            // 又撞一下白球
            whiteSecondCollide = firstCollide;
            whitePosWhenHitSecondBall = firstBallCollision.getPositionArray();
            whiteVelocityWhenHitSecondBall = firstBallCollision.getVelocityArray();
            whiteSpeedWhenHitSecondBall = Math.hypot(whiteVelocityWhenHitSecondBall[0], whiteVelocityWhenHitSecondBall[1]);
        }
    }

    public boolean isFirstBallCollidesOther() {
        return firstBallCollidesOther != null;
    }

    public Ball getFirstBallCollidesOther() {
        return firstBallCollidesOther;
    }

//    public double getMsWhenHitFirst() {
//        return msWhenHitFirst;
//    }
//    
//    public double gapMsFromFirstCollide(double nowMs) {
//        return nowMs - msWhenHitFirst;
//    }

    public boolean willCueBallPot() {
        return cueBallWillPot;
    }

    public boolean willFirstBallPot() {
        return firstBallWillPot;
    }

//    public void setTwiceColl(boolean cueBallFirstBallTwiceColl) {
//        this.cueBallFirstBallTwiceColl = cueBallFirstBallTwiceColl;
//    }

    public boolean isCueBallFirstBallTwiceColl() {
         return firstCollide != null && firstCollide.equals(whiteSecondCollide);
//        return false;
//        return cueBallFirstBallTwiceColl;
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
            hitWallAfterFirstCollide = true;
        }
        
        if (whiteFirstCushionPos == null) {
            if (whitePath.isEmpty()) {
                whiteFirstCushionPos = new double[]{whiteX, whiteY};
            } else {
                whiteFirstCushionPos = whitePath.getLast();
            }
        }
    }
    
    public void firstBallHitCushion() {
        firstBallCushionCount++;
        hitWallAfterFirstCollide = true;
    }

    /**
     * 白球撞上第二颗球时的速度，如果有的话。单位mm/s
     */
    public void setSecondCollide(Ball secondCollide,
                                 double[] whitePosWhenHitSecondBall,
                                 double[] whiteVelocityWhenCollision) {
//        if (happenMs - msWhenHitFirst < TWICE_HIT_MINIMAL_GAP_MS) return;
        // 在没再碰库的情况下，白球不可能和第一颗球二次碰撞
        if (secondCollide.equals(firstCollide) && !hitWallAfterFirstCollide) return;
        
        this.whiteSecondCollide = secondCollide;
        this.whitePosWhenHitSecondBall = whitePosWhenHitSecondBall;
        this.whiteVelocityWhenHitSecondBall = whiteVelocityWhenCollision;
        this.whiteSpeedWhenHitSecondBall = Math.hypot(whiteVelocityWhenCollision[0], whiteVelocityWhenCollision[1]);
    }

    public double[] getWhitePosWhenHitSecondBall() {
        return whitePosWhenHitSecondBall;
    }

    public double whitePathLenBtw1st2ndCollision() {
        return whiteSecondCollide == null ? 
                0 : 
                distanceTravelledAfterCollision - distanceTravelledAfter2ndCollision;
    }

    public void whiteCollidesHoleArcs() {
        this.whiteHitsHoleArcs = true;
    }

    public boolean isWhiteHitsHoleArcs() {
        return whiteHitsHoleArcs;
    }

    public double getWhiteSpeedWhenHitSecondBall() {
        return whiteSpeedWhenHitSecondBall;
    }

    public double[] getWhiteVelocityWhenHitSecondBall() {
        return whiteVelocityWhenHitSecondBall;
    }

    public double getWhiteSpeedWhenHitFirstBall() {
        return whiteSpeedWhenHitFirstBall;
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

    public double getBallDirectionXRaw() {
        return ballDirectionXRaw;
    }

    public double getBallDirectionYRaw() {
        return ballDirectionYRaw;
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

    public double[] getWhiteFirstCushionPos() {
        return whiteFirstCushionPos;
    }

//    public double[] getFirstBallFirstCushionPos() {
//        return firstBallFirstCushionPos;
//    }
}
