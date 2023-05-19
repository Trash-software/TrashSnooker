package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.stage.Stage;

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
        System.out.println("Back");
        Stage stage = getStage();
        stage.setTitle(App.getStrings().getString("appName"));
        stage.setScene(parentScene);
        stage.sizeToScene();
        System.out.println(parentScene);
    }
}
