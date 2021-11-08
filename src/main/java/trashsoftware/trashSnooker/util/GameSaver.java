package trashsoftware.trashSnooker.util;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.EntireGame;

import java.io.File;

public class GameSaver {
    
    public static final String SAVE_PATH = "user" + File.separator + "saved.json";
    
    public static boolean hasSavedGame() {
        return new File(SAVE_PATH).exists();
    }
    
    public static void save(EntireGame game) {

        JSONObject object = new JSONObject(game);
        object.put("startTime", game.getStartTimeSqlString());
        object.put("totalFrames", game.totalFrames);
    }
    
    public static EntireGame load() {
        if (new File(SAVE_PATH).exists()) {
            JSONObject object = Recorder.loadFromDisk(SAVE_PATH);
            
            
        }
        return null;
    }
}
