package trashsoftware.trashSnooker.core.career;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;
import trashsoftware.trashSnooker.core.career.challenge.ChallengeManager;
import trashsoftware.trashSnooker.core.career.challenge.ChallengeSet;
import trashsoftware.trashSnooker.core.career.championship.MatchTreeNode;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.CueTip;
import trashsoftware.trashSnooker.util.JsonUtil;
import trashsoftware.trashSnooker.util.Util;

import java.text.ParseException;
import java.util.*;

public abstract class Invoice {

    public final String type;
    public final Date realTimestamp;
    public final Calendar inGameDate;
    public final int moneyBefore;
    public int moneyAfter;

    protected Invoice(String type, Date realTimestamp, Calendar inGameDate,
                      int moneyBefore, int moneyAfter) {
        this.type = type;
        this.realTimestamp = realTimestamp;
        this.inGameDate = inGameDate;
        this.moneyBefore = moneyBefore;
        this.moneyAfter = moneyAfter;
    }

    public int getMoneyBefore() {
        return moneyBefore;
    }

    public int getMoneyAfter() {
        return moneyAfter;
    }

    public int getMoneyChange() {
        return moneyAfter - moneyBefore;
    }

    public String getShownType(ResourceBundle strings) {
        String upper = "INVOICE_" + Util.toAllCapsUnderscoreCase(type);
        String key = Util.toLowerCamelCase(upper);
        if (strings.containsKey(key)) return strings.getString(key);
        else return type;
    }
    
    public String getItemDes(ResourceBundle strings, HumanCareer humanCareer) {
        return "";
    }

    public static Invoice fromJson(JSONObject json) throws ParseException, JSONException {
        String type = json.getString("type");
        Date realTimestamp = Util.TIME_FORMAT_SEC.parse(json.getString("timestamp"));
        Calendar inGameDate = CareerManager.stringToCalendar(json.getString("inGameDate"));
        int moneyBefore = json.getInt("moneyBefore");
        int moneyAfter = json.getInt("moneyAfter");
        
        return switch (type) {
            case "championshipEarn" -> new ChampionshipEarn(
                    realTimestamp,
                    inGameDate,
                    moneyBefore,
                    moneyAfter,
                    json.getString("match"),
                    json.getInt("year"),
                    PlayEarn.loadItems(json.getJSONObject("items"))
            );
            case "challengeEarn" -> new ChallengeEarn(
                    realTimestamp,
                    inGameDate,
                    moneyBefore,
                    moneyAfter,
                    json.getString("match"),
                    PlayEarn.loadItems(json.getJSONObject("items"))
            );
            case "purchase" -> new Purchase(
                    realTimestamp,
                    inGameDate,
                    moneyBefore,
                    moneyAfter,
                    json.getString("itemType"),
                    json.getString("item"),
                    json.getInt("moneyCost")
            );
            case "upgrade" -> new Upgrade(
                    realTimestamp,
                    inGameDate,
                    moneyBefore,
                    moneyAfter,
                    json.getInt("perkUsed"),
                    json.optInt("freePerkUsed", 0),
                    Upgrade.loadAbility(json.getJSONObject("ability")),
                    json.getInt("moneyCost")
            );
            case "achievementAward" -> new AchievementAward(
                    realTimestamp,
                    inGameDate,
                    moneyBefore,
                    moneyAfter,
                    json.getString("item"),
                    json.getInt("level"),
                    json.getInt("moneyEarn")
            );
            case "fees" -> new Fees(
                    realTimestamp,
                    inGameDate,
                    moneyBefore,
                    moneyAfter,
                    Fees.loadFeeItems(json.getJSONArray("items"))
            );
            case "invitation" -> new Invitation(
                    realTimestamp,
                    inGameDate,
                    moneyBefore,
                    moneyAfter,
                    json.getString("match"),
                    json.getInt("item"),
                    json.getInt("moneyEarn")
            );
            case "participation" -> new Participation(
                    realTimestamp,
                    inGameDate,
                    moneyBefore,
                    moneyAfter,
                    json.getString("match"),
                    Participation.loadItemCosts(json.getJSONArray("items"))
            );
            default -> throw new JSONException("Invalid invoice type '" + type + "'");
        };
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        String timestamp = Util.TIME_FORMAT_SEC.format(realTimestamp);
        json.put("timestamp", timestamp);
        String inGameDate = CareerManager.calendarToString(this.inGameDate);
        json.put("inGameDate", inGameDate);
        json.put("type", type);
        json.put("moneyBefore", moneyBefore);
        json.put("moneyAfter", moneyAfter);

        fillJson(json);
        return json;
    }

    public void setMoneyAfter(int moneyAfter) {
        this.moneyAfter = moneyAfter;
    }
    
    public record TaxedIncome(int raw, int actual) {
    }

    protected abstract void fillJson(JSONObject json);

    protected abstract static class Earn extends Invoice {
        protected final int moneyEarn;

        protected Earn(String type, Date realTimestamp, Calendar inGameDate,
                       int moneyBefore, int moneyAfter, int moneyEarn) {
            super(type, realTimestamp, inGameDate, moneyBefore, moneyAfter);

            this.moneyEarn = moneyEarn;
        }

        @Override
        protected void fillJson(JSONObject json) {
            json.put("moneyEarn", moneyEarn);

            fillJson2(json);
        }

        protected abstract void fillJson2(JSONObject json);
    }

    protected abstract static class Cost extends Invoice {
        protected final int moneyCost;

        protected Cost(String type, Date realTimestamp, Calendar inGameDate,
                       int moneyBefore, int moneyAfter, int moneyCost) {
            super(type, realTimestamp, inGameDate, moneyBefore, moneyAfter);

            this.moneyCost = moneyCost;
        }

        @Override
        protected void fillJson(JSONObject json) {
            json.put("moneyCost", moneyCost);

            fillJson2(json);
        }

        protected abstract void fillJson2(JSONObject json);
    }

    protected abstract static class PlayEarn extends Invoice {

        public final String match;
        public final Map<String, TaxedIncome> items;

        protected PlayEarn(String type, Date realTimestamp, Calendar inGameDate,
                           int moneyBefore, int moneyAfter,
                           String match, Map<String, TaxedIncome> items) {
            super(type, realTimestamp, inGameDate, moneyBefore, moneyAfter);

            this.match = match;
            this.items = items;
        }
        
        protected static Map<String, TaxedIncome> loadItems(JSONObject itemsJson) {
            Map<String, TaxedIncome> res = new TreeMap<>();
            for (String itemKey : itemsJson.keySet()) {
                res.put(itemKey, JsonUtil.jsonToRecord(TaxedIncome.class, itemsJson.getJSONObject(itemKey)));
            }
            return res;
        }

        @Override
        protected void fillJson(JSONObject json) {
            json.put("match", match);
            JSONObject itemsJs = new JSONObject();
            for (Map.Entry<String, TaxedIncome> item : items.entrySet()) {
                itemsJs.put(item.getKey(), JsonUtil.recordToJson(item.getValue()));
            }
            json.put("items", itemsJs);

            fillJson2(json);
        }

        protected abstract void fillJson2(JSONObject json);
    }

    public static class ChampionshipEarn extends PlayEarn {

        public final int year;

        protected ChampionshipEarn(Date realTimestamp, Calendar inGameDate, int moneyBefore, int moneyAfter,
                                   String match, int year, Map<String, TaxedIncome> items) {
            super("championshipEarn", realTimestamp, inGameDate, moneyBefore, moneyAfter,
                    match, items);

            this.year = year;
        }

        @Override
        protected void fillJson2(JSONObject json) {
            json.put("year", year);
        }

        @Override
        public String getItemDes(ResourceBundle strings, HumanCareer humanCareer) {
            ChampionshipData data = CareerManager.getInstance().getChampDataManager()
                    .findDataById(match);
            return data.getName();
        }
    }

    public static class ChallengeEarn extends PlayEarn {

        protected ChallengeEarn(Date realTimestamp, Calendar inGameDate, int moneyBefore, int moneyAfter,
                                String match, Map<String, TaxedIncome> items) {
            super("challengeEarn", realTimestamp, inGameDate, moneyBefore, moneyAfter,
                    match, items);
        }

        @Override
        protected void fillJson2(JSONObject json) {
        }

        @Override
        public String getItemDes(ResourceBundle strings, HumanCareer humanCareer) {
            ChallengeSet cs = ChallengeManager.getInstance().getById(match);
            if (cs == null) return  "";
            return cs.getName();
        }
    }

//    public static class EarnSubItem {
//        protected final int raw;
//        protected final int actual;
//
//        EarnSubItem(int raw, int actual) {
//            this.raw = raw;
//            this.actual = actual;
//        }
//    }

    public static class Purchase extends Cost {

        protected final String item;
        protected final String itemType;

        protected Purchase(Date realTimestamp, Calendar inGameDate, int moneyBefore, int moneyAfter,
                           String itemType, String item, int moneyCost) {
            super("purchase", realTimestamp, inGameDate, moneyBefore, moneyAfter, moneyCost);

            this.itemType = itemType;
            this.item = item;
        }

        @Override
        protected void fillJson2(JSONObject json) {
            json.put("item", item);
            json.put("itemType", itemType);
        }

        @Override
        public String getItemDes(ResourceBundle strings, HumanCareer humanCareer) {
            if (itemType != null) {
                String itemStr = "";
                if ("cue".equals(itemType)) {
//                        typeStr = strings.getString("inventoryCues");
                    itemStr = humanCareer.getInventory().getCueByInstanceId(item).getName();
                } else if ("tip".equals(itemType)) {
//                        typeStr = strings.getString("inventoryTips");
                    itemStr = humanCareer.getInventory().getTipByInstanceId(item).getBrand().shownName();
                }
                return itemStr;
            } else {
                Cue cue;
                CueTip tip;
                if ((cue = humanCareer.getInventory().getCueByInstanceId(item)) != null) {
                    return cue.getName();
                } else if ((tip = humanCareer.getInventory().getTipByInstanceId(item)) != null) {
                    return tip.getBrand().shownName();
                } else {
                    return  "";
                }
            }
        }
    }

    public static class Upgrade extends Cost {

        protected final int perkUsed, freePerkUsed;
        protected final Map<String, double[]> ability;

        protected Upgrade(Date realTimestamp, Calendar inGameDate, int moneyBefore, int moneyAfter,
                          int perkUsed, int freePerkUsed, Map<String, double[]> ability, int moneyCost) {
            super("upgrade", realTimestamp, inGameDate, moneyBefore, moneyAfter, moneyCost);

            this.perkUsed = perkUsed;
            this.freePerkUsed = freePerkUsed;
            this.ability = ability;
        }
        
        protected static Map<String, double[]> loadAbility(JSONObject skillUpgrade) {
            Map<String, double[]> res = new TreeMap<>();
            for (String key : skillUpgrade.keySet()) {
                res.put(key, JsonUtil.jsonToDoubleArray(skillUpgrade.getJSONArray(key)));
            }
            return res;
        }

        @Override
        protected void fillJson2(JSONObject json) {
            JSONObject skillUpgrade = new JSONObject();
            for (Map.Entry<String, double[]> entry : ability.entrySet()) {
                skillUpgrade.put(entry.getKey(), JsonUtil.arrayToJson(entry.getValue()));
            }
            json.put("ability", skillUpgrade);
            json.put("perkUsed", perkUsed);
            json.put("freePerkUsed", freePerkUsed);
        }
    }

    public static class AchievementAward extends Earn {

        protected final int level;
        protected final String item;

        protected AchievementAward(Date realTimestamp, Calendar inGameDate, int moneyBefore, int moneyAfter,
                                   String item, int level, int moneyEarn) {
            super("achievementAward", realTimestamp, inGameDate, moneyBefore, moneyAfter, moneyEarn);

            this.level = level;
            this.item = item;
        }

        @Override
        protected void fillJson2(JSONObject json) {
            json.put("item", item);
            json.put("level", level);
        }

        @Override
        public String getItemDes(ResourceBundle strings, HumanCareer humanCareer) {
            Achievement achievement = Achievement.valueOf(item);
            return achievement.getDescriptionOfLevel(level);
        }
    }

    public static class Fees extends Invoice {

        protected final Map<String, Integer> items;

        protected Fees(Date realTimestamp, Calendar inGameDate, int moneyBefore, int moneyAfter,
                       Map<String, Integer> items) {
            super("fees", realTimestamp, inGameDate, moneyBefore, moneyAfter);

            this.items = items;
        }
        
        protected static Map<String, Integer> loadFeeItems(JSONArray subArray) {
            Map<String, Integer> res = new TreeMap<>();
            for (int i = 0; i< subArray.length(); i++) {
                JSONObject jo = subArray.getJSONObject(i);
                res.put(jo.getString("item"), jo.getInt("moneyCost"));
            }
            return res;
        }

        @Override
        protected void fillJson(JSONObject json) {
            JSONArray subArray = new JSONArray();
            for (Map.Entry<String, Integer> feeItem : items.entrySet()) {
                JSONObject sub = new JSONObject();
                sub.put("item", feeItem.getKey());
                sub.put("moneyCost", feeItem.getValue());
                subArray.put(sub);
            }
            json.put("items", subArray);
        }

        @Override
        public String getItemDes(ResourceBundle strings, HumanCareer humanCareer) {
            return strings.getString("fixedExpenditure");
        }

        public Map<String, Integer> getItems() {
            return items;
        }
    }

    public static class Invitation extends Earn {

        protected final String match;
        protected final int item;

        protected Invitation(Date realTimestamp, Calendar inGameDate, int moneyBefore, int moneyAfter,
                             String match, int item, int moneyEarn) {
            super("invitation", realTimestamp, inGameDate, moneyBefore, moneyAfter, moneyEarn);

            this.match = match;
            this.item = item;
        }

        @Override
        protected void fillJson2(JSONObject json) {
            json.put("match", match);
            json.put("item", item);
        }

        @Override
        public String getItemDes(ResourceBundle strings, HumanCareer humanCareer) {
            return MatchTreeNode.analyzeMatchId(match).data.getName();
        }
    }

    public static class Participation extends Invoice {

        public final String match;
        protected final Map<String, Integer> items;

        protected Participation(Date realTimestamp, Calendar inGameDate, int moneyBefore, int moneyAfter,
                                String match, Map<String, Integer> itemsCosts) {
            super("participation", realTimestamp, inGameDate, moneyBefore, moneyAfter);

            this.match = match;
            this.items = itemsCosts;
        }
        
        protected static Map<String, Integer> loadItemCosts(JSONArray subArray) {
            Map<String, Integer> res = new TreeMap<>();
            for (int i = 0; i< subArray.length(); i++) {
                JSONObject jo = subArray.getJSONObject(i);
                res.put(jo.getString("item"), jo.getInt("moneyCost"));
            }
            return res;
        }

        public Map<String, Integer> getItems() {
            return items;
        }

        @Override
        protected void fillJson(JSONObject json) {
            json.put("match", match);
            
            JSONArray subArray = new JSONArray();
            for (Map.Entry<String, Integer> costEntry : items.entrySet()) {
                JSONObject jo = new JSONObject();
                jo.put("item", costEntry.getKey());
                jo.put("moneyCost", costEntry.getValue());
                subArray.put(jo);
            }
            
            json.put("items", subArray);
        }

        @Override
        public String getItemDes(ResourceBundle strings, HumanCareer humanCareer) {
            return MatchTreeNode.analyzeMatchId(match).data.getName();
        }
    }

//    public enum Type {
//        CHAMPIONSHIP_EARN("championshipEarn"),
//        CHALLENGE_EARN("challengeEarn"),
//        PURCHASE("purchase");
//        
//        public final String typeName;
//        
//        Type(String typeName) {
//            this.typeName = typeName;
//        }
//    }
}
