package trashsoftware.trashSnooker.fxml.drawing;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.control.Label;
import trashsoftware.trashSnooker.util.EventLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AnimationGameLoop extends AnimationTimer implements GameLoop {
    final Runnable frame;
    final Label fpsLabel;
    boolean frameAlive;
    long lastFrameTime;
    long thisFrameTime;

    long cumulatedFrameCount;
    long lastSecondFrameCount;

    long fps;
    long fpsSpike;  // 上一秒内最低的帧数
    long lastFrameNano;
    long longestNanoBtw1s;
    private long animationBeginTime;

    Timer eachSecondRefresher = new Timer();
    
    public AnimationGameLoop(Runnable frame, Label fpsLabel) {
        this.frame = frame;
        this.fpsLabel = fpsLabel;
        lastFrameTime = System.currentTimeMillis();
    }

    public void beginNewAnimation() {
        animationBeginTime = System.currentTimeMillis();
    }

    public long msSinceAnimationBegun() {
        return System.currentTimeMillis() - animationBeginTime;
    }

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public void start() {
        super.start();

        eachSecondRefresher.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> updatePerSecond());
            }
        }, 0, 1000);
    }

    @Override
    public void stop() {
        super.stop();
        
        eachSecondRefresher.cancel();
    }

    @Override
    public void handle(long now) {
        long t = System.currentTimeMillis();

        if (!frameAlive) {
            frameAlive = true;
            lastFrameTime = thisFrameTime;
            thisFrameTime = t;
            cumulatedFrameCount++;
            
            try {
                frame.run();
            } catch (Exception e) {
                EventLogger.error(e);
            }
            frameAlive = false;
            
            long frameNano = now - lastFrameNano;
            if (frameNano < 1e9 && frameNano > longestNanoBtw1s) {
                longestNanoBtw1s = frameNano;
            }
            lastFrameNano = now;
        }
    }

    public double lastAnimationFrameMs() {
        return thisFrameTime - lastFrameTime;
    }
    
    void updatePerSecond() {
        if (longestNanoBtw1s > 0) {
            fpsSpike = (long) (1e9 / longestNanoBtw1s);    
            longestNanoBtw1s = 0;
        }
        fps = cumulatedFrameCount - lastSecondFrameCount;
        lastSecondFrameCount = cumulatedFrameCount;
        
        fpsLabel.setText(fps + " - " + fpsSpike);
    }
}
