package trashsoftware.trashSnooker.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Map;

public class JsonChecksumTest {
    
    @Test
    public void testJsonChecksum() {
        JSONObject root = new JSONObject();
        root.put("name", "example1");
        
        JSONObject subRoot = new JSONObject();
        subRoot.put("int", 1);
        subRoot.put("boolean", false);
        subRoot.put("double1", 3.5423000000001);
        subRoot.put("double2", 0.1);
        subRoot.put("string", "This is a string");

        JSONArray strArray = new JSONArray();
        strArray.put("first");
        strArray.put("second");
        strArray.put("third");
        
        subRoot.put("strArray", strArray);
        
        JSONArray objArr = new JSONArray();
        JSONObject obj1 = new JSONObject(Map.of("a", "aa", "b", "bb"));
        JSONObject obj2 = new JSONObject(Map.of("1", "11", "2", "22"));
        JSONObject obj3 = new JSONObject(Map.of("啊", "啊啊", "吧", "粑粑"));
        objArr.put(obj1);
        objArr.put(obj2);
        objArr.put(obj3);
        
        subRoot.put("obj arr", objArr);
        
        root.put("subRoot", subRoot);
        
        String checksum = JsonChecksum.checksum(root.getJSONObject("subRoot"));
        root.put("checksum", checksum);
        System.out.println(checksum);
        
        String jsonStr = root.toString(2);

//        System.out.println(jsonStr);
        
        JSONObject recoverRoot = new JSONObject(jsonStr);
        JSONObject recoverSubRoot = recoverRoot.getJSONObject("subRoot");
        String recoverChecksum = root.getString("checksum");
        String validate = JsonChecksum.checksum(recoverSubRoot);

        System.out.println(recoverChecksum);
        System.out.println(validate);
    }
}
