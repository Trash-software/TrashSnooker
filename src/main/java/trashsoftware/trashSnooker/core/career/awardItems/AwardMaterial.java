package trashsoftware.trashSnooker.core.career.awardItems;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public abstract class AwardMaterial {
    
    public static List<AwardMaterial> fromJsonList(JSONObject object) {
        List<AwardMaterial> result = new ArrayList<>();
        for (String key : object.keySet()) {
            switch (key) {
                case "perks":
                    int pk = object.getInt("perks");
                    result.add(new AwardPerk(pk));
                    break;
            }
        }
        return result;
    }

    public abstract void putToJson(JSONObject object);
}
