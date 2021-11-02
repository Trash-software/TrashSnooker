package trashsoftware.trashSnooker.core.movement;

import trashsoftware.trashSnooker.core.Ball;

import java.util.ArrayList;
import java.util.List;

public class WhitePrediction {
    public final double whiteX;
    public final double whiteY;
    private Ball firstCollide;
    // 目标球碰撞后的单位行进方向
    private double ballDirectionX;
    private double ballDirectionY;
    // 白球碰到目标球后的单位行进方向
    private double whiteDirectionXBeforeCollision;
    private double whiteDirectionYBeforeCollision;
    private double whiteCollisionX;
    private double whiteCollisionY;
    private boolean hitWallBeforeHitBall;
    private final List<double[]> whitePath = new ArrayList<>();
    
    public WhitePrediction(Ball whiteBall) {
        whiteX = whiteBall.getX();
        whiteY = whiteBall.getY();
    }

    public List<double[]> getWhitePath() {
        return whitePath;
    }

    public void setFirstCollide(Ball firstCollide, boolean hitWallBeforeHitBall,
                                double ballDirectionX, double ballDirectionY,
                                double whiteDirectionXBeforeCollision, 
                                double whiteDirectionYBeforeCollision,
                                double whiteCollisionX, double whiteCollisionY) {
        this.firstCollide = firstCollide;
        this.hitWallBeforeHitBall = hitWallBeforeHitBall;
        this.ballDirectionX = ballDirectionX;
        this.ballDirectionY = ballDirectionY;
        this.whiteDirectionXBeforeCollision = whiteDirectionXBeforeCollision;
        this.whiteDirectionYBeforeCollision = whiteDirectionYBeforeCollision;
        this.whiteCollisionX = whiteCollisionX;
        this.whiteCollisionY = whiteCollisionY;
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
