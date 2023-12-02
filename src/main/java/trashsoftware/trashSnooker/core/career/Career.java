package trashsoftware.trashSnooker.core.career;

import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.aiMatch.AiVsAi;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.util.DataLoader;

import java.util.*;

public class Career {

    public static final double[] PERK_RANDOM_RANGE = {0.5, 2.0};
    
    public static final double AI_HELPER_PRECISION_FACTOR = 0.95;

    /**
     * 注意一点：这里的careerManager有可能处于未完全初始化的状态
     */
    protected final CareerManager careerManager;

    private final List<ChampionshipScore> championshipScores = new ArrayList<>();  // 不一定按时间顺序
    private final boolean isHumanPlayer;
    private final transient Map<GameRule, Double> efforts = new HashMap<>();
    private PlayerPerson playerPerson;
    private double handFeel = 0.9;

    Career(PlayerPerson person, boolean isHumanPlayer, CareerManager careerManager) {
        this.playerPerson = person;
        this.isHumanPlayer = isHumanPlayer;
        this.careerManager = careerManager;

        for (GameRule gameRule : GameRule.values()) efforts.put(gameRule, 1.0);
    }

    public static Career createByPerson(PlayerPerson playerPerson, 
                                        boolean isHumanPlayer,
                                        CareerManager careerManager) {
        Career career;
        if (isHumanPlayer) {
            career = new HumanCareer(playerPerson, careerManager);
        } else {
            career = new Career(playerPerson, false, careerManager);
        }
        career.initNew();
        return career;
    }

    public static Career fromJson(JSONObject jsonObject, CareerManager careerManager) {
        String playerId = jsonObject.getString("playerId");
        PlayerPerson playerPerson = DataLoader.getInstance().getPlayerPerson(playerId);
        if (playerPerson == null) {
            // 有career记录，但是PlayerPerson已经被删了
            return null;
        }

        boolean human = jsonObject.getBoolean("human");
        Career career;
        if (human) {
            career = new HumanCareer(playerPerson, careerManager);
        } else {
            career = new Career(playerPerson, false, careerManager);
        }
        career.fillFromJson(jsonObject, careerManager);
        career.validateLevel();

        if (jsonObject.has("handFeel")) {
            career.handFeel = jsonObject.getDouble("handFeel");
        }
        if (jsonObject.has("efforts")) {
            JSONObject effortObj = jsonObject.getJSONObject("efforts");
            for (String key : effortObj.keySet()) {
                GameRule rule;
                if ("SIDE_POCKET".equals(key)) {
                    rule = GameRule.AMERICAN_NINE;
                } else {
                    rule = GameRule.valueOf(key);
                }
                double effort = effortObj.getDouble(key);
                career.efforts.put(rule, effort);
            }
        }

        JSONArray scoreArr = jsonObject.getJSONArray("scores");
        for (int i = 0; i < scoreArr.length(); i++) {
            ChampionshipScore score = ChampionshipScore.fromJson(scoreArr.getJSONObject(i));
            career.championshipScores.add(score);
        }

        return career;
    }

    public JSONObject toJsonObject() {
        JSONObject out = new JSONObject();
        out.put("playerId", playerPerson.getPlayerId());
        out.put("human", isHumanPlayer);

        JSONArray scoreArr = new JSONArray();
        for (ChampionshipScore score : championshipScores) {
            JSONObject scoreObj = score.toJsonObject();
            scoreArr.put(scoreObj);
        }

        JSONObject effortsObj = new JSONObject();
        for (Map.Entry<GameRule, Double> eff : efforts.entrySet()) {
            effortsObj.put(eff.getKey().name(), eff.getValue());
        }
        out.put("efforts", effortsObj);
        out.put("scores", scoreArr);
        out.put("handFeel", handFeel);

        putExtraInJson(out);

        return out;
    }

    /*
    这几个都是给override用的
     */
    protected void initNew() {
    }

    protected void fillFromJson(JSONObject jsonObject, CareerManager careerManager) {
    }

    protected void putExtraInJson(JSONObject out) {
    }

    protected void validateLevel() {
    }

    public CareerManager getCareerManager() {
        return careerManager;
    }

    public double getEffort(GameRule rule) {
        return Objects.requireNonNullElse(efforts.get(rule), 1.0);
    }

    public double getHandFeel() {
        return handFeel;
    }

    public double getHandFeelEffort(GameRule rule) {
        return getHandFeel() * getEffort(rule) * getPlayerPerson().skillLevelOfGame(rule);
    }

    public void updateEffort(GameRule rule, double effort) {
        efforts.put(rule, effort);

        if (CareerManager.LOG && effort != 1.0) {
            System.out.println(rule.name() + " effort: " + playerPerson.getPlayerId() + " " + effort);
        }
    }

    public void updateHandFeel() {
        double handFeelChange = (Math.random() - 0.5) * 0.05;
        this.handFeel += handFeelChange;
        if (this.handFeel > 1.15) this.handFeel = 1.15;
        else if (this.handFeel < 0.85) this.handFeel = 0.85;

        if (CareerManager.LOG) {
            System.out.println("Hand feel: " + playerPerson.getPlayerId() + " " + handFeel);
        }
    }

    public PlayerPerson getPlayerPerson() {
        return playerPerson;
    }

    public void setPlayerPerson(PlayerPerson playerPerson) {
        this.playerPerson = playerPerson;
    }

    public void addChampionshipScore(ChampionshipScore score) {
        championshipScores.add(score);
    }

    public List<ChampionshipScore> getChampionshipScores() {
        return championshipScores;
    }

    public boolean isHumanPlayer() {
        return isHumanPlayer;
    }

    @Override
    public String toString() {
        return "Career{" +
                "playerPerson=" + playerPerson.getPlayerId() +
                ", isHumanPlayer=" + isHumanPlayer +
                '}';
    }
}
