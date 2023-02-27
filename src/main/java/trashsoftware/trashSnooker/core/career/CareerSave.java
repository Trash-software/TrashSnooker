package trashsoftware.trashSnooker.core.career;

import javafx.fxml.FXML;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.util.DataLoader;

import java.io.File;
import java.io.IOException;

public class CareerSave {
    
    private final File dir;
    private final String playerId;
    
    public CareerSave(File dir) {
        this.dir = dir;
        this.playerId = dir.getName();
    }
    
    public void create() throws IOException {
        if (!this.dir.exists()) {
            if (!this.dir.mkdirs()) {
                throw new IOException();
            }
        }
    }
    
    @FXML
    public String getPlayerName() {
        PlayerPerson playerPerson = DataLoader.getInstance().getPlayerPerson(playerId);
        if (playerPerson != null) return playerPerson.getName();
        else return "";
    }

    public File getDir() {
        return dir;
    }

    public String getPlayerId() {
        return playerId;
    }
}
