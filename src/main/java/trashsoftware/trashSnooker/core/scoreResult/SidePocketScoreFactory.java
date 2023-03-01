package trashsoftware.trashSnooker.core.scoreResult;

import trashsoftware.trashSnooker.recorder.GameReplay;

public class SidePocketScoreFactory implements ScoreFactory {
    @Override
    public int byteLength() {
        return 0;
    }

    @Override
    public ScoreResult fromBytes(GameReplay replay, byte[] bytes) {
        return null;
    }
}
