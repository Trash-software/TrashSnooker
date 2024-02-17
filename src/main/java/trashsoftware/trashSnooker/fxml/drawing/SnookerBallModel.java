package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.paint.PhongMaterial;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.metrics.BallMetrics;
import trashsoftware.trashSnooker.core.snooker.SnookerBall;

public class SnookerBallModel extends BallModel {
    
    protected SnookerBallModel(int value) {
        super();

        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(SnookerBall.snookerColor(value));
        sphere.setMaterial(material);
    }

    @Override
    public boolean textured() {
        // 注意：红点母球可以为true
        return false;
    }
}
