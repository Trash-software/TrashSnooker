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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
    private Button[] buttons;
    private PlayerPerson.ReadableAbility ability;
    private PerkManager perkManager;

    public AbilityShower() {
        super();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "abilityShower.fxml"));
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

    public void setPerkManager(PerkManager perkManager) {
        this.perkManager = perkManager;
        perkManager.setAbility(ability);

        noticePerksReset();
    }

    public void setup(PlayerPerson pp, boolean showAddButtons) {
        this.ability = PlayerPerson.ReadableAbility.fromPlayerPerson(pp);

        if (!showAddButtons) {
            buttonsCol.setPrefWidth(0);
            buttonsCol.setMaxWidth(0);

            for (Button button : buttons) {
                button.setVisible(false);
                button.setManaged(false);
            }
        }

        nameLabel.setText(pp.getName());

        if ("Professional".equals(ability.category)) {
            categoryLabel.setText("职业选手");
        } else if ("Amateur".equals(ability.category)) {
            categoryLabel.setText("业余选手");
        } else if ("Noob".equals(ability.category)) {
            categoryLabel.setText("菜鸡");
        } else if ("God".equals(ability.category)) {
            categoryLabel.setText("神");
        }
        
        setupTexts();
    }
    
    private void setupTexts() {
        aimingLabel.setText(numToString(ability.aiming));
        aimingBar.setProgress(ability.aiming / 100.0);

        cuePrecisionLabel.setText(numToString(ability.cuePrecision));
        cuePrecisionBar.setProgress(ability.cuePrecision / 100);

        powerLabel.setText(String.format("%s/%s",
                numToString(ability.normalPower),
                numToString(ability.maxPower)));
        powerBar.setProgress(ability.normalPower / 100.0);

        powerControlLabel.setText(numToString(ability.powerControl));
        powerControlBar.setProgress(ability.powerControl / 100.0);

        spinLabel.setText(numToString(ability.spin));
        spinBar.setProgress(ability.spin / 100.0);

        spinControlLabel.setText(numToString(ability.spinControl));
        spinControlBar.setProgress(ability.spinControl / 100);

        notGoodHandLabel.setText(numToString(ability.getAnotherHandGoodness()));
        notGoodHandBar.setProgress(ability.getAnotherHandGoodness() / 100);

        restLabel.setText(numToString(ability.getRestGoodness()));
        restBar.setProgress(ability.getRestGoodness() / 100);
    }

    @FXML
    public void addPerk(ActionEvent event) {
        Button src = (Button) event.getSource();

        Combo combo = btnMap.get(src);
        int added = perkManager.addPerkTo(combo.cat);
        src.setText(added + "+");
        
        Label label = combo.label;
        String orig = label.getText().split("\\+")[0];
        label.setText(orig + "+" + PlayerPerson.ReadableAbility.addPerksHowMany(combo.cat, added));
        
        if (perkManager.getAvailPerks() <= 0) {
            for (Button button : buttons) {
                button.setDisable(true);
            }
        }
    }

    public void noticePerksReset() {
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
