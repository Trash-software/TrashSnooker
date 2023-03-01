package trashsoftware.trashSnooker.core;

import org.junit.Test;

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
    public void crossProduct() {
        double[] a = {1, 1};
        double[] b = {1, 0.9};
        double cp = Algebra.crossProduct(a, b);
        System.out.println(cp);
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
        List<double[]> res = Algebra.grahamScanEnclose(points);
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
}
