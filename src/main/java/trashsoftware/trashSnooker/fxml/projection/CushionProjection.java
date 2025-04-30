package trashsoftware.trashSnooker.fxml.projection;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.metrics.GameValues;

public class CushionProjection extends ObstacleProjection {
    // 和BallProjection类似，只不过不是中心。
    // 0代表球心，1代表球底，-1代表球顶（-1不可能）
    private final double lineYLeft;
    private final double lineYRight;
    
    private final double slope;

    public CushionProjection(GameValues gameValues,
                             double distance, 
                             double angleBtwCushionAndCue,
                             double cueAngleDeg,
                             double cueRadius) {
        double realDt = distance - cueRadius;

        // 球心与球边边离库距离的区别。仍未考虑球本身的弧度
        double ballSideCenterDtDiff = Math.tan(angleBtwCushionAndCue) * gameValues.ball.ballRadius;
        double dtBallLeft = realDt + ballSideCenterDtDiff;
        double dtBallRight = realDt - ballSideCenterDtDiff;
        
        dtBallLeft = Math.max(dtBallLeft, 0.0);
        dtBallRight = Math.max(dtBallRight, 0.0);

//        System.out.println("Left: " + dtBallLeft + ", right: " + dtBallRight);
        double cueAngleTan = Math.tan(Math.toRadians(cueAngleDeg));
        
        double leftDown = cueAngleTan * dtBallLeft;
        double rightDown = cueAngleTan * dtBallRight;
        
        double leftToBot = gameValues.table.cushionHeight - leftDown;
        double rightToBot = gameValues.table.cushionHeight - rightDown;
                
//        double down = Math.tan(Math.toRadians(cueAngleDeg)) * realDt;  // 杆向下的垂直高度
//        double dtToBot = gameValues.table.cushionHeight - down;  // 最低点离球底的位置

        lineYLeft = (0.5 - leftToBot / gameValues.ball.ballDiameter) * 2;
        lineYRight = (0.5 - rightToBot / gameValues.ball.ballDiameter) * 2;
        
        slope = (lineYRight - lineYLeft);
    }

    public double getLineYLeft() {
        return lineYLeft;
    }

    public double getLineYRight() {
        return lineYRight;
    }
    
    private double getYatX(double x) {
        // x从-1到1
        x = (x + 1) / 2;
        return lineYLeft + x * slope;
    }

    @Override
    public boolean cueAble(double pointX, double pointY, double cueRadiusRatio) {
//        return pointY + cueRadiusRatio < lineYLeft;
        double yLim = getYatX(pointX);
        return pointY + cueRadiusRatio < yLim;
    }
}
