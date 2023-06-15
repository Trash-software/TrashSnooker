package trashsoftware.trashSnooker.util;

import javafx.scene.paint.Color;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.Cue;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.metrics.TablePreset;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DataLoader {

    public static final boolean SHOW_HIDDEN = true;
    public static final String RECORDS_DIRECTORY = "user/records";
    private static final String[] PLAYER_LIST_FILES = {
            "data/players_prof.json",
            "data/players_amateur.json"
    };
    private static final String CUSTOM_PLAYER_LIST_FILE = "user/custom_players.json";
    private static final String RANDOM_PLAYERS_LIST_FILE = "user/players_random.json";
    private static final String CUE_LIST_FILE = "data/cues.json";
    private static final String TABLE_PRESETS_FILE = "data/tables.json";

    private static DataLoader instance;

    private final Map<String, PlayerPerson> actualPlayers = new HashMap<>();
    private final Map<String, PlayerPerson> playerPeople = new HashMap<>();
    private final Map<String, Cue> cues = new HashMap<>();
    private final Map<String, Cue> publicCues = new HashMap<>();
    private final Map<String, Map<String, TablePreset>> tablePresets = new HashMap<>();

    public static DataLoader getInstance() {
        if (instance == null) {
            instance = new DataLoader();
            instance.loadAll();
        }
        return instance;
    }

    public static Color parseColor(String colorStr) {
        if (colorStr.startsWith("#")) {
            return Color.valueOf(colorStr);
        }
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

    public static String getStringOfLocale(Object probNames) {
        if (probNames instanceof JSONObject) {
            JSONObject names = (JSONObject) probNames;
            String currentLang = ConfigLoader.getInstance().getLocale().getLanguage();
            if (names.has(currentLang)) {
                return names.getString(currentLang);
            } else {
                // 随便返回一个
                for (String key : names.keySet()) {
                    return names.getString(key);
                }
            }
        } else if (probNames instanceof String) {
            return (String) probNames;
        }
        throw new RuntimeException("Cannot find name: " + probNames);
    }

    private static Map<String, PlayerPerson> loadPlayers(JSONObject root,
                                                         Map<String, Cue> cues,
                                                         boolean isCustomPlayer) {
        if (root.has("players")) {
            JSONObject array = root.getJSONObject("players");
            if (isCustomPlayer && root.has("checksum")) {
                String checksum = root.getString("checksum");
                String validation = JsonChecksum.checksum(array);
                if (!checksum.equals(validation)) {
                    System.err.println("You hacked your custom players!");
                    System.err.println("Old: " + checksum + ", new: " + validation);
                }
            }

            Map<String, PlayerPerson> result = new HashMap<>();
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
                        PlayerPerson playerPerson = PlayerPerson.fromJson(key, personObj);

                        if (personObj.has("privateCues")) {
                            JSONArray pCues = personObj.getJSONArray("privateCues");
                            for (Object cueObj : pCues) {
                                if (cueObj instanceof String) {
                                    String pCue = (String) cueObj;
                                    if (cues.containsKey(pCue)) {
                                        playerPerson.addPrivateCue(cues.get(pCue));
                                    } else {
                                        System.out.printf("%s没有%s\n", playerPerson.getPlayerId(), pCue);
                                    }
                                }
                            }
                        }
                        playerPerson.setCustom(isCustomPlayer);
                        result.put(key, playerPerson);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            return result;
        } else {
            throw new RuntimeException("Invalid player root file");
        }
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

    public static JSONObject loadPlayerListFromDisk(String fileName) {
        JSONObject object = loadFromDisk(fileName);
        if (object.isEmpty()) {
            object.put("players", new JSONObject());
        }
        return object;
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
            throw new RuntimeException(e);
        }
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

    public static String generateIdByName(String name) {
        byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(Util.decimalToHex(b & 0xff, 2));
        }
        return "custom_" + builder;
    }
    
//    public void invalidate() {
//        for (Cue cue : cues.values()) {
//            cue.invalidate();
//        }
//    }

    public List<PlayerPerson> filterActualPlayersByCategory(String category) {
        Collection<PlayerPerson> actPlayers = getActualPlayers();
        if ("All".equals(category)) {
            return new ArrayList<>(actPlayers);
        } else {
            List<PlayerPerson> res = new ArrayList<>();
            for (PlayerPerson p : actPlayers) {
                if (p.category.equals(category)) res.add(p);
            }
            return res;
        }
    }

    private Map<String, PlayerPerson> generateAndSaveRandomPlayers(int nRandomPlayers) {
        JSONObject root = new JSONObject();
        JSONObject object = new JSONObject();
        Map<String, PlayerPerson> result = new HashMap<>();
        for (int i = 0; i < nRandomPlayers; i++) {
            String rid = "random_player_" + i;
            String name = "Random Player " + i;
            PlayerPerson rp = PlayerPerson.randomPlayer(
                    rid,
                    name,
                    65.0,
                    85.0
            );
            result.put(rid, rp);
            object.put(rid, rp.toJsonObject());
        }
        root.put("players", object);
        saveToDisk(root, RANDOM_PLAYERS_LIST_FILE);

        return result;
    }

    private void loadAll() {
        cues.clear();
        JSONObject cuesRoot = loadFromDisk(CUE_LIST_FILE);
        loadCues(cuesRoot);
        JSONObject tablesRoot = loadFromDisk(TABLE_PRESETS_FILE);
        loadTablePresets(tablesRoot);

        playerPeople.clear();
        actualPlayers.clear();

        for (String fileName : PLAYER_LIST_FILES) {
            JSONObject playersRoot = loadPlayerListFromDisk(fileName);
            Map<String, PlayerPerson> people = loadPlayers(playersRoot, cues, false);
            playerPeople.putAll(people);
            actualPlayers.putAll(people);
        }

        JSONObject customPlayersRoot = loadPlayerListFromDisk(CUSTOM_PLAYER_LIST_FILE);
        var customs = loadPlayers(customPlayersRoot, cues, true);
        playerPeople.putAll(customs);
        actualPlayers.putAll(customs);

        // 随机球员，填充位置用，以免人数凑不齐
        if (new File(RANDOM_PLAYERS_LIST_FILE).exists()) {
            JSONObject randomPlayers = loadPlayerListFromDisk(RANDOM_PLAYERS_LIST_FILE);
            var randoms = loadPlayers(randomPlayers, cues, false);
            playerPeople.putAll(randoms);
        } else {
            var randoms = generateAndSaveRandomPlayers(128);
            playerPeople.putAll(randoms);
        }

        System.out.println(playerPeople.size() + " players loaded, " + actualPlayers.size() + " actual");
    }

    private void loadCues(JSONObject root) {
        if (root.has("cues")) {
            JSONObject object = root.getJSONObject("cues");
            for (String key : object.keySet()) {
                try {
                    JSONObject cueObject = object.getJSONObject(key);
                    String name = getStringOfLocale(cueObject.get("names"));

                    Cue cue = new Cue(
                            key,
                            name,
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
                    if (cueObject.has("arrow")) {
                        JSONObject arrowObj = cueObject.getJSONObject("arrow");
                        cue.createArrow(arrowObj);
                    }
                    cues.put(key, cue);
                    if (!cue.privacy) publicCues.put(key, cue);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void loadTablePresets(JSONObject root) {
        if (root.has("tables")) {
            JSONArray array = root.getJSONArray("tables");
            for (Object obj : array) {
                JSONObject object = (JSONObject) obj;
                String id = object.getString("id");
                String type = object.getString("type");
                TablePreset tp = TablePreset.fromJson(object);

                System.out.println(id + " " + tp.tableSpec.tableCloth.goodness.name());

                Map<String, TablePreset> tablesOfType = tablePresets.computeIfAbsent(
                        type, k -> new HashMap<>()
                );
                tablesOfType.put(id, tp);
            }
        }
    }

    public TablePreset getTablePresetById(String id) {
        for (Map<String, TablePreset> entry : tablePresets.values()) {
            for (Map.Entry<String, TablePreset> presetEntry : entry.entrySet()) {
                if (presetEntry.getKey().equals(id)) return presetEntry.getValue();
            }
        }
        throw new RuntimeException("Cannot find table " + id);
    }

    public Map<String, TablePreset> getTablesOfType(String type) {
        return tablePresets.getOrDefault(type, new HashMap<>());
    }

    public void addPlayerPerson(PlayerPerson playerPerson) {
        playerPeople.put(playerPerson.getPlayerId(), playerPerson);
        saveToDisk(makeCustomJson(), CUSTOM_PLAYER_LIST_FILE);
    }

    public String getNextCustomPlayerId() {
        int current = playerPeople.size() + 1;
        String id;
        do {
            id = "custom_player_" + current;
            current++;
        } while (playerPeople.containsKey(id));
        return id;
    }

    public PlayerPerson getPlayerPerson(String playerId) {
        return playerPeople.get(playerId);
    }

    public void deletePlayer(String playerId) {
        playerPeople.remove(playerId);
        actualPlayers.remove(playerId);

        saveToDisk(makeCustomJson(), CUSTOM_PLAYER_LIST_FILE);
    }

    public boolean hasActualPlayer(String playerId) {
        return actualPlayers.containsKey(playerId);
    }

    public boolean hasPlayer(String playerId) {
        return getPlayerPerson(playerId) != null;
    }

    public Collection<PlayerPerson> getActualPlayers() {
        return actualPlayers.values();
    }

    public void updatePlayer(PlayerPerson playerPerson) {
        // assert isCustom and is not random
        String id = playerPerson.getPlayerId();
        playerPeople.put(id, playerPerson);

        if (actualPlayers.containsKey(id)) {
            actualPlayers.put(id, playerPerson);
        }

        saveToDisk(makeCustomJson(), CUSTOM_PLAYER_LIST_FILE);
    }

    /**
     * @return 所有的player，包括随机的
     */
    public Collection<PlayerPerson> getAllPlayers() {
        return playerPeople.values();
    }

    public Map<String, PlayerPerson> getPlayerPeopleCopy() {
        return new HashMap<>(playerPeople);
    }

    public Map<String, Cue> getCues() {
        return cues;
    }

    public Cue getStdBreakCue() {
        return cues.get("stdBreakCue");
    }

    public Cue getRestCue() {
        return cues.get("restCue");
    }

    public Map<String, Cue> getPublicCues() {
        return publicCues;
    }

    public Cue getCueById(String cueId) {
        return cues.get(cueId);
    }

    private JSONObject makeCustomJson() {
        JSONObject result = new JSONObject();
        for (PlayerPerson playerPerson : playerPeople.values()) {
            if (!playerPerson.isCustom()) continue;
            JSONObject personObject = playerPerson.toJsonObject();
            result.put(playerPerson.getPlayerId(), personObject);
        }
        String checksum = JsonChecksum.checksum(result);
        JSONObject root = new JSONObject();
        root.put("players", result);
        root.put("checksum", checksum);
        return root;
    }
}
