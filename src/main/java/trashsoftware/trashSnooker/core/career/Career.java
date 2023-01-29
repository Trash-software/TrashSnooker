package trashsoftware.trashSnooker.core.career;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.aiMatch.SnookerAiVsAi;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.util.DataLoader;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Career {

    private final List<ChampionshipScore> championshipScores = new ArrayList<>();  // 不一定按时间顺序
    private final boolean isHumanPlayer;
    private PlayerPerson playerPerson;
    private int cumulatedPerks = 0;
    private int usedPerks = 0;

    private Career(PlayerPerson person, boolean isHumanPlayer) {
        this.playerPerson = person;
        this.isHumanPlayer = isHumanPlayer;
    }

    public static Career createByPerson(PlayerPerson playerPerson, boolean isHumanPlayer) {
        Career career = new Career(playerPerson, isHumanPlayer);

        return career;
    }

    public static Career fromJson(JSONObject jsonObject) {
        String playerId = jsonObject.getString("playerId");
        PlayerPerson playerPerson = DataLoader.getInstance().getPlayerPerson(playerId);

        Career career = new Career(playerPerson, jsonObject.getBoolean("human"));
        career.cumulatedPerks = jsonObject.getInt("cumulatedPerks");
        career.usedPerks = jsonObject.getInt("usedPerks");

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
        out.put("scores", scoreArr);
        out.put("cumulatedPerks", cumulatedPerks);
        out.put("usedPerks", usedPerks);

        return out;
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

    public int snookerAwardsInRecentTwoYears(Calendar timestamp) {
        Calendar startTime = Calendar.getInstance();
        startTime.set(timestamp.get(Calendar.YEAR) - 2,
                timestamp.get(Calendar.MONTH),
                timestamp.get(Calendar.DAY_OF_MONTH) - 1);  // 上上届要算

        int total = 0;
        for (ChampionshipScore score : championshipScores) {
            if (score.data.type == GameRule.SNOOKER && startTime.before(score.timestamp)) {
                for (ChampionshipScore.Rank rank : score.ranks) {
                    total += score.data.getAwardByRank(rank);
                }
            }
        }
        return total;
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
        public final int recentAwards;

        public CareerWithAwards(Career career, int recentAwards) {
            this.career = career;
            this.recentAwards = recentAwards;
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
