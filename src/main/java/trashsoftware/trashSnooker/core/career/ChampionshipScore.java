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

    public enum Rank implements Comparable<Rank> {
        CHAMPION("冠军", true),
        SECOND_PLACE("亚军", true),
        TOP_4("四强", true),
        TOP_8("八强", true),
        TOP_16("十六强", true),
        TOP_32("三十二强", true),
        TOP_64("六十四强", true),
        PRE_GAMES_4("预赛第四轮", false),
        PRE_GAMES_3("预赛第三轮", false),
        PRE_GAMES_2("预赛第二轮", false),
        PRE_GAMES_1("预赛第一轮", false),
        BEST_SINGLE("单杆最高", false),
        MAXIMUM("147", false);

        private final String shown;
        public final boolean isMain;

        Rank(String shown, boolean isMain) {
            this.shown = shown;
            this.isMain = isMain;
        }
        
        public static Rank[] getSequenceOfLosers(int mainRounds, int preRounds) {
            Rank[] res = new Rank[mainRounds + preRounds];
            int mainSkip = SECOND_PLACE.ordinal();
            int preSkip = PRE_GAMES_1.ordinal() - preRounds + 1;
            
            int rounds = mainRounds + preRounds;
            
            for (int i = 0; i < rounds; i++) {
                if (i < mainRounds) {
                    res[i] = values()[i + mainSkip];
                } else {
                    res[i] = values()[i - mainRounds + preSkip];
                }
            }
            return res;
        }

//        @Override
//        public String toString() {
//            return shown;
//        }

        public String getShown() {
            return shown;
        }
    }
}
