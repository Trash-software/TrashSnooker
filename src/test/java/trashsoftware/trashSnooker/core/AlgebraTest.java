package trashsoftware.trashSnooker.core;

import org.junit.Test;
import trashsoftware.trashSnooker.fxml.GraphicsUtil;

import java.util.Arrays;
import java.util.List;

public class AlgebraTest {
    
    @Test
    public void testAngleBtwVectors() {
        double[] u = new double[]{-1, -3};
        double[] v = new double[]{2, 1};
        double theta = Algebra.thetaBetweenVectors(v, u);
        System.out.println(theta);
    }
    
    @Test
    public void testGameValues() {
        double[] uv = Algebra.unitVectorOfAngle(Math.toRadians(270));
        System.out.println(Arrays.toString(uv));
    }
    
    @Test
    public void thetaTest() {
        double[] u = new double[]{1, 1, 0, -1, -1, -1, 0, 1,};
        double[] v = new double[]{0, 1, 1, 1, 0, -1, -1, -1};
        
        for (int i = 0; i < u.length; i++) {
            System.out.println(Math.toDegrees(Algebra.thetaOf(u[i], v[i])));
        }
    }
    
    @Test
    public void crossProduct() {
        double[] a = {1, 1};
        double[] b = {1, 0.9};
        double cp = Algebra.crossProduct(a, b);
        System.out.println(cp);
    }
    
    @Test
    public void testAngleNormalization() {
        double[] anglesDeg = {
                0, 1, 45, 89, 91, 136, 179, 181, 269, 271, 359, 360
        };
        double[] result = new double[anglesDeg.length];
        for (int i = 0; i < anglesDeg.length; i++) {
            double normalized = Algebra.normalizeAngle(Math.toRadians(anglesDeg[i]));
            result[i] = Math.toDegrees(normalized);
        }
        System.out.println(Arrays.toString(result));
    }
    
    @Test
    public void testUnitVectorOfAngle() {
        double[] anglesDeg = {
                0, 1, 45, 89, 91, 136, 179, 181, 269, 271, 359, 360
        };
        double[][] result1 = new double[anglesDeg.length][];
        double[][] result2 = new double[anglesDeg.length][];
        for (int i = 0; i < anglesDeg.length; i++) {
            double rad = Algebra.normalizeAngle(Math.toRadians(anglesDeg[i]));
            double[] a = Algebra.unitVectorOfAngle(rad);
            double[] b = Algebra.angleToUnitVector(rad);
            System.out.println(Math.toDegrees(rad) + ": " + Arrays.toString(a) + ", " + Arrays.toString(b));
        }
        System.out.println(Arrays.toString(Algebra.unitVectorOfAngle(Math.toRadians(361))));
    }
    
    @Test
    public void testAverageAngle() {
        double degA = 170;
        double degB = -160;
        double radA = Math.toRadians(degA);
        double radB = Math.toRadians(degB);
        System.out.println(Arrays.toString(Algebra.unitVectorOfAngle(radA)));
        System.out.println(Arrays.toString(Algebra.unitVectorOfAngle(radB)));
        System.out.println(Arrays.toString(Algebra.unitVectorOfAngle(Algebra.angularBisector(radA, radB))));
    }
    
    @Test
    public void testAngleBetween() {
        
    }
    
    @Test
    public void grahamTest() {
        double[][] points = {
                {0, 0},
                {5, 1},
                {-3, 2},
                {-2, 4},
                {1, 3},
                {2, 6},
                {2, 4},
                {2, 2},
                {4, 4}
        };
        List<double[]> res = GraphicsUtil.grahamScanEnclose(points);
        for (double[] r : res) System.out.print(Arrays.toString(r) + ", ");
        System.out.println();

        System.out.println(Algebra.crossProduct(-3, 1, 2, 2));
    }
    
    @Test
    public void ghTest2() {
        double[][] points = {
                {773.8378039026978, 384.4221329421463}, 
                {756.996003606448, 441.5591860521185}, 
                {588.2948226160563, 513.8049905119274},
                {453.4762131407564, 543.8717349818761}, 
                {593.0864264326176, 453.61293660054866}, 
                {379.56705239833514, 523.1432377201606}, 
                {455.03702304169775, 454.8637108619834}, 
                {642.0195132182149, 388.973111323249}
        };
    }
    
    @Test
    public void negativeShift() {
        System.out.println(Algebra.shiftRange(0, 100, 2, 0, 1));
    }
}
