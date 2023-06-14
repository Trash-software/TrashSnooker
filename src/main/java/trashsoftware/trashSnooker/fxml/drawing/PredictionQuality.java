package trashsoftware.trashSnooker.fxml.drawing;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

public enum PredictionQuality {
    VERY_LOW(0, false),
    LOW(2, false),
    MEDIUM(4, false),
    HIGH(4, true),
    VERY_HIGH(8, true);
    
    public final int nPoints;
    public final boolean secondCollision;
    
    PredictionQuality(int nPoints, boolean secondCollision) {
        this.nPoints = nPoints;
        this.secondCollision = secondCollision;
    }

    @Override
    public String toString() {
        return App.getStrings().getString(Util.toLowerCamelCase("PERFORMANCE_" + name()));
    }
    
    public String toKey() {
        return Util.toLowerCamelCase(name());
    }
    
    public static PredictionQuality fromKey(String key) {
        return valueOf(Util.toAllCapsUnderscoreCase(key));
    }
}
