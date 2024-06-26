package trashsoftware.trashSnooker.audio;

import trashsoftware.trashSnooker.res.SoundFile;

import javax.sound.sampled.AudioFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Soundtrack recorder for {@link trashsoftware.trashSnooker.recorder.VideoCapture}
 */
public class SoundRecorder {
    
    public final AudioFormat audioFormat;
    private final SortedMap<Integer, AudioSegment> segmentMap = new TreeMap<>();
    
    public SoundRecorder(AudioFormat audioFormat) {
        this.audioFormat = audioFormat;
    }
    
    public void write(int[] audioValues, int msPosition) {
        int framePos = (int) (msPosition * audioFormat.getFrameRate() / 1000);
        segmentMap.put(framePos, new AudioSegment(audioValues, framePos));
    }
    
    public byte[] toByteArray() {
        assert audioFormat.getSampleSizeInBits() == 16;
        assert audioFormat.getChannels() == SoundFile.CHANNELS;
        
        var last = segmentMap.lastEntry();
        int nFrames = last.getValue().framePosition + last.getValue().values.length;
        int[] intOut = new int[nFrames * SoundFile.CHANNELS];
        
        for (AudioSegment as : segmentMap.values()) {
            int[] val = as.values;
            for (int i = 0; i < val.length; i++) {
                intOut[as.framePosition * SoundFile.CHANNELS + i] += val[i];
            }
        }
        
        boolean bigEndian = audioFormat.isBigEndian();
        byte[] byteOut = new byte[nFrames * SoundFile.CHANNELS * 2];
        for (int i = 0; i < nFrames; i++) {
            for (int ch = 0; ch < SoundFile.CHANNELS; ch++) {
                int val = intOut[i * SoundFile.CHANNELS + ch];
                int byteIndex = (i * SoundFile.CHANNELS + ch) * 2;
                if (bigEndian) {
                    // Big-endian: MSB is stored first
                    byteOut[byteIndex] = (byte) (val >> 8);
                    byteOut[byteIndex + 1] = (byte) val;
                } else {
                    // Little-endian: LSB is stored first
                    byteOut[byteIndex + 1] = (byte) (val >> 8);
                    byteOut[byteIndex] = (byte) val;
                }
            }
        }
        return byteOut;
    }
    
    private static class AudioSegment {
        int[] values;
        int framePosition;
        
        AudioSegment(int[] values, int framePosition) {
            this.values = values;
            this.framePosition = framePosition;
        }
    }
}
