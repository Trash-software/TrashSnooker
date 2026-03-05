package trashsoftware.trashSnooker.core.metrics;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

import java.util.Optional;

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

    @Override
    public String toString() {
        String key = Util.toLowerCamelCase("CUSHION_SPEC_" + name());
        if (App.getStrings().containsKey(key)) return App.getStrings().getString(key);
        else return name();
    }
}
