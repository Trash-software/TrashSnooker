package trashsoftware.trashSnooker.audio;

import trashsoftware.trashSnooker.res.SoundFile;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import javax.sound.sampled.*;
import java.io.IOException;

public class AudioPlayer {

    private final SoundFile soundFile;
    private final SoundInfo soundInfo;
    private boolean playing = false;

    public AudioPlayer(SoundFile soundFile, SoundInfo soundInfo) {
        this.soundFile = soundFile;
        this.soundInfo = soundInfo;
    }

    public void play() throws IOException, UnsupportedAudioFileException {
        Clip clip = soundFile.getClip();
        
        float volume = (float) ConfigLoader.getInstance().getDouble("effectVolume", 1.0);
        volume *= soundFile.getDefaultVolume();
        
        if (soundInfo != null) {
            float dynamicVolume = (float) soundInfo.getVolume();
            volume *= dynamicVolume;
        }
        
        volume = Math.max(0, Math.min(1, volume));
        float expVol = (float) (20 * Math.log10(volume));
        FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        control.setValue(expVol);
        
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    public void pause() {
        playing = false;
    }

    public void terminate() throws IOException {
        playing = false;
        close();
//        if (runAfterFinished != null)
//            runAfterFinished.run();
    }

    public void close() throws IOException {
//        audioInputStream.close();
//        dataLine.drain();
//        dataLine.close();
    }
}
