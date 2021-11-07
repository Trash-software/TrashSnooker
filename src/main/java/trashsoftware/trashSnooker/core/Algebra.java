package trashsoftware.trashSnooker.core;

import java.util.Arrays;

public class Algebra {

    public static double[] symmetricVector(double vx, double vy, double axisVX, double axisVY) {
        double scalar = 2 * vectorDot(vx, vy, axisVX, axisVY) / vectorDot(axisVX, axisVY, axisVX, axisVY);
        double mulX = axisVX * scalar;
        double mulY = axisVY * scalar;
        return new double[]{mulX - vx, mulY - vy};
    }

    public static double[] normalVector(double x, double y) {
        return new double[]{y, -x};
    }

    public static double[] normalVector(double[] vec) {
        return normalVector(vec[0], vec[1]);
    }

    public static double vectorDot(double ax, double ay, double bx, double by) {
        return ax * bx + ay * by;
    }

    public static double[] reverseVec(double[] vec) {
        return new double[]{-vec[0], -vec[1]};
    }

    public static double projectionLengthOn(double[] base, double[] vec) {
        double[] unitBase = unitVector(base);
        return vectorDot(vec[0], vec[1], unitBase[0], unitBase[1]);
    }

    public static double[] antiProjection(double[] base, double[] vecOnBase) {
        double[] unitBase = unitVector(base);  // 切线的单位向量
        double theta = thetaOf(vecOnBase);
        System.out.println("Theta: " + Math.toDegrees(theta));
        if (Double.isNaN(theta)) {
            System.out.println(Arrays.toString(vecOnBase) + ", " + Arrays.toString(unitBase));
        }
        double outUnitX = Math.cos(theta) * unitBase[0] - Math.sin(theta) * unitBase[1];
        double outUnitY = Math.sin(theta) * unitBase[0] + Math.cos(theta) * unitBase[1];
        double vecNorm = Math.hypot(vecOnBase[0], vecOnBase[1]);
        return new double[]{outUnitX * vecNorm, outUnitY * vecNorm};
    }

    /**
     * 返回向量与X轴正半轴的夹角，范围
     * 
     * @param x 向量的x
     * @param y 向量的y
     * @return 夹角
     */
    public static double thetaOf(double x, double y) {
        double atan = Math.atan(y / x);
        if (x < 0.0) {
            return Math.PI + atan;
        } else {
            return realMod(atan, Math.PI * 2);
        }
    }

    public static double thetaOf(double[] vec) {
        return thetaOf(vec[0], vec[1]);
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

    public static double normalizeAngle(double angleRad) {
        double ang = realMod(angleRad, Math.PI * 2);
        return ang > Math.PI ? ang - Math.PI * 2 : ang;
    }

    public static double[] angleToUnitVector(double angle) {
        double tan = Math.tan(angle);
        if (angle > Math.PI / 2 && angle <= Math.PI * 1.5) {
            return unitVector(-1.0, -tan);
        } else {
            return unitVector(1.0, tan);
        }
    }

    public static double distanceToPoint(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    public static double[] unitVector(double[] vec) {
        return unitVector(vec[0], vec[1]);
    }

    public static double[] unitVector(double x, double y) {
        double norm = Math.hypot(x, y);
        return new double[]{x / norm, y / norm};
    }

    public static double[] rotateVector(double x, double y, double angleRad) {
        double cosA = Math.cos(angleRad);
        double sinA = Math.sin(angleRad);
        return new double[]{
                x * cosA - y * sinA,
                x * sinA + y * cosA
        };
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
}
