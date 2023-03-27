package trashsoftware.trashSnooker.core.career.championship;

import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.career.*;
import trashsoftware.trashSnooker.util.EventLogger;

import java.util.*;

public class SnookerChampionship extends Championship {
    
    protected final NavigableSet<SnookerBreakScore> breaksOver50 = new TreeSet<>();

    public SnookerChampionship(ChampionshipData data, Calendar timestamp) {
        super(data, timestamp);
    }
    
    public List<SnookerBreakScore> getTopNBreaks(int n) {
        List<SnookerBreakScore> res = new ArrayList<>();
        for (SnookerBreakScore sbs : breaksOver50) {
            res.add(sbs);
            if (res.size() >= n) break;
        }
        return res;
    }
    
    protected Map<ChampionshipScore.Rank, List<String>> extraAwardsMap() {
        Map<ChampionshipScore.Rank, List<String>> res = new HashMap<>();
        res.put(ChampionshipScore.Rank.MAXIMUM, new ArrayList<>());
        res.put(ChampionshipScore.Rank.BEST_SINGLE, new ArrayList<>());
        if (breaksOver50.isEmpty()) return res;
        
        SnookerBreakScore best = null;
        for (SnookerBreakScore sbs : breaksOver50) {
            if (best != null && sbs.score != best.score) {
                break;
            }
            res.get(ChampionshipScore.Rank.BEST_SINGLE).add(sbs.playerId);
            if (sbs.score >= 147) res.get(ChampionshipScore.Rank.MAXIMUM).add(sbs.playerId);
            if (best == null) best = sbs;
        }
        return res;
    }
    
    public void updateBreakScore(String playerId, 
                                 ChampionshipStage matchStage, 
                                 int score,
                                 boolean isSim,
                                 String matchId,
                                 int numFrame) {
        if (score >= 50) {
            breaksOver50.add(new SnookerBreakScore(playerId, 
                    score, 
                    matchStage, 
                    isSim,
                    matchId,
                    numFrame));
        }
    }

    protected void loadExtraInfo(JSONObject root) {
        if (root.has("snookerBreaks")) {
            JSONArray array = root.getJSONArray("snookerBreaks");
            for (Object obj : array) {
                try {
                    breaksOver50.add(SnookerBreakScore.fromJson((JSONObject) obj));
                } catch (ClassCastException cce) {
                    EventLogger.error(cce);
                }
            }
        }
    }

    @Override
    protected JSONObject toJson() {
        JSONObject json = super.toJson();
        
        JSONArray breaksRec = new JSONArray();
        for (SnookerBreakScore sbs : breaksOver50) {
            breaksRec.put(sbs.toJson());
        }
        json.put("snookerBreaks", breaksRec);
        
        return json;
    }

    @Override
    protected List<TourCareer> getParticipantsByRank(boolean playerJoin, boolean humanQualified) {
        return CareerManager.getInstance().participants(
                data,
                playerJoin,
                humanQualified
        );
    }
}
