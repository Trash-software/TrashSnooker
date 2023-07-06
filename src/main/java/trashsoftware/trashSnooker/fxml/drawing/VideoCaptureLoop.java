package trashsoftware.trashSnooker.fxml.drawing;

import javafx.application.Platform;
import trashsoftware.trashSnooker.recorder.VideoCapture;

public class VideoCaptureLoop implements GameLoop {

    private final VideoCapture videoCapture;
    private final Runnable frame;
    final int frameRate;
    int frameCount;
    double frameTimeMs;
    long animationBeginTime;
    double relativeTime;

    private final Thread loop;
    private boolean running;

    public VideoCaptureLoop(VideoCapture videoCapture, Runnable frame, int frameRate) {
        this.videoCapture = videoCapture;
        this.frame = frame;
        this.frameRate = frameRate;
        this.frameTimeMs = 1000.0 / frameRate;

        loop = new Thread(this::oneFrameWrapper);
        loop.setDaemon(true);
    }

    private void oneFrameWrapper() {
        Platform.runLater(() -> {
            frame.run();
            relativeTime += frameTimeMs;
            frameCount++;
            videoCapture.getUpdater().update(frameCount, videoCapture.totalFrames);
//            videoCapture.progressProperty().set((double) frameCount / videoCapture.totalFrames);
            if (running) {
                oneFrameWrapper();
            }
        });
    }

    @Override
    public void start() {
        running = true;
        loop.start();
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public void beginNewAnimation() {
        animationBeginTime = (long) relativeTime;
    }

    @Override
    public double lastAnimationFrameMs() {
        return frameTimeMs;
    }

    @Override
    public long msSinceAnimationBegun() {
        return (long) (relativeTime - animationBeginTime);
    }
}
