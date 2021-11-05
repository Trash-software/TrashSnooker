package trashsoftware.trashSnooker.util.db;

import java.util.Collections;
import java.util.List;

public abstract class EntireGameRecord {

    private final EntireGameTitle title;
    private final List<? extends FrameRecord> frameRecords;

    EntireGameRecord(EntireGameTitle title, List<? extends FrameRecord> frameRecords) {
        this.title = title;
        this.frameRecords = frameRecords;
        Collections.sort(frameRecords);
    }
    
    public int[] getP1P2WinsCount() {
        int[] res = new int[2];
        for (FrameRecord fr : frameRecords) {
            if (fr.winnerName.equals(title.player1Name)) {
                res[0]++;
            } else {
                res[1]++;
            }
        }
        return res;
    }

    public EntireGameTitle getTitle() {
        return title;
    }

    public List<? extends FrameRecord> getFrameRecords() {
        return frameRecords;
    }

    public static class Snooker extends EntireGameRecord {

        Snooker(EntireGameTitle title, List<? extends FrameRecord> frameRecords) {
            super(title, frameRecords);
        }
    }

    public static class ChineseEight extends EntireGameRecord {

        ChineseEight(EntireGameTitle title, List<FrameRecord> frameRecords) {
            super(title, frameRecords);
        }
    }

    public static class SidePocket extends EntireGameRecord {

        SidePocket(EntireGameTitle title, List<FrameRecord> frameRecords) {
            super(title, frameRecords);
        }
    }
}
