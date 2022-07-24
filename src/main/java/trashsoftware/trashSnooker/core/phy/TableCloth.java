package trashsoftware.trashSnooker.core.phy;

public class TableCloth {
    
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
        GOOD("优质", 0.05, 0.2),
        NORMAL("一般", 0.25, 0.1),
        BAD("劣质", 0.5, 0.1),
        TERRIBLE("垃圾", 1, 0.0);

        private final String shown;
        public final double errorFactor;
        public final double fixedErrorFactor;

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
                100, 
                6700, 
                1500,
                0.65),
        NORMAL("专业",
                120,
                7200,
                1400,
                0.5),
        MEDIUM("普通", 
                150, 
                7800,
                1200,
                0.25),
        SLOW("旧", 
                180, 
                8400,
                1000,
                0.0);
        
        private final String shown;
        public final double speedReduceFactor;  
        public final double spinReduceFactor;  // 数值越大，旋转衰减越大
        public final double spinEffectFactor;  // 数值越小影响越大
        public final double tailSpeedFactor;  // 尾速相关
        
        Smoothness(String shown, 
                   double speedReduceFactor,
                   double spinReduceFactor,
                   double spinEffectFactor,
                   double tailSpeedFactor) {
            this.shown = shown;
            this.speedReduceFactor = speedReduceFactor;
            this.spinReduceFactor = spinReduceFactor;
            this.spinEffectFactor = spinEffectFactor;
            this.tailSpeedFactor = tailSpeedFactor;
        }

        @Override
        public String toString() {
            return shown;
        }
    }
}
