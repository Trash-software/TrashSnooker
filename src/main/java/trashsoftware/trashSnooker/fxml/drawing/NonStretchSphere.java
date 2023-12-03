package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

public class NonStretchSphere extends MeshView {

    private final int sep;
    private double radius;  // 整个球的半径，无关底座
    private TriangleMesh mesh;
    private double polarLimit = 60.0;

    public NonStretchSphere(int sep, double radius) {
        this.sep = sep;
        this.radius = radius;

        build();
    }

    private void build() {
        mesh = new TriangleMesh();

        double tick = 360.0 / sep;
        float textXTick = 1f / sep;
        int latTicks = sep / 2 + 1;

        float[] points = new float[latTicks * sep * 3];
        float[] textCoords = new float[latTicks * (sep + 1) * 4];
        int[] faces = new int[(latTicks - 1) * sep * 12];

        // 北极点
        for (int lon = 0; lon < sep; lon++) {
//            points[lon * 3] = 0;
            points[lon * 3 + 1] = (float) -radius;
//            points[lon * 3 + 2] = 0;
        }
        for (int lon = 0; lon < sep + 1; lon++) {
            int tcIndex = lon * 4;
            float leftX = lon * textXTick;
            float rightX = (lon + 1) * textXTick;
            textCoords[tcIndex] = leftX;
            textCoords[tcIndex + 2] = rightX;
        }

        for (int latI = 1; latI < latTicks; latI++) {
            double latitude = tick * latI - 90;  // 北极是-90，南极是90
            double cosLat = Math.cos(Math.toRadians(latitude));
            double sinLat = Math.sin(Math.toRadians(latitude));
            float y = (float) (sinLat * radius);
            float cutRadius = (float) (cosLat * radius);

            boolean polarArea = Math.abs(latitude) >= polarLimit;

            float textY;
            if (polarArea) {
//                textY = (float) (latI) / (latTicks - 1);
                textY = (float) (Math.sin(Math.toRadians(latitude + 90))) * 0.5f;
                if (latitude > 0) {
                    textY = 1 - textY;
                }
//                System.out.println(textY);
            } else {
                textY = (float) (sinLat + 1) * 0.5f;
            }

            // text coordinates
            for (int lon = 0; lon < sep + 1; lon++) {
                int gridIndex = latI * sep + lon;
                int tcIndex = gridIndex * 4;

                float leftX;
                float rightX;

                if (polarArea) {
                    double midX = (lon + 0.5) * textXTick;
                    double widthXHalf = cosLat * textXTick * 0.5;

                    leftX = (float) (midX - widthXHalf);
                    rightX = (float) (midX + widthXHalf);
                } else {
                    leftX = lon * textXTick;
                    rightX = (lon + 1) * textXTick;
                }

                textCoords[tcIndex] = leftX;
                textCoords[tcIndex + 1] = textY;
                textCoords[tcIndex + 2] = rightX;
                textCoords[tcIndex + 3] = textY;
            }
//            System.out.println();

            for (int lon = 0; lon < sep; lon++) {
                double longitude = lon * tick;
                float leftX = (float) Math.cos(Math.toRadians(longitude)) * cutRadius;
                float leftZ = (float) Math.sin(Math.toRadians(longitude)) * cutRadius;

                int gridIndex = latI * sep + lon;
                int ptIndex = gridIndex * 3;

                int pt = gridIndex;
                int tc = gridIndex * 2;

                int faceIndex = ((latI - 1) * sep + lon) * 12;

                int upGridIndex = (latI - 1) * sep + lon;
                int upPt = upGridIndex;
                int upTc = upGridIndex * 2;

                int rightLonPtIndex = lon + 1 == sep ? 0 : lon + 1;
                int upRightPt = (latI - 1) * sep + rightLonPtIndex;
                int upRightTc = upTc + 1;
//                int upRightTc = ((latI - 1) * sep + lon + 1) * 2;

                int rightPt = latI * sep + rightLonPtIndex;
                int rightTc = tc + 1;
//                int rightTc = (latI * sep + lon + 1) * 2;

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

        mesh.getPoints().addAll(points);
        mesh.getTexCoords().addAll(textCoords);
        mesh.getFaces().addAll(faces);

        setMesh(mesh);
    }

    public void setRadius(double radius) {
        this.radius = radius;
        
        build();
    }

    public void setPolarLimit(double polarLimit) {
        this.polarLimit = polarLimit;
        
        build();
    }
}
