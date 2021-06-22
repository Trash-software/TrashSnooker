package trashsoftware.trashSnooker.util;

import org.json.JSONObject;

public class RecordItem {

    private int highestBreak;

    public RecordItem() {

    }

    public RecordItem(int highestBreak) {
        this.highestBreak = highestBreak;
    }

    public boolean updateHighestBreak(int newHighestBreak) {
        if (newHighestBreak > highestBreak) {
            highestBreak = newHighestBreak;
            return true;
        } else {
            return false;
        }
    }

    public int getHighestBreak() {
        return highestBreak;
    }

    public static RecordItem fromJson(JSONObject object) {
        RecordItem recordItem = new RecordItem();
        if (object.has("highestBreak")) {
            recordItem.updateHighestBreak(object.getInt("highestBreak"));
        }
        return recordItem;
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        object.put("highestBreak", highestBreak);
        return object;
    }
}
