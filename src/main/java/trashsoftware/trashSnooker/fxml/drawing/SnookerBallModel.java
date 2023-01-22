package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.paint.PhongMaterial;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.BallMetrics;
import trashsoftware.trashSnooker.core.TableMetrics;

public class SnookerBallModel extends BallModel {
    
    protected SnookerBallModel(int value) {
        super(BallMetrics.SNOOKER_BALL.ballRadius);

        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Ball.snookerColor(value));
        sphere.setMaterial(material);
    }
}
