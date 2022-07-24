package trashsoftware.trashSnooker.core.ai;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.GamePlayStage;
import trashsoftware.trashSnooker.core.InGamePlayer;

public class AiPlayStyle {
    public static final AiPlayStyle DEFAULT =
            new AiPlayStyle(100.0, 100.0, 100.0, 100.0, 80.0, 50);

    public final double precision;
    public final double stability;  // 准度稳定性
    public final double position;  // 走位能力
    public final double defense;
    public final double attackPrivilege;  // 进攻偏好
    public final double likeShow;  // 偏好大力及杆法

    public AiPlayStyle(double precision, double stability, double position, double defense,
                       double attackPrivilege, double likeShow) {
        this.precision = precision;
        this.stability = stability;
        this.position = position;
        this.defense = defense;
        this.attackPrivilege = attackPrivilege;
        this.likeShow = likeShow;
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
        double maxSpin = 1.61;  // 0.81 + 0.8
        double spinTotal = Math.abs(spins[0]) + Math.abs(spins[1]);
        double powerMax = inGamePlayer.getPlayerPerson().getControllablePowerPercentage();
        double comfortableLow = 110.0 - inGamePlayer.getPlayerPerson().getPowerControl();
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

        return obj;
    }
}
