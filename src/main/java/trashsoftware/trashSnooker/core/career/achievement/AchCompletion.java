package trashsoftware.trashSnooker.core.career.achievement;

import org.json.JSONObject;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Util;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class AchCompletion {
    //    private Date firstCompletion;
    public final Achievement achievement;
    private final SortedMap<Integer, Date> levelFirstCompletions = new TreeMap<>();  // level index
    private int times;

//    AchCompletion(Achievement achievement, Date firstCompletion, int times) {
//        this.achievement = achievement;
//        this.firstCompletion = firstCompletion;
//        this.times = times;
//    }

    AchCompletion(Achievement achievement, int times) {
//        this(achievement, null, times);
        this.achievement = achievement;
//        this.firstCompletion = firstCompletion;
        this.times = times;
    }

    AchCompletion(Achievement achievement) {
        this(achievement, 0);
    }

    public static AchCompletion fromJson(Achievement achievement, JSONObject json) {
        AchCompletion ac = new AchCompletion(achievement);
        ac.times = json.getInt("completions");
        
        if (json.has("date")) {
            // 兼容旧版
            Date fc;
            try {
                fc = Util.TIME_FORMAT_SEC.parse(json.getString("date"));
            } catch (ParseException e) {
                fc = new Date();
                EventLogger.error(e);
            }
            int canCompleteLevels = achievement.getNCompleted(ac);
            for (int i = 0; i < canCompleteLevels; i++) {
                ac.levelFirstCompletions.put(i, new Date(fc.getTime()));
            }
        }
        
        if (json.has("levelDates")) {
            JSONObject levelDates = json.getJSONObject("levelDates");
            for (String key : levelDates.keySet()) {
                String val = levelDates.getString(key);
                Date date;
                int levelIndex;
                try {
                    levelIndex = Integer.parseInt(key);
                    date = Util.TIME_FORMAT_SEC.parse(val);
                } catch (ParseException | NumberFormatException e) {
                    EventLogger.error(e);
                    continue;
                }
                ac.levelFirstCompletions.put(levelIndex, date);
            }
        }
        
        return ac;
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();
//        if (firstCompletion != null) {
//            object.put("date", Util.TIME_FORMAT_SEC.format(firstCompletion));
//        }
        JSONObject comp = new JSONObject();
        for (Map.Entry<Integer, Date> levelCmp : levelFirstCompletions.entrySet()) {
            comp.put(String.valueOf(levelCmp.getKey()), Util.TIME_FORMAT_SEC.format(levelCmp.getValue()));
        }
        object.put("levelDates", comp);
        object.put("completions", times);
        return object;
    }

//    public void setFirstCompletion(Date firstCompletion) {
//        this.firstCompletion = firstCompletion;
//    }

    /**
     * @return 返回是不是刚好就是这一下把它加够了
     */
    public boolean addOneTime() {
        int pastLevelIndex = achievement.getCompletedLevelIndex(this);
        times++;
        int curLevelIndex = achievement.getCompletedLevelIndex(this);
        if (curLevelIndex != pastLevelIndex) {
            levelFirstCompletions.put(curLevelIndex, new Date());
            return true;
        }
        return false;
    }

    /**
     * 针对recordLike的成就。例如：单杆最高
     */
    public boolean setNewRecord(int newRecord) {
        int pastLevelIndex = achievement.getCompletedLevelIndex(this);
        if (newRecord > this.times) {
            this.times = newRecord;
        }
        int curLevelIndex = achievement.getCompletedLevelIndex(this);
        if (curLevelIndex != pastLevelIndex) {
            for (int i = pastLevelIndex + 1; i <= curLevelIndex; i++) {
                levelFirstCompletions.put(i, new Date());
            }
            return true;
        }
        return false;
    }

    public int getNCompleted() {
        return levelFirstCompletions.size();
    }

    public String getDescription() {
        if (achievement.isFullyComplete(this)) {
            return getDescriptionOfCompleted();
        } else {
            return getDescriptionOfUncompleted();
        }
    }

    public String getDescriptionOfCompleted() {
        int nCompleted = getNCompleted();
        if (nCompleted == 0) {
            System.err.println("Achievement " + achievement + " has no completed record");
            return achievement.getDescriptionOfLevel(nCompleted);
        } else {
            return achievement.getDescriptionOfLevel(nCompleted - 1);
        }
    }

    public String getDescriptionOfUncompleted() {
        int nCompleted = getNCompleted();
        if (nCompleted == achievement.getNLevels()) {
            System.err.println("Achievement " + achievement + " is all completed");
            return achievement.getDescriptionOfLevel(nCompleted - 1);
        } else {
            return achievement.getDescriptionOfLevel(nCompleted);
        }
    }

    public Date getFirstCompletion() {
        if (levelFirstCompletions.isEmpty()) return null;
        return levelFirstCompletions.get(levelFirstCompletions.lastKey());
    }

    public int getTimes() {
        return times;
    }

    public Achievement getAchievement() {
        return achievement;
    }
}
