package trashsoftware.trashSnooker.core.cue;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.career.CareerSave;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.PermanentCounters;
import trashsoftware.trashSnooker.util.Util;

import java.util.Date;
import java.util.Map;

public class Cue {

    private static int fastGameInstanceId;
    
    protected CueBrand brand;
    private String instanceId;
    protected CueTip cueTip;
    
    private String customName;  // fixme

    protected Cue(CueBrand brand, String instanceId, CueTip cueTip, String customName) {
        this.brand = brand;
        this.instanceId = instanceId;
        this.cueTip = cueTip;
        this.customName = customName;
    }

    protected Cue(CueBrand brand, String instanceId) {
        this.brand = brand;
        this.instanceId = instanceId;

        if (brand.isRest) {
            // todo: 可能会有其他形状的架杆
            cueTip = CueTip.createCrossRest();
        } else {
            cueTip = CueTip.createDefault(brand.cueTipWidth, brand.cueTipThickness);
        }
    }
    
    public static Cue createForFastGame(CueBrand brand) {
        System.out.println("Created fast game cue instance for " + brand.cueId);
        return new Cue(brand,
                brand.getCueId() + "-fast-" + fastGameInstanceId++,
                brand.isRest ? 
                        CueTip.createCrossRest() :
                        CueTip.createDefault(brand.cueTipWidth, brand.cueTipThickness),
                brand.getName()
        );
    }

    public static Cue createForCareerGameAi(CueBrand brand) {
        System.out.println("Created ai cue instance for " + brand.cueId);
        return new Cue(brand,
                brand.getCueId() + "-ai-" + fastGameInstanceId++,
                CueTip.createDefault(brand.cueTipWidth, brand.cueTipThickness),
                brand.getName() + "-ai"
        );
    }
    
    public static Cue createForCareer(CueBrand cueBrand, CueTip tip, CareerSave owner) {
//        String instanceId = cueBrand.getCueId() + "-" + owner.getPlayerId() + "-" +
//                Util.TIME_FORMAT_SEC.format(new Date());
        String instanceId = cueBrand.getCueId() + "-" + 
                owner.getPlayerId() + "-" + PermanentCounters.getInstance().nextCueInstance();
        return new Cue(
                cueBrand,
                instanceId,
                tip,
                cueBrand.getName() + "-" + owner.getPlayerName()
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
                jsonObject.getString("customName")
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
