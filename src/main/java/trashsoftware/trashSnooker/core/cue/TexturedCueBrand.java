package trashsoftware.trashSnooker.core.cue;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TexturedCueBrand extends CueBrand implements Cloneable {
    
    public static final boolean SEP_TEXTURE = false;

    private List<Segment> segments;  // 从杆头到杆尾

    public TexturedCueBrand(String cueId,
                            String name,
                            List<Segment> segments,
                            double tipRingThickness,
                            Color tipRingColor,
                            Color backColor,
                            double powerMultiplier,
                            double elasticity,
                            double accuracyMultiplier,
                            boolean privacy,
                            boolean availability,
                            int price) {
        super(cueId,
                name,
                tipRingThickness,
                segments.get(segments.size() - 1).diameter2,
                segments.get(0).diameter1,
                tipRingColor,
                backColor,
                powerMultiplier,
                elasticity,
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
    
    public static final class GraphicSegment {
        private final PhongMaterial material;
        private final double length;
        private final double diameter1;
        private final double diameter2;
        
        GraphicSegment(PhongMaterial material, double length, double diameter1, double diameter2) {
            this.material = material;
            this.length = length;
            this.diameter1 = diameter1;
            this.diameter2 = diameter2;
        }

        public PhongMaterial getMaterial() {
            return material;
        }

        public double getLength() {
            return length;
        }

        public double getDiameter1() {
            return diameter1;
        }

        public double getDiameter2() {
            return diameter2;
        }
    }

    public static final class Segment {
        private String texture;
        private final double length;
        private final double diameter1;
        private final double diameter2;
        private int textureSep = 1;

        private final List<GraphicSegment> graphicSegments = new ArrayList<>();

        public Segment(String texture, double length, double diameter1, double diameter2) {
            this.texture = texture;
            this.length = length;
            this.diameter1 = diameter1;
            this.diameter2 = diameter2;

            if (SEP_TEXTURE) {
                double lwRatio = length / diameter2;
                textureSep = (int) Math.max(1, Math.round(lwRatio));
            }
            
            refillGraphSegments();
        }

        public int getTextureSep() {
            return textureSep;
        }

        public List<GraphicSegment> getGraphicSegments() {
            return graphicSegments;
        }

        private void refillGraphSegments() {
            graphicSegments.clear();
            
            Image texture = readTextureImg();
            
            if (texture != null) {
                Image[] separated = imageSegment(texture, textureSep);
                for (int i = 0; i < textureSep; i++) {
                    double d1 = diameter1 + (diameter2 - diameter1) * ((double) i / textureSep);
                    double d2 = diameter1 + (diameter2 - diameter1) * ((double) (i + 1) / textureSep);
                    
                    PhongMaterial material = new PhongMaterial();
                    material.setDiffuseMap(separated[i]);
                    
                    GraphicSegment gs = new GraphicSegment(material, 
                            length / textureSep,
                            d1, 
                            d2);
                    graphicSegments.add(gs);
                }
            } else {
                textureSep = 1;
                PhongMaterial material = getColorMaterial();
                graphicSegments.add(new GraphicSegment(
                        material,
                        length,
                        diameter1,
                        diameter2
                ));
            }
        }

//        private Image skewTexture(Image srcImage) {
//            double ratio = diameter1 / diameter2;
//            double origW = srcImage.getWidth();
//            double origH = srcImage.getHeight();
//            
//            double ulx = (0.5 - ratio / 2) * origW;
//            double urx = (0.5 + ratio / 2) * origW;
//            
//            PerspectiveTransform transform = new PerspectiveTransform(
//                    ulx, 0,
//                    urx, 0,
//                    origW, origH,
//                    0, origH
//            );
//            ImageView imageView = new ImageView(srcImage);
//            imageView.setEffect(transform);
//            
//            SnapshotParameters parameters = new SnapshotParameters();
//            parameters.setFill(Color.TRANSPARENT);
//            Image out = imageView.snapshot(parameters, null);
//            
//            return out;
//        }

        public void setTexture(String texture) {
            this.texture = texture;
            
            refillGraphSegments();
        }
        
        private Image readTextureImg() {
            if (texture.indexOf('.') != -1) {
                URL url = getClass()
                        .getResource("/trashsoftware/trashSnooker/res/img/256/cue/" +
                                texture);
                if (url != null) {
                    Image orig = new Image(url.toExternalForm());
                    return getImageInCorrectDirection(orig);
                } else {
                    // 外部
                    File file = new File(texture);
                    if (file.exists()) {
                        Image orig = new Image(file.getAbsolutePath());
                        return getImageInCorrectDirection(orig);
                    } else {
                        return null;
                    }
                }
            } else {
                return null;
            }
        }

        private PhongMaterial createMaterial() {
            PhongMaterial mat = new PhongMaterial();

            if (texture.indexOf('.') != -1) {
                URL url = getClass()
                        .getResource("/trashsoftware/trashSnooker/res/img/256/cue/" +
                                texture);
                if (url != null) {
                    Image orig = new Image(url.toExternalForm());
                    mat.setDiffuseMap(getImageInCorrectDirection(orig));
                } else {
                    // 外部
                    File file = new File(texture);
                    if (file.exists()) {
                        Image orig = new Image(file.getAbsolutePath());
                        mat.setDiffuseMap(getImageInCorrectDirection(orig));
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
        
        private PhongMaterial getColorMaterial() {
            PhongMaterial mat = new PhongMaterial();
            Color color;
            try {
                color = DataLoader.parseColor(texture);
            } catch (IllegalArgumentException iae) {
                color = Color.WHITESMOKE;
            }
            mat.setDiffuseColor(color);
            return mat;
        }
        
        private Image getImageInCorrectDirection(Image image) {
            if (image.getWidth() >= image.getHeight()) {
                ImageView iv = new ImageView(image);
                iv.setRotate(90);
                SnapshotParameters params = new SnapshotParameters();
                return iv.snapshot(params, null);
            } else {
                return image;
            }
        }
        
        private static Image[] imageSegment(Image src, int nSeg) {
            Image[] res = new Image[nSeg];
            
            BufferedImage image = SwingFXUtils.fromFXImage(src, null);
            int subHeight = image.getHeight() / nSeg;
            int width = image.getWidth();
            
            for (int r = 0; r < nSeg; r++) {
                // Creating sub image
                BufferedImage sub = new BufferedImage(width, subHeight, image.getType());
                Graphics2D img_creator = sub.createGraphics();

                // coordinates of source image
                int src_first_y = subHeight * r;

                // coordinates of sub-image
                int dst_corner_y = subHeight * r + subHeight;

                img_creator.drawImage(image, 
                        0, 
                        0, 
                        width, 
                        subHeight, 
                        0, 
                        src_first_y, 
                        width, 
                        dst_corner_y, 
                        null);
                res[r] = SwingFXUtils.toFXImage(sub, null);
            }
            return res;
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
