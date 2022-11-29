package trashsoftware.trashSnooker.util;

import javafx.scene.effect.Reflection;
import javafx.scene.paint.Color;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.Cue;
import trashsoftware.trashSnooker.core.CuePlayType;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.ai.AiPlayStyle;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Recorder {
    
    public static final boolean SHOW_HIDDEN = true;

    private static final String PLAYER_LIST_FILE = "user" + File.separator + "players.json";
    private static final String CUSTOM_PLAYER_LIST_FILE = 
            "user" + File.separator + "custom_players.json";
    private static final String CUE_LIST_FILE = "user" + File.separator + "cues.json";
    public static final String RECORDS_DIRECTORY = "user" + File.separator + "records";

    private static final Map<String, PlayerPerson> playerPeople = new HashMap<>();
    private static final Map<String, Cue> cues = new HashMap<>();

    public static void loadAll() {
        cues.clear();
        JSONObject cuesRoot = loadFromDisk(CUE_LIST_FILE);
        loadCues(cuesRoot);

        playerPeople.clear();
        JSONObject playersRoot = loadFromDisk(PLAYER_LIST_FILE);
        JSONObject customPlayersRoot = loadFromDisk(CUSTOM_PLAYER_LIST_FILE);
        loadPlayers(playersRoot, false);
        loadPlayers(customPlayersRoot, true);
    }

    private static void loadCues(JSONObject root) {
        if (root.has("cues")) {
            JSONObject object = root.getJSONObject("cues");
            for (String key : object.keySet()) {
                try {
                    JSONObject cueObject = object.getJSONObject(key);
                    Cue cue = new Cue(
                            key,
                            cueObject.getString("name"),
                            cueObject.getDouble("frontLength"),
                            cueObject.getDouble("midLength"),
                            cueObject.getDouble("backLength"),
                            cueObject.getDouble("ringThickness"),
                            cueObject.getDouble("tipThickness"),
                            cueObject.getDouble("endDiameter"),
                            cueObject.getDouble("tipDiameter"),
                            parseColor(cueObject.getString("ringColor")),
                            parseColor(cueObject.getString("frontColor")),
                            parseColor(cueObject.getString("midColor")),
                            parseColor(cueObject.getString("backColor")),
                            cueObject.getDouble("power"),
                            cueObject.getDouble("spin"),
                            cueObject.getDouble("accuracy"),
                            cueObject.getBoolean("privacy")
                    );
                    cues.put(key, cue);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static Color parseColor(String colorStr) {
        StringBuilder builder = new StringBuilder();
        int brighterCount = 0;
        int darkerCount = 0;
        for (char c : colorStr.toCharArray()) {
            if (Character.isAlphabetic(c)) {
                builder.append(c);
            } else if (c == '+') brighterCount++;
            else if (c == '-') darkerCount++;
        }
        Color color = Color.valueOf(builder.toString());
        for (int i = 0; i < brighterCount; ++i) color = color.brighter();
        for (int i = 0; i < darkerCount; ++i) color = color.darker();
        return color;
    }

    private static void loadPlayers(JSONObject root, boolean isCustomPlayer) {
        if (root.has("players")) {
            JSONObject array = root.getJSONObject("players");
            for (String key : array.keySet()) {
                Object obj = array.get(key);
                if (obj instanceof JSONObject) {
                    JSONObject personObj = (JSONObject) obj;
                    try {
                        if (!SHOW_HIDDEN && 
                                personObj.has("hidden") && 
                                personObj.getBoolean("hidden")) {
                            continue;
                        }
                        String name = personObj.getString("name");
                        AiPlayStyle aiPlayStyle;
                        if (personObj.has("ai")) {
                            JSONObject aiObject = personObj.getJSONObject("ai");
                            aiPlayStyle = new AiPlayStyle(
                                    aiObject.getDouble("precision"),
                                    aiObject.getDouble("stable"),
                                    aiObject.getDouble("position"),
                                    aiObject.getDouble("defense"),
                                    aiObject.getDouble("attackPri"),
                                    aiObject.getDouble("likeShow"),
                                    aiObject.getString("snookerBreak"),
                                    aiObject.getBoolean("cebSideBreak"),
                                    aiObject.getInt("withdrawAfter")
                            );
                        } else {
                            aiPlayStyle = AiPlayStyle.DEFAULT;
                        }

                        PlayerPerson.HandBody handBody;
                        if (personObj.has("hand")) {
                            JSONObject handObj = personObj.getJSONObject("hand");
                            handBody = new PlayerPerson.HandBody(
                                    personObj.getDouble("height"),
                                    personObj.getDouble("width"),
                                    handObj.getDouble("left"),
                                    handObj.getDouble("right"),
                                    handObj.getDouble("rest")
                            );
                        } else {
                            handBody = PlayerPerson.HandBody.DEFAULT;
                        }
                        
                        PlayerPerson playerPerson;
                        if (personObj.has("pullDt")) {
                            JSONArray pullDt = personObj.getJSONArray("pullDt");
                            double minPullDt = pullDt.getDouble(0);
                            double maxPullDt = pullDt.getDouble(1);
                            double aimingOffset = personObj.getDouble("aimingOffset");
                            double cueSwingMag = personObj.getDouble("cueSwingMag");
                            String cuePlayTypeStr = personObj.getString("cuePlayType");
                            CuePlayType cuePlayType = parseCuePlayType(cuePlayTypeStr);
                            JSONArray muSigmaArray = personObj.getJSONArray("cuePointMuSigma");
                            double[] muSigma = new double[4];
                            for (int i = 0; i < 4; ++i) {
                                muSigma[i] = muSigmaArray.getDouble(i);
                            }
                            
                            playerPerson = new PlayerPerson(
                                    key,
                                    name,
                                    personObj.getString("category"),
                                    personObj.getDouble("maxPower"),
                                    personObj.getDouble("controllablePower"),
                                    personObj.getDouble("spin"),
                                    personObj.getDouble("precision"),
                                    personObj.getDouble("anglePrecision"),
                                    personObj.getDouble("longPrecision"),
                                    personObj.getDouble("solving"),
                                    minPullDt,
                                    maxPullDt,
                                    aimingOffset,
                                    cueSwingMag,
                                    muSigma,
                                    personObj.getDouble("powerControl"),
                                    personObj.getDouble("psy"),
                                    cuePlayType,
                                    aiPlayStyle,
                                    handBody
                            );
                        } else {
                            playerPerson = new PlayerPerson(
                                    key,
                                    name,
                                    personObj.getDouble("maxPower"),
                                    personObj.getDouble("controllablePower"),
                                    personObj.getDouble("spin"),
                                    personObj.getDouble("precision"),
                                    personObj.getDouble("anglePrecision"),
                                    personObj.getDouble("longPrecision"),
                                    personObj.getDouble("powerControl"),
                                    aiPlayStyle,
                                    isCustomPlayer
                            );
                        }
                        if (personObj.has("privateCues")) {
                            JSONArray pCues = personObj.getJSONArray("privateCues");
                            for (Object cueObj : pCues) {
                                if (cueObj instanceof String) {
                                    String pCue = (String) cueObj;
                                    if (cues.containsKey(pCue)) {
                                        playerPerson.addPrivateCue(cues.get(pCue));
                                    } else {
                                        System.out.printf("%s没有%s\n", name, pCue);
                                    }
                                }
                            }
                        }
                        playerPerson.setCustom(isCustomPlayer);
                        playerPeople.put(name, playerPerson);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void addPlayerPerson(PlayerPerson playerPerson) {
        playerPeople.put(getNextCustomPlayerId(), playerPerson);
        saveToDisk(makeJsonObject(), CUSTOM_PLAYER_LIST_FILE);
    }
    
    public static String getNextCustomPlayerId() {
        int current = playerPeople.size() + 1;
        String id;
        do {
            id = "custom_player_" + current;
            current++;
        } while (playerPeople.containsKey(id));
        return id;
    }

    public static Collection<PlayerPerson> getPlayerPeople() {
        return playerPeople.values();
    }

    public static Map<String, Cue> getCues() {
        return cues;
    }

    public static Cue getStdBreakCue() {
        return cues.get("stdBreakCue");
    }
    
    public static Cue getRestCue() {
        return cues.get("restCue");
    }

    private static CuePlayType parseCuePlayType(String s) {
        return new CuePlayType(s);
    }

    private static JSONObject makeJsonObject() {
        JSONObject result = new JSONObject();
        for (PlayerPerson playerPerson : playerPeople.values()) {
            if (!playerPerson.isCustom()) continue;
            JSONObject personObject = playerPerson.toJsonObject();
            result.put(playerPerson.getName(), personObject);
        }
        JSONObject root = new JSONObject();
        root.put("players", result);
        return root;
    }

    public static void saveToDisk(JSONObject object, String fileName) {
        createIfNotExists();

        String s = object.toString(2);
        try (BufferedWriter bw = new BufferedWriter(
                new FileWriter(fileName, StandardCharsets.UTF_8))) {
            bw.write(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static JSONObject loadFromDisk(String fileName) {
        try (BufferedReader br = new BufferedReader(
                new FileReader(fileName, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                builder.append(line);
            }
            return new JSONObject(builder.toString());
        } catch (FileNotFoundException e) {
            return new JSONObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

    private static void createIfNotExists() {
        File userDir = new File("user");
        if (!userDir.exists()) {
            if (!userDir.mkdirs()) {
                System.out.println("Cannot create user directory.");
            }
        }
        File recordsDir = new File(RECORDS_DIRECTORY);
        if (!recordsDir.exists()) {
            if (!recordsDir.mkdirs()) {
                System.out.println("Cannot create record directory.");
            }
        }
    }
}
