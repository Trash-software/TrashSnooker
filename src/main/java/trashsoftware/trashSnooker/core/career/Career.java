package trashsoftware.trashSnooker.core.career;

import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.aiMatch.AiVsAi;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.util.DataLoader;

import java.util.*;

public class Career {

    private final List<ChampionshipScore> championshipScores = new ArrayList<>();  // 不一定按时间顺序
    private final boolean isHumanPlayer;
    private final transient Map<GameRule, Double> efforts = new HashMap<>();
    private PlayerPerson playerPerson;
    //    private int cumulatedPerks = 0;
//    private int usedPerks = 0;
    private int availPerks;
    private int totalExp = 0;
    private int level = 1;
    private int expInThisLevel;
    private double handFeel = 0.9;

    private Career(PlayerPerson person, boolean isHumanPlayer) {
        this.playerPerson = person;
        this.isHumanPlayer = isHumanPlayer;

        for (GameRule gameRule : GameRule.values()) efforts.put(gameRule, 1.0);
    }

    public static Career createByPerson(PlayerPerson playerPerson, boolean isHumanPlayer) {
        Career career = new Career(playerPerson, isHumanPlayer);
        career.availPerks = CareerManager.INIT_PERKS;
        return career;
    }

    public static Career fromJson(JSONObject jsonObject) {
        String playerId = jsonObject.getString("playerId");
        PlayerPerson playerPerson = DataLoader.getInstance().getPlayerPerson(playerId);
        if (playerPerson == null) {
            // 有career记录，但是PlayerPerson已经被删了
            return null;
        }

        Career career = new Career(playerPerson, jsonObject.getBoolean("human"));
        career.availPerks = jsonObject.has("availPerks") ? jsonObject.getInt("availPerks") : 0;
        career.totalExp = jsonObject.has("totalExp") ? jsonObject.getInt("totalExp") : 0;
        career.level = jsonObject.has("level") ? jsonObject.getInt("level") : 1;
        career.expInThisLevel = jsonObject.has("expInThisLevel") ? jsonObject.getInt("expInThisLevel") : 0;

        career.validateLevel();

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
        out.put("availPerks", availPerks);
        out.put("totalExp", totalExp);
        out.put("level", level);
        out.put("expInThisLevel", expInThisLevel);

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
        for (ChampionshipScore.Rank rank : score.ranks) {
            int exp = score.data.getExpByRank(rank);
            totalExp += exp;
            expInThisLevel += exp;
        }
        tryLevelUp();
    }

    public int getLevel() {
        return level;
    }

    public int getExpInThisLevel() {
        return expInThisLevel;
    }

    private void tryLevelUp() {
        CareerManager manager = CareerManager.getInstance();
        int expNeed;
        while (expInThisLevel >= (expNeed = manager.getExpNeededToLevelUp(level))) {
            level++;
            availPerks += CareerManager.perksOfLevelUp(level);
            expInThisLevel -= expNeed;
        }
    }

    void validateLevel() {
        int levelTotal = 0;  // 累积的升级所需经验
        int remExp = 0;
        int lv = 1;
        int[] expList = CareerManager.getExpRequiredLevelUp();
        for (int i = 0; i < expList.length; i++) {
            int nextLvReq = expList[i];
            int nextLvTotal = levelTotal + nextLvReq;
            remExp = totalExp - levelTotal;
            lv = i + 1;
            if (levelTotal <= totalExp && nextLvTotal > totalExp) {
                // 结束了，就是这一级了
                break;
            }
            levelTotal = nextLvTotal;
        }
        if (lv != level || remExp != expInThisLevel) {
            System.err.println("You hacked your user data!");
        }
    }

    public List<ChampionshipScore> getChampionshipScores() {
        return championshipScores;
    }

    public boolean isHumanPlayer() {
        return isHumanPlayer;
    }

    public void usePerk(int used) {
        availPerks -= used;

        CareerManager.getInstance().saveToDisk();
    }

    public int getAvailablePerks() {
        return availPerks;
    }

    @Override
    public String toString() {
        return "Career{" +
                "playerPerson=" + playerPerson.getPlayerId() +
                ", isHumanPlayer=" + isHumanPlayer +
                '}';
    }

    public static class CareerWithAwards {
        public final Career career;
        public final GameRule type;
        private int oneSeasonAwards;
        private int twoSeasonsAwards;
        private int totalAwards;

        public CareerWithAwards(GameRule type, Career career, Calendar timestamp) {
            this.career = career;
            this.type = type;

            calculateAwards(timestamp);
        }

        public static int twoSeasonsCompare(CareerWithAwards t, Career.CareerWithAwards o) {
            return compare(t, o, t.twoSeasonsAwards, o.twoSeasonsAwards);
        }

        public static int oneSeasonCompare(CareerWithAwards t, Career.CareerWithAwards o) {
            return compare(t, o, t.oneSeasonAwards, o.oneSeasonAwards);
        }

        private static int compare(CareerWithAwards t, Career.CareerWithAwards o,
                                   int tAwd, int oAwd) {
            int awdCmp = -Integer.compare(tAwd, oAwd);
            if (awdCmp != 0) return awdCmp;
            if ("God".equals(t.career.playerPerson.category) && !"God".equals(o.career.playerPerson.category)) {
                return 1;
            }
            if ("God".equals(o.career.playerPerson.category) && !"God".equals(t.career.playerPerson.category)) {
                return -1;
            }
            int rndCmp = Boolean.compare(t.career.playerPerson.isRandom, o.career.playerPerson.isRandom);
            if (rndCmp != 0) return rndCmp;
            return -Double.compare(t.winScore(), o.winScore());
        }

        /**
         * 可能存在球员看不起小比赛的情况
         *
         * @param selfRanking 本人的排名，从0计
         * @param front       前一位的，如果本人是冠军则null
         * @param back        后一位的，如果本人是垫底则null
         */
        public boolean willJoinMatch(ChampionshipData data, int selfRanking,
                                     CareerWithAwards front, CareerWithAwards back) {
            if ("God".equals(career.getPlayerPerson().category)) return false;  // Master别出来打比赛

            if (selfRanking < 16) {
                int champAwd = data.getAwardByRank(ChampionshipScore.Rank.CHAMPION);

                int selfAwd = getEffectiveAward(data.getSelection());

                double mustJoinRatio = selfAwd * 0.2;
                if (champAwd >= mustJoinRatio) return true;  // 大比赛，要去

                int frontAwd = front == null ? Integer.MAX_VALUE : front.getEffectiveAward(data.getSelection());
                int backAwd = back == null ? 0 : back.getEffectiveAward(data.getSelection());

                if (selfAwd + champAwd < frontAwd) {
                    return false;  // 拿了冠军也追不上前一名
                }
                if (backAwd + champAwd < selfAwd) {
                    return false;  // 后一名拿了冠军也追不上我
                }
            }
            return true;
        }

        private void calculateAwards(Calendar timestamp) {
            oneSeasonAwards = 0;
            twoSeasonsAwards = 0;
            totalAwards = 0;

            Calendar twoYearBefore = Calendar.getInstance();
            twoYearBefore.set(timestamp.get(Calendar.YEAR) - 2,
                    timestamp.get(Calendar.MONTH),
                    timestamp.get(Calendar.DAY_OF_MONTH) - 1);  // 上上届要算

            Calendar oneYearBefore = Calendar.getInstance();
            oneYearBefore.set(timestamp.get(Calendar.YEAR) - 1,
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
                        if (oneYearBefore.before(score.timestamp)) {
                            oneSeasonAwards += awards;
                        }
                        if (twoYearBefore.before(score.timestamp)) {
                            twoSeasonsAwards += awards;
                        }
                    }
                }
            }
        }

        public int getEffectiveAward(ChampionshipData.Selection selection) {
            switch (selection) {
                case REGULAR:
                case ALL_CHAMP:
                default:
                    return twoSeasonsAwards;
                case SINGLE_SEASON:
                    return oneSeasonAwards;
            }
        }

        public int getTwoSeasonsAwards() {
            return twoSeasonsAwards;
        }

        public int getOneSeasonAwards() {
            return oneSeasonAwards;
        }

        public int getTotalAwards() {
            return totalAwards;
        }

        private double winScore() {
            return AiVsAi.playerSimpleWinningScore(
                    career.getPlayerPerson(),
                    career.getPlayerPerson().getAiPlayStyle(),
                    PlayerPerson.ReadableAbility.fromPlayerPerson(career.getPlayerPerson()),
                    false
            );
        }

        @Override
        public String toString() {
            return career.getPlayerPerson().getPlayerId() + ": " + twoSeasonsAwards;
        }
    }
}
