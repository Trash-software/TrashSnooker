package trashsoftware.trashSnooker.core.cue;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import trashsoftware.trashSnooker.fxml.drawing.TruncateCone;
import trashsoftware.trashSnooker.util.DataLoader;

import java.util.List;
import java.util.Objects;

public class TexturedCue extends Cue {

    private final List<Segment> segments;  // 从杆头到杆尾

    public TexturedCue(String cueId,
                       String name,
                       List<Segment> segments,
                       double tipRingThickness,
                       double cueTipThickness,
                       Color tipRingColor,
                       Color backColor,
                       double powerMultiplier,
                       double spinMultiplier,
                       double accuracyMultiplier,
                       boolean privacy) {
        super(cueId,
                name,
                tipRingThickness,
                cueTipThickness,
                segments.get(segments.size() - 1).diameter2,
                segments.get(0).diameter1,
                tipRingColor,
                backColor,
                powerMultiplier,
                spinMultiplier,
                accuracyMultiplier,
                privacy);

        this.segments = segments;
    }

    public List<Segment> getSegments() {
        return segments;
    }

    @Override
    public double getTotalLength() {
        return getWoodPartLength() + tipRingThickness;
    }

    @Override
    public double getWoodPartLength() {
        return segments.stream().map(s -> s.length).reduce(0.0, Double::sum);
    }

    public record Segment(String texture, double length, double diameter1, double diameter2) {
        
        public PhongMaterial createMaterial() {
            PhongMaterial mat = new PhongMaterial();
            
            if (texture.indexOf('.') != -1) {
                mat.setDiffuseMap(new Image(
                        Objects.requireNonNull(
                                        getClass()
                                                .getResource("/trashsoftware/trashSnooker/res/img/256/cue/" +
                                                        texture))
                                .toExternalForm()));
            } else {
                mat.setDiffuseColor(DataLoader.parseColor(texture));
            }
            return mat;
        }
    }
}
