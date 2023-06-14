package trashsoftware.trashSnooker.fxml;

import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GraphicsUtil {
    
    public static final double ENLARGE_RATIO = 1.25;
    
    private static double[][] enlargePointsArea(TableMetrics metrics, double[][] outPoints, double ratio) {
        double[] centroid = centroidOf(outPoints);
        
        double[][] result = new double[outPoints.length][2];
        
        for (int i = 0; i < outPoints.length; i++) {
            double[] point = outPoints[i];
            double dirX = point[0] - centroid[0];
            double dirY = point[1] - centroid[1];

            result[i][0] = centroid[0] + dirX * ratio;
            result[i][1] = centroid[1] + dirY * ratio;
            
            moveInTableIfNot(metrics, result[i]);
        }
        return result;
    }

    /**
     * 把5个点增到9个点(现在还没调好)
     */
    public static List<double[]> populatePoints(TableMetrics metrics,
                                                List<double[]> centerPath,
                                                double[][] fourCorners) {  // 必须按照顺序(左下，左上，右上，右下)
        if (centerPath.size() < 3) return new ArrayList<>();
        double[] centerStop = centerPath.get(centerPath.size() - 1);
        double[] centerSecondLast = centerPath.get(centerPath.size() - 3);  // 倒数第二个不太确定，但倒数第三个肯定没问题

        double[] direction = Algebra.unitVector(centerStop[0] - centerSecondLast[0], centerStop[1] - centerSecondLast[1]);
//        double[] normal = Algebra.normalVector(direction);
//        double[] negDirection = Algebra.reverseVector(direction);
//        double[] negNormal = Algebra.reverseVector(normal);

//        double[] smallLeftDir = new double[]{fourCorners[0][0] - centerStop[0], fourCorners[0][1] - centerStop[1]};
        double[] bigLeftDir = new double[]{fourCorners[1][0] - centerStop[0], fourCorners[1][1] - centerStop[1]};
        double[] bigRightDir = new double[]{fourCorners[2][0] - centerStop[0], fourCorners[2][1] - centerStop[1]};
//        double[] smallRightDir = new double[]{fourCorners[3][0] - centerStop[0], fourCorners[3][1] - centerStop[1]};

//        double[] left = generatePointBtw(centerStop, normal, smallLeftDir, bigLeftDir);
        double[] big = generatePointBtw(centerStop, direction, bigLeftDir, bigRightDir);
//        double[] right = generatePointBtw(centerStop, negNormal, bigRightDir, smallRightDir);
//        double[] small = generatePointBtw(centerStop, negDirection, smallRightDir, smallLeftDir);

        double[][] all = new double[][]{
                centerStop, 
//                left, 
                fourCorners[0], 
                big, 
                fourCorners[1], 
//                right, 
                fourCorners[2], 
//                small, 
                fourCorners[3]
        };

//        for (double[] p : all) {
//            moveInTableIfNot(metrics, p);
//        }
        
        return grahamScanEnclose(enlargePointsArea(metrics, all, ENLARGE_RATIO));
    }

    /**
     * 把3个点增到7个点
     */
    public static List<double[]> populatePoints(TableMetrics metrics,
                                                List<double[]> centerPath,
                                                double[] p1,
                                                double[] p2) {
        if (centerPath.size() < 3) return new ArrayList<>();
        double[] centerStop = centerPath.get(centerPath.size() - 1);
        double[] centerSecondLast = centerPath.get(centerPath.size() - 3);  // 倒数第二个不太确定，但倒数第三个肯定没问题

        double[] direction = Algebra.unitVector(centerStop[0] - centerSecondLast[0], centerStop[1] - centerSecondLast[1]);
        double[] normal = Algebra.normalVector(direction);

        // 左前点
        double[] p1Dir = new double[]{p1[0] - centerStop[0], p1[1] - centerStop[1]};

        double[][] frontSide = generateBySingle(centerStop, direction, normal, p1Dir);

        // 对于后点会稍微麻烦一些，因为要考虑刚刚弹库弹起来的球
        // todo: 先不考虑了
//        double d2 = Algebra.distanceToPoint(centerStop, p2);

        // 右后点
        double[] p2Dir = new double[]{p2[0] - centerStop[0], p2[1] - centerStop[1]};
        double[][] backSide = generateBySingle(centerStop, direction, normal, p2Dir);

        double[][] all = new double[][]{centerStop, frontSide[1], p1, frontSide[0], backSide[1], p2, backSide[0]};

//        for (double[] p : all) {
//            moveInTableIfNot(metrics, p);
//        }

        return grahamScanEnclose(enlargePointsArea(metrics, all, ENLARGE_RATIO));
    }
    
    public static List<double[]> processPoints(TableMetrics metrics, double[][] points) {
        return grahamScanEnclose(enlargePointsArea(metrics, points, ENLARGE_RATIO));
    }

    private static double[][] generateBySingle(double[] center,
                                               double[] whiteUnitDir,
                                               double[] whiteNormal,
                                               double[] pointToCenter) {
        double alongsideLength = Algebra.projectionLengthOn(whiteUnitDir, pointToCenter);
        double orthLength = Algebra.projectionLengthOn(whiteNormal, pointToCenter);
        return new double[][]{
                {
                        center[0] + whiteUnitDir[0] * alongsideLength,
                        center[1] + whiteUnitDir[1] * alongsideLength
                },
                {
                        center[0] + whiteNormal[0] * orthLength,
                        center[1] + whiteNormal[1] * orthLength
                },
        };
    }

    private static double[] generatePointBtw(double[] center,
                                             double[] base,  // 并非白球的方向，而是我们需要哪个方向
                                             double[] point1ToCenter,
                                             double[] point2ToCenter) {
        double len1 = Math.hypot(point1ToCenter[0], point1ToCenter[1]);
        double len2 = Math.hypot(point2ToCenter[0], point2ToCenter[1]);
        double resultLen = (len1 + len2) / 2;

        return new double[]{
                center[0] + base[0] * resultLen,
                center[1] + base[1] * resultLen
        };
    }

    private static void moveInTableIfNot(TableMetrics metrics, double[] p) {
        p[0] = Math.max(p[0], metrics.leftX);
        p[0] = Math.min(p[0], metrics.rightX);
        p[1] = Math.max(p[1], metrics.topY);
        p[1] = Math.min(p[1], metrics.botY);
    }
    
    private static double[] centroidOf(double[][] points) {
        double x = 0.0;
        double y = 0.0;
        
        for (double[] p : points) {
            x += p[0];
            y += p[1];
        }
        
        return new double[]{x / points.length, y / points.length};
    }

    /**
     * @return 返回可以围成凸包的所有点，按逆时针顺序
     */
    public static List<double[]> grahamScanEnclose(double[][] allPoints) {
        double[] basePoint = allPoints[0];
        for (double[] point : allPoints) {
            if (point[1] < basePoint[1]) {
                basePoint = point;  // 最下面的点作为基点
            }
        }

        final double[] base = basePoint;
        double[][] otherPoints = new double[allPoints.length - 1][];
        int oIndex = 0;
        for (double[] point : allPoints) {
            if (point != base) {
                otherPoints[oIndex++] = point;
            }
        }

        Arrays.sort(otherPoints, (o1, o2) -> {
            double o1vx = o1[0] - base[0];
            double o1vy = o1[1] - base[1];
            double o2vx = o2[0] - base[0];
            double o2vy = o2[1] - base[1];
            double cp = Algebra.crossProduct(o1vx, o1vy, o2vx, o2vy);
            if (cp < 0) return 1;
            else if (cp > 0) return -1;
            else {
                return Double.compare(Math.hypot(o1vx, o1vy), Math.hypot(o2vx, o2vy));
            }
        });

        List<double[]> stack = new ArrayList<>();
        stack.add(basePoint);
        stack.add(otherPoints[0]);

        int i = 1;
        while (i < otherPoints.length && stack.size() >= 2) {
            double[] point = otherPoints[i];
            double[] peek = stack.get(stack.size() - 1);
            double[] older = stack.get(stack.size() - 2);
            double[] lastEdge = new double[]{peek[0] - older[0], peek[1] - older[1]};
            double[] newEdge = new double[]{point[0] - peek[0], point[1] - peek[1]};
            double cross = Algebra.crossProduct(lastEdge, newEdge);
            if (cross >= 0) {
                stack.add(point);
                i++;
            } else {
                stack.remove(stack.size() - 1);
            }
        }
        return stack;
    }
}
