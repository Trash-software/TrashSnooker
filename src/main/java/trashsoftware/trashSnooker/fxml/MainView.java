package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.GameType;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Recorder;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

public class MainView implements Initializable {

    @FXML
    ComboBox<PlayerPerson> player1Box, player2Box;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadPlayerList();
    }

    public void reloadPlayerList() {
        loadPlayerList();
    }

    private void loadPlayerList() {
        Collection<PlayerPerson> playerPeople = Recorder.getPlayerPeople();
        player1Box.getItems().clear();
        player2Box.getItems().clear();
        player1Box.getItems().addAll(playerPeople);
        player2Box.getItems().addAll(playerPeople);
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

    private void showGame(GameType gameType) {
        PlayerPerson p1 = player1Box.getValue();
        PlayerPerson p2 = player2Box.getValue();
        if (p1 == null || p2 == null) {
            System.out.println("没有足够的球员");
            return;
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
            gameView.setup(stage, gameType, p1, p2);

            stage.show();
        } catch (Exception e) {
            EventLogger.log(e);
        }
    }
}
