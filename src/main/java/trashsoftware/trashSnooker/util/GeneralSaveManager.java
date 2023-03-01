package trashsoftware.trashSnooker.util;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.EntireGame;

import java.io.File;

public class GeneralSaveManager {

    public static final File GENERAL_SAVE = new File("user/save.json");
    
    private static GeneralSaveManager instance;
    private EntireGame save;
    
    private GeneralSaveManager() {
        save = EntireGame.loadFrom(GENERAL_SAVE);
    }

    public static GeneralSaveManager getInstance() {
        if (instance == null) {
            instance = new GeneralSaveManager();
        }
        return instance;
    }

    public boolean hasSavedGame() {
        return save != null && !save.isFinished();
    }
    
    public void save(EntireGame game) {
        save = game;
        JSONObject json = game.toJson();
        Util.writeJson(json, GENERAL_SAVE);
    }

    public EntireGame getSave() {
        return save;
    }
}
