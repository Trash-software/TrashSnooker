package trashsoftware.trashSnooker.core;

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
}
