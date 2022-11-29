package trashsoftware.trashSnooker.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.ai.AiPlayStyle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PlayerPerson {

    private final String playerId;
    private final String name;
    public final String category;
    private boolean isCustom;
    private final double maxPowerPercentage;
    private final double controllablePowerPercentage;
    private final double maxSpinPercentage;
    private final double precisionPercentage;
    private final double anglePrecision;
    private final double longPrecision;
    private final List<Cue> privateCues = new ArrayList<>();
    private final double solving;
    private final double minPullDt;
    private final double maxPullDt;
    private final double aimingOffset;  // 瞄和打的偏差，正值向右偏
    private final double powerControl;
    
    // 横向的mu，横向的sigma，纵向的mu，纵向的sigma
    // 横向的左塞负右塞正，纵向的高杆正低杆负
    private final double[] cuePointMuSigmaXY;
    private final double cueSwingMag;
    private final CuePlayType cuePlayType;
    private final AiPlayStyle aiPlayStyle;
    public final HandBody handBody;
    public final double psy;

    public PlayerPerson(String playerId,
                        String name,
                        String category,
                        double maxPowerPercentage,
                        double controllablePowerPercentage,
                        double maxSpinPercentage,
                        double precisionPercentage,
                        double anglePrecision,
                        double longPrecision,
                        double solving,
                        double minPullDt,
                        double maxPullDt,
                        double aimingOffset,
                        double cueSwingMag,
                        double[] cuePointMuSigmaXY,
                        double powerControl,
                        double psy,
                        CuePlayType cuePlayType,
                        AiPlayStyle aiPlayStyle,
                        HandBody handBody) {
        this.playerId = playerId;
        this.name = name;
        this.category = category;
        this.maxPowerPercentage = maxPowerPercentage;
        this.controllablePowerPercentage = controllablePowerPercentage;
        this.maxSpinPercentage = maxSpinPercentage;
        this.precisionPercentage = precisionPercentage;
        this.anglePrecision = anglePrecision;
        this.longPrecision = longPrecision;
        this.solving = solving;
        this.minPullDt = minPullDt;
        this.maxPullDt = maxPullDt;
        this.aimingOffset = aimingOffset;
        this.cueSwingMag = cueSwingMag;
        this.cuePointMuSigmaXY = cuePointMuSigmaXY;
        this.powerControl = powerControl;
        this.psy = psy;
        this.cuePlayType = cuePlayType;
        this.aiPlayStyle = aiPlayStyle;
        this.handBody = handBody;
    }

    public PlayerPerson(String playerId,
                        String name,
                        double maxPowerPercentage,
                        double controllablePowerPercentage,
                        double maxSpinPercentage,
                        double precisionPercentage,                        
                        double anglePrecision,
                        double longPrecision,
                        double control,
                        AiPlayStyle aiPlayStyle,
                        boolean isCustom) {
        this(
                playerId,
                name,
                estimateCategory(precisionPercentage, control),
                maxPowerPercentage,
                controllablePowerPercentage,
                maxSpinPercentage,
                precisionPercentage,
                anglePrecision,
                longPrecision,
                (precisionPercentage + control) / 2,
                50.0,
                200.0,
                0.0,
                100.0,
                estimateCuePoint(precisionPercentage, control),
                control,
                90,
                CuePlayType.DEFAULT_PERFECT,
                aiPlayStyle,
                HandBody.DEFAULT
        );
        
        setCustom(isCustom);
    }
    
    private static String estimateCategory(double precisionPercentage, 
                                           double control) {
        double avg = (precisionPercentage + control) / 2;
        return avg > 80.0 ? "Professional" : 
                (avg > 50.0 ? "Amateur" : " Noob");
    }
    
    private static double[] estimateCuePoint(double precisionPercentage, 
                                               double control) {
        return new double[]{0, 
                (100 - (precisionPercentage + control) / 2) / 4.0, 
                0, 
                (100 - control) / 4.0};
    }
    
    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        obj.put("spin", getMaxSpinPercentage());
        obj.put("precision", getPrecisionPercentage());
        obj.put("anglePrecision", getAnglePrecision());
        obj.put("longPrecision", getLongPrecision());
        obj.put("name", getName());
        
        obj.put("maxPower", getMaxPowerPercentage());
        obj.put("controllablePower", getControllablePowerPercentage());
        obj.put("solving", getSolving());

        JSONArray cueAction = new JSONArray(List.of(getMinPullDt(), getMaxPullDt()));
        obj.put("pullDt", cueAction);
        obj.put("cuePlayType", getCuePlayType().toString());
        obj.put("aimingOffset", getAimingOffset());
        obj.put("cueSwingMag", getCueSwingMag());
        
        JSONArray cuePoint = new JSONArray(cuePointMuSigmaXY);
        obj.put("cuePointMuSigma", cuePoint);
        obj.put("powerControl", getPowerControl());
        
        JSONArray privateCues = new JSONArray();
        for (Cue cue : getPrivateCues()) {
            privateCues.put(cue.getName());
        }
        
        obj.put("privateCues", privateCues);
        obj.put("category", category);
        obj.put("psy", psy);
        
        obj.put("ai", getAiPlayStyle().toJsonObject());
        
        JSONObject handObj = new JSONObject();
        handObj.put("left", handBody.leftHand);
        handObj.put("right", handBody.rightHand);
        handObj.put("rest", handBody.restPot);
        
        obj.put("height", handBody.height);
        obj.put("width", handBody.bodyWidth);
        obj.put("hand", handObj);
        
        return obj;
    }

    public double getSolving() {
        return solving;
    }

    public boolean isCustom() {
        return isCustom;
    }

    public void setCustom(boolean custom) {
        isCustom = custom;
    }

    public AiPlayStyle getAiPlayStyle() {
        return aiPlayStyle;
    }

    public double[] getCuePointMuSigmaXY() {
        return cuePointMuSigmaXY;
    }

    public CuePlayType getCuePlayType() {
        return cuePlayType;
    }

    public double getAimingOffset() {
        return aimingOffset;
    }

    public double getCueSwingMag() {
        return cueSwingMag;
    }

    public double getMaxPullDt() {
        return maxPullDt;
    }

    public double getMinPullDt() {
        return minPullDt;
    }

    public void addPrivateCue(Cue privateCue) {
        privateCues.add(privateCue);
    }

    public List<Cue> getPrivateCues() {
        return privateCues;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerPerson that = (PlayerPerson) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public String getName() {
        return name;
    }

    public String getPlayerId() {
        return playerId;
    }

    public double getMaxPowerPercentage() {
        return maxPowerPercentage;
    }

    public double getControllablePowerPercentage() {
        return controllablePowerPercentage;
    }

    public double getMaxSpinPercentage() {
        return maxSpinPercentage;
    }

    public double getPrecisionPercentage() {
        return precisionPercentage;
    }

    public double getAnglePrecision() {
        return anglePrecision;
    }

    public double getLongPrecision() {
        return longPrecision;
    }

    public double getPowerControl() {
        return powerControl;
    }
    
    public enum Hand {
        LEFT(1.0),
        RIGHT(1.0),
        REST(0.7);
        
        public final double powerMul;
        
        Hand(double powerMul) {
            this.powerMul = powerMul;
        }
    }
    
    public static class HandSkill implements Comparable<HandSkill> {
        public final Hand hand;
        public final double skill;
        
        HandSkill(Hand hand, double skill) {
            this.hand = hand;
            this.skill = skill;
        }

        @Override
        public int compareTo(@NotNull PlayerPerson.HandSkill o) {
            return -Double.compare(this.skill, o.skill);
        }

        @Override
        public String toString() {
            return "HandSkill{" +
                    "hand=" + hand +
                    ", skill=" + skill +
                    '}';
        }
    }
    
    public static class HandBody {
        public static final HandBody DEFAULT = 
                new HandBody(180.0, 1.0, 50.0, 100.0, 80.0);
        
        public final double height;
        public final double bodyWidth;
        public final double leftHand;
        public final double rightHand;
        public final double restPot;
        private final boolean leftHandRest;    // 是否用左手拿架杆

        private final HandSkill[] precedence;
        
        public HandBody(double height, double bodyWidth,
                        double leftHand, double rightHand, double restPot) {
            this.height = height;
            this.bodyWidth = bodyWidth;
            this.leftHand = leftHand;
            this.rightHand = rightHand;
            this.restPot = restPot;
            
            precedence = new HandSkill[]{
                    new HandSkill(Hand.LEFT, leftHand),
                    new HandSkill(Hand.RIGHT, rightHand),
                    new HandSkill(Hand.REST, restPot)
            };
            Arrays.sort(precedence);
            this.leftHandRest = precedence[0].hand == Hand.RIGHT;
        }
        
        public HandSkill getPrimary() {
            return precedence[0];
        }
        
        public HandSkill getSecondary() {
            return precedence[1];
        }
        
        public HandSkill getThird() {
            return precedence[2];
        }
        
        public static double getPowerMulOfHand(@Nullable HandSkill handSkill) {
            return handSkill == null ? 1.0 : (handSkill.skill * handSkill.hand.powerMul / 100);
        }
        
        public static double getPrecisionOfHand(@Nullable HandSkill handSkill) {
            return handSkill == null ? 1.0 : (handSkill.skill / 100);
        }

        public static double getSdOfHand(@Nullable HandSkill handSkill) {
            return handSkill == null ? 1.0 : (100.0 / handSkill.skill);
        }
        
        public double getRestPot() {
            return restPot;
        }

        public boolean isLeftHandRest() {
            return leftHandRest;
        }
        
        public HandSkill getHandSkillByHand(Hand hand) {
            for (HandSkill handSkill : precedence) {
                if (handSkill.hand == hand) return handSkill;
            }
            throw new RuntimeException("No such hand");
        }
    }
}
