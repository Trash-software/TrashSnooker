package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import trashsoftware.trashSnooker.fxml.widgets.CueList;

import java.net.URL;
import java.util.ResourceBundle;

public class CueSelectionView implements Initializable {
    
    @FXML
    CueList cueList;
    
    private ResourceBundle strings;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;
    }
}
