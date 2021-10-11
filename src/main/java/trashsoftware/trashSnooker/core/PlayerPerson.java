package trashsoftware.trashSnooker.core;

import java.util.ArrayList;
import java.util.List;

public class PlayerPerson {

    private final String name;
    private final double maxPowerPercentage;
    private final double maxSpinPercentage;
    private final double precisionPercentage;
    private final List<Cue> privateCues = new ArrayList<>();
    private double minPullDt;
    private double maxPullDt;
    private final double cuePrecision;
    private final CuePlayType cuePlayType;

    public PlayerPerson(String name,
                        double maxPowerPercentage,
                        double maxSpinPercentage,
                        double precisionPercentage,
                        double minPullDt,
                        double maxPullDt,
                        double cuePrecision,
                        CuePlayType cuePlayType) {
        this.name = name;
        this.maxPowerPercentage = maxPowerPercentage;
        this.maxSpinPercentage = maxSpinPercentage;
        this.precisionPercentage = precisionPercentage;
        this.minPullDt = minPullDt;
        this.maxPullDt = maxPullDt;
        this.cuePrecision = cuePrecision;
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
                CuePlayType.DEFAULT_PERFECT
        );
    }

    public CuePlayType getCuePlayType() {
        return cuePlayType;
    }

    public double getCuePrecision() {
        return cuePrecision;
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
}
