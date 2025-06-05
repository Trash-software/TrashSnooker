package trashsoftware.trashSnooker.core.attempt;

import org.json.JSONObject;

public class AttemptBase {

    public final CueType type;
    private boolean success;
    
    public AttemptBase(CueType type) {
        this.type = type;
    }

    AttemptBase(CueType type, boolean initSuccess) {
        this.type = type;
        this.success = initSuccess;
    }
    
    public static AttemptBase fromJson(JSONObject json) {
        return new AttemptBase(
                CueType.valueOf(json.getString("type")),
                json.getBoolean("success")
        );
    }
    
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("type", type.name());
        json.put("success", success);
        return json;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}
