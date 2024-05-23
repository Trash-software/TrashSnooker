package trashsoftware.trashSnooker.fxml;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.ChampDataManager;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Util;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class NewCareerView extends ChildInitializable {
    @FXML
    Pane basePane;
    @FXML
    ComboBox<Hand> handBox;
    @FXML
    TextField nameField;
    @FXML
    ComboBox<PlayerPerson> existingPlayersBox;
    @FXML
    Button playerInfoBtn;
    @FXML
    Button createBtn;
    @FXML
    Label promptLabel;
    @FXML
    ComboBox<PlayerPerson.Sex> sexBox;
    @FXML
    ComboBox<Double> heightBox;
    @FXML
    ComboBox<Difficulty> aiGoodnessBox;
    @FXML
    ComboBox<Difficulty> playerGoodnessBox;
    @FXML
    CheckBox includeCustomPlayerBox;
    @FXML
    RadioButton createPlayerCheck, usePlayerCheck;
    @FXML
    GridPane createPlayerPane, usePlayerPane;
    @FXML
    ToggleGroup paneToggle;

    private EntryView entryView;
    private Stage owner;
    private ResourceBundle strings;

    public static void fillPlayerDifficulty(ComboBox<Difficulty> comboBox) {
        comboBox.getItems().addAll(
                new Difficulty("difEasiest", 3.0),
                new Difficulty("difEasy", 1.5),
                new Difficulty("difMedium", 1.0),
                new Difficulty("difHard", 0.75),
                new Difficulty("difExtreme", 0.0)
        );
        comboBox.getSelectionModel().select(2);
    }

    public static void fillAiDifficulty(ComboBox<Difficulty> comboBox) {
        comboBox.getItems().addAll(
                new Difficulty("aiGoodNoob", 0.15),
                new Difficulty("aiGoodBad", 0.4),
                new Difficulty("aiGoodNormal", 1.0),
                new Difficulty("aiGoodGood", 2.0),
                new Difficulty("aiGoodExtreme", 10.0)
        );
        comboBox.getSelectionModel().select(1);
    }

    public static int getGoodnessIndex(List<Difficulty> difficulties, double multiplier) {
        double first = difficulties.get(0).multiplier;
        double last = difficulties.get(difficulties.size() - 1).multiplier;

        int sign = first < last ? 1 : -1;  // 从小到大是1，从大到小是-1
        double mul = multiplier * sign;

        if (mul <= first * sign) return 0;
        if (mul >= last * sign) return difficulties.size() - 1;

        for (int index = 1; index < difficulties.size() - 1; index++) {
            double low = difficulties.get(index - 1).multiplier * sign;
            double mid = difficulties.get(index).multiplier * sign;
            double high = difficulties.get(index + 1).multiplier * sign;

            double tick1 = (low + mid) / 2;
            double tick2 = (mid + high) / 2;
            if (mul >= tick1 && mul < tick2) {
                return index;
            }
        }
        return difficulties.size() / 2;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;

        handBox.getItems().addAll(Hand.values());
        handBox.getSelectionModel().select(1);

        existingPlayersBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(PlayerPerson object) {
                return object == null ? null : object.getName();
            }

            @Override
            public PlayerPerson fromString(String string) {
                throw new RuntimeException("Should not convert string to player person");
            }
        });

        fillBox();
        setListeners();
        triggerPaneToggle();

        ChampDataManager.getInstance();
    }

//    @Override
//    public Stage getStage() {
//        return owner;
//    }

    public void setup(EntryView entryView, Stage owner) {
        this.entryView = entryView;
        this.owner = owner;
    }

    private void fillBox() {
        existingPlayersBox.getItems().addAll(DataLoader.getInstance().getActualPlayers());
        existingPlayersBox.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) ->
                playerInfoBtn.setDisable(newValue == null)));

        sexBox.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue != null) {
                heightValues(newValue.minHeight, newValue.maxHeight, newValue.stdHeight);
            }
        }));
        sexBox.getItems().addAll(PlayerPerson.Sex.values());
        sexBox.getSelectionModel().select(0);

        fillPlayerDifficulty(playerGoodnessBox);
        fillAiDifficulty(aiGoodnessBox);
    }

    private void setListeners() {
        paneToggle.selectedToggleProperty().addListener((observable, oldValue, newValue) ->
                triggerPaneToggle());
    }

    private void triggerPaneToggle() {
        if (createPlayerCheck.isSelected()) {
            createPlayerPane.setDisable(false);
            usePlayerPane.setDisable(true);
        } else {
            createPlayerPane.setDisable(true);
            usePlayerPane.setDisable(false);
        }
    }

    private void heightValues(double from, double to, double select) {
        heightBox.getItems().clear();
        for (double i = from; i <= to; i += 1) {
            heightBox.getItems().add(i);
        }

        heightBox.getSelectionModel().select(select);
    }

    @FXML
    public void createCareerAction() {
        if (createPlayerCheck.isSelected()) {
            createPlayer();
        } else {
            usePlayer();
        }
    }

    private void createPlayer() {
        String name = nameField.getText();
        if (name.isBlank()) {
            promptLabel.setText(strings.getString("pleaseInputName"));
            return;
        }

        String generatedId;
        do {
            generatedId = DataLoader.generateIdByName(name);
        } while (DataLoader.getInstance().hasPlayer(generatedId));

        promptLabel.setText("");

        boolean leftHanded = handBox.getValue() == Hand.LEFT;

        PlayerPerson playerPerson = PlayerPerson.randomPlayer(
                generatedId,
                name,
                leftHanded,
                70.0,
                80.0,
                true,
                heightBox.getValue(),
                sexBox.getValue()
        );

        DataLoader.getInstance().addPlayerPerson(playerPerson);
        createCareerInfo(playerPerson);
    }

    private void usePlayer() {
        PlayerPerson person = existingPlayersBox.getValue();
        if (person == null) {
            promptLabel.setText(strings.getString("pleaseSelectAPlayer"));
            return;
        }
        promptLabel.setText("");
        createCareerInfo(person);
    }

    @FXML
    public void playerInfoAction() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("abilityView.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            // 这里还是让ability当弹窗
            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);

            Scene scene = App.createScene(root);
            stage.setScene(scene);

            stage.show();

            AbilityView controller = loader.getController();
            controller.setup(scene, existingPlayersBox.getValue());
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }

    private void launchNext() {
//        thisStage.close();
        entryView.refreshGui();
        entryView.startCareerView(owner);
    }

    private void createCareerInfo(PlayerPerson person) {
        assert person != null;
        Service<Void> service = new Service<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() {
                        CareerManager.createNew(person,
                                playerGoodnessBox.getValue().multiplier,
                                aiGoodnessBox.getValue().multiplier,
                                includeCustomPlayerBox.isSelected());
                        System.out.println("Start simulating");
                        long st = System.currentTimeMillis();
                        CareerManager.getInstance().simulateMatchesInPastTwoYears();
                        AchManager.newCareerInstance(CareerManager.getInstance().getCareerSave());

                        // 一定要在simulateMatches之后
                        System.out.println("Simulation ends in " + (System.currentTimeMillis() - st) + " ms");
                        return null;
                    }
                };
            }
        };
        service.setOnSucceeded(event -> launchNext());
        service.setOnFailed(event -> EventLogger.error(event.getSource().getException()));

        basePane.setDisable(true);
        promptLabel.setText(strings.getString("initializingCareer"));
//        createBtn.setDisable(true);

        service.start();
    }

    enum Hand {
        LEFT,
        RIGHT;

        @Override
        public String toString() {
            return App.getStrings().getString(Util.toLowerCamelCase(name()));
        }
    }

    public static class Difficulty {
        double multiplier;
        String key;

        Difficulty(String shown, double multiplier) {
            this.multiplier = multiplier;
            this.key = shown;
        }

        @Override
        public String toString() {
            return App.getStrings().getString(key);
        }
    }
}
