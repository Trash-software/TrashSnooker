package trashsoftware.trashSnooker.core.cue;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.career.CareerSave;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.PermanentCounters;

import java.util.Map;

public class Cue {

    private static int oneTimeInstanceCounter;
    
    public static final String BRAND_SEPARATOR = ":";
    
    protected CueBrand brand;
    private String instanceId;
    protected CueTip cueTip;
    private final boolean permanent;  // 是否是生涯模式的球员私有杆
    
    private String customName;  // fixme

    protected Cue(CueBrand brand, 
                  String instanceId, 
                  CueTip cueTip, 
                  String customName,
                  boolean isPermanent) {
        this.brand = brand;
        this.instanceId = instanceId;
        this.cueTip = cueTip;
        this.customName = customName;
        this.permanent = isPermanent;

        System.out.println("Created cue instance " + instanceId);
    }

//    protected Cue(CueBrand brand, String instanceId) {
//        this.brand = brand;
//        this.instanceId = instanceId;
//
//        if (brand.isRest) {
//            // todo: 可能会有其他形状的架杆
//            cueTip = CueTip.createCrossRest();
//        } else {
//            cueTip = CueTip.createDefault(brand.cueTipWidth, brand.cueTipThickness);
//        }
//    }

    public static Cue createRest(CueBrand brand) {
        return new Cue(brand,
                brand.getCueId() + BRAND_SEPARATOR + "rest",
                CueTip.createCrossRest(),
                brand.getName(),
                false
        );
    }
    
    public static Cue createOneTimeInstance(CueBrand brand) {
        return new Cue(brand,
                brand.getCueId() + BRAND_SEPARATOR + "fast-" + oneTimeInstanceCounter++,
                CueTip.createDefault(brand.cueTipWidth, brand.cueTipThickness),
                brand.getName(),
                false
        );
    }

//    public static Cue createForCareerGameAi(CueBrand brand) {
//        System.out.println("Created ai cue instance for " + brand.cueId);
//        return new Cue(brand,
//                brand.getCueId() + "-ai-" + oneTimeInstanceCounter++,
//                CueTip.createDefault(brand.cueTipWidth, brand.cueTipThickness),
//                brand.getName() + "-ai",
//                false
//        );
//    }

    public static Cue createForReplay(CueBrand brand) {
        return new Cue(brand,
                brand.getCueId() + BRAND_SEPARATOR + "replay",
                CueTip.createDefault(brand.cueTipWidth, brand.cueTipThickness),
                brand.getName() + BRAND_SEPARATOR + "replay",
                false
        );
    }
    
    public static Cue createForCareer(CueBrand cueBrand, CueTip tip, CareerSave owner) {
//        String instanceId = cueBrand.getCueId() + "-" + owner.getPlayerId() + "-" +
//                Util.TIME_FORMAT_SEC.format(new Date());
        String instanceId = cueBrand.getCueId() + BRAND_SEPARATOR + 
                owner.getPlayerId() + "-" + PermanentCounters.getInstance().nextCueInstance();
        return new Cue(
                cueBrand,
                instanceId,
                tip,
                cueBrand.getName() + BRAND_SEPARATOR + owner.getPlayerName(),
                true
        );
    }
    
    public static Cue fromJson(JSONObject jsonObject,
                               DataLoader loader,
                               Map<String, CueTip> tipInstances) {
        String instanceId = jsonObject.getString("instanceId");
        CueBrand cueBrand = loader.getCueById(jsonObject.getString("brand"));
        CueTip tip = tipInstances.get(jsonObject.getString("tipId"));
        return new Cue(
                cueBrand,
                instanceId,
                tip,
                jsonObject.getString("customName"),
                true
        );
    }
    
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("instanceId", instanceId);
        jsonObject.put("brand", brand.cueId);
        jsonObject.put("tipId", cueTip.getInstanceId());
        jsonObject.put("customName", customName);
        return jsonObject;
    }

    public boolean isPermanent() {
        return permanent;
    }

    public void setCueTip(CueTip cueTip) {
        this.cueTip = cueTip;
    }

    @NotNull
    public CueTip getCueTip() {
        return cueTip;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public CueBrand getBrand() {
        return brand;
    }

    public double getOrigPowerMultiplier() {
        return brand.powerMultiplier;
    }

    public double getPowerMultiplier() {
        return brand.powerMultiplier;
    }

    public double getOrigSpinMultiplier() {
        return brand.spinMultiplier;
    }

    public double getSpinMultiplier() {
        return brand.spinMultiplier * cueTip.getGrip();
    }

    public double getAccuracyMultiplier() {
        return brand.accuracyMultiplier;
    }

    public double getEndWidth() {
        return brand.endWidth;
    }

    public double getCueTipWidth() {
        return brand.cueTipWidth;
    }
    
    public String getName() {
        return brand.getName();  // todo
    }

    public enum Size {
        VERY_SMALL,
        SMALL,
        MEDIUM,
        BIG,
        HUGE
    }
}
