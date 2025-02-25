package trashsoftware.trashSnooker.core.metrics;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BallsGroupPreset {

    public final String id;
    public final String name;
    public final Set<GameRule> types;

    // For non-textured
    private final Map<Integer, Color> valueColorMap;

    // For textured presets
    private final Map<Integer, String> valueImageNameMap;
    private Map<Integer, Image> valueImageMap;

    public BallsGroupPreset(String id,
                            String name,
                            Set<GameRule> types,
                            @Nullable Map<Integer, Color> valueColorMap,
                            @Nullable Map<Integer, String> valueImageNameMap
    ) {
        this.id = id;
        this.name = name;
        this.types = types;
        this.valueColorMap = valueColorMap;
        this.valueImageNameMap = valueImageNameMap;
    }

    public static BallsGroupPreset fromJson(JSONObject json) {
        Set<GameRule> rules = new HashSet<>();
        JSONArray array = json.getJSONArray("types");
        for (int i = 0; i < array.length(); i++) {
            String item = array.getString(i);
            GameRule rule = GameRule.fromSqlKey(item);
            rules.add(rule);
        }

        JSONObject colorsJson = json.has("colorMap") ? json.getJSONObject("colorMap") : null;
        Map<Integer, Color> valueColorMap = null;
        if (colorsJson != null) {
            valueColorMap = new HashMap<>();
            for (String key : colorsJson.keySet()) {
                try {
                    Integer val = Integer.parseInt(key);
                    valueColorMap.put(val, DataLoader.parseColor(colorsJson.getString(key)));
                } catch (IllegalArgumentException e) {
                    EventLogger.warning(e);
                }
            }
        }
        JSONObject textureJson = json.has("textureMap") ? json.getJSONObject("textureMap") : null;
        Map<Integer, String> valueImageNameMap = null;
        if (textureJson != null) {
            valueImageNameMap = new HashMap<>();
            for (String key : textureJson.keySet()) {
                try {
                    Integer val = Integer.parseInt(key);
                    valueImageNameMap.put(val, textureJson.getString(key));
                } catch (IllegalArgumentException e) {
                    EventLogger.warning(e);
                }
            }
        }

        return new BallsGroupPreset(
                json.getString("id"),
                DataLoader.getObjectOfLocale(json.getJSONObject("name")),
                rules,
                valueColorMap,
                valueImageNameMap
        );
    }

    public Color getColorByBallValue(int value) {
        if (valueColorMap == null) return null;
        return valueColorMap.get(value);
    }

    public Image getImageByBallValue(int value) {
        if (valueImageNameMap == null) return null;

        if (valueImageMap == null) loadTextures();

        return valueImageMap.get(value);
    }

    private void loadTextures() {
        if (valueImageNameMap == null) return;
        valueImageMap = new HashMap<>();

        String path = "/trashsoftware/trashSnooker/res/img/%s/ball/%s/"
                .formatted(ConfigLoader.getInstance().getBallMaterialResolution(),
                        id
                );
        for (Map.Entry<Integer, String> entry : valueImageNameMap.entrySet()) {
            String fileName = path + entry.getValue();
            URL url = getClass().getResource(fileName);
            if (url == null) {
                EventLogger.error("Cannot load resource: " + fileName);
                return;
            }
            Image image = new Image(url.toExternalForm());
            valueImageMap.put(entry.getKey(), image);
        }
    }
}
