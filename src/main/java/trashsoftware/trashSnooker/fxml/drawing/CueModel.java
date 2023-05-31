package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.Cue;
import trashsoftware.trashSnooker.fxml.GameView;

import java.util.ArrayList;
import java.util.List;

public class CueModel extends Group {

    private static final List<CueModel> allCueModels = new ArrayList<>();
    private final boolean isRest;
    private final Rotate baseRotate = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);
    private final Scale scalar = new Scale();
    private final Scale cueAngleScale = new Scale(1.0, 1.0);

    public Polygon tip;
    public Polygon tipRing;
    public Polygon front;
    public Polygon mid;
    public Polygon back;
    public Polygon tail;

    public CueModel(Cue cue) {
        double tipFrontX = 0;
        double tipBackX = cue.cueTipThickness;
        double tipY = cue.getCueTipWidth() / 2;

        isRest = "restCue".equals(cue.cueId);
        if (isRest) {
            double restX = 30.0;
            tip = new Polygon(
                    tipFrontX, -restX,
                    tipBackX, -restX,
                    tipBackX, restX,
                    tipFrontX, restX
            );
            tip.setFill(GameView.REST_METAL_COLOR);
        } else {
            tip = new Polygon(
                    tipFrontX, -tipY,
                    tipBackX, -tipY,
                    tipBackX, tipY,
                    tipFrontX, tipY);
            tip.setFill(GameView.CUE_TIP_COLOR);
        }
        getChildren().add(tip);

        double ringBackX = tipBackX + cue.tipRingThickness;

        tipRing = new Polygon(
                tipBackX, -tipY,
                ringBackX, -tipY,
                ringBackX, tipY,
                tipBackX, tipY
        );
        tipRing.setFill(cue.tipRingColor);
        getChildren().add(tipRing);

        double frontBackX = tipBackX + cue.frontLength;
        double frontBackY = cue.getFrontMaxWidth() / 2;
        front = new Polygon(
                ringBackX, -tipY,
                frontBackX, -frontBackY,
                frontBackX, frontBackY,
                ringBackX, tipY
        );
        front.setFill(cue.frontColor);
        getChildren().add(front);

        if (cue.arrow != null) {
            for (int i = 0; i < cue.arrow.arrowScales.length; i++) {
                double[] arrow = cue.arrow.arrowScales[i];
                Arc arc = new Arc(
                        arrow[1],
                        0,
                        arrow[1] - arrow[0],
                        arrow[2] / 2 - cue.arrow.depth / 2,
                        90,
                        180
                );
                arc.setType(ArcType.OPEN);
                arc.setStroke(cue.arrow.arrowColor);
                arc.setStrokeWidth(cue.arrow.depth);
                arc.setFill(cue.frontColor);
                getChildren().add(arc);
            }

            // 握把向前涂色部分
            double[] last = cue.arrow.arrowScales[cue.arrow.arrowScales.length - 1];
            double lastScale = last[1] - last[0];
            Arc arc = new Arc(
                    frontBackX,
                    0,
                    lastScale,
                    cue.getFrontMaxWidth() / 2,
                    90,
                    180
            );
            arc.setStrokeWidth(0);
            arc.setFill(cue.backColor);
            getChildren().add(arc);
        }

        double midBackX = frontBackX + cue.midLength;
        double midBackY = cue.getMidMaxWidth() / 2;
        mid = new Polygon(
                frontBackX, -frontBackY,
                midBackX, -midBackY,
                midBackX, midBackY,
                frontBackX, frontBackY
        );
        mid.setFill(cue.midColor);
        getChildren().add(mid);

        double backBackX = midBackX + cue.backLength;
        double backBackY = cue.getEndWidth() / 2;
        back = new Polygon(
                midBackX, -midBackY,
                backBackX, -backBackY,
                backBackX, backBackY,
                midBackX, midBackY
        );
        back.setFill(cue.backColor);
        getChildren().add(back);

        double tailLength = cue.getCueTipWidth() * 0.2;
        double tailY = tailLength * 0.75;
        double tailBackX = backBackX + tailLength;
        tail = new Polygon(
                backBackX, -backBackY,
                tailBackX, -tailY,
                tailBackX, tailY,
                backBackX, backBackY
        );
        tail.setFill(Color.BLACK.brighter());
        getChildren().add(tail);

        getTransforms().addAll(baseRotate, scalar);
        if (!isRest) {
            getTransforms().add(cueAngleScale);
        }

        allCueModels.add(this);
    }

    public static List<CueModel> getAllCueModels() {
        return allCueModels;
    }

    public void show(double correctedTipX, double correctedTipY,
                     double pointingUnitX, double pointingUnitY,
                     double cueAngleDeg,
                     double scale) {
        setTranslateX(correctedTipX);
        setTranslateY(correctedTipY);

        scalar.setX(scale);
        scalar.setY(scale);

        double angleRad = Algebra.thetaOf(-pointingUnitX, -pointingUnitY);  // 因为默认杆头是朝左的
        baseRotate.setAngle(Math.toDegrees(angleRad));

        double cueAngleCos = Math.cos(Math.toRadians(cueAngleDeg));
        cueAngleScale.setX(cueAngleCos);

        setVisible(true);
    }

    public void hide() {
        setVisible(false);
    }
}
