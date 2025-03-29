package trashsoftware.trashSnooker.fxml.drawing;

import javafx.geometry.Point3D;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Rotate;
import org.jcodec.codecs.mpeg4.es.ES;

public class EquirectangularSphere extends CustomSphere {
    
    private final int sep;
    private double radius;

    public EquirectangularSphere(int sep, double radius) {
        super();
        
        this.radius = radius;
        this.sep = sep;
    }
    
    private void build() {
        // Use triangular mesh
        int latLevels = sep;
        int lonLevels = sep * 2;

        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);

        double latIncAngle = (Math.PI/latLevels);
        double lonIncAngle = (Math.PI * 2)/lonLevels;
        double textLatIncr = 1.0/latLevels;
        double textLonIncr = 1.0/lonLevels;

        int currentPointOffset = 0;
        int currentNormalOffset = 0;
        int currentTextOffset = 0;
        for(int i = 0; i < latLevels; ++i) {
            for(int j = 0; j < lonLevels; ++j) {
                // The point list is: top left - bottom left - bottom right - top right
                // The faces-normal points are: (0,0) (1,1) (2,2) (0,3) (2,4) (3,5)
                Point3D tp1 = new Point3D(0,radius * Math.cos(Math.PI - (i * latIncAngle)), radius * Math.sin(Math.PI - (i * latIncAngle)));
                Point3D tp2 = new Point3D(0,radius * Math.cos(Math.PI - (i * latIncAngle + latIncAngle)), radius * Math.sin(Math.PI - (i * latIncAngle + latIncAngle)));
                Point3D topLeft = new Rotate(Math.toDegrees(j * lonIncAngle), new Point3D(0, 1, 0)).transform(tp1);
                Point3D bottomLeft =  new Rotate(Math.toDegrees(j * lonIncAngle), new Point3D(0, 1, 0)).transform(tp2);
                Point3D bottomRight = new Rotate(Math.toDegrees(j * lonIncAngle + lonIncAngle), new Point3D(0, 1, 0)).transform(tp2);
                Point3D topRight = new Rotate(Math.toDegrees(j * lonIncAngle + lonIncAngle), new Point3D(0, 1, 0)).transform(tp1);

                // Compute normals
                Point3D topLeftNormal_1 = computeNormal(topLeft, bottomLeft, bottomRight); // 0
                Point3D bottomLeftNormal_1 = computeNormal(bottomLeft, bottomRight, topLeft); // 1
                Point3D bottomRightNormal_1 = computeNormal(bottomRight, topLeft, bottomLeft); // 2
                Point3D topLeftNormal_2 = computeNormal(topLeft, bottomRight, topRight); // 3
                Point3D bottomRightNormal_2 = computeNormal(bottomRight, topRight, topLeft); // 4
                Point3D topRightNormal_2 = computeNormal(topRight, topLeft, bottomRight); // 5

                // Add points
                mesh.getPoints().addAll((float) topLeft.getX(), (float) topLeft.getY(), (float) topLeft.getZ()); // 0
                mesh.getPoints().addAll((float) bottomLeft.getX(), (float) bottomLeft.getY(), (float) bottomLeft.getZ()); // 1
                mesh.getPoints().addAll((float) bottomRight.getX(), (float) bottomRight.getY(), (float) bottomRight.getZ()); // 2
                mesh.getPoints().addAll((float) topRight.getX(), (float) topRight.getY(), (float) topRight.getZ()); // 3

                // Add normals
                mesh.getNormals().addAll((float) topLeftNormal_1.getX(), (float) topLeftNormal_1.getY(), (float) topLeftNormal_1.getZ()); // 0
                mesh.getNormals().addAll((float) bottomLeftNormal_1.getX(), (float) bottomLeftNormal_1.getY(), (float) bottomLeftNormal_1.getZ()); // 1
                mesh.getNormals().addAll((float) bottomRightNormal_1.getX(), (float) bottomRightNormal_1.getY(), (float) bottomRightNormal_1.getZ()); // 2
                mesh.getNormals().addAll((float) topLeftNormal_2.getX(), (float) topLeftNormal_2.getY(), (float) topLeftNormal_2.getZ()); // 3
                mesh.getNormals().addAll((float) bottomRightNormal_2.getX(), (float) bottomRightNormal_2.getY(), (float) bottomRightNormal_2.getZ()); // 4
                mesh.getNormals().addAll((float) topRightNormal_2.getX(), (float) topRightNormal_2.getY(), (float) topRightNormal_2.getZ()); // 5

                // Add texture
                float[] p0t = { (float) (i * textLatIncr), 1.0f - (float) (j * textLonIncr) };
                float[] p1t = { (float) (i * textLatIncr + textLatIncr), 1.0f - (float) (j * textLonIncr) };
                float[] p2t = { (float) (i * textLatIncr + textLatIncr), 1.0f - (float) (j * textLonIncr + textLonIncr) };
                float[] p3t = { (float) (i * textLatIncr), 1.0f - (float) (j * textLonIncr + textLonIncr) };

                mesh.getTexCoords().addAll(
                        p0t[1], p0t[0],
                        p1t[1], p1t[0],
                        p2t[1], p2t[0],
                        p3t[1], p3t[0]
                );

                // Add faces
                mesh.getFaces().addAll(
                        currentPointOffset + 0, currentNormalOffset + 0, currentTextOffset + 0,
                        currentPointOffset + 2, currentNormalOffset + 2, currentTextOffset + 2,
                        currentPointOffset + 1, currentNormalOffset + 1, currentTextOffset + 1,
                        currentPointOffset + 0, currentNormalOffset + 3, currentTextOffset + 0,
                        currentPointOffset + 3, currentNormalOffset + 5, currentTextOffset + 3,
                        currentPointOffset + 2, currentNormalOffset + 4, currentTextOffset + 2

                );

                currentPointOffset += 4;
                currentNormalOffset += 6;
                currentTextOffset += 4;
            }
        }

        setMesh(mesh);
        setCullFace(CullFace.BACK);
//        PhongMaterial material = new PhongMaterial();
//        material.setDiffuseMap(new Image(Main.class.getResourceAsStream("/images/earth.jpg")));
//        meshView.setMaterial(material);
//        return new Group(meshView);
    }

    private static Point3D computeNormal(Point3D p1, Point3D p2, Point3D p3) {
        return (p3.subtract(p1).normalize()).crossProduct(p2.subtract(p1).normalize()).normalize();
    }
    
    @Override
    public void setRadius(double radius) {
        this.radius = radius;
        build();
    }

    @Override
    public EquirectangularSphere copyNoTrans() {
        EquirectangularSphere copy = new EquirectangularSphere(sep, radius);
        copy.setMesh(this.getMesh());
        copy.setMaterial(getMaterial());
        copy.setTranslateX(0);
        copy.setTranslateY(0);
        copy.setTranslateZ(0);
        return copy;
    }
}
