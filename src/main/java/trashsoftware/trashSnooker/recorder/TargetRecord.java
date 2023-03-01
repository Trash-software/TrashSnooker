package trashsoftware.trashSnooker.recorder;

public class TargetRecord {
    
    public final int playerNum;
    public final int targetRep;
    public final boolean isSnookerFreeBall;
    
    public TargetRecord(int playerNum, 
                        int targetRep, 
                        boolean isSnookerFreeBall) {
        this.playerNum = playerNum;
        this.targetRep = targetRep;
        this.isSnookerFreeBall = isSnookerFreeBall;
    }
}
