package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import trashsoftware.trashSnooker.core.metrics.BallMetrics;

import java.util.Objects;

public class PoolBallModel extends BallModel {
    protected PoolBallModel(int number) {
        super(BallMetrics.POOL_BALL.ballRadius);
        
        String fileName = "/trashsoftware/trashSnooker/img/pool/pool" + number + ".png";
        Image img = new Image(Objects.requireNonNull(getClass().getResource(fileName)).toExternalForm());
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(img);
        material.setSpecularMap(img);
        
        sphere.setMaterial(material);
    }
}
