package trashsoftware.trashSnooker.core.cue;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.career.CareerSave;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.PermanentCounters;
import trashsoftware.trashSnooker.util.Util;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

public class Cue {

    private static int oneTimeInstanceCounter;
    
    public static final String BRAND_SEPARATOR = ":";
    
    protected CueBrand brand;
    private final String instanceId;
    protected CueTip cueTip;
    private final boolean permanent;  // 是否是生涯模式的球员私有杆
    @NotNull
    private Date creationTime;
    @NotNull
    private Date lastSelectTime;
    
    private final String customName;  // fixme

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
        
        creationTime = new Date();
        lastSelectTime = new Date(0);

        System.out.println("Created cue instance " + instanceId);
    }

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
                CueTip.createDefault(brand.cueTipWidth, brand.tipSize.getDefaultTipThickness()),
                brand.getName(),
                false
        );
    }

    public static Cue createForReplay(CueBrand brand) {
        return new Cue(brand,
                brand.getCueId() + BRAND_SEPARATOR + "replay",
                CueTip.createDefault(brand.cueTipWidth, brand.tipSize.getDefaultTipThickness()),
                brand.getName() + BRAND_SEPARATOR + "replay",
                false
        );
    }
    
    public static Cue createForCareer(CueBrand cueBrand, CueTip tip, CareerSave owner) {
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
        if (tip == null) {
            System.err.println("No tip!");
        }
        Cue cue = new Cue(
                cueBrand,
                instanceId,
                tip,
                jsonObject.getString("customName"),
                true
        );
        try {
            if (jsonObject.has("creationTime")) {
                cue.creationTime = Util.TIME_FORMAT_SEC.parse(jsonObject.getString("creationTime"));
            }
            if (jsonObject.has("lastSelectTime")) {
                cue.lastSelectTime = Util.TIME_FORMAT_SEC.parse(jsonObject.getString("lastSelectTime"));
            }
        } catch (ParseException e) {
            EventLogger.warning(e);
        }
        return cue;
    }
    
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("instanceId", instanceId);
        jsonObject.put("brand", brand.cueId);
        jsonObject.put("tipId", cueTip.getInstanceId());
        jsonObject.put("customName", customName);
        jsonObject.put("creationTime", Util.TIME_FORMAT_SEC.format(creationTime));
        jsonObject.put("lastSelectTime", Util.TIME_FORMAT_SEC.format(lastSelectTime));
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
        return brand.powerMultiplier * cueTip.getPower();
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

    public @NotNull Date getCreationTime() {
        return creationTime;
    }

    public @NotNull Date getLastSelectTime() {
        return lastSelectTime;
    }

    public void setLastSelectTime() {
        this.lastSelectTime = new Date();
    }
}
