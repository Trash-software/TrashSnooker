package trashsoftware.trashSnooker.fxml;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.ai.AiCueResult;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.HumanCareer;
import trashsoftware.trashSnooker.core.career.challenge.*;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.fxml.widgets.GamePane;
import trashsoftware.trashSnooker.fxml.widgets.LabelTable;
import trashsoftware.trashSnooker.fxml.widgets.LabelTableColumn;
import trashsoftware.trashSnooker.res.ResourcesLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.ThumbLoader;
import trashsoftware.trashSnooker.util.Util;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ResourceBundle;

public class CareerTrainingView extends ChildInitializable {
    @FXML
    TableView<ChallengeItem> challengeTable;
    @FXML
    TableColumn<ChallengeItem, String> challengeTitleCol;
    @FXML
    TableColumn<ChallengeItem, String> challengeProgressCol;
    @FXML
    TableColumn<ChallengeItem, String> challengeBestScoreCol;
    @FXML
    TableColumn<ChallengeItem, String> challengeCompletedCol;
    @FXML
    ComboBox<CueSelection.CueAndBrand> cueBox;
    @FXML
    VBox outBox;
    @FXML
    GamePane previewPane;
    @FXML
    ImageView previewImage;
    @FXML
    Button startBtn;
    @FXML
    LabelTable<RewardEach> rewardsTable;
    @FXML
    LabelTable<ChaHistoryEach> historyTable;

    private HumanCareer career;
    private Stage stage;
    private ResourceBundle strings;
    private CareerView parent;

    public void setup(Stage stage, HumanCareer career, CareerView parent) {
        this.stage = stage;
        this.career = career;
        this.parent = parent;

        refreshList();
        refreshCueBox();
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;

        initTable();
        initRewardsTable();
        initHistoryTable();
    }

    private void refreshCueBox() {
//        ChampDrawView.refreshCueBox(cueBox);
        cueBox.getSelectionModel().select(0);
    }

    private void initHistoryTable() {
        LabelTableColumn<ChaHistoryEach, String> timeCol = new LabelTableColumn<>(
                historyTable,
                strings.getString("historyDateTime"),
                che -> new ReadOnlyStringWrapper(che.getTime())
        );
        LabelTableColumn<ChaHistoryEach, String> scoreCol = new LabelTableColumn<>(
                historyTable,
                strings.getString("historyScore"),
                che -> new ReadOnlyStringWrapper(che.record.score == 0 ? "-" : String.valueOf(che.record.score))
        );

        historyTable.addColumns(timeCol, scoreCol);
    }

    private void initRewardsTable() {
        LabelTableColumn<RewardEach, String> conditionCol = new LabelTableColumn<>(
                rewardsTable,
                "",
                re -> new ReadOnlyStringWrapper(re.condition.toReadable())
        );
        LabelTableColumn<RewardEach, Integer> expCol = new LabelTableColumn<>(
                rewardsTable,
                ResourcesLoader.getInstance().createExpIcon(),
                re -> new ReadOnlyObjectWrapper<>(re.reward.getExp())
        );
        LabelTableColumn<RewardEach, Integer> moneyCol = new LabelTableColumn<>(
                rewardsTable,
                ResourcesLoader.getInstance().createMoneyIcon(),
                re -> new ReadOnlyObjectWrapper<>(re.reward.getMoney())
        );
        LabelTableColumn<RewardEach, String> everCompleteCol = new LabelTableColumn<>(
                rewardsTable,
                "",
                re -> new ReadOnlyStringWrapper(re.completed())
        );

        rewardsTable.addColumns(conditionCol, expCol, moneyCol, everCompleteCol);
        rewardsTable.setFitToHeight(true);
    }

    private void initTable() {
        challengeTitleCol.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().data.getName()));
        challengeProgressCol.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getProgress()));
        challengeBestScoreCol.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getHighest()));
//        challengeCompletedCol.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().completed()));

        challengeTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                    startBtn.setDisable(newValue == null);
                    if (newValue != null) {
                        drawPreview(newValue.data);
                    } else {
                        drawPreview(null);
                    }
                    updateRewardsTable();
                    updateHistoryTable();
                }
        );
    }

    public void refreshList() {
        challengeTable.getItems().clear();

        for (ChallengeSet cs : ChallengeManager.getInstance().getAllChallenges()) {
            ChallengeHistory ch = career.getChallengeHistory(cs.getId());
            ChallengeItem item = new ChallengeItem(cs, ch);
            challengeTable.getItems().add(item);
        }

        updateRewardsTable();
        updateHistoryTable();
    }

    private void updateRewardsTable() {
        rewardsTable.clearItems();

        ChallengeItem selected = challengeTable.getSelectionModel().getSelectedItem();
        if (selected != null && selected.ch != null) {
            rewardsTable.setVisible(true);
            ChallengeSet challengeSet = ChallengeManager.getInstance().getById(selected.ch.challengeId);
            var completed = challengeSet.getFulfilledBy(selected.ch.getScores());
            var crs = challengeSet.getConditionRewards();
            var sorted = new ArrayList<>(crs.keySet());
            Collections.sort(sorted);
            System.out.println(sorted);
            for (var cond : sorted) {
                boolean everComp = completed.containsKey(cond);
                RewardEach each = new RewardEach(
                        cond,
                        crs.get(cond),
                        everComp
                );
                rewardsTable.addItem(each);
            }
        } else {
            rewardsTable.setVisible(false);
        }
    }

    private void updateHistoryTable() {
        historyTable.clearItems();

        ChallengeItem selected = challengeTable.getSelectionModel().getSelectedItem();
        if (selected != null && selected.ch != null) {
            historyTable.setVisible(true);
            for (ChallengeHistory.Record record : selected.ch.getScores()) {
                historyTable.addItem(new ChaHistoryEach(record));
            }
        } else {
            historyTable.setVisible(false);
        }
    }

    private void drawPreview(ChallengeSet challengeSet) {
        if (challengeSet == null) {
//            previewPane.clear();
        } else {
//            Image image = ThumbLoader.loadThumbOf(challengeSet.getId());
//            if (image == null) {
            previewImage.setVisible(false);
            previewImage.setManaged(false);
            previewPane.setVisible(true);
            previewPane.setManaged(true);

            double[] resolution = ConfigLoader.getInstance().getEffectiveResolution();
            previewPane.setupPane(challengeSet.getGameValues(), 0.32 * 1536 / resolution[0]);
            Game<?, ?> fakeGame = Game.createGame(null, challengeSet.getGameValues(), null);
            previewPane.setupBalls(fakeGame, false);
            previewPane.drawTable(fakeGame);
            previewPane.drawStoppedBalls(fakeGame.getTable(), fakeGame.getAllBalls(), null);
            outBox.setPrefWidth(previewPane.getPrefWidth() + 455);

//                snapshotPreview(challengeSet.getId());
//            } else {
//                previewImage.setVisible(true);
//                previewImage.setManaged(true);
//                previewPane.setVisible(false);
//                previewPane.setManaged(false);
//                previewImage.setImage(image);
//            }
        }
        stage.sizeToScene();
    }

    private void snapshotPreview(String name) {
        SnapshotParameters params = new SnapshotParameters();
//        double scale = 1.0 / previewPane.getScale();
//        params.setTransform(new Scale(scale, scale));
        WritableImage writableImage = previewPane.snapshot(params, null);
        ThumbLoader.writeThumbnail(writableImage, name);
    }

    @FXML
    public void startChallengeAction() {
        startGameChallenge(challengeTable.getSelectionModel().getSelectedItem());
    }

    private void startGameChallenge(ChallengeItem item) {
        ChallengeSet challengeSet = item.data;
//        Cue cue = cueBox.getSelectionModel().getSelectedItem().getNonNullInstance();
        PlayerPerson person = career.getPlayerPerson();

//        InGamePlayer igp1 = new InGamePlayer(person, cue, PlayerType.PLAYER, 1, 1.0);
//        InGamePlayer igp2 = new InGamePlayer(person, cue, PlayerType.PLAYER, 2, 1.0);
        InGamePlayer igp1 = new InGamePlayer(person, PlayerType.PLAYER, 
                CareerManager.getInstance().getInventory(), challengeSet.getGameValues().rule,
                1, 1.0);
        InGamePlayer igp2 = new InGamePlayer(person, PlayerType.PLAYER, 
                null, challengeSet.getGameValues().rule, 2, 1.0);

        EntireGame game = new EntireGame(igp1, igp2, challengeSet.getGameValues(), 1, challengeSet.getCloth(), null);

        ChallengeMatch match = new ChallengeMatch(career, challengeSet);
        match.setGame(game);

        match.setGuiCallback(this::challengeFinish, this::challengeFinish);

        startGame(match);
    }

    private void challengeFinish() {
        refreshList();

        parent.refreshGui();
    }

    private void startGame(ChallengeMatch match) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("gameView.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage stage = new Stage();

            stage.setTitle(match.challengeSet.getName());

            stage.initOwner(this.stage);
            stage.initModality(Modality.WINDOW_MODAL);

            Scene scene = App.createScene(root);
            stage.setScene(scene);

            AiCueResult.setAiPrecisionFactor(CareerManager.getInstance().getAiGoodness());

            GameView gameView = loader.getController();
            gameView.setupCareerMatch(stage, match);

            stage.show();

            App.scaleGameStage(stage, gameView);
        } catch (Exception e) {
            EventLogger.error(e);
        }
    }

    public static class ChallengeItem {
        final ChallengeSet data;
        final ChallengeHistory ch;
        int highest;
        int total;
        int got;

        ChallengeItem(ChallengeSet data, ChallengeHistory challengeHistory) {
            this.data = data;
            this.ch = challengeHistory;
            this.highest = challengeHistory != null ?
                    challengeHistory.getBestScore() :
                    0;
            this.got = challengeHistory != null ?
                    challengeHistory.getCompleted(data, ChallengeReward.Type.EXP) :
                    0;
            this.total = data.getTotal(ChallengeReward.Type.EXP);
        }

        public String getProgress() {
            return got + "/" + total;
        }

//        public boolean isComplete() {
//            return ch != null && ch.isCompleted();
//        }

        public String getHighest() {
            return highest == 0 ? "" : String.valueOf(highest);
        }

//        public String completed() {
//            return isComplete() ? "✔" : "";
//        }
    }

    public static class ChaHistoryEach {
        public final ChallengeHistory.Record record;

        ChaHistoryEach(ChallengeHistory.Record record) {
            this.record = record;
        }

        public String getTime() {
            return Util.SHOWING_DATE_FORMAT.format(record.finishTime);
        }
    }

    public static class RewardEach {
        public final RewardCondition condition;
        public final ChallengeReward reward;
        boolean everCompleted;

        RewardEach(RewardCondition condition, ChallengeReward reward, boolean everCompleted) {
            this.condition = condition;
            this.reward = reward;
            this.everCompleted = everCompleted;
        }

        public String completed() {
            return everCompleted ? "✔" : "";
        }
    }
}
