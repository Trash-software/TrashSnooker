package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import javafx.stage.FileChooser;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.TexturedCueBrand;
import trashsoftware.trashSnooker.fxml.drawing.CueModel;
import trashsoftware.trashSnooker.fxml.drawing.CueModel3D;
import trashsoftware.trashSnooker.util.DataLoader;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class CuePreviewerView implements Initializable {
    
    @FXML
    Pane basePane;
    @FXML
    ComboBox<CueType> cueTypeBox;
    @FXML
    TextField 
            frontColorField, 
            backColorField;
    @FXML
    Label 
//            frontTexturePath, 
            backTexturePath;
    
    CueModel3D model;

    private Rotate xRotate = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
    private Rotate yRotate = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
    
    double pivotY = 640;
    double translateX = 100;
    
    double scale = 0.7;

    private double lastDragX, lastDragY;
    private boolean dragging;
    
    private ResourceBundle strings;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;
        
        cueTypeBox.getItems().addAll(CueType.values());
        
        cueTypeBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            refreshModel();
        });

        basePane.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
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
            } else if (e.getButton() == MouseButton.SECONDARY) {
                if (dragging) {
                    double dx = e.getX() - lastDragX;
                    
                    translateX += dx;
                    pivotY += dx;
//                    xRotate.setPivotY(pivotY);
                    if (model != null) {
                        model.setTranslateX(translateX);
                    }
                }
                lastDragX = e.getX();
                lastDragY = e.getY();
                dragging = true;
            }
        });

        basePane.setOnMouseDragReleased(e -> dragging = false);  // 这个好像不管用
        basePane.setOnMouseReleased(e -> dragging = false);
        
        basePane.setOnScroll(e -> {
            if (model != null) {
                if (e.getDeltaY() > 0) {
                    scale += 0.05;
                } else if (e.getDeltaY() < 0) {
                    scale -= 0.05;
                }
                model.setScale(scale);
            }
        });

        basePane.setOnMouseClicked(e -> {
            System.out.println("CLick!");
        });
    }
    
    @FXML
    void resetViewport() {
        xRotate.setAngle(0);
        yRotate.setAngle(0);
        
        translateX = 100;
        scale = 0.7;
        pivotY = basePane.getWidth() / 2;
        xRotate.setPivotY(pivotY);

        dragging = false;
        
        if (model != null) {
            model.setScale(scale);
            model.setTranslateX(translateX);
            model.setTranslateY(100);
        }
    }
    
//    @FXML
//    void selectFrontFile() {
//        File file = selectFile();
//        if (file != null) {
//            frontTexturePath.setText(file.getAbsolutePath());
//        }
//    }
    
    @FXML
    void selectBackFile() {
        File file = selectFile();
        if (file != null) {
            backTexturePath.setText(file.getAbsolutePath());
        }
    }
    
    private File selectFile() {
        FileChooser chooser = new FileChooser();
        String curFile = backTexturePath.getText();
        File cur = new File(curFile);
        if (cur.exists()) {
            chooser.setInitialDirectory(cur.getParentFile());
        }
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("jpg", "*.jpg"),
                new FileChooser.ExtensionFilter("png", "*.png"),
                new FileChooser.ExtensionFilter("all", "*.jpg", "*.png")
        );
        return chooser.showOpenDialog(null);
    }
    
    @FXML
    void apply() {
        refreshModel();
    }
    
    private void refreshModel() {
        if (model != null) {
            model.hide();
            basePane.getChildren().clear();
        }
        
        if (cueTypeBox.getValue() == null) return;
        TexturedCueBrand cueBrand = create(cueTypeBox.getValue());

        for (int i = 0; i < cueBrand.getSegments().size() - 1; i++) {
            // 最后一段是屁股
            // 倒数第二段是握把
            TexturedCueBrand.Segment segment = cueBrand.getSegments().get(i);
            if (i == cueBrand.getSegments().size() - 2) {
                segment.setTexture(backColorField.getText());
                String back = backTexturePath.getText();
                File backFile = new File(back);
                if (backFile.exists()) {
                    segment.setTexture(backFile.getAbsolutePath());
                }
            } else {
                segment.setTexture(frontColorField.getText());
            }
        }
        
        Cue instance = Cue.createForTempView(cueBrand);
        model = (CueModel3D) CueModel.createCueModel(instance, 0.5);
        
        basePane.getChildren().add(model);
        model.getTransforms().addAll(xRotate, yRotate);
        
        resetViewport();
    }
    
    private TexturedCueBrand create(CueType cueType) {
        TexturedCueBrand cueBrand;
        if (cueType == CueType.BIG_HEAD) {
            cueBrand = (TexturedCueBrand) DataLoader.getInstance().getCues().get("trashCue1");
        } else {
            cueBrand = (TexturedCueBrand) DataLoader.getInstance().getCues().get("stdSnookerCue");
        }
        cueBrand = cueBrand.clone();  // 避免影响原本的
        cueBrand.removeTextures();
        return cueBrand;
    }
    
    public enum CueType {
        BIG_HEAD("bigHeadCue"),
        SMALL_HEAD("smallHeadCue");
        
        private final String key;
        
        CueType(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return App.getStrings().getString(key);
        }
    }
}
