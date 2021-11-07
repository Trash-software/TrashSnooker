package trashsoftware.trashSnooker.fxml.projection;

public abstract class ObstacleProjection {

    /**
     * 返回给定的击球点是否可行
     * 
     * @param pointX 打点的横向值，左负右正，区间在{-1, 1} 之间
     * @param pointY 打点的纵向值，上负下正
     * @param cueRadiusRatio 杆头本身的半径/球的半径
     */
    public abstract boolean cueAble(double pointX, double pointY, double cueRadiusRatio);
}
