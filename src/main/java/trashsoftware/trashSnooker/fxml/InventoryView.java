package trashsoftware.trashSnooker.fxml;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
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
    ImageView moneyImage, inventoryImage, storeImage;

    private HumanCareer humanCareer;

    private Stage stage;
    private ChangeListener<Number> resizeListener;

    @Override
    public void backAction() {
        stage.widthProperty().removeListener(resizeListener);
        stage.heightProperty().removeListener(resizeListener);
        
        super.backAction();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        
        resizeListener = (observable, oldValue, newValue) -> updateView();

        stage.widthProperty().addListener(resizeListener);
        stage.heightProperty().addListener(resizeListener);
    }

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
        updateView();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ResourcesLoader rl = ResourcesLoader.getInstance();
        rl.setIconImage(rl.getMoneyImg(), moneyImage);
        rl.setIconImage(rl.getInventoryIcon(), inventoryImage, 1.0, 1.25);
        rl.setIconImage(rl.getStoreIcon(), storeImage, 1.0, 1.25);

        humanCareer = CareerManager.getInstance().getHumanPlayerCareer();
    }

    public void updateView() {
        int money = humanCareer.getMoney();
        moneyLabel.setText(Util.moneyToReadable(money));
        if (money < 0) {
            moneyLabel.setTextFill(CareerView.SPEND_MONEY_COLOR);
        } else {
            moneyLabel.setTextFill(Color.BLACK);
        }

        storeRoot.reload();
        inventoryRoot.reload();
    }

    public double getCueWidth() {
        return getStage().getWidth() * 0.9;
    }

    public int getNumRows() {
        double w = getStage().getWidth();
        double h = getStage().getHeight() - 150;
        double aspectRatio = w / h;
        return Math.max(1, (int) (10 / aspectRatio));
    }
}
