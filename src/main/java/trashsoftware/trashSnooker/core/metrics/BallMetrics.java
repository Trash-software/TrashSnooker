package trashsoftware.trashSnooker.core.metrics;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

public enum BallMetrics {
    SNOOKER_BALL(52.5, 0.97),
    POOL_BALL(57.15, 0.96);
    public final double ballDiameter;
    public final double ballRadius;
    public final double ballWeightRatio;
    public final double ballBounceRatio;

    BallMetrics(double ballDiameter, double ballBounceRatio, double ballWeightRatio) {
        this.ballDiameter = ballDiameter;
        this.ballRadius = ballDiameter / 2;
        this.ballWeightRatio = ballWeightRatio;
        this.ballBounceRatio = ballBounceRatio;
    }

    BallMetrics(double ballDiameter, double ballBounceRatio) {
        this(ballDiameter, ballBounceRatio, Math.pow(ballDiameter, 3) / Math.pow(52.5, 3));
    }

    @Override
    public String toString() {
        String key = Util.toLowerCamelCase(name());
        return App.getStrings().getString(key);
    }
}
