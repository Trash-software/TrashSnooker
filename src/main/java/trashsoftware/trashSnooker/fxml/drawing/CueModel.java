package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.Group;
import javafx.scene.transform.Rotate;
import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.PlanarCueBrand;
import trashsoftware.trashSnooker.core.cue.TexturedCueBrand;
import trashsoftware.trashSnooker.core.person.PlayerHand;
import trashsoftware.trashSnooker.fxml.GameView;

public abstract class CueModel extends Group {

    protected final Rotate baseRotate = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);

    public static CueModel createCueModel(Cue cue) {
        return createCueModel(cue, 1.0);
    }

    public static CueModel createCueModel(Cue cue, double initScale) {
        if (cue.getBrand() instanceof TexturedCueBrand) {
            return new CueModel3D(cue,
                    GameView.CUE_TIP_COLOR,
                    initScale
            );
        } else if (cue.getBrand() instanceof PlanarCueBrand) {
            return new CueModel2D(cue,
                    initScale);
        } else {
            throw new RuntimeException("Unsupported cue type");
        }
    }
    
    public void show() {
        setVisible(true);
    }

    public void show(double correctedTipX,
                     double correctedTipY,
                     double pointingUnitX,
                     double pointingUnitY,
                     double cueAngleDeg,
                     double scale,
                     PlayerHand.CueExtension extension) {
        setTranslateX(correctedTipX);
        setTranslateY(correctedTipY);

        setScale(scale);

        double angleRad = Algebra.thetaOf(-pointingUnitX, -pointingUnitY);  // 因为默认杆头是朝左的
        baseRotate.setAngle(Math.toDegrees(angleRad));

        setCueAngle(cueAngleDeg);
        setExtension(extension);

        setVisible(true);
    }

    public void hide() {
        setVisible(false);
    }

    public abstract void setCueAngle(double cueAngleDeg);

    public abstract void setScale(double scale);
    
    public abstract void redraw();

    public abstract void setCueRotation(double rotationDeg);
    
    public abstract void setExtension(PlayerHand.CueExtension extension);
}
