package trashsoftware.trashSnooker.core.metrics;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

public class PocketSize {
    public static final PocketSize[] SNOOKER_HOLES = {
            new PocketSize("pocketLarge", 89, 95),
            new PocketSize("pocketStd", 85, 89),
            new PocketSize("pocketSmall", 82, 87),
            new PocketSize("pocketLittle", 78, 84),
            new PocketSize("pocketTiny", 72, 78)
    };
    public static final PocketSize[] CHINESE_EIGHT_HOLES = {
            new PocketSize("pocketHuge", 93, 98),
            new PocketSize("pocketLarge", 89, 95),
            new PocketSize("pocketStd", 85, 89),
            new PocketSize("pocketSmall", 82, 85),
            new PocketSize("pocketLittle", 78, 82)
    };
    public static final PocketSize[] SIDE_POCKET_HOLES = {
            new PocketSize("pocketLarge", 112, 133),
            new PocketSize("pocketStd", 105, 127),  // 中袋是mouth的直径
            new PocketSize("pocketSmall", 98, 120)
    };
    
    public final String key;
    public final double cornerHoleDiameter;
    public final double midHoleDiameter;

    public PocketSize(String name, double cornerHoleDiameter, double midHoleDiameter) {
        this.key = name;
        this.cornerHoleDiameter = cornerHoleDiameter;
        this.midHoleDiameter = midHoleDiameter;
    }

    public static PocketSize valueOf(TableMetrics.TableBuilderFactory factory, String jsonString) {
        String camelString = Util.toLowerCamelCase("POCKET_" + jsonString);
        for (PocketSize pd : factory.supportedHoles) {
            if (pd.key.equals(camelString) || pd.key.equals(jsonString)) return pd;
        }
        throw new RuntimeException("No match pocket size: " + jsonString);
    }

    @Override
    public String toString() {
        return String.format("%s (%d mm)", App.getStrings().getString(key), (int) cornerHoleDiameter);
    }
}
