package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.HumanCareer;
import trashsoftware.trashSnooker.fxml.inventoryPages.InventoryPage;
import trashsoftware.trashSnooker.fxml.inventoryPages.StorePage;
import trashsoftware.trashSnooker.res.ResourcesLoader;
import trashsoftware.trashSnooker.util.Util;

import java.net.URL;
import java.util.ResourceBundle;

public class InventoryView extends ChildInitializable {

    @FXML
    TabPane baseTabPane;

    @FXML
    InventoryPage inventoryRoot;

    @FXML
    StorePage storeRoot;

    @FXML
    Label moneyLabel;
    
    @FXML
    ImageView moneyImage;
    
    private HumanCareer humanCareer;

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
        inventoryRoot.setParent(this);
        storeRoot.setParent(this);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ResourcesLoader rl = ResourcesLoader.getInstance();
        rl.setIconImage(rl.getMoneyImg(), moneyImage);
        
        humanCareer = CareerManager.getInstance().getHumanPlayerCareer();
        
        updateView();
    }
    
    public void updateView() {
        moneyLabel.setText(Util.moneyToReadable(humanCareer.getMoney()));
        
        storeRoot.reload();
        inventoryRoot.reload();
    }
}
