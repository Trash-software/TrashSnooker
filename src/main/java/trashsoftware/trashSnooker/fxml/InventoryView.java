package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.fxml.inventoryPages.InventoryPage;
import trashsoftware.trashSnooker.fxml.inventoryPages.StorePage;

import java.net.URL;
import java.util.ResourceBundle;

public class InventoryView extends ChildInitializable {
    
    @FXML
    TabPane baseTabPane;
    
    @FXML
    InventoryPage inventoryRoot;
    
    @FXML
    StorePage storeRoot;
    
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Override
    public Stage getStage() {
        return stage;
    }
    
    public void setup(boolean isInventory) {
        if (isInventory) {
            baseTabPane.getSelectionModel().select(0);
        } else {
            baseTabPane.getSelectionModel().select(1);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }
}
