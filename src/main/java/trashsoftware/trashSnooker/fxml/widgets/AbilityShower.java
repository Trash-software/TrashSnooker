package trashsoftware.trashSnooker.fxml.widgets;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.fxml.App;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class AbilityShower extends GridPane {
    private final Map<Button, Combo> btnMap = new HashMap<>();
    @FXML
    Label nameLabel, categoryLabel;
    @FXML
    Label aimingLabel, cuePrecisionLabel, powerLabel, spinLabel, powerControlLabel,
            spinControlLabel, notGoodHandLabel, restLabel;
    @FXML
    ProgressBar aimingBar, cuePrecisionBar, powerBar, spinBar, powerControlBar,
            spinControlBar, notGoodHandBar, restBar;
    @FXML
    Button aimingBtn, cuePrecisionBtn, powerBtn, spinBtn, powerControlBtn,
            spinControlBtn, notGoodHandBtn, restBtn;
    @FXML
    ColumnConstraints buttonsCol;
    private final Button[] buttons;
//    private PlayerPerson.ReadableAbility ability;
    private PerkManager perkManager;
    
    private ResourceBundle strings;
    
    public AbilityShower() {
        this(App.getStrings());
    }

    public AbilityShower(ResourceBundle strings) {
        super();
        
        this.strings = strings;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "abilityShower.fxml"), strings);
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        buttons = new Button[]{
                aimingBtn, cuePrecisionBtn, powerBtn, spinBtn, powerControlBtn,
                spinControlBtn, notGoodHandBtn, restBtn
        };
        btnMap.putAll(Map.of(
                aimingBtn, new Combo(PerkManager.AIMING, aimingLabel),
                cuePrecisionBtn, new Combo(PerkManager.CUE_PRECISION, cuePrecisionLabel),
                powerBtn, new Combo(PerkManager.POWER, powerLabel),
                powerControlBtn, new Combo(PerkManager.POWER_CONTROL, powerControlLabel),
                spinBtn, new Combo(PerkManager.SPIN, spinLabel),
                spinControlBtn, new Combo(PerkManager.SPIN_CONTROL, spinControlLabel),
                notGoodHandBtn, new Combo(PerkManager.ANTI_HAND, notGoodHandLabel),
                restBtn, new Combo(PerkManager.REST, restLabel)
        ));
    }

    public static String numToString(double d) {
        return d == (long) d ? String.format("%d", (long) d) : String.format("%.1f", d);
    }

//    public void setPerkManager(PerkManager perkManager) {
//        this.perkManager = perkManager;
//        perkManager.setAbility(ability);
//
//        noticePerksReset();
//    }

    public void setup(PerkManager perkManager, boolean showAddButtons) {
        this.perkManager = perkManager;
        
//        this.ability = PlayerPerson.ReadableAbility.fromPlayerPerson(pp);

        if (!showAddButtons) {
            buttonsCol.setPrefWidth(0);
            buttonsCol.setMaxWidth(0);

            for (Button button : buttons) {
                button.setVisible(false);
                button.setManaged(false);
            }
        } else {
            notifyPerksReset();
        }
        
        setupTexts();
    }
    
    private void setupTexts() {
        PlayerPerson.ReadableAbility realAbility = perkManager.getOriginalAbility();
        PlayerPerson.ReadableAbility preview = perkManager.getShownAbility();
        
        nameLabel.setText(realAbility.getName());

        if ("Professional".equals(realAbility.category)) {
            categoryLabel.setText(strings.getString("catProf"));
        } else if ("Amateur".equals(realAbility.category)) {
            categoryLabel.setText(strings.getString("catAmateur"));
        } else if ("Noob".equals(realAbility.category)) {
            categoryLabel.setText(strings.getString("catNoob"));
        } else if ("God".equals(realAbility.category)) {
            categoryLabel.setText(strings.getString("catGod"));
        }
        
        String aiming = numToString(realAbility.aiming);
        if (preview.aiming != realAbility.aiming) {
            aiming += " (" + numToString(preview.aiming) + ")";
        }
        
        aimingLabel.setText(aiming);
        aimingBar.setProgress(realAbility.aiming / 100.0);

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

        String anotherHandGoodness = numToString(realAbility.getAnotherHandGoodness());
        if (preview.getAnotherHandGoodness() != realAbility.getAnotherHandGoodness()) {
            anotherHandGoodness += " (" + numToString(preview.getAnotherHandGoodness()) + ")";
        }

        notGoodHandLabel.setText(anotherHandGoodness);
        notGoodHandBar.setProgress(realAbility.getAnotherHandGoodness() / 100);

        String restGoodness = numToString(realAbility.getRestGoodness());
        if (preview.getRestGoodness() != realAbility.getRestGoodness()) {
            restGoodness += " (" + numToString(preview.getRestGoodness()) + ")";
        }

        restLabel.setText(restGoodness);
        restBar.setProgress(realAbility.getRestGoodness() / 100);
    }

    @FXML
    public void addPerk(ActionEvent event) {
        Button src = (Button) event.getSource();

        Combo combo = btnMap.get(src);
        int added = perkManager.addPerkTo(combo.cat);
        src.setText(added + "+");
        
        setupTexts();
        
//        Label label = combo.label;
//        String orig = label.getText().split("\\+")[0];
//        label.setText(orig + "+" + PlayerPerson.ReadableAbility.addPerksHowMany(combo.cat, added));
        
        if (perkManager.getAvailPerks() <= 0) {
            for (Button button : buttons) {
                button.setDisable(true);
            }
        }
    }

    public void notifyPerksReset() {
        if (perkManager.getAvailPerks() <= 0) {
            for (Button button : buttons) {
                // todo: 加满了就不准加了
                button.setDisable(true);
                button.setText("+");
            }
        } else {
            for (Button button : buttons) {
                button.setDisable(false);
                button.setText("+");
            }
        }
        setupTexts();
    }

    private static class Combo {
        int cat;
        Label label;

        Combo(int cat, Label label) {
            this.cat = cat;
            this.label = label;
        }
    }
}
