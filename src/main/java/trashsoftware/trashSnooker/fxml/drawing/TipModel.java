package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import trashsoftware.trashSnooker.core.cue.CueTip;

public class TipModel extends Group {
    
    private final CueTip tip;
    private Color chalkColor;
    
    public TipModel(CueTip tip) {
        this(tip, Color.LIGHTSEAGREEN);
    }
    
    public TipModel(CueTip tip, Color chalkColor) {
        this.tip = tip;
        this.chalkColor = chalkColor;
        
        build();
    }

    public void setChalkColor(Color chalkColor) {
        this.chalkColor = chalkColor;
        
        build();
    }
    
    private void build() {
        getChildren().clear();

        PhongMaterial mat1 = new PhongMaterial();
        mat1.setDiffuseColor(chalkColor);

        Cylinder cylinder = new Cylinder(tip.getRadius(), tip.getThickness());
        cylinder.setMaterial(mat1);

        PhongMaterial mat2 = new PhongMaterial();
        mat2.setDiffuseColor(chalkColor);
        
        Hemisphere hemi = Hemisphere.createByBaseRadius(48, tip.getRadius(), 90);
        hemi.setMaterial(mat2);
        
        cylinder.setTranslateY(-cylinder.getHeight() / 2);
        hemi.setTranslateY(-cylinder.getHeight() - hemi.getHeight());
        
        getChildren().add(cylinder);
        getChildren().add(hemi);
    }
}
