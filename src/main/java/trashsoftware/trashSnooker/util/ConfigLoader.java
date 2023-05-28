package trashsoftware.trashSnooker.util;

import trashsoftware.trashSnooker.fxml.App;

import java.awt.*;
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
            EventLogger.error(e);
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
        return keyValues.computeIfAbsent(key, v -> defaultValue);
    }

    public int getInt(String key) {
        return Integer.parseInt(keyValues.get(key));
    }

    public int getInt(String key, int defaultValue) {
        String s = getString(key);
        try {
            if (s == null) {
                keyValues.put(key, String.valueOf(defaultValue));
                return defaultValue;
            }
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getDouble(String key, double defaultValue) {
        String s = getString(key);
        try {
            if (s == null) {
                keyValues.put(key, String.valueOf(defaultValue));
                return defaultValue;
            }
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getLastVersion() {
        return lastVersion;
    }
    
    public int getFrameRate() {
        return getInt("frameRate", 50);
    }

    /**
     * @return {屏幕宽, 屏幕高, 缩放}
     */
    public double[] getResolution() {
        String res = getString("resolution");
        String zoom = getString("systemZoom");
        
        double[] result;
        if (res == null || zoom == null) {
            result = autoDetectScreenParams();
            putScreenParams(result);
        } else {
            String[] widthHeight = res.split("x");
            result = new double[]{Double.parseDouble(widthHeight[0]), Double.parseDouble(widthHeight[1]), Double.parseDouble(zoom)};
        }
        return result;
    }
    
    private double[] autoDetectScreenParams() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        double windowsHeight = screen.getHeight();
        DisplayMode mode = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDisplayMode();
        int hardwareWidth = mode.getWidth();
        int hardwareHeight = mode.getHeight();

        double zoomFactor = hardwareHeight / windowsHeight;

        return new double[]{hardwareWidth, hardwareHeight, zoomFactor};
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
        put("frameRate", 50);
        
        double[] screenParams = autoDetectScreenParams();
        putScreenParams(screenParams);
    }
    
    private void putScreenParams(double[] screenParams) {
        put("resolution", String.format("%dx%d", (int) screenParams[0], (int) screenParams[1]));
        put("systemZoom", screenParams[2]);
    }

    private void writeConfig() {
        File pathFile = new File(PATH);
        File dir = pathFile.getParentFile();
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                EventLogger.error("Cannot create user directory!");
            }
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(pathFile))) {
            for (Map.Entry<String, String> entry : keyValues.entrySet()) {
                bw.write(entry.getKey() + "=" + entry.getValue() + '\n');
            }
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }
}
