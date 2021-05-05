package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsView implements Initializable {
    @FXML
    ComboBox<Difficulty> difficultyBox;

    private Stage stage;
    private GameView parent;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        difficultyBox.getItems().addAll(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD);
        difficultyBox.getSelectionModel().select(Difficulty.MEDIUM);
    }

    void setup(Stage stage, GameView parent) {
        this.stage = stage;
        this.parent = parent;

        stage.setOnHidden(event ->
                parent.setDifficulty(difficultyBox.getSelectionModel().getSelectedItem()));
    }

    enum Difficulty {
        EASY("简单"),
        MEDIUM("标准"),
        HARD("困难");

        private final String shown;

        Difficulty(String shown) {
            this.shown = shown;
        }

        @Override
        public String toString() {
            return shown;
        }
    }
}
