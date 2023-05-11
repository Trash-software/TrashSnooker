package trashsoftware.trashSnooker.core.metrics;

import trashsoftware.trashSnooker.fxml.App;

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
            new PocketSize("pocketStd", 105, 105),
    };
    
    public final String key;
    public final double cornerHoleDiameter;
    public final double midHoleDiameter;

    public PocketSize(String name, double cornerHoleDiameter, double midHoleDiameter) {
        this.key = name;
        this.cornerHoleDiameter = cornerHoleDiameter;
        this.midHoleDiameter = midHoleDiameter;
    }

    @Override
    public String toString() {
        return String.format("%s (%d mm)", App.getStrings().getString(key), (int) cornerHoleDiameter);
    }
}
