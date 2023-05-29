package trashsoftware.trashSnooker.fxml.drawing;

import javafx.application.Platform;

import java.util.TimerTask;

public class AnimationFrame extends TimerTask {

    final double frameTimeMs;
    final Runnable frame;
    boolean frameAlive;
    long lastFrameTime;
    long thisFrameTime;

    long cumulatedFrameCount;
    long lastSecondFrameCount;

    long fps;

    private long animationBeginTime;

    public AnimationFrame(Runnable frame, double frameTimeMs) {
        this.frame = frame;
        this.frameTimeMs = frameTimeMs;
        lastFrameTime = System.currentTimeMillis();
    }

    public void beginNewAnimation() {
        animationBeginTime = System.currentTimeMillis();
    }

    public long msSinceAnimationBegun() {
        return System.currentTimeMillis() - animationBeginTime;
    }

    public void run() {
        long t = System.currentTimeMillis();

        if (Math.floor(t / frameTimeMs) != Math.floor(lastFrameTime / frameTimeMs)) {
            if (!frameAlive) {
                frameAlive = true;
                thisFrameTime = t;
                if (thisFrameTime / 1000 != lastFrameTime / 1000) {
                    fps = cumulatedFrameCount - lastSecondFrameCount;
                    lastSecondFrameCount = cumulatedFrameCount;
                }

                cumulatedFrameCount++;

                Platform.runLater(() -> {
                    frame.run();
                    frameAlive = false;
                    lastFrameTime = t;
                });
            }
        }
    }

    public double lastAnimationFrameMs() {
        return thisFrameTime - lastFrameTime;
    }

    public int getCurrentFps() {
        return (int) fps;
    }
}
