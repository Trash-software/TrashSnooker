package trashsoftware.trashSnooker.util;

public class PointInPoly {

//    public static class Point {
//        public double x, y;
//
//        public Point(double x, double y) {
//            this[0] = x;
//            this[1] = y;
//        }
//    }

    public static class Line {
        public double[] p1, p2;

        public Line(double[] p1, double[] p2) {
            this.p1 = p1;
            this.p2 = p2;
        }
    }

    static int onLine(Line l1, double[] p) {
        // Check whether p is on the line or not
        if (p[0] <= Math.max(l1.p1[0], l1.p2[0])
                && p[0] >= Math.min(l1.p1[0], l1.p2[0])
                && (p[1] <= Math.max(l1.p1[1], l1.p2[1])
                && p[1] >= Math.min(l1.p1[1], l1.p2[1])))
            return 1;

        return 0;
    }

    static int direction(double[] a, double[] b, double[] c) {
        double val = (b[1] - a[1]) * (c[0] - b[0])
                - (b[0] - a[0]) * (c[1] - b[1]);

        if (val == 0)

            // Collinear
            return 0;

        else if (val < 0)

            // Anti-clockwise direction
            return 2;

        // Clockwise direction
        return 1;
    }

    static int isIntersect(Line l1, Line l2) {
        // Four direction for two lines and points of other
        // line
        int dir1 = direction(l1.p1, l1.p2, l2.p1);
        int dir2 = direction(l1.p1, l1.p2, l2.p2);
        int dir3 = direction(l2.p1, l2.p2, l1.p1);
        int dir4 = direction(l2.p1, l2.p2, l1.p2);

        // When intersecting
        if (dir1 != dir2 && dir3 != dir4)
            return 1;

        // When p2 of line2 are on the line1
        if (dir1 == 0 && onLine(l1, l2.p1) == 1)
            return 1;

        // When p1 of line2 are on the line1
        if (dir2 == 0 && onLine(l1, l2.p2) == 1)
            return 1;

        // When p2 of line1 are on the line2
        if (dir3 == 0 && onLine(l2, l1.p1) == 1)
            return 1;

        // When p1 of line1 are on the line2
        if (dir4 == 0 && onLine(l2, l1.p2) == 1)
            return 1;

        return 0;
    }

    static int checkInside(double[][] poly, int n, double[] p) {

        // When polygon has less than 3 edge, it is not
        // polygon

        if (n < 3)
            return 0;

        // Create a point at infinity, y is same as point p
        double[] pt = new double[]{9999, p[1]};
        Line exline = new Line(p, pt);
        int count = 0;
        int i = 0;
        do {

            // Forming a line from two consecutive points of
            // poly
            Line side
                    = new Line(poly[i], poly[(i + 1) % n]);
            if (isIntersect(side, exline) == 1) {

                // If side is intersects exline
                if (direction(side.p1, p, side.p2) == 0)
                    return onLine(side, p);
                count++;
            }
            i = (i + 1) % n;
        } while (i != 0);

        // When count is odd
        return count & 1;
    }
    
    public static boolean pointInPoly(double[] point, double[][] poly) {
        // 权宜之计，反正我们目前的多边形都是包围原点的
        if (point[0] == 0.0 && point[1] == 0.0) return true;
        
        return checkInside(poly, poly.length, point) == 1;
    }
}
