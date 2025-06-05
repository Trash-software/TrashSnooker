package trashsoftware.trashSnooker.core.infoRec;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.FoulInfo;
import trashsoftware.trashSnooker.core.person.PlayerHand;
import trashsoftware.trashSnooker.core.attempt.AttemptBase;
import trashsoftware.trashSnooker.core.attempt.PotAttempt;
import trashsoftware.trashSnooker.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CueInfoRec {
    
    int player;
    int target;
    int specifiedTarget;
    int firstHit;
    PlayerHand.CueHand hand;
    Map<Integer, Integer> pots;  // 这一杆打进的球
    int[] gainScores;
    int[] scoresAfter;
    AttemptBase attemptBase;
    @Nullable PotInfo potInfo;
    @Nullable FoulInfo foulInfo;
    @Nullable List<Special> specials;
    
    public static CueInfoRec fromJson(JSONObject json) {
        CueInfoRec cir = new CueInfoRec();
        cir.player = json.getInt("player");
        cir.target = json.getInt("target");
        if (json.has("specifiedTarget")) {
            cir.specifiedTarget = json.getInt("specifiedTarget");
        }
        cir.gainScores = Util.jsonToIntArray(json.getJSONArray("gainScores"));
        cir.scoresAfter = Util.jsonToIntArray(json.getJSONArray("scoresAfter"));
        if (json.has("cueHand")) {
            cir.hand = PlayerHand.CueHand.fromJson(json.getJSONObject("cueHand"));
        } else {
            cir.hand = PlayerHand.CueHand.makeDefault(
                    PlayerHand.Hand.valueOf(json.getString("hand")),
                    null);
        }
        
        cir.attemptBase = AttemptBase.fromJson(json.getJSONObject("attemptBase"));
        cir.pots = new TreeMap<>();
        if (json.has("pots")) {
            JSONObject potsObj = json.getJSONObject("pots");
            for (String key : potsObj.keySet()) {
                cir.pots.put(Integer.parseInt(key), potsObj.getInt(key));
            }
        }
        if (json.has("foulInfo")) {
            cir.foulInfo = FoulInfo.fromJson(json.getJSONObject("foulInfo"));
        } else {
            cir.foulInfo = new FoulInfo();
        }
        if (json.has("specials")) {
            JSONArray speArr = json.getJSONArray("specials");
            cir.specials = new ArrayList<>();
            for (int i = 0; i < speArr.length(); i++) {
                cir.specials.add(Special.valueOf(speArr.getString(i)));
            }
        }
        if (json.has("firstHit")) {
            cir.firstHit = json.getInt("firstHit");
        } else {
            // 只是为了兼容，不准确也无所谓了
            if (cir.foulInfo.isMiss()) cir.firstHit = 0;
            else cir.firstHit = cir.target;
        }
        JSONObject potInfo = json.optJSONObject("potInfo", null);
        if (potInfo != null) {
            cir.potInfo = PotInfo.fromJson(potInfo);
        }
        
        return cir;
    }
    
    public JSONObject toJson() {
        JSONObject out = new JSONObject();
        out.put("player", player);
        out.put("target", target);
        if (specifiedTarget != 0) {
            out.put("specifiedTarget", specifiedTarget);
        }
        out.put("firstHit", firstHit);
        out.put("cueHand", hand.toJson());
        out.put("gainScores", Util.arrayToJson(gainScores));
        out.put("scoresAfter", Util.arrayToJson(scoresAfter));
        out.put("attemptBase", attemptBase.toJson());
        if (pots != null && !pots.isEmpty()) {
            JSONObject potsObj = Util.mapToJson(pots);
            out.put("pots", potsObj);
        }
        if (foulInfo != null && (foulInfo.isFoul() || foulInfo.isIllegal())) {
            out.put("foulInfo", foulInfo.toJson());
        }
        if (specials != null && !specials.isEmpty()) {
            JSONArray specialArr = new JSONArray();
            for (Special special : specials) {
                specialArr.put(special.name());
            }
            out.put("specials", specialArr);
        }
        if (potInfo != null) {
            out.put("potInfo", potInfo.toJson());
        }
        
        return out;
    }

    public int getTarget() {
        return target;
    }

    public FoulInfo getFoulInfo() {
        return foulInfo;
    }
    
    public boolean isFoul() {
        return foulInfo != null && foulInfo.isFoul();
    }
    
    public boolean legallyPot() {
        int index = player - 1;
        return !isFoul() && gainScores[index] > 0 && pots != null && !pots.isEmpty();
    }

    public AttemptBase getAttemptBase() {
        return attemptBase;
    }

    public int getPlayer() {
        return player;
    }

    public int[] getGainScores() {
        return gainScores;
    }

    public int[] getScoresAfter() {
        return scoresAfter;
    }

    public @Nullable List<Special> getSpecials() {
        return specials;
    }

    public Map<Integer, Integer> getPots() {
        return pots;
    }
    
    public boolean isSnookerFreeBall() {
        return specials != null && specials.contains(Special.SNOOKER_FREE_BALL);
    }

    public enum Special {
        SNOOKER_FREE_BALL,
        REPOSITION,
        LET_OTHER_PLAY,
        BALL_IN_HAND,
        AMERICAN_PUSH_OUT
    }
    
    public record PotInfo(
            double whiteTarDt,
            double tarPocketDt,
            double angle,
            double estPotProb,
            boolean isDouble
    ) {
        
        static PotInfo fromPotAttempt(PotAttempt potAttempt) {
            double[] targetBallOrigPos = potAttempt.getTargetBallOrigPos();
            double[] cueBallOrigPos =  potAttempt.getCueBallOrigPos();
            double whiteTargetDt = Math.hypot(
                    targetBallOrigPos[0] - cueBallOrigPos[0],
                    targetBallOrigPos[1] - cueBallOrigPos[1]
            );
            
            double targetHoleDt;
            boolean isDouble = potAttempt.isDoubleShot();
            if (isDouble) {
                targetHoleDt = -1;
            } else {
                double[][] targetDirHole = potAttempt.getTargetDirHole();
                targetHoleDt = Math.hypot(
                        targetDirHole[1][0] - targetBallOrigPos[0],
                        targetDirHole[1][1] - targetBallOrigPos[1]
                );
            }
            System.out.println("Pot prob is " + potAttempt.attackChoice.getDefaultRef().getPotProb());
            return new PotInfo(
                    whiteTargetDt,
                    targetHoleDt,
                    potAttempt.attackChoice.getAngleRad(),
                    potAttempt.attackChoice.getDefaultRef().getPotProb(),
                    isDouble
            );

        }
        
        static PotInfo fromJson(JSONObject json) {
            return Util.jsonToRecord(PotInfo.class, json);
        }
        
        JSONObject toJson() {
            return Util.recordToJson(this);
        }
    }
}
