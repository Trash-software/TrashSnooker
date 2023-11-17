package trashsoftware.trashSnooker.recorder;

import trashsoftware.trashSnooker.core.cue.Cue;

public class CueAnimationRec {
    
    private final Cue cue;
    private int beforeCueMs;  // 1倍速下出杆之前的时间
    private int afterCueMs;
    
    public CueAnimationRec(Cue cue, int beforeCueMs, int afterCueMs) {
        this.cue = cue;
        this.beforeCueMs = beforeCueMs;
        this.afterCueMs = afterCueMs;
    }
    
    public CueAnimationRec(Cue cue) {
        this(cue, 0, 0);
    }

    public void setAfterCueMs(int afterCueMs) {
        this.afterCueMs = afterCueMs;
    }

    public void setBeforeCueMs(int beforeCueMs) {
        this.beforeCueMs = beforeCueMs;
    }

    public Cue getCue() {
        return cue;
    }

    public int getAfterCueMs() {
        return afterCueMs;
    }

    public int getBeforeCueMs() {
        return beforeCueMs;
    }
}
