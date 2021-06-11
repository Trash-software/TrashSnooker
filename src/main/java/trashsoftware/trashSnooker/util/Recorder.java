package trashsoftware.trashSnooker.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.configLoader.ConfigLoader;
import trashsoftware.trashSnooker.core.PlayerPerson;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Recorder {

    private static final String PLAYER_LIST_FILE = "user" + File.separator + "players.json";

    private static final List<PlayerPerson> playerPeople = new ArrayList<>();

    public static void loadAll() {
        playerPeople.clear();
        JSONObject root = loadFromDisk(PLAYER_LIST_FILE);
        if (root.has("players")) {
            JSONArray array = root.getJSONArray("players");
            for (Object obj : array) {
                if (obj instanceof JSONObject) {
                    JSONObject personObj = (JSONObject) obj;
                    try {
                        PlayerPerson playerPerson = new PlayerPerson(
                                personObj.getString("name"),
                                personObj.getDouble("power"),
                                personObj.getDouble("spin"),
                                personObj.getDouble("precision")
                        );
                        playerPeople.add(playerPerson);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void addPlayerPerson(PlayerPerson playerPerson) {
        playerPeople.add(playerPerson);
        saveToDisk(makeJsonObject(), PLAYER_LIST_FILE);
    }

    public static List<PlayerPerson> getPlayerPeople() {
        return playerPeople;
    }

    private static JSONObject makeJsonObject() {
        JSONArray array = new JSONArray();
        for (PlayerPerson playerPerson : playerPeople) {
            JSONObject personObject = new JSONObject();
            personObject.put("name", playerPerson.getName());
            personObject.put("power", playerPerson.getMaxPowerPercentage());
            personObject.put("spin", playerPerson.getMaxSpinPercentage());
            personObject.put("precision", playerPerson.getPrecisionPercentage());
            array.put(personObject);
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
