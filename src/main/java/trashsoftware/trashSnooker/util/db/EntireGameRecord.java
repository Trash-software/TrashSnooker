package trashsoftware.trashSnooker.util.db;

import java.util.SortedMap;

public abstract class EntireGameRecord {

    protected final EntireGameTitle title;
    protected final SortedMap<Integer, PlayerFrameRecord[]> frameRecords;
    protected final SortedMap<Integer, Integer> frameDurations;

    EntireGameRecord(EntireGameTitle title,
                     SortedMap<Integer, PlayerFrameRecord[]> frameRecords,
                     SortedMap<Integer, Integer> frameDurations) {
        this.title = title;
        this.frameRecords = frameRecords;
        this.frameDurations = frameDurations;
    }

    public int[][] totalBasicStats() {
        int[][] res = new int[2][12];
        for (PlayerFrameRecord[] oneFrame : frameRecords.values()) {
            res[0][0] += oneFrame[0].basicPots[0];
            res[0][1] += oneFrame[0].basicPots[1];
            res[0][2] += oneFrame[0].basicPots[2];
            res[0][3] += oneFrame[0].basicPots[3];
            res[0][4] += oneFrame[0].basicPots[4];
            res[0][5] += oneFrame[0].basicPots[5];
            res[0][6] += oneFrame[0].basicPots[6];
            res[0][7] += oneFrame[0].basicPots[7];
            res[0][8] += oneFrame[0].basicPots[8];
            res[0][9] += oneFrame[0].basicPots[9];
            res[0][10] += oneFrame[0].basicPots[10];
            res[0][11] += oneFrame[0].basicPots[11];

            res[1][0] += oneFrame[1].basicPots[0];
            res[1][1] += oneFrame[1].basicPots[1];
            res[1][2] += oneFrame[1].basicPots[2];
            res[1][3] += oneFrame[1].basicPots[3];
            res[1][4] += oneFrame[1].basicPots[4];
            res[1][5] += oneFrame[1].basicPots[5];
            res[1][6] += oneFrame[1].basicPots[6];
            res[1][7] += oneFrame[1].basicPots[7];
            res[1][8] += oneFrame[1].basicPots[8];
            res[1][9] += oneFrame[1].basicPots[9];
            res[1][10] += oneFrame[1].basicPots[10];
            res[1][11] += oneFrame[1].basicPots[11];
        }
        return res;
    }

    public int[] getP1P2WinsCount() {
        int[] res = new int[2];
        for (PlayerFrameRecord[] twoFrames : frameRecords.values()) {
            if (twoFrames[0].winnerName.equals(title.player1Name)) {
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

    public SortedMap<Integer, PlayerFrameRecord[]> getFrameRecords() {
        return frameRecords;
    }

    public SortedMap<Integer, Integer> getFrameDurations() {
        return frameDurations;
    }

    public static class Snooker extends EntireGameRecord {

        Snooker(EntireGameTitle title,
                SortedMap<Integer, PlayerFrameRecord[]> frameRecords,
                SortedMap<Integer, Integer> frameDurations) {
            super(title, frameRecords, frameDurations);
        }

        /**
         * @see PlayerFrameRecord.Snooker#snookerScores
         */
        public int[][] totalScores() {
            int[][] res = new int[2][5];
            for (PlayerFrameRecord[] records : getFrameRecords().values()) {
                PlayerFrameRecord.Snooker p1r = (PlayerFrameRecord.Snooker) records[0];
                PlayerFrameRecord.Snooker p2r = (PlayerFrameRecord.Snooker) records[1];

                res[0][0] += p1r.snookerScores[0];
                if (res[0][1] < p1r.snookerScores[1]) {
                    res[0][1] = p1r.snookerScores[1];
                }
                res[0][2] += p1r.snookerScores[2];
                res[0][3] += p1r.snookerScores[3];
                res[0][4] += p1r.snookerScores[4];

                res[1][0] += p2r.snookerScores[0];
                if (res[1][1] < p2r.snookerScores[1]) {
                    res[1][1] = p2r.snookerScores[1];
                }
                res[1][2] += p2r.snookerScores[2];
                res[1][3] += p2r.snookerScores[3];
                res[1][4] += p2r.snookerScores[4];
            }
            return res;
        }
    }
    
    public abstract static class NumberedBall extends EntireGameRecord {

        NumberedBall(EntireGameTitle title, 
                     SortedMap<Integer, PlayerFrameRecord[]> frameRecords, 
                     SortedMap<Integer, Integer> frameDurations) {
            super(title, frameRecords, frameDurations);
        }
        
        public int[][] totalScores() {
            int[][] res = new int[2][5];
            for (PlayerFrameRecord[] records : getFrameRecords().values()) {
                PlayerFrameRecord.Numbered p1r = (PlayerFrameRecord.Numbered) records[0];
                PlayerFrameRecord.Numbered p2r = (PlayerFrameRecord.Numbered) records[1];
                
                res[0][0] += p1r.clears[0];
                res[0][1] += p1r.clears[1];
                res[0][2] += p1r.clears[2];
                res[0][3] += p1r.clears[3];
                res[1][0] += p2r.clears[0];
                res[1][1] += p2r.clears[1];
                res[1][2] += p2r.clears[2];
                res[1][3] += p2r.clears[3];
                
                if (res[0][4] < p1r.clears[4]) {
                    res[0][4] = p1r.clears[4];
                }
                if (res[1][4] < p2r.clears[4]) {
                    res[1][4] = p2r.clears[4];
                }
            }
            return res;
        }
    }

    public static class ChineseEight extends NumberedBall {

        ChineseEight(EntireGameTitle title,
                     SortedMap<Integer, PlayerFrameRecord[]> frameRecords,
                     SortedMap<Integer, Integer> frameDurations) {
            super(title, frameRecords, frameDurations);
        }
    }

    public static class SidePocket extends NumberedBall {

        SidePocket(EntireGameTitle title,
                   SortedMap<Integer, PlayerFrameRecord[]> frameRecords,
                   SortedMap<Integer, Integer> frameDurations) {
            super(title, frameRecords, frameDurations);
        }
    }
}
