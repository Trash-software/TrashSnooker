package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import java.util.Objects;

public class PoolBallModel extends BallModel {
    
    protected PoolBallModel(int number) {
        super();
        
        String fileName = "/trashsoftware/trashSnooker/img/" 
                + ConfigLoader.getInstance().getBallMaterialResolution() + "/pool/pool" + number + ".png";
        Image img = new Image(Objects.requireNonNull(getClass().getResource(fileName)).toExternalForm());
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(img);
//        material.setSpecularMap(img);
        
        sphere.setMaterial(material);
    }
}
