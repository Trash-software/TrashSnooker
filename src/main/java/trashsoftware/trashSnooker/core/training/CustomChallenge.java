package trashsoftware.trashSnooker.core.training;

import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.metrics.GameRule;

import java.util.ArrayList;
import java.util.List;

public class CustomChallenge extends Challenge {
    
    private final List<BallSchema> ballSchemas = new ArrayList<>();
    
    private CustomChallenge(GameRule rule) {
        super(rule, TrainType.CUSTOM);
    }
    
    public static CustomChallenge fromJson(GameRule rule, JSONObject schema) {
        CustomChallenge customChallenge = new CustomChallenge(rule);
        JSONArray balls = schema.getJSONArray("balls");
        for (int i = 0; i < balls.length(); i++) {
            JSONObject ballObj = balls.getJSONObject(i);
            BallSchema ballSchema = new BallSchema(
                    ballObj.getInt("value"),
                    ballObj.getDouble("unitX"),
                    ballObj.getDouble("unitY")
            );
            customChallenge.ballSchemas.add(ballSchema);
        }
        return customChallenge;
    }

    public List<BallSchema> getBallSchemas() {
        return ballSchemas;
    }

    public static class BallSchema {
        public final int value;
        public final double unitX;
        public final double unitY;
        
        BallSchema(int value, double unitX, double unitY) {
            this.value = value;
            this.unitX = unitX;
            this.unitY = unitY;
        }
    }
}
