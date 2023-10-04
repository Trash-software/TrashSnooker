package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.Group;
import javafx.scene.transform.Rotate;
import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.PlanarCue;
import trashsoftware.trashSnooker.core.cue.TexturedCue;
import trashsoftware.trashSnooker.fxml.GameView;

public abstract class CueModel extends Group {

    protected final Rotate baseRotate = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);
    
    public static CueModel createCueModel(Cue cue) {
        if (cue instanceof TexturedCue tc) {
            // todo: 锥度
            return CueModel3D.makeSingleBodyCue(
                    tc,
                    GameView.CUE_TIP_COLOR
            );
        } else if (cue instanceof PlanarCue pc) {
            return new CueModel2D(pc);
        } else {
            throw new RuntimeException("Unsupported cue type");
        }
    }

    public void show(double correctedTipX,
                     double correctedTipY,
                     double pointingUnitX,
                     double pointingUnitY,
                     double cueAngleDeg,
                     double scale) {
        setTranslateX(correctedTipX);
        setTranslateY(correctedTipY);

        setScale(scale);

        double angleRad = Algebra.thetaOf(-pointingUnitX, -pointingUnitY);  // 因为默认杆头是朝左的
        baseRotate.setAngle(Math.toDegrees(angleRad));
        
        setCueAngle(cueAngleDeg);

        setVisible(true);
    }

    public void hide() {
        setVisible(false);
    }
    
    protected abstract void setCueAngle(double cueAngleDeg);
    
    protected abstract void setScale(double scale);
    
    public abstract void setCueRotation(double rotationDeg);
}
