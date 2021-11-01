package trashsoftware.trashSnooker.core.movement;

import trashsoftware.trashSnooker.core.Ball;

import java.util.ArrayList;
import java.util.List;

public class WhitePrediction {
    public final double whiteX;
    public final double whiteY;
    private Ball firstCollide;
    private double ballUnitX;
    private double ballUnitY;
    private double whiteCollisionX;
    private double whiteCollisionY;
    private final List<double[]> whitePath = new ArrayList<>();
    
    public WhitePrediction(Ball whiteBall) {
        whiteX = whiteBall.getX();
        whiteY = whiteBall.getY();
    }

    public List<double[]> getWhitePath() {
        return whitePath;
    }

    public void setFirstCollide(Ball firstCollide, 
                                double unitX, double unitY,
                                double whiteCollisionX, double whiteCollisionY) {
        this.firstCollide = firstCollide;
        this.ballUnitX = unitX;
        this.ballUnitY = unitY;
        this.whiteCollisionX = whiteCollisionX;
        this.whiteCollisionY = whiteCollisionY;
    }

    public Ball getFirstCollide() {
        return firstCollide;
    }

    public double getBallUnitX() {
        return ballUnitX;
    }

    public double getBallUnitY() {
        return ballUnitY;
    }

    public double getWhiteCollisionX() {
        return whiteCollisionX;
    }

    public double getWhiteCollisionY() {
        return whiteCollisionY;
    }
}
