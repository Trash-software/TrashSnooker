package trashsoftware.trashSnooker.core.career.challenge;

import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Util;

import java.text.ParseException;
import java.util.*;

public class ChallengeHistory {
    public final String challengeId;
    List<Record> scores = new ArrayList<>();

    public ChallengeHistory(String challengeId) {
        this.challengeId = challengeId;
    }

    public static ChallengeHistory fromJson(JSONObject jsonObject) {
        String cid = jsonObject.getString("id");
        ChallengeHistory ch = new ChallengeHistory(cid);
        JSONArray scores = jsonObject.getJSONArray("history");
        for (int i = 0; i < scores.length(); i++) {
            JSONObject recObj = scores.getJSONObject(i);
            try {
                Record record = new Record(
                        Util.TIME_FORMAT_SEC.parse(recObj.getString("time")),
                        recObj.getInt("score"),
                        recObj.getBoolean("success")
                );
                ch.scores.add(record);
            } catch (ParseException e) {
                EventLogger.error(e);
            }
        }
        return ch;
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        object.put("id", challengeId);
        JSONArray his = new JSONArray();
        for (Record record : scores) {
            JSONObject rec = new JSONObject();
            rec.put("time", Util.TIME_FORMAT_SEC.format(record.finishTime));
            rec.put("score", record.score);
            rec.put("success", record.clearedAll);
            his.put(rec);
        }
        object.put("history", his);
        return object;
    }

    /**
     * @return 第一次成功的条目
     */
    public Map<RewardCondition, ChallengeReward> newComplete(ChallengeSet challengeSet, boolean newClearance, int score) {
        Map<RewardCondition, ChallengeReward> alreadyCompleted = challengeSet.getFulfilledBy(scores);

        Map<RewardCondition, ChallengeReward> newFulfills = new HashMap<>();
        Record nr = new Record(
                new Date(),
                score,
                newClearance
        );
        for (Map.Entry<RewardCondition, ChallengeReward> entry : challengeSet.getConditionRewards().entrySet()) {
            RewardCondition cond = entry.getKey();
            if (!alreadyCompleted.containsKey(cond)) {
                if (cond.fulfilled(nr)) {
                    newFulfills.put(cond, entry.getValue());
                }
            }
        }
        
        scores.add(nr);
        
        return newFulfills;
    }
    
    public int getCompleted(ChallengeSet challengeSet, ChallengeReward.Type type) {
        Map<RewardCondition, ChallengeReward> alreadyCompleted = challengeSet.getFulfilledBy(scores);
        
        return ChallengeSet.getTotal(alreadyCompleted.values(), type);
    }
    
    public int getBestScore() {
        return scores.isEmpty() ? 0 : scores.stream().max(Comparator.comparingInt(a -> a.score)).get().score;
    }

    public List<Record> getScores() {
        return scores;
    }

    public static class Record {
        public final Date finishTime;
        public final int score;
        public final boolean clearedAll;

        Record(Date finishTime, int score, boolean clearedAll) {
            this.finishTime = finishTime;
            this.score = score;
            this.clearedAll = clearedAll;
        }
    }
}
