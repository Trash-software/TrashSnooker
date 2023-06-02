package trashsoftware.trashSnooker.core.phy;

public class Phy {
    
    public static final double PLAY_MS = 1.0;
    public static final double WHITE_PREDICT_MS = 1.0;
    public static final double AI_PREDICT_MS = 1.0;

    public final TableCloth cloth;
    public final boolean isPrediction;
    public final double calculateMs;
    public final double calculationsPerSec;
    public final double calculationsPerSecSqr;
    public final double speedReducer;
    public final double spinReducer;  // 数值越大，旋转衰减越大
    public final double sideSpinReducer;
    public final double spinEffect;  // 数值越小影响越大
    
    Phy(double calculateMs, TableCloth cloth, boolean isPrediction) {
        this.cloth = cloth;
        this.calculateMs = calculateMs;
        this.isPrediction = isPrediction;
        
        calculationsPerSec = 1000.0 / calculateMs;
        calculationsPerSecSqr = calculationsPerSec * calculationsPerSec;
        speedReducer = cloth.smoothness.speedReduceFactor / calculationsPerSecSqr;
        spinReducer = cloth.smoothness.spinReduceFactor / calculationsPerSecSqr;
        sideSpinReducer = 120.0 / calculationsPerSecSqr;
        spinEffect = cloth.smoothness.spinEffectFactor / calculateMs;
    }
    
    public double accelerationMultiplier() {
        return Math.pow(speedMultiplier(), 2);
    }
    
    public double speedMultiplier() {
        return calculateMs / PLAY_MS;
    }
    
    public static class Factory {
        public static Phy createPlayPhy(TableCloth cloth) {
//            System.out.println("Play " + cloth);
            return new Phy(PLAY_MS, cloth, false);
        }

        public static Phy createWhitePredictPhy(TableCloth cloth) {
//            System.out.println("Predict " + cloth);
            return new Phy(WHITE_PREDICT_MS, cloth, true);
        }

        public static Phy createAiPredictPhy(TableCloth cloth) {
//            System.out.println("Predict " + cloth);
            return new Phy(AI_PREDICT_MS, cloth, true);
        }
    }
}
