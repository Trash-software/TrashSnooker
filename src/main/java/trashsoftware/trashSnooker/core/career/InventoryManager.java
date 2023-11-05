package trashsoftware.trashSnooker.core.career;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.CueBrand;
import trashsoftware.trashSnooker.core.cue.CueTip;
import trashsoftware.trashSnooker.core.cue.CueTipBrand;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryManager {
    
    public static final String INVENTORY_FILE = "inventory.json";
    
    private final File file;
    private final Map<String, CueTip> cueTips = new HashMap<>();
    private final Map<String, Cue> cues = new HashMap<>();  // 注意，key是CueBrand的id
    
    private InventoryManager(CareerSave save, File file) {
        this.file = file;
    }
    
    public static InventoryManager createInstance(CareerSave save) {
        File file = new File(save.getDir(), INVENTORY_FILE);
        InventoryManager manager = new InventoryManager(save, file);

        if (file.exists()) {
            try {
                manager.loadFromDisk();
            } catch (JSONException e) {
                EventLogger.error(e);
                manager.initNew(save);
            }
        } else {
            manager.initNew(save);
        }
        
        return manager;
    }
    
    private void loadFromDisk() {
        JSONObject json = DataLoader.loadFromDisk(file.getAbsolutePath());
        load(json);
    }

    /**
     * 创建新存档
     */
    private void initNew(CareerSave save) {
        PlayerPerson human = DataLoader.getInstance().getPlayerPerson(save.getPlayerId());
        List<CueBrand> cues = new ArrayList<>(DataLoader.getInstance().getPublicCues().values());
        cues.addAll(human.getPrivateCues());
        
        for (CueBrand cueBrand : cues) {
            CueTip tip = CueTip.createByCue(cueBrand, 
                    CueTipBrand.getById("stdTip"),
                    save);
            cueTips.put(tip.getInstanceId(), tip);
            Cue cue = Cue.createForCareer(cueBrand, tip, save);
            this.cues.put(cue.getInstanceId(), cue);
        }
        
        saveToDisk();
    }
    
    private void load(JSONObject jsonObject) {
        if (jsonObject.has("inventory")) {
            DataLoader loader = DataLoader.getInstance();
            JSONObject inventory = jsonObject.getJSONObject("inventory");
            
            if (inventory.has("tips")) {
                JSONObject tipsObject = inventory.getJSONObject("tips");
                for (String tipId : tipsObject.keySet()) {
                    try {
                        JSONObject tipObj = tipsObject.getJSONObject(tipId);
                        cueTips.put(tipId, CueTip.fromJson(tipObj));
                    } catch (JSONException e) {
                        EventLogger.error(e);
                    }
                }
            }
            if (inventory.has("cueInstances")) {
                JSONArray jsonArray = inventory.getJSONArray("cueInstances");
                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        JSONObject cueInsObj = jsonArray.getJSONObject(i);
                        Cue cue = Cue.fromJson(cueInsObj, loader, cueTips);
                        cues.put(cue.getInstanceId(), cue);
                    } catch (JSONException e) {
                        EventLogger.error(e);
                    }
                }
            }
        } else {
            throw new JSONException("No inventory root specified");
        }
    }
    
    public JSONObject toJson() {
        JSONObject root = new JSONObject();
        JSONObject inventory = new JSONObject();

        JSONObject tipsRoot = new JSONObject();
        for (Map.Entry<String, CueTip> tipEntry : cueTips.entrySet()) {
            JSONObject cueObj = tipEntry.getValue().toJson();
            tipsRoot.put(tipEntry.getKey(), cueObj);
        }
        inventory.put("tips", tipsRoot);

        JSONArray cueInstanceRoot = new JSONArray();
        for (Cue cue : cues.values()) {
            JSONObject cueObj = cue.toJson();
            cueInstanceRoot.put(cueObj);
        }
        inventory.put("cueInstances", cueInstanceRoot);
        
        root.put("inventory", inventory);
        return root;
    }
    
    public Cue getCueByBrandId(String cueBrandId) {
        // fixme: 一个brand多个instance
        for (Cue cue : cues.values()) {
            if (cue.getBrand().cueId.equals(cueBrandId)) {
                return cue;
            }
        }
        return null;
    }
    
    public Cue getCueByInstanceId(String instanceId) {
        return cues.get(instanceId);
    }
    
    public List<Cue> getAllCues() {
        return new ArrayList<>(cues.values());
    }
    
    public List<CueTip> getAllCueTips() {
        return new ArrayList<>(cueTips.values());
    }
    
    public void saveToDisk() {
        JSONObject jsonObject = toJson();
        DataLoader.saveToDisk(jsonObject, file.getAbsolutePath());
    }
}
