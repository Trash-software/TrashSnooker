package trashsoftware.trashSnooker.core.career;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.person.PlayerPerson;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;
import trashsoftware.trashSnooker.core.career.awardItems.AwardMaterial;
import trashsoftware.trashSnooker.core.career.awardItems.AwardPerk;
import trashsoftware.trashSnooker.core.career.challenge.ChallengeHistory;
import trashsoftware.trashSnooker.core.career.challenge.ChallengeReward;
import trashsoftware.trashSnooker.core.career.challenge.ChallengeSet;
import trashsoftware.trashSnooker.core.career.challenge.RewardCondition;
import trashsoftware.trashSnooker.core.career.championship.Championship;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.fxml.widgets.PerkManager;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.JsonChecksum;
import trashsoftware.trashSnooker.util.Util;

import java.io.File;
import java.time.Duration;
import java.util.*;

public class HumanCareer extends Career {
    public static final double TAX_RATE = 0.2;
    public static final double YEAR_IZE_INTEREST_RATE = 0.12;  // 贷款利率
    public static final int DAILY_LIFE_FEE_LOW = 50;
    public static final int DAILY_LIFE_FEE = 100;
    public static final int DAILY_LIFE_FEE_HIGH = 200;

    private final Map<String, ChallengeHistory> completedChallenges = new HashMap<>();
    private final Map<Integer, List<AwardMaterial>> levelAwards = new HashMap<>();

    private FinancialManager finance;
    private AwardDistributionHint unShownAwd;
    private final CareerManager careerManager;
    private ChampionshipLocation liveLocation = ChampionshipLocation.CHN;  // 居住地

    HumanCareer(PlayerPerson playerPerson, CareerManager careerManager) {
        super(playerPerson, true, careerManager);
        
        this.careerManager = careerManager;
    }

    @Override
    protected void initNew() {
        finance = new FinancialManager(careerManager.getCareerSave());
        finance.availPerks = CareerManager.INIT_PERKS;
        finance.totalPerks = finance.availPerks;
        finance.remFreePerks = finance.availPerks;  // 初始升级不要钱
        finance.money = CareerManager.INIT_MONEY;
    }
    
    void setInitLevel(int tarLevel) {
        for (int i = getLevel(); i < tarLevel; i++) {
            List<AwardMaterial> result = new ArrayList<>();
            int expNeed = CareerManager.getExpNeededToLevelUp(finance.level);
            finance.level++;
            finance.totalExp += expNeed;
            int[] range = levelUpPerkRange(finance.level);
            int index = (int) (Math.random() * range.length);
            int perk = range[index];
            finance.availPerks += perk;
            finance.totalPerks += perk;

            AwardMaterial pm = new AwardPerk(perk);
            result.add(pm);

            levelAwards.put(finance.level, result);
        }
        finance.remFreePerks = finance.availPerks;
    }

    @Override
    protected void fillFromJson(JSONObject jsonObject, CareerManager careerManager) {
        finance = new FinancialManager(careerManager.getCareerSave());
        if (!finance.loadFromJson()) {
            try {
                finance.availPerks = jsonObject.getInt("availPerks");
                finance.totalPerks = jsonObject.getInt("totalPerks");
                finance.totalExp = jsonObject.getInt("totalExp");
                finance.level = jsonObject.getInt("level");
                finance.expInThisLevel = jsonObject.getInt("expInThisLevel");
                finance.money = jsonObject.getInt("money");
            } catch (JSONException e) {
                finance.availPerks = 0;
                finance.totalPerks = 0;
                finance.totalExp = 0;
                finance.level = 1;
                finance.expInThisLevel = 0;
                finance.money = 0;
                System.err.println("No financial info loaded");
            }
        }

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
        // 悄悄存另一个json，不讲武德
        // 理论上不应该这样，但考虑到本method只会在CareerManager#saveToDisk里调用，所以也将就了
        finance.writeToDisk();

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
            if (lv == finance.level) break;
            int nextLvReq = expList[i];
            int nextLvTotal = levelTotal + nextLvReq;
            remExp = finance.totalExp - levelTotal;
            lv = i + 1;
            if (levelTotal <= finance.totalExp && nextLvTotal > finance.totalExp) {
                // 结束了，就是这一级了
                break;
            }
            levelTotal = nextLvTotal;
        }
        if (lv != finance.level || remExp != finance.expInThisLevel) {
            System.err.printf("You hacked your user data: should %d,%d, actual: %d,%d \n",
                    lv,
                    remExp,
                    finance.level,
                    finance.expInThisLevel);
        }
    }

    public void setLiveLocation(ChampionshipLocation liveLocation) {
        this.liveLocation = liveLocation;
    }

    public void completeChallenge(ChallengeSet challengeSet, boolean clearance, int score) {
        // 仅第一次完成给经验
        ChallengeHistory history = completedChallenges.computeIfAbsent(
                challengeSet.getId(),
                ChallengeHistory::new
        );
        int moneyBefore = finance.money;
        JSONObject invoice = new JSONObject();
        String timestamp = Util.TIME_FORMAT_SEC.format(new Date());
        invoice.put("timestamp", timestamp);
        String inGameDate = CareerManager.calendarToString(getCareerManager().getTimestamp());
        invoice.put("inGameDate", inGameDate);
        invoice.put("type", "challengeEarn");
        invoice.put("match", challengeSet.getId());
        JSONObject items = new JSONObject();
        
        Map<RewardCondition, ChallengeReward> newFulfills = history.newComplete(challengeSet, clearance, score);
        if (!newFulfills.isEmpty()) {
            for (Map.Entry<RewardCondition, ChallengeReward> entry : newFulfills.entrySet()) {
                ChallengeReward cr = entry.getValue();
                
                int before = finance.money;
                
                finance.totalExp += cr.getExp();
                finance.expInThisLevel += cr.getExp();
                int raw = cr.getMoney();
                earnMoney(raw, liveLocation);
                int real = finance.money - before;

                JSONObject subItem = new JSONObject();
                subItem.put("raw", raw);
                subItem.put("actual", real);
                items.put(entry.getKey().toJsonString(), subItem);
            }
        }

        invoice.put("items", items);
        invoice.put("moneyBefore", moneyBefore);
        invoice.put("moneyAfter", finance.money);
        finance.invoices.add(invoice);
        
        checkScoreAchievements();
        saveFinance();
    }

    public ChallengeHistory getChallengeHistory(String challengeId) {
        return completedChallenges == null ? null : completedChallenges.get(challengeId);
    }

    /**
     * 记录一笔合法收入
     * 
     * @param earned  标价
     */
    public void earnMoney(int earned, ChampionshipLocation location) {
        finance.cumulativeAwards += earned;
        
        double tax = location.tax(earned);
        
        finance.money += (int) Math.round(earned - tax);
        // 这里不检查成就，因为earnMoney之后一般都跟着checkScoreAchievements()
    }

    public void buyCueTip(String tipInstanceId, int price) {
        int moneyBefore = finance.money;
        finance.money -= price;

        JSONObject invoice = new JSONObject();
        String timestamp = Util.TIME_FORMAT_SEC.format(new Date());
        invoice.put("timestamp", timestamp);
        String inGameDate = CareerManager.calendarToString(getCareerManager().getTimestamp());
        invoice.put("inGameDate", inGameDate);
        invoice.put("type", "purchase");
        invoice.put("itemType", "tip");
        invoice.put("moneyBefore", moneyBefore);
        invoice.put("moneyCost", price);
        invoice.put("moneyAfter", finance.money);
        invoice.put("item", tipInstanceId);
        finance.invoices.add(invoice);
        saveFinance();
    }

    public void buyCue(String cueInstanceId, int price) {
        int moneyBefore = finance.money;
        finance.money -= price;

        JSONObject invoice = new JSONObject();
        String timestamp = Util.TIME_FORMAT_SEC.format(new Date());
        invoice.put("timestamp", timestamp);
        String inGameDate = CareerManager.calendarToString(getCareerManager().getTimestamp());
        invoice.put("inGameDate", inGameDate);
        invoice.put("type", "purchase");
        invoice.put("itemType", "cue");
        invoice.put("moneyBefore", moneyBefore);
        invoice.put("moneyCost", price);
        invoice.put("moneyAfter", finance.money);
        invoice.put("item", cueInstanceId);
        finance.invoices.add(invoice);
        saveFinance();
    }

    @Override
    public void addChampionshipScore(ChampionshipScore score) {
        super.addChampionshipScore(score);
        
        int moneyBefore = finance.money;
        JSONObject invoice = new JSONObject();
        String timestamp = Util.TIME_FORMAT_SEC.format(new Date());
        invoice.put("timestamp", timestamp);
        String inGameDate = CareerManager.calendarToString(getCareerManager().getTimestamp());
        invoice.put("inGameDate", inGameDate);
        invoice.put("type", "championshipEarn");
        invoice.put("match", score.data.getId());
        invoice.put("year", score.getYear());
        
        int moneyEarned = 0;
        int expEarned = 0;

        JSONObject items = new JSONObject();
        for (ChampionshipScore.Rank rank : score.ranks) {
            int exp = score.data.getExpByRank(rank);
            finance.totalExp += exp;
            finance.expInThisLevel += exp;
            expEarned += exp;
            
            int before = finance.money;
            int raw = score.data.getAwardByRank(rank);
            earnMoney(raw, score.data.location);
            int real = finance.money - before;
            
            JSONObject subItem = new JSONObject();
            subItem.put("raw", raw);
            subItem.put("actual", real);
            items.put(rank.name(), subItem);
            
            moneyEarned += real;
        }
        
        invoice.put("items", items);
        invoice.put("moneyBefore", moneyBefore);
        invoice.put("moneyAfter", finance.money);
        finance.invoices.add(invoice);
        
        unShownAwd = new AwardDistributionHint(moneyEarned, expEarned);
        
        checkScoreAchievements();
        saveFinance();
    }

    public void checkScoreAchievements() {
        AchManager achManager = AchManager.getInstance();

        Set<String> remSnookerTripleCrown = new HashSet<>(Set.of(ChampDataManager.getSnookerTripleCrownIds()));

        int cumMainTimes = 0;
        int cumAwards = 0;
        for (ChampionshipScore score : getChampionshipScores()) {
            for (ChampionshipScore.Rank rank : score.ranks) {
                int award = score.data.getAwardByRank(rank);
                cumAwards += award;
                if (rank.isMain) {
                    cumMainTimes += 1;
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

        if (cumMainTimes > 0) {
            achManager.addAchievement(Achievement.FIRST_MAIN_OF_TOURNAMENT, null);
        }
        achManager.addAchievement(Achievement.PARTICIPATE_TOURNAMENTS, getChampionshipScores().size(), null);
        achManager.addAchievement(Achievement.MAIN_OF_TOURNAMENTS, cumMainTimes, null);

        System.out.println("awards: " + finance.cumulativeAwards + ", " + cumAwards);
        finance.cumulativeAwards = cumAwards;
        checkFinancialAch();
    }

    public int getLevel() {
        return finance.level;
    }

    public int getExpInThisLevel() {
        return finance.expInThisLevel;
    }

    public boolean canLevelUp() {
        return finance.expInThisLevel >= CareerManager.getExpNeededToLevelUp(finance.level);
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

        int expNeed = CareerManager.getExpNeededToLevelUp(finance.level);
        finance.level++;
        finance.expInThisLevel -= expNeed;

        int[] range = levelUpPerkRange(finance.level);
        int index = (int) (Math.random() * range.length);
        int perk = range[index];
        finance.availPerks += perk;
        finance.totalPerks += perk;

        AwardMaterial pm = new AwardPerk(perk);
        result.add(pm);

        levelAwards.put(finance.level, result);

        CareerManager.getInstance().saveToDisk();

        return result;
    }

    public void recordUpgradeAndUsePerk(PerkManager.UpgradeRec upgradeRec) {
        int moneyBefore = finance.money;
        int freePerksUsed = Math.min(finance.remFreePerks, upgradeRec.perkUsed());
        
        finance.money -= upgradeRec.moneyCost();
        finance.availPerks -= upgradeRec.perkUsed();
        finance.remFreePerks -= freePerksUsed;

        JSONObject record = new JSONObject();
        String timestamp = Util.TIME_FORMAT_SEC.format(new Date());
        record.put("timestamp", timestamp);
        String inGameDate = CareerManager.calendarToString(getCareerManager().getTimestamp());
        record.put("inGameDate", inGameDate);
        record.put("type", "upgrade");
        record.put("perkUsed", upgradeRec.perkUsed());
        record.put("freePerkUsed", freePerksUsed);
        record.put("moneyBefore", moneyBefore);
        record.put("moneyCost", upgradeRec.moneyCost());
        record.put("moneyAfter", finance.money);
        JSONObject skillUpgrade = new JSONObject();
        for (Map.Entry<String, double[]> entry : upgradeRec.abilityUpdated().entrySet()) {
            skillUpgrade.put(entry.getKey(), Util.arrayToJson(entry.getValue()));
        }
        record.put("ability", skillUpgrade);

        finance.invoices.add(record);

        saveFinance();
    }

    public int getAvailablePerks() {
        return finance.availPerks;
    }
    
    public int getFreePerksRem() {
        return finance.remFreePerks;
    }

    public int getTotalPerks() {
        return finance.totalPerks;
    }
    
    public void earnAchievementAward(Achievement achievement, int levelRec, int money) {
        int moneyBefore = finance.money;
        finance.money += money;
        
        JSONObject invoice = new JSONObject();
        String timestamp = Util.TIME_FORMAT_SEC.format(new Date());
        invoice.put("timestamp", timestamp);
        String inGameDate = CareerManager.calendarToString(getCareerManager().getTimestamp());
        invoice.put("inGameDate", inGameDate);
        invoice.put("type", "achievementAward");
        invoice.put("item", achievement.name());
        invoice.put("level", levelRec);
        
        invoice.put("moneyBefore", moneyBefore);
        invoice.put("moneyEarn", money);
        invoice.put("moneyAfter", finance.money);
        
        finance.invoices.add(invoice);
        saveFinance();
    }

    /**
     * 存款越多，日常开销越高
     */
    public int calculateDailyLifeFee() {
        int moneyLow = 0;
        int moneyHigh = 1_000_000;
        
        double lifeFee = Algebra.shiftRangeSafe(moneyLow, moneyHigh,
                DAILY_LIFE_FEE_LOW, DAILY_LIFE_FEE_HIGH,
                finance.money);
//        System.out.println("Daily life fee " + lifeFee);
        
        return (int) (Math.round(lifeFee / 10) * 10);  // 整10
    }

    /**
     * 返回开始下一个赛事时，应缴的强制费用
     */
    public Map<String, Integer> calculateFixedFees(ChampionshipData.WithYear nextChampData) {
        ChampionshipData.WithYear last = careerManager.getChampDataManager().getPreviousChampionship(
                nextChampData.year, nextChampData.data.month, nextChampData.data.day
        );
        
        Calendar lastTime = last.toCalendar();
        if (lastTime.before(careerManager.getBeginTimestamp())) return Map.of();

        Calendar nextTime = nextChampData.toCalendar();
        int diffDays = (int) Duration.between(lastTime.toInstant(), nextTime.toInstant()).toDays();

        Map<String, Integer> res = new HashMap<>();

        int lifeFee = diffDays * calculateDailyLifeFee();
        System.out.println("Life fee: " + diffDays + " * " + calculateDailyLifeFee());
        res.put("lifeFee", lifeFee);
        if (finance.money < 0) {
            double owe = -finance.money;
            int loanInterest = (int) Math.round(diffDays / 365.0 * owe * YEAR_IZE_INTEREST_RATE);
            res.put("oweInterest", loanInterest);
        }

        return res;
    }

    /**
     * 收取一些固定费用，如利息
     * 不包含参赛费用
     */
    public void updateMoneyChampStart(ChampionshipData.WithYear nextChampData) {
        Map<String, Integer> feesMap = calculateFixedFees(nextChampData);
        int fees = feesMap.values().stream().reduce(0, Integer::sum);

        if (fees > 0) {
            int moneyBefore = finance.money;
            finance.money -= fees;

            JSONObject invoice = new JSONObject();
            String timestamp = Util.TIME_FORMAT_SEC.format(new Date());
            invoice.put("timestamp", timestamp);
            String inGameDate = CareerManager.calendarToString(getCareerManager().getTimestamp());
            invoice.put("inGameDate", inGameDate);
            invoice.put("type", "fees");
            invoice.put("moneyBefore", moneyBefore);
            JSONArray subArray = new JSONArray();
            for (Map.Entry<String, Integer> feeItem : feesMap.entrySet()) {
                JSONObject sub = new JSONObject();
                sub.put("item", feeItem.getKey());
                sub.put("moneyCost", feeItem.getValue());
                subArray.put(sub);
            }
            invoice.put("items", subArray);
            invoice.put("moneyAfter", finance.money);
            finance.invoices.add(invoice);

            saveFinance();
        }
    }
    
    public void receiveInviteAward(Championship championship) {
        Integer humanSeedNum = championship.getCareerSeedMap().get(getPlayerPerson().getPlayerId());
        if (humanSeedNum == null) {
            EventLogger.warning("Human joined, but no seed num.");
            return;
        }
        int award = 0;
        for (Map.Entry<Integer, Integer> entry : championship.getData().getInviteAwardMap().entrySet()) {
            if (humanSeedNum <= entry.getKey()) {  // seed num从1开始的
                award += entry.getValue();
            }
        }
        if (award > 0) {
            AchManager.getInstance().addAchievement(Achievement.GET_INVITED, null);
            
            int moneyBefore = finance.money;
            finance.money += award;
                    
            JSONObject invoice = new JSONObject();
            String timestamp = Util.TIME_FORMAT_SEC.format(new Date());
            invoice.put("timestamp", timestamp);
            String inGameDate = CareerManager.calendarToString(getCareerManager().getTimestamp());
            invoice.put("inGameDate", inGameDate);
            invoice.put("type", "invitation");
            invoice.put("match", championship.uniqueId());
            invoice.put("item", humanSeedNum);
            invoice.put("moneyBefore", moneyBefore);
            invoice.put("moneyEarn", award);
            invoice.put("moneyAfter", finance.money);
            finance.invoices.add(invoice);
            saveFinance();
        }
    }

    /**
     * 这个方法可以把钱扣到负数
     * 目前没做债务管理器
     */
    public void payParticipateFees(Championship championship) {
        ChampionshipData data = championship.getData();
        int moneyBefore = finance.money;
        int registryFee = data.getRegistryFee();
        int travelFee = data.getFlightFee();
        int hotelFee = data.getHotelFee();

        finance.money -= (registryFee + travelFee + hotelFee);

        JSONObject invoice = new JSONObject();
        String timestamp = Util.TIME_FORMAT_SEC.format(new Date());
        invoice.put("timestamp", timestamp);
        String inGameDate = CareerManager.calendarToString(getCareerManager().getTimestamp());
        invoice.put("inGameDate", inGameDate);
        invoice.put("type", "participation");
        invoice.put("match", championship.uniqueId());
        invoice.put("moneyBefore", moneyBefore);

        JSONObject o1 = new JSONObject();
        o1.put("item", "registry");
        o1.put("moneyCost", registryFee);

        JSONObject o2 = new JSONObject();
        o2.put("item", "travel");
        o2.put("moneyCost", travelFee);

        JSONObject o3 = new JSONObject();
        o3.put("item", "hotel");
        o3.put("moneyCost", hotelFee);

        JSONArray subArray = new JSONArray();
        subArray.put(o1);
        subArray.put(o2);
        subArray.put(o3);

        invoice.put("items", subArray);

        invoice.put("moneyAfter", finance.money);
        finance.invoices.add(invoice);
        saveFinance();
    }

    public int getMoney() {
        checkMoney();

        return finance.money;
    }

    private void checkMoney() {
        boolean save = false;
        if (finance.money == 0 && CareerManager.getInstance().getLastSavedVersion() < 40) {
            int awards = computeAllAwards();
            finance.money = (int) ((1 - TAX_RATE) * awards);
            save = true;
        }
        if (CareerManager.getInstance().getLastSavedVersion() < 46) {
            // 大概扣一下参赛费和生活费
            Calendar beginDate = null;
            Calendar lastDate = null;
            for (ChampionshipScore cs : getChampionshipScores()) {
                int cost = cs.data.getRegistryFee() + cs.data.getFlightFee() + cs.data.getHotelFee();
                finance.money -= cost;
                
                if (beginDate == null) {
                    beginDate = cs.timestamp;
                }
                
                lastDate = cs.timestamp;
            }
            
            if (beginDate != null && lastDate != null && beginDate != lastDate) {
                int diffDays = (int) Duration.between(beginDate.toInstant(), lastDate.toInstant()).toDays();
                finance.money -= diffDays * DAILY_LIFE_FEE;
            }
            
            // 大致模拟加点花的钱
            int usedPerks = finance.totalPerks - finance.availPerks;
            if (usedPerks > 0) {
                // 防止篡改perk得钱
                // 假设平均一个点把一个技能升2.5
                int moneyUsed = 0;
                int pkRem = usedPerks;
                double[] simAbility = new double[PerkManager.N_CATEGORIES];
                Arrays.fill(simAbility, 75);
                while (pkRem > 0) {
                    for (int sk = 0; sk < PerkManager.N_CATEGORIES; sk++) {
                        simAbility[sk] += (120 - simAbility[sk]) * 0.05;
                        int cost = PerkManager.moneySpent(simAbility[sk]);
                        moneyUsed += cost;
                        pkRem--;
                        if (pkRem == 0) break;
                    }
                }

                System.out.println("Simulate money use: " + moneyUsed);
                finance.money = Math.max(10000, finance.money - moneyUsed);
            } else {
                System.out.println("Not simulate money use because perk inconsistency");
            }
            save = true;
        }

        if (save) {
            CareerManager.getInstance().saveToDisk();
        }
    }
    
    public InventoryManager getInventory() {
        return CareerManager.getInstance().getInventory();
    }

    private int computeAllAwards() {
        int awards = 0;
        // 只是利用这个类来算钱，没有实际作用
        awards += new CareerRanker.ByAwards(GameRule.SNOOKER, this, Calendar.getInstance()).getTotalAwards();
        awards += new CareerRanker.ByAwards(GameRule.CHINESE_EIGHT, this, Calendar.getInstance()).getTotalAwards();
        awards += new CareerRanker.ByAwards(GameRule.LIS_EIGHT, this, Calendar.getInstance()).getTotalAwards();
        awards += new CareerRanker.ByAwards(GameRule.AMERICAN_NINE, this, Calendar.getInstance()).getTotalAwards();
        return awards;
    }

    public AwardDistributionHint getAndRemoveUnShownAwd() {
        AwardDistributionHint awd = unShownAwd;
        unShownAwd = null;
        return awd;
    }
    
    public List<JSONObject> getInvoices() {
        return new ArrayList<>(finance.invoices);
    }
    
    public void saveFinance() {
        checkFinancialAch();
        
        finance.writeToDisk();
    }
    
    private void checkFinancialAch() {
        AchManager achManager = AchManager.getInstance();
        if (finance.cumulativeAwards > 0) {
            achManager.addAchievement(Achievement.EARNED_MONEY, null);
        }
        achManager.addAchievement(Achievement.CHAMP_EARN_CUMULATIVE, finance.cumulativeAwards, null);
        achManager.addAchievement(Achievement.SAVE_MONEY, finance.money, null);
        if (finance.money < 0) {
            achManager.addAchievement(Achievement.OWE_MONEY, null);
        }
        
        int cumPurchase = 0;
        int cumExpenditure = 0;
        for (JSONObject invoice: finance.invoices) {
            try {
                String type = invoice.getString("type");
                int moneyBefore = invoice.getInt("moneyBefore");
                int moneyAfter = invoice.getInt("moneyAfter");
                int moneyChange = moneyAfter - moneyBefore;
                if (moneyChange < 0) {
                    cumExpenditure -= moneyChange;
                }
                
                if ("purchase".equals(type)) {
                    cumPurchase += 1;
                }
            } catch (IllegalArgumentException | JSONException e) {
                EventLogger.warning(e);
            }
        }
        achManager.addAchievement(Achievement.BUY_ITEMS, cumPurchase, null);
        achManager.addAchievement(Achievement.EXPENDITURE, cumExpenditure, null);
    }

    public static class FinancialManager {
        private final File jsonFile;
        private final List<JSONObject> invoices = new ArrayList<>();
        private int remFreePerks;  // 剩余的免费升级次数
        private int totalPerks;
        private int availPerks;
        private int totalExp = 0;
        private int level = 1;
        private int expInThisLevel;
        private int money;
        private int cumulativeAwards;  // 历史上的税前总奖金

        FinancialManager(CareerSave save) {
            jsonFile = new File(save.getDir(), "financial.json");
        }

        boolean loadFromJson() {
            if (jsonFile.exists()) {
                try {
                    JSONObject json = DataLoader.loadFromDisk(jsonFile.getAbsolutePath());

                    String checksum = json.has("checksum") ? json.getString("checksum") : "";
                    JSONObject jsonObject = json.getJSONObject("financial");
                    
                    String curCheck = JsonChecksum.checksum(jsonObject);
                    if (!curCheck.equals(checksum)) {
                        EventLogger.warning("You hacked your financial record!");
                    }

                    remFreePerks = jsonObject.optInt("remFreePerks", 0);
                    availPerks = jsonObject.has("availPerks") ? jsonObject.getInt("availPerks") : 0;
                    totalPerks = jsonObject.has("totalPerks") ? jsonObject.getInt("totalPerks") : 0;
                    totalExp = jsonObject.has("totalExp") ? jsonObject.getInt("totalExp") : 0;
                    level = jsonObject.has("level") ? jsonObject.getInt("level") : 1;
                    expInThisLevel = jsonObject.has("expInThisLevel") ? jsonObject.getInt("expInThisLevel") : 0;
                    money = jsonObject.has("money") ? jsonObject.getInt("money") : 0;

                    if (jsonObject.has("invoices")) {
                        JSONArray invoiceArr = jsonObject.getJSONArray("invoices");
                        for (int i = 0; i < invoiceArr.length(); i++) {
                            JSONObject invObj = invoiceArr.getJSONObject(i);
                            invoices.add(invObj);
                        }
                    }

                    return true;
                } catch (JSONException e) {
                    return false;
                }
            } else {
                return false;
            }
        }

        JSONObject toJson() {
            JSONObject json = new JSONObject();

            JSONObject out = new JSONObject();
            out.put("remFreePerks", remFreePerks);
            out.put("availPerks", availPerks);
            out.put("totalPerks", totalPerks);
            out.put("totalExp", totalExp);
            out.put("level", level);
            out.put("expInThisLevel", expInThisLevel);
            out.put("money", money);

            JSONArray invoiceArr = new JSONArray();
            for (JSONObject inv : invoices) {
                invoiceArr.put(inv);
            }
            out.put("invoices", invoiceArr);

            json.put("financial", out);

            String checksum = JsonChecksum.checksum(out);

            json.put("checksum", checksum);

            return json;
        }

        void writeToDisk() {
            JSONObject json = toJson();
            DataLoader.saveToDisk(json, jsonFile.getAbsolutePath());
        }
    }
    
    public record AwardDistributionHint(int money, int exp) {
    }
}
