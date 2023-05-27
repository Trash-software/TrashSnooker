package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.paint.PhongMaterial;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.metrics.BallMetrics;

public class SnookerBallModel extends BallModel {
    
    protected SnookerBallModel(int value) {
        super();

        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Ball.snookerColor(value));
        sphere.setMaterial(material);
    }
}
