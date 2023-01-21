package trashsoftware.trashSnooker.core.ai;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.GamePlayStage;
import trashsoftware.trashSnooker.core.InGamePlayer;

import java.util.Locale;

public class AiPlayStyle {
    public static final AiPlayStyle DEFAULT =
            new AiPlayStyle(100.0, 100.0, 100.0, 100.0, 
                    80.0, 50,
                    "right", false, 2);

    public final double precision;
    public final double stability;  // 准度稳定性
    public final double position;  // 走位能力
    public final double defense;
    public final double attackPrivilege;  // 进攻偏好
    public final double likeShow;  // 偏好大力及杆法
    public final SnookerBreakMethod snookerBreakMethod;
    public final boolean cebSideBreak;  // Chinese eight balls 侧面冲球
    public final int snookerWithdrawLimit;  // 需要做多少杆斯诺克才认输

    public AiPlayStyle(double precision, double stability, double position, double defense,
                       double attackPrivilege, double likeShow,
                       String snookerBreakMethod, boolean cebSideBreak,
                       int snookerWithdrawLimit) {
        this.precision = precision;
        this.stability = stability;
        this.position = position;
        this.defense = defense;
        this.attackPrivilege = attackPrivilege == 100 ? Double.POSITIVE_INFINITY : attackPrivilege;  // 权重100的选手只要有下就会进攻
        this.likeShow = likeShow;
        this.snookerBreakMethod = SnookerBreakMethod.valueOf(snookerBreakMethod.toUpperCase(Locale.ROOT));
        this.cebSideBreak = cebSideBreak;
        this.snookerWithdrawLimit = snookerWithdrawLimit;
    }

    /**
     * @param spins         {高低杆, 左右塞}, 范围都是(-1,1)
     * @param selectedPower 选择的力度, 0-100
     * @param inGamePlayer  球手及球杆
     * @param playStage     阶段
     * @return AI球手打这种球的偏好程度, 范围[0,1]
     */
    public double priceOf(double[] spins, double selectedPower,
                          InGamePlayer inGamePlayer, GamePlayStage playStage) {
        double maxSpin = 2.972;  // 0.81 * 1.2 + 0.8 * 2.5
        double spinTotal = Math.abs(spins[0]) * 1.2 + Math.abs(spins[1]) * 2.5;
        double powerMax = inGamePlayer.getPlayerPerson().getControllablePowerPercentage();
        double comfortableLow = 105.0 - inGamePlayer.getPlayerPerson().getPowerControl();
        double comfortableHigh;
        double spinLimit;
        if (playStage == GamePlayStage.NO_PRESSURE) {
            comfortableHigh = powerMax * (0.5 + likeShow / 200);
            spinLimit = maxSpin * likeShow * 1.8 / 100;
        } else {
            comfortableHigh = powerMax * (0.4 + likeShow / 250);
            if (playStage == GamePlayStage.THIS_BALL_WIN) {
                spinLimit = maxSpin * likeShow * 0.6 / 100;
            } else {
                spinLimit = maxSpin * likeShow * 1.2 / 100;
            }
        }
        
        double powerPrice;
        if (selectedPower < comfortableLow) {
            powerPrice = selectedPower / comfortableLow;
        } else if (selectedPower > comfortableHigh) {
            // range (0.5,1]
            powerPrice = (2.0 - (selectedPower - comfortableHigh) / (powerMax - comfortableHigh)) / 2;
        } else {
            powerPrice = 1.0;
        }
        
        double spinPrice;
        if (spinTotal <= spinLimit) {
            spinPrice = 1.0;
        } else {
            // range (0.5,1]
            spinPrice = (2.0 - (spinTotal - spinLimit) / (maxSpin - spinLimit)) / 2;
        }
        return powerPrice * spinPrice;
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();

        obj.put("precision", precision);
        obj.put("position", position);
        obj.put("defense", defense);
        obj.put("attackPri", attackPrivilege);
        obj.put("snookerBreak", snookerBreakMethod.name());
        obj.put("cebSideBreak", cebSideBreak);

        return obj;
    }
    
    public enum SnookerBreakMethod {
        LEFT, RIGHT, BACK
    }
}
