package trashsoftware.trashSnooker.fxml.drawing;

import javafx.animation.AnimationTimer;

public class GameLoop extends AnimationTimer {

    final Runnable frame;
    boolean frameAlive;
    long lastFrameTime;
    long thisFrameTime;

    long cumulatedFrameCount;
    long lastSecondFrameCount;

    long fps;

    private long animationBeginTime;

    public GameLoop(Runnable frame) {
        this.frame = frame;
        lastFrameTime = System.currentTimeMillis();
    }

    public void beginNewAnimation() {
        animationBeginTime = System.currentTimeMillis();
    }

    public long msSinceAnimationBegun() {
        return System.currentTimeMillis() - animationBeginTime;
    }

    @Override
    public void handle(long now) {
        long t = System.currentTimeMillis();

        if (!frameAlive) {
            frameAlive = true;
            thisFrameTime = t;
            if (thisFrameTime / 1000 != lastFrameTime / 1000) {
                fps = cumulatedFrameCount - lastSecondFrameCount;
                lastSecondFrameCount = cumulatedFrameCount;
            }

            cumulatedFrameCount++;

            frame.run();
            frameAlive = false;
            lastFrameTime = t;
        }
    }

    public double lastAnimationFrameMs() {
        return thisFrameTime - lastFrameTime;
    }

    public int getCurrentFps() {
        return (int) fps;
    }
}
