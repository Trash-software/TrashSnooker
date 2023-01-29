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
    int[] preMatchNewAdded;  // 预赛每一轮新加的选手
    boolean professionalOnly;  // 是否仅允许职业选手参加
    boolean ranked;  // 是否为排名赛
    int month;  // 真实的month，从1开始的，注意和calendar转化
    int day;
    
    Map<ChampionshipStage, Integer> stageFrames = new HashMap<>();  // 每一轮的总局数
    List<ChampionshipStage> stages = new ArrayList<>();  // 决赛在前
    
    Map<ChampionshipScore.Rank, Integer> awards = new HashMap<>();  // 每个等级的奖金，包含单杆最高
    Map<ChampionshipScore.Rank, Integer> perks = new HashMap<>();  // 每个等级的技能点
    
    TableSpec tableSpec;
    
    public static ChampionshipData fromJsonObject(JSONObject jsonObject) {
        ChampionshipData data = new ChampionshipData();
        
        data.id = jsonObject.getString("id");
        data.name = jsonObject.getString("name");
        data.type = GameRule.valueOf(jsonObject.getString("type").toUpperCase(Locale.ROOT));
        data.seedPlaces = jsonObject.getInt("seeds");
        data.mainPlaces = jsonObject.getInt("places");
        JSONArray preNewAdd = jsonObject.getJSONArray("pre_matches");
        data.preMatchNewAdded = new int[preNewAdd.length()];
        for (int i = 0; i < data.preMatchNewAdded.length; i++) {
            data.preMatchNewAdded[i] = preNewAdd.getInt(i);
        }
        
        data.ranked = jsonObject.getBoolean("ranked");
        data.professionalOnly = jsonObject.getBoolean("professional");
        
        String[] date = jsonObject.getString("date").split("/");
        data.month = Integer.parseInt(date[0]);
        data.day = Integer.parseInt(date[1]);

        JSONArray awards = jsonObject.getJSONArray("awards");
        JSONArray perks = jsonObject.getJSONArray("perks");
        for (int i = 0; i < awards.length(); i++) {
            int awd = awards.getInt(i);
            int perk = perks.getInt(i);
            ChampionshipScore.Rank rank = ChampionshipScore.Rank.values()[i];
            data.awards.put(rank, awd);
            data.perks.put(rank, perk);
        }
        data.awards.put(ChampionshipScore.Rank.BEST_SINGLE, jsonObject.getInt("best_single"));
        System.out.println(data.awards);
        
        JSONArray frames = jsonObject.getJSONArray("frames");
        data.analyzeFramesStages(frames);

        System.out.println(data.id);
        System.out.println(data.stages);
        System.out.println(data.stageFrames);
        
        data.setupTable(jsonObject);
        
        return data;
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
        int mainRounds = Algebra.log2(mainPlaces);
//        int nonSeedMainPos = mainPlaces - seedPlaces;
        int preRounds = preMatchNewAdded.length;

        int index = 0;
        
        for (int i = 0; i < mainRounds; i++) {
            ChampionshipStage stage;
            if (i <= 2) {  // 从四分之一决赛开始名字就定了
                stage = ChampionshipStage.values()[i];
            } else {
                stage = ChampionshipStage.values()[i + (6 - mainRounds)];
            }
            stages.add(stage);
            stageFrames.put(stage, frames.getInt(index++));
        }
        for (int i = 0; i < preRounds; i++) {
            ChampionshipStage stage = ChampionshipStage.values()[i + 6 + (3 - preRounds)];
            stages.add(stage);
            stageFrames.put(stage, frames.getInt(index++));
        }
        if (stages.size() != frames.length()) throw new RuntimeException();
    }
    
    public int getAwardByRank(ChampionshipScore.Rank rank) {
        return Objects.requireNonNullElse(awards.get(rank), 0);
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
        return seedPlaces + Arrays.stream(preMatchNewAdded).sum();
    }

    public boolean isProfessionalOnly() {
        return professionalOnly;
    }

    public boolean isRanked() {
        return ranked;
    }

    public List<ChampionshipStage> getStages() {
        return stages;
    }
    
    public int getNFramesOfStage(ChampionshipStage stage) {
        return stageFrames.get(stage);
    }

    public Map<ChampionshipScore.Rank, Integer> getPerks() {
        return perks;
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

    public static int dayOfYear(int month, int day) {
        // 只是个大概，能比出先后就行了
        int dayOfYear = 1;
        for (int i = 1; i < month; i++) {
            dayOfYear += MONTH_DAYS[i - 1];
        }
        dayOfYear += day;
        return dayOfYear;
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
