package trashsoftware.trashSnooker.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class PermanentCounters {

    private static PermanentCounters instance;

    private final File file;
    private int cueInstanceCounter;
    private int tipInstanceCounter;
    private int fastGamesCounter;

    private PermanentCounters() {
        file = new File(DataLoader.COUNTERS_FILE);
    }

    public static PermanentCounters getInstance() {
        if (instance == null) {
            instance = new PermanentCounters();
            instance.load();
        }
        return instance;
    }

    public int nextCueInstance() {
        int rtn = cueInstanceCounter++;
        save();
        return rtn;
    }

    public int nextTipInstance() {
        int rtn = tipInstanceCounter++;
        save();
        return rtn;
    }

    public int nextFastGame() {
        int rtn = fastGamesCounter++;
        save();
        return rtn;
    }

    public int currentFastGame() {
        return fastGamesCounter;
    }

    private void load() {
        if (file.exists()) {
            try {
                JSONObject jsonObject = DataLoader.loadFromDisk(file.getAbsolutePath());
                JSONObject root = jsonObject.getJSONObject("counters");
                cueInstanceCounter = root.optInt("cueInstanceCounter", 0);
                tipInstanceCounter = root.optInt("tipInstanceCounter", 0);
                fastGamesCounter = root.optInt("fastGameCounter", 0);
            } catch (JSONException e) {
                EventLogger.error(e);
                save();
            }
        }
    }

    public void save() {
        JSONObject json = new JSONObject();
        JSONObject root = new JSONObject();

        root.put("cueInstanceCounter", cueInstanceCounter);
        root.put("tipInstanceCounter", tipInstanceCounter);
        root.put("fastGameCounter", fastGamesCounter);
        
        json.put("counters", root);
        DataLoader.saveToDisk(json, file.getAbsolutePath());
    }
}
