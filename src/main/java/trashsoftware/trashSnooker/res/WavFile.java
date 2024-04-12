package trashsoftware.trashSnooker.res;

import trashsoftware.trashSnooker.util.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;

public class WavFile {

    public static final int DEFAULT_SAMPLE_RATE = 22050;

//    private File file;
    private int[][] data;
    private int numFrames;
    private int numChannels;
    private int audioFormat;
    private int bitsPerSample;
    private int blockAlign;
    private long sampleRate;
    private long byteRate;
    private Util.IntList[] writeBuffer;

    private int lastOverFrames;  // 上一次造波时额外的帧数，用于校正
    private boolean lastWaveGoesDown;  // 正弦波上一次结束是向下

    private WavFile(File file) {
//        this.file = file;
    }
    
    private WavFile() {
        
    }

    public static WavFile createNew(File outFile, long sampleRate, int numChannels) {
        WavFile wavFile = new WavFile(outFile);
        wavFile.sampleRate = sampleRate;
        wavFile.bitsPerSample = 16;
        wavFile.numChannels = numChannels;
        wavFile.byteRate = (long) numChannels * sampleRate * wavFile.bitsPerSample / 8;
        wavFile.blockAlign = numChannels * wavFile.bitsPerSample / 8;
        wavFile.audioFormat = 1;
        wavFile.writeBuffer = new Util.IntList[numChannels];
        for (int i = 0; i < numChannels; ++i) {
            wavFile.writeBuffer[i] = new Util.IntList();
        }
        return wavFile;
    }

    public static WavFile fromFile(File file) throws IOException {
        return fromFile(file, true);
    }

    public static WavFile fromFile(File file, boolean readData) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return fromStream(fis, readData);
        }
    }

    public static WavFile fromStream(InputStream is) throws IOException {
        return fromStream(is, true);
    }

    public static WavFile fromStream(InputStream is, boolean readData) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(is)) {
            WavFile wavFile = new WavFile();
            String chunkDescriptor = Util.readString(bis, 4);
            if (!chunkDescriptor.endsWith("RIFF")) {
                throw new IllegalArgumentException("Not RIFF");
            }
            long chunkSize = Util.readInt4Little(bis);
            String fmtFlag = Util.readString(bis, 4);
            if (!fmtFlag.endsWith("WAVE")) {
                throw new IllegalArgumentException("Not WAVE");
            }
            String subChunk1Id = Util.readString(bis, 4);
            if (!subChunk1Id.endsWith("fmt ")) {
                throw new IllegalArgumentException("Not fmt");
            }
            long subChunk1Size = Util.readInt4Little(bis);
            wavFile.audioFormat = Util.readInt2Little(bis);
            wavFile.numChannels = Util.readInt2Little(bis);
            wavFile.sampleRate = Util.readInt4Little(bis);
            wavFile.byteRate = Util.readInt4Little(bis);
            wavFile.blockAlign = Util.readInt2Little(bis);
            wavFile.bitsPerSample = Util.readInt2Little(bis);

            String subChunk2Id;
            long subChunk2Size;
            while (!(subChunk2Id = Util.readString(bis, 4)).endsWith("data")) {
                subChunk2Size = Util.readInt4Little(bis);
                System.out.println(subChunk2Id + ": " + subChunk2Size);
                if (bis.skip(subChunk2Size) != subChunk2Size) {
                    throw new IOException();
                }
            }
            subChunk2Size = Util.readInt4Little(bis);  // data chunk size
            if (subChunk2Size == 0) throw new IllegalArgumentException("Empty data chunk");

            wavFile.numFrames = (int) (subChunk2Size / (wavFile.bitsPerSample / 8) / wavFile.numChannels);
//            System.out.println(subChunk2Id + subChunk2Size);

            if (readData) {
                wavFile.data = new int[wavFile.numChannels][wavFile.numFrames];
                // todo
//                wavFile.endFreq = new double[wavFile.numChannels];
//                wavFile.lastEndFreq = new double[wavFile.numChannels];
                for (int f = 0; f < wavFile.numFrames; ++f) {
                    for (int c = 0; c < wavFile.numChannels; ++c) {
                        if (wavFile.bitsPerSample == 8) {
                            wavFile.data[c][f] = bis.read();
                        } else if (wavFile.bitsPerSample == 16) {
                            wavFile.data[c][f] = Util.readInt2Little(bis);
                        }
                    }
                }
            }
            return wavFile;
        }
    }

    public static int[] makeSimpleWaveData(double freq, double durationMs, int bitsPerSample, long sampleRate,
                                           double volPercent) {
        int halfTotal = bitsPerSample == 8 ? 128 : 32768;
        int nFrames = (int) Math.round(durationMs * sampleRate / 1000.0);
        double waveLength = sampleRate / freq;
        double multiplier = Math.PI / (waveLength / 2);

        int[] res = new int[nFrames];

        for (int frame = 0; frame < nFrames; ++frame) {
            double y = Math.sin(frame * multiplier) * halfTotal * volPercent / 100.0;
            res[frame] = (int) y;
        }

        return res;
    }

    public boolean hasData() {
        return data != null;
    }

//    public File getFile() {
//        return file;
//    }
//
//    public void setFile(File file) {
//        this.file = file;
//    }
    
    public void overlay(WavFile other) {
        if (sampleRate != other.sampleRate) {
            throw new RuntimeException("Sample rate not the same");
        }
        if (numChannels != other.numChannels) {
            throw new RuntimeException("Number of channels do not match");
        }
        if (other.data == null) {
            throw new RuntimeException("Cannot overlay from an empty wave");
        }
        int length = data == null ? 0 : data[0].length;
        int oLength = other.data[0].length;
        
        int[][] result;
        if (data != null && length >= oLength) {
            result = data;
        } else {
            result = new int[numChannels][oLength];
        }
        
        for (int c = 0; c < numChannels; c++) {
            for (int i = 0; i < result[c].length; ++i) {
                int a = i < length ? data[c][i] : 0;
                int b = i < oLength ? other.data[c][i] : 0;
                result[c][i] = a + b;
            }
        }
        data = result;
        numFrames = data[0].length;
    }

    public int getBitsPerSample() {
        return bitsPerSample;
    }

    public int getNumChannels() {
        return numChannels;
    }

    public int getNumFrames() {
        return data == null ? 0 : data[0].length;
    }

    public long getSampleRate() {
        return sampleRate;
    }

    public long getByteRate() {
        return byteRate;
    }

    public double getLengthSeconds() {
        return (double) numFrames / sampleRate;
    }

    /**
     * 如果是新建的wav，必须在{@link WavFile#flushBuffer()}后使用。
     *
     * @param channelIndex 声道号，从0开始
     * @return 数据
     */
    public int[] getChannel(int channelIndex) {
        return data[channelIndex];
    }

    public void setData(int[] data, int channelIndex) {
        if (this.data == null) {
            this.data = new int[numChannels][];
            this.data[channelIndex] = data;
        }
    }

    public void putData(int[] data, int channelIndex) {
        for (int datum : data) {
            writeBuffer[channelIndex].add(datum);
        }
    }

    public void flushBuffer() {
        if (data != null) {
            throw new RuntimeException("Wave file already has data.");
        }
        data = new int[numChannels][numFrames];
        for (int c = 0; c < numChannels; ++c) {
            for (int f = 0; f < numFrames; ++f) {
                data[c][f] = writeBuffer[c].get(f);
            }
        }
        writeBuffer = null;
    }

    @Override
    public String toString() {
        return "WavFile{" +
                ", numFrames=" + numFrames +
                ", numChannels=" + numChannels +
                ", audioFormat=" + audioFormat +
                ", bitsPerSample=" + bitsPerSample +
                ", blockAlign=" + blockAlign +
                ", sampleRate=" + sampleRate +
                ", byteRate=" + byteRate +
//                ",\n data=" + Arrays.toString(data[0]) +
                '}';
    }
    
    static class IntList extends ArrayList<Integer> {
        
    }
}
