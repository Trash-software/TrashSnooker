package trashsoftware.trashSnooker.core.career;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.SubRule;
import trashsoftware.trashSnooker.core.metrics.*;
import trashsoftware.trashSnooker.core.phy.TableCloth;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.Util;

import java.util.*;

public class ChampionshipData {

    public static final int[] MONTH_DAYS = {
            31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    };

    String id;
    String name;
    GameRule type;
    int classLevel = 5;
    int seedPlaces;  // 直接进正赛的种子选手数
    int mainPlaces;  // 正赛参赛人数
    int mainRounds;
    int[] preMatchNewAdded;  // 预赛每一轮新加的选手
    Map<ChampionshipStage, Integer> stageNewAdd;  // 每一轮新加的选手的数量
    boolean professionalOnly;  // 是否仅允许职业选手参加
    boolean ranked;  // 是否为排名赛
    int forbidden;  // 排名前多少的球员不允许参加
    int month;  // 真实的month，从1开始的，注意和calendar转化
    int day;
    Selection selection;
    int expPerFrame;  // 假如每轮打满，每局比赛的exp
    Map<ChampionshipStage, Integer> stageFrames = new HashMap<>();  // 每一轮的总局数
    ChampionshipStage[] stages;  // 决赛在前
    ChampionshipScore.Rank[] ranksOfLosers;  // 决赛在前，各个的输家的rank
    ChampionshipScore.Rank[] ranksOfAll;  // 也就比ranksOfLosers多一个冠军
    Map<ChampionshipScore.Rank, Integer> awards = new HashMap<>();  // 每个等级的奖金，包含单杆最高
    Map<ChampionshipScore.Rank, Integer> expMap = new HashMap<>();  // 每个等级的技能点
    TablePreset tablePreset;
    TableSpec tableSpec;
    BallMetrics ballMetrics;
    Collection<SubRule> subRules;
    ChampionshipLocation location;
    private int registryFee;
    private int flightFee;
    private int hotelFee;
    /**
     * 种子选手的邀请金
     * 注意：会累加
     * 比如{16:1000, 4: 2000}
     * 则排行前4的选手会拿到3000
     */
    private final SortedMap<Integer, Integer> inviteAwardMap = new TreeMap<>();
    private String description;
    private String sponsor;

    public static ChampionshipData fromJsonObject(JSONObject jsonObject) {
        ChampionshipData data = new ChampionshipData();

        data.id = jsonObject.getString("id");
        data.name = DataLoader.getObjectOfLocale(jsonObject.get("names"));
        if (jsonObject.has("description")) {
            data.description = DataLoader.getObjectOfLocale(jsonObject.get("description"));
        } else {
            data.description = "";
        }

        if (jsonObject.has("sponsor")) {
            data.sponsor = DataLoader.getObjectOfLocale(jsonObject.get("sponsor"));
        } else {
            data.sponsor = "";
        }

        data.type = GameRule.valueOf(jsonObject.getString("type").toUpperCase(Locale.ROOT));
        data.seedPlaces = jsonObject.getInt("seeds");
        data.mainPlaces = jsonObject.getInt("places");
        data.mainRounds = Algebra.log2(data.mainPlaces);
        JSONArray preNewAdd = jsonObject.getJSONArray("pre_matches");
        data.preMatchNewAdded = new int[preNewAdd.length()];
        for (int i = 0; i < data.preMatchNewAdded.length; i++) {
            data.preMatchNewAdded[i] = preNewAdd.getInt(i);
        }

        if (jsonObject.has("subRules")) {
            data.subRules = new ArrayList<>();
            JSONArray subRuleJson = jsonObject.getJSONArray("subRules");
            for (int i = 0; i < subRuleJson.length(); i++) {
                data.subRules.add(SubRule.valueOf(subRuleJson.getString(i)));
            }
        } else {
            data.subRules = SubRule.defaultSubRule(data.type);
        }

        data.selection = jsonObject.has("selection") ?
                Selection.valueOf(jsonObject.getString("selection").toUpperCase(Locale.ROOT)) :
                Selection.REGULAR;

        data.ranked = jsonObject.getBoolean("ranked");
        data.professionalOnly = jsonObject.getBoolean("professional");
        if (jsonObject.has("forbidden")) {
            data.forbidden = jsonObject.getInt("forbidden");
        } else {
            data.forbidden = 0;
        }

        if (jsonObject.has("class")) {
            data.classLevel = jsonObject.getInt("class");
        } else {
            data.classLevel = 5;
        }

        String[] date = jsonObject.getString("date").split("/");
        data.month = Integer.parseInt(date[0]);
        data.day = Integer.parseInt(date[1]);

        JSONObject location = jsonObject.optJSONObject("location", null);
        ChampionshipLocation cl = ChampionshipLocation.CHN;
        if (location != null) {
            try {
                cl = ChampionshipLocation.valueOf(location.optString("country"));
            } catch (JSONException | IllegalArgumentException iae) {
                System.err.println("Championship " + data.id + " cannot get country.");
            }
        }
        data.location = cl;

        JSONArray frames = jsonObject.getJSONArray("frames");
        data.analyzeFramesStages(frames);

        JSONArray awards = jsonObject.getJSONArray("awards");

        data.expPerFrame = jsonObject.getInt("frameExp");

        for (int i = 0; i < data.ranksOfAll.length; i++) {
            int awd = i < awards.length() ? awards.getInt(i) : 0;
            ChampionshipScore.Rank rank = data.ranksOfAll[i];

//            int exp = i < expList.length() ? expList.getInt(i) : 0;
            data.awards.put(rank, awd);
        }

        int cumulativeTotalFrames = 0;
        for (int i = data.ranksOfAll.length - 1; i >= 1; i--) {
            ChampionshipScore.Rank rank = data.ranksOfAll[i];
            int exp;
            ChampionshipStage stage = data.stages[i - 1];

            int frameOf = data.getNFramesOfStage(stage);

            exp = cumulativeTotalFrames * data.expPerFrame;
            cumulativeTotalFrames += frameOf;

            if (exp == 0) {
                exp = frameOf / 2 * data.expPerFrame;  // 一来就洗白。一局一胜那种就算了，不值得得经验
            }

            data.expMap.put(rank, exp);
        }
        data.expMap.put(ChampionshipScore.Rank.CHAMPION, cumulativeTotalFrames * data.expPerFrame);

        data.awards.put(ChampionshipScore.Rank.BEST_SINGLE,
                jsonObject.has("best_single") ?
                        jsonObject.getInt("best_single") :
                        0);
        if (jsonObject.has("maximum")) {
            data.awards.put(ChampionshipScore.Rank.MAXIMUM, jsonObject.getInt("maximum"));
        }
        if (jsonObject.has("167")) {
            data.awards.put(ChampionshipScore.Rank.GOLD_MAXIMUM, jsonObject.getInt("167"));
        }

        if (jsonObject.has("fees")) {
            JSONObject fees = jsonObject.getJSONObject("fees");
            data.registryFee = fees.getInt("registry");
            data.flightFee = fees.getInt("flight");
            data.hotelFee = fees.getInt("hotel");
        } else {
            System.err.println("Championship " + data.id + " contains no fees.");
        }

        if (jsonObject.has("invite")) {
            JSONObject invite = jsonObject.getJSONObject("invite");
            for (String key : invite.keySet()) {
                int value = invite.getInt(key);
                data.inviteAwardMap.put(Integer.parseInt(key), value);
            }
        }

//        System.out.println(data.id);
//        System.out.println(Arrays.toString(data.stages));
//        System.out.println(Arrays.toString(data.ranksOfLosers));
//        System.out.println(data.awards);
//        System.out.println(data.expMap);
//        System.out.println(data.stageFrames);

        data.setupTable(jsonObject);

        return data;
    }

    public static int dayOfYear(int month, int day) {
        // 只是个大概，能比出先后就行了
        int dayOfYear = 1;
        for (int i = 1; i < month; i++) {
            dayOfYear += MONTH_DAYS[i - 1];
        }
        dayOfYear += day;
        return dayOfYear;
    }

    private void setupTable(JSONObject jsonObject) {
        TableMetrics.TableBuilderFactory factory = tableType();

        TableCloth cloth;
        TableMetrics metrics;
        if (jsonObject.has("tablePreset")) {
            tablePreset = DataLoader.getInstance().getTablesOfType(factory.key).get(jsonObject.getString("tablePreset"));
            tableSpec = tablePreset.tableSpec;
        } else if (jsonObject.has("table")) {
            JSONObject table = jsonObject.getJSONObject("table");
            tableSpec = TableSpec.fromJsonObject(
                    table, factory
            );
        } else {
            cloth = new TableCloth(
                    TableCloth.Goodness.GOOD,
                    TableCloth.Smoothness.NORMAL
            );
            metrics = factory
                    .create()
                    .pocketDifficulty(factory.defaultDifficulty())
                    .holeSize(factory.defaultPocketSize())
                    .build();
            tableSpec = new TableSpec(cloth, metrics);
        }

        switch (getType()) {
            case SNOOKER:
            case MINI_SNOOKER:
            case SNOOKER_TEN:
                ballMetrics = BallMetrics.SNOOKER_BALL;
                break;
            case CHINESE_EIGHT:
            case LIS_EIGHT:
            case AMERICAN_NINE:
                ballMetrics = BallMetrics.POOL_BALL;
                break;
            default:
                throw new RuntimeException();
        }
    }

    public TablePreset getTablePreset() {
        return tablePreset;
    }

    /**
     * 必须在人数设好之后调用
     */
    private void analyzeFramesStages(JSONArray frames) {
//        int nonSeedMainPos = mainPlaces - seedPlaces;
        int preRounds = preMatchNewAdded.length;

        stages = ChampionshipStage.getSequence(mainRounds, preRounds);
        ranksOfLosers = ChampionshipScore.Rank.getSequenceOfLosers(mainRounds, preRounds);
        ranksOfAll = new ChampionshipScore.Rank[ranksOfLosers.length + 1];
        ranksOfAll[0] = ChampionshipScore.Rank.CHAMPION;
        System.arraycopy(ranksOfLosers, 0, ranksOfAll, 1, ranksOfLosers.length);

        for (int i = 0; i < stages.length; i++) {
            stageFrames.put(stages[i], frames.getInt(i));
        }

//        for (int i = 0; i < mainRounds; i++) {
//            ChampionshipStage stage;
//            if (i <= 2) {  // 从四分之一决赛开始名字就定了
//                stage = ChampionshipStage.values()[i];
//            } else {
//                stage = ChampionshipStage.values()[mainSkip + i];
//            }
//            stages.add(stage);
//            ranks.add(ChampionshipScore.Rank.values()[i + 1]);
//            stageFrames.put(stage, frames.getInt(index++));
//        }
//        for (int i = 0; i < preRounds; i++) {
//            ChampionshipStage stage = ChampionshipStage.values()[i + 6 + (4 - preRounds)];
//            stages.add(stage);
//            ranks.add(ChampionshipScore.Rank.values()[ChampionshipScore.Rank.preGameEndIndex() - i]);
//            stageFrames.put(stage, frames.getInt(index++));
//        }
//        if (stages.size() != frames.length()) throw new RuntimeException();
    }

    public int getAwardByRank(ChampionshipScore.Rank rank) {
        Integer awd = awards.get(rank);
        return awd == null ? 0 : awd;
    }

    public int getExpByRank(ChampionshipScore.Rank rank) {
        Integer ex = expMap.get(rank);
        return ex == null ? 0 : ex;
    }

    public Selection getSelection() {
        return selection;
    }

    public String getId() {
        return id;
    }

    public GameRule getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public int getMainPlaces() {
        return mainPlaces;
    }

    public int getSeedPlaces() {
        return seedPlaces;
    }

    public int[] getPreMatchNewAdded() {
        return preMatchNewAdded;
    }

    public Map<ChampionshipStage, Integer> getStageNewAdd() {
        return stageNewAdd;
    }

    public int getClassLevel() {
        return classLevel;
    }

    public boolean hasForbidden() {
        return forbidden > 0;
    }

    public int getForbidden() {
        return forbidden;
    }

    public int getTotalPlaces() {
        if (preMatchNewAdded.length == 0) {
            return mainPlaces;
        } else {
            return seedPlaces + Arrays.stream(preMatchNewAdded).sum();
        }
    }

    public boolean isProfessionalOnly() {
        return professionalOnly;
    }

    public boolean isRanked() {
        return ranked;
    }

    public ChampionshipStage[] getStages() {
        return stages;
    }

    public ChampionshipScore.Rank[] getRanksOfLosers() {
        return ranksOfLosers;
    }

    public int getNFramesOfStage(ChampionshipStage stage) {
        return stageFrames.get(stage);
    }

    public Map<ChampionshipScore.Rank, Integer> getExpMap() {
        return expMap;
    }

    public Map<ChampionshipScore.Rank, Integer> getAwards() {
        return awards;
    }

    public Collection<SubRule> getSubRules() {
        return subRules;
    }

    public int getRegistryFee() {
        return registryFee;
    }

    public int getHotelFee() {
        return hotelFee;
    }

    public int getFlightFee() {
        return flightFee;
    }

    public SortedMap<Integer, Integer> getInviteAwardMap() {
        return inviteAwardMap;
    }

    private TableMetrics.TableBuilderFactory tableType() {
        return switch (type) {
            case SNOOKER -> TableMetrics.TableBuilderFactory.SNOOKER;
            case CHINESE_EIGHT, LIS_EIGHT -> TableMetrics.TableBuilderFactory.CHINESE_EIGHT;
            case AMERICAN_NINE -> TableMetrics.TableBuilderFactory.POOL_TABLE_9;
            default -> throw new RuntimeException("Unknown game type " + type);
        };
    }

    public Calendar toCalendar(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day);
        return calendar;
    }

    public TableSpec getTableSpec() {
        return tableSpec;
    }

    public BallMetrics getBallMetrics() {
        return ballMetrics;
    }

    private void appendAwardString(ChampionshipScore.Rank rank, StringBuilder builder) {
        int award = getAwardByRank(rank);
        int exp = getExpByRank(rank);
        if (award == 0 && exp == 0) return;
        builder.append(rank.getShown())
                .append(": ")
                .append(award)
                .append(" / ")
                .append(exp)
                .append('\n');
    }

    public String awardsString() {
        StringBuilder builder = new StringBuilder();

        if (ranked) {
            builder.append("排名赛。");
        } else {
            builder.append("非排名赛。");
        }
        builder.append("正赛名额：")
                .append(mainPlaces)
                .append("\n")
                .append("奖金/经验：\n");
        appendAwardString(ChampionshipScore.Rank.CHAMPION, builder);
        for (ChampionshipScore.Rank rank : getRanksOfLosers()) {
            appendAwardString(rank, builder);
        }

        return builder.toString();
    }

    public int getDay() {
        return day;
    }

    public int getMonth() {
        return month;
    }

    public String getDescription() {
        return description;
    }

    public String getSponsor() {
        return sponsor;
    }

    public WithYear getWithYear(int year) {
        return new WithYear(this, year);
    }

    public enum Selection {
        REGULAR,
        SINGLE_SEASON,
        ALL_CHAMP;

        @Override
        public String toString() {
            return App.getStrings().getString(Util.toLowerCamelCase("SEL_" + name()));
        }
    }

    public static class WithYear implements Comparable<WithYear> {
        public final ChampionshipData data;
        public final int year;

        WithYear(ChampionshipData data, int year) {
            this.data = data;
            this.year = year;
        }

        @Override
        public int compareTo(@NotNull ChampionshipData.WithYear o) {
            int yearCmp = Integer.compare(year, o.year);
            if (yearCmp != 0) return yearCmp;
            int monthCmp = Integer.compare(data.month, o.data.month);
            if (monthCmp != 0) return monthCmp;
            return Integer.compare(data.day, o.data.day);
        }

        public Calendar toCalendar() {
            return data.toCalendar(year);
        }

        public String fullName() {
            return year + " " + data.getName();
        }

        public String fullId() {
            return year + "+" + data.getId();
        }

        public static WithYear fromFullId(String fullId, ChampDataManager cdm) {
            String[] split = fullId.split("\\+");
            if (split.length != 2) throw new RuntimeException("Invalid full id: " + fullId);

            int year = Integer.parseInt(split[0]);
            ChampionshipData data1 = cdm.findDataById(split[1]);
            return new WithYear(data1, year);
        }
    }
}
