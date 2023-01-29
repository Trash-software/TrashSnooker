package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.fxml.widgets.AbilityShower;

import java.net.URL;
import java.util.ResourceBundle;

public class AbilityView implements Initializable {
    
    @FXML
    AbilityShower abilityContainer;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void setup(Scene scene, PlayerPerson pp) {
        abilityContainer.setup(pp, false);
    }
}
