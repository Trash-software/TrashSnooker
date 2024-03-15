package trashsoftware.trashSnooker.core;

public class BreakStats {
    
    public final int nBallsPot;
    public final int nBallsHitCushion;  // 碰库了的球个数
    public final int nBallTimesEnterBreakArea;  // 越过了开球线的球次
    
    BreakStats(int nBallsPot, int nBallsHitCushion, int nBallTimesEnterBreakArea) {
        this.nBallsPot = nBallsPot;
        this.nBallsHitCushion = nBallsHitCushion;
        this.nBallTimesEnterBreakArea = nBallTimesEnterBreakArea;
    }
}
