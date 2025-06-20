package trashsoftware.trashSnooker.core;

import org.json.JSONObject;
import trashsoftware.trashSnooker.util.Util;

import java.util.*;

public class FoulInfo {
    private boolean miss = false;
    private boolean illegal = false;  // 失机/违例一类的东西，且在本不犯规的前提下
    private String headerReason;  // 为null时应该就是“犯规”
    private final Map<String, Integer> foulReasonAndScore = new LinkedHashMap<>();

    public void addFoul(String reason, int score, boolean miss) {
        this.foulReasonAndScore.put(reason, score);
        this.miss = this.miss || miss;
    }

    public void addFoul(String reason, boolean miss) {
        addFoul(reason, 1, miss);
    }

    public void addFoul(String reason) {
        addFoul(reason, 1, false);
    }

    public void setHeaderReason(String headerReason) {
        this.headerReason = headerReason;
    }

    public String getHeaderReason(ResourceBundle strings) {
        return headerReason == null ? strings.getString("foul") : headerReason;
    }

    public boolean isFoul() {
        return !foulReasonAndScore.isEmpty();
    }

    public boolean isMiss() {
        return miss;
    }

    public void setMiss(boolean miss) {
        this.miss = miss;
    }

    public void setIllegal(boolean illegal) {
        this.illegal = illegal;
    }

    public boolean isIllegal() {
        return illegal;
    }

    public String getAllReasons() {
        return String.join("\n", foulReasonAndScore.keySet());
    }
    
    public int getFoulScore() {
        int maxFoul = 0;
        for (Map.Entry<String, Integer> entry : foulReasonAndScore.entrySet()) {
            if (entry.getValue() > maxFoul) {
                maxFoul = entry.getValue();
            }
        }
        return maxFoul;
    }
    
    public Integer removeFoul(String reason) {
        return foulReasonAndScore.remove(reason);
    }
    
    public JSONObject toJson() {
        JSONObject out = new JSONObject();
        out.put("miss", miss);
        out.put("illegal", illegal);
        out.put("headerReason", headerReason);
        JSONObject reasons = Util.mapToJson(foulReasonAndScore);
        out.put("foulReasonAndScore", reasons);
        
        return out;
    }
    
    public static FoulInfo fromJson(JSONObject json) {
        FoulInfo foulInfo = new FoulInfo();
        foulInfo.miss = json.getBoolean("miss");
        foulInfo.illegal = json.getBoolean("illegal");
        foulInfo.headerReason = json.optString("headerReason", null);
        JSONObject reasons = json.getJSONObject("foulReasonAndScore");
        for (String key : reasons.keySet()) {
            foulInfo.foulReasonAndScore.put(key, reasons.getInt(key));
        }
        
        return foulInfo;
    }
}
