package trashsoftware.trashSnooker.core.career;

import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.career.championship.SnookerBreakScore;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

import java.util.*;

/**
 * 一个人一次赛事的成绩
 */
public class ChampionshipScore {

    public final ChampionshipData data;
    public final Calendar timestamp;
    public final Rank[] ranks;  // 一般也就一个，两个的时候是额外的奖，比如单杆最高
    private final SnookerBreakScore bestBreak;

    public ChampionshipScore(String championshipId,
                             int year,
                             Rank[] ranks,
                             SnookerBreakScore bestBreak) {
        this.data = ChampDataManager.getInstance().findDataById(championshipId);
        this.ranks = ranks;
        this.bestBreak = bestBreak;

        this.timestamp = data.toCalendar(year);
    }

    public static ChampionshipScore fromJson(JSONObject jsonObject) {
        JSONArray ranksArr = jsonObject.getJSONArray("ranks");
        Rank[] ranks = new Rank[ranksArr.length()];
        for (int i = 0; i < ranks.length; i++) {
            ranks[i] = Rank.valueOf(ranksArr.getString(i));
        }

        SnookerBreakScore best = null;
        if (jsonObject.has("highestBreak")) {
            JSONObject bbo = jsonObject.getJSONObject("highestBreak");
            best = SnookerBreakScore.fromJson(bbo);
        }
        
        return new ChampionshipScore(
                jsonObject.getString("id"),
                jsonObject.getInt("year"),
                ranks,
                best
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
        
        if (bestBreak != null) {
            out.put("highestBreak", bestBreak.toJson());
        }

        return out;
    }

    public int getYear() {
        return timestamp.get(Calendar.YEAR);
    }
    
    public SnookerBreakScore getHighestBreak() {
        return bestBreak;
    }

    public enum Rank implements Comparable<Rank> {
        CHAMPION(true, true),
        SECOND_PLACE(true, true),
        TOP_4(true, true),
        TOP_8(true, true),
        TOP_16(true, true),
        TOP_32(true, true),
        TOP_64(true, true),
        PRE_GAMES_4(false, true),
        PRE_GAMES_3(false, true),
        PRE_GAMES_2(false, true),
        PRE_GAMES_1(false, true),
        BEST_SINGLE(false, false),
        MAXIMUM(false, false),
        GOLD_MAXIMUM(false, false);

        public final boolean isMain;
        public final boolean ranked;  // 是否计入排名

        Rank(boolean isMain, boolean ranked) {
            this.isMain = isMain;
            this.ranked = ranked;
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
            return App.getStrings().getString(Util.toLowerCamelCase(name()));
        }
        
        public static Rank[] getAllRanked() {
            int count = 0;
            for (Rank rank : values()) if (rank.ranked) count++;
            
            Rank[] res = new Rank[count];
            int i = 0;
            for (Rank rank : values()) {
                if (rank.ranked) res[i++] = rank;
            }
            return res;
        }
    }
}
