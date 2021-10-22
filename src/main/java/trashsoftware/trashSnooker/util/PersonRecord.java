package trashsoftware.trashSnooker.util;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.GameType;
import trashsoftware.trashSnooker.core.Player;
import trashsoftware.trashSnooker.core.PotAttempt;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;
import trashsoftware.trashSnooker.core.snooker.SnookerPlayer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PersonRecord {

    public static boolean RECORD = true;

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

    public static File[] listRecordFiles() {
        File root = new File(Recorder.RECORDS_DIRECTORY);
        File[] recFiles = root.listFiles();
        return Objects.requireNonNullElse(recFiles, new File[0]);
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
        Map<String, Integer> typeMap = getIntRecordOfType(gameType);
        if (gameType.snookerLike) {
            if (breakScore > typeMap.get("highestBreak")) {
                typeMap.put("highestBreak", breakScore);
            }

            if (breakScore == 147) {
                incrementMap(typeMap, "147");
            }
            if (breakScore >= 100) {
                incrementMap(typeMap, "100+breaks");
            }
            if (breakScore >= 50) {
                incrementMap(typeMap, "50+breaks");
            }
        }
    }

    public void generalEndGame(GameType gameType, Player player) {
        if (player instanceof SnookerPlayer) {
            SnookerPlayer snookerPlayer = (SnookerPlayer) player;
            snookerPlayer.flushSinglePoles();
            for (Integer singlePole : snookerPlayer.getSinglePolesInThisGame()) {
                updateBreakScore(gameType, singlePole);
            }
        }
    }

    public void wonAgainstOpponent(GameType gameType, Player player, String opponentName) {
        Map<String, int[]> oppo = opponentsRecords.computeIfAbsent(gameType, k -> new HashMap<>());
        int[] winLoss = oppo.get(opponentName);
        if (winLoss == null) {
            winLoss = new int[]{1, 0};
            oppo.put(opponentName, winLoss);
        } else {
            winLoss[0]++;
        }

        if (player instanceof NumberedBallPlayer) {
            int playTimes = ((NumberedBallPlayer) player).getPlayTimes();
            boolean breaks = ((NumberedBallPlayer) player).isBreakingPlayer();
            Map<String, Integer> intMap = getIntRecordOfType(gameType);
            if (playTimes == 1) {
                if (breaks) {  // 炸清
                    incrementMap(intMap, "break-clear");
                } else {  // 接清
                    incrementMap(intMap, "continue-clear");
                }
            }
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
        if (!RECORD) return;
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

    public Map<GameType, Map<String, Integer>> getIntRecords() {
        return intRecords;
    }

    public Map<GameType, Map<String, int[]>> getOpponentsRecords() {
        return opponentsRecords;
    }

    @NotNull
    private Map<String, Integer> getIntRecordOfType(GameType gameType) {
        Map<String, Integer> intMap = intRecords.get(gameType);
        if (intMap == null) {
            intMap = createTypeMap(gameType);
            intRecords.put(gameType, intMap);
        }
        return intMap;
    }

    private static void incrementMap(Map<String, Integer> map, String key) {
        Integer val = map.get(key);
        map.put(key, val == null ? 1 : val + 1);
    }

    private Map<String, Integer> createTypeMap(GameType gameType) {
        Map<String, Integer> map = new HashMap<>();
        map.put("potAttempts", 0);
        map.put("potSuccesses", 0);
        map.put("longPotAttempts", 0);
        map.put("longPotSuccesses", 0);

        if (gameType.snookerLike) {
            map.put("highestBreak", 0);
            map.put("50+breaks", 0);
            map.put("100+breaks", 0);
            map.put("147", 0);
        } else if (gameType == GameType.CHINESE_EIGHT || gameType == GameType.SIDE_POCKET) {
            map.put("break-clear", 0);
            map.put("continue-clear", 0);
        }
        return map;
    }
}
