package trashsoftware.trashSnooker.core.training;

import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.util.Util;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SingleBallRepeater {
    
    public final int maxRepeat;
    public final Map<Integer, int[]> mapperFromSchema;
    protected int shotIndex = 0;
    
    SingleBallRepeater(int maxRepeat, Map<Integer, int[]> mapperFromSchema) {
        this.maxRepeat = maxRepeat;
        this.mapperFromSchema = mapperFromSchema;
    }
    
    public static SingleBallRepeater fromJson(JSONObject jsonObject) {
        int max = jsonObject.getInt("max");
        JSONObject mapper = jsonObject.getJSONObject("mapper");
        Map<Integer, int[]> mapperFromSchema = new TreeMap<>();
        for (String key : mapper.keySet()) {
            int keyInt = Integer.parseInt(key);
            JSONArray values = mapper.getJSONArray(key);
            int[] mapList = new int[values.length()];
            for (int i = 0; i < mapList.length; i++) {
                mapList[i] = values.getInt(i);
            }
            mapperFromSchema.put(keyInt, mapList);
        }
        return new SingleBallRepeater(max, mapperFromSchema);
    }
    
    public int getMapped(int schemaBallVal) {
        int[] mapArr = mapperFromSchema.get(schemaBallVal);
        if (mapArr == null) return schemaBallVal;
        else return mapArr[shotIndex % mapArr.length];
    }

    public int getShotIndex() {
        return shotIndex;
    }
    
    public int nextShot() {
        return ++shotIndex;
    }

    public int getMaxRepeat() {
        return maxRepeat;
    }
}
