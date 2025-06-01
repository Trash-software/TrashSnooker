package trashsoftware.trashSnooker.core.career.achievement;

import javafx.scene.image.Image;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.person.PlayerPerson;
import trashsoftware.trashSnooker.res.ResourcesLoader;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Util;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

public class AchCompletion {
    public final Achievement achievement;
    protected final SortedMap<Integer, Date> levelFirstCompletions = new TreeMap<>();  // level index
    /**
     * 跟上面这个map结合起来
     */
    protected final SortedMap<Integer, AwardStatus> levelAwardReceived = new TreeMap<>();
    protected int times;

    AchCompletion(Achievement achievement, int times) {
        this.achievement = achievement;
        this.times = times;
        
        initLevelAwards();
    }

    AchCompletion(Achievement achievement) {
        this(achievement, 0);
    }

    public static AchCompletion fromJson(Achievement achievement, JSONObject json) {
        AchCompletion ac = new AchCompletion(achievement);
        fillFromJson(ac, achievement, json);
        return ac;
    }

    protected static void fillFromJson(AchCompletion ac, Achievement achievement, JSONObject json) {
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

        if (json.has("levelAwardReceived")) {
            JSONObject lar = json.getJSONObject("levelAwardReceived");
            for (String key : lar.keySet()) {
                String val = lar.getString(key);
                AwardStatus status;
                int levelIndex;
                try {
                    levelIndex = Integer.parseInt(key);
                    status = AwardStatus.valueOf(val);
                } catch (IllegalArgumentException e) {
                    EventLogger.error(e);
                    continue;
                }
                ac.levelAwardReceived.put(levelIndex, status);
            }
            for (int lvi = 0; lvi < ac.achievement.getLevels().length; lvi++) {
                // 查找漏网之鱼
                if (!ac.levelAwardReceived.containsKey(lvi)) {
                    AwardStatus status;
                    if (ac.levelFirstCompletions.containsKey(lvi)) {
                        status = AwardStatus.NOT_RECEIVED;
                    } else {
                        status = AwardStatus.NOT_COMPLETED;
                    }
                    ac.levelAwardReceived.put(lvi, status);
                }
            }
        }
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        JSONObject comp = new JSONObject();
        for (Map.Entry<Integer, Date> levelCmp : levelFirstCompletions.entrySet()) {
            comp.put(String.valueOf(levelCmp.getKey()), Util.TIME_FORMAT_SEC.format(levelCmp.getValue()));
        }
        object.put("levelDates", comp);
        JSONObject awardRec = new JSONObject();
        for (Map.Entry<Integer, AwardStatus> levelAwd : levelAwardReceived.entrySet()) {
            awardRec.put(String.valueOf(levelAwd.getKey()), levelAwd.getValue().name());
        }
        object.put("levelAwardReceived", awardRec);
        object.put("completions", times);
        return object;
    }
    
    private void initLevelAwards() {
        for (int lvi = 0; lvi < achievement.getLevels().length; lvi++) {
            // 初始化
            levelAwardReceived.put(lvi, AwardStatus.NOT_COMPLETED);
        }
    }
    
    private void updateLevelAwards() {
        for (int lvi = 0; lvi < achievement.getLevels().length; lvi++) {
            AwardStatus current = levelAwardReceived.get(lvi);
            if (current == AwardStatus.NOT_COMPLETED) {
                if (levelFirstCompletions.containsKey(lvi)) {
                    levelAwardReceived.put(lvi, AwardStatus.NOT_RECEIVED);
                }
            }
        }
    }

    /**
     * @return 返回是不是刚好就是这一下把它加够了
     */
    public boolean addOneTime() {
        int pastLevelIndex = achievement.getCompletedLevelIndex(this);
        times++;
        int curLevelIndex = achievement.getCompletedLevelIndex(this);
        if (curLevelIndex != pastLevelIndex) {
            levelFirstCompletions.put(curLevelIndex, new Date());
            updateLevelAwards();
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
            updateLevelAwards();
            return true;
        }
        return false;
    }

    public int getNCompleted() {
        return levelFirstCompletions.size();
    }

    public Image getImage() {
        return ResourcesLoader.getInstance().getAwardImgByLevel(getNCompleted(), achievement.getNLevels());
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
            EventLogger.error("Achievement " + achievement + " has no completed record");
            return achievement.getDescriptionOfLevel(nCompleted);
        } else {
            return achievement.getDescriptionOfLevel(nCompleted - 1);
        }
    }

    public String getDescriptionOfUncompleted() {
        int nCompleted = getNCompleted();
        if (nCompleted == achievement.getNLevels()) {
            EventLogger.error("Achievement " + achievement + " is all completed");
            return achievement.getDescriptionOfLevel(nCompleted - 1);
        } else {
            return achievement.getDescriptionOfLevel(nCompleted);
        }
    }

    public Date getFirstCompletion() {
        if (levelFirstCompletions.isEmpty()) return null;
        return levelFirstCompletions.get(levelFirstCompletions.lastKey());
    }

    /**
     * @return 等级index, 钱
     */
    public SortedMap<Integer, Integer> getUnreceivedAwards() {
        SortedMap<Integer, Integer> result = new TreeMap<>();
        for (Map.Entry<Integer, AwardStatus> entry : levelAwardReceived.entrySet()) {
            if (entry.getValue() == AwardStatus.NOT_RECEIVED) {
                result.put(entry.getKey(), achievement.getMoneyByCompLevel(entry.getKey()));
            }
        }
        return result;
    }
    
    public boolean receiveAward(int levelIndex) {
        AwardStatus as = levelAwardReceived.get(levelIndex);
        if (as == AwardStatus.NOT_RECEIVED) {
            levelAwardReceived.put(levelIndex, AwardStatus.RECEIVED);
            AchManager.getInstance().saveToDisk();
            return true;
        } else {
            return false;
        }
    }

    public int getTimes() {
        return times;
    }

    public Achievement getAchievement() {
        return achievement;
    }

    public String getTitle() {
        return achievement.title();
    }

    public static class Sub extends AchCompletion {

        private String key;

        Sub(Achievement achievement, String key, int times) {
            super(achievement, times);

            this.key = key;
        }

        Sub(Achievement achievement) {
            this(achievement, null, 0);
        }

        @Override
        public JSONObject toJson() {
            JSONObject object = super.toJson();
            object.put("key", key);
            return object;
        }

        public static Sub fromJson(Achievement achievement, JSONObject json) {
            Sub sub = new Sub(achievement);
            fillFromJson(sub, achievement, json);
            sub.key = json.getString("key");
            return sub;
        }

        @Override
        public String getTitle() {
            String fmt = super.getTitle();
            if (achievement == Achievement.UNIQUE_DEFEAT) {
                PlayerPerson person = DataLoader.getInstance().getPlayerPerson(key);
                return String.format(fmt, person.getName());
            } else {
                return String.format(fmt, key);
            }
        }

        @Override
        public int getNCompleted() {
            return times > 0 ? 1 : 0;
        }
    }

    public static class Collective extends AchCompletion {

        //        private final String uniqueId;
        private final Map<String, Sub> collection = new HashMap<>();

        Collective(Achievement achievement) {
            super(achievement, 0);
        }

        public static Collective fromJson(Achievement achievement, JSONObject jsonObject) {
            Collective collective = new Collective(achievement);
            if (jsonObject.has("collection")) {
                JSONObject col = jsonObject.getJSONObject("collection");
                for (String key : col.keySet()) {
                    JSONObject item = col.getJSONObject(key);
                    Sub comp = Sub.fromJson(achievement, item);
                    collective.collection.put(key, comp);
                }
            }
            return collective;
        }

        @Override
        public JSONObject toJson() {
            JSONObject container = new JSONObject();
            JSONObject json = new JSONObject();
            for (var entry : collection.entrySet()) {
                json.put(entry.getKey(), entry.getValue().toJson());
            }
            container.put("collection", json);
            return container;
        }

        @Override
        public int getNCompleted() {
            return collection
                    .values()
                    .stream()
                    .map(Sub::getNCompleted)
                    .reduce(0, Integer::sum);
        }

        public boolean setIndividual(String key, int newValue) {
            Sub indComp = collection.get(key);
            boolean wasCompleted = indComp != null && indComp.getNCompleted() > 0;  // todo: 目前没有多次检测

            if (indComp == null) {
                indComp = new Sub(achievement, key, newValue);
                collection.put(key, indComp);
            }
            indComp.times = newValue;  // 绕过newRecord那一套

            boolean nc = !wasCompleted && indComp.getNCompleted() > 0;
            if (nc) {
                indComp.levelFirstCompletions.put(0, new Date());
            }
            return nc;
        }

        public Sub getIndividual(String key) {
            return collection.get(key);
        }

        public List<String> getKeys() {
            List<Sub> people = new ArrayList<>(collection.values());
            // 最晚战胜的在最前
            people.sort((a, b) -> -a.getFirstCompletion().compareTo(b.getFirstCompletion()));
            return people.stream().map(s -> s.key).collect(Collectors.toList());
        }
    }
    
    public enum AwardStatus {
        RECEIVED,  // 收了
        NOT_RECEIVED,  // 达成了，但没收
        NOT_COMPLETED  // 没达成
    }
}
