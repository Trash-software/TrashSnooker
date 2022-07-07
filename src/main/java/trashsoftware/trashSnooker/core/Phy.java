package trashsoftware.trashSnooker.core;

public enum Phy {
    PLAY(1.0),
    PREDICT(10.0);

    public final double calculateMs;
    public final double calculationsPerSec;
    public final double calculationsPerSecSqr;
    public final double speedReducer;
    public final double spinReducer;  // 数值越大，旋转衰减越大
    public final double sideSpinReducer;
    public final double spinEffect;  // 数值越小影响越大
    
    Phy(double calculateMs) {
        this.calculateMs = calculateMs;
        calculationsPerSec = 1000.0 / calculateMs;
        calculationsPerSecSqr = calculationsPerSec * calculationsPerSec;
        speedReducer = 120.0 / calculationsPerSecSqr;
        spinReducer = 4000.0 / calculationsPerSecSqr;
        sideSpinReducer = 100.0 / calculationsPerSecSqr;
        spinEffect = 1400.0 / calculateMs;
    }
}
