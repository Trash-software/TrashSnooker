package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.CareerSave;
import trashsoftware.trashSnooker.core.career.ChampDataManager;
import trashsoftware.trashSnooker.util.DataLoader;

import java.io.IOException;
import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;

public class NewCareerView implements Initializable {
    @FXML
    ComboBox<Hand> handBox;
    @FXML
    TextField nameField;
    
    private Stage owner, thisStage;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        handBox.getItems().addAll(Hand.values());
        handBox.getSelectionModel().select(1);

        ChampDataManager.getInstance();
    }
    
    public void setup(Stage owner, Stage thisStage) {
        this.owner = owner;
        this.thisStage = thisStage;
    }
    
    @FXML
    public void createPlayer() throws IOException {
        String name = nameField.getText();
        if (name.isBlank()) return;
        
        boolean leftHanded = handBox.getValue() == Hand.LEFT;

        PlayerPerson playerPerson = PlayerPerson.randomPlayer(
                DataLoader.getInstance().getNextCustomPlayerId(),
                name,
                leftHanded,
                60.0,
                75.0,
                true
        );
        
        DataLoader.getInstance().addPlayerPerson(playerPerson);
        createCareerInfo(playerPerson);
        launchNext();
    }
    
    private void launchNext() throws IOException {
        thisStage.close();
        EntryView.startCareerView(owner, new Stage());
    }
    
    private void createCareerInfo(PlayerPerson person) throws IOException {
        CareerSave cs = CareerManager.createNew(person);
        CareerManager.setCurrentSave(cs);

        long st = System.currentTimeMillis();
        CareerManager.getInstance().simulateMatchesInPastTwoYears();
        System.out.println("Simulation ends in " + (System.currentTimeMillis() - st) + " ms");
    }
    
    private static double generateDouble(Random random, double origin, double bound) {
        double d = random.nextDouble();
        return origin + d * (bound - origin);
    }
    
    public enum Hand {
        LEFT("左"),
        RIGHT("右");
        
        private final String shown;
        Hand(String shown) {
            this.shown = shown;
        }

        @Override
        public String toString() {
            return shown;
        }
    }
}
