package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.GameType;
import trashsoftware.trashSnooker.util.EventLogger;

import java.net.URL;
import java.util.ResourceBundle;

public class MainView implements Initializable {
    @Override
    public void initialize(URL location, ResourceBundle resources) {

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
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("gameView.fxml")
            );
            Parent root = loader.load();

            Stage stage = new Stage();

            Scene scene = new Scene(root);
            stage.setScene(scene);

            GameView gameView = loader.getController();
            gameView.setup(stage, gameType);

            stage.show();
        } catch (Exception e) {
            EventLogger.log(e);
        }
    }
}
