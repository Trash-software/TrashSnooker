package trashsoftware.trashSnooker.fxml.projection;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.metrics.GameValues;

public class CushionProjection extends ObstacleProjection {
    // 和BallProjection类似，只不过不是中心。
    // 0代表球心，1代表球底，-1代表球顶（-1不可能）
    private final double lineY;

    public CushionProjection(GameValues gameValues,
                             Ball whiteBall, double distance, double cueAngleDeg,
                             double cueRadius) {
        // 简化版，不考虑角度
        double realDt = distance - cueRadius;
        if (realDt < 0) {
            System.err.println("你豁我" + distance);
            realDt = 0.0;
        }
        double down = Math.tan(Math.toRadians(cueAngleDeg)) * realDt;  // 杆向下的垂直高度
        double dtToBot = gameValues.table.cushionHeight - down;  // 最低点离球底的位置

        lineY = (0.5 - dtToBot / gameValues.ball.ballDiameter) * 2;
    }

    public double getLineY() {
        return lineY;
    }

    @Override
    public boolean cueAble(double pointX, double pointY, double cueRadiusRatio) {
        return pointY + cueRadiusRatio < lineY;
    }
}
