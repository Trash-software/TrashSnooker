package trashsoftware.trashSnooker.recorder;

import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;

import java.io.IOException;

public interface GameRecorder {

    void setCompression(int compression);

    void setCompression(String compression);

    void recordCue(CueRecord cueRecord, TargetRecord thisTarget);

    void recordMovement(Movement movement);

    void recordScore(ScoreResult scoreResult);

    void recordNextTarget(TargetRecord nextTarget);

    void writeCueToStream();

    void startRecoding() throws IOException;

    void stopRecording(boolean normalFinish);

    void recordPositions() throws IOException;

    void writeCue(CueRecord cueRecord, Movement movement, TargetRecord thisTarget, TargetRecord nextTarget) throws IOException;

    void writeBallInHand() throws IOException;

    void writeBallInHandPlacement();
    
    boolean isFinished();
}
