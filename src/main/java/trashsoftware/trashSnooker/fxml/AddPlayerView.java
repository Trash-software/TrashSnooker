package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.ai.AiPlayStyle;
import trashsoftware.trashSnooker.util.Recorder;

import java.net.URL;
import java.util.ResourceBundle;

public class AddPlayerView implements Initializable {

    @FXML
    TextField nameField;
    @FXML
    Slider powerSlider, spinSlider, precisionSlider, positionSlider;
    @FXML
    Label powerLabel, spinLabel, precisionLabel, positionLabel;

    private Stage stage;
    private MainView parent;

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

    public void setStage(Stage stage, MainView parent) {
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
                name,
                powerSlider.getValue(),
                powerSlider.getValue() * 0.85,
                spinSlider.getValue(),
                precisionSlider.getValue(),
                precisionSlider.getValue(),
                precisionSlider.getValue(),
                positionSlider.getValue(), 
                AiPlayStyle.DEFAULT
        );
        Recorder.addPlayerPerson(playerPerson);
        parent.reloadPlayerList();

        stage.close();
    }
}
