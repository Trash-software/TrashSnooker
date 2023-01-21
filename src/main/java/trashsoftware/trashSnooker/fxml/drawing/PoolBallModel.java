package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import trashsoftware.trashSnooker.core.GameValues;

import java.util.Objects;

public class PoolBallModel extends BallModel {
    protected PoolBallModel(int number) {
        super(GameValues.CHINESE_EIGHT_VALUES.ballRadius);
        
        String fileName = "/trashsoftware/trashSnooker/img/pool/pool" + number + ".png";
        Image img = new Image(Objects.requireNonNull(getClass().getResource(fileName)).toExternalForm());
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(img);
        material.setSpecularMap(img);
        
        sphere.setMaterial(material);
    }
}
