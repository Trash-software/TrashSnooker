package trashsoftware.trashSnooker.core.cue;

import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.util.Util;

public class CueTip {
    
    private final String name;
    private final double origThickness;
    private final double radius;
    private double totalDurability = 1000;
    private double durability = 1000;
    
    public CueTip(String name, double radius, double origThickness) {
        this.name = name;
        this.radius = radius;
        this.origThickness = origThickness;
    }

    public double getDurability() {
        return durability;
    }

    public double getTotalDurability() {
        return totalDurability;
    }

    public double getRadius() {
        return radius;
    }

    public double getThickness() {
        return Algebra.shiftRangeSafe(
                0, totalDurability,
                origThickness * 0.3, origThickness,
                durability
        );
    }
}
