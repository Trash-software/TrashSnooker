package trashsoftware.trashSnooker.core.career.challenge;

import org.json.JSONObject;
import trashsoftware.trashSnooker.util.Util;

import java.util.HashMap;
import java.util.Map;

public class ChallengeReward {
    
    private final Map<Type, Integer> rewards = new HashMap<>();
    
    private ChallengeReward() {
    }
    
    static ChallengeReward fromJson(JSONObject json) {
        ChallengeReward cr = new ChallengeReward();
        for (String key : json.keySet()) {
            Type type = Type.valueOf(Util.toAllCapsUnderscoreCase(key));
            Integer value = json.getInt(key);
            cr.rewards.put(type, value);
        }
        return cr;
    }

    public Map<Type, Integer> getRewards() {
        return rewards;
    }
    
    public int getBy(Type type) {
        return rewards.getOrDefault(type, 0);
    }
    
    public int getExp() {
        return rewards.getOrDefault(Type.EXP, 0);
    }

    public int getMoney() {
        return rewards.getOrDefault(Type.MONEY, 0);
    }

    public enum Type {
        EXP,
        MONEY
    }
}
