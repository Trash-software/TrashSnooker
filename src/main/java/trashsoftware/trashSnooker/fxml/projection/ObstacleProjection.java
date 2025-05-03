package trashsoftware.trashSnooker.fxml.projection;

import org.jetbrains.annotations.Nullable;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.CueBackPredictor;
import trashsoftware.trashSnooker.core.metrics.GameValues;

public abstract class ObstacleProjection {

    /**
     * 返回给定的击球点是否可行
     * 
     * @param pointX 打点的横向值，左负右正，区间在{-1, 1} 之间
     * @param pointY 打点的纵向值，上负下正
     * @param cueRadiusRatio 杆头本身的半径/球的半径
     */
    public abstract boolean cueAble(double pointX, double pointY, double cueRadiusRatio);
    
    @Nullable
    public static ObstacleProjection createProjection(CueBackPredictor.Result backPre,
                                                      double dirX, 
                                                      double dirY,
                                                      double cueAngleDeg,
                                                      Ball cueBall,
                                                      GameValues gameValues,
                                                      double cueTipRadius) {
        if (backPre != null) {
            if (backPre instanceof CueBackPredictor.CushionObstacle cushionObstacle) {
                // 影响来自裤边
                return new CushionProjection(
                        gameValues,
                        cushionObstacle.distance,
                        cushionObstacle.relativeAngle,
                        cueAngleDeg,
                        cueTipRadius);
            } else if (backPre instanceof CueBackPredictor.BallObstacle ballObstacle) {
                // 后斯诺
                return new BallProjection(
                        ballObstacle.obstacle, 
                        cueBall,
                        dirX, 
                        dirY,
                        cueAngleDeg);
            }
        }
        return null;
    }
}
