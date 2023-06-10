package trashsoftware.trashSnooker.core.career.awardItems;

import org.json.JSONObject;

public class AwardPerk extends AwardMaterial {
    private final int nPerks;

    public AwardPerk(int nPerks) {
        this.nPerks = nPerks;
    }
    
    @Override
    public void putToJson(JSONObject object) {
        object.put("perks", nPerks);
    }

    public int getPerks() {
        return nPerks;
    }

    @Override
    public String toString() {
        return String.valueOf(nPerks);
    }
}
