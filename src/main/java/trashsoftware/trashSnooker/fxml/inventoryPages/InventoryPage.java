package trashsoftware.trashSnooker.fxml.inventoryPages;

import javafx.fxml.FXML;
import trashsoftware.trashSnooker.core.CueSelection;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.widgets.FixedCueList;

import java.util.ResourceBundle;

public class InventoryPage extends AbsInvPage {

    @FXML
    FixedCueList cueList;

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
                    900, 
                    humanCareer, () -> inventoryView.updateView(),
                    null, 
                    null);
        }
    }

    @Override
    public void reload() {
        fill();
    }
}
