package trashsoftware.trashSnooker.fxml.widgets;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import trashsoftware.trashSnooker.core.PlayerHand;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.res.ResourcesLoader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import static trashsoftware.trashSnooker.fxml.widgets.AbilityShower.numToString;

public class PerkAdder extends VBox {

    @FXML
    VBox selfBox;
    @FXML
    Rectangle colorRect;
    @FXML
    GridPane barChartRoot;
    @FXML
    Label nameLabel, categoryLabel, sexLabel, heightLabel;
    @FXML
    Label aimingLabel, cuePrecisionLabel, powerLabel, spinLabel, powerControlLabel,
            spinControlLabel;
    @FXML
    ProgressBar aimingBar, cuePrecisionBar, powerBar, spinBar, powerControlBar,
            spinControlBar;
    @FXML
    Button aimingBtn, cuePrecisionBtn, powerBtn, spinBtn, powerControlBtn,
            spinControlBtn;
    @FXML
    HBox extraField;
    @FXML
    ColumnConstraints buttonsCol;

    @FXML
    Label currentHandLabel;
    @FXML
    Button prevHandBtn, nextHandBtn;

    private int handIndex;

    private final Map<Button, Combo> btnMap = new HashMap<>();
    private final Button[] buttons;
    private final Button[] handButtons;
    //    private PlayerPerson.ReadableAbility ability;
    private PerkManager perkManager;
//    private Map<PlayerHand.Hand, PerkAdderHandPage> pagesMap;

    private final ResourceBundle strings;
    private final ResourcesLoader rl = ResourcesLoader.getInstance();

    public PerkAdder() {
        this(App.getStrings());
    }

    public PerkAdder(ResourceBundle strings) {
        super();

        this.strings = strings;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "perkAdder.fxml"), strings);
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        buttons = new Button[]{
                aimingBtn,
        };
        handButtons = new Button[]{
                cuePrecisionBtn, powerBtn, spinBtn, powerControlBtn,
                spinControlBtn
        };
        btnMap.putAll(Map.of(
                aimingBtn, new Combo(PerkManager.AIMING, aimingLabel),
                cuePrecisionBtn, new PerkAdder.Combo(PerkManager.CUE_PRECISION, cuePrecisionLabel),
                powerBtn, new PerkAdder.Combo(PerkManager.POWER, powerLabel),
                powerControlBtn, new PerkAdder.Combo(PerkManager.POWER_CONTROL, powerControlLabel),
                spinBtn, new PerkAdder.Combo(PerkManager.SPIN, spinLabel),
                spinControlBtn, new PerkAdder.Combo(PerkManager.SPIN_CONTROL, spinControlLabel)
        ));
        
//        setSpinner();

//        switchButtonGraphic = new ImageView();
//        switchButton.setGraphic(switchButtonGraphic);
//        setSwitchButton();
    }

    public void setup(PerkManager perkManager, boolean showAddButtons) {
        this.perkManager = perkManager;

        if (!showAddButtons) {
            buttonsCol.setPrefWidth(0);
            buttonsCol.setMaxWidth(0);

            for (Button button : buttons) {
                button.setVisible(false);
                button.setManaged(false);
            }
            for (Button button : handButtons) {
                button.setVisible(false);
                button.setManaged(false);
            }
        } else {
            notifyPerksReset();
        }
        
        PlayerPerson.ReadableAbilityHand rah = perkManager.getOriginalAbility().primary();
        setByHand(rah.hand);

//        pagesMap = new HashMap<>();
//        addHandPage(PlayerHand.Hand.LEFT, showAddButtons);
//        addHandPage(PlayerHand.Hand.RIGHT, showAddButtons);
//        addHandPage(PlayerHand.Hand.REST, showAddButtons);

//        setSpinner();
    }

//    private void addHandPage(PlayerHand.Hand hand, boolean showAddButtons) {
//        PerkAdderHandPage page = new PerkAdderHandPage(strings);
//        page.setup(perkManager, hand, showAddButtons);
//        pagesMap.put(hand, page);
//    }

    public void setExtraField(Node node) {
        extraField.getChildren().clear();
        extraField.getChildren().add(node);
    }
    
    void setByHand(PlayerHand.Hand hand) {
        handIndex = hand.ordinal();
        loadHand();
    }
    
    void loadHand() {
        PlayerHand.Hand hand = getHand();
        currentHandLabel.setText(hand.shownName(strings));
        
        setValuesHand();
    }

    @FXML
    public void prevHandAction() {
        if (handIndex == 0) {
            return;
        }
        
        handIndex -= 1;
        nextHandBtn.setDisable(false);
        
        if (handIndex == 0) {
            prevHandBtn.setDisable(true);
        }
        
        loadHand();
    }
    
    @FXML
    public void nextHandAction() {
        if (handIndex == 2) return;

        handIndex += 1;
        prevHandBtn.setDisable(false);

        if (handIndex == 2) {
            nextHandBtn.setDisable(true);
        }

        loadHand();
    }

    @FXML
    public void addPerk(ActionEvent event) {
        Button src = (Button) event.getSource();

        PlayerHand.Hand hand = getHand();

        Combo combo = btnMap.get(src);
        int added = perkManager.addPerkTo(combo.cat, hand);
        src.setText(added + "+");

//        double afterAdd = perkManager.getShownAbility().getAbilityByCat(combo.cat);
        double afterAdd;
        if (perkManager.isGeneral(combo.cat)) {
            afterAdd = perkManager.getShownAbility().getAbilityByCat(combo.cat);
        } else {
            afterAdd = perkManager.getShownOf(hand).getAbilityByCat(combo.cat);
        }
        double max = perkManager.getShownAbility().maxAbilityByCat(combo.cat);
        if (afterAdd >= max * 0.999) src.setDisable(true);

        setValuesBasic();
        setValuesHand();
//        setupRadar();

        if (perkManager.getAvailPerks() <= 0) {
            for (Button button : buttons) {
                button.setDisable(true);
            }
            for (Button button : handButtons) {
                button.setDisable(true);
            }
        }
    }

    public void notifyPerksReset() {
        if (perkManager.getAvailPerks() <= 0) {
            for (Button button : buttons) {
                button.setDisable(true);
                button.setText("+");
            }
            for (Button button : handButtons) {
                button.setDisable(true);
                button.setText("+");
            }
        } else {
            for (Button button : buttons) {
                Combo combo = btnMap.get(button);
                double curPerk = perkManager.getShownAbility().getAbilityByCat(combo.cat);
                double max = perkManager.getShownAbility().maxAbilityByCat(combo.cat);
                button.setDisable(curPerk >= max * 0.999);
                button.setText("+");
            }
            PlayerHand.Hand hand = getHand();
            for (Button button : handButtons) {
                Combo combo = btnMap.get(button);
                double curPerk = perkManager.getShownOf(hand).getAbilityByCat(combo.cat);
                double max = perkManager.getShownAbility().maxAbilityByCat(combo.cat);
                button.setDisable(curPerk >= max * 0.999);
                button.setText("+");
            }
        }
//        setupRadar();
        setValuesBasic();
        setValuesHand();
    }

    private void setValuesBasic() {
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
    }
    
    private PlayerHand.Hand getHand() {
        return PlayerHand.Hand.values()[handIndex];
    }
    
    private void setValuesHand() {
        PlayerHand.Hand hand = getHand();
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

    static class Combo {
        int cat;
        Label label;

        Combo(int cat, Label label) {
            this.cat = cat;
            this.label = label;
        }
    }

//    private class HandPerks {
//        PlayerHand.Hand hand;
//        PerkAdderHandPage page;
//        
//        HandPerks(PlayerHand.Hand hand, PerkAdderHandPage page) {
//            this.hand = hand;
//            this.page = page;
//        }
//        
//        
//    }
}
