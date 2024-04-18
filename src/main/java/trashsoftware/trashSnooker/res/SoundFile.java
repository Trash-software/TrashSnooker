package trashsoftware.trashSnooker.res;

import trashsoftware.trashSnooker.util.EventLogger;

import javax.sound.sampled.*;
import java.io.File;
import java.util.Locale;
//import javafx.scene.media.AudioClip

public class SoundFile {

    private String fmt;  // "mp3" or "wav"
    private final File file;
    private final AudioFormat audioFormat;
    private final Clip clip;
    private final float defaultVolume;

    private SoundFile(File file, String fmt, float defaultVolume) throws Exception {
        this.file = file;
        this.defaultVolume = defaultVolume;

        AudioInputStream audioInputStream;
        if ("wav".equals(fmt.toLowerCase(Locale.ROOT))) {
            audioInputStream = AudioSystem.getAudioInputStream(file);
            audioFormat = audioInputStream.getFormat();
        } else if ("mp3".equals(fmt.toLowerCase(Locale.ROOT))) {
            AudioInputStream in= AudioSystem.getAudioInputStream(file);
            AudioFormat baseFormat = in.getFormat();
            audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false);
            audioInputStream = AudioSystem.getAudioInputStream(audioFormat, in);
        } else {
            throw new RuntimeException("Unsupported format " + fmt);
        }

        DataLine.Info info = new DataLine.Info(Clip.class, audioFormat);
        clip = (Clip) AudioSystem.getLine(info);
        clip.open(audioInputStream);
    }

    public static SoundFile load(String path) {
        return load(path, 1.0f);
    }

    public static SoundFile load(String path, float defaultVolume) {
        File f = new File(path);

        String[] split = path.split("\\.");
        String fmt = split[split.length - 1];
        if (f.exists()) {
            try {
                return new SoundFile(f, fmt, defaultVolume);
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
