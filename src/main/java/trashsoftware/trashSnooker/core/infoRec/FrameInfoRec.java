package trashsoftware.trashSnooker.core.infoRec;

import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;

import java.util.ArrayList;
import java.util.List;

public class FrameInfoRec {
    
    protected final MatchInfoRec parent;
    protected List<CueInfoRec> cueRecs = new ArrayList<>();
    protected int winner;  // player 1 or 2, can be 0 in the case that the game is restarted
    protected FrameAnalyze<?> frameAnalyze;
    /**
     * 为了区分八球和九球加入了开球违例后重开本局
     * frameIndex, frameNumber: 程序内创建的第几局，与台球规则上应是第几局
     * 比如某一局重开了，则新的一局会有新的frameIndex，但frameNumber不变
     * 而且是从1开始数的
     */
    protected final int frameIndex, frameNumber;
    
    FrameInfoRec(MatchInfoRec parent, int frameIndex, int frameNumber) {
        this.parent = parent;
        this.frameIndex = frameIndex;
        this.frameNumber = frameNumber;
    }
    
    public static FrameInfoRec fromJson(MatchInfoRec parent, int frameIndex, JSONObject json) {
        int frameNumber = json.optInt("frameNumber", frameIndex);
        
        FrameInfoRec fir = new FrameInfoRec(parent, frameIndex, frameNumber);
        if (json.has("winner")) fir.winner = json.getInt("winner");
        JSONArray curRecArray = json.getJSONArray("cueRecs");
        for (int i = 0; i < curRecArray.length(); i++) {
            fir.cueRecs.add(CueInfoRec.fromJson(curRecArray.getJSONObject(i)));
        }
        return fir;
    }
    
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        JSONArray cueRecArray = new JSONArray();
        for (CueInfoRec cir : cueRecs) {
            cueRecArray.put(cir.toJson());
        }
        json.put("cueRecs", cueRecArray);
        if (winner != 0) {
            json.put("winner", winner);
        }
        json.put("frameIndex", frameIndex);
        json.put("frameNumber", frameNumber);
        return json;
    }

    public List<CueInfoRec> getCueRecs() {
        return cueRecs;
    }
    
    private void analyzeFrame() {
        if (parent.gameValues.rule.snookerLike()) {
            frameAnalyze = new SnookerFrameAnalyze(parent.gameValues.rule);
        } else if (parent.gameValues.rule.eightBallLike()) {
            frameAnalyze = new EightBallFrameAnalyze(parent.gameValues.rule);
        }
        
        if (frameAnalyze != null) {
            frameAnalyze.analyze(this);
        }
    }
    
    public FrameAnalyze<?> getFrameAnalyze() {
        if (frameAnalyze == null) analyzeFrame();
        
        return frameAnalyze;
    }
    
    public void populateAttackAnalysis(AttackAnalysis attackAnalysis, TableMetrics tableMetrics) {
        for (CueInfoRec cir : getCueRecs()) {
            if (cir.potInfo != null) {
                AttackAnalysis.PotAttemptRec par = AttackAnalysis.PotAttemptRec.create(cir, tableMetrics, frameIndex);
                attackAnalysis.addAttempt(par, cir.player);
            }
        }
    }
}
