package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class AboutView implements Initializable {
    
    @FXML
    Label versionLabel;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        versionLabel.setText(String.format("%s-%s", App.VERSION_NAME, App.CLASSIFIER));
    }
}
