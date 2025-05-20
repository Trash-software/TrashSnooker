package trashsoftware.trashSnooker.core.infoRec;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.attempt.AttemptBase;
import trashsoftware.trashSnooker.core.attempt.CueAttempt;
import trashsoftware.trashSnooker.core.FoulInfo;
import trashsoftware.trashSnooker.core.PlayerHand;
import trashsoftware.trashSnooker.core.attempt.PotAttempt;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MatchInfoRec {

    public static final MatchInfoRec INVALID = new MatchInfoRec(null,
            null, null, 0, null, false);
    
    public final String entireBeginTime;  // 
    public final String careerMatchId;
    public final String[] playerIds;
    public final GameValues gameValues;
    public final int totalFrames;
    protected final File recFile;
    public final boolean valid;

    protected final List<FrameInfoRec> frames = new ArrayList<>();

    MatchInfoRec(String entireBeginTime, String careerMatchId, 
                 GameValues gameValues, int totalFrames,
                 String[] playerIds, boolean valid) {
        this.careerMatchId = careerMatchId;
        this.entireBeginTime = entireBeginTime;
        this.gameValues = gameValues;
        this.totalFrames = totalFrames;
        this.playerIds = playerIds;
        this.valid = valid;
        this.recFile = getRecFile(entireBeginTime);
    }

    static File getRecFile(String entireBeginTime) {
        File recFile = new File(DataLoader.INFO_REC_DIR, entireBeginTime + ".json");
        if (!recFile.getParentFile().exists()) {
            if (!recFile.getParentFile().mkdirs()) {
                EventLogger.error("Cannot create file " + recFile.getParentFile().getAbsolutePath());
            }
        }
        return recFile;
    }

    public static MatchInfoRec createMatchRec(GameValues gameValues,
                                              int totalFrames,
                                              String entireBeginTime,
                                              String careerMatchId,
                                              String[] playerIds) {
        MatchInfoRec mir = new MatchInfoRec(entireBeginTime, careerMatchId, gameValues,
                totalFrames, playerIds, true);
        return mir;
    }

    public static MatchInfoRec loadMatchRec(String entireBeginTime) {
        return loadMatchRec(entireBeginTime, INVALID);
    }

    public static MatchInfoRec loadMatchRec(String entireBeginTime, MatchInfoRec defaultValue) {
        File file = getRecFile(entireBeginTime);
        if (file.exists()) {
            JSONObject loaded = DataLoader.loadFromDisk(file.getAbsolutePath());
            try {
                JSONObject valuesObj = loaded.getJSONObject("gameValues");
                GameValues gameValues = GameValues.fromJson(valuesObj);
                JSONArray playerArray = loaded.getJSONArray("players");
                String[] playerIds = new String[playerArray.length()];
                for (int i = 0; i < playerIds.length; i++) {
                    playerIds[i] = playerArray.getString(i);
                }
                String careerMatchId = loaded.optString("careerMatchId", null);

                MatchInfoRec mir = new MatchInfoRec(entireBeginTime, careerMatchId, gameValues,
                        loaded.getInt("totalFrames"), playerIds, true);
                
                JSONArray frameArray = loaded.getJSONArray("frames");
                for (int i = 0; i < frameArray.length(); i++) {
                    FrameInfoRec frameInfoRec = FrameInfoRec.fromJson(mir, frameArray.getJSONObject(i));
                    mir.frames.add(frameInfoRec);
                }

                System.out.println("Loaded from '" + entireBeginTime + "', finished frames: " + mir.frames.size());
                return mir;
            } catch (JSONException je) {
                EventLogger.error("JSON error when read " + file.getAbsolutePath());
                EventLogger.error(je);
                return defaultValue;
            }
        } else {
            EventLogger.error("Match file of '" + entireBeginTime + "' does not exist!");
            return defaultValue;
        }
    }
    
    @Nullable
    public static MatchInfoRec tryToLoad(String entireBeginTime) {
        return loadMatchRec(entireBeginTime, null);
    }

    JSONObject toJson() {
        JSONObject root = new JSONObject();
        root.put("entireBeginTime", entireBeginTime);
        root.put("careerMatchId", careerMatchId);
        root.put("gameValues", gameValues.toJson());
        root.put("totalFrames", totalFrames);
        JSONArray players = new JSONArray();
        for (String pid : playerIds) {
            players.put(pid);
        }
        root.put("players", players);

        JSONArray framesArray = new JSONArray();
        for (FrameInfoRec fir : frames) {
            framesArray.put(fir.toJson());
        }
        root.put("frames", framesArray);

        return root;
    }

    public void writeToDisk() {
        JSONObject json = toJson();
        DataLoader.saveToDisk(json, recFile.getAbsolutePath());
    }

    public void startNextFrame() {
        if (!valid) return;
        FrameInfoRec frame = createFrame();
        frames.add(frame);
    }

    protected FrameInfoRec createFrame() {
        return new FrameInfoRec(this);
    }

    public FrameInfoRec getCurrentFrame() {
        return frames.getLast();
    }

    public void finishCurrentFrame(boolean matchFinished, int frameWinnerNumber) {
        if (!valid) return;
        frames.getLast().winner = frameWinnerNumber;
        writeToDisk();
    }

    public void recordACue(int playerFrom1,
                           int target,
                           Ball specifiedTarget,
                           int firstHit,
                           PlayerHand.Hand hand,
                           Map<Integer, Integer> pots,
                           int[] gainScores,
                           int[] scoresAfter,
                           CueAttempt attempt,
                           FoulInfo foulInfo,
                           List<CueInfoRec.Special> specials) {
        if (!valid) return;
        FrameInfoRec fir = getCurrentFrame();
        CueInfoRec cir = new CueInfoRec();
        cir.player = playerFrom1;
        cir.target = target;
        if (specifiedTarget != null) {
            cir.specifiedTarget = specifiedTarget.getValue();
        }
        cir.firstHit = firstHit;
        cir.hand = hand;
        cir.pots = pots;
        cir.gainScores = gainScores;
        cir.scoresAfter = scoresAfter;
        // 注意：这时候还不能写入json，因为defense attempt的success会因为下一杆的结果而改变
        cir.attemptBase = attempt.getAttemptBase();
        cir.foulInfo = foulInfo;
        cir.specials = specials;
        
        if (attempt instanceof PotAttempt pa) {
            cir.potInfo = CueInfoRec.PotInfo.fromPotAttempt(pa);
        }

        fir.cueRecs.add(cir);
    }
    
    public FrameInfoRec getFrame(int frameIndexFrom0) {
        return frames.get(frameIndexFrom0);
    }
}
