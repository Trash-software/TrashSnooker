package trashsoftware.trashSnooker.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlayerPerson {

    private final String name;
    private final double maxPowerPercentage;
    private final double maxSpinPercentage;
    private final double precisionPercentage;
    private final List<Cue> privateCues = new ArrayList<>();
    private final double minPullDt;
    private final double maxPullDt;
    private double powerControl;
    private double[] cuePointMuSigmaXY;  // 横向的mu，横向的sigma，纵向的mu，纵向的sigma
    private final double cueSwingMag;
    private final CuePlayType cuePlayType;

    public PlayerPerson(String name,
                        double maxPowerPercentage,
                        double maxSpinPercentage,
                        double precisionPercentage,
                        double minPullDt,
                        double maxPullDt,
                        double cueSwingMag,
                        double[] cuePointMuSigmaXY,
                        double powerControl,
                        CuePlayType cuePlayType) {
        this.name = name;
        this.maxPowerPercentage = maxPowerPercentage;
        this.maxSpinPercentage = maxSpinPercentage;
        this.precisionPercentage = precisionPercentage;
        this.minPullDt = minPullDt;
        this.maxPullDt = maxPullDt;
        this.cueSwingMag = cueSwingMag;
        this.cuePointMuSigmaXY = cuePointMuSigmaXY;
        this.powerControl = powerControl;
        this.cuePlayType = cuePlayType;
    }

    public PlayerPerson(String name,
                        double maxPowerPercentage,
                        double maxSpinPercentage,
                        double precisionPercentage) {
        this(
                name,
                maxPowerPercentage,
                maxSpinPercentage,
                precisionPercentage,
                50.0,
                200.0,
                100.0,
                new double[]{0, 0, 0, 0},
                100.0,
                CuePlayType.DEFAULT_PERFECT
        );
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

    public double getMaxSpinPercentage() {
        return maxSpinPercentage;
    }

    public double getPrecisionPercentage() {
        return precisionPercentage;
    }

    public double getPowerControl() {
        return powerControl;
    }
}
