package trashsoftware.trashSnooker.fxml;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.ArrayDeque;
import java.util.Deque;

public abstract class ChildInitializable implements Initializable {
    private Scene parentScene;
//    private T parentController;
    
    public void setParent(Scene parentScene) {
        this.parentScene = parentScene;
//        this.parentController = parentController;
    }
    
    public abstract Stage getStage();
    
    @FXML
    public void backAction() {
        Stage stage = getStage();
        stage.setTitle(App.getStrings().getString("appName"));
        stage.setScene(parentScene);
        stage.sizeToScene();
    }
}
