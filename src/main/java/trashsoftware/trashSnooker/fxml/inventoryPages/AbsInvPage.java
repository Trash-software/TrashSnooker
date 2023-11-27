package trashsoftware.trashSnooker.fxml.inventoryPages;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import trashsoftware.trashSnooker.core.career.Career;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.HumanCareer;
import trashsoftware.trashSnooker.core.career.InventoryManager;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.InventoryView;

import java.io.IOException;
import java.util.ResourceBundle;

public abstract class AbsInvPage extends VBox {

    protected final ResourceBundle strings;
    
    protected HumanCareer humanCareer;
    protected InventoryManager inventoryManager;
    
    protected InventoryView inventoryView;

    public AbsInvPage(String path) {
        this(path, App.getStrings());
    }

    public AbsInvPage(String path, ResourceBundle strings) {
        super();

        this.strings = strings;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                path), strings);
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        
        CareerManager careerManager = CareerManager.getInstance();
        this.humanCareer = careerManager.getHumanPlayerCareer();
        this.inventoryManager = careerManager.getInventory();
    }
    
    public abstract void reload();

    public void setParent(InventoryView inventoryView) {
        this.inventoryView = inventoryView;
    }
}
