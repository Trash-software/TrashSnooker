package trashsoftware.trashSnooker.recorder;

import trashsoftware.trashSnooker.core.Game;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;

import java.io.IOException;

public class InvalidRecorder implements GameRecorder {

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public void startRecoding() throws IOException {
        
    }

    @Override
    public void stopRecording(boolean normalFinish) {
        
    }

    @Override
    public void deleteRecord() {
        
    }

    @Override
    public void recordPositions() throws IOException {

    }

    @Override
    public void writeCue(CueRecord cueRecord, Movement movement, TargetRecord thisTarget, TargetRecord nextTarget) throws IOException {

    }

    @Override
    public void writeBallInHand() throws IOException {

    }

    @Override
    public void writeBallInHandPlacement() {
        
    }

    @Override
    public void setCompression(int compression) {
        
    }

    @Override
    public void setCompression(String compression) {

    }

    @Override
    public void recordCue(CueRecord cueRecord, TargetRecord thisTarget) {

    }

    @Override
    public void recordMovement(Movement movement) {

    }

    @Override
    public void recordScore(ScoreResult scoreResult) {

    }

    @Override
    public void recordNextTarget(TargetRecord nextTarget) {

    }

    @Override
    public void writeCueToStream() {
        
    }
}
