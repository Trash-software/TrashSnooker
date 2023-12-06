package trashsoftware.trashSnooker.core.scoreResult;

public abstract class ScoreResult {
    protected int thinkTime;
    protected int justCuedPlayerNum;
    
    public ScoreResult(int thinkTime, int justCuedPlayerNum) {
        this.thinkTime = thinkTime;
        this.justCuedPlayerNum = justCuedPlayerNum;
    }

    public abstract int getSinglePoleBallCount();

    public abstract byte[] toBytes();

    public int getJustCuedPlayerNum() {
        return justCuedPlayerNum;
    }
}
