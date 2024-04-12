package trashsoftware.trashSnooker.res;

import trashsoftware.trashSnooker.util.EventLogger;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
//import javafx.scene.media.AudioClip

public class WavInfo {

    private final File file;
    private final AudioFormat audioFormat;
    private final Clip clip;
    private final float defaultVolume;

    private WavInfo(File file, float defaultVolume) throws Exception {
        this.file = file;
        this.defaultVolume = defaultVolume;

        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
        audioFormat = audioInputStream.getFormat();

        DataLine.Info info = new DataLine.Info(Clip.class, audioFormat);
        clip = (Clip) AudioSystem.getLine(info);
        clip.open(audioInputStream);
    }

    public static WavInfo load(String path) {
        return load(path, 1.0f);
    }

    public static WavInfo load(String path, float defaultVolume) {
        File f = new File(path);

        if (f.exists()) {
            try {
                return new WavInfo(f, defaultVolume);
            } catch (Exception e) {
                EventLogger.error(e);   
            }
        }
        EventLogger.warning("Sound file '" + path + "' does not exist");
        return null;

    }

    public Clip getClip() {
        return clip;
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public float getDefaultVolume() {
        return defaultVolume;
    }

    public File getFile() {
        return file;
    }
}
