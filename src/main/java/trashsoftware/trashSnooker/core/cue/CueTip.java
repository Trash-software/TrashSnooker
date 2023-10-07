package trashsoftware.trashSnooker.core.cue;

import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.util.Util;

public class CueTip {
    
    private final String name;
    private final double origThickness;
    private double totalDurability;
    private double durability;
    
    public CueTip(String name, double origThickness) {
        this.name = name;
        this.origThickness = origThickness;
    }

    public double getDurability() {
        return durability;
    }

    public double getTotalDurability() {
        return totalDurability;
    }
    
    public double getThickness() {
        return Algebra.shiftRangeSafe(
                0, totalDurability,
                origThickness * 0.3, origThickness,
                durability
        );
    }
}
