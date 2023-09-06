package trashsoftware.trashSnooker.util.db;

import java.util.SortedMap;

public abstract class EntireGameRecord {

    protected final EntireGameTitle title;
    protected final SortedMap<Integer, PlayerFrameRecord[]> frameRecords;
    protected final SortedMap<Integer, Integer> frameDurations;
    private final int[] p1p2wins = new int[2];

    EntireGameRecord(EntireGameTitle title,
                     SortedMap<Integer, PlayerFrameRecord[]> frameRecords,
                     SortedMap<Integer, Integer> frameDurations) {
        this.title = title;
        this.frameRecords = frameRecords;
        this.frameDurations = frameDurations;
        
        calculateP1P2Wins();
    }

    /**
     * @see DBAccess#getMatchDetail(EntireGameTitle)
     */
    public int[][] totalBasicStats() {
        int[][] res = new int[2][12];
        for (PlayerFrameRecord[] oneFrame : frameRecords.values()) {
            for (int i = 0; i < 2; i++) {
                res[i][0] += oneFrame[i].basicPots[0];
                res[i][1] += oneFrame[i].basicPots[1];
                res[i][2] += oneFrame[i].basicPots[2];
                res[i][3] += oneFrame[i].basicPots[3];
                res[i][4] += oneFrame[i].basicPots[4];
                res[i][5] += oneFrame[i].basicPots[5];
                res[i][6] += oneFrame[i].basicPots[6];
                res[i][7] += oneFrame[i].basicPots[7];
                res[i][8] += oneFrame[i].basicPots[8];
                res[i][9] += oneFrame[i].basicPots[9];
                res[i][10] += oneFrame[i].basicPots[10];
                res[i][11] += oneFrame[i].basicPots[11];
            }
        }
        return res;
    }
    
    private void calculateP1P2Wins() {
        for (PlayerFrameRecord[] twoFrames : frameRecords.values()) {
            if (twoFrames[0].winnerName.equals(title.player1Id)) {
                p1p2wins[0]++;
            } else {
                p1p2wins[1]++;
            }
        }
    }

    public int[] getP1P2WinsCount() {
        return p1p2wins;
    }
    
    public boolean isFinished() {
        int won = (int) Math.ceil((double) title.totalFrames / 2);
        return p1p2wins[0] == won || p1p2wins[1] == won;
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
            int[][] res = new int[2][6];
            for (PlayerFrameRecord[] records : getFrameRecords().values()) {
                PlayerFrameRecord.Numbered p1r = (PlayerFrameRecord.Numbered) records[0];
                PlayerFrameRecord.Numbered p2r = (PlayerFrameRecord.Numbered) records[1];
                
                res[0][0] += p1r.clears[0];
                res[0][1] += p1r.clears[1];
                res[0][2] += p1r.clears[2];
                res[0][3] += p1r.clears[3];
                res[0][5] += p1r.clears[5];
                res[1][0] += p2r.clears[0];
                res[1][1] += p2r.clears[1];
                res[1][2] += p2r.clears[2];
                res[1][3] += p2r.clears[3];
                res[1][5] += p1r.clears[5];
                
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
