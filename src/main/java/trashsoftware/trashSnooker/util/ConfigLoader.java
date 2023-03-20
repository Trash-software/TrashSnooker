package trashsoftware.trashSnooker.util;

import trashsoftware.trashSnooker.fxml.App;

import java.io.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ConfigLoader {

    public static final String PATH = "user/config.cfg";
    private static final Locale DEFAULT_LOCALE = new Locale("zh", "CN");

    private static ConfigLoader instance;
    private final Map<String, String> keyValues = new HashMap<>();
    private int lastVersion;

    private Locale locale;

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
            
            lastVersion = getInt("version", 0);
            if (lastVersion != App.VERSION_CODE) {
                put("version", App.VERSION_CODE);
            }
        } catch (FileNotFoundException e) {
            writeConfig();
        } catch (IOException e) {
            EventLogger.log(e);
        }
    }

    public static ConfigLoader getInstance() {
        if (instance == null) {
            instance = new ConfigLoader();
        }
        return instance;
    }

    public void put(String key, Object value) {
        keyValues.put(key, value.toString());
    }

    public String getString(String key) {
        return keyValues.get(key);
    }

    public String getString(String key, String defaultValue) {
        return keyValues.getOrDefault(key, defaultValue);
    }

    public int getInt(String key) {
        return Integer.parseInt(keyValues.get(key));
    }

    public int getInt(String key, int defaultValue) {
        String s = getString(key);
        try {
            return s == null ? defaultValue : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getLastVersion() {
        return lastVersion;
    }

    public Locale getLocale() {
        if (locale == null) {
            String locCode = getString("locale");
            if (locCode != null) {
                String[] sp = locCode.split("_");
                try {
                    locale = new Locale(sp[0], sp[1]);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }

            if (locale == null) locale = DEFAULT_LOCALE;
        }
        return locale;
    }
    
    public void save() {
        writeConfig();
    }

    private void initConfig() {
        put("nThreads", 4);
        put("locale", "zh_CN");
        put("recordCompression", "xz");
    }

    private void writeConfig() {
        File pathFile = new File(PATH);
        File dir = pathFile.getParentFile();
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                EventLogger.log("Cannot create user directory!");
            }
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(pathFile))) {
            for (Map.Entry<String, String> entry : keyValues.entrySet()) {
                bw.write(entry.getKey() + "=" + entry.getValue() + '\n');
            }
        } catch (IOException e) {
            EventLogger.log(e);
        }
    }
}
