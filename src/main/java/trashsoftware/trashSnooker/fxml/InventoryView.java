package trashsoftware.trashSnooker.fxml;

import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class InventoryView extends ChildInitializable {
    
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Override
    public Stage getStage() {
        return null;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }
}
