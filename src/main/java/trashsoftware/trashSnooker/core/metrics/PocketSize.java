package trashsoftware.trashSnooker.core.metrics;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

public class PocketSize {
    public static final PocketSize[] SNOOKER_HOLES = {
            new PocketSize("pocketLarge", 
                    88.9, 
                    86.36),
            new PocketSize("pocketStd", 
                    84.15, 
                    82.55),
            new PocketSize("pocketStdS",
                    82.55,
                    80.01),
            new PocketSize("pocketSmall", 
                    80.01, 
                    78.74),
            new PocketSize("pocketLittle", 
                    77.47, 
                    74.93),
            new PocketSize("pocketTiny", 
                    72.39, 
                    69.85)
    };
    public static final PocketSize[] CHINESE_EIGHT_HOLES = {
            new PocketSize("pocketHuge", 92.71, 90.17),
            new PocketSize("pocketLarge", 88.9, 86.36),
            new PocketSize("pocketStd",
                    86.36,
                    84.15),
            new PocketSize("pocketStdS",
                    84.15,
                    82.55),
            new PocketSize("pocketSmall", 82.55, 80.01),
            new PocketSize("pocketLittle", 77.47, 74.93)
    };
    public static final PocketSize[] AMERICAN_NINE_POCKETS = {
            new PocketSize("pocketLarge", 123.825, 133),
            new PocketSize("pocketStd", 117.475, 127),  // 中袋是mouth的直径
            new PocketSize("pocketSmall", 111.125, 120)
    };
    public static final PocketSize[] RUSSIAN_POCKETS = {
            new PocketSize("pocketStd", 71.0, 80.0)
    };
    
    public final String key;
    public final double cornerHoleDiameter;
    public final double cornerMouthWidth;
    public final double midHoleDiameter;
    public final double midThroatWidth;
    public final double midArcRadius;
    public final boolean midHoleThroatSpecified;

    public PocketSize(String name, 
                      double cornerHoleDiameter, 
                      double cornerMouthWidth,
                      double midHoleDiameter, 
                      double midThroatWidth,
                      double midArcRadius,
                      boolean midHoleThroatSpecified) {
        this.key = name;
        this.cornerHoleDiameter = cornerHoleDiameter;
        this.cornerMouthWidth = cornerMouthWidth;
        this.midHoleDiameter = midHoleDiameter;
        this.midThroatWidth = midThroatWidth;
        this.midArcRadius = midArcRadius;
        this.midHoleThroatSpecified = midHoleThroatSpecified;
    }

    public PocketSize(String name, 
                      double cornerHoleDiameter, 
                      double midHoleDiameter) {
        this(name, cornerHoleDiameter, cornerHoleDiameter, midHoleDiameter, midHoleDiameter, midHoleDiameter / 2, false);
    }

    public static PocketSize valueOf(TableMetrics.TableBuilderFactory factory, String jsonString) {
        String resString = Util.toAllCapsUnderscoreCase(jsonString);
        String camelString = Util.toLowerCamelCase("POCKET_" + resString);
        for (PocketSize pd : factory.supportedHoles) {
            if (pd.key.equals(camelString) || pd.key.equals(jsonString)) return pd;
        }
        throw new RuntimeException("No match pocket size: " + jsonString);
    }

    @Override
    public String toString() {
        return String.format("%s (%.0f mm)", App.getStrings().getString(key), cornerHoleDiameter);
    }
}
