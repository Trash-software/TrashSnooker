package trashsoftware.trashSnooker.fxml.drawing;

import javafx.application.Platform;
import trashsoftware.trashSnooker.fxml.GameView;

public class AnimationFrame {
    
    Runnable frame;
    final double frameTimeMs;
    boolean frameAlive;
    long lastFrameTime;
    long thisFrameTime;
    long beginTime;
    
    long cumulatedFrameCount;
    long lastSecondFrameCount;
    
    long lastUpdateTime;
    long fps;
    
    double curAvgFrameTimeMs = GameView.frameTimeMs;  // 不重要
    int skippedFrames;
    
    public AnimationFrame(Runnable frame, double frameTimeMs) {
        this.frame = frame;
        this.frameTimeMs = frameTimeMs;
        beginTime = System.currentTimeMillis();
        lastFrameTime = beginTime;
    }
    
    public void run() {
        long t = System.currentTimeMillis();
        
        if (Math.floor(t / frameTimeMs) != Math.floor(lastUpdateTime / frameTimeMs)) {
            if (frameAlive) {
                skippedFrames++;
            } else {
                lastUpdateTime = t;
                frameAlive = true;
                thisFrameTime = System.currentTimeMillis();
                if (thisFrameTime / 1000 != lastFrameTime / 1000) {

                    lastFrameTime = thisFrameTime;
                    fps = cumulatedFrameCount - lastSecondFrameCount;
                    lastSecondFrameCount = cumulatedFrameCount;

                    curAvgFrameTimeMs = 1000.0 / fps;
//                System.out.println("Avg frame rate: " + fps);
                }

                cumulatedFrameCount++;

                Platform.runLater(() -> {
                    frame.run();
                    frameAlive = false;
                    skippedFrames = 0;
                });
            }
        }
    }
    
    public double lastAnimationFrameMs() {
        return curAvgFrameTimeMs;
//        return frameTimeMs;
    }
    
    public int getCurrentFps() {
        return (int) fps;
    }

    public int getSkippedFrames() {
        return skippedFrames;
    }
}
