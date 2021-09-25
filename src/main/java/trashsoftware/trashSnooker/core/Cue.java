package trashsoftware.trashSnooker.core;

import javafx.scene.paint.Color;

public class Cue {

    public final String name;
    public final double frontLength;
    public final double midLength;
    public final double backLength;
    public final double cueTipThickness;
    private final double endWidth;
    private final double cueTipWidth;

    public final double powerMultiplier;
    public final double spinMultiplier;
    public final double accuracyMultiplier;

    public final Color frontColor;
    public final Color midColor;
    public final Color backColor;

    public final boolean privacy;

    public Cue(String name,
               double frontLength,
               double midLength,
               double backLength,
               double cueTipThickness,
               double endWidth,
               double cueTipWidth,
               Color frontColor,
               Color midColor,
               Color backColor,
               double powerMultiplier,
               double spinMultiplier,
               double accuracyMultiplier,
               boolean privacy) {
        this.name = name;
        this.frontLength = frontLength;
        this.midLength = midLength;
        this.backLength = backLength;
        this.cueTipThickness = cueTipThickness;
        this.endWidth = endWidth;
        this.cueTipWidth = cueTipWidth;
        this.frontColor = frontColor;
        this.midColor = midColor;
        this.backColor = backColor;
        this.powerMultiplier = powerMultiplier;
        this.spinMultiplier = spinMultiplier;
        this.accuracyMultiplier = accuracyMultiplier;
        this.privacy = privacy;
    }

    public String getName() {
        return name;
    }

    public double getFrontMaxWidth() {
        return cueTipWidth + (endWidth - cueTipWidth) * (frontLength / getWoodPartLength());
    }

    public double getMidMaxWidth() {
        return cueTipWidth + (endWidth - cueTipWidth) *
                ((frontLength + midLength) / getWoodPartLength());
    }

    public double getTotalLength() {
        return getWoodPartLength() + cueTipThickness;
    }

    public double getWoodPartLength() {
        return frontLength + midLength + backLength;
    }

    public double getEndWidth() {
        return endWidth;
    }

    public double getCueTipWidth() {
        return cueTipWidth;
    }
}
