package trashsoftware.trashSnooker.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.util.Util;

import java.util.*;

public class PlayerHand implements Cloneable, Comparable<PlayerHand> {
    
    public static final double REST_NATIVE_POWER_MUL = 0.8;

    final double maxPowerPercentage;
    final double controllablePowerPercentage;
    final double maxSpinPercentage;
    final double minPullDt;  // 最小力时的拉杆长度
    final double maxPullDt;
    final double minExtension;  // 最小力时的延伸长度
    final double maxExtension;
    final double powerControl;

    // 横向的mu，横向的sigma，纵向的mu，纵向的sigma
    // 横向的左塞负右塞正，纵向的高杆正低杆负
    private double[] cuePointMuSigmaXY;
    final double cueSwingMag;
    final CuePlayType cuePlayType;
    public final Hand hand;
    
    public PlayerHand(Hand hand,
                      double maxPowerPercentage,
                      double controllablePowerPercentage,
                      double maxSpinPercentage,
                      double[] pullDtExtensionDt,
                      double cueSwingMag,
                      double[] cuePointMuSigmaXY,
                      double powerControl,
                      CuePlayType cuePlayType) {
        this.hand = hand;
        this.maxPowerPercentage = maxPowerPercentage;
        this.controllablePowerPercentage = controllablePowerPercentage;
        this.maxSpinPercentage = maxSpinPercentage;
        this.minPullDt = pullDtExtensionDt[0];
        this.maxPullDt = pullDtExtensionDt[1];
        this.minExtension = pullDtExtensionDt[2];
        this.maxExtension = pullDtExtensionDt[3];
        this.cueSwingMag = cueSwingMag;
        this.cuePointMuSigmaXY = cuePointMuSigmaXY;
        this.powerControl = powerControl;
        this.cuePlayType = cuePlayType;
        
        cuePointMuSigmaXY[1] = Math.max(cuePointMuSigmaXY[1], 0.1);
        cuePointMuSigmaXY[3] = Math.max(cuePointMuSigmaXY[3], 0.1);
    }
    
    public static PlayerHand fromJson(JSONObject json, @Nullable PlayerHand reference, 
                                      double skillMul, double powerMul) {
        if (json == null) {
            if (reference == null) {
                throw new IllegalArgumentException();
            } else {
                return reference.derive(reference.hand.getAnother(), skillMul, powerMul);
            }
        }
        
        Hand hand = Hand.valueOf(json.getString("hand").toUpperCase(Locale.ROOT));

        double[] pullDtExtensionDt;
        if (json.has("pullDt")) {
            JSONArray pullDt = json.getJSONArray("pullDt");

            if (pullDt.length() == 2) {
                pullDtExtensionDt = new double[]{pullDt.getDouble(0), pullDt.getDouble(1), 50, 150};
            } else if (pullDt.length() == 4) {
                pullDtExtensionDt = new double[]{pullDt.getDouble(0),
                        pullDt.getDouble(1),
                        pullDt.getDouble(2),
                        pullDt.getDouble(3)};
            } else {
                System.err.println("Player " + " has wrong pull dt");
                pullDtExtensionDt = new double[]{50, 200, 50, 150};
            }
        } else if (reference != null) {
            pullDtExtensionDt = reference.getPullDtExtensionDt();
        } else {
            pullDtExtensionDt = new double[]{50, 200, 50, 150};
        }

        double[] muSigma;
        if (json.has("cuePointMuSigma")) {
            JSONArray muSigmaArray = json.getJSONArray("cuePointMuSigma");
            muSigma = new double[4];
            for (int i = 0; i < 4; ++i) {
                muSigma[i] = muSigmaArray.getDouble(i);
            }
        } else if (reference != null) {
            muSigma = new double[]{-reference.cuePointMuSigmaXY[0],
                    reference.cuePointMuSigmaXY[1] * skillMul,
                    reference.cuePointMuSigmaXY[2], 
                    reference.cuePointMuSigmaXY[3] * skillMul};
        } else {
            muSigma = new double[4];
        }

        CuePlayType cuePlayType;
        if (json.has("cuePlayType")) {
            String cuePlayTypeStr = json.getString("cuePlayType");
            cuePlayType = PlayerPerson.parseCuePlayType(cuePlayTypeStr);
        } else if (reference != null) {
            cuePlayType = reference.cuePlayType;
        } else {
            cuePlayType = CuePlayType.DEFAULT_PERFECT;
        }
        
        if (json.has("specialAction")) {
            cuePlayType.setSpecialAction(PlayerPerson.parseSpecialAction(json.getJSONObject("specialAction")));
        }
        
        try {
            return new PlayerHand(
                    hand,
                    json.getDouble("maxPower"),
                    json.getDouble("controllablePower"),
                    json.getDouble("spin"),
                    pullDtExtensionDt,
                    json.has("cueSwingMag") ? json.getDouble("cueSwingMag") : Objects.requireNonNull(reference).cueSwingMag * skillMul,
                    muSigma,
                    json.has("powerControl") ? json.getDouble("powerControl") : Objects.requireNonNull(reference).powerControl * skillMul,
                    cuePlayType
            );
        } catch (NullPointerException e) {
            throw new RuntimeException("Hand json has missing values, but reference object is not provided", e);
        }
    }

    public static PlayerHand fromJson(JSONObject json) {
        return fromJson(json, null, 1.0, 1.0);
    }
    
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("hand", hand.name().toLowerCase(Locale.ROOT));
        obj.put("spin", getMaxSpinPercentage());
        obj.put("maxPower", getMaxPowerPercentage());
        obj.put("controllablePower", getControllablePowerPercentage());

        JSONArray cueAction = new JSONArray(List.of(getMinPullDt(), getMaxPullDt(), getMinExtension(), getMaxExtension()));
        obj.put("pullDt", cueAction);
        obj.put("cuePlayType", getCuePlayType().toString());

//        System.out.println("Swing mag " + getCueSwingMag());
        obj.put("cueSwingMag", getCueSwingMag());

        JSONArray cuePoint = new JSONArray(cuePointMuSigmaXY);
        obj.put("cuePointMuSigma", cuePoint);
        obj.put("powerControl", getPowerControl());

        obj.put("cuePlayType", cuePlayType.toJsonStr());

        if (cuePlayType.hasAnySpecialAction()) {
            JSONObject spe = cuePlayType.specialActionsJson();
            obj.put("specialAction", spe);
        }
        
        return obj;
    }
    
    public PlayerHand derive(Hand hand, double skillMul, double powerMul) {
        double ctrlPower = controllablePowerPercentage * powerMul;
        double maxPower = Math.min(ctrlPower / 0.9, 100);
        double cuePrecision = computeCuePrecision(1.0);
        double spinControl = computeSpinControl(1.0);
        double[] cuePoint = PlayerPerson.estimateCuePoint(cuePrecision * skillMul, spinControl * skillMul);
        cuePoint[0] = -cuePointMuSigmaXY[0];
        cuePoint[2] = cuePointMuSigmaXY[2];
        double swingMag = cueSwingMag * (cuePoint[1] / cuePointMuSigmaXY[1]);
        
        return new PlayerHand(
                hand,
                maxPower,
                ctrlPower,
                maxSpinPercentage * skillMul,
                getPullDtExtensionDt(),
                swingMag,
                cuePoint,
                powerControl * skillMul,
                cuePlayType
        );
    }

    @Override
    protected PlayerHand clone() throws CloneNotSupportedException {
        PlayerHand copy = (PlayerHand) super.clone();
        copy.cuePointMuSigmaXY = cuePointMuSigmaXY.clone();
        
        return copy;
    }

    /**
     * 返回该球员在选定的发力下，控制力量的标准差
     */
    public double getPowerSd(double selectedPower) {
        double sd = (100.0 - getPowerControl()) / 100.0;
        return sd * getErrorMultiplierOfPower(selectedPower);
    }

    public double getErrorMultiplierOfPower(double selectedPower) {
        double ctrlAblePwr = getControllablePowerPercentage();
        double mul = 1;
        if (selectedPower > ctrlAblePwr) {
            // 超过正常发力范围，打点准确度大幅下降
            // 一般来说，球手的最大力量大于可控力量15%左右
            // 打点准确度最大应下降3倍
            mul += (selectedPower - ctrlAblePwr) / 5;
        }
        return mul * selectedPower / ctrlAblePwr;
    }

    public double[] getCuePointMuSigmaXY() {
        return cuePointMuSigmaXY;
    }
    
    public double[] getPullDtExtensionDt() {
        return new double[]{minPullDt,
                maxPullDt,
                minExtension,
                maxExtension};
    }

    public double getMaxPowerPercentage() {
        return maxPowerPercentage;
    }

    public double getControllablePowerPercentage() {
        return controllablePowerPercentage;
    }

    public double getPowerControl() {
        return powerControl;
    }

    public double getMaxSpinPercentage() {
        return maxSpinPercentage;
    }

    public CuePlayType getCuePlayType() {
        return cuePlayType;
    }

    public double getCueSwingMag() {
        return cueSwingMag;
    }

    public double getMaxPullDt() {
        return maxPullDt;
    }

    public double getMinPullDt() {
        return minPullDt;
    }

    public double getMinExtension() {
        return minExtension;
    }

    public double getMaxExtension() {
        return maxExtension;
    }
    
    public double average() {
//        double powerScore = (controllablePowerPercentage + maxPowerPercentage) / 2 / hand.nativePowerMul;
        double powerScore = (controllablePowerPercentage + maxPowerPercentage) / 2;
//        if (hand == Hand.REST) powerScore /= REST_NATIVE_POWER_MUL;
        
        double sum = powerScore + 
                maxSpinPercentage + 
                powerControl + 
                computeCuePrecision(1.0) + 
                computeSpinControl(1.0);
        
        return sum / 5.0;
    }
    
    public double computeCuePrecision(double handFeelEffort) {
        return Math.max(0,
                100.0 - (getCuePointMuSigmaXY()[0] +
                        getCuePointMuSigmaXY()[1]) * PlayerPerson.CUE_PRECISION_FACTOR) * handFeelEffort;
    }
    
    public double computeSpinControl(double handFeelEffort) {
        return Math.max(0,
                100.0 - (getCuePointMuSigmaXY()[2] +
                        getCuePointMuSigmaXY()[3]) * PlayerPerson.CUE_PRECISION_FACTOR) * handFeelEffort;
    }

    @Override
    public int compareTo(@NotNull PlayerHand o) {
        int cmp = Double.compare(this.average(), o.average());
        if (cmp == 0) {
            if (this.hand == Hand.REST) return 1;
            if (o.hand == Hand.REST) return -1;
        }
        return -cmp;
    }

    @Override
    public String toString() {
        return "PlayerHand{" +
                "maxPowerPercentage=" + maxPowerPercentage +
                ", controllablePowerPercentage=" + controllablePowerPercentage +
                ", maxSpinPercentage=" + maxSpinPercentage +
                ", minPullDt=" + minPullDt +
                ", maxPullDt=" + maxPullDt +
                ", minExtension=" + minExtension +
                ", maxExtension=" + maxExtension +
                ", powerControl=" + powerControl +
                ", cuePointMuSigmaXY=" + Arrays.toString(cuePointMuSigmaXY) +
                ", cueSwingMag=" + cueSwingMag +
                ", cuePlayType=" + cuePlayType +
                ", hand=" + hand +
                '}';
    }

    public enum Hand {
        LEFT(1.0),
        RIGHT(1.0),
        REST(REST_NATIVE_POWER_MUL);
        
        public final double nativePowerMul;
        
        Hand(double nativePowerMul) {
            this.nativePowerMul = nativePowerMul;
        }
        
        public Hand getAnother() {
            return switch (this) {
                case LEFT -> RIGHT;
                case RIGHT -> LEFT;
                case REST -> throw new RuntimeException("Rest does not have another");
            };
        }
        
        public String shownName(ResourceBundle strings) {
            String key = name() + "_" + "HAND";
            key = Util.toLowerCamelCase(key);
            return strings.getString(key);
        }
    }
}
