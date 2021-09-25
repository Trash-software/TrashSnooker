package trashsoftware.trashSnooker.core;

import org.json.JSONObject;
import trashsoftware.trashSnooker.util.Recorder;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PersonRecord {

    private final String playerName;
    private final Map<GameType, Map<String, Integer>> intRecords = new HashMap<>();

    // (wins, ties, losses) for each opponent
    private final Map<GameType, Map<String, int[]>> opponentsRecords = new HashMap<>();

    PersonRecord(String playerName) {
        this.playerName = playerName;
    }

    public static PersonRecord loadRecord(String playerName) {
        JSONObject root = Recorder.loadFromDisk(
                Recorder.RECORDS_DIRECTORY + File.separator + playerName + ".json");
        PersonRecord record = new PersonRecord(playerName);
        try {
            for (String gameTypeStr : root.keySet()) {
                GameType gameType = GameType.valueOf(gameTypeStr);
                Map<String, Integer> typeRecords = new HashMap<>();
                Map<String, int[]> oppoRecords = new HashMap<>();
                JSONObject object = root.getJSONObject(gameTypeStr);
                for (String key : object.keySet()) {
                    if ("opponents".equals(key)) {
                        JSONObject value = object.getJSONObject(key);
                        for (String oppo : value.keySet()) {
                            JSONObject matchRec = value.getJSONObject(oppo);
                            int[] mr = new int[]{
                                    matchRec.getInt("wins"),
                                    matchRec.getInt("losses")
                            };
                            oppoRecords.put(oppo, mr);
                        }
                    } else {
                        int value = object.getInt(key);
                        typeRecords.put(key, value);
                    }
                }
                record.intRecords.put(gameType, typeRecords);
                record.opponentsRecords.put(gameType, oppoRecords);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return record;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void potAttempt(PotAttempt attempt, boolean success) {
        boolean longPot = attempt.isLongPot();
        Map<String, Integer> typeMap = intRecords.get(attempt.getGameType());
        if (typeMap == null) {
            typeMap = createTypeMap(attempt.getGameType());
            intRecords.put(attempt.getGameType(), typeMap);
        }
        typeMap.put("potAttempts", typeMap.get("potAttempts") + 1);
        if (success) typeMap.put("potSuccesses", typeMap.get("potSuccesses") + 1);

        if (longPot) {
            typeMap.put("longPotAttempts", typeMap.get("longPotAttempts") + 1);
            if (success) typeMap.put("longPotSuccesses", typeMap.get("longPotSuccesses") + 1);
        }
    }

    public void updateBreakScore(GameType gameType, int breakScore) {
        Map<String, Integer> typeMap = intRecords.get(gameType);
        if (typeMap == null) {
            typeMap = createTypeMap(gameType);
            intRecords.put(gameType, typeMap);
        }
        if (gameType.scoredGame) {
            if (breakScore > typeMap.get("highestBreak")) {
                typeMap.put("highestBreak", breakScore);
            }

            if (breakScore >= 100) {
                typeMap.put("100+breaks", typeMap.get("100+breaks") + 1);
            }
            if (breakScore >= 50) {
                typeMap.put("50+breaks", typeMap.get("50+breaks") + 1);
            }
        }
    }

    public void wonAgainstOpponent(GameType gameType, String opponentName) {
        Map<String, int[]> oppo = opponentsRecords.computeIfAbsent(gameType, k -> new HashMap<>());
        int[] winLoss = oppo.get(opponentName);
        if (winLoss == null) {
            winLoss = new int[]{1, 0};
            oppo.put(opponentName, winLoss);
        } else {
            winLoss[0]++;
        }
    }

    public void lostAgainstOpponent(GameType gameType, String opponentName) {
        Map<String, int[]> oppo = opponentsRecords.computeIfAbsent(gameType, k -> new HashMap<>());
        int[] winLoss = oppo.get(opponentName);
        if (winLoss == null) {
            winLoss = new int[]{0, 1};
            oppo.put(opponentName, winLoss);
        } else {
            winLoss[1]++;
        }
    }

    public void writeToFile() {
        JSONObject root = new JSONObject();
        for (Map.Entry<GameType, Map<String, Integer>> entry : intRecords.entrySet()) {
            JSONObject object = new JSONObject();
            for (Map.Entry<String, Integer> item : entry.getValue().entrySet()) {
                object.put(item.getKey(), item.getValue());
            }
            Map<String, int[]> oppoRecords = opponentsRecords.get(entry.getKey());
            JSONObject opponents = new JSONObject();
            for (Map.Entry<String, int[]> oppoEntry : oppoRecords.entrySet()) {
                JSONObject oppo = new JSONObject();
                oppo.put("wins", oppoEntry.getValue()[0]);
                oppo.put("losses", oppoEntry.getValue()[1]);
                opponents.put(oppoEntry.getKey(), oppo);
            }
            object.put("opponents", opponents);
            root.put(entry.getKey().name(), object);
        }
        Recorder.saveToDisk(root,
                Recorder.RECORDS_DIRECTORY + File.separator + playerName + ".json");
    }

    private Map<String, Integer> createTypeMap(GameType gameType) {
        Map<String, Integer> map = new HashMap<>();
        map.put("potAttempts", 0);
        map.put("potSuccesses", 0);
        map.put("longPotAttempts", 0);
        map.put("longPotSuccesses", 0);

        if (gameType.scoredGame) {
            map.put("highestBreak", 0);
            map.put("50+breaks", 0);
            map.put("100+breaks", 0);
            map.put("147", 0);
        }
        return map;
    }
}
