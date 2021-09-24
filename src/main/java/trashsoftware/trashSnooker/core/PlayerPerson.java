package trashsoftware.trashSnooker.core;

import java.util.ArrayList;
import java.util.List;

public class PlayerPerson {

    private final String name;
    private final double maxPowerPercentage;
    private final double maxSpinPercentage;
    private final double precisionPercentage;
    private List<Cue> privateCues = new ArrayList<>();

    public PlayerPerson(String name,
                        double maxPowerPercentage,
                        double maxSpinPercentage,
                        double precisionPercentage) {
        this.name = name;
        this.maxPowerPercentage = maxPowerPercentage;
        this.maxSpinPercentage = maxSpinPercentage;
        this.precisionPercentage = precisionPercentage;
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
