package trashsoftware.trashSnooker.core.cue;

import javafx.scene.SnapshotParameters;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TexturedCueBrand extends CueBrand implements Cloneable {

    private List<Segment> segments;  // 从杆头到杆尾

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
                            boolean privacy,
                            boolean availability,
                            int price) {
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
                privacy,
                availability,
                price);

        this.segments = segments;
    }

    public List<Segment> getSegments() {
        return segments;
    }

    @Override
    public double getWoodPartLength() {
        return segments.stream().map(s -> s.length).reduce(0.0, Double::sum);
    }

    public void removeTextures() {
        for (Segment segment : segments) {
            segment.setTexture("WHITESMOKE");
        }
    }

    @Override
    public TexturedCueBrand clone() {
        try {
            TexturedCueBrand clone = (TexturedCueBrand) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            clone.segments = new ArrayList<>();
            for (Segment segment : segments) {
                clone.segments.add(new Segment(
                        segment.texture, 
                        segment.length, 
                        segment.diameter1, 
                        segment.diameter2));
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public static final class Segment {
        private String texture;
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

        public void setTexture(String texture) {
            this.texture = texture;
            
            createMaterial();
        }

        private PhongMaterial createMaterial() {
            PhongMaterial mat = new PhongMaterial();

            if (texture.indexOf('.') != -1) {
                URL url = getClass()
                        .getResource("/trashsoftware/trashSnooker/res/img/256/cue/" +
                                texture);
                if (url != null) {
                    Image orig = new Image(url.toExternalForm());
                    mat.setDiffuseMap(orig);
                } else {
                    // 外部
                    File file = new File(texture);
                    if (file.exists()) {
                        Image orig = new Image(file.getAbsolutePath());
                        mat.setDiffuseMap(orig);
                    } else {
                        mat.setDiffuseColor(Color.WHITESMOKE);
                    }
                }
            } else {
                Color color;
                try {
                    color = DataLoader.parseColor(texture);
                } catch (IllegalArgumentException iae) {
                    color = Color.WHITESMOKE;
                }
                mat.setDiffuseColor(color);
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
