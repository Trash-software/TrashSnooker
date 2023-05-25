package trashsoftware.trashSnooker.core.career.challenge;

import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.util.DataLoader;

import java.util.*;

public class ChallengeManager {

    public static final String CHALLENGE_FILE = "data/challenges.json";
    private static ChallengeManager instance;
    
    private final List<ChallengeSet> orderedChallenges = new ArrayList<>();
    private final Map<String, ChallengeSet> challengeSetMap = new HashMap<>();

    private ChallengeManager() {
        loadChallenges();
    }
    
    private void loadChallenges() {
        JSONObject root = DataLoader.loadFromDisk(CHALLENGE_FILE);
        if (root.has("challenges")) {
            JSONArray array = root.getJSONArray("challenges");
            for (Object cha : array) {
                JSONObject chaObject = (JSONObject) cha;
                ChallengeSet challengeSet = ChallengeSet.fromJson(chaObject);
                orderedChallenges.add(challengeSet);
                challengeSetMap.put(challengeSet.getId(), challengeSet);
            }
        }
    }

    public List<ChallengeSet> getAllChallenges() {
        return orderedChallenges;
    }

    public ChallengeSet getById(String id) {
        return challengeSetMap.get(id);
    }

    public static ChallengeManager getInstance() {
        if (instance == null) {
            instance = new ChallengeManager();
        }
        return instance;
    }
}
