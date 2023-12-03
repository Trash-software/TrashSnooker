package trashsoftware.trashSnooker.fxml.widgets;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import trashsoftware.trashSnooker.core.CueSelection;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.drawing.CueModel;
import trashsoftware.trashSnooker.fxml.drawing.CueModel3D;
import trashsoftware.trashSnooker.util.EventLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class CueList extends ScrollPane {
    
    @FXML
    GridPane content;
    
//    private List<CueViewer> cueViewers = new ArrayList<>();
    private int nCues;
    
    private ResourceBundle strings;
    
    public CueList() {
        this(App.getStrings());
    }
    
    public CueList(ResourceBundle strings) {
        super();

        this.strings = strings;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "cueList.fxml"), strings);
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        // clip造成各种各样的怪毛病
//        Rectangle bound = new Rectangle(0, 0, 0, 0);
////        Box bound = new Box(0, 0, 0);
////        bound.setTranslateZ(-5000);
//        
//        bound.widthProperty().bind(widthProperty());
//        bound.heightProperty().bind(heightProperty());
//        setClip(bound);
//        content.setClip(bound);
        
//        vvalueProperty().addListener((observable, oldValue, newValue) -> {
//            updateVisibility();
//        });
    }
    
    private void updateVisibility() {
        List<CueModel> showing = getVisibleModels();
        
        for (Node node : content.getChildren()) {
            if (node instanceof CueViewer cv) {
                CueModel model = cv.getCueModel();
                if (showing.contains(model)) {
//                    System.out.println(cv.getCue().getInstanceId() + " In");
                    model.show();
                } else {
//                    System.out.println(cv.getCue().getInstanceId() + " Out");
                    model.hide();
                }
            }
        }
        System.out.println("==========");
    }

    private List<CueModel> getVisibleModels() {
        List<CueModel> visibleModels = new ArrayList<>();
        Bounds paneBounds = localToScene(getBoundsInParent());
//        System.out.println("Pane " + paneBounds);
//        if (getContent() instanceof Parent) {
            for (Node n : content.getChildrenUnmodifiable()) {
                if (n instanceof CueViewer cv) {
                    CueModel cm3 = cv.getCueModel();
                    Bounds nodeBounds = cm3.localToScene(cm3.getBoundsInLocal());
//                    System.out.println(nodeBounds);
                    if (paneBounds.getMinY() < nodeBounds.getMinY() && paneBounds.getMaxY() > nodeBounds.getMaxY()) {
                        visibleModels.add(cm3);
                    }
//                    if (paneBounds.intersects(nodeBounds)) {
//                        visibleModels.add(cm3);
//                    }
                }
            }
//        }
        return visibleModels;
    }
    
    public void clear() {
        content.getChildren().clear();
        nCues = 0;
    }
    
    public void addCue(CueSelection.CueAndBrand cueAndBrand, 
                       double prefWidth, 
                       String buttonText, 
                       Runnable buttonCallback) {
        try {
            CueViewer viewer = new CueViewer(strings, cueAndBrand, prefWidth);
//            viewer.hide();
            
            content.add(viewer, 0, nCues);
            
//            updateVisibility();
//            Cue cue = cueAndBrand.getNonNullInstance();
//            CueModel cueModel = CueModel.createCueModel(cue, (prefWidth * 0.95) / cueAndBrand.brand.getWoodPartLength());
//            content.addRow(nCues, cueModel);
//            content.add(cueModel, 0, nCues);
            
            if (buttonText != null) {
                Button actionButton = new Button(buttonText);
                if (buttonCallback == null) {
                    actionButton.setDisable(true);
                } else {
                    actionButton.setOnAction(event -> buttonCallback.run());
                }
                content.add(actionButton, 1, nCues);
            }

            nCues++;
        } catch (Exception e) {
            EventLogger.error(e);
        }
    }
}
