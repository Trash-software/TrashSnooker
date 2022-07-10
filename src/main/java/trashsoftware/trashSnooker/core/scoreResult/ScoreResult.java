package trashsoftware.trashSnooker.core.scoreResult;

public abstract class ScoreResult {
    protected int thinkTime;
    
    public ScoreResult(int thinkTime) {
        this.thinkTime = thinkTime;
    }
    
    public abstract byte[] toBytes();
}
