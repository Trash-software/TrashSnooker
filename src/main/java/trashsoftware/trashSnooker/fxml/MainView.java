package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.Cue;
import trashsoftware.trashSnooker.core.GameType;
import trashsoftware.trashSnooker.core.InGamePlayer;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Recorder;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.ResourceBundle;

public class MainView implements Initializable {
    
    @FXML
    ComboBox<Integer> totalFramesBox;

    @FXML
    ComboBox<PlayerPerson> player1Box, player2Box;

    @FXML
    ComboBox<CueItem> player1CueBox, player2CueBox;

    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initTotalFramesBox();
        loadPlayerList();
        loadCueList();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void reloadPlayerList() {
        loadPlayerList();
    }
    
    private void initTotalFramesBox() {
        totalFramesBox.getItems().addAll(
                1, 3, 5, 7, 9, 11, 13, 15, 17, 19,
                21, 23, 25, 27, 29, 31, 33, 35
        );
        totalFramesBox.getSelectionModel().select(0);
    }

    private void loadCueList() {
        refreshCueList(player1CueBox);
        refreshCueList(player2CueBox);
    }

    private void refreshCueList(ComboBox<CueItem> box) {
        box.getItems().clear();
        for (Cue cue : Recorder.getCues().values()) {
            if (!cue.privacy) {
                box.getItems().add(new CueItem(cue, cue.getName()));
            }
        }
        box.getSelectionModel().select(0);
    }

    private void loadPlayerList() {
        Collection<PlayerPerson> playerPeople = Recorder.getPlayerPeople();
        player1Box.getItems().clear();
        player2Box.getItems().clear();
        player1Box.getItems().addAll(playerPeople);
        player2Box.getItems().addAll(playerPeople);

        addPlayerBoxProperty(player1Box, player1CueBox);
        addPlayerBoxProperty(player2Box, player2CueBox);
    }

    private void addPlayerBoxProperty(ComboBox<PlayerPerson> playerBox, ComboBox<CueItem> cueBox) {
        playerBox.getSelectionModel().selectedItemProperty()
                .addListener(((observable, oldValue, newValue) -> {
                    refreshCueList(cueBox);
                    boolean sel = false;
                    for (Cue cue : newValue.getPrivateCues()) {
                        cueBox.getItems().add(new CueItem(cue, cue.getName()));
                        if (!sel) {
                            // 有私杆的人默认选择第一根私杆
                            cueBox.getSelectionModel().select(cueBox.getItems().size() - 1);
                            sel = true;
                        }
                    }
                }));
    }

    @FXML
    void addPlayerAction() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("addPlayerView.fxml")
            );
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initOwner(this.stage);
            stage.initModality(Modality.WINDOW_MODAL);

            Scene scene = new Scene(root);
            stage.setScene(scene);

            AddPlayerView view = loader.getController();
            view.setStage(stage, this);

            stage.show();
        } catch (IOException e) {
            EventLogger.log(e);
        }
    }

    @FXML
    void recordsAction() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("statsView.fxml")
            );
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initOwner(this.stage);
            stage.initModality(Modality.WINDOW_MODAL);

            Scene scene = new Scene(root);
            stage.setScene(scene);

            stage.show();
        } catch (IOException e) {
            EventLogger.log(e);
        }
    }

    @FXML
    void snookerAction() {
        showGame(GameType.SNOOKER);
    }

    @FXML
    void miniSnookerAction() {
        showGame(GameType.MINI_SNOOKER);
    }

    @FXML
    void chineseEightAction() {
        showGame(GameType.CHINESE_EIGHT);
    }

    @FXML
    void sidePocketAction() {
        showGame(GameType.SIDE_POCKET);
    }

    private void showGame(GameType gameType) {
        PlayerPerson p1 = player1Box.getValue();
        PlayerPerson p2 = player2Box.getValue();
        if (p1 == null || p2 == null) {
            System.out.println("没有足够的球员");
            return;
        }

        InGamePlayer igp1;
        InGamePlayer igp2;
        Cue stdBreakCue = Recorder.getStdBreakCue();
        if (stdBreakCue == null ||
                gameType == GameType.SNOOKER ||
                gameType == GameType.MINI_SNOOKER) {
            igp1 = new InGamePlayer(p1, player1CueBox.getValue().cue);
            igp2 = new InGamePlayer(p2, player2CueBox.getValue().cue);
        } else {
            igp1 = new InGamePlayer(p1, stdBreakCue, player1CueBox.getValue().cue);
            igp2 = new InGamePlayer(p2, stdBreakCue, player2CueBox.getValue().cue);
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("gameView.fxml")
            );
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initOwner(this.stage);
            stage.initModality(Modality.WINDOW_MODAL);

            Scene scene = new Scene(root);
            stage.setScene(scene);

            GameView gameView = loader.getController();
            gameView.setup(stage, gameType, totalFramesBox.getSelectionModel().getSelectedItem(), 
                    igp1, igp2);

            stage.show();
        } catch (Exception e) {
            EventLogger.log(e);
        }
    }

    public static class CueItem {
        private final Cue cue;
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
}
