package trashsoftware.trashSnooker.core.cue;

import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.core.metrics.BallMetrics;

import java.util.Objects;

public abstract class CueBrand {
    public final String cueId;
    public final String name;
    
    public final double tipRingThickness;

    protected final double endWidth;
    protected final double cueTipWidth;

    final double powerMultiplier;
//    final double spinMultiplier;
    final double accuracyMultiplier;

    /**
     * 材质的弹性，直接影响杆法效果
     */
    final double elasticity;

    /**
     * 前段的硬度，影响发力准确度
     */
    final double hardness;

    public Color tipRingColor;
    public Color backColor;

    public final boolean isRest;
    public final boolean privacy;
    public final boolean available;
    public final int price;

    public final CueSize tipSize;
    public final Material material;

    protected CueBrand(String cueId,
                       String name,
                       double tipRingThickness,
                       double endWidth,
                       double cueTipWidth,
                       Color tipRingColor,
                       Color backColor,
                       Material material,
                       double powerMultiplier,
                       double elasticity,
                       double hardness,
                       double accuracyMultiplier,
                       boolean privacy,
                       boolean availability,
                       int price) {
        this.cueId = cueId;
        this.name = name;

        this.tipRingThickness = tipRingThickness;
        this.endWidth = endWidth;
        this.cueTipWidth = cueTipWidth;
        this.tipRingColor = tipRingColor;
        this.material = material;

        this.backColor = backColor;
        this.powerMultiplier = powerMultiplier;
//        this.spinMultiplier = spinMultiplier;
        this.elasticity = elasticity;
        this.hardness = hardness;
        this.accuracyMultiplier = accuracyMultiplier;
        this.privacy = privacy;
        this.available = availability;
        this.price = price;

        if (cueTipWidth <= 9.8) tipSize = CueSize.VERY_SMALL;
        else if (cueTipWidth < 11.0) tipSize = CueSize.SMALL;
        else if (cueTipWidth < 12.5) tipSize = CueSize.MEDIUM;
        else if (cueTipWidth < 14.5) tipSize = CueSize.BIG;
        else tipSize = CueSize.HUGE;

        isRest = "restCue".equals(cueId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CueBrand cue = (CueBrand) o;
        return Objects.equals(cueId, cue.cueId);
    }

    public boolean isAvailable() {
        return available;
    }

    public int getPrice() {
        return price;
    }

    public double getElasticity() {
        return elasticity;
    }
    
    public double getHardness() {
        return powerMultiplier;
    }

    public boolean isBreakCue() {
        return cueId.toLowerCase().endsWith("breakcue");
    }

    @Override
    public int hashCode() {
        return Objects.hash(cueId);
    }

    public String getCueId() {
        return cueId;
    }

    public String getName() {
        return name;
    }

    public double getCueTipWidth() {
        return cueTipWidth;
    }

    /**
     * @see Cue#getCueAbleRelRadius(BallMetrics) 
     */
    public double theoreticalSpinStrength() {
        double maxGripAngle = Math.pow(cueTipWidth / 2 / 57.00, 0.75)
                * 1 * 170.0;
//        System.out.println(tip.getBrand().id() + " Grip angle " + maxGripAngle);
        return Math.sin(Math.toRadians(maxGripAngle)) * getElasticity() * 2.0;
    }

    public abstract double getWoodPartLength();

    @Override
    public String toString() {
        return "CueBrand-" + cueId;
    }
    
    public enum Material {
        HARD_WOOD,
        SOFT_WOOD,
        CARBON
    }
}
