package trashsoftware.trashSnooker.core.metrics;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

public class PocketDifficulty {
    public static PocketDifficulty[] GREEN_TABLE_DIFFICULTIES = {
            new PocketDifficulty("veryLoose",
                    25.0, 3.0, 1.0,
                    15.0, 1.0, 0.0),
            new PocketDifficulty("loose",
                    20.0, 1.8, 2.0,
                    13.5, 1.0, 0.0),
            new PocketDifficulty("normal",
                    16.0, 1.5, 4.0,
                    12.0, 1.0, 0.0),
            new PocketDifficulty("tight",
                    14.0, 1.35, 5.0,
                    10.0, 1.0, 0.0),
            new PocketDifficulty("veryTight",
                    14.0, 1.25, 6.0,
                    10.0, 1.0, 0.0)
    };
    public static PocketDifficulty[] BLUE_TABLE_DIFFICULTIES = {
            new PocketDifficulty("normal",
                    15.0, 0.0, 7.0,
                    10.0, 0.0, 20.0)
    };

    public final String key;
    public final double cornerPocketGravityZone;
    public final double cornerPocketArcSize;  // currently useless
    public final double cornerPocketAngle;
    public final double midPocketGravityZone;
    public final double midPocketArcSize;
    public final double midPocketAngle;

    public PocketDifficulty(String key,
                            double cornerPocketGravityZone,
                            double cornerPocketArcSize,
                            double cornerPocketAngle,
                            double midPocketGravityZone,
                            double midPocketArcSize,
                            double midPocketAngle) {
        this.key = key;
        this.cornerPocketGravityZone = cornerPocketGravityZone;
        this.cornerPocketArcSize = cornerPocketArcSize;
        this.cornerPocketAngle = cornerPocketAngle;

        this.midPocketGravityZone = midPocketGravityZone;
        this.midPocketArcSize = midPocketArcSize;
        this.midPocketAngle = midPocketAngle;
    }

    @Override
    public String toString() {
        String langKey = Util.toLowerCamelCase("POCKET_DIFF_" + Util.toAllCapsUnderscoreCase(key));
        return App.getStrings().getString(langKey);
    }
}
