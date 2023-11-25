package trashsoftware.trashSnooker.core.cue;

import javafx.scene.paint.Color;

import java.util.Objects;

public abstract class CueBrand {
    public final String cueId;
    public final String name;

    public final double cueTipThickness;
    public final double tipRingThickness;

    protected final double endWidth;
    protected final double cueTipWidth;

    final double powerMultiplier;
    final double spinMultiplier;
    final double accuracyMultiplier;

    public Color tipRingColor;
    public Color backColor;

    public final boolean isRest;
    public final boolean privacy;
    public final boolean available;
    public final int price;

    public final Cue.Size tipSize;

    protected CueBrand(String cueId,
                       String name,
                       double tipRingThickness,
                       double cueTipThickness,
                       double endWidth,
                       double cueTipWidth,
                       Color tipRingColor,
                       Color backColor,
                       double powerMultiplier,
                       double spinMultiplier,
                       double accuracyMultiplier,
                       boolean privacy,
                       boolean availability,
                       int price) {
        this.cueId = cueId;
        this.name = name;

        this.tipRingThickness = tipRingThickness;
        this.cueTipThickness = cueTipThickness;
        this.endWidth = endWidth;
        this.cueTipWidth = cueTipWidth;
        this.tipRingColor = tipRingColor;

        this.backColor = backColor;
        this.powerMultiplier = powerMultiplier;
        this.spinMultiplier = spinMultiplier;
        this.accuracyMultiplier = accuracyMultiplier;
        this.privacy = privacy;
        this.available = availability;
        this.price = price;

        if (cueTipWidth <= 9.8) tipSize = Cue.Size.VERY_SMALL;
        else if (cueTipWidth < 11.0) tipSize = Cue.Size.SMALL;
        else if (cueTipWidth < 12.5) tipSize = Cue.Size.MEDIUM;
        else if (cueTipWidth < 14.5) tipSize = Cue.Size.BIG;
        else tipSize = Cue.Size.HUGE;

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

    public abstract double getWoodPartLength();

    @Override
    public String toString() {
        return "CueBrand-" + cueId;
    }
}
