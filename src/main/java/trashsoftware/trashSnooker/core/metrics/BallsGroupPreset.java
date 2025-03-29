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
    private final Map<Integer, TextureBallName> valueImageNameMap;
    private Map<Integer, TextureBall> valueImageMap;

    public BallsGroupPreset(String id,
                            String name,
                            Set<GameRule> types,
                            @Nullable Map<Integer, Color> valueColorMap,
                            @Nullable Map<Integer, TextureBallName> valueImageNameMap
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
        Map<Integer, TextureBallName> valueImageNameMap = null;
        if (textureJson != null) {
            valueImageNameMap = new HashMap<>();
            for (String key : textureJson.keySet()) {
                try {
                    Integer val = Integer.parseInt(key);

                    JSONObject textObj = textureJson.getJSONObject(key);
                    String name = textObj.getString("path");
                    double xRotate = textObj.optDouble("xRotate", 0.0);
                    boolean equirectangular = textObj.optBoolean("equirectangular", false);

                    valueImageNameMap.put(val, new TextureBallName(name, xRotate, equirectangular));
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

    public TextureBall getImageByBallValue(int value) {
        if (valueImageNameMap == null) return null;

        if (valueImageMap == null) loadTextures();

        return valueImageMap.get(value);
    }
    
    public boolean isEquirectangular(int value) {
        return valueImageNameMap != null && 
                valueImageNameMap.containsKey(value) && 
                valueImageNameMap.get(value).equirectangular;
    }

    private void loadTextures() {
        if (valueImageNameMap == null) return;
        valueImageMap = new HashMap<>();

        String path = "/trashsoftware/trashSnooker/res/img/%s/ball/%s/"
                .formatted(ConfigLoader.getInstance().getBallMaterialResolution(),
                        id
                );
        for (Map.Entry<Integer, TextureBallName> entry : valueImageNameMap.entrySet()) {
            TextureBallName tbn = entry.getValue();
            String fileName = path + tbn.imageName;
            URL url = getClass().getResource(fileName);
            if (url == null) {
                EventLogger.error("Cannot load resource: " + fileName);
                return;
            }
            Image image = new Image(url.toExternalForm());
            valueImageMap.put(entry.getKey(), new TextureBall(image, tbn.xRotate, tbn.equirectangular));
        }
    }

    public record TextureBallName(String imageName,
                                  double xRotate,
                                  boolean equirectangular) {

    }
    
    public record TextureBall(Image image,
                              double xRotate,
                              boolean equirectangular) {
        
    }
}
