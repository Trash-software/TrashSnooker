package trashsoftware.trashSnooker.fxml.widgets;

import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.TexturedCue;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.drawing.CueModel;
import trashsoftware.trashSnooker.fxml.drawing.CueModel3D;

import java.util.ResourceBundle;

public class CueViewer extends Pane {
    
    private final Cue cue;
    private final CueModel cueModel;
    
    VBox cueInfoBox = new VBox();

    private Rotate xRotate = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
    private Rotate yRotate = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
    
    private double lastDragX, lastDragY;
    private boolean dragging;
    
    private ResourceBundle strings;
    
    public CueViewer(ResourceBundle strings, Cue cue, double width) {
        this.strings = strings;
        this.cue = cue;
        this.cueModel = CueModel.createCueModel(cue, (width * 0.95) / cue.getTotalLength());
        xRotate.setPivotY(width / 2);
        
        cueModel.setTranslateX(width * 0.05);
        cueModel.setTranslateY(width * 0.075);
        
//        setStyle("-fx-background-color: grey;");
        
        cueModel.getTransforms().addAll(xRotate, yRotate);
        
        if (this.cueModel instanceof CueModel3D) {
            setOnMouseDragged(e -> {
                if (dragging) {
                    double dx = e.getX() - lastDragX;
                    double dy = e.getY() - lastDragY;

                    double dstXRotate = Math.max(0, Math.min(75, xRotate.getAngle() - dx * 0.5));
                    xRotate.setAngle(dstXRotate);
                    yRotate.setAngle(yRotate.getAngle() + dy * 0.5);
                }
                lastDragX = e.getX();
                lastDragY = e.getY();
                dragging = true;
            });
            
            setOnMouseDragReleased(e -> {
                dragging = false;
            });
            
            setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    xRotate.setAngle(0);
                    yRotate.setAngle(0);
                }
            });
        }
        
        fillCueInfo();
        getChildren().addAll(cueInfoBox, cueModel);

//        setVgrow(cueModel, Priority.NEVER);
        
        setWidth(width);
        setMinHeight(width * 0.1);
        setMaxWidth(width);
        setMaxHeight(width * 0.1);
    }
    
    private void fillCueInfo() {
        Label nameLabel = new Label(cue.getName());
        nameLabel.setFont(new Font(App.FONT.getName(), App.FONT.getSize() * 1.35));
        cueInfoBox.getChildren().addAll(nameLabel);
        String cueInfo = String.format(
                "%s: %.1f cm  %s: %.0f  %s: %.0f  %s: %.0f",
                strings.getString("cueRingDiameter"), cue.getCueTipWidth(),
                strings.getString("cuePower"), cue.powerMultiplier * 100,
                strings.getString("cueAiming"), cue.accuracyMultiplier * 100,
                strings.getString("cueSpin"), cue.spinMultiplier * 100
        );
        cueInfoBox.getChildren().add(new Label(cueInfo));
    }
}
