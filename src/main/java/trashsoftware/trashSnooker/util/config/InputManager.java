package trashsoftware.trashSnooker.util.config;

import javafx.scene.input.KeyCode;
import trashsoftware.trashSnooker.util.EventLogger;

import java.io.*;
import java.util.*;

public class InputManager {

    public static final Set<KeyCode> SYMBOL_KEYS = Set.of(
            KeyCode.UNDERSCORE, KeyCode.EQUALS, 
            KeyCode.BRACELEFT, KeyCode.BRACERIGHT, KeyCode.BACK_SLASH,
            KeyCode.SEMICOLON, KeyCode.QUOTE,
            KeyCode.COMMA, KeyCode.PERIOD, KeyCode.SLASH
    );
    
    private final Map<KeyBehavior, KeyCode> keyCodeMap = new HashMap<>();  // 键位
    private final Map<KeyCode, KeyBehavior> reverseMap = new TreeMap<>();
    
    InputManager() {
        initDefaultKeyMap();
        loadFromDisk();
        updateReverseMap();
    }
    
    public static String keyCodeShown(KeyCode keyCode) {
        if (keyCode.isArrowKey()) {
            return switch (keyCode) {
                case LEFT -> "←";
                case RIGHT -> "→";
                case UP -> "↑";
                case DOWN -> "↓";
                default -> keyCode.name();
            };
        } 
        else if (keyCode.isWhitespaceKey()) {
            return keyCode.name();
        } 
        else if (SYMBOL_KEYS.contains(keyCode)) {
            return keyCode.getChar();
        }
        else if (keyCode.isKeypadKey()) {
            return keyCode.name();
        }
        else if (keyCode.isDigitKey()) {
            return keyCode.getChar();
        }
        return keyCode.name();
    }
    
    private void loadFromDisk() {
        // load key codes
        try (BufferedReader br = new BufferedReader(new FileReader(ConfigLoader.KEY_MAP_PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] split = line.split("=");
                if (split.length == 2) {
                    String key = split[0].strip();
                    String val = split[1].strip();
                    KeyCode valCode = KeyCode.valueOf(val);
                    try {
                        keyCodeMap.put(KeyBehavior.valueOf(key), valCode);
                    } catch (IllegalArgumentException iae) {
                        EventLogger.warning("Key behavior code: " + key + " not existed.");
                    }
//                    System.out.println("Put: " + key + "=" + valCode);
                }
            }
        } catch (FileNotFoundException e) {
            writeKeyCodeMap();
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }

    public KeyCode getKeyCode(KeyBehavior keyBehavior) {
        KeyCode val = keyCodeMap.get(keyBehavior);
        if (val == null) {
            EventLogger.warning("Cannot find key of behavior: " + keyBehavior + "\nAvailable: " + keyCodeMap);
        }
        return val;
    }

    public void setKeyCode(KeyBehavior keyBehavior, KeyCode keyCode) {
        keyCodeMap.put(keyBehavior, keyCode);
        updateReverseMap();
    }

    public KeyBehavior getBehavior(KeyCode keyCode) {
        return reverseMap.get(keyCode);
    }

    public void reset() {
        initDefaultKeyMap();
        updateReverseMap();
    }
    
    private void updateReverseMap() {
        for (Map.Entry<KeyBehavior, KeyCode> entry : keyCodeMap.entrySet()) {
            reverseMap.put(entry.getValue(), entry.getKey());
        }
    }

    private void initDefaultKeyMap() {
        for (KeyBehavior keyBehavior : KeyBehavior.values()) {
            keyCodeMap.put(keyBehavior, keyBehavior.defaultKey);
        }
    }

    private void writeKeyCodeMap() {
        File pathFile = new File(ConfigLoader.KEY_MAP_PATH);
        File dir = pathFile.getParentFile();
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                EventLogger.error("Cannot create user directory!");
            }
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(pathFile))) {
            for (Map.Entry<KeyBehavior, KeyCode> entry : keyCodeMap.entrySet()) {
                bw.write(entry.getKey().name() + "=" + entry.getValue() + '\n');
            }
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }
    
    public void save() {
        writeKeyCodeMap();
    }
}
