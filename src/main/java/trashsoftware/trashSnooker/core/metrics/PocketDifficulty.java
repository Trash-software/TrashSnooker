package trashsoftware.trashSnooker.core.metrics;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

public class PocketDifficulty {
    public static PocketDifficulty[] GREEN_TABLE_DIFFICULTIES = {
            new PocketDifficulty("veryLoose",
                    16.0, 1.5, 8.0,
                    0.6, 1.0,
                    15.0, 0.8, 0.0),
            new PocketDifficulty("loose",
                    12.5, 1.5, 6.0,
                    0.45, 0.75,
                    12.5, 1.0, 0.0),
            new PocketDifficulty("normal",
                    10.0, 1.5, 5.0,
                    0.3, 0.5,
                    10.0, 1.0, 0.0),
            new PocketDifficulty("tight",
                    8.5, 1.5, 4.0,
                    0.15, 0.25,
                    8.5, 1.0, 0.0),
            new PocketDifficulty("veryTight",
                    7.0, 1.5, 4.0,
                    0.0, 0.0,
                    7.0, 1.0, 0.0)
    };
    public static PocketDifficulty[] BLUE_TABLE_DIFFICULTIES = {
            new PocketDifficulty("normal",
                    15.0, 0.5, 7.0,
                    0.5, 0.5,
                    10.0, 0.2, 12.5)
    };

    public final String key;
    public final double cornerPocketGravityZone;
    public final double cornerPocketArcSize;  // currently useless
    public final double cornerPocketAngle;
    public final double cornetPocketOut;
    public final double arcBounceAngleRate;
    public final double midPocketGravityZone;
    public final double midPocketArcSize;
    public final double midPocketAngle;

    public PocketDifficulty(String key,
                            double cornerPocketGravityZone,
                            double cornerPocketArcSize,
                            double cornerPocketAngle,
                            double cornetPocketOut,
                            double arcBounceAngleRate,
                            double midPocketGravityZone,
                            double midPocketArcSize,
                            double midPocketAngle) {
        this.key = key;
        this.cornerPocketGravityZone = cornerPocketGravityZone;
        this.cornerPocketArcSize = cornerPocketArcSize;
        this.cornerPocketAngle = cornerPocketAngle;
        this.cornetPocketOut = cornetPocketOut;
        this.arcBounceAngleRate = arcBounceAngleRate;

        this.midPocketGravityZone = midPocketGravityZone;
        this.midPocketArcSize = midPocketArcSize;
        this.midPocketAngle = midPocketAngle;
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
