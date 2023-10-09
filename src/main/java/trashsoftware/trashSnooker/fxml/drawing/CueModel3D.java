package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import trashsoftware.trashSnooker.core.cue.TexturedCue;
import trashsoftware.trashSnooker.util.DataLoader;

public class CueModel3D extends CueModel {

    private final TexturedCue cue;
    private Color tipColor;
    private final int conePoly;

    Cylinder ring;
    Cylinder tip;
    Cylinder base;

    private final Rotate cueAngleRotate = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
    private final Rotate rollRotate = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
    private double scale;
    private double renderingScale;

    public CueModel3D(TexturedCue cue,
                      Color tipColor,
                      double initScale) {
        this(cue, tipColor, 48, initScale);
    }

    public CueModel3D(TexturedCue cue,
                      Color tipColor,
                      int conePoly,
                      double initScale) {
        this.cue = cue;
        this.tipColor = tipColor;
        this.conePoly = conePoly;
        
        this.scale = initScale;
        this.renderingScale = initScale;

        build();

        Rotate alignRotate = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);
        alignRotate.setAngle(270);

        rollRotate.setAngle(180);

        getTransforms().addAll(alignRotate, baseRotate, cueAngleRotate, rollRotate);
    }

    public static CueModel3D makeDefault(String cueId, int poly) {
        return new CueModel3D((TexturedCue) DataLoader.getTestInstance().getCueById(cueId),
                Color.BLUE,
                poly,
                1.0);
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

        for (TexturedCue.Segment segment : cue.getSegments()) {
            double len = segment.length() * scale;
            TruncateCone cone = new TruncateCone(conePoly,
                    (float) (segment.diameter1() * 0.5 * scale),
                    (float) (segment.diameter2() * 0.5 * scale),
                    (float) len,
                    segment.getMaterial(),
                    false
            );
            cone.setTranslateY(position);
            getChildren().add(cone);
            position += len;
        }

        base.setTranslateY(position);
        getChildren().add(base);
    }

    @Override
    public void setCueAngle(double cueAngleDeg) {
        cueAngleRotate.setAngle(cueAngleDeg);
    }

    @Override
    public void setScale(double scale) {
        if (scale != renderingScale) {
            this.scale = scale;
            build();
            renderingScale = scale;
        }
    }

    @Override
    public void setCueRotation(double rotationDeg) {
        rollRotate.setAngle(rotationDeg);
    }
}