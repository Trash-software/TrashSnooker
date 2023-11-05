package trashsoftware.trashSnooker.fxml.drawing;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import trashsoftware.trashSnooker.fxml.GameView;

public class RestTipModel extends TipModel {
    
    private final String identifier;
    private static final double RADIUS = 6.0;
    
    public RestTipModel(String identifier) {
        this.identifier = identifier;
        build();
    }
    
    private void buildCrossRest() {
        System.out.println("Building cross rest");
        getChildren().clear();
        Group h1 = buildCapsule();
        Group h2 = buildCapsule();
        
        h1.getTransforms().addAll(new Rotate(90, new Point3D(1, 0, 0)), 
                new Rotate(45), new Rotate(0, 1, 0));
        h2.getTransforms().addAll(new Rotate(90, new Point3D(1, 0, 0)),
                new Rotate(-45), new Rotate(0, 1, 0));
        
        getChildren().addAll(h1, h2);
        
        setTranslateY(-RADIUS * scale);
    }
    
    private Group buildCapsule() {
        Cylinder cylinder = new Cylinder(RADIUS * scale, 45 * scale);
        Hemisphere s1 = new Hemisphere(CueModel3D.CIRCLE_DIVISION, RADIUS * scale, 180);
        Hemisphere s2 = new Hemisphere(CueModel3D.CIRCLE_DIVISION, RADIUS * scale, 180);
        
        s1.setTranslateY(-cylinder.getHeight() / 2);
        s2.setTranslateY(cylinder.getHeight() / 2);
        s2.getTransforms().add(new Rotate(180, new Point3D(1, 0, 0)));

        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(GameView.REST_METAL_COLOR);
        cylinder.setMaterial(material);
        s1.setMaterial(material);
        s2.setMaterial(material);
        
        Group group = new Group();
        group.getChildren().addAll(s1, cylinder, s2);
        return group;
    }

    @Override
    protected void build() {
        if ("crossRest".equals(identifier)) {
            buildCrossRest();
        }
    }
}
