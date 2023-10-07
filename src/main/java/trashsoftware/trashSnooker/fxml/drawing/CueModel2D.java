package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Scale;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.PlanarCue;
import trashsoftware.trashSnooker.fxml.GameView;

public class CueModel2D extends CueModel {

//    private static final List<CueModel> allCueModels = new ArrayList<>();
    private final boolean isRest;

    public Polygon tip;
    public Polygon tipRing;
    public Polygon front;
    public Polygon mid;
    public Polygon back;
    public Polygon tail;

    private final Scale cueAngleScale = new Scale(1.0, 1.0, 1.0);
    private final Scale scalar = new Scale();

    CueModel2D(PlanarCue cue, double initScale) {
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
        
        setScale(initScale);

//        allCueModels.add(this);
    }

    @Override
    public void setCueAngle(double cueAngleDeg) {
        double cueAngleCos = Math.cos(Math.toRadians(cueAngleDeg));
        cueAngleScale.setX(cueAngleCos);
    }

    @Override
    public void setScale(double scale) {
        scalar.setX(scale);
        scalar.setY(scale);
        scalar.setZ(scale);
    }

    @Override
    public void setCueRotation(double rotationDeg) {
    }

    //    public static List<CueModel> getAllCueModels() {
//        return allCueModels;
//    }
}
