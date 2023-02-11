package trashsoftware.trashSnooker.core.career;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.aiMatch.SnookerAiVsAi;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.util.DataLoader;

import java.util.*;

public class Career {

    private final List<ChampionshipScore> championshipScores = new ArrayList<>();  // 不一定按时间顺序
    private final boolean isHumanPlayer;
    private PlayerPerson playerPerson;
    private int cumulatedPerks = 0;
    private int usedPerks = 0;
    private double handFeel = 0.9;
    private final transient Map<GameRule, Double> efforts = new HashMap<>();

    private Career(PlayerPerson person, boolean isHumanPlayer) {
        this.playerPerson = person;
        this.isHumanPlayer = isHumanPlayer;
        
        for (GameRule gameRule : GameRule.values()) efforts.put(gameRule, 1.0);
    }

    public static Career createByPerson(PlayerPerson playerPerson, boolean isHumanPlayer) {
        Career career = new Career(playerPerson, isHumanPlayer);
        career.cumulatedPerks = CareerManager.INIT_PERKS;
        return career;
    }

    public static Career fromJson(JSONObject jsonObject) {
        String playerId = jsonObject.getString("playerId");
        PlayerPerson playerPerson = DataLoader.getInstance().getPlayerPerson(playerId);

        Career career = new Career(playerPerson, jsonObject.getBoolean("human"));
        career.cumulatedPerks = jsonObject.getInt("cumulatedPerks");
        career.usedPerks = jsonObject.getInt("usedPerks");
        
        if (jsonObject.has("handFeel")) {
            career.handFeel = jsonObject.getDouble("handFeel");
        }
        if (jsonObject.has("efforts")) {
            JSONObject effortObj = jsonObject.getJSONObject("efforts");
            for (String key : effortObj.keySet()) {
                GameRule rule = GameRule.valueOf(key);
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
            scoreArr.put(score.toJsonObject());
        }
        
        JSONObject effortsObj = new JSONObject();
        for (Map.Entry<GameRule, Double> eff : efforts.entrySet()) {
            effortsObj.put(eff.getKey().name(), eff.getValue());
        }
        out.put("efforts", effortsObj);
        out.put("scores", scoreArr);
        out.put("handFeel", handFeel);
        out.put("cumulatedPerks", cumulatedPerks);
        out.put("usedPerks", usedPerks);

        return out;
    }

    public double getEffort(GameRule rule) {
        return Objects.requireNonNullElse(efforts.get(rule), 1.0);
    }

    public double getHandFeel() {
        return handFeel;
    }
    
    public double getHandFeelEffort(GameRule rule) {
        return getHandFeel() * getEffort(rule);
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
        if (this.handFeel > 1.0) this.handFeel = 1.0;
        else if (this.handFeel < 0.8) this.handFeel = 0.8;
        
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
        int perks = 0;
        for (ChampionshipScore.Rank rank : score.ranks) {
            Integer perk = score.data.getPerks().get(rank);
            if (perk != null) {
                // 因为预赛一般没有奖金
                perks += perk;
            }
        }
        cumulatedPerks += perks;
    }

    public List<ChampionshipScore> getChampionshipScores() {
        return championshipScores;
    }

    public boolean isHumanPlayer() {
        return isHumanPlayer;
    }

    public int getCumulatedPerks() {
        return cumulatedPerks;
    }

    public void usePerk(int used) {
        usedPerks += used;

        CareerManager.getInstance().saveToDisk();
    }

    public int getUsedPerks() {
        return usedPerks;
    }

    public int getAvailablePerks() {
        return cumulatedPerks - usedPerks;
    }

    @Override
    public String toString() {
        return "Career{" +
                "playerPerson=" + playerPerson.getPlayerId() +
                ", isHumanPlayer=" + isHumanPlayer +
                '}';
    }

    public static class CareerWithAwards implements Comparable<CareerWithAwards> {
        public final Career career;
        public final GameRule type;
        private int recentAwards;
        private int totalAwards;

        public CareerWithAwards(GameRule type, Career career, Calendar timestamp) {
            this.career = career;
            this.type = type;

            calculateAwards(timestamp);
        }

        private void calculateAwards(Calendar timestamp) {
            recentAwards = 0;
            totalAwards = 0;
            Calendar startTime = Calendar.getInstance();
            startTime.set(timestamp.get(Calendar.YEAR) - 2,
                    timestamp.get(Calendar.MONTH),
                    timestamp.get(Calendar.DAY_OF_MONTH) - 1);  // 上上届要算

            for (ChampionshipScore score : career.getChampionshipScores()) {
                int awards = 0;
                if (score.data.type == type) {
                    for (ChampionshipScore.Rank rank : score.ranks) {
                        awards += score.data.getAwardByRank(rank);
                    }
                    totalAwards += awards;
                    if (score.data.ranked) {
                        if (startTime.before(score.timestamp)) {
                            recentAwards += awards;
                        }
                    }
                }
            }
        }

        public int getRecentAwards() {
            return recentAwards;
        }

        public int getTotalAwards() {
            return totalAwards;
        }

        private double winScore() {
            return SnookerAiVsAi.playerSnookerWinningScore(
                    career.getPlayerPerson(),
                    career.getPlayerPerson().getAiPlayStyle(),
                    PlayerPerson.ReadableAbility.fromPlayerPerson(career.getPlayerPerson()),
                    false
            );
        }

        @Override
        public int compareTo(@NotNull Career.CareerWithAwards o) {
            int awdCmp = -Integer.compare(this.recentAwards, o.recentAwards);
            if (awdCmp != 0) return awdCmp;
            if ("God".equals(this.career.playerPerson.category) && !"God".equals(o.career.playerPerson.category)) {
                return 1;
            }
            if ("God".equals(o.career.playerPerson.category) && !"God".equals(this.career.playerPerson.category)) {
                return -1;
            }
            int rndCmp = Boolean.compare(this.career.playerPerson.isRandom, o.career.playerPerson.isRandom);
            if (rndCmp != 0) return rndCmp;
            return -Double.compare(this.winScore(), o.winScore());
        }

        @Override
        public String toString() {
            return career.getPlayerPerson().getPlayerId() + ": " + recentAwards;
        }
    }
}
