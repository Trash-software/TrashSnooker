package trashsoftware.trashSnooker.core.cue;

import javafx.scene.SnapshotParameters;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import trashsoftware.trashSnooker.util.DataLoader;

import java.util.List;
import java.util.Objects;

public class TexturedCueBrand extends CueBrand {

    private final List<Segment> segments;  // 从杆头到杆尾

    public TexturedCueBrand(String cueId,
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
    public double getWoodPartLength() {
        return segments.stream().map(s -> s.length).reduce(0.0, Double::sum);
    }

    public static final class Segment {
        private final String texture;
        private final double length;
        private final double diameter1;
        private final double diameter2;

        private PhongMaterial material;

        public Segment(String texture, double length, double diameter1, double diameter2) {
            this.texture = texture;
            this.length = length;
            this.diameter1 = diameter1;
            this.diameter2 = diameter2;
        }

        private Image skewTexture(Image srcImage) {
            double ratio = diameter1 / diameter2;
            double origW = srcImage.getWidth();
            double origH = srcImage.getHeight();
            
            double ulx = (0.5 - ratio / 2) * origW;
            double urx = (0.5 + ratio / 2) * origW;
            
            PerspectiveTransform transform = new PerspectiveTransform(
                    ulx, 0,
                    urx, 0,
                    origW, origH,
                    0, origH
            );
            ImageView imageView = new ImageView(srcImage);
            imageView.setEffect(transform);
            
            SnapshotParameters parameters = new SnapshotParameters();
            parameters.setFill(Color.TRANSPARENT);
            Image out = imageView.snapshot(parameters, null);
            
            return out;
        }

        public PhongMaterial getMaterial() {
            if (material == null) {
                material = createMaterial();
            }
            return material;
        }

        private PhongMaterial createMaterial() {
            PhongMaterial mat = new PhongMaterial();

            if (texture.indexOf('.') != -1) {
                Image orig = new Image(
                        Objects.requireNonNull(
                                        getClass()
                                                .getResource("/trashsoftware/trashSnooker/res/img/256/cue/" +
                                                        texture))
                                .toExternalForm());
                mat.setDiffuseMap(orig);
            } else {
                mat.setDiffuseColor(DataLoader.parseColor(texture));
            }
            return mat;
        }

        public double length() {
            return length;
        }

        public double diameter1() {
            return diameter1;
        }

        public double diameter2() {
            return diameter2;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Segment) obj;
            return Objects.equals(this.texture, that.texture) &&
                    Double.doubleToLongBits(this.length) == Double.doubleToLongBits(that.length) &&
                    Double.doubleToLongBits(this.diameter1) == Double.doubleToLongBits(that.diameter1) &&
                    Double.doubleToLongBits(this.diameter2) == Double.doubleToLongBits(that.diameter2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(texture, length, diameter1, diameter2);
        }

        @Override
        public String toString() {
            return "Segment[" +
                    "texture=" + texture + ", " +
                    "length=" + length + ", " +
                    "diameter1=" + diameter1 + ", " +
                    "diameter2=" + diameter2 + ']';
        }

    }
}
