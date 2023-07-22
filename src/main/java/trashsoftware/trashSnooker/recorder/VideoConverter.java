package trashsoftware.trashSnooker.recorder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.WritableImage;
import org.jcodec.api.SequenceEncoder;
//import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

import static org.jcodec.common.model.ColorSpace.RGB;

public class VideoConverter {

    private final SequenceEncoder encoder;
    FileOutputStream baseOut;
    SeekableByteChannel channel;

    private final File outFile;

    BufferedImage buffer;
    BufferedImage cropBuffer;
    BufferedImage scaledOutput;
    private Rectangle2D crop;
    final Picture picture;
    private final Params params;
    private int topBorder;
    private int leftBorder;
    private int centerWidth;  // 视频中央有画面部分的长宽
    private int centerHeight;
    private boolean finished;

    public VideoConverter(File outFile, Params params) throws IOException {
        this.params = params;
        this.outFile = outFile;
        this.baseOut = new FileOutputStream(outFile);
        channel = new FileChannelWrapper(baseOut.getChannel());
        encoder = new SequenceEncoder(channel, Rational.R1(params.fps()), Format.MOV, Codec.H264, null);

        picture = Picture.create(params.width(), params.height(), RGB);
        fillDstBackground(picture);
    }

    public void writeOneFrame(WritableImage image) throws IOException {
        if (finished) return;
        if (buffer == null) {
            calculateScales(image.getWidth(), image.getHeight());
            buffer = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_RGB);
            if (crop != null) {
                cropBuffer = new BufferedImage((int) crop.getWidth(), (int) crop.getHeight(), BufferedImage.TYPE_INT_RGB);
                System.out.printf("Crop size: (%f, %f) %d, %d\n",
                        crop.getMinX(), crop.getMinY(),
                        cropBuffer.getWidth(), cropBuffer.getHeight());
            }
            scaledOutput = new BufferedImage(centerWidth, centerHeight, BufferedImage.TYPE_INT_RGB);
        }

        long t1 = System.currentTimeMillis();

        BufferedImage frame = SwingFXUtils.fromFXImage(image, buffer);
        BufferedImage resizeSrc;
        if (crop != null) {
            cropImage(frame);
            resizeSrc = cropBuffer;
        } else {
            resizeSrc = frame;
        }
        
        resizeImage(resizeSrc, scaledOutput);

        fromBufferedImage(scaledOutput, picture);
        long t2 = System.currentTimeMillis();
        encoder.encodeNativeFrame(picture);
        long t3 = System.currentTimeMillis();
//        System.out.println("snapshot: " + (t2 - t1) + ", " + (t3 - t2));
    }

    private static File createImageFile(String name) {
        File dir = new File("user/replays/snapshot");
        if (!dir.exists()) dir.mkdirs();

        return new File(dir, name + ".png");
    }

    public void setCrop(Rectangle2D crop) {
        this.crop = crop;
    }

    private void calculateScales(double srcW, double srcH) {
        double wScale = params.width() / srcW;
        double hScale = params.height() / srcH;

        double scale = Math.min(wScale, hScale);
        leftBorder = (int) ((params.width() - srcW * scale) / 2);
        topBorder = (int) ((params.height() - srcH * scale) / 2);
        centerWidth = (int) (scale * srcW);
        centerHeight = (int) (scale * srcH);

        System.out.println("Size: " + centerWidth + " " + centerHeight + " " + scale + " " +
                leftBorder + " " + topBorder + " / " + srcW + " " + srcH);
    }
    
    private void cropImage(BufferedImage src) {
        int minX = (int) crop.getMinX();
        int minY = (int) crop.getMinY();
        
        for (int y = 0; y < cropBuffer.getHeight(); y++) {
            for (int x = 0; x < cropBuffer.getWidth(); x++) {
                cropBuffer.setRGB(x, y, src.getRGB(x + minX, y + minY));
            }
        }
    }

    private void fillDstBackground(Picture dst) {
        byte[] dstData = dst.getPlaneData(0);
        Arrays.fill(dstData, (byte) -96);
    }

    private void resizeImage(BufferedImage src, BufferedImage dst) {
        java.awt.Image scaled = src.getScaledInstance(centerWidth, centerHeight, BufferedImage.SCALE_AREA_AVERAGING);
        dst.getGraphics().drawImage(scaled, 0, 0, null);
    }

    private void fromBufferedImage(BufferedImage src, Picture dst) {
        byte[] dstData = dst.getPlaneData(0);

//        int off = 0;
        for (int cy = 0; cy < centerHeight; cy++) {
            int y = cy + topBorder;
            int rowIndex = y * params.width() * 3;
            for (int cx = 0; cx < centerWidth; cx++) {
                int x = cx + leftBorder;
                int rgb1 = src.getRGB(cx, cy);

                int index = rowIndex + x * 3;

                dstData[index] = (byte) (((rgb1 >> 16) & 0xff) - 128);
                dstData[index + 1] = (byte) (((rgb1 >> 8) & 0xff) - 128);
                dstData[index + 2] = (byte) ((rgb1 & 0xff) - 128);
            }
        }
    }

    public void finish() throws IOException {
        System.out.println("Finished!");
        encoder.finish();
        channel.close();
        baseOut.close();
        finished = true;
    }

    public boolean isFinished() {
        return finished;
    }

    public File getOutFile() {
        return outFile;
    }

    public int getFps() {
        return params.fps();
    }

    public Params getParams() {
        return params;
    }

    public record Params(int fps, int width, int height, Area area) {
    }

    public enum Area {
        GAME_PANE,
        FULL;

        @Override
        public String toString() {
            return App.getStrings().getString(Util.toLowerCamelCase("EXPORT_AREA_" + name()));
        }
    }
}
