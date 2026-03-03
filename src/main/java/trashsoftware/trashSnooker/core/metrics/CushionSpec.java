package trashsoftware.trashSnooker.core.metrics;

public enum CushionSpec {
    VERY_HARD(1.8),
    HARD(1.35),
    NORMAL(1.0),
    SOFT(0.8),
    VERY_SOFT(0.6);
    
    public final double hardness;
    
    CushionSpec(double hardness) {
        this.hardness = hardness;
    }
}
