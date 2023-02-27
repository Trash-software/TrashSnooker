package trashsoftware.trashSnooker.core.career;

import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.metrics.BallMetrics;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;
import trashsoftware.trashSnooker.core.phy.TableCloth;

import java.util.*;

public class ChampionshipData {

    public static final int[] MONTH_DAYS = {
            31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    };

    String id;
    String name;
    GameRule type;
    int seedPlaces;  // 直接进正赛的种子选手数
    int mainPlaces;  // 正赛参赛人数
    int mainRounds;
    int[] preMatchNewAdded;  // 预赛每一轮新加的选手
    boolean professionalOnly;  // 是否仅允许职业选手参加
    boolean ranked;  // 是否为排名赛
    int month;  // 真实的month，从1开始的，注意和calendar转化
    int day;
    Selection selection;

    Map<ChampionshipStage, Integer> stageFrames = new HashMap<>();  // 每一轮的总局数
    ChampionshipStage[] stages;  // 决赛在前
    ChampionshipScore.Rank[] ranksOfLosers;  // 决赛在前，各个的输家的rank
    ChampionshipScore.Rank[] ranksOfAll;  // 也就比ranksOfLosers多一个冠军

    Map<ChampionshipScore.Rank, Integer> awards = new HashMap<>();  // 每个等级的奖金，包含单杆最高
    Map<ChampionshipScore.Rank, Integer> expMap = new HashMap<>();  // 每个等级的技能点

    TableSpec tableSpec;

    public static ChampionshipData fromJsonObject(JSONObject jsonObject) {
        ChampionshipData data = new ChampionshipData();

        data.id = jsonObject.getString("id");
        data.name = jsonObject.getString("name");
        data.type = GameRule.valueOf(jsonObject.getString("type").toUpperCase(Locale.ROOT));
        data.seedPlaces = jsonObject.getInt("seeds");
        data.mainPlaces = jsonObject.getInt("places");
        data.mainRounds = Algebra.log2(data.mainPlaces);
        JSONArray preNewAdd = jsonObject.getJSONArray("pre_matches");
        data.preMatchNewAdded = new int[preNewAdd.length()];
        for (int i = 0; i < data.preMatchNewAdded.length; i++) {
            data.preMatchNewAdded[i] = preNewAdd.getInt(i);
        }

        data.selection = jsonObject.has("selection") ?
                Selection.valueOf(jsonObject.getString("selection").toUpperCase(Locale.ROOT)) :
                Selection.REGULAR;

        data.ranked = jsonObject.getBoolean("ranked");
        data.professionalOnly = jsonObject.getBoolean("professional");

        String[] date = jsonObject.getString("date").split("/");
        data.month = Integer.parseInt(date[0]);
        data.day = Integer.parseInt(date[1]);

        JSONArray frames = jsonObject.getJSONArray("frames");
        data.analyzeFramesStages(frames);

        JSONArray awards = jsonObject.getJSONArray("awards");
        JSONArray expList = jsonObject.getJSONArray("exp");

        for (int i = 0; i < data.ranksOfAll.length; i++) {
            int awd = i < awards.length() ? awards.getInt(i) : 0;
            int exp = i < expList.length() ? expList.getInt(i) : 0;
            ChampionshipScore.Rank rank = data.ranksOfAll[i];
            data.awards.put(rank, awd);
            data.expMap.put(rank, exp);
        }

        data.awards.put(ChampionshipScore.Rank.BEST_SINGLE,
                jsonObject.has("best_single") ?
                        jsonObject.getInt("best_single") :
                        0);
        if (jsonObject.has("147")) {
            data.awards.put(ChampionshipScore.Rank.MAXIMUM, jsonObject.getInt("147"));
        }

        System.out.println(data.id);
        System.out.println(Arrays.toString(data.stages));
        System.out.println(Arrays.toString(data.ranksOfLosers));
        System.out.println(data.awards);
        System.out.println(data.stageFrames);

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
        if (jsonObject.has("table")) {
            JSONObject table = jsonObject.getJSONObject("table");
            cloth = new TableCloth(
                    TableCloth.Goodness.valueOf(table.getString("goodness").toUpperCase(Locale.ROOT)),
                    TableCloth.Smoothness.valueOf(table.getString("smoothness").toUpperCase(Locale.ROOT))
            );
            metrics = factory
                    .create()
                    .holeSize(holeSize(table.getString("hole_size"), factory.supportedHoles))
                    .build();
        } else {
            cloth = new TableCloth(
                    TableCloth.Goodness.GOOD,
                    TableCloth.Smoothness.NORMAL
            );
            metrics = factory
                    .create()
                    .holeSize(factory.defaultHole())
                    .build();
        }

        BallMetrics ballMetrics;
        switch (getType()) {
            case SNOOKER:
            case MINI_SNOOKER:
                ballMetrics = BallMetrics.SNOOKER_BALL;
                break;
            case CHINESE_EIGHT:
            case SIDE_POCKET:
                ballMetrics = BallMetrics.POOL_BALL;
                break;
            default:
                throw new RuntimeException();
        }

        tableSpec = new TableSpec(cloth, metrics, ballMetrics);
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
        return awards.get(rank);
    }

    public int getExpByRank(ChampionshipScore.Rank rank) {
        return expMap.get(rank);
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

    public int getTotalPlaces() {
        return mainPlaces + Arrays.stream(preMatchNewAdded).sum();
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

    private TableMetrics.TableBuilderFactory tableType() {
        switch (type) {
            case SNOOKER:
                return TableMetrics.TableBuilderFactory.SNOOKER;
            case CHINESE_EIGHT:
                return TableMetrics.TableBuilderFactory.CHINESE_EIGHT;
            case SIDE_POCKET:
                return TableMetrics.TableBuilderFactory.SIDE_POCKET;
            default:
                throw new RuntimeException("Unknown game type " + type);
        }
    }

    private TableMetrics.HoleSize holeSize(String jsonString, TableMetrics.HoleSize[] supported) {
        int index;
        switch (jsonString.toLowerCase(Locale.ROOT)) {
            case "big":
                index = 0;
                break;
            case "mid":
                index = 1;
                break;
            case "small":
                index = 2;
                break;
            default:
                throw new RuntimeException("Unknown hole size " + jsonString);
        }
        return supported[index];
    }

    public Calendar toCalendar(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day);
        return calendar;
    }

    public TableSpec getTableSpec() {
        return tableSpec;
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

    public enum Selection {
        REGULAR("常规"),
        SINGLE_SEASON("单赛季"),
        ALL_CHAMP("冠军");

        final String shown;

        Selection(String shown) {
            this.shown = shown;
        }

        @Override
        public String toString() {
            return shown;
        }
    }

    public static class WithYear {
        public final ChampionshipData data;
        public final int year;

        WithYear(ChampionshipData data, int year) {
            this.data = data;
            this.year = year;
        }

        public String fullName() {
            return year + " " + data.getName();
        }
    }

    public static class TableSpec {
        public final TableCloth tableCloth;
        public final TableMetrics tableMetrics;
        public final BallMetrics ballMetrics;

        private TableSpec(TableCloth tableCloth, TableMetrics tableMetrics, BallMetrics ballMetrics) {
            this.tableCloth = tableCloth;
            this.tableMetrics = tableMetrics;
            this.ballMetrics = ballMetrics;
        }
    }
}
