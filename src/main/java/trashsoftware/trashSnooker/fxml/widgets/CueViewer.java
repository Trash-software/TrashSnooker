package trashsoftware.trashSnooker.fxml.widgets;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import trashsoftware.trashSnooker.core.CueSelection;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.FastGameView;
import trashsoftware.trashSnooker.fxml.drawing.CueModel;
import trashsoftware.trashSnooker.fxml.drawing.CueModel3D;

import java.util.ResourceBundle;

public class CueViewer extends Pane {
    
    private final CueSelection.CueAndBrand cueAndBrand;
    private final Cue cue;
    private final CueModel cueModel;
    
    VBox cueInfoBox = new VBox();
    private final HBox cueInfoSecondRow = new HBox();

    private Rotate xRotate = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
    private Rotate yRotate = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
    
    private double scale;
    
    private double lastDragX, lastDragY;
    private boolean dragging;
    
    private ResourceBundle strings;
    
    public CueViewer(ResourceBundle strings, CueSelection.CueAndBrand cueAndBrand, double width) {
        this.strings = strings;
        this.cueAndBrand = cueAndBrand;
        this.cue = cueAndBrand.getNonNullInstance();
        
        scale = (width * 0.95) / cueAndBrand.brand.getWoodPartLength();
        this.cueModel = CueModel.createCueModel(this.cue, scale);
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

                    double dstXRotate = Math.max(0, Math.min(75, xRotate.getAngle() - dx * 0.25));
                    xRotate.setAngle(dstXRotate);
                    yRotate.setAngle(yRotate.getAngle() + dy);
                }
                lastDragX = e.getX();
                lastDragY = e.getY();
                dragging = true;
            });
            
            setOnMouseDragReleased(e -> dragging = false);  // 这个好像不管用
            setOnMouseReleased(e -> dragging = false);
            
            setOnMouseClicked(e -> {
                System.out.println("CLick!");
                if (e.getButton() == MouseButton.SECONDARY) {
                    xRotate.setAngle(0);
                    yRotate.setAngle(0);
                }
            });
        }
        
        fillCueInfo();
        Separator sp1 = new Separator(Orientation.HORIZONTAL);
        sp1.setHalignment(HPos.CENTER);
        sp1.setPrefWidth(width);
//        Separator sp2 = new Separator(Orientation.HORIZONTAL);
//        sp2.setHalignment(HPos.CENTER);
//        sp2.setPrefWidth(width);
        
        getChildren().addAll(sp1, cueInfoBox, cueModel);
        
        setPadding(new Insets(5.0));
        
        setPrefWidth(width);
        setWidth(width);
        setMinHeight(width * 0.1);
        setMaxWidth(width);
        setMaxHeight(width * 0.1);
    }
    
    public void updateModel() {
        cueModel.setScale(scale);
    }

    public void addExtra(Node extra) {
        cueInfoSecondRow.setAlignment(Pos.CENTER_LEFT);
        cueInfoSecondRow.setSpacing(10.0);
        cueInfoSecondRow.getChildren().add(extra);
    }

    public void show() {
        setVisible(true);
        cueModel.show();
    }
    
    public void hide() {
        setVisible(false);
        cueModel.hide();
    }

    public Cue getCue() {
        return cue;
    }

    public CueSelection.CueAndBrand getCueAndBrand() {
        return cueAndBrand;
    }

    public CueModel getCueModel() {
        return cueModel;
    }

    private void fillCueInfo() {
        Label nameLabel = new Label(cue.getName());
        nameLabel.setFont(new Font(App.FONT.getName(), App.FONT.getSize() * 1.35));
        cueInfoBox.getChildren().addAll(nameLabel);
        String cueInfo = String.format(
                "%s: %.1f cm  %s: %.0f  %s: %.0f  %s: %.0f",
                strings.getString("cueRingDiameter"), cueAndBrand.brand.getCueTipWidth(),
                strings.getString("cuePower"), cue.getOrigPowerMultiplier() * 100,
                strings.getString("cueAiming"), cue.getAccuracyMultiplier() * 100,
                strings.getString("cueSpin"), cueAndBrand.brand.theoreticalSpinStrength() * 100
        );
        cueInfoSecondRow.getChildren().add(new Label(cueInfo));
        cueInfoBox.getChildren().add(cueInfoSecondRow);
    }
}
