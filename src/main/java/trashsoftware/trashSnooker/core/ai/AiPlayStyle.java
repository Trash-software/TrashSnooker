package trashsoftware.trashSnooker.core.ai;

import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.GamePlayStage;
import trashsoftware.trashSnooker.core.InGamePlayer;

import java.util.Locale;

public class AiPlayStyle {
    public static final AiPlayStyle PERFECT =
            new AiPlayStyle(100.0, 100.0, 100.0, 100.0, 
                    80.0, 50,
                    new double[]{15.0, 60.0}, 75, 100,
                    "right", false, 2);

    public final double precision;
    public final double stability;  // 准度稳定性
    public final double position;  // 走位能力
    public final double defense;
    public final double attackPrivilege;  // 进攻偏好
    public final double likeShow;  // 偏好大力及杆法
    public final double likeSide;  // 偏好加塞
    public final double[] likePowerRange;  // 舒适的力度区间，0-100
    public final double doubleAbility;  // 翻袋能力
    public final SnookerBreakMethod snookerBreakMethod;
    public final boolean cebSideBreak;  // Chinese eight balls 侧面冲球
    public final int snookerWithdrawLimit;  // 需要做多少杆斯诺克才认输

    public AiPlayStyle(double precision, double stability, double position, double defense,
                       double attackPrivilege, double likeShow,
                       double[] likePowerRange, 
                       double likeSide,
                       double doubleAbility,
                       String snookerBreakMethod, boolean cebSideBreak,
                       int snookerWithdrawLimit) {
        this.precision = precision;
        this.stability = stability;
        this.position = position;
        this.defense = defense;
        this.attackPrivilege = attackPrivilege;  // 权重100的选手只要有下就会进攻
        this.likeShow = likeShow;
        this.likePowerRange = likePowerRange;
        this.likeSide = likeSide;
        this.doubleAbility = doubleAbility;
        this.snookerBreakMethod = SnookerBreakMethod.valueOf(snookerBreakMethod.toUpperCase(Locale.ROOT));
        this.cebSideBreak = cebSideBreak;
        this.snookerWithdrawLimit = snookerWithdrawLimit;
    }

//    /**
//     * @param spins         {高低杆, 左右塞}, 范围都是(-1,1)
//     * @param selectedPower 选择的力度, 0-100
//     * @param inGamePlayer  球手及球杆
//     * @param playStage     阶段
//     * @return AI球手打这种球的偏好程度, 范围[0,1]
//     */
//    public double priceOf(double[] spins, double selectedPower,
//                          InGamePlayer inGamePlayer, GamePlayStage playStage) {
//        double maxSpin = 2.972;  // 0.81 * 1.2 + 0.8 * 2.5
//        double spinTotal = Math.abs(spins[0]) * 1.2 + Math.abs(spins[1]) * 2.5;
//        double powerMax = inGamePlayer.getPlayerPerson().getControllablePowerPercentage();
//        double comfortableLow = 105.0 - inGamePlayer.getPlayerPerson().getPowerControl();
//        double comfortableHigh;
//        double spinLimit;
//        if (playStage == GamePlayStage.NO_PRESSURE) {
//            comfortableHigh = powerMax * (0.5 + likeShow / 200);
//            spinLimit = maxSpin * likeShow * 1.8 / 100;
//        } else {
//            comfortableHigh = powerMax * (0.4 + likeShow / 250);
//            if (playStage == GamePlayStage.THIS_BALL_WIN) {
//                spinLimit = maxSpin * likeShow * 0.6 / 100;
//            } else {
//                spinLimit = maxSpin * likeShow * 1.2 / 100;
//            }
//        }
//        
//        double powerPrice;
//        if (selectedPower < comfortableLow) {
//            powerPrice = selectedPower / comfortableLow;
//        } else if (selectedPower > comfortableHigh) {
//            // range (0.5,1]
//            powerPrice = (2.0 - (selectedPower - comfortableHigh) / (powerMax - comfortableHigh)) / 2;
//        } else {
//            powerPrice = 1.0;
//        }
//        
//        double spinPrice;
//        if (spinTotal <= spinLimit) {
//            spinPrice = 1.0;
//        } else {
//            // range (0.5,1]
//            spinPrice = (2.0 - (spinTotal - spinLimit) / (maxSpin - spinLimit)) / 2;
//        }
//        return powerPrice * spinPrice;
//    }
    
    public static AiPlayStyle fromJson(JSONObject aiObject) {
        double[] likePwrRng;
        if (aiObject.has("likePowerRange")) {
            JSONArray array = aiObject.getJSONArray("likePowerRange");
            likePwrRng = new double[2];
            likePwrRng[0] = array.getDouble(0);
            likePwrRng[1] = array.getDouble(1);
        } else {
            likePwrRng = new double[]{15, 60};
        }
        double doubleAbi = 75;
        if (aiObject.has("doubleAbility")) {
            doubleAbi = aiObject.getDouble("doubleAbility");
        }
        
        return new AiPlayStyle(
                aiObject.getDouble("precision"),
                aiObject.getDouble("stable"),
                aiObject.getDouble("position"),
                aiObject.getDouble("defense"),
                aiObject.getDouble("attackPri"),
                aiObject.getDouble("likeShow"),
                likePwrRng,
                aiObject.optDouble("likeSide", 75),
                doubleAbi,
                aiObject.getString("snookerBreak"),
                aiObject.getBoolean("cebSideBreak"),
                aiObject.getInt("withdrawAfter")
        );
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();

        JSONArray likePwrRng = new JSONArray();
        likePwrRng.put(likePowerRange[0]);
        likePwrRng.put(likePowerRange[1]);

        obj.put("precision", precision);
        obj.put("stable", stability);
        obj.put("position", position);
        obj.put("defense", defense);
        obj.put("attackPri", attackPrivilege);
        obj.put("likeShow", likeShow);
        obj.put("likePowerRange", likePwrRng);
        obj.put("likeSide", likeSide);
        obj.put("doubleAbility", doubleAbility);
        obj.put("snookerBreak", snookerBreakMethod.name());
        obj.put("cebSideBreak", cebSideBreak);
        obj.put("withdrawAfter", snookerWithdrawLimit);

        return obj;
    }
    
    public enum SnookerBreakMethod {
        LEFT, RIGHT, BACK
    }
}
