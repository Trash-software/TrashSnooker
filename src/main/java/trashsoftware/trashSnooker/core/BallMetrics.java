package trashsoftware.trashSnooker.core;

public enum BallMetrics {
    SNOOKER_BALL(
            "斯诺克球", 52.5, 0.97
    ),
    POOL_BALL(
            "撞球", 57.15, 0.96
    );
    public final String name;
    public final double ballDiameter;
    public final double ballRadius;
    public final double ballWeightRatio;
    public final double ballBounceRatio;

    BallMetrics(String name,
                double ballDiameter, double ballBounceRatio, double ballWeightRatio) {
        this.name = name;
        this.ballDiameter = ballDiameter;
        this.ballRadius = ballDiameter / 2;
        this.ballWeightRatio = ballWeightRatio;
        this.ballBounceRatio = ballBounceRatio;
    }

    BallMetrics(String name,
                double ballDiameter, double ballBounceRatio) {
        this(name, ballDiameter, ballBounceRatio, Math.pow(ballDiameter, 3) / Math.pow(52.5, 3));
    }

    @Override
    public String toString() {
        return name;
    }
}
