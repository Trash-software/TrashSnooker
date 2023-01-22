package trashsoftware.trashSnooker.core;

import org.junit.Test;
import trashsoftware.trashSnooker.core.snooker.SnookerGame;

import java.util.Arrays;

public class CoreTest {

    public static void main(String[] args) {
        double[] lineSt = {0, 0};
        double[] lineEnd = {2, 2};
        System.out.println(Algebra.distanceToLine(-2, 1, lineSt, lineEnd));

        double[] a = {2, 1};
        double[] b = {1, 1};
        System.out.println(Algebra.projectionLengthOn(a, b));
        System.out.println("========");

        double[][] testVectors = {
                {1, 2},
                {1, 0},
                {0, 1},
                {-1, 0},
                {-1, -1},
                {0, -1},
                {1, -1},
                {-0.6588526302198671, -0.7522720330122363}
        };
        for (double[] tv : testVectors) {
            System.out.println(Math.toDegrees(Algebra.thetaOf(tv)));
        }

        System.out.println("=================");

        double[] angleDegrees = {
                0.0,
                45.0,
                90.0,
                135.0,
                180.0,
                225.0,
                270.0,
                315.0
        };
        for (double d : angleDegrees) {
            System.out.println(Arrays.toString(Algebra.angleToUnitVector(Math.toRadians(d))));
        }
    }
    
    @Test
    public void passBallTest() {
//        TableMetrics values = TableMetrics.SNOOKER_VALUES;
//
////        SnookerBall blue = new SnookerBall(5, new double[]{values.midX, values.midY}, values);
//        double[] hole = values.topRightHoleOpenCenter;
//        Game game = new SnookerGame(null, null, new GameSettings.Builder().build(), 1);
//        Ball blue = game.getAllBalls()[18];
//        boolean b = game.pointToPointCanPassBall(
//                blue.getX(), blue.getY(), hole[0], hole[1], blue, null, true, true
//        );
//        System.out.println(b);
    }
}
