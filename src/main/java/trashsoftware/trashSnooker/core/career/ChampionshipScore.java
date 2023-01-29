package trashsoftware.trashSnooker.core.career;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * 一个人一次赛事的成绩
 */
public class ChampionshipScore {
    
    public final ChampionshipData data;
    public final Calendar timestamp;
    public final Rank[] ranks;  // 一般也就一个，两个的时候是额外的奖，比如单杆最高

    public ChampionshipScore(String championshipId,
                             int year,
                             Rank[] ranks) {
        this.data = ChampDataManager.getInstance().findDataById(championshipId);
        this.ranks = ranks;
        
        this.timestamp = data.toCalendar(year);
    }
    
    public static ChampionshipScore fromJson(JSONObject jsonObject) {
        JSONArray ranksArr = jsonObject.getJSONArray("ranks");
        Rank[] ranks = new Rank[ranksArr.length()];
        for (int i = 0; i < ranks.length; i++) {
            ranks[i] = Rank.valueOf(ranksArr.getString(i));
        }
        return new ChampionshipScore(
                jsonObject.getString("id"),
                jsonObject.getInt("year"),
                ranks
        );
    }
    
    public JSONObject toJsonObject() {
        JSONObject out = new JSONObject();
        out.put("id", data.id);
        out.put("year", getYear());

        JSONArray ranksArr = new JSONArray();
        for (Rank rank : ranks) {
            ranksArr.put(rank.name());
        }
        out.put("ranks", ranksArr);
        
        return out;
    }
    
    public int getYear() {
        return timestamp.get(Calendar.YEAR);
    }

    public enum Rank {
        CHAMPION("冠军"),
        SECOND_PLACE("亚军"),
        TOP_4("四强"),
        TOP_8("八强"),
        TOP_16("十六强"),
        TOP_32("三十二强"),
        TOP_64("六十四强"),
        PRE_GAMES("预赛"),
        BEST_SINGLE("单杆最高");

        private final String shown;

        Rank(String shown) {
            this.shown = shown;
        }

//        @Override
//        public String toString() {
//            return shown;
//        }
    }
}
