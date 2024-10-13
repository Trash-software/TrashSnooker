package trashsoftware.trashSnooker.fxml.widgets;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.res.ResourcesLoader;

import java.io.IOException;
import java.util.ResourceBundle;

public class AbilityShower extends VBox {

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
    //    @FXML
//    ColumnConstraints buttonsCol;
    @FXML
    GridPane barChartRoot;
    @FXML
    RadarChart radarChartRoot;
    @FXML
    Button switchButton;
    ImageView switchButtonGraphic;
    boolean showingRadar = false;

    private PlayerPerson.ReadableAbility ability;
    private PlayerPerson.ReadableAbilityHand primary;
    private PlayerPerson.ReadableAbility opponentAbi;

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

        switchButtonGraphic = new ImageView();
        switchButton.setGraphic(switchButtonGraphic);
        setSwitchButton();
    }

    public static String numToString(double d) {
        return d == (long) d ? String.format("%d", (long) d) : String.format("%.1f", d);
    }

    public void setup(PlayerPerson.ReadableAbility ability) {
        this.ability = ability;
        this.primary = ability.primary();

        setupTexts();
        setupRadar();
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

        PlayerPerson.ReadableAbility ability1 = ability;

        PlayerPerson.ReadableAbility ability2 = null;
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
            titles = new String[]{
                    strings.getString("aiming") + "\n" + numToString(ability1.aiming),
                    strings.getString("cuePrecision") + "\n" + numToString(primary.cuePrecision),
                    strings.getString("spinControlText") + "\n" + numToString(primary.spinControl),
                    strings.getString("powerControl") + "\n" + numToString(primary.powerControl),
                    strings.getString("power") + "\n" + numToString((primary.normalPower + primary.maxPower) / 2),
                    strings.getString("spinText") + "\n" + numToString(primary.spin),
                    strings.getString("offHand") + "\n" + numToString(ability1.getAnotherHandGoodness()),
                    strings.getString("restHand") + "\n" + numToString(ability1.getRestGoodness()),
            };
        }
//        else {
//            ability2 = perkManager.getShownAbility();
//            titles = new String[]{
//                    strings.getString("aiming") + "\n" + numToString(ability1.aiming),
//                    strings.getString("cuePrecision") + "\n" + numToString(ability1.cuePrecision),
//                    strings.getString("spinControlText") + "\n" + numToString(ability1.spinControl),
//                    strings.getString("powerControl") + "\n" + numToString(ability1.powerControl),
//                    strings.getString("power") + "\n" + numToString((ability1.normalPower + ability1.maxPower) / 2),
//                    strings.getString("spinText") + "\n" + numToString(ability1.spin),
//                    strings.getString("offHand") + "\n" + numToString(ability1.getAnotherHandGoodness()),
//                    strings.getString("restHand") + "\n" + numToString(ability1.getRestGoodness()),
//            };
//        }

        double[] valuesReal = getRadarValues(ability1);
        if (ability2 == null) {
            radarChartRoot.setValues(titles, valuesReal);
        } else {
            double[] valuesPre = getRadarValues(ability2);
            radarChartRoot.setValues(titles, valuesReal, valuesPre);
        }

        System.out.println(ability.antiHand());
    }

    private double[] getRadarValues(PlayerPerson.ReadableAbility ability) {
        PlayerPerson.ReadableAbilityHand primary = ability.primary();
        return new double[]{
                abilityRate(ability.aiming),
                abilityRate(primary.cuePrecision),
                abilityRate(primary.spinControl),
                abilityRate(primary.powerControl),
                abilityRate((primary.normalPower + primary.maxPower) / 2),
                abilityRate(primary.spin),
                abilityRate(ability.getAnotherHandGoodness()),
                abilityRate(ability.getRestGoodness()),
        };
    }

    private double abilityRate(double ability100) {
        return Math.min(Math.max((ability100 - 50) / 50, 0), 1);
    }

    private void setupTexts() {
//        PlayerPerson.ReadableAbility realAbility = perkManager.getOriginalAbility();
//        PlayerPerson.ReadableAbility preview = perkManager.getShownAbility();
        PlayerPerson.ReadableAbility realAbility = ability;
        PlayerPerson.ReadableAbilityHand primary = realAbility.primary();

        nameLabel.setText(realAbility.getShownName());
        categoryLabel.setText(PlayerPerson.getPlayerCategoryShown(realAbility.category, strings));

        sexLabel.setText(realAbility.getSex().toString());
        heightLabel.setText(String.format("%.0f cm", realAbility.getHandBody().height));

        String aiming = numToString(realAbility.aiming);
//        if (preview.aiming != realAbility.aiming) {
//            aiming += " (" + numToString(preview.aiming) + ")";
//        }

        aimingLabel.setText(aiming);
        aimingBar.setProgress(realAbility.aiming / 100.0);

        String cuePrecision = numToString(primary.cuePrecision);
//        if (preview.cuePrecision != realAbility.cuePrecision) {
//            cuePrecision += " (" + numToString(preview.cuePrecision) + ")";
//        }
        cuePrecisionLabel.setText(cuePrecision);
        cuePrecisionBar.setProgress(primary.cuePrecision / 100);

        String power = String.format("%s/%s",
                numToString(primary.normalPower),
                numToString(primary.maxPower));

//        if (preview.normalPower != realAbility.normalPower) {
//            power += " (" + numToString(preview.normalPower) + ")";
//        }

        powerLabel.setText(power);
        powerBar.setProgress((primary.normalPower + primary.maxPower) / 200.0);

        String powerControl = numToString(primary.powerControl);
//        if (preview.powerControl != realAbility.powerControl) {
//            powerControl += " (" + numToString(preview.powerControl) + ")";
//        }

        powerControlLabel.setText(powerControl);
        powerControlBar.setProgress(primary.powerControl / 100.0);

        String spin = numToString(primary.spin);
//        if (preview.spin != realAbility.spin) {
//            spin += " (" + numToString(preview.spin) + ")";
//        }

        spinLabel.setText(spin);
        spinBar.setProgress(primary.spin / 100.0);

        String spinControl = numToString(primary.spinControl);
//        if (preview.spinControl != realAbility.spinControl) {
//            spinControl += " (" + numToString(preview.spinControl) + ")";
//        }

        spinControlLabel.setText(spinControl);
        spinControlBar.setProgress(primary.spinControl / 100);

        String anotherHandGoodness = numToString(realAbility.getAnotherHandGoodness());
//        if (preview.getAnotherHandGoodness() != realAbility.getAnotherHandGoodness()) {
//            anotherHandGoodness += " (" + numToString(preview.getAnotherHandGoodness()) + ")";
//        }

        notGoodHandLabel.setText(anotherHandGoodness);
        notGoodHandBar.setProgress(realAbility.getAnotherHandGoodness() / 100);

        String restGoodness = numToString(realAbility.getRestGoodness());
//        if (preview.getRestGoodness() != realAbility.getRestGoodness()) {
//            restGoodness += " (" + numToString(preview.getRestGoodness()) + ")";
//        }

        restLabel.setText(restGoodness);
        restBar.setProgress(realAbility.getRestGoodness() / 100);
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

}
