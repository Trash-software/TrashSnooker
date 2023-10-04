package trashsoftware.trashSnooker.fxml;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.ai.AiCueResult;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.metrics.*;
import trashsoftware.trashSnooker.core.phy.TableCloth;
import trashsoftware.trashSnooker.core.training.Challenge;
import trashsoftware.trashSnooker.core.training.TrainType;
import trashsoftware.trashSnooker.util.config.ConfigLoader;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.GeneralSaveManager;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

public class FastGameView extends ChildInitializable {

    @FXML
    Button resumeButton;

    @FXML
    Button player1InfoButton, player2InfoButton;

    @FXML
    ComboBox<Integer> totalFramesBox;

    @FXML
    ComboBox<GameRule> gameRuleBox;

    @FXML
    ComboBox<TableMetrics.TableBuilderFactory> tableMetricsBox;

    @FXML
    ComboBox<BallMetrics> ballMetricsBox;

    @FXML
    ComboBox<TableCloth.Smoothness> clothSmoothBox;

    @FXML
    ComboBox<TableCloth.Goodness> clothGoodBox;

    @FXML
    ComboBox<PocketSize> holeSizeBox;

    @FXML
    ComboBox<PocketDifficulty> pocketDifficultyBox;

    @FXML
    ComboBox<PersonItem> player1Box, player2Box;

    @FXML
    ComboBox<CueItem> player1CueBox, player2CueBox;

    @FXML
    ComboBox<PlayerType> player1Player, player2Player;
    @FXML
    ComboBox<CategoryItem> player1CatBox, player2CatBox;
    @FXML
    ComboBox<TablePresetWrapper> tablePresetBox;

    @FXML
    CheckBox devModeBox;

    @FXML
    ToggleGroup gameTrainToggleGroup;
    @FXML
    RadioButton gameRadioBtn, trainRadioBtn;
    @FXML
    Label vsText, trainingItemText;
    @FXML
    ComboBox<TrainType> trainingItemBox;
    @FXML
    CheckBox trainChallengeBox;

    private Stage stage;
    private ResourceBundle strings;

    private static <T> void selectBox(ComboBox<T> box, T value) {
        int index = box.getItems().indexOf(value);
        if (index != -1) {
//            System.out.println(index);
            box.getSelectionModel().select(index);
        } else System.out.println("Not value of " + value);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.strings = resources;

        initTypeSelectionToggle();
        initGameTypeBox();
        initTotalFramesBox();
        initClothBox();
        loadPlayerList();
        loadCueList();

        initTablePresentBox();

        resumeButton.setDisable(!GeneralSaveManager.getInstance().hasSavedGame());

        gameRuleBox.getSelectionModel().select(0);
        tableMetricsBox.getSelectionModel().select(0);
        ballMetricsBox.getSelectionModel().select(0);
    }

    public void reloadPlayerList() {
        loadPlayerList();
    }

    private void initTypeSelectionToggle() {
        gameTrainToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            changeTrainGameVisibility(Objects.equals(newValue.getUserData(), "TRAIN"));
        });
    }

    private void changeTrainGameVisibility(boolean isTrain) {
        trainingItemText.setVisible(isTrain);
        trainingItemBox.setVisible(isTrain);
        trainChallengeBox.setVisible(isTrain);

        totalFramesBox.setDisable(isTrain);

        vsText.setVisible(!isTrain);
        player2InfoButton.setVisible(!isTrain);
        player2Box.setVisible(!isTrain);
        player2Player.setVisible(!isTrain);
        player2CueBox.setVisible(!isTrain);
        player2CatBox.setVisible(!isTrain);
    }

    private void reloadTrainingItemByGameType(GameRule rule) {
        trainingItemBox.getItems().clear();
        trainingItemBox.getItems().addAll(rule.supportedTrainings);
        trainingItemBox.getSelectionModel().select(0);
    }

    private void initTotalFramesBox() {
        for (int i = 1; i <= 41; i += 2) {
            totalFramesBox.getItems().add(i);
        }
        totalFramesBox.getSelectionModel().select(0);
    }

    private void initClothBox() {
        clothSmoothBox.getItems().addAll(TableCloth.Smoothness.values());
        clothGoodBox.getItems().addAll(TableCloth.Goodness.values());
        clothSmoothBox.getSelectionModel().select(1);
        clothGoodBox.getSelectionModel().select(1);
    }

    private void initTablePresentBox() {
        tableMetricsBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> fillTablePresetBox(newValue));

        tablePresetBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (newValue != null && newValue.preset != null) {
                        holeSizeBox.setValue(newValue.preset.tableSpec.tableMetrics.pocketSize);
                        pocketDifficultyBox.setValue(newValue.preset.tableSpec.tableMetrics.factory.defaultDifficulty());
                        clothSmoothBox.setValue(newValue.preset.tableSpec.tableCloth.smoothness);
                        clothGoodBox.setValue(newValue.preset.tableSpec.tableCloth.goodness);
                    }
                });
    }

    private void fillTablePresetBox(TableMetrics.TableBuilderFactory factory) {
        tablePresetBox.getItems().clear();

        tablePresetBox.getItems().add(new TablePresetWrapper(strings.getString("tableCustom"), null));
        Map<String, TablePreset> tables = DataLoader.getInstance().getTablesOfType(factory.key);
        for (TablePreset tp : tables.values()) {
            tablePresetBox.getItems().add(new TablePresetWrapper(tp.name, tp));
        }
        tablePresetBox.getSelectionModel().select(0);
    }

    private void initGameTypeBox() {
        gameRuleBox.getItems().addAll(
                GameRule.values()
        );
        tableMetricsBox.getItems().addAll(
                TableMetrics.TableBuilderFactory.values()
        );
        ballMetricsBox.getItems().addAll(
                BallMetrics.SNOOKER_BALL,
                BallMetrics.POOL_BALL,
                BallMetrics.CAROM_BALL,
                BallMetrics.RUSSIAN_BALL
        );

        gameRuleBox.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            switch (newValue) {
                case SNOOKER, MINI_SNOOKER -> {
                    tableMetricsBox.getSelectionModel().select(TableMetrics.TableBuilderFactory.SNOOKER);
                    ballMetricsBox.getSelectionModel().select(BallMetrics.SNOOKER_BALL);
                }
                case CHINESE_EIGHT, LIS_EIGHT -> {
                    tableMetricsBox.getSelectionModel().select(TableMetrics.TableBuilderFactory.CHINESE_EIGHT);
                    ballMetricsBox.getSelectionModel().select(BallMetrics.POOL_BALL);
                }
                case AMERICAN_NINE -> {
                    tableMetricsBox.getSelectionModel().select(TableMetrics.TableBuilderFactory.POOL_TABLE_9);
                    ballMetricsBox.getSelectionModel().select(BallMetrics.POOL_BALL);
                }
            }
            reloadTrainingItemByGameType(newValue);
            PersonItem p1Item = player1Box.getValue();
            PersonItem p2Item = player2Box.getValue();
            selectSuggestedCue(player1CueBox, newValue, p1Item == null ? null : p1Item.person);
            selectSuggestedCue(player2CueBox, newValue, p2Item == null ? null : p2Item.person);
        }));

        tableMetricsBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                holeSizeBox.getItems().clear();
                holeSizeBox.getItems().addAll(newValue.supportedHoles);
                holeSizeBox.getSelectionModel().select(newValue.supportedHoles.length / 2);

                pocketDifficultyBox.getItems().clear();
                pocketDifficultyBox.getItems().addAll(newValue.supportedDifficulties);
                pocketDifficultyBox.getSelectionModel().select(newValue.supportedDifficulties.length / 2);
            }
        });
    }

    private void loadCueList() {
        refreshCueList(player1CueBox);
        refreshCueList(player2CueBox);
    }

    private void refreshCueList(ComboBox<CueItem> box) {
        box.getItems().clear();
        for (Cue cue : DataLoader.getInstance().getCues().values()) {
            if (!cue.privacy) {
                box.getItems().add(new CueItem(cue, cue.getName()));
            }
        }
        box.getSelectionModel().select(0);
    }

    private void loadPlayerList() {
        player1CatBox.getItems().addAll(CategoryItem.values());
        player2CatBox.getItems().addAll(CategoryItem.values());

        addCatBoxProperty(player1CatBox, player1Box);
        addCatBoxProperty(player2CatBox, player2Box);

        addPlayerBoxProperty(player1Box, player1CueBox, player1InfoButton);
        addPlayerBoxProperty(player2Box, player2CueBox, player2InfoButton);

        player1Player.getItems().addAll(PlayerType.PLAYER, PlayerType.COMPUTER);
        player2Player.getItems().addAll(PlayerType.PLAYER, PlayerType.COMPUTER);
        player1Player.getSelectionModel().select(0);
        player2Player.getSelectionModel().select(0);
    }

    private void addCatBoxProperty(ComboBox<CategoryItem> catBox,
                                   ComboBox<PersonItem> playerBox) {
        catBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                playerBox.getItems().clear();
                Collection<PlayerPerson> catPlayers = DataLoader.getInstance().filterActualPlayersByCategory(newValue.cat);
                for (PlayerPerson person : catPlayers) {
                    playerBox.getItems().add(new PersonItem(person));
                }
            }
        });
        catBox.getSelectionModel().select(0);
    }

    private void addPlayerBoxProperty(ComboBox<PersonItem> playerBox,
                                      ComboBox<CueItem> cueBox,
                                      Button infoButton) {
        playerBox.getSelectionModel().selectedItemProperty()
                .addListener(((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        infoButton.setDisable(false);
                        refreshCueList(cueBox);
                        for (Cue cue : newValue.person.getPrivateCues()) {
                            cueBox.getItems().add(new CueItem(cue, cue.getName()));
                        }
                        selectSuggestedCue(cueBox, gameRuleBox.getValue(), newValue.person);
                    } else {
                        infoButton.setDisable(true);
                    }
                }));
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    void playerInfoAction(ActionEvent event) {
        ComboBox<PersonItem> personBox;
        if (Objects.equals(event.getSource(), player1InfoButton)) {
            personBox = player1Box;
        } else {
            personBox = player2Box;
        }
        PersonItem person = personBox.getValue();
        if (person != null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("abilityView.fxml"),
                        strings
                );
                Parent root = loader.load();
                root.setStyle(App.FONT_STYLE);

                Stage stage = new Stage();
                stage.initOwner(this.stage);
                stage.initModality(Modality.WINDOW_MODAL);

                Scene scene = App.createScene(root);
                stage.setScene(scene);

                stage.show();

                AbilityView controller = loader.getController();
                controller.setup(scene, person.person);
            } catch (IOException e) {
                EventLogger.error(e);
            }
        }
    }

    @FXML
    void resumeAction() {
        EntireGame game = GeneralSaveManager.getInstance().getSave();
        if (game != null) {
            startGame(game);
        } else {
            throw new RuntimeException("???");
        }
    }

    @FXML
    void startGameAction() {
        TableCloth cloth = new TableCloth(clothGoodBox.getValue(), clothSmoothBox.getValue());

        TableMetrics.TableBuilderFactory tableMetricsFactory =
                tableMetricsBox.getValue();
        TableMetrics tableMetrics = tableMetricsFactory
                .create()
                .pocketDifficulty(pocketDifficultyBox.getValue())
                .holeSize(holeSizeBox.getValue())
                .build();

        GameRule rule = gameRuleBox.getValue();
        BallMetrics ballMetrics = ballMetricsBox.getValue();

        // todo: 检查规则兼容性

        GameValues values = new GameValues(rule, tableMetrics, ballMetrics);

        TrainType trainType = getTrainType();
        if (trainType != null) {
            Challenge challenge = null;
            if (trainChallengeBox.isSelected()) {
                challenge = new Challenge(rule, trainType);
            }

            values.setTrain(getTrainType(), challenge);
        }

        values.setTablePreset(tablePresetBox.getValue().preset);  // 可以是null
        values.setDevMode(devModeBox.isSelected());
        showGame(values, cloth);
    }

    public static void selectSuggestedCue(ComboBox<CueItem> cueBox, GameRule rule, PlayerPerson human) {
        Cue humanSuggestedCue = PlayerPerson.getPreferredCue(rule, human);
        selectCue(cueBox, humanSuggestedCue);
    }

    public static void selectCue(ComboBox<CueItem> cueBox, Cue cue) {
        for (CueItem cueItem : cueBox.getItems()) {
            if (cueItem.cue == cue) {
                cueBox.getSelectionModel().select(cueItem);
                return;
            }
        }

        System.err.println("Cue '" + cue.getCueId() + "' not in list");
        cueBox.getSelectionModel().select(0);
    }

    private TrainType getTrainType() {
        if (gameTrainToggleGroup.getSelectedToggle().getUserData().equals("TRAIN")) {
            return trainingItemBox.getValue();
        } else {
            return null;
        }
    }

    private void showGame(GameValues gameValues, TableCloth cloth) {
        PersonItem p1 = player1Box.getValue();
        PersonItem p2 = player2Box.getValue();
        if (p1 == null) {
            System.out.println("No enough players");
            return;
        }
        InGamePlayer igp1;
        InGamePlayer igp2;
        if (gameValues.isTraining()) {
            igp1 = new InGamePlayer(p1.person, player1CueBox.getValue().cue, player1Player.getValue(), 1, 1.0);
            igp2 = new InGamePlayer(p1.person, player1CueBox.getValue().cue, player1Player.getValue(), 2, 1.0);
        } else {
            if (p2 == null) {
                System.out.println("No enough players");
                return;
            }
            if (p1.person.getPlayerId().equals(p2.person.getPlayerId())) {
                System.out.println("Cannot self fight");
                return;
            }
            double p1RuleProficiency = player1Player.getValue() == PlayerType.COMPUTER ? p1.person.skillLevelOfGame(gameValues.rule) : 1.0;
            double p2RuleProficiency = player2Player.getValue() == PlayerType.COMPUTER ? p2.person.skillLevelOfGame(gameValues.rule) : 1.0;

            Cue stdBreakCue = DataLoader.getInstance().getStdBreakCue();
            if (stdBreakCue == null ||
                    gameValues.rule == GameRule.SNOOKER ||
                    gameValues.rule == GameRule.MINI_SNOOKER) {
                igp1 = new InGamePlayer(p1.person, player1CueBox.getValue().cue, player1Player.getValue(), 1,
                        p1RuleProficiency);
                igp2 = new InGamePlayer(p2.person, player2CueBox.getValue().cue, player2Player.getValue(), 2,
                        p2RuleProficiency);
            } else {
                igp1 = new InGamePlayer(p1.person, stdBreakCue, player1CueBox.getValue().cue, player1Player.getValue(), 1,
                        p1RuleProficiency);
                igp2 = new InGamePlayer(p2.person, stdBreakCue, player2CueBox.getValue().cue, player2Player.getValue(), 2,
                        p2RuleProficiency);
            }
        }

        EntireGame game = new EntireGame(igp1, igp2, gameValues, totalFramesBox.getValue(), cloth, null);
        startGame(game);
    }

    private void startGame(EntireGame entireGame) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("gameView.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage stage = new Stage();
            stage.initOwner(this.stage);
            stage.initModality(Modality.WINDOW_MODAL);

            Scene scene = App.createScene(root);
            stage.setScene(scene);

            AiCueResult.setAiPrecisionFactor(ConfigLoader.getInstance().getDouble("fastGameAiStrength", 1.0));

            GameView gameView = loader.getController();
            gameView.setup(stage, entireGame);
            gameView.setAimingLengthFactor(ConfigLoader.getInstance().getDouble("fastGameAiming", 1.0));

            stage.show();

            App.scaleGameStage(stage, gameView);
        } catch (Exception e) {
            EventLogger.error(e);
        }
    }

    public enum CategoryItem {
        ALL("All"),
        PROFESSIONAL("Professional"),
        AMATEUR("Amateur"),
        NOOB("Noob"),
        GOD("God");

        private final String cat;

        CategoryItem(String cat) {
            this.cat = cat;
        }

        @Override
        public String toString() {
            return PlayerPerson.getPlayerCategoryShown(cat, App.getStrings());
        }
    }

    public static class PersonItem {
        public final PlayerPerson person;

        PersonItem(PlayerPerson person) {
            this.person = person;
        }

        @Override
        public String toString() {
            return person.getName();
        }
    }

    public static class CueItem {
        public final Cue cue;
        private final String string;

        CueItem(Cue cue, String string) {
            this.cue = cue;
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }

    public static class TablePresetWrapper {
        public String shown;
        public TablePreset preset;

        TablePresetWrapper(String shown, TablePreset preset) {
            this.shown = shown;
            this.preset = preset;
        }

        @Override
        public String toString() {
            return shown;
        }
    }
}
