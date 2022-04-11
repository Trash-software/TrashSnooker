package trashsoftware.trashSnooker.core.ai;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.PlayerPerson;

public class AiPlayStyle {
    public static final AiPlayStyle DEFAULT = new AiPlayStyle(100.0, 100.0, 100.0, 80.0);
    
    public final double precision;
    public final double position;  // 走位能力
    public final double defense;
    public final double attackPrivilege;  // 进攻偏好
    
    public AiPlayStyle(double precision, double position, double defense, double attackPrivilege) {
        this.precision = precision;
        this.position = position;
        this.defense = defense;
        this.attackPrivilege = attackPrivilege;
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
