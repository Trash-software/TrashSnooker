package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.TexturedCue;
import trashsoftware.trashSnooker.util.DataLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CueModel3D extends CueModel {

    private final TexturedCue cue;
    private Color tipColor;

    List<SegmentGroup> bodySegments = new ArrayList<>();

    Cylinder ring;
    Cylinder tip;
    Cylinder base;

    private final Rotate cueAngleRotate = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
    private final Rotate rollRotate = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
    private double scale = 1.0;

    private CueModel3D(TexturedCue cue,
                       Color tipColor) {
        this.cue = cue;
        this.tipColor = tipColor;
        
        initSegments();

        build();

        Rotate alignRotate = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);
        alignRotate.setAngle(270);

        rollRotate.setAngle(180);

        getTransforms().addAll(alignRotate, baseRotate, cueAngleRotate, rollRotate);

//        setEffect(new Glow());

//        AmbientLight light = new AmbientLight();
//        light.setv
//        light.setColor(Color.WHITE);
//
//        getChildren().add(light);

//        DirectionalLight dl = new DirectionalLight(Color.WHITE);
//        dl.setDirection(new Point3D(0, 0, 1));
//        dl.setTranslateZ(-100);
//        dl.setTranslateX(-100);
//        dl.setTranslateY(-100);
//        
//        getChildren().add(dl);
    }

    public static CueModel3D makeDefault() {
        return makeSingleBodyCue((TexturedCue) DataLoader.getInstance().getCueById("stdSnookerCue"),
                Color.BLUE);
    }

    public static CueModel3D makeSingleBodyCue(TexturedCue cue,
                                               Color tipColor) {
        return new CueModel3D(
                cue,
                tipColor
        );
    }
    
    private void initSegments() {
        for (TexturedCue.Segment segment : cue.getSegments()) {
            SegmentGroup group = new SegmentGroup(segment, segment.createMaterial());
            bodySegments.add(group);
        }
    }

    private void build() {
        getChildren().clear();
        
        double tipRadius = cue.getCueTipWidth() / 2 * scale;
        double tipThickness = cue.cueTipThickness * scale;
        double ringThickness = cue.tipRingThickness * scale;
        double baseRadius = cue.getEndWidth() / 2 * scale;

        tip = new Cylinder(tipRadius, tipThickness);
        tip.setMaterial(new PhongMaterial(tipColor));
        ring = new Cylinder(tipRadius, ringThickness);
        ring.setMaterial(new PhongMaterial(cue.tipRingColor));
        base = new Cylinder(baseRadius, 1);
        base.setMaterial(new PhongMaterial(cue.backColor));

        tip.setTranslateY(-tipThickness / 2);
        ring.setTranslateY(ringThickness / 2);

        getChildren().addAll(tip, ring);
        
        double position = ringThickness;
        
        for (SegmentGroup seg : bodySegments) {
            double len = seg.segment.length() * scale;
            TruncateCone cone = new TruncateCone(48,
                    (float) (seg.segment.diameter1() * 0.5 * scale),
                    (float) (seg.segment.diameter2() * 0.5 * scale),
                    (float) len,
                    seg.material
                    );
            cone.setTranslateY(position);
            getChildren().add(cone);
            position += len;
        }

        base.setTranslateY(position);
        getChildren().add(base);
    }

    @Override
    protected void setCueAngle(double cueAngleDeg) {
        cueAngleRotate.setAngle(cueAngleDeg);
    }

    @Override
    public void setScale(double scale) {
        this.scale = scale;

        build();
    }

    @Override
    public void setCueRotation(double rotationDeg) {
        rollRotate.setAngle(rotationDeg);
    }

    static class SegmentGroup {
        final TexturedCue.Segment segment;
        final PhongMaterial material;
        
        SegmentGroup(TexturedCue.Segment segment, PhongMaterial material) {
            this.segment = segment;
            this.material = material;
        }
    }
}