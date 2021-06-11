package trashsoftware.trashSnooker.core;

public class PlayerPerson {

    private final String name;
    private final double maxPowerPercentage;
    private final double maxSpinPercentage;
    private final double precisionPercentage;

    public PlayerPerson(String name, double maxPowerPercentage, double maxSpinPercentage, double precisionPercentage) {
        this.name = name;
        this.maxPowerPercentage = maxPowerPercentage;
        this.maxSpinPercentage = maxSpinPercentage;
        this.precisionPercentage = precisionPercentage;
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
