package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.QuadCurve;
import javafx.scene.shape.QuadCurveTo;

import java.util.List;

public class CurvedPolygonDrawer {

    private final double curve;

    /**
     * @param curve 弯的程度，在[0.0, 1.0]区间
     */
    public CurvedPolygonDrawer(double curve) {
        this.curve = curve / 2;
        
        if (curve > 1.0) {
            System.err.println("Curve too big!");
        }
    }
    
    private void oneSegment(double[] p1, double[] p2, double[] p3, GraphicsContext gc, boolean isBegin) {
        double[] p1p2b = interpolate(p1, p2, 1 - curve);
        double[] p2p3a = interpolate(p2, p3, curve);
        
        if (isBegin) {
            double[] p1p2a = interpolate(p1, p2, curve);
            gc.moveTo(p1p2a[0], p1p2a[1]);
        }
        gc.lineTo(p1p2b[0], p1p2b[1]);
        gc.quadraticCurveTo(p2[0], p2[1], p2p3a[0], p2p3a[1]);
    }
    
    public void draw(List<double[]> points, GraphicsContext gc) {
        gc.beginPath();
        
        for (int i = 0; i < points.size() - 2; i++) {
            oneSegment(points.get(i), points.get(i + 1), points.get(i + 2), gc, i == 0);
        }
        oneSegment(points.get(points.size() - 2), points.get(points.size() - 1), points.get(0), gc, false);
        oneSegment(points.get(points.size() - 1), points.get(0), points.get(1), gc, false);
        gc.closePath();
        gc.stroke();
    }
    
    private double[] interpolate(double[] p1, double[] p2, double t) {
        return new double[]{(int) Math.round(p1[0] * (1 - t) + p2[0] * t),
                (int) Math.round(p1[1] * (1 - t) + p2[1] * t)};
    }
}
