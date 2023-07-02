package trashsoftware.trashSnooker.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class JsonChecksum {
    
    private final MessageDigest md;
    private final byte[] buffer = new byte[8];
    
    private JsonChecksum() throws NoSuchAlgorithmException {
        this.md = MessageDigest.getInstance("MD5");
    }
    
    public void hash(String key, Object object) {
        if ("checksum".equals(key)) return;
        md.update(key.getBytes(StandardCharsets.UTF_8));  // 先存key
        if (object == null) {
            // do nothing  
        } else if (object instanceof Long l) {
            Util.intToBytesN(l, buffer, 0, 8);
            md.update(buffer);
        } else if (object instanceof Integer i) {
            Util.intToBytesN(i, buffer, 0, 4);
            md.update(buffer,0,4);
        } else if (object instanceof Double d) {
            Util.doubleToBytes(d, buffer, 0);
            md.update(buffer);
        } else if (object instanceof Float f) {
            Util.floatToBytes(f, buffer, 0);
            md.update(buffer);
        } else if (object instanceof BigDecimal bd) {
            Util.doubleToBytes(bd.doubleValue(), buffer, 0);
            md.update(buffer);
        } else if (object instanceof Boolean bool) {
            buffer[0] = (byte) (bool ? 1 : 0);
            md.update(buffer, 0, 1);
        } else if (object instanceof String str) {
            md.update(str.getBytes(StandardCharsets.UTF_8));
        } else if (object instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) {
                hash(String.valueOf(i), array.get(i));
            }
        } else if (object instanceof JSONObject json) {
            List<String> keys = new ArrayList<>(json.keySet());
            keys.sort(String::compareTo);
            for (String subKey : keys) {
                hash(subKey, json.get(subKey));
            }
        } else {
            throw new RuntimeException("Cannot hash object type: " + object + " " + object.getClass() + ", key=" + key);
        }
    }
    
    public byte[] getResult() {
        return md.digest();
    }
    
    public static String checksum(JSONObject jsonObject) {
        try {
            JsonChecksum jsonChecksum = new JsonChecksum();
            jsonChecksum.hash("json", jsonObject);
            byte[] result = jsonChecksum.getResult();
            return Util.byteArrayToHex(result);
        } catch (NoSuchAlgorithmException e) {
            EventLogger.error(e);
            return "null";
        }
    }
}
