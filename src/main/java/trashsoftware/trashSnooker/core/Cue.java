package trashsoftware.trashSnooker.core;

public class Cue {

    public static final Cue STD_ASH = new Cue(
        1450.0, 30.0, 9.0, Material.ASH
    );
    public static final Cue STD_BREAK_CUE = new Cue(
            1450.0, 30.0, 15.0, Material.BREAK
    );

    private final double length;
    private final double endWidth;
    private final double cueTipWidth;
    private final Material material;

    public Cue(double length, double endWidth, double cueTipWidth, Material material) {
        this.length = length;
        this.endWidth = endWidth;
        this.cueTipWidth = cueTipWidth;
        this.material = material;
    }

    public double getLength() {
        return length;
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
