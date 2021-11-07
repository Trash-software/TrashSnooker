package trashsoftware.trashSnooker.fxml.projection;

import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.Ball;

public class BallProjection extends ObstacleProjection {
    
    private final double centerHor;  // 目标球在白球上的投影中心，x值，单位为球半径的倍数，原点为白球中心
    private final double centerVer;  // 目标球在白球上垂直方向的投影中心，非负数

    /**
     *          White ------> cue aiming
     *          /|
     *       a/  |p
     *      /    |
     * Ball------>----------> cue aiming(平行)
     *        d
     */
    public BallProjection(Ball ball, Ball whiteBall, 
                          double directionUnitX, double directionUnitY, 
                          double cueAngleDeg) {
        // 障碍球指向白球的向量(a)
        double[] ballToWhite = new double[]{whiteBall.getX() - ball.getX(), 
                whiteBall.getY() - ball.getY()};
        
        // 投影的长度(||d||)
        double projLen = Algebra.projectionLengthOn(
                new double[]{directionUnitX, directionUnitY},
                ballToWhite);
        double[] projVec = new double[]{projLen * directionUnitX, projLen * directionUnitY};
        // 投影的高度(||p||)
        double[] projOrthogonal = Algebra.vectorSubtract(ballToWhite, projVec);
        double projOffset = Math.hypot(projOrthogonal[0], projOrthogonal[1]);
        
        boolean ballAtLineLeft = Algebra.pointAtLeftOfVec(
                new double[]{whiteBall.getX(), whiteBall.getY()},
                new double[]{whiteBall.getX() + directionUnitX, whiteBall.getY() + directionUnitY},
                new double[]{ball.getX(), ball.getY()});
        
        double horFactor = ballAtLineLeft ? -1 : 1;
        
        centerHor = projOffset / whiteBall.getRadius() * horFactor;
        
        // 垂直方向投影
        double tan = Math.tan(Math.toRadians(cueAngleDeg));
        centerVer = tan * (projLen - ball.getRadius()) / whiteBall.getRadius();
    }

    public double getCenterHor() {
        return centerHor;
    }

    public double getCenterVer() {
        return centerVer;
    }

    @Override
    public boolean cueAble(double pointX, double pointY, double cueRadiusRatio) {
        double dt = Algebra.distanceToPoint(pointX, pointY, centerHor, centerVer);
//        System.out.println(pointX + " " + pointY + " " + centerHor + " " + centerVer);
        return dt - cueRadiusRatio > 1;
    }
}
