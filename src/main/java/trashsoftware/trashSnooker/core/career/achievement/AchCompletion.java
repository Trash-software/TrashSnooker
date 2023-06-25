package trashsoftware.trashSnooker.core.career.achievement;

import org.json.JSONObject;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Util;

import java.text.ParseException;
import java.util.Date;

public class AchCompletion {
    private Date firstCompletion;
    private int times;

    AchCompletion(Date firstCompletion, int times) {
        this.firstCompletion = firstCompletion;
        this.times = times;
    }

    AchCompletion(int times) {
        this(null, times);
    }

    AchCompletion() {
        this(1);
    }

    public static AchCompletion fromJson(JSONObject json) {
        Date fc;
        if (json.has("date")) {
            try {
                fc = Util.TIME_FORMAT_SEC.parse(json.getString("date"));
            } catch (ParseException e) {
                fc = new Date();
                EventLogger.error(e);
            }
        } else {
            fc = null;
        }
        AchCompletion ac = new AchCompletion();
        ac.setFirstCompletion(fc);
        ac.times = json.getInt("completions");
        return ac;
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        if (firstCompletion != null) {
            object.put("date", Util.TIME_FORMAT_SEC.format(firstCompletion));
        }
        object.put("completions", times);
        return object;
    }

    public void setFirstCompletion(Date firstCompletion) {
        this.firstCompletion = firstCompletion;
    }

    /**
     * @return 返回是不是刚好就是这一下把它加够了
     */
    public boolean addOneTime(Achievement achievement) {
        boolean wasComplete = achievement.isComplete(this);
        times++;
        boolean nc = !wasComplete && achievement.isComplete(this);
        if (nc) {
            firstCompletion = new Date();
        }
        return nc;
    }

    /**
     * 针对recordLike的成就。例如：单杆最高
     */
    public boolean setNewRecord(Achievement achievement, int newRecord) {
        boolean wasComplete = achievement.isComplete(this);
        if (newRecord > this.times) {
            this.times = newRecord;
        }

        boolean nc = !wasComplete && achievement.isComplete(this);
        if (nc) {
            firstCompletion = new Date();
        }
        return nc;
    }

    public Date getFirstCompletion() {
        return firstCompletion;
    }

    public int getTimes() {
        return times;
    }
}
