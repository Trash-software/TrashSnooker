package trashsoftware.trashSnooker.core.cue;

public enum CueSize {
    VERY_SMALL(3.0),
    SMALL(3.0),
    MEDIUM(4.0),
    BIG(4.5),
    HUGE(5.0);
    
    private final double defaultTipThickness;
    
    CueSize(double defaultTipThickness) {
        this.defaultTipThickness = defaultTipThickness;
    }

    public double getDefaultTipThickness() {
        return defaultTipThickness;
    }
}
