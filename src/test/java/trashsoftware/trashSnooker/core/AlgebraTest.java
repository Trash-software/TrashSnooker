package trashsoftware.trashSnooker.core;

import org.junit.Test;

import java.util.Arrays;

public class AlgebraTest {
    
    @Test
    public void testGameValues() {
        double[] uv = Algebra.unitVectorOfAngle(Math.toRadians(270));
        System.out.println(Arrays.toString(uv));
    }
}
