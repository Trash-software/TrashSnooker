package trashsoftware.trashSnooker.fxml.drawing;

import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.essential.Ball;
import trashsoftware.trashSnooker.core.metrics.GameValues;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShadowInspection {
    
    protected final double[] whitePos;
    protected final List<Ball> obstacleBalls, legalBalls;
    protected final GameValues gameValues;
    protected final List<BallShadow> shadows = new ArrayList<>();
    
    public ShadowInspection(double[] whitePos, 
                            List<Ball> obstacleBalls, 
                            List<Ball> legalBalls, 
                            GameValues gameValues) {
        this.whitePos = whitePos;
        this.obstacleBalls = obstacleBalls;
        this.legalBalls = legalBalls;
        this.gameValues = gameValues;
        
        analyze();
    }
    
    private void analyze() {
        for (Ball obstacle : obstacleBalls) {
            BallShadow shadow = oneBallShadow(obstacle);
            // todo: 去掉无效的
            shadows.add(shadow);
        }
    }
    
    private BallShadow oneBallShadow(Ball ball) {
        // 每一边相当于两个对顶的全等直角三角形
        // 两个球心的连线是斜边*2
        // 球的半径是对边
        // 需要的切线就是直角边
        // tsDraft-ballShadowDraft.png
        double[] ballPos = ball.getPositionArray();
        double[] connection = Algebra.vectorSubtract(ballPos, whitePos);
        double[] midPoint = Algebra.vectorScale(Algebra.vectorAdd(ballPos, whitePos), 0.5);
        double diameter = gameValues.ball.ballDiameter;
        
        double dt = Math.hypot(connection[0], connection[1]);
        double alpha = Math.asin(diameter / dt);
        double[] leftDir = Algebra.unitVector(Algebra.rotateVector(connection[0], connection[1], -alpha));
        double[] rightDir = Algebra.unitVector(Algebra.rotateVector(connection[0], connection[1], alpha));
        // 勾股定理，找到直角边长度
        double baseLength = Math.sqrt(dt * dt - diameter * diameter) / 2;
        double[] leftStart = Algebra.vectorAdd(midPoint, Algebra.vectorScale(leftDir, baseLength));
        double[] rightStart = Algebra.vectorAdd(midPoint, Algebra.vectorScale(rightDir, baseLength));
        return new BallShadow(ball, leftStart, rightStart, leftDir, rightDir);
    }

    public List<BallShadow> getShadows() {
        return shadows;
    }

    public record BallShadow(Ball ball, 
                             double[] leftStart, 
                             double[] rightStart, 
                             double[] leftRayDirection,
                             double[] rightRayDirection) {

        @Override
        public String toString() {
            return "BallShadow[" + ball + 
                    ", left=" + Arrays.toString(leftStart) + " towards " + Arrays.toString(leftRayDirection) + 
                    ", right=" + Arrays.toString(rightStart) + " towards " + Arrays.toString(rightRayDirection) + "]";
        }
    }
}
