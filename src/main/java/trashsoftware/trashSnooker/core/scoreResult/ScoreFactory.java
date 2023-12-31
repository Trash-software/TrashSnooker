package trashsoftware.trashSnooker.core.scoreResult;

import trashsoftware.trashSnooker.recorder.GameReplay;

public interface ScoreFactory {
    
    int byteLength();
    
    ScoreResult fromBytes(GameReplay replay, byte[] bytes);
}
