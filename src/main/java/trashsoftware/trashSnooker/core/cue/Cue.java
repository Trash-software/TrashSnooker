package trashsoftware.trashSnooker.core.cue;

import javafx.scene.paint.Color;
import org.json.JSONObject;
import trashsoftware.trashSnooker.util.DataLoader;

import java.util.Objects;

public abstract class Cue {

    public final String cueId;
    public final String name;

    public final double cueTipThickness;
    public final double tipRingThickness;
    
    protected final double endWidth;
    protected final double cueTipWidth;

    public final double powerMultiplier;
    public final double spinMultiplier;
    public final double accuracyMultiplier;

    public final Color tipRingColor;
    public final Color backColor;

    public final boolean privacy;
    
    public final Size tipSize;

    protected Cue(String cueId,
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
               boolean privacy) {
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
        
        if (cueTipWidth <= 9.8) tipSize = Size.VERY_SMALL;
        else if (cueTipWidth < 11.0) tipSize = Size.SMALL;
        else if (cueTipWidth < 12.5) tipSize = Size.MEDIUM;
        else if (cueTipWidth < 14.5) tipSize = Size.BIG;
        else tipSize = Size.HUGE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cue cue = (Cue) o;
        return Objects.equals(cueId, cue.cueId);
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

    public abstract double getTotalLength();

    public abstract double getWoodPartLength();

    public double getEndWidth() {
        return endWidth;
    }

    public double getCueTipWidth() {
        return cueTipWidth;
    }
    
    public enum Size {
        VERY_SMALL,
        SMALL,
        MEDIUM,
        BIG,
        HUGE
    }
}
