package trashsoftware.trashSnooker.core.phy;

import trashsoftware.trashSnooker.core.Values;

public class Phy {
    
    public static final double PLAY_MS = 1.0;
    public static final double WHITE_PREDICT_MS = 1.0;
    public static final double AI_PREDICT_MS = 1.0;

    public final TableCloth cloth;
    public final boolean isPrediction;
    public final double calculateMs;
    public final double calculationsPerSec;
    public final double calculationsPerSecSqr;
    public final double slippingFrictionTimed;
    public final double rollingFrictionTimed;
    public final double sideSpinFrictionTimed;
    
    Phy(double calculateMs, TableCloth cloth, boolean isPrediction) {
        this.cloth = cloth;
        this.calculateMs = calculateMs;
        this.isPrediction = isPrediction;
        
        calculationsPerSec = 1000.0 / calculateMs;
        calculationsPerSecSqr = calculationsPerSec * calculationsPerSec;
        slippingFrictionTimed = cloth.smoothness.slippingFriction / calculationsPerSecSqr * 1000;
        rollingFrictionTimed = cloth.smoothness.rollingFriction / calculationsPerSecSqr * 1000;
        sideSpinFrictionTimed = (slippingFrictionTimed + rollingFrictionTimed) * 0.075;
    }
    
    public double accelerationMultiplier() {
        return Math.pow(speedMultiplier(), 2);
    }
    
    public double speedMultiplier() {
        return calculateMs / PLAY_MS;
    }
    
    public double maxPowerSpeed() {
        return Values.MAX_POWER_SPEED / calculationsPerSec;
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
