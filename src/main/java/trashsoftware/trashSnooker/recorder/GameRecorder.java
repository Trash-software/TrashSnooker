package trashsoftware.trashSnooker.recorder;

import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;

import java.io.IOException;

public interface GameRecorder {

    void setCompression(int compression);

    void setCompression(String compression);

    void recordCue(CueRecord cueRecord, TargetRecord thisTarget) throws RecordingException;

    void recordMovement(Movement movement) throws RecordingException;

    void recordScore(ScoreResult scoreResult) throws RecordingException;
    
    void recordCueAnimation(CueAnimationRec cueAnimationRec) throws RecordingException;

    void recordNextTarget(TargetRecord nextTarget) throws RecordingException;

    void writeCueToStream() throws RecordingException;

    void startRecoding() throws IOException;

    void stopRecording(boolean normalFinish);
    
    void deleteRecord();
    
    void abort();

    void recordPositions() throws IOException;

    void writeCue(CueRecord cueRecord, Movement movement, TargetRecord thisTarget, TargetRecord nextTarget, 
                  CueAnimationRec animationRec) throws IOException;

    void writeBallInHand() throws IOException;

    void writeBallInHandPlacement();
    
    boolean isFinished();
}
