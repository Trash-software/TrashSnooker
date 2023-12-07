package trashsoftware.trashSnooker.core.career;

import org.json.JSONObject;
import trashsoftware.trashSnooker.util.Util;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Deprecated
public abstract class Invoice {
    
    public final Type type;
    public final Date realTimestamp; 
    public final Calendar inGameDate;
    public final int moneyBefore;
    protected int moneyAfter;
    
    protected Invoice(Type type, Date realTimestamp, Calendar inGameDate, int moneyBefore) {
        this.type = type;
        this.realTimestamp = realTimestamp;
        this.inGameDate = inGameDate;
        this.moneyBefore = moneyBefore;
    }
    
    public static Invoice fromJson(JSONObject json) {
        return null;
    }
    
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        String timestamp = Util.TIME_FORMAT_SEC.format(realTimestamp);
        json.put("timestamp", timestamp);
        String inGameDate = CareerManager.calendarToString(this.inGameDate);
        json.put("inGameDate", inGameDate);
        json.put("type", type.name());
        json.put("moneyBefore", moneyBefore);
        json.put("moneyAfter", moneyAfter);
        
        fillJson(json);
        return json;
    }
    
    public void setMoneyAfter(int moneyAfter) {
        this.moneyAfter = moneyAfter;
    }
    
    protected abstract void fillJson(JSONObject json);
    
    public static class ChampionshipEarn extends Invoice {

        protected ChampionshipEarn(Date realTimestamp, Calendar inGameDate, int moneyBefore) {
            super(Type.CHAMPIONSHIP_EARN, realTimestamp, inGameDate, moneyBefore);
        }

        @Override
        protected void fillJson(JSONObject json) {
            
        }
    }

    public static class ChallengeEarn extends Invoice {
        
        protected final String id;
//        protected final List<>

        protected ChallengeEarn(Date realTimestamp, Calendar inGameDate, int moneyBefore, String id) {
            super(Type.CHALLENGE_EARN, realTimestamp, inGameDate, moneyBefore);
            
            this.id = id;
        }

        @Override
        protected void fillJson(JSONObject json) {

        }
    }
    
    public enum Type {
        CHAMPIONSHIP_EARN,
        CHALLENGE_EARN,
    }
}
