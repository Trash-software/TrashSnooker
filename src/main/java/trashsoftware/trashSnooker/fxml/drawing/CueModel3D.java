package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.TexturedCueBrand;
import trashsoftware.trashSnooker.util.DataLoader;

public class CueModel3D extends CueModel {
    
    public static final int CIRCLE_DIVISION = 48;

    private final Cue cue;
    private final TexturedCueBrand brand;
    private Color tipColor;
    private final int conePoly;

    Cylinder ring;
    Cylinder base;
    TipModel tipModel;

    private final Rotate cueAngleRotate = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
    private final Rotate rollRotate = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
    private double scale;
    private double renderingScale;

    public CueModel3D(Cue cue,
                      Color tipColor,
                      double initScale) {
        this(cue, tipColor, CIRCLE_DIVISION, initScale);
    }

    public CueModel3D(Cue cue,
                      Color tipColor,
                      int conePoly,
                      double initScale) {
        this.cue = cue;
        this.tipColor = tipColor;
        this.conePoly = conePoly;
        
        this.brand = (TexturedCueBrand) cue.getBrand();
        
        this.scale = initScale;
        this.renderingScale = initScale;

        build();

        Rotate alignRotate = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);
        alignRotate.setAngle(270);

        rollRotate.setAngle(180);

        getTransforms().addAll(alignRotate, baseRotate, cueAngleRotate, rollRotate);
    }

    public static CueModel3D makeDefault(String cueId, int poly) {
        TexturedCueBrand tcb = (TexturedCueBrand) DataLoader.getTestInstance().getCueById(cueId);
        Cue testCue = Cue.createOneTimeInstance(tcb);
        return new CueModel3D(testCue,
                Color.BLUE,
                poly,
                1.0);
    }

    protected void build() {
        getChildren().clear();

        double tipRadius = cue.getCueTipWidth() / 2 * scale;
//        double tipThickness = cue.cueTipThickness * scale;
        double ringThickness = cue.getBrand().tipRingThickness * scale;
        double baseRadius = cue.getEndWidth() / 2 * scale;
        
        tipModel = CueTipModel.create(cue.getCueTip());
        tipModel.setScale(scale);
        ring = new Cylinder(tipRadius, ringThickness);
        ring.setMaterial(new PhongMaterial(cue.getBrand().tipRingColor));
        base = new Cylinder(baseRadius, 1);
        base.setMaterial(new PhongMaterial(cue.getBrand().backColor));

//        tip.setTranslateY(-tipThickness / 2);
        ring.setTranslateY(ringThickness / 2);
        
        getChildren().add(tipModel);
        getChildren().add(ring);

        double position = ringThickness;

        for (TexturedCueBrand.Segment segment : brand.getSegments()) {
            double len = segment.length() * scale;
            TruncateCone cone = new TruncateCone(conePoly,
                    (float) (segment.diameter1() * 0.5 * scale),
                    (float) (segment.diameter2() * 0.5 * scale),
                    (float) len,
                    segment.getMaterial()
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