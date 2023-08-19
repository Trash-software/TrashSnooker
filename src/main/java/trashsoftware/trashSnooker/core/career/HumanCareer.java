package trashsoftware.trashSnooker.core.career;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;
import trashsoftware.trashSnooker.core.career.awardItems.AwardMaterial;
import trashsoftware.trashSnooker.core.career.awardItems.AwardPerk;
import trashsoftware.trashSnooker.core.career.challenge.ChallengeHistory;
import trashsoftware.trashSnooker.core.career.challenge.ChallengeReward;
import trashsoftware.trashSnooker.core.career.challenge.ChallengeSet;
import trashsoftware.trashSnooker.core.career.challenge.RewardCondition;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.util.EventLogger;

import java.util.*;

public class HumanCareer extends Career {
    public static final double TAX_RATE = 0.2;
    private final Map<String, ChallengeHistory> completedChallenges = new HashMap<>();
    private final Map<Integer, List<AwardMaterial>> levelAwards = new HashMap<>();
    private int totalPerks;
    private int availPerks;
    private int totalExp = 0;
    private int level = 1;
    private int expInThisLevel;
    private int money;
    private int cumulativeAwards;  // 历史上的税前总奖金

    HumanCareer(PlayerPerson playerPerson) {
        super(playerPerson, true);
    }

    @Override
    protected void initNew() {
        availPerks = CareerManager.INIT_PERKS;
        totalPerks = availPerks;
    }

    @Override
    protected void fillFromJson(JSONObject jsonObject) {
        availPerks = jsonObject.has("availPerks") ? jsonObject.getInt("availPerks") : 0;
        totalPerks = jsonObject.has("totalPerks") ? jsonObject.getInt("totalPerks") : 0;
        totalExp = jsonObject.has("totalExp") ? jsonObject.getInt("totalExp") : 0;
        level = jsonObject.has("level") ? jsonObject.getInt("level") : 1;
        expInThisLevel = jsonObject.has("expInThisLevel") ? jsonObject.getInt("expInThisLevel") : 0;
        money = jsonObject.has("money") ? jsonObject.getInt("money") : 0;

        if (jsonObject.has("levelAwards")) {
            JSONObject levelAwdObj = jsonObject.getJSONObject("levelAwards");
            for (String key : levelAwdObj.keySet()) {
                int level = Integer.parseInt(key);
                JSONObject awdObj = levelAwdObj.getJSONObject(key);
                List<AwardMaterial> thisLevelAwards = AwardMaterial.fromJsonList(awdObj);
                levelAwards.put(level, thisLevelAwards);
            }
        }

        try {
            if (jsonObject.has("completedChallenges")) {
                JSONArray compCha = jsonObject.getJSONArray("completedChallenges");
                for (Object cha : compCha) {
                    if (cha instanceof JSONObject) {
                        ChallengeHistory ch = ChallengeHistory.fromJson((JSONObject) cha);
                        completedChallenges.put(ch.challengeId, ch);
                    }
                    // 舍弃掉旧的String形式
                }
            }
        } catch (JSONException e) {
            EventLogger.log(e, EventLogger.INFO, true);
        }
    }

    @Override
    protected void putExtraInJson(JSONObject out) {
        out.put("availPerks", availPerks);
        out.put("totalPerks", totalPerks);
        out.put("totalExp", totalExp);
        out.put("level", level);
        out.put("expInThisLevel", expInThisLevel);
        out.put("money", money);

        JSONArray compCha = new JSONArray();
        if (completedChallenges != null) {
            for (ChallengeHistory ch : completedChallenges.values()) {
                compCha.put(ch.toJson());
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

    @Override
    protected void validateLevel() {
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

    public void completeChallenge(ChallengeSet challengeSet, boolean clearance, int score) {
        // 仅第一次完成给经验
        ChallengeHistory history = completedChallenges.computeIfAbsent(
                challengeSet.getId(),
                ChallengeHistory::new
        );
        Map<RewardCondition, ChallengeReward> newFulfills = history.newComplete(challengeSet, clearance, score);
        if (!newFulfills.isEmpty()) {
            for (ChallengeReward cr : newFulfills.values()) {
                totalExp += cr.getExp();
                expInThisLevel += cr.getExp();
                earnMoney(cr.getMoney());
            }
        }
        checkScoreAchievements();
    }

    public ChallengeHistory getChallengeHistory(String challengeId) {
        return completedChallenges == null ? null : completedChallenges.get(challengeId);
    }

    public void earnMoney(int earned) {
        this.cumulativeAwards += earned;
        this.money += (int) Math.round(earned * (1 - TAX_RATE));
        // 这里不检查成就，因为earnMoney之后一般都跟着checkScoreAchievements()
    }

    @Override
    public void addChampionshipScore(ChampionshipScore score) {
        super.addChampionshipScore(score);

        for (ChampionshipScore.Rank rank : score.ranks) {
            int exp = score.data.getExpByRank(rank);
            totalExp += exp;
            expInThisLevel += exp;

            earnMoney(score.data.getAwardByRank(rank));
        }
        checkScoreAchievements();
    }

    public void checkScoreAchievements() {
        AchManager achManager = AchManager.getInstance();
        
        Set<String> remSnookerTripleCrown = new HashSet<>(Set.of(ChampDataManager.getSnookerTripleCrownIds()));
        
        int cumAwards = 0;
        for (ChampionshipScore score : getChampionshipScores()) {
            for (ChampionshipScore.Rank rank : score.ranks) {
                int award = score.data.getAwardByRank(rank);
                cumAwards += award;
                if (award > 0) {
                    achManager.addAchievement(Achievement.EARNED_MONEY, null);
                }
                
                if (rank == ChampionshipScore.Rank.CHAMPION) {
                    achManager.addAchievement(Achievement.CHAMPION, null);
                    if (score.data.getType() == GameRule.SNOOKER) {
                        if (score.data.ranked) {
                            achManager.addAchievement(Achievement.SNOOKER_RANKED_CHAMPION, null);
                        }
                        remSnookerTripleCrown.remove(score.data.id);
                        if (ChampDataManager.isSnookerWorldChamp(score.data)) {
                            achManager.addAchievement(Achievement.SNOOKER_WORLD_CHAMPION, null);
                        }
                    }
                } else if (rank == ChampionshipScore.Rank.SECOND_PLACE) {
                    achManager.addAchievement(Achievement.SECOND_PLACE, null);
                } else if (rank == ChampionshipScore.Rank.TOP_4) {
                    achManager.addAchievement(Achievement.BEST_FOUR, null);
                } else if (rank == ChampionshipScore.Rank.BEST_SINGLE) {
                    achManager.addAchievement(Achievement.POTTING_MACHINE, null);
                }
            }
        }
        
        if (remSnookerTripleCrown.isEmpty()) {
            achManager.addAchievement(Achievement.SNOOKER_TRIPLE_CROWN, null);
        }

        System.out.println("awards: " + cumulativeAwards + ", " + cumAwards);
        cumulativeAwards = cumAwards;
        achManager.addAchievement(Achievement.EARN_MONEY_CUMULATIVE, cumulativeAwards, null);
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

        levelAwards.put(level, result);

        CareerManager.getInstance().saveToDisk();

        return result;
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

    public int getMoney() {
        if (money == 0 && CareerManager.getInstance().getLastSavedVersion() < 40) {
            int awards = computeAllAwards();
            money = (int) ((1 - TAX_RATE) * awards);
            CareerManager.getInstance().saveToDisk();
        }
        return money;
    }
    
    private int computeAllAwards() {
        int awards = 0;
        awards += new CareerWithAwards(GameRule.SNOOKER, this, Calendar.getInstance()).getTotalAwards();
        awards += new CareerWithAwards(GameRule.CHINESE_EIGHT, this, Calendar.getInstance()).getTotalAwards();
        awards += new CareerWithAwards(GameRule.LIS_EIGHT, this, Calendar.getInstance()).getTotalAwards();
        awards += new CareerWithAwards(GameRule.AMERICAN_NINE, this, Calendar.getInstance()).getTotalAwards();
        return awards;
    }
}
