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
    private static final String[] SNOOKER_THREE_BIG_IDS = {
            "world_champ", "masters", "united_kingdom_champ"
    };
    private static ChampDataManager instance;
    /**
     * 一年内的所有赛事，按时间排序
     */
    private final List<ChampionshipData> championshipData = new ArrayList<>();

    private final List<ChampionshipData> snookerThreeBig = new ArrayList<>();

    public static ChampDataManager getInstance() {
        if (instance == null) {
            instance = new ChampDataManager();
            JSONObject root = DataLoader.loadFromDisk(DATA_FILE);
            JSONArray array = root.getJSONArray("tournaments");
            for (int i = 0; i < array.length(); i++) {
                JSONObject champObj = array.getJSONObject(i);
                ChampionshipData data = ChampionshipData.fromJsonObject(champObj);
                instance.championshipData.add(data);

                if (Util.arrayContainsEqual(SNOOKER_THREE_BIG_IDS, data.id)) {
                    instance.snookerThreeBig.add(data);
                }
            }
            instance.championshipData.sort(Comparator.comparingInt((ChampionshipData o) -> o.month).thenComparingInt(o -> o.day));
        }
        return instance;
    }

    public List<ChampionshipData> getChampionshipData() {
        return championshipData;
    }

    public List<ChampionshipData> getSnookerThreeBig() {
        return snookerThreeBig;
    }

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

    public ChampionshipData findDataById(String championshipId) {
        for (ChampionshipData data : championshipData) {
            if (data.id.equals(championshipId)) return data;
        }
        throw new RuntimeException("No championship named " + championshipId);
    }
}
