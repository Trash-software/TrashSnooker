package trashsoftware.trashSnooker.core.career;

import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChampDataManager {

    public static final String DATA_FILE = "data/tournaments.json";
    private static final String[] SNOOKER_TRIPLE_CROWN_IDS = {
            "world_champ", "masters", "united_kingdom_champ"
    };
    private static ChampDataManager instance;
    /**
     * 一年内的所有赛事，按时间排序
     */
    private final List<ChampionshipData> championshipData = new ArrayList<>();

    private final List<ChampionshipData> snookerTripleCrown = new ArrayList<>();

    public static ChampDataManager getInstance() {
        if (instance == null) {
            instance = new ChampDataManager();
            JSONObject root = DataLoader.loadFromDisk(DATA_FILE);
            JSONArray array = root.getJSONArray("tournaments");
            for (int i = 0; i < array.length(); i++) {
                JSONObject champObj = array.getJSONObject(i);
                ChampionshipData data = ChampionshipData.fromJsonObject(champObj);
                instance.championshipData.add(data);

                if (Util.arrayContainsEqual(SNOOKER_TRIPLE_CROWN_IDS, data.id)) {
                    instance.snookerTripleCrown.add(data);
                }
            }
            instance.championshipData.sort(Comparator.comparingInt((ChampionshipData o) -> o.month).thenComparingInt(o -> o.day));
        }
        return instance;
    }

    public static String[] getSnookerTripleCrownIds() {
        return SNOOKER_TRIPLE_CROWN_IDS;
    }

    public static boolean isSnookerWorldChamp(ChampionshipData data) {
        return SNOOKER_TRIPLE_CROWN_IDS[0].equals(data.id);
    }

    public static boolean isSnookerTripleCrown(ChampionshipData data) {
        return Util.arrayContains(SNOOKER_TRIPLE_CROWN_IDS, data.id);
    }

    public List<ChampionshipData> getChampionshipData() {
        return championshipData;
    }

    public List<ChampionshipData> getSnookerTripleCrown() {
        return snookerTripleCrown;
    }

    /**
     * Immutable
     */
    public ChampionshipData.WithYear getNextChampionship(int year, int month, int day) {
        // month是现实月份，从1开始
        int dayOfYear = ChampionshipData.dayOfYear(month, day);
        for (ChampionshipData data : championshipData) {
            int champDay = ChampionshipData.dayOfYear(data.month, data.day);
            if (champDay > dayOfYear) {
                return new ChampionshipData.WithYear(data, year);
            }
        }
        return new ChampionshipData.WithYear(championshipData.get(0), year + 1);  // 明年的第一个比赛了
    }

    /**
     * Immutable
     */
    public ChampionshipData.WithYear getPreviousChampionship(int year, int month, int day) {
        // month是现实月份，从1开始
        int dayOfYear = ChampionshipData.dayOfYear(month, day);
        for (ChampionshipData data : championshipData.reversed()) {
            int champDay = ChampionshipData.dayOfYear(data.month, data.day);
            if (champDay < dayOfYear) {
                return new ChampionshipData.WithYear(data, year);
            }
        }
        return new ChampionshipData.WithYear(championshipData.get(championshipData.size() - 1), 
                year - 1);  // 去年的最后一个比赛了
    }

    public ChampionshipData findDataById(String championshipId) {
        for (ChampionshipData data : championshipData) {
            if (data.id.equals(championshipId)) return data;
        }
        throw new RuntimeException("No championship named " + championshipId);
    }
}
