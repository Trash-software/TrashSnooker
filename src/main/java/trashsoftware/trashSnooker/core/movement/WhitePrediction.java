package trashsoftware.trashSnooker.core.movement;

import trashsoftware.trashSnooker.core.Ball;

import java.util.ArrayList;
import java.util.List;

public class WhitePrediction {
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
    private boolean hitWallBeforeHitBall;
    private boolean cueBallWillPot;
    
    // 非必选项
    private Ball whiteSecondCollide;
    private double whiteSpeedWhenHitSecondBall;

    public WhitePrediction(Ball whiteBall) {
        whiteX = whiteBall.getX();
        whiteY = whiteBall.getY();
    }

    public List<double[]> getWhitePath() {
        return whitePath;
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
    }

    public void potCueBall() {
        this.cueBallWillPot = true;
    }

    public boolean willCueBallPot() {
        return cueBallWillPot;
    }

    /**
     * 白球撞上第二颗球时的速度，如果有的话。单位mm/s
     */
    public void setSecondCollide(Ball secondCollide, double whiteSpeedWhenCollision) {
        this.whiteSecondCollide = secondCollide;
        this.whiteSpeedWhenHitSecondBall = whiteSpeedWhenCollision;
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
