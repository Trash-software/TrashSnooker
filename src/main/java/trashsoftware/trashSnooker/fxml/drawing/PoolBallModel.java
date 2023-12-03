package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import java.util.Objects;

public class PoolBallModel extends BallModel {
    
    protected PoolBallModel(int number) {
        super();
        
        String fileName = "/trashsoftware/trashSnooker/res/img/" 
                + ConfigLoader.getInstance().getBallMaterialResolution() + "/pool/pool" + number + ".png";
        Image img = new Image(Objects.requireNonNull(getClass().getResource(fileName)).toExternalForm());
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(img);
//        material.setSpecularMap(img);
        
        sphere.setMaterial(material);
        
        if (number == 0) {
            sphere.setPolarLimit(45);
        } else {
            sphere.setPolarLimit(75);
        }
    }

    @Override
    public boolean textured() {
        return true;
    }
}
