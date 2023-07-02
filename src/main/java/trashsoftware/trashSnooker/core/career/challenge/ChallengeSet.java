package trashsoftware.trashSnooker.core.career.challenge;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.metrics.*;
import trashsoftware.trashSnooker.core.phy.TableCloth;
import trashsoftware.trashSnooker.core.training.Challenge;
import trashsoftware.trashSnooker.core.training.CustomChallenge;
import trashsoftware.trashSnooker.core.training.TrainType;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.Util;

import java.util.*;

public class ChallengeSet {

    private final Map<RewardCondition, ChallengeReward> conditionRewards = new HashMap<>();
    private String id;
    private String name;
    private GameValues gameValues;
    private TableCloth cloth;

    private ChallengeSet() {
    }

    public static ChallengeSet fromJson(JSONObject object) {
        ChallengeSet challengeSet = new ChallengeSet();

        challengeSet.id = object.getString("id");
        challengeSet.name = DataLoader.getObjectOfLocale(object.get("name"));
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

        JSONObject rwd = object.getJSONObject("rewards");
        for (String key : rwd.keySet()) {
            RewardCondition condition = RewardCondition.parse(key);
            ChallengeReward reward = ChallengeReward.fromJson(rwd.getJSONObject(key));
            challengeSet.conditionRewards.put(condition, reward);
        }
        return challengeSet;
    }

    public static int getTotal(Collection<ChallengeReward> rewards, ChallengeReward.Type type) {
        int sum = 0;
        for (ChallengeReward cr : rewards) {
            sum += cr.getBy(type);
        }
        return sum;
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

    public Map<RewardCondition, ChallengeReward> getConditionRewards() {
        return conditionRewards;
    }

    public Map<RewardCondition, ChallengeReward> getFulfilledBy(List<ChallengeHistory.Record> records) {
        Map<RewardCondition, ChallengeReward> result = new HashMap<>();
        for (ChallengeHistory.Record record : records) {
            for (Map.Entry<RewardCondition, ChallengeReward> entry : conditionRewards.entrySet()) {
                if (entry.getKey().fulfilled(record)) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    public int getTotal(ChallengeReward.Type type) {
        return getTotal(conditionRewards.values(), type);
    }

    public TableCloth getCloth() {
        return cloth;
    }
}
