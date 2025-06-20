package trashsoftware.trashSnooker.fxml;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.ai.AiCueResult;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.CueBrand;
import trashsoftware.trashSnooker.core.metrics.*;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.LetBall;
import trashsoftware.trashSnooker.core.person.PlayerPerson;
import trashsoftware.trashSnooker.core.phy.TableCloth;
import trashsoftware.trashSnooker.core.training.Challenge;
import trashsoftware.trashSnooker.core.training.TrainType;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.GeneralSaveManager;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import java.io.IOException;
import java.net.URL;
import java.util.*;

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
    ComboBox<SubRule> subRuleBox;
    @FXML
    ComboBox<PersonItem> player1Box, player2Box;
    @FXML
    ComboBox<PlayerType> player1Player, player2Player;
    @FXML
    ComboBox<LetScoreOrBall> player1LetScore, player2LetScore;
    @FXML
    Label letScoreOrBallLabel;
    @FXML
    ComboBox<CategoryItem> player1CatBox, player2CatBox;
    @FXML
    ComboBox<TablePresetWrapper> tablePresetBox;
    @FXML
    ComboBox<BallsPresetWrapper> ballsPresetBox;

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

        initPresetBoxes();

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
//        player2CueBtn.setVisible(!isTrain);
        player2CatBox.setVisible(!isTrain);
        
        player1LetScore.setVisible(!isTrain);
        player1LetScore.setVisible(!isTrain);
        letScoreOrBallLabel.setVisible(!isTrain);
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

    private void initPresetBoxes() {
        tableMetricsBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> fillTablePresetBox(newValue));

        tablePresetBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (newValue != null && newValue.preset != null) {
                        holeSizeBox.setValue(newValue.preset.tableSpec.tableMetrics.pocketSize);
                        pocketDifficultyBox.setValue(newValue.preset.tableSpec.tableMetrics.pocketDifficulty);
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
    
    private void fillBallsPresetBox(GameRule rule) {
        ballsPresetBox.getItems().clear();
        
        ballsPresetBox.getItems().add(new BallsPresetWrapper(strings.getString("ballsStandard"), null));
        Map<String, BallsGroupPreset> balls = DataLoader.getInstance().getBallsPresetsOfType(rule);
        for (BallsGroupPreset bgp : balls.values()) {
            ballsPresetBox.getItems().add(new BallsPresetWrapper(bgp.name, bgp));
        }
        ballsPresetBox.getSelectionModel().select(0);
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
                case SNOOKER_TEN -> {
                    tableMetricsBox.getSelectionModel().select(TableMetrics.TableBuilderFactory.CHINESE_EIGHT);
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
            subRuleBox.getItems().clear();
            switch (newValue) {
                case SNOOKER -> 
                        subRuleBox.getItems().addAll(SubRule.SNOOKER_STD, SubRule.SNOOKER_GOLDEN);
                case CHINESE_EIGHT ->
                        subRuleBox.getItems().addAll(SubRule.CHINESE_EIGHT_STD, SubRule.CHINESE_EIGHT_JOE);
                default -> subRuleBox.getItems().add(SubRule.RAW_STD);
            }
            subRuleBox.getSelectionModel().select(0);
            fillBallsPresetBox(newValue);
            reloadTrainingItemByGameType(newValue);
            loadLetScoreOrBallList(newValue);
        }));

        tableMetricsBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                holeSizeBox.getItems().clear();
                holeSizeBox.getItems().addAll(newValue.supportedHoles);
                holeSizeBox.getSelectionModel().select(newValue.defaultPocketSize());

                pocketDifficultyBox.getItems().clear();
                pocketDifficultyBox.getItems().addAll(newValue.supportedDifficulties);
                pocketDifficultyBox.getSelectionModel().select(newValue.defaultDifficulty());
            }
        });
    }
    
    private void loadLetScoreOrBallList(GameRule gameRule) {
        player1LetScore.getItems().clear();
        player2LetScore.getItems().clear();
        switch (gameRule) {
            case SNOOKER, SNOOKER_TEN, MINI_SNOOKER -> {
                fillSnookerLikeLetScores(player1LetScore);
                fillSnookerLikeLetScores(player2LetScore);
            }
            case CHINESE_EIGHT -> {
                fillChineseEightLetScores(player1LetScore);
                fillChineseEightLetScores(player2LetScore);
            }
            default -> {
                player1LetScore.setEditable(false);
                player1LetScore.getItems().add(LetScoreOrBall.NOT_LET);
                player1LetScore.getSelectionModel().select(0);
                player2LetScore.setEditable(false);
                player2LetScore.getItems().add(LetScoreOrBall.NOT_LET);
                player2LetScore.getSelectionModel().select(0);
            }
        }
    }

    private void fillChineseEightLetScores(ComboBox<LetScoreOrBall> box) {
        box.setEditable(false);
        box.getItems().addAll(
                LetScoreOrBall.NOT_LET,
                new LetScoreOrBall.LetBallFace(Map.of(LetBall.FRONT, 1)),
                new LetScoreOrBall.LetBallFace(Map.of(LetBall.MID, 1)),
                new LetScoreOrBall.LetBallFace(Map.of(LetBall.BACK, 1)),
                new LetScoreOrBall.LetBallFace(Map.of(LetBall.FRONT, 1, LetBall.BACK, 1)),
                new LetScoreOrBall.LetBallFace(Map.of(LetBall.MID, 1, LetBall.BACK, 1)),
                new LetScoreOrBall.LetBallFace(Map.of(LetBall.FRONT, 1, LetBall.MID, 1, LetBall.BACK, 1)),
                new LetScoreOrBall.LetBallFace(Map.of(LetBall.FRONT, 2)),
                new LetScoreOrBall.LetBallFace(Map.of(LetBall.FRONT, 2, LetBall.BACK, 1)),
                new LetScoreOrBall.LetBallFace(Map.of(LetBall.BACK, 2)),
                new LetScoreOrBall.LetBallFace(Map.of(LetBall.FRONT, 1, LetBall.BACK, 2)),
                new LetScoreOrBall.LetBallFace(Map.of(LetBall.BACK, 3)),
                new LetScoreOrBall.LetBallFace(Map.of(LetBall.FRONT, 2, LetBall.BACK, 2)),
                new LetScoreOrBall.LetBallFace(Map.of(LetBall.BACK, 4)),
                new LetScoreOrBall.LetBallFace(Map.of(LetBall.BACK, 5)),
                new LetScoreOrBall.LetBallFace(Map.of(LetBall.BACK, 6))
        );
        box.getSelectionModel().select(0);
    }
    
    private void fillSnookerLikeLetScores(ComboBox<LetScoreOrBall> box) {
        box.setEditable(true);
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(LetScoreOrBall object) {
                return object == null ? "" : object.toString();
            }

            @Override
            public LetScoreOrBall fromString(String string) {
                try {
                    int score = Integer.parseInt(string);
                    return new LetScoreOrBall.LetScoreFace(Math.max(0, Math.min(score, 147)));
                } catch (IllegalArgumentException e) {
                    //
                }
                return LetScoreOrBall.NOT_LET;
            }
        });
        
        box.getItems().addAll(
                LetScoreOrBall.NOT_LET,
                new LetScoreOrBall.LetScoreFace(15),
                new LetScoreOrBall.LetScoreFace(20),
                new LetScoreOrBall.LetScoreFace(30),
                new LetScoreOrBall.LetScoreFace(40),
                new LetScoreOrBall.LetScoreFace(50),
                new LetScoreOrBall.LetScoreFace(60),
                new LetScoreOrBall.LetScoreFace(70),
                new LetScoreOrBall.LetScoreFace(80),
                new LetScoreOrBall.LetScoreFace(100)
        );
        
        box.getSelectionModel().select(0);
    }

    private void loadPlayerList() {
        player1CatBox.getItems().addAll(CategoryItem.values());
        player2CatBox.getItems().addAll(CategoryItem.values());

        addCatBoxProperty(player1CatBox, player1Box);
        addCatBoxProperty(player2CatBox, player2Box);

        addPlayerBoxProperty(player1Box, player1InfoButton);
        addPlayerBoxProperty(player2Box, player2InfoButton);

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
                                      Button infoButton) {
        playerBox.getSelectionModel().selectedItemProperty()
                .addListener(((observable, oldValue, newValue) -> {
                    infoButton.setDisable(newValue == null);
                }));
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    void playerInfoAction(ActionEvent event) {
        ComboBox<PersonItem> personBox;
        ComboBox<PersonItem> anotherBox;
        if (Objects.equals(event.getSource(), player1InfoButton)) {
            personBox = player1Box;
            anotherBox = player2Box;
        } else {
            personBox = player2Box;
            anotherBox = player1Box;
        }
        PersonItem person = personBox.getValue();
        PersonItem another = anotherBox.getValue();
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

                if (another != null) {
                    controller.setOpponent(another.person);
                }
            } catch (IOException e) {
                EventLogger.error(e);
            }
        }
    }

    @FXML
    void resumeAction() {
        EntireGame game = GeneralSaveManager.getInstance().getSave();
        if (game != null && !game.isFinished()) {
            startGame(game);
        } else {
            resumeButton.setDisable(true);
//            throw new RuntimeException("???");
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
        SubRule subRule = subRuleBox.getValue();
        Collection<SubRule> subRules = List.of(subRule);
        BallMetrics ballMetrics = ballMetricsBox.getValue();

        // todo: 检查规则兼容性
        // todo: subRules多选
        GameValues values = new GameValues(rule, subRules, tableMetrics, ballMetrics);

        TrainType trainType = getTrainType();
        if (trainType != null) {
            Challenge challenge = null;
            if (trainChallengeBox.isSelected()) {
                challenge = new Challenge(rule, trainType);
            }

            values.setTrain(getTrainType(), challenge);
        }

        TablePreset preset = tablePresetBox.getValue().preset;
        values.setTablePreset(preset);  // 可以是null
        if (preset != null) {
            if (preset.tableBorderColor != null) {
                values.table.tableBorderColor = preset.tableBorderColor;
            }
            if (preset.clothColor != null) {
                values.table.tableColor = preset.clothColor;
            }
        }
        
        BallsGroupPreset ballsGroupPreset = ballsPresetBox.getValue().preset;
        values.setBallsGroupPreset(ballsGroupPreset);  // 可以是null
        
        values.setDevMode(devModeBox.isSelected());
        showGame(values, cloth);
    }

    public static void selectSuggestedCue(CueSelection cueSelection, GameRule rule, PlayerPerson person) {
        CueBrand personSuggestedCue = PlayerPerson.getPreferredCue(rule, person);

        cueSelection.selectByBrand(personSuggestedCue);
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

        GameRule gameRule = gameValues.rule;

        if (gameValues.isTraining()) {
            igp1 = new InGamePlayer(p1.person,
                    player1Player.getValue(),
                    null,
                    gameRule,
                    1,
                    1.0);
            igp2 = new InGamePlayer(p1.person,
                    player1Player.getValue(),
                    null,
                    gameRule,
                    2,
                    1.0);
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
            
            igp1 = new InGamePlayer(p1.person,
                    player1Player.getValue(),
                    null,
                    gameRule,
                    1,
                    p1RuleProficiency);
            igp2 = new InGamePlayer(p2.person,
                    player2Player.getValue(),
                    null,
                    gameRule, 2,
                    p2RuleProficiency);
            igp1.setLetScoreOrBall(player1LetScore.getValue());
            igp2.setLetScoreOrBall(player2LetScore.getValue());
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

    public static class BallsPresetWrapper {
        public String shown;
        public BallsGroupPreset preset;

        BallsPresetWrapper(String shown, BallsGroupPreset preset) {
            this.shown = shown;
            this.preset = preset;
        }

        @Override
        public String toString() {
            return shown;
        }
    }
    
}
