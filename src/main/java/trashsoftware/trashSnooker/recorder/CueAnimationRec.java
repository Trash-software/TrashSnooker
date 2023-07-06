package trashsoftware.trashSnooker.recorder;

public class CueAnimationRec {
    
    private int beforeCueMs;  // 1倍速下出杆之前的时间
    private int afterCueMs;
    
    public CueAnimationRec(int beforeCueMs, int afterCueMs) {
        this.beforeCueMs = beforeCueMs;
        this.afterCueMs = afterCueMs;
    }
    
    public CueAnimationRec() {
        this(0, 0);
    }

    public void setAfterCueMs(int afterCueMs) {
        this.afterCueMs = afterCueMs;
    }

    public void setBeforeCueMs(int beforeCueMs) {
        this.beforeCueMs = beforeCueMs;
    }

    public int getAfterCueMs() {
        return afterCueMs;
    }

    public int getBeforeCueMs() {
        return beforeCueMs;
    }
}
