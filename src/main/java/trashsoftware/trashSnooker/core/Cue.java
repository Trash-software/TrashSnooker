package trashsoftware.trashSnooker.core;

import javafx.scene.paint.Color;

public class Cue {

    public static final Cue STD_ASH = new Cue(
            1080.0, 5.0, 365.0, 7.0, 26.0, 10.0,
            Material.ASH, Color.SIENNA,Color.ORANGE, Color.BLACK.brighter().brighter()
    );
    public static final Cue STD_BIG = new Cue(
            700.0, 450.0, 300.0, 9.0, 26.0, 10.0,
            Material.ASH, Color.BEIGE, Color.GOLDENROD, Color.BLACK.brighter().brighter()
    );
    public static final Cue STD_BREAK_CUE = new Cue(
            1400.0, 7.0, 30.0, 14.0,
            Material.BREAK, Color.BLACK.brighter()
    );

    public final double frontLength;
    public final double midLength;
    public final double backLength;
    public final double cueTipThickness;
    private final double endWidth;
    private final double cueTipWidth;
    private final Material material;
    public final Color frontColor;
    public final Color midColor;
    public final Color backColor;

    public Cue(double frontLength,
               double midLength,
               double backLength,
               double cueTipThickness,
               double endWidth,
               double cueTipWidth,
               Material material,
               Color frontColor,
               Color midColor,
               Color backColor) {
        this.frontLength = frontLength;
        this.midLength = midLength;
        this.backLength = backLength;
        this.cueTipThickness = cueTipThickness;
        this.endWidth = endWidth;
        this.cueTipWidth = cueTipWidth;
        this.material = material;
        this.frontColor = frontColor;
        this.midColor = midColor;
        this.backColor = backColor;
    }

    public Cue(double length, double cueTipThickness, double endWidth, double cueTipWidth,
               Material material,
               Color color) {
        this(length, 0, 0, cueTipThickness,
                endWidth, cueTipWidth, material, color, color, color);
    }

    public Cue(double frontLength, double backLength,
               double cueTipThickness, double endWidth, double cueTipWidth,
               Material material,
               Color frontColor, Color backColor) {
        this(frontLength, 0.0, backLength, cueTipThickness,
                endWidth, cueTipWidth, material, frontColor, Color.WHITE, backColor);
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

    public Material getMaterial() {
        return material;
    }

    public enum Material {
        ASH(100.0, 100.0),
        MAPLE(110.0, 110.0),
        BREAK(120.0, 80.0);

        public final double powerPercentage;
        public final double spinPercentage;

        Material(double powerPercentage, double spinPercentage) {
            this.powerPercentage = powerPercentage;
            this.spinPercentage = spinPercentage;
        }
    }
}
