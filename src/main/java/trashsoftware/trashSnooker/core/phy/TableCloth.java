package trashsoftware.trashSnooker.core.phy;

import org.json.JSONObject;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

public class TableCloth {

    public static final double RANDOM_ERROR_FACTOR = 1.0 / 1.2;
    public static final double FIXED_ERROR_FACTOR = 1.0 / 60.0;
    public static final double SLIP_ACCELERATE_EFFICIENCY = 0.333;

    private static final double baseSlippingFriction = 2.75;
    private static final double baseRollingFriction = 0.075;
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
                0.96),
        NORMAL(1.12,
                1.12,
                0.96),
        MEDIUM(1.35,
                1.3,
                0.92),
        SLOW(1.67,
                1.5,
                0.85);

        public final double slippingFriction;
        public final double rollingFriction;
        public final double cushionBounceFactor;  // 库的硬度

        Smoothness(double slippingFrictionFactor,
                   double rollingFrictionFactor,
                   double cushionBounceFactor) {
            this.slippingFriction = baseSlippingFriction * slippingFrictionFactor;
            this.rollingFriction = baseRollingFriction * rollingFrictionFactor;
            this.cushionBounceFactor = cushionBounceFactor;
        }

        @Override
        public String toString() {
            String key = Util.toLowerCamelCase("CLOTH_SMOOTHNESS_" + name());
            return App.getStrings().getString(key);
        }
    }
}
