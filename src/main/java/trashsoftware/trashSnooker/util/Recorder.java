package trashsoftware.trashSnooker.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.configLoader.ConfigLoader;
import trashsoftware.trashSnooker.core.PlayerPerson;

import java.io.*;
import java.util.*;

public class Recorder {

    private static final String PLAYER_LIST_FILE = "user" + File.separator + "players.json";

    private static final Map<String, PlayerPerson> playerPeople = new HashMap<>();
    private static final Map<String, RecordItem> playerRecords = new HashMap<>();
    private static final RecordItem globalRecord = new RecordItem();
    private static PlayerPerson highestBreakPerson;

    public static void loadAll() {
        playerPeople.clear();
        JSONObject root = loadFromDisk(PLAYER_LIST_FILE);
        if (root.has("players")) {
            JSONObject array = root.getJSONObject("players");
            for (String key : array.keySet()) {
                Object obj = array.get(key);
                if (obj instanceof JSONObject) {
                    JSONObject personObj = (JSONObject) obj;
                    try {
                        String name = personObj.getString("name");
                        PlayerPerson playerPerson = new PlayerPerson(
                                name,
                                personObj.getDouble("power"),
                                personObj.getDouble("spin"),
                                personObj.getDouble("precision")
                        );
                        playerPeople.put(name, playerPerson);
                        JSONObject recordObj = personObj.getJSONObject("records");
                        RecordItem recordItem = RecordItem.fromJson(recordObj);
                        playerRecords.put(name, recordItem);
                        if (globalRecord.updateHighestBreak(recordItem.getHighestBreak())) {
                            highestBreakPerson = playerPerson;
                        }
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

    public static void updatePlayerBreak(String playerName, int currentBreak) {
        RecordItem recordItem = playerRecords.get(playerName);
        if (recordItem == null) {
            recordItem = new RecordItem();
            playerRecords.put(playerName, recordItem);
        }
        recordItem.updateHighestBreak(currentBreak);
        globalRecord.updateHighestBreak(currentBreak);
    }

    public static void save() {
        saveToDisk(makeJsonObject(), PLAYER_LIST_FILE);
    }

    public static Collection<PlayerPerson> getPlayerPeople() {
        return playerPeople.values();
    }

    private static JSONObject makeJsonObject() {
        JSONObject array = new JSONObject();
        for (PlayerPerson playerPerson : playerPeople.values()) {
            JSONObject personObject = new JSONObject();
            personObject.put("name", playerPerson.getName());
            personObject.put("power", playerPerson.getMaxPowerPercentage());
            personObject.put("spin", playerPerson.getMaxSpinPercentage());
            personObject.put("precision", playerPerson.getPrecisionPercentage());

            RecordItem recordItem = playerRecords.get(playerPerson.getName());
            JSONObject recordObj = recordItem != null ? recordItem.toJson() : new JSONObject();
            personObject.put("records", recordObj);
            array.put(playerPerson.getName(), personObject);
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
    }
}
