package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.ai.AiPlayStyle;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.util.DataLoader;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AddPlayerView implements Initializable {

    @FXML
    TextField nameField;
    @FXML
    Slider powerSlider, spinSlider, precisionSlider, positionSlider;
    @FXML
    Label powerLabel, spinLabel, precisionLabel, positionLabel;

    private Stage stage;
    private FastGameView parent;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        powerSlider.valueProperty().addListener(((observable, oldValue, newValue) ->
                powerLabel.setText(String.valueOf(Math.round(newValue.doubleValue())))));
        spinSlider.valueProperty().addListener(((observable, oldValue, newValue) ->
                spinLabel.setText(String.valueOf(Math.round(newValue.doubleValue())))));
        precisionSlider.valueProperty().addListener(((observable, oldValue, newValue) ->
                precisionLabel.setText(String.valueOf(Math.round(newValue.doubleValue())))));
        positionSlider.valueProperty().addListener(((observable, oldValue, newValue) ->
                positionLabel.setText(String.valueOf(Math.round(newValue.doubleValue())))));

        powerSlider.setValue(80.0);
        spinSlider.setValue(80.0);
        precisionSlider.setValue(80.0);
        positionSlider.setValue(80.0);
    }

    public void setStage(Stage stage, FastGameView parent) {
        this.stage = stage;
        this.parent = parent;
    }

    @FXML
    void saveAction() {
        String name = nameField.getText();
        if (name.isBlank()) {
            System.out.println("No name");
            return;
        }

        PlayerPerson playerPerson = new PlayerPerson(
                DataLoader.getInstance().getNextCustomPlayerId(),
                name,
                powerSlider.getValue(),
                powerSlider.getValue() * 0.88,
                spinSlider.getValue(),
                precisionSlider.getValue(),
                precisionSlider.getValue(),  // todo
                1.0,
                1.0,
                positionSlider.getValue(),
                positionSlider.getValue(),
                90.0,
                new AiPlayStyle(
                        Math.min(99.5, precisionSlider.getValue() * 1.1),
                        Math.min(99.5, precisionSlider.getValue()),
                        Math.min(99.5, positionSlider.getValue() * 1.1),
                        Math.min(99.5, positionSlider.getValue() * 1.1),
                        Math.min(100, precisionSlider.getValue()),
                        50,
                        "right",
                        powerSlider.getValue() * 0.88 < 80.0,  // 不化简是为了易读
                        2
                ),
                true,
                null,
                PlayerPerson.Sex.M,
                List.of(GameRule.values())
        );
        DataLoader.getInstance().addPlayerPerson(playerPerson);
        parent.reloadPlayerList();

        stage.close();
    }
}
