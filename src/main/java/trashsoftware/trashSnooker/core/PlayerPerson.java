package trashsoftware.trashSnooker.core;

import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.ai.AiPlayStyle;

import java.util.ArrayList;
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
    private final double minPullDt;
    private final double maxPullDt;
    private final double powerControl;
    private final double[] cuePointMuSigmaXY;  // 横向的mu，横向的sigma，纵向的mu，纵向的sigma
    private final double cueSwingMag;
    private final CuePlayType cuePlayType;
    private final AiPlayStyle aiPlayStyle;
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
                        double minPullDt,
                        double maxPullDt,
                        double cueSwingMag,
                        double[] cuePointMuSigmaXY,
                        double powerControl,
                        double psy,
                        CuePlayType cuePlayType,
                        AiPlayStyle aiPlayStyle) {
        this.playerId = playerId;
        this.name = name;
        this.category = category;
        this.maxPowerPercentage = maxPowerPercentage;
        this.controllablePowerPercentage = controllablePowerPercentage;
        this.maxSpinPercentage = maxSpinPercentage;
        this.precisionPercentage = precisionPercentage;
        this.anglePrecision = anglePrecision;
        this.longPrecision = longPrecision;
        this.minPullDt = minPullDt;
        this.maxPullDt = maxPullDt;
        this.cueSwingMag = cueSwingMag;
        this.cuePointMuSigmaXY = cuePointMuSigmaXY;
        this.powerControl = powerControl;
        this.psy = psy;
        this.cuePlayType = cuePlayType;
        this.aiPlayStyle = aiPlayStyle;
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
                50.0,
                200.0,
                100.0,
                estimateCuePoint(precisionPercentage, control),
                control,
                90,
                CuePlayType.DEFAULT_PERFECT,
                aiPlayStyle
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

        JSONArray cueAction = new JSONArray(List.of(getMinPullDt(), getMaxPullDt()));
        obj.put("pullDt", cueAction);
        obj.put("cuePlayType", getCuePlayType().toString());
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
        
        return obj;
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
}
