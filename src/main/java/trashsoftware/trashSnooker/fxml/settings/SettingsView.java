package trashsoftware.trashSnooker.fxml.settings;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.ChildInitializable;
import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.fxml.alert.AlertShower;
import trashsoftware.trashSnooker.util.Util;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import java.net.URL;
import java.util.*;

public class SettingsView extends ChildInitializable {
    private final List<ComboBox<?>> allBoxes = new ArrayList<>();
    private final List<Slider> allSliders = new ArrayList<>();
    private final Map<ComboBox<?>, Integer> lastSavedSelections = new HashMap<>();
    private final Map<Slider, Double> lastSavedValues = new HashMap<>();
    @FXML
    Button backButton;
    @FXML
    ScrollPane contentContainer;

    @FXML
    Button confirmBtn;
    private Stage stage;
    private ConfigLoader configLoader;
    private ResourceBundle strings;

    GeneralPage generalPage;
    GamePage gamePage;
    DisplayPage displayPage;

    // In game preference only
    GameView gameView;
    Window standaloneWindow;

    private boolean forceEnableSaveBtn;  // 有些操作如改键位会实时保存, 但给用户一个安慰剂

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.strings = resources;

        generalPage = new GeneralPage(resources);
        generalPage.setParent(this);
        generalPage.setupItems(allBoxes, allSliders);

        gamePage = new GamePage(resources);
        gamePage.setParent(this);
        gamePage.setupItems(allBoxes, allSliders);

        displayPage = new DisplayPage(resources);
        displayPage.setParent(this);
        displayPage.setupItems(allBoxes, allSliders);

        configLoader = ConfigLoader.getInstance();

        generalPage.initSelections(configLoader);
        gamePage.initSelections(configLoader);
        displayPage.initSelections(configLoader);

        storeSelectionsToMap();
        addGeneralChangeListeners();
    }

    public void setAsInGamePreferences(GameView gameView, Window standaloneWindow) {
        this.gameView = gameView;
        this.standaloneWindow = standaloneWindow;

        standaloneWindow.setOnCloseRequest(e -> cancelAction());

        backButton.setManaged(false);
        backButton.setVisible(false);

        generalPage.languageBox.setDisable(true);

        displayPage.displayBox.setDisable(true);
        displayPage.resolutionComboBox.setDisable(true);
        displayPage.systemZoomComboBox.setDisable(true);
        displayPage.antiAliasingComboBox.setDisable(true);

        gamePage.aiStrengthBox.setDisable(true);
        gamePage.aimLingBox.setDisable(true);
    }

    void closeAsInGamePreferences() {
        standaloneWindow.hide();
    }

    private boolean anyHasChanged() {
        for (ComboBox<?> box : allBoxes) {
            if (hasChanged(box)) return true;
        }
        return false;
    }
    
    void forceEnableConfirmButton() {
        forceEnableSaveBtn = true;
        confirmBtn.setDisable(false);
    }

    private void addGeneralChangeListeners() {
        for (ComboBox<?> box : allBoxes) {
            box.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
                Integer lastSaved = lastSavedSelections.get(box);
                if (forceEnableSaveBtn) {
                    confirmBtn.setDisable(false);
                } else if (!Objects.equals(newValue, lastSaved)) {
                    confirmBtn.setDisable(false);
                } else if (!anyHasChanged()) {
                    confirmBtn.setDisable(true);
                }
            });
        }
        for (Slider slider : allSliders) {
            slider.valueProperty().addListener((observable, oldValue, newValue) -> {
                Double lastSaved = lastSavedValues.get(slider);
                if (forceEnableSaveBtn) {
                    confirmBtn.setDisable(false);
                } else if (!Objects.equals(newValue, lastSaved)) {
                    confirmBtn.setDisable(false);
                } else if (!anyHasChanged()) {
                    confirmBtn.setDisable(true);
                }
            });
        }
    }

    private void storeSelectionsToMap() {
        for (ComboBox<?> box : allBoxes) {
            lastSavedSelections.put(box, box.getSelectionModel().getSelectedIndex());
        }
        for (Slider slider : allSliders) {
            lastSavedValues.put(slider, slider.getValue());
        }
    }

    private boolean hasChanged(Control control) {
        if (control instanceof ComboBox<?> box) {
            return comboBoxHasChanged(box);
        } else if (control instanceof Slider slider) {
            return sliderHasChanged(slider);
        } else {
            throw new RuntimeException("Not supporting control type: " + control.getClass().getName());
        }
    }

    private boolean comboBoxHasChanged(ComboBox<?> box) {
        Integer lastIndex = lastSavedSelections.get(box);
        return box.getSelectionModel().getSelectedIndex() != lastIndex;  // null也是changed
    }

    private boolean sliderHasChanged(Slider slider) {
        Double lastValue = lastSavedValues.get(slider);
        return slider.getValue() != lastValue;  // null也是changed
    }

//    @Override
//    public Stage getStage() {
//        return stage;
//    }

    public void setup(Stage stage) {
        this.stage = stage;
    }

    public Stage getStage() {
        return stage;
    }

    @FXML
    void generalPageAction() {
        contentContainer.setContent(generalPage);
    }

    @FXML
    void gamePageAction() {
        contentContainer.setContent(gamePage);
    }

    @FXML
    void displayPageAction() {
        contentContainer.setContent(displayPage);
    }

    @FXML
    void confirmAction() {
        generalPage.saveIfChanged(this::hasChanged, configLoader);
        gamePage.saveIfChanged(this::hasChanged, configLoader);
        displayPage.saveIfChanged(this::hasChanged, configLoader);

        configLoader.save();

        if (gameView != null) {
            gameView.notifySettingsChanged();
            closeAsInGamePreferences();
        } else {
            super.backAction();
        }
    }

    @FXML
    void cancelAction() {
        if (anyHasChanged()) {
            AlertShower.askConfirmation(
                    stage,
                    strings.getString("confirmDiscardChanges"),
                    strings.getString("pleaseConfirm"),
                    () -> {
                        if (gameView == null) super.backAction();
                        else closeAsInGamePreferences();
                    },
                    null
            );
        } else {
            if (gameView == null) super.backAction();
            else closeAsInGamePreferences();
        }
    }

    @Override
    public void backAction() {
        cancelAction();
    }

    public enum YesNo {
        YES,
        NO;

        public static YesNo fromBoolean(Boolean value) {
            if (value == null || !value) {
                return NO;
            }
            return YES;
        }

        public boolean toBoolean() {
            return this == YES;
        }

        @Override
        public String toString() {
            String key = this == YES ? "yes" : "no";
            return App.getStrings().getString(key);
        }
    }
    
    public enum AIHelperDefense {
        ALLOW("aiHelperDefenseEnable"),
        ASK("aiHelperDefenseAsk"),
        NOT_ALLOW("aiHelperDefenseDisable");

        private final String stringKey;

        AIHelperDefense(String stringKey) {
            this.stringKey = stringKey;
        }

        @Override
        public String toString() {
            return App.getStrings().getString(stringKey);
        }

        public static AIHelperDefense fromKey(String key) {
            try {
                return valueOf(Util.toAllCapsUnderscoreCase(key));
            } catch (IllegalArgumentException e) {
                return ASK;
            }
        }

        public String toKey() {
            return Util.toLowerCamelCase(name());
        }
    }

    public enum MouseDragMethod {
        POSITION("mouseDragAbsolute"),
        MOVEMENT("mouseDragRelative");

        private final String stringKey;

        MouseDragMethod(String stringKey) {
            this.stringKey = stringKey;
        }

        @Override
        public String toString() {
            return App.getStrings().getString(stringKey);
        }

        public static MouseDragMethod fromKey(String key) {
            try {
                return valueOf(Util.toAllCapsUnderscoreCase(key));
            } catch (IllegalArgumentException e) {
                return MOVEMENT;
            }
        }

        public String toKey() {
            return Util.toLowerCamelCase(name());
        }
    }

    public static class LocaleName {
        public final Locale locale;

        public LocaleName(Locale locale) {
            this.locale = locale;
        }

        @Override
        public String toString() {
            return locale.getDisplayLanguage(locale);
        }
    }
}
