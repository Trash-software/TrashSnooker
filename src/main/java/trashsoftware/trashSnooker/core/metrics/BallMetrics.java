package trashsoftware.trashSnooker.core.metrics;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

public enum BallMetrics {
    SNOOKER_BALL(52.5, 0.92, 145.0),
    POOL_BALL(57.15, 0.92, 170.0),
    CAROM_BALL(61.5, 0.91, 220.0),
    RUSSIAN_BALL(66.675, 0.90, 255.0);
    public final double ballDiameter;
    public final double ballRadius;
    public final double ballWeightRatio;
    public final double ballBounceRatio;

    BallMetrics(double ballDiameter, double ballBounceRatio, double ballWeightGrams) {
        this.ballDiameter = ballDiameter;
        this.ballRadius = ballDiameter / 2;
        this.ballWeightRatio = ballWeightGrams / 145.0;
        this.ballBounceRatio = ballBounceRatio;
    }

    BallMetrics(double ballDiameter, double ballBounceRatio) {
        this(ballDiameter, ballBounceRatio, Math.pow(ballDiameter, 3) / Math.pow(52.5, 3) * 145.0);
    }

    @Override
    public String toString() {
        String key = Util.toLowerCamelCase(name());
        return App.getStrings().getString(key);
    }
}
