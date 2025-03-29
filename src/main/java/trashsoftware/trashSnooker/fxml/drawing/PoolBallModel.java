package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import trashsoftware.trashSnooker.core.metrics.BallsGroupPreset;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import java.util.Objects;

public class PoolBallModel extends BallModel {
    
    protected PoolBallModel(BallsGroupPreset preset, int number) {
        super(preset, preset != null && preset.isEquirectangular(number));
        
        PhongMaterial material;
        if (preset == null) material = loadDefaultMaterial(number);
        else material = loadPresetMaterial(preset, number);
        
        sphere.setMaterial(material);
        
        if (sphere instanceof NonStretchSphere nss) {
            if (number == 0) {
                nss.setPolarLimit(45);
            } else {
                nss.setPolarLimit(75);
            }
        }
    }

    @Override
    protected PhongMaterial loadDefaultMaterial(int value) {
        String fileName = "/trashsoftware/trashSnooker/res/img/"
                + ConfigLoader.getInstance().getBallMaterialResolution() + "/ball/stdPool/pool" + value + ".png";
        Image img = new Image(Objects.requireNonNull(getClass().getResource(fileName)).toExternalForm());
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(img);
        return material;
    }
}
