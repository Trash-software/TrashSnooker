package trashsoftware.trashSnooker.fxml.widgets;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.res.ResourcesLoader;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class AbilityShower extends VBox {
    private final Map<Button, Combo> btnMap = new HashMap<>();
    @FXML
    VBox selfBox, opponentBox;
    @FXML
    Label nameLabel, categoryLabel, sexLabel, heightLabel;
    @FXML
    Label nameLabel2, categoryLabel2, sexLabel2, heightLabel2;
    @FXML
    Rectangle colorRect, colorRect2;
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
    @FXML
    GridPane barChartRoot;
    @FXML
    RadarChart radarChartRoot;
    @FXML
    HBox extraField;
    @FXML
    Button switchButton;
    ImageView switchButtonGraphic;
    boolean showingRadar = false;

    private PlayerPerson.ReadableAbility opponentAbi;

    private final Button[] buttons;
    //    private PlayerPerson.ReadableAbility ability;
    private PerkManager perkManager;

    private final ResourceBundle strings;
    private final ResourcesLoader rl = ResourcesLoader.getInstance();

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
        
        switchButtonGraphic = new ImageView();
        switchButton.setGraphic(switchButtonGraphic);
        setSwitchButton();
    }

    public static String numToString(double d) {
        return d == (long) d ? String.format("%d", (long) d) : String.format("%.1f", d);
    }

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
        setupRadar();
    }
    
    public void setExtraField(Node node) {
        extraField.getChildren().clear();
        extraField.getChildren().add(node);
    }
    
    public void setOpponent(PlayerPerson.ReadableAbility opponentAbi) {
        this.opponentAbi = opponentAbi;

        nameLabel2.setText(opponentAbi.getShownName());
        categoryLabel2.setText(PlayerPerson.getPlayerCategoryShown(opponentAbi.category, strings));

        sexLabel2.setText(opponentAbi.getSex().toString());
        heightLabel2.setText(String.format("%.0f cm", opponentAbi.getHandBody().height));
        
        setupRadar();
    }

    private void setupRadar() {
        colorRect.setFill(RadarChart.LINES[0]);
        colorRect2.setFill(RadarChart.LINES[1]);
        
        PlayerPerson.ReadableAbility ability1 = perkManager.getOriginalAbility();

        PlayerPerson.ReadableAbility ability2;
        String[] titles;
        if (opponentAbi != null) {
            ability2 = opponentAbi;
            titles = new String[]{
                    strings.getString("aiming"),
                    strings.getString("cuePrecision"),
                    strings.getString("spinControlText"),
                    strings.getString("powerControl"),
                    strings.getString("power"),
                    strings.getString("spinText"),
                    strings.getString("offHand"),
                    strings.getString("restHand"),
            };
        } else {
            ability2 = perkManager.getShownAbility();
            titles = new String[]{
                    strings.getString("aiming") + "\n" + numToString(ability1.aiming),
                    strings.getString("cuePrecision") + "\n" + numToString(ability1.cuePrecision),
                    strings.getString("spinControlText") + "\n" + numToString(ability1.spinControl),
                    strings.getString("powerControl") + "\n" + numToString(ability1.powerControl),
                    strings.getString("power") + "\n" + numToString((ability1.normalPower + ability1.maxPower) / 2),
                    strings.getString("spinText") + "\n" + numToString(ability1.spin),
                    strings.getString("offHand") + "\n" + numToString(ability1.getAnotherHandGoodness()),
                    strings.getString("restHand") + "\n" + numToString(ability1.getRestGoodness()),
            };
        }

        double[] valuesReal = getRadarValues(ability1);
        double[] valuesPre = getRadarValues(ability2);

        if (Arrays.equals(valuesReal, valuesPre)) {
            radarChartRoot.setValues(titles, valuesReal);
        } else {
            radarChartRoot.setValues(titles, valuesReal, valuesPre);
        }
    }
    
    private double[] getRadarValues(PlayerPerson.ReadableAbility ability) {
        return new double[]{
                abilityRate(ability.aiming),
                abilityRate(ability.cuePrecision),
                abilityRate(ability.spinControl),
                abilityRate(ability.powerControl),
                abilityRate((ability.normalPower + ability.maxPower) / 2),
                abilityRate(ability.spin),
                abilityRate(ability.getAnotherHandGoodness()),
                abilityRate(ability.getRestGoodness()),
        };
    }

    private double abilityRate(double ability100) {
        return Math.min(Math.max((ability100 - 50) / 50, 0), 1);
    }

    private void setupTexts() {
        PlayerPerson.ReadableAbility realAbility = perkManager.getOriginalAbility();
        PlayerPerson.ReadableAbility preview = perkManager.getShownAbility();

        nameLabel.setText(realAbility.getShownName());
        categoryLabel.setText(PlayerPerson.getPlayerCategoryShown(realAbility.category, strings));

        sexLabel.setText(realAbility.getSex().toString());
        heightLabel.setText(String.format("%.0f cm", realAbility.getHandBody().height));

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

        double afterAdd = perkManager.getShownAbility().getAbilityByCat(combo.cat);
        if (afterAdd >= 99.95) src.setDisable(true);

        setupTexts();
        setupRadar();

        if (perkManager.getAvailPerks() <= 0) {
            for (Button button : buttons) {
                button.setDisable(true);
            }
        }
    }

    @FXML
    void switchChart() {
        showingRadar = !showingRadar;

        radarChartRoot.setVisible(showingRadar);
        radarChartRoot.setManaged(showingRadar);
        barChartRoot.setVisible(!showingRadar);
        barChartRoot.setManaged(!showingRadar);

        boolean showingComparison = showingRadar && opponentAbi != null;
        opponentBox.setVisible(showingComparison);
        opponentBox.setManaged(showingComparison);
        colorRect.setVisible(showingComparison);
        colorRect2.setVisible(showingComparison);
        
        setSwitchButton();
    }
    
    private void setSwitchButton() {
        if (showingRadar) {
            rl.setIconImage1x1(rl.getBarIcon(), switchButtonGraphic, 1.25);
        } else {
            rl.setIconImage1x1(rl.getRadarIcon(), switchButtonGraphic, 1.25);
        }
    }

    public void notifyPerksReset() {
        if (perkManager.getAvailPerks() <= 0) {
            for (Button button : buttons) {
                button.setDisable(true);
                button.setText("+");
            }
        } else {
            for (Button button : buttons) {
                Combo combo = btnMap.get(button);
                double curPerk = perkManager.getShownAbility().getAbilityByCat(combo.cat);
                button.setDisable(curPerk >= 99.95);
                button.setText("+");
            }
        }
        setupRadar();
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
