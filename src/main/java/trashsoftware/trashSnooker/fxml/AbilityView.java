package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import trashsoftware.trashSnooker.core.person.PlayerPerson;
import trashsoftware.trashSnooker.fxml.widgets.AbilityShower;

import java.net.URL;
import java.util.ResourceBundle;

public class AbilityView implements Initializable {
    
    @FXML
    AbilityShower abilityContainer;
    
    private ResourceBundle strings;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;
    }

    public void setup(Scene scene, PlayerPerson pp) {
//        PerkManager pm = new PerkManager(null, 0, );
        abilityContainer.setup(PlayerPerson.ReadableAbility.fromPlayerPerson(pp));
    }
    
    public void setOpponent(PlayerPerson opponent) {
        abilityContainer.setOpponent(PlayerPerson.ReadableAbility.fromPlayerPerson(opponent));
    }
}
