package trashsoftware.trashSnooker.recorder;

import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import trashsoftware.trashSnooker.audio.SoundRecorder;
import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.fxml.VideoExportView;
import trashsoftware.trashSnooker.res.SoundFile;

import java.io.File;
import java.io.IOException;

public class VideoCapture {

    public final long processStartTime = System.currentTimeMillis();
    public int totalFrames;  // 视频导出部分的帧数

    public final int beginCueIndex;
    public final int endCueIndex;
    private final int gapMsBtwCue;

    final VideoExportView.ProgressUpdater updater;

    final GameReplay replay;
    final GameView view;
    final SnapshotParameters params = new SnapshotParameters();
    final VideoConverter.Params videoParams;
    final VideoConverter videoConverter;

    final SoundRecorder soundRecorder;

    public VideoCapture(File outFile,
                        GameReplay replay,
                        VideoConverter.Params videoParams,
                        int gapMsBtwCue,
                        int beginCueIndex,
                        int endCueIndex,
                        VideoExportView.ProgressUpdater updater,
                        GameView view) throws IOException {
        this.videoParams = videoParams;
        this.replay = replay;
        this.updater = updater;
        this.view = view;
        this.beginCueIndex = beginCueIndex;
        this.endCueIndex = endCueIndex;
        this.gapMsBtwCue = gapMsBtwCue;

//        params.setFill(Color.TRANSPARENT);

        readReplayContent();

        videoConverter = new VideoConverter(outFile, videoParams);
        soundRecorder = new SoundRecorder(SoundFile.STD_FMT);
    }

    public void setupScreenshotParams(Rectangle2D viewport) {
        videoConverter.setCrop(viewport);
//        params.setViewport(viewport);
    }

    public VideoConverter.Params getVideoParams() {
        return videoParams;
    }

    private void readReplayContent() {
        replay.skipCues(beginCueIndex);

        int fps = videoParams.fps();
        int idleFrames = gapMsBtwCue / fps;
//        totalFrames = -idleFrames;
        while (replay.getCueIndex() < endCueIndex) {
            if (!replay.loadNext()) {
                break;
            }

            if (replay.getCurrentFlag() == ActualRecorder.FLAG_CUE) {
                totalFrames += idleFrames;
                totalFrames += replay.getMovement().getNFrames() * fps / replay.getFrameRate();
                totalFrames += replay.getAnimationRec().getBeforeCueMs() / fps;
            }
        }

        replay.revertTo(beginCueIndex);
    }

    public void recordFrame(Node node) {
        WritableImage writableImage = node.snapshot(params, null);
        try {
            videoConverter.writeOneFrame(writableImage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getGapMsBtwCue() {
        return gapMsBtwCue;
    }

    public int getBeginCueIndex() {
        return beginCueIndex;
    }

    public int getEndCueIndex() {
        return endCueIndex;
    }

    public int getFps() {
        return videoConverter.getFps();
    }

    public VideoExportView.ProgressUpdater getUpdater() {
        return updater;
    }

    public SoundRecorder getSoundRecorder() {
        return soundRecorder;
    }

    public void success() {
        try {
//            videoConverter.finishRecordAndMuxAudio(soundRecorder);
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
            // 不管audio了
            videoConverter.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void interrupt() {
        view.closeWindowAction();
    }

    public boolean isFinished() {
        return videoConverter.isCaptureFinished();
    }
}
