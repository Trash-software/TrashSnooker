package trashsoftware.trashSnooker.fxml.widgets;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.BoundingBox;
import javafx.scene.AmbientLight;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Box;
import javafx.scene.shape.Rectangle;
import trashsoftware.trashSnooker.core.CueSelection;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.FastGameView;
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
//        bound.widthProperty().bind(content.widthProperty());
//        bound.heightProperty().bind(content.heightProperty());
//        content.setClip(bound);
    }
    
    public void clear() {
        content.getChildren().clear();
        nCues = 0;
    }
    
    public void addCue(CueSelection.CueAndBrand cue, 
                       double prefWidth, 
                       String buttonText, 
                       Runnable buttonCallback) {
        try {
            CueViewer viewer = new CueViewer(strings, cue, prefWidth);
            content.add(viewer, 0, nCues);

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