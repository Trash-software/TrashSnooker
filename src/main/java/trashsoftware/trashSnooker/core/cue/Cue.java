package trashsoftware.trashSnooker.core.cue;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.career.CareerSave;
import trashsoftware.trashSnooker.core.metrics.BallMetrics;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.PermanentCounters;
import trashsoftware.trashSnooker.util.Util;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

public class Cue {

    public static double BOTTOM_SUE_POINT = 0.8;
    private static int oneTimeInstanceCounter;
    private static Cue placeHolderCue;  // 有的时候没办法，确实需要一根杆
    
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
        CueTip tip;
        if (brand.isBreakCue()) {
            tip = CueTip.createDefaultBreak(brand.cueTipWidth, brand.tipSize.getDefaultTipThickness());
        } else {
            tip = CueTip.createDefault(brand.cueTipWidth, brand.tipSize.getDefaultTipThickness());
        }
        
        return new Cue(brand,
                brand.getCueId() + BRAND_SEPARATOR + "fast-" + oneTimeInstanceCounter++,
                tip,
                brand.getName(),
                false
        );
    }

    public static Cue createForTempView(CueBrand brand) {
        return new Cue(brand,
                brand.getCueId() + BRAND_SEPARATOR + "view",
                CueTip.createDefault(brand.cueTipWidth, brand.tipSize.getDefaultTipThickness()),
                brand.getName() + BRAND_SEPARATOR + "view",
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
    
    public static Cue getPlaceHolderCue() {
        if (placeHolderCue == null) {
            placeHolderCue = createForTempView(DataLoader.getInstance().getCueById("stdSnookerCue"));
        }
        return placeHolderCue;
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
        return brand.getElasticity();
    }

    public double getSpinMultiplier() {
        return brand.getElasticity();
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

    /**
     * 将[-1,1]范围的打点转化为不会呲杆的打点
     */
    public double[] aiCuePoint(double[] aiCuePoint, BallMetrics ballMetrics) {
        double fb = aiCuePoint[0];
        double side = aiCuePoint[1];
        
        double radius = getCueAbleRelRadius(ballMetrics);
        
        if (fb < 0) {
            // 这里是高杆正低杆负
            fb *= BOTTOM_SUE_POINT;
        }
        
        return new double[]{fb * radius, side * radius};
    }
    
    public double[][] getCueAbleArea(BallMetrics ballMetrics, int sep) {
        double baseRadius = getCueAbleRelRadius(ballMetrics);
        
        double tickDeg = 360.0 / sep;
        
        double[][] result = new double[sep][2];
        
        for (int degI = 0; degI < sep; degI++) {
            double deg = degI * tickDeg;
            double rad = Math.toRadians(deg);
            double x = Math.cos(rad);
            double y = Math.sin(rad);
            
            double yMul = 1.0;
            if (deg > 0 && deg < 180) {
                yMul = 1.0 - y * (1 - BOTTOM_SUE_POINT);  // sin(rad)，如果要改y就要把这里写明
            }

            result[degI][0] = x * baseRadius;
            result[degI][1] = y * baseRadius * yMul;
        }
        return result;
    }

    /**
     * @return 不会呲杆的打点范围，0-1之间
     */
    public double getCueAbleRelRadius(BallMetrics ballMetrics) {
        CueTip tip = getCueTip();
        
        // 来自于一个我没想起来的公式里的3/4次方
        double maxGripAngle = Math.pow(tip.getRadius() / ballMetrics.ballRadius, 0.75) 
                * tip.getGrip() * 170.0;
//        System.out.println(tip.getBrand().id() + " Grip angle " + maxGripAngle);
        return Math.sin(Math.toRadians(maxGripAngle));
    }
}
