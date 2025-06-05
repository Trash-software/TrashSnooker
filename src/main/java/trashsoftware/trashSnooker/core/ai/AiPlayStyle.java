package trashsoftware.trashSnooker.core.ai;

import org.json.JSONArray;
import org.json.JSONObject;

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
