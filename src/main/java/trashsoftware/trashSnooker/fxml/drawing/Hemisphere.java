package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.Arrays;

public class Hemisphere extends MeshView {
    
    private final int sep;
    private double sphereRadius;  // 整个球的半径，无关底座
    private double arcExtent;  // 如果是180度则为标准半球
    private TriangleMesh mesh;
    
    public Hemisphere(int sep, double sphereRadius, double arcExtent) {
        this.sep = sep;
        this.sphereRadius = sphereRadius;
        this.arcExtent = arcExtent;
        
        build();
    }
    
    public static Hemisphere createByBaseRadius(int sep, double baseRadius, double arcExtent) {
        double angle = arcExtent / 2;
        double sphereRadius = 1 / Math.sin(Math.toRadians(angle)) * baseRadius;
        return new Hemisphere(sep, sphereRadius, arcExtent);
    }
    
    private void build() {
        mesh = new TriangleMesh();
        
        if (arcExtent > 180) {
            System.err.println("Does not support hemisphere greater than 180 degrees");
            arcExtent = 180.0;
        }
        
        double tick = 360.0 / sep;
        double nLatitudes = arcExtent / 2;
        int nLatTicks = (int) (nLatitudes / tick);
        
        float textXTick = 1f / sep;

//        boolean hasRem = nLatTicks != nLatitudes / tick;
//        int yPts = hasRem ? nLatTicks + 1 : nLatTicks;
        int yPts = nLatTicks + 1;
        float[] points = new float[yPts * sep * 3];
        float[] textCoords = new float[yPts * (sep + 1) * 2];
        int[] faces = new int[(yPts - 1) * sep * 12];
        
        double height = getHeight();
        
        // 第一个点就是北极点
        
        for (int latI = 1; latI < yPts; latI++) {
            double latitude;
            if (latI == nLatTicks) {
                latitude = 90 - nLatitudes;
            } else {
                latitude = 90 - tick * latI;
            }
            
            float y = (float) ((1 - Math.sin(Math.toRadians(latitude))) * sphereRadius - height);
            float cutRadius = (float) (Math.cos(Math.toRadians(latitude)) * sphereRadius);
            float textY = (float) latI / yPts;  // 不严谨，最后一格会被拉伸，但影响不大
            
            // text coordinates
            for (int lon = 0; lon < sep + 1; lon++) {
                int gridIndex = latI * sep + lon;
                int tcIndex = gridIndex * 2;
                
                float textX = lon * textXTick;
                textCoords[tcIndex] = textX;
                textCoords[tcIndex + 1] = textY;
            }
            
            for (int lon = 0; lon < sep; lon++) {
                double longitude = lon * tick;
                float leftX = (float) Math.cos(Math.toRadians(longitude)) * cutRadius;
                float leftZ = (float) Math.sin(Math.toRadians(longitude)) * cutRadius;
                
                int gridIndex = latI * sep + lon;
                int ptIndex = gridIndex * 3;
//                int tcIndex = gridIndex * 2;
                
                int pt = gridIndex;
                int tc = gridIndex;
                
                int faceIndex = ((latI - 1) * sep + lon) * 12;
                
                int upGridIndex = (latI - 1) * sep + lon;
                int upPt = upGridIndex;
                int upTc = upGridIndex;

                int rightLonPtIndex = lon + 1 == sep ? 0 : lon + 1;
                int upRightPt = (latI - 1) * sep + rightLonPtIndex;
                int upRightTc = (latI - 1) * sep + lon + 1;

                int rightPt = latI * sep + rightLonPtIndex;
                int rightTc = latI * sep + lon + 1;
                
                points[ptIndex] = leftX;
                points[ptIndex + 1] = y;
                points[ptIndex + 2] = leftZ;

                // （左上，左下，右下）三角形
                faces[faceIndex] = upPt;
                faces[faceIndex + 1] = upTc;
                faces[faceIndex + 2] = pt;
                faces[faceIndex + 3] = tc;
                faces[faceIndex + 4] = rightPt;
                faces[faceIndex + 5] = rightTc;

                // （左上，右下，右上）三角形
                faces[faceIndex + 6] = upPt;
                faces[faceIndex + 7] = upTc;
                faces[faceIndex + 8] = rightPt;
                faces[faceIndex + 9] = rightTc;
                faces[faceIndex + 10] = upRightPt;
                faces[faceIndex + 11] = upRightTc;
            }
        }

//        for (int i = 0; i < 3; i++) {
//            for (int j = i; j < points.length; j += 3) {
//                System.out.print(points[j] + " ");
//            }
//            System.out.println();
//        }
//        System.out.println(textCoords.length);
//        System.out.println(Arrays.toString(faces));
        
        mesh.getPoints().addAll(points);
        mesh.getTexCoords().addAll(textCoords);
        mesh.getFaces().addAll(faces);
        
        setMesh(mesh);
    }
    
    public void setSphereRadius(double sphereRadius) {
        this.sphereRadius = sphereRadius;
    }
    
    public double getHeight() {
        double deg = arcExtent / 2;
        double extra = Math.cos(Math.toRadians(deg));
        return sphereRadius * (1 - extra);
    }
}
