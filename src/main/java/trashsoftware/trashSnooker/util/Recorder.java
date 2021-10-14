package trashsoftware.trashSnooker.util;

import javafx.scene.paint.Color;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.Cue;
import trashsoftware.trashSnooker.core.CuePlayType;
import trashsoftware.trashSnooker.core.PlayerPerson;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Recorder {

    private static final String PLAYER_LIST_FILE = "user" + File.separator + "players.json";
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
        loadPlayers(playersRoot);
    }

    private static void loadCues(JSONObject root) {
        if (root.has("cues")) {
            JSONObject object = root.getJSONObject("cues");
            for (String key : object.keySet()) {
                try {
                    JSONObject cueObject = object.getJSONObject(key);
                    Cue cue = new Cue(
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

    private static void loadPlayers(JSONObject root) {
        if (root.has("players")) {
            JSONObject array = root.getJSONObject("players");
            for (String key : array.keySet()) {
                Object obj = array.get(key);
                if (obj instanceof JSONObject) {
                    JSONObject personObj = (JSONObject) obj;
                    try {
                        String name = personObj.getString("name");
                        PlayerPerson playerPerson;
                        if (personObj.has("pullDt")) {
                            JSONArray pullDt = personObj.getJSONArray("pullDt");
                            double minPullDt = pullDt.getDouble(0);
                            double maxPullDt = pullDt.getDouble(1);
                            double cueSwingMag = personObj.getDouble("cueSwingMag");
                            String cuePlayTypeStr = personObj.getString("cuePlayType");
                            CuePlayType cuePlayType = parseCuePlayType(cuePlayTypeStr);
                            JSONArray muSigmaArray = personObj.getJSONArray("cuePointMuSigma");
                            double[] muSigma = new double[4];
                            for (int i = 0; i < 4; ++i) {
                                muSigma[i] = muSigmaArray.getDouble(i);
                            }
                            playerPerson = new PlayerPerson(
                                    name,
                                    personObj.getDouble("power"),
                                    personObj.getDouble("spin"),
                                    personObj.getDouble("precision"),
                                    minPullDt,
                                    maxPullDt,
                                    cueSwingMag,
                                    muSigma,
                                    cuePlayType
                            );
                        } else {
                            playerPerson = new PlayerPerson(
                                    name,
                                    personObj.getDouble("power"),
                                    personObj.getDouble("spin"),
                                    personObj.getDouble("precision")
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

                        playerPeople.put(name, playerPerson);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void addPlayerPerson(PlayerPerson playerPerson) {
        playerPeople.put(playerPerson.getName(), playerPerson);
        saveToDisk(makeJsonObject(), PLAYER_LIST_FILE);
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

    private static CuePlayType parseCuePlayType(String s) {
        return new CuePlayType(s);
    }

    private static JSONObject makeJsonObject() {
        JSONObject array = new JSONObject();
        for (PlayerPerson playerPerson : playerPeople.values()) {
            JSONObject personObject = new JSONObject();
            personObject.put("name", playerPerson.getName());
            personObject.put("power", playerPerson.getMaxPowerPercentage());
            personObject.put("spin", playerPerson.getMaxSpinPercentage());
            personObject.put("precision", playerPerson.getPrecisionPercentage());
            array.put(playerPerson.getName(), personObject);

            JSONArray cuesArray = new JSONArray();
            for (Cue cue : playerPerson.getPrivateCues()) {
                cuesArray.put(cue.getName());
            }
            personObject.put("privateCues", cuesArray);
        }
        JSONObject root = new JSONObject();
        root.put("players", array);
        return root;
    }

    public static void saveToDisk(JSONObject object, String fileName) {
        createIfNotExists();

        String s = object.toString(2);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            bw.write(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static JSONObject loadFromDisk(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
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
