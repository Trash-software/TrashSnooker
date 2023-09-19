package trashsoftware.trashSnooker.core.metrics;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

public class PocketDifficulty {
    public static PocketDifficulty[] GREEN_TABLE_DIFFICULTIES = {
            new PocketDifficulty("veryLoose",
                    20.0, 2.21, -2.0, 95.25,
                    1.0,
                    18.0, 1.3, 0.0,
                    53.1875, 3.96),
            new PocketDifficulty("loose",
                    14.0, 2.21, 0.0, 88.9,
                    0.75,
                    13.0, 1.3, 0.0,
                    53.1875, 3.96),
            new PocketDifficulty("normal",
                    10.0, 2.21, 1.0, 82.55, 
                    0.5,
                    9.0, 1.3, 0.0,
                    53.1875, 3.96),
            new PocketDifficulty("tight",
                    8.0, 2.21, 2.0, 76.2, 
                    0.25,
                    7.5, 1.25, 0.0,
                    53.1875, 3.96),
            new PocketDifficulty("veryTight",
                    6.0, 2.21, 3.0, 69.85,
                    0.0,
                    5.0, 1.2, 0.0,
                    53.1875, 3.96)
    };
    public static PocketDifficulty[] BLUE_TABLE_DIFFICULTIES = {
            new PocketDifficulty("normal",
                    12.0, 0.5, 5.0, 101.6,
                    0.5,
                    12.0, 0.25, 12.5,
                    76.2, 19.05)
    };
    public static PocketDifficulty[] RUSSIAN_TABLE_DIFFICULTIES = {
            new PocketDifficulty("normal",
                    10.0, 0.25, -7.5, 80.0,
                    0.5, 
                    10.0, 0.25, -7.5,
                    53.0, 10.0)
    };

    public final String key;
    public final double cornerPocketGravityZone;
    public final double cornerPocketArcSize;  // currently useless
    public final double cornerPocketAngle;
//    public final double cornetPocketOut;
    public final double cornerPocketFallRadius;  // 底袋的外围，真实的下落半径
    public final double arcBounceAngleRate;
    public final double midPocketGravityZone;
    public final double midPocketArcSize;
    public final double midPocketAngle;
    public final double midPocketFallRadius;
    public final double midCenterToSlate;

    public PocketDifficulty(String key,
                            double cornerPocketGravityZone,
                            double cornerPocketArcSize,
                            double cornerPocketAngle,
                            double cornerPocketFallRadius,
                            double arcBounceAngleRate,
                            double midPocketGravityZone,
                            double midPocketArcSize,
                            double midPocketAngle,
                            double midPocketFallRadius,
                            double midCenterToSlate) {
        this.key = key;
        this.cornerPocketGravityZone = cornerPocketGravityZone;
        this.cornerPocketArcSize = cornerPocketArcSize;
        this.cornerPocketAngle = cornerPocketAngle;
        this.cornerPocketFallRadius = cornerPocketFallRadius;
        this.arcBounceAngleRate = arcBounceAngleRate;

        this.midPocketGravityZone = midPocketGravityZone;
        this.midPocketArcSize = midPocketArcSize;
        this.midPocketAngle = midPocketAngle;
        this.midPocketFallRadius = midPocketFallRadius;
        this.midCenterToSlate = midCenterToSlate;
    }
    
    public static PocketDifficulty valueOf(TableMetrics.TableBuilderFactory factory, String jsonString) {
        String camelString = Util.toLowerCamelCase(jsonString);
        for (PocketDifficulty pd : factory.supportedDifficulties) {
            if (pd.key.equals(camelString) || pd.key.equals(jsonString)) return pd;
        }
        throw new RuntimeException("No match pocket difficulty: " + jsonString);
    }

    @Override
    public String toString() {
        String langKey = Util.toLowerCamelCase("POCKET_DIFF_" + Util.toAllCapsUnderscoreCase(key));
        return App.getStrings().getString(langKey);
    }
}
