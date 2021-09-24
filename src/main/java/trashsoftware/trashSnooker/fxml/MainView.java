package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
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
    ComboBox<PlayerPerson> player1Box, player2Box;

    @FXML
    ComboBox<CueItem> player1CueBox, player2CueBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadPlayerList();
        loadCueList();
    }

    public void reloadPlayerList() {
        loadPlayerList();
    }

    private void loadCueList() {
        refreshCueList(player1CueBox);
        refreshCueList(player2CueBox);
    }

    private void refreshCueList(ComboBox<CueItem> box) {
        box.getItems().clear();
        box.getItems().add(new CueItem(Cue.STD_ASH, "斯诺克杆"));
        box.getItems().add(new CueItem(Cue.STD_BIG, "九球杆"));
        box.getSelectionModel().select(0);
    }

    private void loadPlayerList() {
        Collection<PlayerPerson> playerPeople = Recorder.getPlayerPeople();
        player1Box.getItems().clear();
        player2Box.getItems().clear();
        player1Box.getItems().addAll(playerPeople);
        player2Box.getItems().addAll(playerPeople);

        player1Box.getSelectionModel().selectedItemProperty()
                .addListener(((observable, oldValue, newValue) -> {
                    refreshCueList(player1CueBox);
                    for (Cue cue : newValue.getPrivateCues()) {
                        player1CueBox.getItems().add(new CueItem(cue, cue.getName()));
                    }
                }));
        player2Box.getSelectionModel().selectedItemProperty()
                .addListener(((observable, oldValue, newValue) -> {
                    refreshCueList(player2CueBox);
                    for (Cue cue : newValue.getPrivateCues()) {
                        player2CueBox.getItems().add(new CueItem(cue, cue.getName()));
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
        if (gameType == GameType.SNOOKER || gameType == GameType.MINI_SNOOKER) {
            igp1 = new InGamePlayer(p1, player1CueBox.getValue().cue);
            igp2 = new InGamePlayer(p2, player2CueBox.getValue().cue);
        } else {
            igp1 = new InGamePlayer(p1, Cue.STD_BREAK_CUE, player1CueBox.getValue().cue);
            igp2 = new InGamePlayer(p2, Cue.STD_BREAK_CUE, player2CueBox.getValue().cue);
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("gameView.fxml")
            );
            Parent root = loader.load();

            Stage stage = new Stage();

            Scene scene = new Scene(root);
            stage.setScene(scene);

            GameView gameView = loader.getController();
            gameView.setup(stage, gameType, igp1, igp2);

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
