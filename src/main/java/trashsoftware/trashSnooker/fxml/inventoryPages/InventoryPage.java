package trashsoftware.trashSnooker.fxml.inventoryPages;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import trashsoftware.trashSnooker.core.CueSelection;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.widgets.CueList;

import java.io.IOException;
import java.util.ResourceBundle;

public class InventoryPage extends AbsInvPage {
    
    @FXML
    CueList cueList;
    
    public InventoryPage() {
        this(App.getStrings());
    }
    
    public InventoryPage(ResourceBundle strings) {
        super("inventoryPage.fxml", strings);
        
        reload();
    }
    
    private void fill() {
        cueList.clear();
        for (Cue cue : inventoryManager.getAllCues()) {
            CueSelection.CueAndBrand cab = new CueSelection.CueAndBrand(cue);
            cueList.addCue(cab, 
                    640,
                    null, 
                    null);
        }
    }

    @Override
    public void reload() {
        fill();
    }
}
