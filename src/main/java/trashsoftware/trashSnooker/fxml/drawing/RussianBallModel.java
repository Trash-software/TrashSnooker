package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.paint.PhongMaterial;
import trashsoftware.trashSnooker.core.metrics.BallsGroupPreset;
import trashsoftware.trashSnooker.core.russian.RussianBall;
import trashsoftware.trashSnooker.core.snooker.SnookerBall;

public class RussianBallModel extends BallModel {
    protected RussianBallModel(BallsGroupPreset preset, int value) {
        super(preset, preset != null && preset.isEquirectangular(value));

        PhongMaterial material;
        if (preset == null) material = loadDefaultMaterial(value);
        else material = loadPresetMaterial(preset, value);

        sphere.setMaterial(material);
    }

    @Override
    protected PhongMaterial loadDefaultMaterial(int value) {
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(RussianBall.russianColor(value));
        return material;
    }
}
