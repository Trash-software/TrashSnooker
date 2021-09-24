package trashsoftware.trashSnooker.core;

import javafx.scene.paint.Color;

import java.util.List;

public class Cue {

    public static final Cue STD_ASH = new Cue(
            "Ash cue",
            1080.0, 5.0, 365.0, 7.0, 26.0, 10.0,
            Color.SIENNA, Color.ORANGE.brighter(), Color.BLACK.brighter().brighter(),
            1.0, 1.0, 1.0
    );
    public static final Cue STD_BIG = new Cue(
            "Pool cue",
            700.0, 450.0, 300.0, 9.0, 26.0, 13.5,
            Color.BEIGE, Color.GOLDENROD, Color.BLACK.brighter().brighter(),
            1.1, 1.1, 0.8
    );
    public static final Cue STD_BREAK_CUE = new Cue(
            "Break cue",
            1400.0, 7.0, 30.0, 15.0,
            Color.BLACK.brighter(),
            1.2, 0.8, 0.6
    );

    // 定制球杆
    public static final Cue LOLITA_CUE = new Cue(
            "Lolita's private cue",
            1080, 5.0, 365.0, 7.0, 24.0, 9.5,
            Color.LIGHTPINK, Color.CRIMSON, Color.MEDIUMORCHID,
            0.95, 0.95, 1.2
    );

    public static final List<Cue> ALL_CUES = List.of(
            STD_ASH, STD_BIG, STD_BREAK_CUE,
            LOLITA_CUE
    );

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
               double accuracyMultiplier) {
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
    }

    public Cue(String name,
               double length, double cueTipThickness, double endWidth, double cueTipWidth,
               Color color,
               double powerMultiplier,
               double spinMultiplier,
               double accuracyMultiplier) {
        this(name, length, 0, 0, cueTipThickness,
                endWidth, cueTipWidth, color, color, color,
                powerMultiplier, spinMultiplier, accuracyMultiplier);
    }

    public Cue(String name, double frontLength, double backLength,
               double cueTipThickness, double endWidth, double cueTipWidth,
               Color frontColor, Color backColor,
               double powerMultiplier,
               double spinMultiplier,
               double accuracyMultiplier) {
        this(name, frontLength, 0.0, backLength, cueTipThickness,
                endWidth, cueTipWidth, frontColor, Color.WHITE, backColor,
                powerMultiplier, spinMultiplier, accuracyMultiplier);
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
