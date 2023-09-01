package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.career.CareerManager;

import java.net.URL;
import java.util.ResourceBundle;

public class CareerSettingsView implements Initializable {

    @FXML
    ComboBox<NewCareerView.Difficulty> aiGoodnessBox;
    @FXML
    ComboBox<NewCareerView.Difficulty> playerGoodnessBox;

    CareerManager cm = CareerManager.getInstance();
    
    private Stage stage;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        
        NewCareerView.fillAiDifficulty(aiGoodnessBox);
        aiGoodnessBox.getSelectionModel().select(
                NewCareerView.getGoodnessIndex(aiGoodnessBox.getItems(), cm.getAiGoodness()));
        NewCareerView.fillPlayerDifficulty(playerGoodnessBox);
        playerGoodnessBox.getSelectionModel().select(
                NewCareerView.getGoodnessIndex(playerGoodnessBox.getItems(), cm.getPlayerGoodness()));
    }
    
    public void setup(Stage stage) {
        this.stage = stage;
    }
    
    @FXML
    public void confirmAction() {
        cm.changeDifficulty(playerGoodnessBox.getValue().multiplier, 
                aiGoodnessBox.getValue().multiplier);
        cm.saveSettings();
        stage.close();
    }
    
    @FXML
    public void cancelAction() {
        stage.close();
    }
}
