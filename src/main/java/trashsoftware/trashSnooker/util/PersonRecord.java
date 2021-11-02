package trashsoftware.trashSnooker.util;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.GameType;
import trashsoftware.trashSnooker.core.Player;
import trashsoftware.trashSnooker.core.PotAttempt;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;
import trashsoftware.trashSnooker.core.snooker.SnookerPlayer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class PersonRecord {

    public static boolean RECORD = true;

    private final String playerName;
    private final Map<GameType, Map<String, Integer>> intRecords = new HashMap<>();

    // (wins, ties, losses) for each opponent
    private final Map<GameType, Map<String, Map<String, Object>>> opponentsRecords = new HashMap<>();

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
                Map<String, Map<String, Object>> oppoRecords = new HashMap<>();
                JSONObject object = root.getJSONObject(gameTypeStr);
                for (String key : object.keySet()) {
                    if ("opponents".equals(key)) {
                        JSONObject value = object.getJSONObject(key);
                        for (String oppo : value.keySet()) {
                            JSONObject matchRec = value.getJSONObject(oppo);
                            Map<String, Object> thisOppoRec = new TreeMap<>();
                            for (String keyOfThisOppo : matchRec.keySet()) {
                                switch (keyOfThisOppo) {
                                    case "wins":
                                        thisOppoRec.put("wins", matchRec.getInt("wins"));
                                        break;
                                    case "losses":
                                        thisOppoRec.put("losses", matchRec.getInt("losses"));
                                        break;
                                    default:
                                        JSONObject framesRec = matchRec.getJSONObject(keyOfThisOppo);
                                        // 如 "frames 9": {"wins": 2, "losses": 1}
                                        // 代表9局5胜制的胜率
                                        Map<String, Integer> winLoseOfFrames
                                                = new TreeMap<>();
                                        winLoseOfFrames.put("wins", framesRec.getInt("wins"));
                                        winLoseOfFrames.put("losses", framesRec.getInt("losses"));
                                        thisOppoRec.put(keyOfThisOppo, winLoseOfFrames);
                                        break;
                                }
                            }

                            oppoRecords.put(oppo, thisOppoRec);
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

    private static void incrementMap(Map<String, Integer> map, String key) {
        Integer val = map.get(key);
        map.put(key, val == null ? 1 : val + 1);
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

    public void wonFrameAgainstOpponent(GameType gameType, Player player, String opponentName) {
        Map<String, Map<String, Object>> oppo =
                opponentsRecords.computeIfAbsent(gameType, k -> new HashMap<>());
        Map<String, Object> winLoss = oppo.get(opponentName);
        if (winLoss == null) {
            winLoss = new TreeMap<>();
            winLoss.put("wins", 1);
            winLoss.put("losses", 0);
            oppo.put(opponentName, winLoss);
        } else {
            winLoss.put("wins", (int) winLoss.get("wins") + 1);
        }

        if (player instanceof NumberedBallPlayer) {
            int playTimes = ((NumberedBallPlayer) player).getPlayTimes();
            boolean breaks = ((NumberedBallPlayer) player).isBreakingPlayer();
            Map<String, Integer> intMap = getIntRecordOfType(gameType);
            System.out.println("Play times: " + playTimes);
            if (playTimes == 1) {
                if (breaks) {  // 炸清
                    incrementMap(intMap, "break-clear");
                } else {  // 接清
                    incrementMap(intMap, "continue-clear");
                }
            }
        }
    }

    public void lostFrameAgainstOpponent(GameType gameType, String opponentName) {
        Map<String, Map<String, Object>> oppo =
                opponentsRecords.computeIfAbsent(gameType, k -> new HashMap<>());
        Map<String, Object> winLoss = oppo.get(opponentName);
        if (winLoss == null) {
            winLoss = new TreeMap<>();
            winLoss.put("wins", 0);
            winLoss.put("losses", 1);
            oppo.put(opponentName, winLoss);
        } else {
            winLoss.put("losses", (int) winLoss.get("losses") + 1);
        }
    }

    public void wonEntireGameAgainstOpponent(EntireGame entireGame,
                                             String opponentName) {
        Map<String, Map<String, Object>> oppo =
                opponentsRecords.computeIfAbsent(entireGame.gameType, k -> new HashMap<>());
        Map<String, Object> winLoss = oppo.get(opponentName);
        if (winLoss == null) {
            throw new RuntimeException("一局没输，一局没赢，你咋就赢一场比赛了？");
        } else {
            String framesKey = "frame " + entireGame.totalFrames;
            if (winLoss.containsKey(framesKey)) {
                Map<String, Integer> frameOfThisLen = (Map<String, Integer>) winLoss.get(framesKey);
                frameOfThisLen.put("wins", frameOfThisLen.get("wins") + 1);
            } else {
                Map<String, Integer> frameOfThisLen = new TreeMap<>();
                frameOfThisLen.put("wins", 1);
                frameOfThisLen.put("losses", 0);
                winLoss.put(framesKey, frameOfThisLen);
            }
        }
    }

    public void lostEntireGameAgainstOpponent(EntireGame entireGame,
                                              String opponentName) {
        Map<String, Map<String, Object>> oppo =
                opponentsRecords.computeIfAbsent(entireGame.gameType, k -> new HashMap<>());
        Map<String, Object> winLoss = oppo.get(opponentName);
        if (winLoss == null) {
            throw new RuntimeException("一局没输，一局没赢，你咋就赢一场比赛了？");
        } else {
            String framesKey = "frame " + entireGame.totalFrames;
            if (winLoss.containsKey(framesKey)) {
                Map<String, Integer> frameOfThisLen = (Map<String, Integer>) winLoss.get(framesKey);
                frameOfThisLen.put("losses", frameOfThisLen.get("losses") + 1);
            } else {
                Map<String, Integer> frameOfThisLen = new TreeMap<>();
                frameOfThisLen.put("wins", 0);
                frameOfThisLen.put("losses", 1);
                winLoss.put(framesKey, frameOfThisLen);
            }
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
            Map<String, Map<String, Object>> oppoRecords = opponentsRecords.get(entry.getKey());
            JSONObject opponents = new JSONObject();
            for (Map.Entry<String, Map<String, Object>> oppoEntry : oppoRecords.entrySet()) {
                JSONObject oppo = new JSONObject();
                for (String key : oppoEntry.getValue().keySet()) {
                    switch (key) {
                        case "wins":
                            oppo.put("wins", oppoEntry.getValue().get(key));
                            break;
                        case "losses":
                            oppo.put("losses", oppoEntry.getValue().get(key));
                            break;
                        default:
                            JSONObject framesObj = new JSONObject();
                            Map<String, Integer> framesMap
                                    = (Map<String, Integer>) oppoEntry.getValue().get(key);
                            framesObj.put("wins", framesMap.get("wins"));
                            framesObj.put("losses", framesMap.get("losses"));
                            oppo.put(key, framesObj);
                            break;
                    }
                }

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

    @NotNull
    private Map<String, Integer> getIntRecordOfType(GameType gameType) {
        Map<String, Integer> intMap = intRecords.get(gameType);
        if (intMap == null) {
            intMap = createTypeMap(gameType);
            intRecords.put(gameType, intMap);
        }
        return intMap;
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
