package trashsoftware.trashSnooker.recorder;

import javafx.beans.property.*;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.fxml.VideoExportView;

import java.io.File;
import java.io.IOException;

public class VideoCapture {
    
    public final long processStartTime = System.currentTimeMillis();
    public final int totalFrames;  // 视频的总帧数，与replay不一定一样
    
    final VideoExportView.ProgressUpdater updater;
    
    final GameReplay replay;
    final GameView view;
    final SnapshotParameters params = new SnapshotParameters();
    final VideoConverter.Params videoParams;
    final VideoConverter videoConverter;
    
    public VideoCapture(File outFile, 
                        GameReplay replay, 
                        VideoConverter.Params videoParams, 
                        int gapMsBtwCue,
                        VideoExportView.ProgressUpdater updater,
                        GameView view) throws IOException {
        params.setFill(Color.TRANSPARENT);
        this.videoParams = videoParams;
        this.replay = replay;
        this.updater = updater;
        this.view = view;
        
        int fps = videoParams.fps();
        
        int actualFrames = replay.getItem().nMovementFrames / replay.getFrameRate() * fps;
        int idleFrames = (replay.getItem().nCues - 1) * gapMsBtwCue / fps;  // 第一杆没有
        int cueAnimationFrames = replay.getItem().totalBeforeCueMs / fps;

//        System.out.printf("Act %d, idle %d, cue %d: \n", actualFrames, idleFrames, cueAnimationFrames);
        
        this.totalFrames = actualFrames + idleFrames + cueAnimationFrames + 1;  // 保险
        videoConverter = new VideoConverter(outFile, videoParams);
    }
    
    public void recordFrame(Node node) {
        WritableImage writableImage = node.snapshot(params, null);
        try {
            videoConverter.writeOneFrame(writableImage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public int getFps() {
        return videoConverter.getFps();
    }

    public VideoExportView.ProgressUpdater getUpdater() {
        return updater;
    }
    
    public void success() {
        try {
            videoConverter.finish();
            updater.end(true);
        } catch (IOException e) {
            updater.end(false);
            throw new RuntimeException(e);
        }
    }
    
    public void fail() {
        updater.end(false);
        try {
            videoConverter.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void interrupt() {
        view.closeWindowAction();
    }
    
    public boolean isFinished() {
        return videoConverter.isFinished();
    }
}
