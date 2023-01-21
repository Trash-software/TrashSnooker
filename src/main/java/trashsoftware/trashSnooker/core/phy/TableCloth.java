package trashsoftware.trashSnooker.core.phy;

public class TableCloth {

    private static final double baseSpeedReducer = 90;
    private static final double baseSpinReducer = 4300;
    private static final double baseSpinEffect = 1100;
    public final Goodness goodness;
    public final Smoothness smoothness;
    
    public TableCloth(Goodness goodness, Smoothness smoothness) {
        this.goodness = goodness;
        this.smoothness = smoothness;
    }

    @Override
    public String toString() {
        return "TableCloth{" +
                "goodness=" + goodness +
                ", smoothness=" + smoothness +
                '}';
    }
    public enum Goodness {
        EXCELLENT("极佳", 0.0, 0.0),
        GOOD("优质", 0.08, 0.2),
        NORMAL("一般", 0.2, 0.1),
        BAD("劣质", 0.45, 0.1),
        TERRIBLE("垃圾", 1, 0.0);

        public final double errorFactor;
        public final double fixedErrorFactor;
        private final String shown;

        Goodness(String shown, double errorFactor, double fixedErrorFactor) {
            this.shown = shown;
            this.errorFactor = errorFactor;
            this.fixedErrorFactor = fixedErrorFactor;
        }

        @Override
        public String toString() {
            return shown;
        }
    }

    public enum Smoothness {
        FAST("全新",
                1.0,
                1.0,
                1.0,
                0.75,
                1.0),
        NORMAL("专业",
                1.12,
                1.12,
                1.12,
                0.65,
                0.98),
        MEDIUM("普通",
                1.35,
                1.3,
                1.25,
                0.4,
                0.9),
        SLOW("旧",
                1.67,
                1.5,
                1.35,
                0.1,
                0.8);

        public final double speedReduceFactor;
        public final double spinReduceFactor;  // 数值越大，旋转衰减越大
        public final double spinEffectFactor;  // 数值越小影响越大
        public final double tailSpeedFactor;  // 尾速相关
        public final double cushionBounceFactor;
        private final String shown;

        Smoothness(String shown,
                   double speedReduceFactor,
                   double spinReduceFactor,
                   double spinEffectFactor,
                   double tailSpeedFactor,
                   double cushionBounceFactor) {
            this.shown = shown;
            this.speedReduceFactor = baseSpeedReducer * speedReduceFactor;
            this.spinReduceFactor = baseSpinReducer * spinReduceFactor;
            this.spinEffectFactor = baseSpinEffect / spinEffectFactor;
            this.tailSpeedFactor = tailSpeedFactor;
            this.cushionBounceFactor = cushionBounceFactor;
        }

        @Override
        public String toString() {
            return shown;
        }
    }
}