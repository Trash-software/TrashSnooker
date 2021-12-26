package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.core.ai.AiPlayStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlayerPerson {

    private final String name;
    public final String category;
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
    private double[] cuePointMuSigmaXY;  // 横向的mu，横向的sigma，纵向的mu，纵向的sigma
    private final double cueSwingMag;
    private final CuePlayType cuePlayType;
    private final AiPlayStyle aiPlayStyle;
    public final double psy;

    public PlayerPerson(String name,
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

    public PlayerPerson(String name,
                        double maxPowerPercentage,
                        double controllablePowerPercentage,
                        double maxSpinPercentage,
                        double precisionPercentage,                        
                        double anglePrecision,
                        double longPrecision,
                        AiPlayStyle aiPlayStyle) {
        this(
                name,
                "Amateur",
                maxPowerPercentage,
                controllablePowerPercentage,
                maxSpinPercentage,
                precisionPercentage,
                anglePrecision,
                longPrecision,
                50.0,
                200.0,
                100.0,
                new double[]{0, 0, 0, 0},
                100.0,
                100.0,
                CuePlayType.DEFAULT_PERFECT,
                aiPlayStyle
        );
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
