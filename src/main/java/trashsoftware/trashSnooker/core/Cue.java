package trashsoftware.trashSnooker.core;

import javafx.scene.paint.Color;

public class Cue {

    public final String name;
    public final double frontLength;
    public final double midLength;
    public final double backLength;
    public final double cueTipThickness;
    public final double tipRingThickness;
    private final double endWidth;
    private final double cueTipWidth;

    public final double powerMultiplier;
    public final double spinMultiplier;
    public final double accuracyMultiplier;

    public final Color tipRingColor;
    public final Color frontColor;
    public final Color midColor;
    public final Color backColor;

    public final boolean privacy;

    public Cue(String name,
               double frontLength,
               double midLength,
               double backLength,
               double tipRingThickness,
               double cueTipThickness,
               double endWidth,
               double cueTipWidth,
               Color tipRingColor,
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
        this.tipRingThickness = tipRingThickness;
        this.cueTipThickness = cueTipThickness;
        this.endWidth = endWidth;
        this.cueTipWidth = cueTipWidth;
        this.tipRingColor = tipRingColor;
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
        return cueTipWidth + (endWidth - cueTipWidth) *
                ((tipRingThickness + frontLength) / getWoodPartLength());
    }

    public double getMidMaxWidth() {
        return cueTipWidth + (endWidth - cueTipWidth) *
                ((tipRingThickness + frontLength + midLength) / getWoodPartLength());
    }

    public double getRingMaxWidth() {
        return cueTipWidth + (endWidth - cueTipWidth) *
                (tipRingThickness / getWoodPartLength());
    }

    public double getTotalLength() {
        return getWoodPartLength() + cueTipThickness;
    }

    public double getWoodPartLength() {
        return tipRingThickness + frontLength + midLength + backLength;
    }

    public double getEndWidth() {
        return endWidth;
    }

    public double getCueTipWidth() {
        return cueTipWidth;
    }
}
