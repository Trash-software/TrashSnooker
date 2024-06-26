package trashsoftware.trashSnooker.audio;

import trashsoftware.trashSnooker.res.SoundFile;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AudioPlayer {

    private final SoundFile soundFile;
    private final SoundInfo soundInfo;
    private boolean playing = false;

    public AudioPlayer(SoundFile soundFile, SoundInfo soundInfo) {
        this.soundFile = soundFile;
        this.soundInfo = soundInfo;
    }

    public void playToStream(SoundRecorder recorder, int timeInMs) throws IOException {
        int[] soundBytes = toSoundValues();
        recorder.write(soundBytes, timeInMs);
    }

    private int[] toSoundValues() throws IOException {
        float volume = 1.0f;
        if (soundInfo != null) {
            float dynamicVolume = (float) soundInfo.getVolume();
            volume *= dynamicVolume;
        }

        volume = Math.max(0.00001f, Math.min(1, volume));
        float expVol = (float) (20 * Math.log10(volume));
        
        boolean bigEndian = soundFile.getAudioFormat().isBigEndian();
        
        AudioInputStream ais = soundFile.getAudioInputStream();
        List<Integer> out = new ArrayList<>();
        int read;
        byte[] buffer = new byte[8192];
        while ((read = ais.read(buffer)) >= 0) {
            // assume 16 bits 
            for (int i = 0; i < read; i += 2) {  // todo: db or value
                int val;
                if (bigEndian) {
                    val = ((buffer[i] & 0xff) << 8) | (buffer[i + 1] & 0xff);
                } else {
                    val = ((buffer[i + 1] & 0xff) << 8) | (buffer[i] & 0xff);
                }
                val = (int) (volume * val);
                out.add(val);
//                if (bigEndian) {
//                    buffer[i] = (byte) (val >> 8);
//                    buffer[i + 1] = (byte) val;
//                } else {
//                    buffer[i + 1] = (byte) (val >> 8);
//                    buffer[i] = (byte) val;
//                }
            }
//            outputStream.write(buffer, 0, read);
        }
        int[] res = new int[out.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = out.get(i);
        }
        return res;
    }

    public void play() throws IOException, UnsupportedAudioFileException {
        Clip clip = soundFile.getClip();

        float volume = (float) ConfigLoader.getInstance().getDouble("effectVolume", 1.0);
        volume *= soundFile.getDefaultVolume();

        if (soundInfo != null) {
            float dynamicVolume = (float) soundInfo.getVolume();
            volume *= dynamicVolume;
        }

        volume = Math.max(0.00001f, Math.min(1, volume));
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
