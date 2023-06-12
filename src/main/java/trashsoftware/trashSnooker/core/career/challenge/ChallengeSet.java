package trashsoftware.trashSnooker.core.career.challenge;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.metrics.*;
import trashsoftware.trashSnooker.core.phy.TableCloth;
import trashsoftware.trashSnooker.core.training.Challenge;
import trashsoftware.trashSnooker.core.training.CustomChallenge;
import trashsoftware.trashSnooker.core.training.TrainType;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.Util;

public class ChallengeSet {
    
    private String id;
    private String name;
    private GameValues gameValues;
    private TableCloth cloth;
    private int exp;
    
    private ChallengeSet() {
    }
    
    public static ChallengeSet fromJson(JSONObject object) {
        ChallengeSet challengeSet = new ChallengeSet();

        challengeSet.id = object.getString("id");
        challengeSet.name = DataLoader.getStringOfLocale(object.get("name"));
        challengeSet.cloth = new TableCloth(TableCloth.Goodness.GOOD, TableCloth.Smoothness.NORMAL);

        String ruleKey = Util.toAllCapsUnderscoreCase(object.getString("rule"));
        TableMetrics.TableBuilderFactory tableMetricsFactory =
                TableMetrics.TableBuilderFactory.valueOf(Util.toAllCapsUnderscoreCase(object.getString("table")));
        TableMetrics tableMetrics = tableMetricsFactory
                .create()
                .pocketDifficulty(PocketDifficulty.valueOf(tableMetricsFactory, object.getString("pocketDifficulty")))
                .holeSize(PocketSize.valueOf(tableMetricsFactory, object.getString("pocketSize")))
                .build();

        GameRule rule = GameRule.valueOf(ruleKey);
        BallMetrics ballMetrics = GameRule.getDefaultBall(rule);

        GameValues values = new GameValues(rule, tableMetrics, ballMetrics);
        TrainType trainType = TrainType.valueOf(Util.toAllCapsUnderscoreCase(object.getString("type")));
        Challenge challenge;
        if (trainType == TrainType.CUSTOM) {
            JSONObject schema = object.getJSONObject("schema");
            challenge = CustomChallenge.fromJson(rule, schema);
        } else {
            challenge = new Challenge(rule, trainType);
        }
        
        values.setTrain(trainType, challenge);
        
        challengeSet.gameValues = values;
        
        challengeSet.exp = object.getInt("exp");
        return challengeSet;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public GameValues getGameValues() {
        return gameValues;
    }

    public int getExp() {
        return exp;
    }

    public TableCloth getCloth() {
        return cloth;
    }
}
