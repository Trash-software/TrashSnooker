package trashsoftware.trashSnooker.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.Arrays;
import java.util.Map;

public class SoundMixer {
    
    public static final float SAMPLE_RATE = 48000.0f;
    public static final int BITS = 16;
    public static final int MAX_VOL = (int) (Math.pow(2, BITS) / 2);
    public static final int BYTES = BITS / 8;
    private final Track[] tracks;
    private final int[] sequence;
    
    public static final Map<Character, Integer> MINOR_MAP = Map.of(
            'A', 0,
            'B', 2,
            'C', 3,
            'D', 5,
            'E', 7,
            'F', 8,
            'G', 10
    );
    
    public SoundMixer(boolean smoothEnd, Track... tracks) {
        this.tracks = tracks;
        
        int maxLength = 0;
        for (Track track : tracks) {
            double endLength = smoothEnd ? (1000 / track.hz) : 0;
            int nFrames = (int) ((track.lengthMs + endLength) * SAMPLE_RATE / 1000);
            if (nFrames > maxLength) maxLength = nFrames;
        }
        double convergence = MAX_VOL / 32.0;
        sequence = new int[maxLength]; 
        for (Track track : tracks) {
            double endLength = smoothEnd ? (1000 / track.hz) : 0;
            int nFrames = (int) (track.lengthMs * SAMPLE_RATE / 1000);
            int endFrames = (int) (endLength * SAMPLE_RATE / 1000);
            System.out.println(nFrames + ", " + endFrames);
            for (int i = 0; i < nFrames + endFrames; i++) {
                double vol = track.volume * MAX_VOL;
                if (track.volumeDecrease) {
                    if (i < nFrames) vol *= 1 - ((double) i / nFrames);
                    else vol = 0;
                }
                double x = i / SAMPLE_RATE;
                double y = Math.sin(x * 2 * Math.PI * track.hz) * vol;
                sequence[i] += y;
                if (i >= nFrames && Math.abs(y) <= convergence) {
                    break;
                }
            }
        }
//        System.out.println(sequence.length + Arrays.toString(sequence));
    }
    
    public static int[] toNum(String rep) {
        boolean sharp = rep.contains("#") || rep.contains("♯");
        boolean flat = rep.contains("b") || rep.contains("♭");

        int major = 0;
        char minorChar = 0;
        for (char c : rep.toCharArray()) {
            if (c >= '0' && c <= '8') {
                major = c - '0';
            } else if (c >= 'A' && c <= 'G') {
                minorChar = c;
            }
        }
        major = major - 4;
        if (minorChar >= 'C') major -= 1;
        
        int minor = MINOR_MAP.get(minorChar);
        if (sharp) minor += 1;
        if (flat) minor -= 1;
        
        return new int[]{major, minor};
    }
    
    public static double toHz(String rep) {
        int[] num = toNum(rep);
        return toHz(num[0], num[1]);
    }
    
    public static double toHz(int major, int minor) {
        // 0, 0 = A4
        double base = Math.pow(2, major);
        double times = base * Math.pow(2, (double) minor / 12);
        double std = 440.0;
        return std * times;
    }
    
    private byte[] calculateByteArray(double volMul) {
        byte[] bytes = new byte[sequence.length * BYTES];
        for (int i = 0; i < sequence.length; i++) {
            int y = (int) (sequence[i] * volMul);
            y = Math.min(Math.max(y, -MAX_VOL), MAX_VOL - 1);
            if (BITS == 8) bytes[i] = (byte) y;
            else if (BITS == 16) {
                bytes[i << 1] = (byte) (y >> 8);
                bytes[(i << 1) + 1] = (byte) y;
            }
        }
        return bytes;
    }
    
    public void play() {
        this.play(1.0);
    }
    
    public void play(double volMul) {
        byte[] bytes = calculateByteArray(volMul);
        try {
            AudioFormat af = new AudioFormat(SAMPLE_RATE, BITS, 1, true, true);
            SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
            sdl.open(af);
            sdl.start();
            sdl.write(bytes, 0, bytes.length);
            sdl.drain();
            sdl.stop();
            sdl.close();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }
    
    public static class Track {
        
        public final double hz;
        public final double lengthMs;
        public final double volume;
        public final boolean volumeDecrease;
        
        public Track(double hz, double lengthMs, double volume, boolean volumeDecrease) {
            this.hz = hz;
            this.lengthMs = lengthMs;
            this.volume = volume;
            this.volumeDecrease = volumeDecrease;
        }

        public Track(String tone, double lengthMs, double volume, boolean volumeDecrease) {
            this(toHz(tone), lengthMs, volume, volumeDecrease);
        }
    }
}
