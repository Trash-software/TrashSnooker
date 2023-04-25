package trashsoftware.trashSnooker.core;

import java.util.*;

public class FoulInfo {
    private boolean foul = false;
    private boolean miss = false;
    private final Map<String, Integer> foulReasonAndScore = new LinkedHashMap<>();

    public void addFoul(String reason, int score, boolean miss) {
        this.foulReasonAndScore.put(reason, score);
        this.foul = true;
        this.miss |= miss;
    }

    public boolean isFoul() {
        return foul;
    }

    public boolean isMiss() {
        return miss;
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

    public enum Reason {
        MISS,
        WHITE_POTTED,
        NO_TOUCH_CUSHION
    }
}
