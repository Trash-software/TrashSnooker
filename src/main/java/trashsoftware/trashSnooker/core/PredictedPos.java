package trashsoftware.trashSnooker.core;

public class PredictedPos {

    private final Ball targetBall;
    private final double[] whitePos;

    PredictedPos(Ball targetBall, double[] whitePos) {
        this.targetBall = targetBall;
        this.whitePos = whitePos;
    }

    public Ball getTargetBall() {
        return targetBall;
    }

    public double[] getPredictedWhitePos() {
        return whitePos;
    }
}
