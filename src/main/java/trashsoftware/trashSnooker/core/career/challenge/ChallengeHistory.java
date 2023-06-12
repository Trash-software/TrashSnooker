package trashsoftware.trashSnooker.core.career.challenge;

import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Util;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class ChallengeHistory {
    public final String challengeId;
    List<Record> scores = new ArrayList<>();
    boolean completed;

    public ChallengeHistory(String challengeId) {
        this.challengeId = challengeId;
        this.completed = false;
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
                if (record.success) ch.completed = true;
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
            rec.put("success", record.success);
            his.put(rec);
        }
        object.put("history", his);
        return object;
    }

    /**
     * @return 这是不是第一次成功
     */
    public boolean newComplete(boolean newSuccess, int score) {
        boolean old = this.completed;
        
        scores.add(new Record(
                new Date(),
                score,
                newSuccess
        ));
        
        this.completed |= newSuccess;
        return newSuccess && !old;
    }

    public boolean isCompleted() {
        return completed;
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
        public final boolean success;

        Record(Date finishTime, int score, boolean success) {
            this.finishTime = finishTime;
            this.score = score;
            this.success = success;
        }
    }
}
