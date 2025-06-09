package trashsoftware.trashSnooker.util.db;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PlayerFrameRecord implements Comparable<PlayerFrameRecord> {

    public final int frameIndex;
    public final int frameNumber;
    public final boolean frameRestarted;

    // attempts, success, 
    // long, longSuccess, 
    // defenses, defenseSuccesses, 
    // positions, positionSuccesses
    // rest, restSuccess
    // solves, solveSuccess
    public final int[] basicPots;
    public final @Nullable String winnerName;

    PlayerFrameRecord(int frameIndex,
                      int frameNumber,
                      boolean frameRestarted,
                      int[] basicPots,
                      @Nullable String winnerName) {
        this.frameIndex = frameIndex;
        this.basicPots = basicPots;
        this.winnerName = winnerName;
        this.frameNumber = frameNumber;
        this.frameRestarted = frameRestarted;
    }

    @Override
    public int compareTo(@NotNull PlayerFrameRecord o) {
        return Integer.compare(frameIndex, o.frameIndex);
    }

    public static class Snooker extends PlayerFrameRecord {

        public final int[] snookerScores;  // total score, highest, breaks50, breaks100, 147

        Snooker(int frameIndex, int frameNumber,
                boolean frameRestarted,
                int[] basicPots, @Nullable String winnerName, int[] snookerScores) {
            super(frameIndex, frameNumber, frameRestarted, basicPots, winnerName);
            this.snookerScores = snookerScores;
        }
    }

    public static class Numbered extends PlayerFrameRecord {

        public final int[] clears;  // breaks, break-pots, break-clear, continue-clear, highest

        Numbered(int frameIndex, int frameNumber,
                 boolean frameRestarted, int[] basicPots, @Nullable String winnerName, int[] clears) {
            super(frameIndex, frameNumber, frameRestarted, basicPots, winnerName);
            this.clears = clears;
        }
    }
}
