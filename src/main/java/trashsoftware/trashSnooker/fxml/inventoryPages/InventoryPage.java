package trashsoftware.trashSnooker.fxml.inventoryPages;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import trashsoftware.trashSnooker.core.CueSelection;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.CueTip;
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
        cueList.setDisplayComparator((a, b) -> {
            // 1. 最近买的在前
            int purchaseTimeCmp = -a.getNonNullInstance().getCreationTime().compareTo(b.getCueInstance().getCreationTime());
            if (purchaseTimeCmp != 0) return purchaseTimeCmp;
            // 2. 贵的在前
            return -Integer.compare(a.brand.getPrice(), b.brand.getPrice());
        });
        for (Cue cue : inventoryManager.getAllCues()) {
            CueSelection.CueAndBrand cab = new CueSelection.CueAndBrand(cue);
            cueList.addCue(cab, 
                    900, 
                    humanCareer, 
                    () -> inventoryView.updateView(),
                    null, 
                    null,
                    false);
        }
        cueList.display();
    }
    
    @FXML
    void tipHelpAction() {
        Stage stage = new Stage();
        stage.initOwner(getScene().getWindow());
        stage.initStyle(StageStyle.UTILITY);

        String tipDes = String.format(
                strings.getString("tipHpDesFmt"),
                (int) CueTip.TIP_HEALTH_LOW * 100
        );
        
        VBox root = new VBox();
        root.setPadding(new Insets(10.0));
        root.getChildren().add(new Label(tipDes));

        Scene scene = new Scene(root);
        stage.setScene(scene);
        
        stage.show();
    }

    @Override
    public void reload() {
        fill();
    }
}
