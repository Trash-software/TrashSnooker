package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.paint.PhongMaterial;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.GameValues;

public class SnookerBallModel extends BallModel {
    
    protected SnookerBallModel(int value) {
        super(GameValues.SNOOKER_VALUES.ballRadius);

        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Ball.snookerColor(value));
        sphere.setMaterial(material);
    }
}
