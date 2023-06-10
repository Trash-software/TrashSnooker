package trashsoftware.trashSnooker.core.career;

import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.aiMatch.AiVsAi;
import trashsoftware.trashSnooker.core.career.awardItems.AwardMaterial;
import trashsoftware.trashSnooker.core.career.awardItems.AwardPerk;
import trashsoftware.trashSnooker.core.career.challenge.ChallengeSet;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.training.Challenge;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.JsonChecksum;

import java.util.*;

public class Career {
    
    public static final double[] PERK_RANDOM_RANGE = {0.5, 2.0};

    private final List<ChampionshipScore> championshipScores = new ArrayList<>();  // 不一定按时间顺序
    private final boolean isHumanPlayer;
    private final transient Map<GameRule, Double> efforts = new HashMap<>();
    private PlayerPerson playerPerson;
    private int totalPerks;
    private int availPerks;
    private int totalExp = 0;
    private int level = 1;
    private int expInThisLevel;
    private double handFeel = 0.9;
    
    //  这些都是Human Career特有的，考虑移入CareerManager
    private Set<String> completedChallenges;
    private Map<Integer, List<AwardMaterial>> levelAwards;

    private Career(PlayerPerson person, boolean isHumanPlayer) {
        this.playerPerson = person;
        this.isHumanPlayer = isHumanPlayer;

        for (GameRule gameRule : GameRule.values()) efforts.put(gameRule, 1.0);
    }

    public static Career createByPerson(PlayerPerson playerPerson, boolean isHumanPlayer) {
        Career career = new Career(playerPerson, isHumanPlayer);
        career.availPerks = CareerManager.INIT_PERKS;
        career.totalPerks = career.availPerks;
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
        career.totalPerks = jsonObject.has("totalPerks") ? jsonObject.getInt("totalPerks") : 0;
        career.totalExp = jsonObject.has("totalExp") ? jsonObject.getInt("totalExp") : 0;
        career.level = jsonObject.has("level") ? jsonObject.getInt("level") : 1;
        career.expInThisLevel = jsonObject.has("expInThisLevel") ? jsonObject.getInt("expInThisLevel") : 0;
        
        if (jsonObject.has("levelAwards")) {
            career.levelAwards = new HashMap<>();
            JSONObject levelAwdObj = jsonObject.getJSONObject("levelAwards");
            for (String key : levelAwdObj.keySet()) {
                int level = Integer.parseInt(key);
                JSONObject awdObj = levelAwdObj.getJSONObject(key);
                List<AwardMaterial> thisLevelAwards = AwardMaterial.fromJsonList(awdObj);
                career.levelAwards.put(level, thisLevelAwards);
            }
        }

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
        
        if (jsonObject.has("completedChallenges")) {
            career.completedChallenges = new HashSet<>();
            JSONArray compCha = jsonObject.getJSONArray("completedChallenges");
            for (Object cha : compCha) {
                career.completedChallenges.add((String) cha);
            }
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
        
        if (isHumanPlayer) {
            out.put("availPerks", availPerks);
            out.put("totalPerks", totalPerks);
            out.put("totalExp", totalExp);
            out.put("level", level);
            out.put("expInThisLevel", expInThisLevel);
            
            JSONArray compCha = new JSONArray();
            if (completedChallenges != null) {
                for (String challengeId : completedChallenges) {
                    compCha.put(challengeId);
                }
            }
            out.put("completedChallenges", compCha);
            
            JSONObject levelAwdObj = new JSONObject();
            if (levelAwards != null) {
                for (Map.Entry<Integer, List<AwardMaterial>> entry : levelAwards.entrySet()) {
                    JSONObject levelObj = new JSONObject();
                    for (AwardMaterial am : entry.getValue()) {
                        am.putToJson(levelObj);
                    }
                    levelAwdObj.put(String.valueOf(entry.getKey()), levelObj);
                }
            }
            out.put("levelAwards", levelAwdObj);
        }

        return out;
    }
    
    public void completeChallenge(ChallengeSet challengeSet) {
        if (completedChallenges == null) completedChallenges = new HashSet<>();
        // 仅第一次完成给经验
        if (!completedChallenges.contains(challengeSet.getId())) {
            completedChallenges.add(challengeSet.getId());

            totalExp += challengeSet.getExp();
            expInThisLevel += challengeSet.getExp();
//            tryLevelUp();
        }
    }
    
    public boolean challengeIsComplete(String challengeId) {
        return completedChallenges != null && completedChallenges.contains(challengeId);
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
        for (ChampionshipScore.Rank rank : score.ranks) {
            int exp = score.data.getExpByRank(rank);
            totalExp += exp;
            expInThisLevel += exp;
        }
//        tryLevelUp();
    }

    public int getLevel() {
        return level;
    }

    public int getExpInThisLevel() {
        return expInThisLevel;
    }
    
    public boolean canLevelUp() {
        return expInThisLevel >= CareerManager.getInstance().getExpNeededToLevelUp(level);
    }
    
    public int[] levelUpPerkRange(int toLevel) {
        int mean = CareerManager.perksOfLevelUp(toLevel);
        int low = (int) Math.max(1, Math.round(PERK_RANDOM_RANGE[0] * mean));
        int high = (int) Math.round(PERK_RANDOM_RANGE[1] * mean);
        
        int[] res = new int[high - low + 1];
        for (int i = 0; i < res.length; i++) {
            res[i] = i + low;
        }
        return res;
    }
    
    public List<AwardMaterial> levelUp() {
        List<AwardMaterial> result = new ArrayList<>();

        int expNeed = CareerManager.getInstance().getExpNeededToLevelUp(level);
        level++;
        expInThisLevel -= expNeed;
        
        int[] range = levelUpPerkRange(level);
        int index = (int) (Math.random() * range.length);
        int perk = range[index];
        availPerks += perk;
        totalPerks += perk;

        AwardMaterial pm = new AwardPerk(perk);
        result.add(pm);
        
        if (levelAwards == null) levelAwards = new HashMap<>();
        levelAwards.put(level, result);
        
        CareerManager.getInstance().saveToDisk();
        
        return result;
    }

    void validateLevel() {
        int levelTotal = 0;  // 累积的升级所需经验
        int remExp = 0;
        int lv = 1;
        int[] expList = CareerManager.getExpRequiredLevelUp();
        for (int i = 0; i < expList.length; i++) {
            if (lv == level) break;
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
            System.err.printf("You hacked your user data: should %d,%d, actual: %d,%d \n",
                    lv, 
                    remExp,
                    level,
                    expInThisLevel);
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

    public int getTotalPerks() {
        return totalPerks;
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

            if (career.isHumanPlayer) return true;  // 我们无权替真人玩家决定

            if ("God".equals(career.getPlayerPerson().category)) return false;  // Master别出来打比赛
            if (!career.getPlayerPerson().isPlayerOf(data.type)) return false;  // 不是玩这个的

            if (selfRanking < 16) {
                int champAwd = data.getAwardByRank(ChampionshipScore.Rank.CHAMPION);

                int selfAwd = getEffectiveAward(data.getSelection());

                if (data.getClassLevel() <= 2) return true;  // 重要比赛，要去

                double mustJoinRatio = selfAwd * 0.2;
                if (champAwd >= mustJoinRatio) return true;  // 大比赛，要去

                if (!data.isRanked() && data.getClassLevel() >= 4) return false;  // 小的非排名赛，算了吧

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
                int rankAwards = 0;
                int completeAwards = 0; 
                if (score.data.type == type) {
                    for (ChampionshipScore.Rank rank : score.ranks) {
                        Integer awd = score.data.getAwardByRank(rank);
                        if (awd == null) System.err.println("Maybe tournaments.json has changed?");
                        else {
                            completeAwards += awd;
                            if (rank.ranked) rankAwards += awd;
                        }
                    }
                    totalAwards += completeAwards;
                    if (score.data.ranked) {
                        if (oneYearBefore.before(score.timestamp)) {
                            oneSeasonAwards += rankAwards;
                        }
                        if (twoYearBefore.before(score.timestamp)) {
                            twoSeasonsAwards += rankAwards;
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
