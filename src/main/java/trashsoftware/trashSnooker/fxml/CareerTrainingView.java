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
import trashsoftware.trashSnooker.core.career.Career;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.challenge.ChallengeManager;
import trashsoftware.trashSnooker.core.career.challenge.ChallengeMatch;
import trashsoftware.trashSnooker.core.career.challenge.ChallengeSet;
import trashsoftware.trashSnooker.fxml.widgets.GamePane;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.ThumbLoader;

import java.net.URL;
import java.util.ResourceBundle;

public class CareerTrainingView extends ChildInitializable {
    @FXML
    TableView<ChallengeItem> challengeTable;
    @FXML
    TableColumn<ChallengeItem, String> challengeTitleCol;
    @FXML
    TableColumn<ChallengeItem, Integer> challengeExpCol;
    @FXML
    TableColumn<ChallengeItem, String> challengeCompletedCol;
    @FXML
    ComboBox<FastGameView.CueItem> cueBox;
    @FXML
    VBox outBox;
    @FXML
    GamePane previewPane;
    @FXML
    ImageView previewImage;
    @FXML
    Button startBtn;

    private Career career;
    private Stage stage;
    private ResourceBundle strings;
    private CareerView parent;

    public void setup(Stage stage, Career career, CareerView parent) {
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
    }

    private void refreshCueBox() {
        ChampDrawView.refreshCueBox(cueBox);
        cueBox.getSelectionModel().select(0);
    }

    private void initTable() {
        challengeTitleCol.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().data.getName()));
        challengeExpCol.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().data.getExp()));
        challengeCompletedCol.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().completed()));

        challengeTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                    startBtn.setDisable(newValue == null);
                    if (newValue != null) {
                        drawPreview(newValue.data);
                    } else {
                        drawPreview(null);
                    }
                }
        );
    }

    public void refreshList() {
        challengeTable.getItems().clear();

        for (ChallengeSet cs : ChallengeManager.getInstance().getAllChallenges()) {
            boolean completed = career.challengeIsComplete(cs.getId());
            ChallengeItem item = new ChallengeItem(cs, completed);
            challengeTable.getItems().add(item);
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

            previewPane.setupPane(challengeSet.getGameValues(), 0.32);
            Game<?, ?> fakeGame = Game.createGame(null, challengeSet.getGameValues(), null);
            previewPane.setupBalls(fakeGame, false);
            previewPane.drawTable(fakeGame);
            previewPane.drawStoppedBalls(fakeGame.getTable(), fakeGame.getAllBalls(), null);
            outBox.setPrefWidth(previewPane.getPrefWidth() + 385);

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
        Cue cue = cueBox.getSelectionModel().getSelectedItem().cue;
        PlayerPerson person = career.getPlayerPerson();

        InGamePlayer igp1 = new InGamePlayer(person, cue, PlayerType.PLAYER, 1, 1.0);
        InGamePlayer igp2 = new InGamePlayer(person, cue, PlayerType.PLAYER, 2, 1.0);

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

            Scene scene = new Scene(root);
            stage.setScene(scene);

            AiCueResult.setAiPrecisionFactor(CareerManager.getInstance().getAiGoodness());

            GameView gameView = loader.getController();
            gameView.setupCareerMatch(stage, match);

            stage.show();
        } catch (Exception e) {
            EventLogger.error(e);
        }
    }

    public static class ChallengeItem {
        final ChallengeSet data;
        final boolean complete;

        ChallengeItem(ChallengeSet data, boolean complete) {
            this.data = data;
            this.complete = complete;
        }

        public String completed() {
            return complete ? "✓" : "";
        }
    }
}
