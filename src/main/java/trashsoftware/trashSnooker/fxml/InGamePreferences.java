package trashsoftware.trashSnooker.fxml;

import trashsoftware.trashSnooker.enums.CueBallSelectionMouseMode;
import trashsoftware.trashSnooker.enums.TrajectoryHide;
import trashsoftware.trashSnooker.enums.TrajectoryMode;
import trashsoftware.trashSnooker.fxml.drawing.PredictionQuality;
import trashsoftware.trashSnooker.fxml.settings.SettingsView;
import trashsoftware.trashSnooker.util.config.ConfigLoader;
import trashsoftware.trashSnooker.util.config.InputManager;

public class InGamePreferences {
    TrajectoryMode trajectoryMode;
    TrajectoryHide trajectoryHide;
    CueBallSelectionMouseMode cueBallSelectionMouseMode;
    PredictionQuality predictionQuality;
    private SettingsView.MouseDragMethod mouseDragMethod;
    
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
        cueBallSelectionMouseMode = CueBallSelectionMouseMode.fromKey(
                configLoader.getString("cueBallSelectionMouseMode", "doubleClick")
        );
        
        mouseDragMethod = SettingsView.MouseDragMethod.fromKey(configLoader.getString("mouseDragMethod"));
    }

    public SettingsView.MouseDragMethod getMouseDragMethod() {
        return mouseDragMethod;
    }

    public InputManager getInputManager() {
        return ConfigLoader.getInstance().getInputManager();
    }
}