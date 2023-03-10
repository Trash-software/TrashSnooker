package trashsoftware.trashSnooker.core.career;

import javafx.fxml.FXML;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.util.DataLoader;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CareerSave {
    private static final String INFO_NAME = "career_info.ini";
    
    private final File dir;
    private final File infoFile;
    private final String playerId;
    private Map<String, String> cacheInfo;
    
    public CareerSave(File dir) {
        this.dir = dir;
        this.playerId = dir.getName();
        
        this.infoFile = new File(dir, INFO_NAME);
        
        updateCacheInfo();
    }
    
    public void updateCacheInfo() {
        this.cacheInfo = CareerManager.readCacheInfo(infoFile);
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
    
    @FXML
    public String getLevel() {
        return cacheInfo.getOrDefault("level", "");
    }

    public File getDir() {
        return dir;
    }

    public String getPlayerId() {
        return playerId;
    }

    public File getInfoFile() {
        return infoFile;
    }
}
