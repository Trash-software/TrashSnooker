package trashsoftware.trashSnooker.fxml.widgets;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.VBox;
import trashsoftware.trashSnooker.core.PlayerHand;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.fxml.App;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import static trashsoftware.trashSnooker.fxml.widgets.AbilityShower.numToString;

public class PerkAdderHandPage extends VBox {

    @FXML
    Label cuePrecisionLabel, powerLabel, spinLabel, powerControlLabel,
            spinControlLabel;
    @FXML
    ProgressBar cuePrecisionBar, powerBar, spinBar, powerControlBar,
            spinControlBar;
    @FXML
    Button cuePrecisionBtn, powerBtn, spinBtn, powerControlBtn,
            spinControlBtn;
    @FXML
    ColumnConstraints buttonsCol;

    private final Map<Button, PerkAdder.Combo> btnMap = new HashMap<>();
    private final Button[] buttons;
    
    PerkManager perkManager;
    PlayerHand.Hand hand;

    private final ResourceBundle strings;

    public PerkAdderHandPage() {
        this(App.getStrings());
    }

    public PerkAdderHandPage(ResourceBundle strings) {
        super();

        this.strings = strings;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "perkAdderHandPage.fxml"), strings);
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        buttons = new Button[]{
                cuePrecisionBtn, powerBtn, spinBtn, powerControlBtn,
                spinControlBtn
        };
        btnMap.putAll(Map.of(
                cuePrecisionBtn, new PerkAdder.Combo(PerkManager.CUE_PRECISION, cuePrecisionLabel),
                powerBtn, new PerkAdder.Combo(PerkManager.POWER, powerLabel),
                powerControlBtn, new PerkAdder.Combo(PerkManager.POWER_CONTROL, powerControlLabel),
                spinBtn, new PerkAdder.Combo(PerkManager.SPIN, spinLabel),
                spinControlBtn, new PerkAdder.Combo(PerkManager.SPIN_CONTROL, spinControlLabel)
        ));
    }
    
    public void setup(PerkManager perkManager, PlayerHand.Hand hand, boolean showAddButtons) {
        this.perkManager = perkManager;
        this.hand = hand;

        if (!showAddButtons) {
            buttonsCol.setPrefWidth(0);
            buttonsCol.setMaxWidth(0);

            for (Button button : buttons) {
                button.setVisible(false);
                button.setManaged(false);
            }
        }
        
        setupTexts();
    }

    @FXML
    public void addPerk(ActionEvent event) {
        Button src = (Button) event.getSource();

        PerkAdder.Combo combo = btnMap.get(src);
        int added = perkManager.addPerkTo(combo.cat, hand);
        src.setText(added + "+");

        double afterAdd = perkManager.getShownAbility().getAbilityByCat(combo.cat);
        double max = perkManager.getShownAbility().maxAbilityByCat(combo.cat);
        if (afterAdd >= max * 0.999) src.setDisable(true);

        setupTexts();
//        setupRadar();

        if (perkManager.getAvailPerks() <= 0) {
            for (Button button : buttons) {
                button.setDisable(true);
            }
        }
    }
    
    private void setupTexts() {
        PlayerPerson.ReadableAbilityHand realAbility = perkManager.getOriginalOf(hand);
        PlayerPerson.ReadableAbilityHand preview = perkManager.getShownOf(hand);
        
        String cuePrecision = numToString(realAbility.cuePrecision);
        if (preview.cuePrecision != realAbility.cuePrecision) {
            cuePrecision += " (" + numToString(preview.cuePrecision) + ")";
        }
        cuePrecisionLabel.setText(cuePrecision);
        cuePrecisionBar.setProgress(realAbility.cuePrecision / 100);

        String power = String.format("%s/%s",
                numToString(realAbility.normalPower),
                numToString(realAbility.maxPower));

        if (preview.normalPower != realAbility.normalPower) {
            power += " (" + numToString(preview.normalPower) + ")";
        }

        powerLabel.setText(power);
        powerBar.setProgress((realAbility.normalPower + realAbility.maxPower) / 200.0);

        String powerControl = numToString(realAbility.powerControl);
        if (preview.powerControl != realAbility.powerControl) {
            powerControl += " (" + numToString(preview.powerControl) + ")";
        }

        powerControlLabel.setText(powerControl);
        powerControlBar.setProgress(realAbility.powerControl / 100.0);

        String spin = numToString(realAbility.spin);
        if (preview.spin != realAbility.spin) {
            spin += " (" + numToString(preview.spin) + ")";
        }

        spinLabel.setText(spin);
        spinBar.setProgress(realAbility.spin / 100.0);

        String spinControl = numToString(realAbility.spinControl);
        if (preview.spinControl != realAbility.spinControl) {
            spinControl += " (" + numToString(preview.spinControl) + ")";
        }

        spinControlLabel.setText(spinControl);
        spinControlBar.setProgress(realAbility.spinControl / 100);
    }
}
