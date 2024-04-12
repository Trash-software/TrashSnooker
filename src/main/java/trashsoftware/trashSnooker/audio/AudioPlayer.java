package trashsoftware.trashSnooker.audio;

import trashsoftware.trashSnooker.res.WavFile;
import trashsoftware.trashSnooker.res.WavInfo;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class AudioPlayer {

    private final WavInfo wavInfo;
    private final SoundInfo soundInfo;
    private boolean playing = false;

    public AudioPlayer(WavInfo wavInfo, SoundInfo soundInfo) {
        this.wavInfo = wavInfo;
        this.soundInfo = soundInfo;
    }

    public void play() throws IOException, UnsupportedAudioFileException {
        Clip clip = wavInfo.getClip();
        
        float volume = (float) ConfigLoader.getInstance().getDouble("effectVolume", 1.0);
        volume *= wavInfo.getDefaultVolume();
        
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
