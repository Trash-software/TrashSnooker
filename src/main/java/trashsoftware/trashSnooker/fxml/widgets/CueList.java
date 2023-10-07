package trashsoftware.trashSnooker.fxml.widgets;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.fxml.App;

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
    }
    
    public void addCue(Cue cue, double prefWidth, String buttonText, Runnable buttonCallback) {
        CueViewer viewer = new CueViewer(strings, cue, prefWidth);
        content.add(viewer, 0, nCues);

        Button actionButton = new Button(buttonText);
        if (buttonCallback == null) {
            actionButton.setDisable(true);
        } else {
            actionButton.setOnAction(event -> buttonCallback.run());
        }
        content.add(actionButton, 1, nCues);
        
        nCues++;
    }
}
