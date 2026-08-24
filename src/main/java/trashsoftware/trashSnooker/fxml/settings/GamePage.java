package trashsoftware.trashSnooker.fxml.settings;

import javafx.beans.value.ObservableValueBase;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.alert.AlertShower;
import trashsoftware.trashSnooker.util.config.ConfigLoader;
import trashsoftware.trashSnooker.util.config.InputManager;
import trashsoftware.trashSnooker.util.config.KeyBehavior;

import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Function;

public class GamePage extends AbsSettingsPage {
    @FXML
    ComboBox<Double> aimLingBox, aiStrengthBox;
    @FXML
    ComboBox<SettingsView.YesNo> autoChangeBreakCueBox;
    @FXML
    ComboBox<SettingsView.AIHelperDefense> aiHelperDefenseBox;
    @FXML
    ComboBox<SettingsView.YesNo> aiVsAiAutoNextFrameBox;
    @FXML
    ComboBox<SettingsView.MouseDragMethod> mouseDragMethodBox;
    @FXML
    TableView<KeyMap> keyMapTable;
    @FXML
    TableColumn<KeyMap, String> behaviorColumn;
    @FXML
    TableColumn<KeyMap, String> keyColumn;

    InputManager inputManager;

    public GamePage() {
        this(App.getStrings());
    }

    public GamePage(ResourceBundle strings) {
        super("gamePage.fxml", strings);
    }

    @Override
    public void setupItems(List<ComboBox<?>> allBoxes, List<Slider> allSliders) {
        allBoxes.addAll(List.of(aimLingBox,
                aiStrengthBox,
                autoChangeBreakCueBox,
                aiHelperDefenseBox,
                aiVsAiAutoNextFrameBox,
                mouseDragMethodBox));
    }

    @Override
    public void initSelections(ConfigLoader configLoader) {
        inputManager = configLoader.getInputManager();

        aimLingBox.getItems().addAll(
                0.0, 0.15, 0.4, 0.75, 1.0, 1.5, 2.0, 3.0, 5.0, 10.0
        );
        aiStrengthBox.getItems().addAll(
                0.15, 0.4, 0.75, 1.0, 1.5, 2.0, 3.0, 5.0, 10.0
        );
        aimLingBox.getSelectionModel().select(configLoader.getDouble("fastGameAiming", 1.0));
        aiStrengthBox.getSelectionModel().select(configLoader.getDouble("fastGameAiStrength", 1.0));

        autoChangeBreakCueBox.getItems().addAll(SettingsView.YesNo.values());
        autoChangeBreakCueBox.getSelectionModel().select(SettingsView.YesNo.fromBoolean(
                configLoader.getBoolean("autoChangeBreakCue", false)
        ));

        aiHelperDefenseBox.getItems().addAll(SettingsView.AIHelperDefense.values());
        aiHelperDefenseBox.getSelectionModel().select(SettingsView.AIHelperDefense.fromKey(
                configLoader.getString("aiHelperDefense", "ask")
        ));
        
        aiVsAiAutoNextFrameBox.getItems().addAll(SettingsView.YesNo.values());
        aiVsAiAutoNextFrameBox.getSelectionModel().select(SettingsView.YesNo.fromBoolean(
                configLoader.getBoolean(ConfigLoader.KEY_AI_AUTO_NEXT_FRAME, false)
        ));

        mouseDragMethodBox.getItems().addAll(SettingsView.MouseDragMethod.values());
        mouseDragMethodBox.getSelectionModel().select(SettingsView.MouseDragMethod.fromKey(
                configLoader.getString("mouseDragMethod", "movement")
        ));

        setupKeyMapTable();
    }

    @Override
    public void saveIfChanged(Function<Control, Boolean> hasChanged, ConfigLoader configLoader) {
        if (hasChanged.apply(aimLingBox)) {
            configLoader.put("fastGameAiming", aimLingBox.getSelectionModel().getSelectedItem());
        }
        if (hasChanged.apply(aiStrengthBox)) {
            configLoader.put("fastGameAiStrength", aiStrengthBox.getSelectionModel().getSelectedItem());
        }
        if (hasChanged.apply(autoChangeBreakCueBox)) {
            configLoader.put("autoChangeBreakCue", autoChangeBreakCueBox.getSelectionModel().getSelectedItem().toBoolean());
        }
        if (hasChanged.apply(aiHelperDefenseBox)) {
            configLoader.put("aiHelperDefense", aiHelperDefenseBox.getSelectionModel().getSelectedItem().toKey());
        }
        if (hasChanged.apply(aiVsAiAutoNextFrameBox)) {
            configLoader.put(ConfigLoader.KEY_AI_AUTO_NEXT_FRAME, aiVsAiAutoNextFrameBox.getSelectionModel().getSelectedItem().toBoolean());
        }
        if (hasChanged.apply(mouseDragMethodBox)) {
            configLoader.put("mouseDragMethod", mouseDragMethodBox.getSelectionModel().getSelectedItem().toKey());
        }
    }

    private void setupKeyMapTable() {
        behaviorColumn.setCellValueFactory(param -> {
            if (param == null) return null;
            return new ObservableValueBase<>() {
                @Override
                public String getValue() {
                    return param.getValue().keyBehavior.getShown(strings);
                }
            };
        });

        keyColumn.setCellValueFactory(param -> {
            if (param == null || param.getValue() == null) return null;
            return new ObservableValueBase<>() {
                @Override
                public String getValue() {
                    return InputManager.keyCodeShown(param.getValue().key);
                }
            };
        });

        keyMapTable.setRowFactory(tv -> {
            TableRow<KeyMap> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty()) {
                    showKeyInputWindow(row.getItem());
                }
            });
            return row;
        });

        refreshKeyMapTable();
    }

    private void refreshKeyMapTable() {
        keyMapTable.getItems().clear();
        for (KeyBehavior beh : KeyBehavior.values()) {
            KeyCode keyCode = inputManager.getKeyCode(beh);
            keyMapTable.getItems().add(new KeyMap(beh, keyCode));
        }
    }

    private void showKeyInputWindow(KeyMap selected) {
        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER);
        vBox.setSpacing(10.0);

        Label hint = new Label(strings.getString("keyMapInputHint"));
        hint.setTextAlignment(TextAlignment.CENTER);

        Label behavior = new Label(selected.keyBehavior.getShown(strings));
        behavior.setTextAlignment(TextAlignment.CENTER);

        vBox.setPadding(new Insets(5, 5, 5, 5));

        vBox.getChildren().addAll(hint, behavior);

        Stage window = new Stage(StageStyle.UTILITY);
        window.initModality(Modality.WINDOW_MODAL);
        window.initOwner(parent.getStage());

        Scene scene = new Scene(vBox);
        window.setScene(scene);

        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            System.out.printf("%s %s %s\n", code.name(), code.getName(), code.getChar());
            inputManager.setKeyCode(selected.keyBehavior, code);
            inputManager.save();
            parent.forceEnableConfirmButton();
            window.close();
            refreshKeyMapTable();
        });

        window.showAndWait();
    }

    @FXML
    void resetKeyMapAction() {
        AlertShower.askConfirmation(parent.getStage(),
                strings.getString("confirmResetKeyMaps"),
                strings.getString("resetKeyMap"),
                () -> {
                    inputManager.reset();
                    inputManager.save();
                    refreshKeyMapTable();
                },
                null);
    }

    public static class KeyMap {
        public final KeyBehavior keyBehavior;
        public KeyCode key;

        public KeyMap(KeyBehavior keyBehavior, KeyCode key) {
            this.keyBehavior = keyBehavior;
            this.key = key;
        }
    }
}
