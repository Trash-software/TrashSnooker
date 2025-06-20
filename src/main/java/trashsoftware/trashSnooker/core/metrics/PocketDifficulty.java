package trashsoftware.trashSnooker.core.metrics;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

import java.util.MissingResourceException;

public class PocketDifficulty {
    public static PocketDifficulty[] GREEN_TABLE_DIFFICULTIES = {
            new PocketDifficulty("veryLoose",
                    20.0, 2.21, -2.0, 95.25,
                    1.0,
                    18.0, 
                    79.375, 53.1876, 0.0,
                    53.1876, 3.96),
            new PocketDifficulty("loose",
                    14.0, 2.21, 0.0, 88.9,
                    0.75,
                    13.0, 
                    79.375, 53.1876, 0.0,
                    53.1876, 3.96),
            new PocketDifficulty("normal",
                    10.0, 2.21, 1.0, 82.55,
                    0.5,
                    10.0, 
                    79.375, 53.1876, 0.0,
                    53.1876, 3.96),
            new PocketDifficulty("tight",
                    8.0, 2.21, 2.0, 76.2,
                    0.25,
                    8.0, 
                    74.5, 53.1876, 0.0,
                    52.07, 5.08),
            new PocketDifficulty("veryTight",
                    6.0, 2.21, 3.0, 69.85,
                    0.0,
                    5.0, 
                    69.85, 53.1876, 0.0,
                    50.8, 6.35),
            new PocketDifficulty("extremeTight",
                    5.0, 2.21, 3.0, 53.1875,
                    0.0,
                    5.0,
                    40.64, 38.1, 0.0,
                    38.1, 7.62)
    };
    public static PocketDifficulty[] BLUE_TABLE_DIFFICULTIES = {
            new PocketDifficulty("normal",
                    12.0, 0.5, 5.0, 101.6,
                    0.5,
                    12.0, 15, 12.5,
                    76.2, 19.05)
    };
    public static PocketDifficulty[] RUSSIAN_TABLE_DIFFICULTIES = {
            new PocketDifficulty("normal",
                    10.0, 0.25, -7.5, 80.0,
                    0.5,
                    10.0, 15, -7.5,
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
    public final double midPocketArcRadius;
//    public final double midPocketArcWidth;
    public final double midInnerArcRadius;
    
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
                            double midPocketArcRadius,
//                            double midPocketArcWidth,
                            double midInnerArcRadius,
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
        this.midPocketArcRadius = midPocketArcRadius;
//        this.midPocketArcWidth = midPocketArcWidth;
        this.midInnerArcRadius = midInnerArcRadius;
        
        this.midPocketAngle = midPocketAngle;
        this.midPocketFallRadius = midPocketFallRadius;
        this.midCenterToSlate = midCenterToSlate;
    }

    public PocketDifficulty(String key,
                            double cornerPocketGravityZone,
                            double cornerPocketArcSize,
                            double cornerPocketAngle,
                            double cornerPocketFallRadius,
                            double arcBounceAngleRate,
                            double midPocketGravityZone,
                            double midPocketArcRadius,
                            double midPocketAngle,
                            double midPocketFallRadius,
                            double midCenterToSlate) {
        this(key,
                cornerPocketGravityZone,
                cornerPocketArcSize,
                cornerPocketAngle,
                cornerPocketFallRadius,
                arcBounceAngleRate,
                midPocketGravityZone,
                midPocketArcRadius,
                0.0,
                midPocketAngle,
                midPocketFallRadius,
                midCenterToSlate);
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
        try {
            return App.getStrings().getString(langKey);
        } catch (MissingResourceException e) {
            return key;
        }
    }
}
