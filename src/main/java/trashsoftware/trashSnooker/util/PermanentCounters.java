package trashsoftware.trashSnooker.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class PermanentCounters {
    
    private static PermanentCounters instance;

    private final File file;
    private int cueInstanceCounter;
    private int tipInstanceCounter;
    
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
    
    private void load() {
        if (file.exists()) {
            try {
                JSONObject jsonObject = DataLoader.loadFromDisk(file.getAbsolutePath());
                JSONObject root = jsonObject.getJSONObject("counters");
                cueInstanceCounter = root.getInt("cueInstanceCounter");
                tipInstanceCounter = root.getInt("tipInstanceCounter");
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
        
        json.put("counters", root);
        DataLoader.saveToDisk(json, file.getAbsolutePath());
    }
}
