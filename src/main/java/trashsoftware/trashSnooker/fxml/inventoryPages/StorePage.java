package trashsoftware.trashSnooker.fxml.inventoryPages;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import trashsoftware.trashSnooker.core.CueSelection;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.CueBrand;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.widgets.CueList;
import trashsoftware.trashSnooker.util.DataLoader;

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;

public class StorePage extends AbsInvPage {

    @FXML
    CueList cueList;

    public StorePage() {
        this(App.getStrings());
    }

    public StorePage(ResourceBundle strings) {
        super("storePage.fxml", strings);
        
        fill();
    }

    private void fill() {
        List<Cue> haves = inventoryManager.getAllCues();
        OUT_LOOP:
        for (CueBrand cueBrand : DataLoader.getInstance().getCues().values()) {
            if (cueBrand.isAvailable()) {
                for (Cue have : haves) {
                    if (have.getBrand().getCueId().equals(cueBrand.getCueId())) {
                        continue OUT_LOOP;
                    }
                }
                // todo: 预览版instance
                CueSelection.CueAndBrand cab = new CueSelection.CueAndBrand(cueBrand);
                cueList.addCue(cab, 640,
                        strings.getString("buy"), null);
            }
        }
    }
}
