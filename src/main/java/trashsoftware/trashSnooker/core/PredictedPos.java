package trashsoftware.trashSnooker.core;

import java.util.Arrays;

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

    @Override
    public String toString() {
        return "PredictedPos{" +
                "targetBall=" + targetBall +
                ", whitePos=" + Arrays.toString(whitePos) +
                '}';
    }
}
