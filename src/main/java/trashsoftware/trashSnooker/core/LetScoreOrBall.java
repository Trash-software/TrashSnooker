package trashsoftware.trashSnooker.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.LetBall;
import trashsoftware.trashSnooker.fxml.App;

import java.util.*;

public class LetScoreOrBall {
    public static final LetScoreOrBall NOT_LET = new LetScoreOrBall();

    @Override
    public String toString() {
        if (this == NOT_LET) return App.getStrings().getString("notLetScoreOrBall");
        return super.toString();
    }
    
    public static LetScoreOrBall fromJson(JSONObject jsonObject) {
        String type = jsonObject.getString("type");
        if ("notLet".equals(type)) return NOT_LET;
        else if ("score".equals(type)) {
            int score = jsonObject.getInt("score");
            return new LetScoreFace(score);
        } else if ("ball".equals(type)) {
            JSONObject lets = jsonObject.getJSONObject("lets");
            Map<LetBall, Integer> letBalls = new HashMap<>();
            for (String letKey : lets.keySet()) {
                letBalls.put(LetBall.valueOf(letKey), lets.getInt(letKey));
            }
            return new LetBallFace(letBalls);
        } else {
            throw new JSONException("Unknown let type: " + type);
        }
    }

    public JSONObject toJson() {
        if (this == NOT_LET) {
            JSONObject json = new JSONObject();
            json.put("type", "notLet");
            return json;
        }
        throw new RuntimeException("Should not call this");
    }

    public static class LetScoreFace extends LetScoreOrBall {
        public final int score;

        public LetScoreFace(int score) {
            this.score = score;
        }

        @Override
        public String toString() {
            return String.valueOf(score);
        }

        @Override
        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            json.put("type", "score");
            json.put("score", score);
            return json;
        }
    }

    public static class LetBallFace extends LetScoreOrBall {
        public final Map<LetBall, Integer> letBalls;

        public LetBallFace(Map<LetBall, Integer> letBalls) {
            this.letBalls = letBalls;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            List<LetBall> keys = new ArrayList<>(letBalls.keySet());
            Collections.sort(keys);
            for (LetBall key : keys) {
                sb.append(key.getShown(letBalls.get(key), App.getStrings()));
            }
            return sb.toString();
        }

        @Override
        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            json.put("type", "ball");
            JSONObject lets = new JSONObject();
            for (Map.Entry<LetBall, Integer> let : letBalls.entrySet()) {
                lets.put(let.getKey().name(), let.getValue());
            }
            json.put("lets", lets);
            return json;
        }
    }
}
