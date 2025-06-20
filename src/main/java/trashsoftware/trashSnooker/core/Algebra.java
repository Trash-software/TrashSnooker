package trashsoftware.trashSnooker.core;

import java.util.*;

public class Algebra {
    
    public static final double SQRT2 = Math.sqrt(2);
    public static final double HALF_SQRT2 = SQRT2 / 2;
    public static final double TWO_PI = Math.PI * 2;
    public static final double HALF_PI = Math.PI / 2;  // 90度
    public static final double QUARTER_PI = Math.PI / 4;  // 45度
    
    public static final double[] X_AXIS = {1, 0};
    public static final double[] Y_AXIS = {0, 1};

    public static double[] symmetricVector(double vx, double vy, double axisVX, double axisVY) {
        double scalar = 2 * vectorDot(vx, vy, axisVX, axisVY) / vectorDot(axisVX, axisVY, axisVX, axisVY);
        double mulX = axisVX * scalar;
        double mulY = axisVY * scalar;
        return new double[]{mulX - vx, mulY - vy};
    }

    /**
     * 返回法向量，在原向量的右手边90度
     */
    public static double[] normalVector(double x, double y) {
        return new double[]{y, -x};
    }

    public static double[] normalVector(double[] vec) {
        return normalVector(vec[0], vec[1]);
    }
    
    public static double[] vectorSubtract(double[] a, double[] b) {
        return new double[]{a[0] - b[0], a[1] - b[1]};
    }

    public static double vectorDot(double ax, double ay, double bx, double by) {
        return ax * bx + ay * by;
    }

    public static double[] reverseVec(double[] vec) {
        return new double[]{-vec[0], -vec[1]};
    }
    
    public static boolean pointAtLeftOfVec(double[] vecStart, double[] vecEnd, double[] point) {
        double temp = (vecStart[1] - vecEnd[1]) * point[0] +
                (vecEnd[0] - vecStart[0]) * point[1] + 
                vecStart[0] * vecEnd[1] - vecEnd[0] * vecStart[1];
        return temp < 0;
    }

    /**
     * 返回vec在base上的投影的长度
     */
    public static double projectionLengthOn(double[] base, double[] vec) {
        double[] unitBase = unitVector(base);
        return vectorDot(vec[0], vec[1], unitBase[0], unitBase[1]);
    }

    public static double[] antiProjection(double[] base, double[] vecOnBase) {
        double[] unitBase = unitVector(base);  // 切线的单位向量
        double theta = thetaOf(vecOnBase);
//        System.out.println("Theta: " + Math.toDegrees(theta));
        if (Double.isNaN(theta)) {
            System.err.println(Arrays.toString(vecOnBase) + ", " + Arrays.toString(unitBase));
        }
        double outUnitX = Math.cos(theta) * unitBase[0] - Math.sin(theta) * unitBase[1];
        double outUnitY = Math.sin(theta) * unitBase[0] + Math.cos(theta) * unitBase[1];
        double vecNorm = Math.hypot(vecOnBase[0], vecOnBase[1]);
        return new double[]{outUnitX * vecNorm, outUnitY * vecNorm};
    }
    
    public static double[][] changeOfBasisMatrix(double rotate) {
        double sin = Math.sin(rotate);
        double cos = Math.cos(rotate);
        return new double[][]{
                {cos, -sin},
                {sin, cos}
        };
    }
    
    public static double[] matrixMultiplyVector(double[][] matrix, double[] vector) {
        double[] res = new double[matrix.length];
        for (int r = 0; r < matrix.length; r++) {
            for (int c = 0; c < matrix[r].length; c++) {
                res[r] += matrix[r][c] * vector[c];
            }
        }
        return res;
    }

    /**
     * @param angleRad 角，弧度
     * @return 力经过一个角度之后剩下的比例，角度越大，力传得越差，范围[0,1]
     */
    public static double powerTransferOfAngle(double angleRad) {
        return (Math.PI / 2 - angleRad) / Math.PI * 2;
    }

    /**
     * 返回向量与X轴正半轴的夹角，范围 [0~2PI)
     * 
     * @param x 向量的x
     * @param y 向量的y
     * @return 夹角
     */
    public static double thetaOf(double x, double y) {
        double a = Math.atan2(y, x);
        if (a < 0.0) a += TWO_PI;
        return a;
    }
    
    public static double rawThetaOf(double x, double y) {
        return Math.atan2(y, x);
    }
    
    public static double rawThetaOf(double[] vec) {
        return rawThetaOf(vec[0], vec[1]);
    }

    public static double thetaOf(double[] vec) {
        return thetaOf(vec[0], vec[1]);
    }
    
    public static double thetaBetweenVectors(double[] v1, double[] v2) {
        return thetaBetweenVectors(v1[0], v1[1], v2[0], v2[1]);
    }

    /**
     * 返回两个向量之间的夹角，小于等于180度
     */
    public static double thetaBetweenVectors(double v1x, double v1y, double v2x, double v2y) {
        double t1 = thetaOf(v1x, v1y);
        double t2 = thetaOf(v2x, v2y);
        
//        double theta = Math.abs(t1 - t2);
//        if (theta > Math.PI) {
//            theta = Math.PI * 2 - theta;
//        }
//        return theta;
        return angleBetweenTwoAngles(t1, t2);
    }

    /**
     * @param radA 方位角A，非负
     * @param radB 方位角B，非负
     * @return 夹角，非负
     */
    public static double angleBetweenTwoAngles(double radA, double radB) {
        double theta = Math.abs(radA - radB);
        if (theta > Math.PI) {
            theta = Math.PI * 2 - theta;
        }
        if (theta < 0) throw new RuntimeException();
        return theta;
    }

    /**
     * 
     * @param radA 方位角A，任意范围
     * @param radB 方位角B，任意范围
     * @return 若A在B之左，返回-1；若A在B之右，返回1；若A,B重合，返回0
     */
    public static int compareAngleDirection(double radA, double radB) {
        radA = normalizeAnglePositive(radA);
        radB = normalizeAnglePositive(radB);

        // Calculate the difference from A to B counter-clockwise
        double diff = (radB - radA + TWO_PI) % TWO_PI;

        if (diff == 0) {
            return 0; // Same direction
        } else if (diff < Math.PI) {
            return -1; // A is to the left of B
        } else {
            return 1; // A is to the right of B
        }
    }

    public static int compareAngleDirectionDeg(double degA, double degB) {
        return compareAngleDirection(Math.toRadians(degA), Math.toRadians(degB));
    }

    /**
     * @param vector 二维向量
     * @return 向量的象限。如果在坐标轴上则返回其右上侧的象限。如果在原点，返回0
     */
    public static int quadrant(double[] vector) {
        if (vector[0] == 0 && vector[1] == 0) return 0;  // original point
        else if (vector[0] >= 0) {
            if (vector[1] >= 0) return 1;
            else return 4;
        } else {
            if (vector[1] >= 0) return 2;
            else return 3;
        }
    }

    public static double realMod(double x, double mod) {
        return x < 0 ? x % mod + mod : x % mod;
    }

    /**
     * 把一个角转化为[-PI, PI)内
     */
    public static double normalizeAngle(double angleRad) {
        double ang = realMod(angleRad, TWO_PI);
        return ang > Math.PI ? ang - TWO_PI : ang;
    }
    
    public static double normalizeAngleDeg(double angleDeg) {
        return Math.toDegrees(normalizeAngle(Math.toRadians(angleDeg)));
    }

    /**
     * 把一个角转化为[0, 2PI)内
     */
    public static double normalizeAnglePositive(double angleRad) {
        return realMod(angleRad, TWO_PI);
    }

    // 这两个到底是怎么在混用，没看懂
    public static double[] unitVectorOfAngle(double angleRad) {
        return new double[]{Math.cos(angleRad), Math.sin(angleRad)};
    }

    @Deprecated
    public static double[] angleToUnitVector(double angle) {
        double tan = Math.tan(angle);
        if (angle > Math.PI / 2 && angle <= Math.PI * 1.5) {
            return unitVector(-1.0, -tan);
        } else {
            return unitVector(1.0, tan);
        }
    }

    /**
     * 返回角平分线的弧度值，在[0, 2PI)之间
     */
    public static double angularBisector(double radA, double radB) {
//        return angularSector(radA, radB, 0.5);
        radA = normalizeAnglePositive(radA);
        radB = normalizeAnglePositive(radB);

        // 这里需要处理内角外角的问题
        double res = normalizeAnglePositive((radA + radB) / 2);
        if (angleBetweenTwoAngles(res, radA) > HALF_PI) {
            // 内角平分线和两个边的夹角必定小于等于90度
            return normalizeAnglePositive(res + Math.PI);
        } else {
            return res;
        }
    }

    /**
     * 返回从角a走到角b，走ratio那么多的比例时走到的角的弧度值，在[0, 2PI)之间
     */
    public static double angularSector(double radA, double radB, double ratio) {
        radA = normalizeAnglePositive(radA);
        radB = normalizeAnglePositive(radB);
        if (radA > radB) {
            double tmp = radA;
            radA = radB;
            radB = tmp;
        }

        // 这里需要处理内角外角的问题
        double res = normalizeAnglePositive(radA + (radB - radA) * ratio);
        if (angleBetweenTwoAngles(res, radA) > HALF_PI) {
            // 内角平分线和两个边的夹角必定小于等于90度
            return normalizeAnglePositive(res + Math.PI);
        } else {
            return res;
        }
    }
    
//    public static double[] angularBisector(double[] angleA, double[] angleB) {
//        
//    }

    public static double distanceToPoint(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    public static double distanceToPoint(double[] v1, double[] v2) {
        return distanceToPoint(v1[0], v1[1], v2[0], v2[1]);
    }

    /**
     * @param a 点A
     * @param b 点B
     * @param p 要检查的点
     * @return p是否投影在ab连线上
     */
    public static boolean isBetweenPerpendiculars(double[] a, double[] b, double[] p) {
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];

        double abSquared = dx * dx + dy * dy;

        if (abSquared == 0) {
            // A and B are the same point — treat as not between
            return false;
        }

        // Vector from A to P
        double apx = p[0] - a[0];
        double apy = p[1] - a[1];

        // Projection of AP onto AB (normalized to [0, 1])
        double t = (apx * dx + apy * dy) / abSquared;

        return t >= 0.0 && t <= 1.0;
    }

    public static double[] unitVector(double[] vec) {
        return unitVector(vec[0], vec[1]);
    }

    public static double[] unitVector(double x, double y) {
        double norm = Math.hypot(x, y);
        return new double[]{x / norm, y / norm};
    }
    
    public static double[] reverseVector(double[] vec) {
        return new double[]{-vec[0], -vec[1]};
    }

    public static double[] rotateVector(double x, double y, double angleRad) {
        double cosA = Math.cos(angleRad);
        double sinA = Math.sin(angleRad);
        return new double[]{
                x * cosA - y * sinA,
                x * sinA + y * cosA
        };
    }

    public static double distanceToLine(double[] pos, double[][] line) {
        return distanceToLine(pos[0], pos[1], line[0], line[1]);
    }

    public static double distanceToLine(double x, double y, double[] lineStartXY, double[] lineEndXY) {
        double x1 = lineStartXY[0];
        double x2 = lineEndXY[0];
        double y1 = lineStartXY[1];
        double y2 = lineEndXY[1];

        double pqx = x2 - x1;
        double pqy = y2 - y1;
        double dx = x - x1;
        double dy = y - y1;
        double d = pqx * pqx + pqy * pqy;  // qp线段长度的平方
        double t = pqx * dx + pqy * dy;  // p pt向量 点积 pq 向量（p相当于A点，q相当于B点，pt相当于P点）
        if (d > 0) // 除数不能为0; 如果为零 t应该也为零。下面计算结果仍然成立。
            t /= d;// 此时t 相当于 上述推导中的 r。
        if (t < 0)
            t = 0;// 当t（r）< 0时，最短距离即为 pt点 和 p点（A点和P点）之间的距离。
        else if (t > 1)
            t = 1;// 当t（r）> 1时，最短距离即为 pt点 和 q点（B点和P点）之间的距离。

        // t = 0，计算 pt点 和 p点的距离; t = 1, 计算 pt点 和 q点 的距离; 否则计算 pt点 和 投影点 的距离。
        dx = x1 + t * pqx - x;
        dy = y1 + t * pqy - y;
        return Math.hypot(dx, dy);
    }

    public static double crossProduct(double ax, double ay, double bx, double by) {
        return ax * by - bx * ay;
    }
    
    public static double crossProduct(double[] a, double[] b) {
        return crossProduct(a[0], a[1], b[0], b[1]);
    }
    
    public static double[] vectorScale(double[] a, double x) {
        double[] res = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            res[i] = a[i] * x;
        }
        return res;
    }
    
    public static int log2(int powerOf2) {
        int bit = powerOf2 - 1;
        int count = 0;
        while (bit != 0) {
            count++;
            bit >>= 1;
        }
        return count;
    }

    /**
     * 把值从domain映射到range里
     */
    public static double shiftRange(double domainLow, double domainHigh,
                                    double rangeLow, double rangeHigh,
                                    double value) {
        if (value < domainLow || value > domainHigh) throw new ArithmeticException();
        double ratio = (value - domainLow) / (domainHigh - domainLow);
        return rangeLow + (rangeHigh - rangeLow) * ratio;
    }

    /**
     * 把值从domain映射到range里
     */
    public static double shiftRangeSafe(double domainLow, double domainHigh,
                                        double rangeLow, double rangeHigh,
                                        double value) {
        if (value < domainLow) return rangeLow;
        if (value > domainHigh) return rangeHigh;
        double ratio = (value - domainLow) / (domainHigh - domainLow);
        return rangeLow + (rangeHigh - rangeLow) * ratio;
    }

    /**
     * 返回从a往b走，走到rate时所在的位置
     */
    public static double rateBetween(double a, double b, double rate) {
        return a + (b - a) * rate;
    }

    public static double[] generateSkewedRange(double a, double b, int n, double exponent) {
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            double t = (double) i / (n - 1);      // normalized [0, 1]
            double skewedT = Math.pow(t, exponent); // apply exponent
            result[i] = a + (b - a) * skewedT;      // map back to [a, b]
        }
        return result;
    }

    public static void main(String[] args) {
        double[] skr = generateSkewedRange(10, 100, 10, 1.35);
        System.out.println(Arrays.toString(skr));
    }
}
