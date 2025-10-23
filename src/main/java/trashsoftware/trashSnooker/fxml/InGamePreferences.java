package trashsoftware.trashSnooker.fxml;

import trashsoftware.trashSnooker.enums.TrajectoryHide;
import trashsoftware.trashSnooker.enums.TrajectoryMode;
import trashsoftware.trashSnooker.fxml.drawing.PredictionQuality;
import trashsoftware.trashSnooker.util.config.ConfigLoader;
import trashsoftware.trashSnooker.util.config.InputManager;

public class InGamePreferences {
    TrajectoryMode trajectoryMode;
    TrajectoryHide trajectoryHide;
    PredictionQuality predictionQuality;
    boolean absoluteDragAngle;
    
    InGamePreferences() {
        ConfigLoader configLoader = ConfigLoader.getInstance();
        predictionQuality = PredictionQuality.fromKey(
                configLoader.getString("performance", "veryHigh"));
        trajectoryMode = TrajectoryMode.fromKey(
                configLoader.getString("trajectoryMode", "slipRollGradient")
        );
        trajectoryHide = TrajectoryHide.fromKey(
                configLoader.getString("trajectoryHide", "nextCue")
        );

        String mouseDragMethod = configLoader.getString("mouseDragMethod");
        absoluteDragAngle = "position".equals(mouseDragMethod);
    }
    
    public InputManager getInputManager() {
        return ConfigLoader.getInstance().getInputManager();
    }
}