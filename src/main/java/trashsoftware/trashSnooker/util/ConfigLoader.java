package trashsoftware.trashSnooker.util;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ConfigLoader {
    
    public static final String PATH = "user/config.cfg";
    
    private static ConfigLoader instance;
    private final Map<String, String> keyValues = new HashMap<>();

    public static ConfigLoader getInstance() {
        if (instance == null) {
            instance = new ConfigLoader();
        }
        return instance;
    }
    
    private ConfigLoader() {
        initConfig();
        try (BufferedReader br = new BufferedReader(new FileReader(PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] split = line.split("=");
                if (split.length == 2) {
                    String key = split[0].strip();
                    String val = split[1].strip();
                    keyValues.put(key, val);
                }
            }
        } catch (FileNotFoundException e) {
            writeConfig();
        } catch (IOException e) {
            EventLogger.log(e);
        }
    }
    
    public void put(String key, Object value) {
        keyValues.put(key, value.toString());
    }
    
    public String getString(String key) {
        return keyValues.get(key);
    }
    
    public int getInt(String key) {
        return Integer.parseInt(keyValues.get(key));
    }

    public int getInt(String key, int defaultValue) {
        String s = getString(key);
        return s == null ? defaultValue : Integer.parseInt(s);
    }
    
    private void initConfig() {
        put("nThreads", 8);
    }
    
    private void writeConfig() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(PATH))) {
            for (Map.Entry<String, String> entry : keyValues.entrySet()) {
                bw.write(entry.getKey() + "=" + entry.getValue() + '\n');
            }
        } catch (IOException e) {
            EventLogger.log(e);
        }
    }
}
