package trashsoftware.trashSnooker.core.career.championship;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.career.ChampionshipStage;

public class SnookerBreakScore implements Comparable<SnookerBreakScore> {
    public final String playerId;
    public final int score;
    public final ChampionshipStage stage;
    public final boolean sim;  // 是否是AiVsAi模拟出来的
    public final String matchId;
    public final int numFrame;  // 第几局，从1开始
    
    SnookerBreakScore(String playerId, int score, ChampionshipStage stage, boolean isSim,
                      String matchId, int numFrame) {
        this.playerId = playerId;
        this.score = score;
        this.stage = stage;
        this.sim = isSim;
        this.matchId = matchId;
        this.numFrame = numFrame;
    }
    
    public static SnookerBreakScore fromJson(JSONObject object) {
        return new SnookerBreakScore(
                object.getString("playerId"),
                object.getInt("score"),
                ChampionshipStage.valueOf(object.getString("stage")),
                object.getBoolean("sim"),
                object.getString("matchId"),
                object.getInt("numFrame")
        );
    }
    
    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        object.put("playerId", playerId);
        object.put("score", score);
        object.put("stage", stage.name());
        object.put("sim", sim);
        object.put("matchId", matchId);
        object.put("numFrame", numFrame);
        return object;
    }

    @Override
    public int compareTo(@NotNull SnookerBreakScore o) {
        int scoreCmp = Integer.compare(this.score, o.score);
        if (scoreCmp != 0) return -scoreCmp;  // 大的在前
        
        int stageCmp = Integer.compare(this.stage.ordinal(), o.stage.ordinal());
        if (stageCmp != 0) return scoreCmp;  // ordinal小（接近决赛）的在前
        
        if (this.sim && !o.sim) return 1;  // 假的靠后
        if (!this.sim && o.sim) return -1;
        return 0;
    }
}
