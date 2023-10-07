package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.Arrays;
import java.util.Objects;

public class TruncateCone extends MeshView {

    private final int poly;
    private final float r1;
    private final float r2;
    private final float h;

    public TruncateCone(int poly, 
                        float topRadius, 
                        float botRadius, 
                        float length, 
                        PhongMaterial texture, 
                        boolean skewedTexture) {
        this.poly = poly;
        this.r1 = topRadius;
        this.r2 = botRadius;
        this.h = length;

        float[] points = new float[poly * 6];
        int[] faces = new int[poly * 12];
        float[] textCoords = new float[poly * 4];

        double sep = 1.0 / poly;
        double baseX;
        double topSepX;
        if (skewedTexture) {
            double ratio = topRadius / botRadius;
            baseX = 0.5 - ratio / 2;
            topSepX = sep * ratio;
        } else {
            baseX = 0.0;
            topSepX = sep;
        }
        // texture coords

        for (int i = 0; i < poly; i++) {
            // 棱台的每个侧边由两个三角形拼成
            // 实际上只需一条边，但我们写了两条边，为了简单
            int index = i * 4;
            
            float ulx = (float) (i * topSepX + baseX);
            float urx = (float) (ulx + topSepX);
            float llx = (float) (i * sep);
            float lrx = (float) (llx + sep);
            
            // 0 左上
            textCoords[index] = ulx;
            textCoords[index + 1] = 0;
            // 1 左下
            textCoords[index + 2] = llx;
            textCoords[index + 3] = 1;
//            // 2 右下
//            textCoords[index + 4] = lrx;
//            textCoords[index + 5] = 1;
//            // 3 右上
//            textCoords[index + 6] = urx;
//            textCoords[index + 7] = 0;
        }
        
        int nPoints = points.length / 3;
        int nCoords = textCoords.length / 2;

        double eachDeg = 360.0 / poly;
        for (int i = 0; i < poly; i++) {
            double deg = i * eachDeg;
            double deg2 = (i + 1) * eachDeg;
            int index = i * 6;

            float leftX = (float) Math.cos(Math.toRadians(deg));
            float leftZ = (float) Math.sin(Math.toRadians(deg));
//            float rightX = (float) Math.cos(Math.toRadians(deg2));
//            float rightZ = (float) Math.sin(Math.toRadians(deg2));

            // 0 左上
            points[index] = leftX * r1;
            points[index + 1] = 0;
            points[index + 2] = leftZ * r1;
            // 1 左下
            points[index + 3] = leftX * r2;
            points[index + 4] = h;
            points[index + 5] = leftZ * r2;
//            // 2 右下
//            points[index + 6] = rightX * r2;
//            points[index + 7] = h;
//            points[index + 8] = rightZ * r2;
//            // 3 右上
//            points[index + 9] = rightX * r1;
//            points[index + 10] = 0;
//            points[index + 11] = rightZ * r1;
            
            // 每一个面的排列
            // v0, t0, v1, t1, v2, t2
            
            int faceIndex = i * 12;
            int ptIndex = i * 2;
            int tcIndex = i * 2;
            
            // （左上，左下，右下）三角形
            faces[faceIndex] = ptIndex;
            faces[faceIndex + 1] = tcIndex;
            faces[faceIndex + 2] = ptIndex + 1;
            faces[faceIndex + 3] = tcIndex + 1;
            faces[faceIndex + 4] = ptIndex + 3 >= nPoints ? 1 : ptIndex + 3;
            faces[faceIndex + 5] = tcIndex + 3 >= nCoords ? 1 : tcIndex + 3;

            // （左上，右下，右上）三角形
            faces[faceIndex + 6] = ptIndex;
            faces[faceIndex + 7] = tcIndex;
            faces[faceIndex + 8] = ptIndex + 3 >= nPoints ? 1 : ptIndex + 3;
            faces[faceIndex + 9] = tcIndex + 3 >= nCoords ? 1 : tcIndex + 3;
            faces[faceIndex + 10] = ptIndex + 2 >= nPoints ? 0 : ptIndex + 2;
            faces[faceIndex + 11] = tcIndex + 2 >= nCoords ? 0 : tcIndex + 2;
        }
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(points);
        mesh.getFaces().addAll(faces);
        mesh.getTexCoords().addAll(textCoords);

        setMesh(mesh);
        setMaterial(texture);
    }

    public float getR1() {
        return r1;
    }

    public float getR2() {
        return r2;
    }

    public float getH() {
        return h;
    }
}
