package trashsoftware.trashSnooker.core.phy;

import org.json.JSONObject;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

public class TableCloth {

    public static final double RANDOM_ERROR_FACTOR = 1.0 / 1.2;
    public static final double FIXED_ERROR_FACTOR = 1.0 / 60.0;
    
    private static final double baseSpeedReducer = 85;
    private static final double baseSpinReducer = 3500;
    private static final double baseSpinEffect = 920;
    public final Goodness goodness;
    public final Smoothness smoothness;
    
    public TableCloth(Goodness goodness, Smoothness smoothness) {
        this.goodness = goodness;
        this.smoothness = smoothness;
    }
    
    public static TableCloth fromJson(JSONObject jsonObject) {
        return new TableCloth(
                Goodness.valueOf(jsonObject.getString("goodness")),
                Smoothness.valueOf(jsonObject.getString("smoothness")));
    }
    
    public JSONObject toJson() {
        JSONObject clothObj = new JSONObject();
        clothObj.put("smoothness", smoothness.name());
        clothObj.put("goodness", goodness.name());
        return clothObj;
    }

    @Override
    public String toString() {
        return "TableCloth{" +
                "goodness=" + goodness +
                ", smoothness=" + smoothness +
                '}';
    }
    public enum Goodness {
        EXCELLENT(0.0, 0.0),
        GOOD(0.07, 0.12),
        NORMAL(0.15, 0.15),
        BAD(0.3, 0.15),
        WORSE(0.6, 0.1),
        TERRIBLE( 0.9, 0.0);

        public final double errorFactor;
        public final double fixedErrorFactor;

        Goodness(double errorFactor, double fixedErrorFactor) {
            this.errorFactor = errorFactor;
            this.fixedErrorFactor = fixedErrorFactor;
        }

        @Override
        public String toString() {
            String key = Util.toLowerCamelCase("CLOTH_GOODNESS_" + name());
            return App.getStrings().getString(key);
        }
    }

    public enum Smoothness {
        FAST(1.0,
                1.0,
                1.0,
                0.75,
                0.96),
        NORMAL(1.12,
                1.12,
                1.12,
                0.65,
                0.95),
        MEDIUM(1.35,
                1.3,
                1.25,
                0.4,
                0.9),
        SLOW(1.67,
                1.5,
                1.35,
                0.1,
                0.8);

        public final double speedReduceFactor;
        public final double spinReduceFactor;  // 数值越大，旋转衰减越大
        public final double spinEffectFactor;  // 数值越小影响越大
        public final double tailSpeedFactor;  // 尾速相关
        public final double cushionBounceFactor;  // 库的硬度

        Smoothness(double speedReduceFactor,
                   double spinReduceFactor,
                   double spinEffectFactor,
                   double tailSpeedFactor,
                   double cushionBounceFactor) {
            this.speedReduceFactor = baseSpeedReducer * speedReduceFactor;
            this.spinReduceFactor = baseSpinReducer * spinReduceFactor;
            this.spinEffectFactor = baseSpinEffect / spinEffectFactor;
            this.tailSpeedFactor = tailSpeedFactor;
            this.cushionBounceFactor = cushionBounceFactor;
        }

        @Override
        public String toString() {
            String key = Util.toLowerCamelCase("CLOTH_SMOOTHNESS_" + name());
            return App.getStrings().getString(key);
        }
    }
}
