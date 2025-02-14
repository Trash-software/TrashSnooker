package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Objects;

public abstract class ChildInitializable implements Initializable {
    private Parent parentRoot;
//    private T parentController;
    
    public void setParent(Parent parentRoot) {
        this.parentRoot = parentRoot;
//        this.parentController = parentController;
    }
//    
//    public void setParent(Window window) {
//        setParent(window.getScene());
//    }

    public void setParent(Scene scene) {
        parentRoot = (Parent) scene.getRoot().getChildrenUnmodifiable().get(0);
    }
    
    private Parent getParentRoot() {
        return Objects.requireNonNull(parentRoot);
    }
    
//    public abstract Stage getStage();
    
    @FXML
    public void backAction() {
        App.setRoot(getParentRoot());
//        Stage stage = getStage();
////        stage.setTitle(App.getStrings().getString("appName"));
////        stage.setScene(parentRoot);
//        
//        boolean wasMax = stage.isMaximized();
//        stage.sizeToScene();
//        if (wasMax) {
//            stage.setMaximized(false);
//            stage.setMaximized(true);
//        }
    }
}
