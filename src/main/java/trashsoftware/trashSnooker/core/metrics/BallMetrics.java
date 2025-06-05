package trashsoftware.trashSnooker.core.metrics;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

public enum BallMetrics {
    SNOOKER_BALL(52.5, 0.96, 1.0, 145.0),
    POOL_BALL(57.15, 0.96, 1.05, 170.0),
    CAROM_BALL(61.5, 0.94, 1.15, 220.0),
    RUSSIAN_BALL(66.675, 0.93, 1.05, 255.0);
    public final double ballDiameter;
    public final double ballRadius;
    public final double ballWeightRatio;
    public final double ballBounceRatio;
    public final double frictionRatio;

    BallMetrics(double ballDiameter, double ballBounceRatio, double frictionRatio, double ballWeightGrams) {
        this.ballDiameter = ballDiameter;
        this.ballRadius = ballDiameter / 2;
        this.ballWeightRatio = ballWeightGrams / 145.0;
        this.ballBounceRatio = ballBounceRatio;
        this.frictionRatio = frictionRatio;
    }

    BallMetrics(double ballDiameter, double ballBounceRatio, double frictionRatio) {
        this(ballDiameter, ballBounceRatio, frictionRatio, Math.pow(ballDiameter, 3) / Math.pow(52.5, 3) * 145.0);
    }

    @Override
    public String toString() {
        String key = Util.toLowerCamelCase(name());
        return App.getStrings().getString(key);
    }
}
