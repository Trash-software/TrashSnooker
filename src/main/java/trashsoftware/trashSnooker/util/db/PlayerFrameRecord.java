package trashsoftware.trashSnooker.util.db;

import org.jetbrains.annotations.NotNull;

public abstract class PlayerFrameRecord implements Comparable<PlayerFrameRecord> {
    
    public final int frameIndex;
    public final int[] basicPots;  // attempts, success, long, longSuccess, defenses, defenseSuccesses
    public final String winnerName;
    
    PlayerFrameRecord(int frameIndex, int[] basicPots, String winnerName) {
        this.frameIndex = frameIndex;
        this.basicPots = basicPots;
        this.winnerName = winnerName;
    }

    @Override
    public int compareTo(@NotNull PlayerFrameRecord o) {
        return Integer.compare(frameIndex, o.frameIndex);
    }
    
    public static class Snooker extends PlayerFrameRecord {
        
        public final int[] snookerScores;  // total score, highest, breaks50, breaks100, 147

        Snooker(int frameIndex, int[] basicPots, String winnerName, int[] snookerScores) {
            super(frameIndex, basicPots, winnerName);
            this.snookerScores = snookerScores;
        }
    }
}
