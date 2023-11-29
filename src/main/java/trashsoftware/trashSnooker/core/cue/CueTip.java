package trashsoftware.trashSnooker.core.cue;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.career.CareerSave;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.PermanentCounters;
import trashsoftware.trashSnooker.util.Util;

import java.util.Date;

public class CueTip {
    
    public static double TIP_HEALTH_LOW = 0.1;
    private static int instanceCounter;
    
    private final String instanceId;
    private final CueTipBrand brand;
    public final boolean isRest;
    private double radius;
    private double totalHp;
    private double hp;
    
    CueTip(String instanceId, CueTipBrand brand, double radius, double totalHp, double hp, boolean isRest) {
        this.instanceId = instanceId;
        this.brand = brand;
        this.radius = radius;
        this.totalHp = totalHp;
        this.hp = hp;
        this.isRest = isRest;
    }
    
    public static CueTip createByCue(CueBrand cueBrand, CueTipBrand tipBrand, CareerSave owner) {
//        String instanceId = tipBrand.id() + "-" + owner.getPlayerId() + "-" + 
//                Util.TIME_FORMAT_SEC.format(new Date());
        String instanceId = tipBrand.id() + "-" + PermanentCounters.getInstance().nextCueInstance();
        
        double totalHp = calculateTotalHp(tipBrand.totalHp(), 
                tipBrand.maxRadius() * 2,
                cueBrand.getCueTipWidth());
        
        return new CueTip(instanceId,
                tipBrand,
                cueBrand.getCueTipWidth() / 2,
                totalHp,
                totalHp,
                false);
    }
    
    public static CueTip createDefault(double diameter, double thickness) {
        var brand = CueTipBrand.createDefault(diameter, thickness);
        return new CueTip(
//                "universalTip-" + instanceCounter++,
                "universalTip-" + PermanentCounters.getInstance().nextTipInstance(),
                brand,
                brand.maxRadius(), 
                brand.totalHp(),
                calculateTotalHp(brand.totalHp(), brand.maxRadius() * 2, diameter),
                false
        );
    }

    public static CueTip createCrossRest() {
        var brand = CueTipBrand.createCrossRest();
        return new CueTip(
                "crossRest",
                brand,
                1,
                1,
                1,
                true
        );
    }
    
    public static double calculateTotalHp(double origTotalHp, double origDiameter, double diameter) {
        return origTotalHp * Math.pow(diameter / origDiameter, 2);
    }
    
    public static CueTip fromJson(JSONObject json) {
        CueTipBrand brand = CueTipBrand.getById(json.getString("brand"));
        if (brand == null) {
            EventLogger.warning("No tip brand called " + json.getString("brand"));
            return createDefault(10, 5);
        }
        String instanceId = json.getString("instanceId");
        double totalHp = json.getDouble("totalHp");
        double hp = json.getDouble("hp");
        double diameter = json.getDouble("diameter");
        
        return new CueTip(instanceId, brand, diameter / 2, totalHp, hp, false);
    }
    
    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        object.put("brand", brand.id());
        object.put("instanceId", instanceId);
        object.put("diameter", radius * 2);
        object.put("totalHp", totalHp);
        object.put("hp", hp);
        return object;
    }

    public CueTipBrand getBrand() {
        return brand;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public double getHp() {
        return hp;
    }

    public double getTotalDurability() {
        return totalHp;
    }
    
    public double getBrandOrigDurability() {
        return brand.totalHp();
    }

    public double getRadius() {
        return radius;
    }

    /**
     * @return 是不是就是这杆打爆的
     */
    public boolean reduceHp(double value) {
        double curHp = hp;
        System.out.println("hp reduced " + value);
        hp -= value;
        return curHp > 0 && hp <= 0;
    }
    
    public double getPower() {
        if (isBroken()) {
            return 0.75;
        } else {
            return 1.0;  // todo: 皮头也有传力
        }
    }
    
    public double getGrip() {
        double hpPercentage = getHpPercentage();
        if (hpPercentage <= 0) {
            return 0.25;
        } else if (hpPercentage < TIP_HEALTH_LOW) {
            return brand.origGrip() * Algebra.shiftRangeSafe(
                    0.0,
                    TIP_HEALTH_LOW, 
                    0.8,
                    1.0,
                    hpPercentage
            );
        }
        return brand.origGrip();
    }
    
    public double getHpPercentage() {
        return hp / totalHp;
    }
    
    public boolean isDecaying() {
        return getHpPercentage() < TIP_HEALTH_LOW;
    }
    
    public boolean isBroken() {
        return getHpPercentage() <= 0;
    }

    public double getThickness() {
        return Algebra.shiftRangeSafe(
                0, brand.totalHp(),
                brand.origThickness() * 0.3, brand.origThickness(),
                getHp()
        );
    }
}
