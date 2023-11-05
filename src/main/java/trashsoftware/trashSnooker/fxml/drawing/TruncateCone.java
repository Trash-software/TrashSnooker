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
                        PhongMaterial texture) {
        this.poly = poly;
        this.r1 = topRadius;
        this.r2 = botRadius;
        this.h = length;

        float[] points = new float[poly * 6];
        int[] faces = new int[poly * 12];
        float[] textCoords = new float[(poly + 1) * 4];

        double sep = 1.0 / poly;
        
        // texture coords
        for (int i = 0; i < poly + 1; i++) {
            // +1是因为
            // 圆柱的最后一个侧面可以接到第一个面，但贴图不能从最右边接到最左边
            
            // 棱台的每个侧边由两个三角形拼成
            // 实际上只需一条边，但我们写了两条边，为了简单
            int index = i * 4;
            
            float x = (float) (i * sep);
            
            // 0 左上
            textCoords[index] = x;
            textCoords[index + 1] = 0;
            // 1 左下
            textCoords[index + 2] = x;
            textCoords[index + 3] = 1;
        }
        
        int nPoints = points.length / 3;

        double eachDeg = 360.0 / poly;
        for (int i = 0; i < poly; i++) {
            double deg = i * eachDeg;
//            double deg2 = (i + 1) * eachDeg;
            int index = i * 6;

            float leftX = (float) Math.cos(Math.toRadians(deg));
            float leftZ = (float) Math.sin(Math.toRadians(deg));

            // 0 左上
            points[index] = leftX * r1;
            points[index + 1] = 0;
            points[index + 2] = leftZ * r1;
            // 1 左下
            points[index + 3] = leftX * r2;
            points[index + 4] = h;
            points[index + 5] = leftZ * r2;
            
            // 每一个面的排列
            // v0, t0, v1, t1, v2, t2
            
            int faceIndex = i * 12;
            int ptIndex = i * 2;
            
            int ul = ptIndex;
            int ll = ptIndex + 1;
            int urPt = ptIndex + 2 >= nPoints ? 0 : ptIndex + 2;
            int lrPt = ptIndex + 3 >= nPoints ? 1 : ptIndex + 3;
            int urTc = ptIndex + 2;
            int lrTc = ptIndex + 3;
            
            // （左上，左下，右下）三角形
            faces[faceIndex] = ul;
            faces[faceIndex + 1] = ul;
            faces[faceIndex + 2] = ll;
            faces[faceIndex + 3] = ll;
            faces[faceIndex + 4] = lrPt;
            faces[faceIndex + 5] = lrTc;

            // （左上，右下，右上）三角形
            faces[faceIndex + 6] = ul;
            faces[faceIndex + 7] = ul;
            faces[faceIndex + 8] = lrPt;
            faces[faceIndex + 9] = lrTc;
            faces[faceIndex + 10] = urPt;
            faces[faceIndex + 11] = urTc;
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
