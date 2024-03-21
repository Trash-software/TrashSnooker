package trashsoftware.trashSnooker.core.scoreResult;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.snooker.SnookerBall;
import trashsoftware.trashSnooker.util.Util;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class SnookerScoreResult extends ScoreResult {
    public static final int BYTE_LENGTH = 32;
    
    private final int p1TotalScore;  // 是已经加过这一杆的分之后的
    private final int p2TotalScore;
    private final int p1AddedScore;
    private final int p2AddedScore;
    private final SortedMap<Ball, Integer> singlePoleMap;  // 一定得是SortedMap
    private final byte[] singlePoles = new byte[7];
    
    public SnookerScoreResult(int thinkTime,
                              int p1Score, int p2Score,
                              int p1AddedScore, int p2AddedScore,
                              int justCuedPlayerNum,
                              SortedMap<Ball, Integer> singlePoleMap) {
        super(thinkTime, justCuedPlayerNum);
        
        this.p1TotalScore = p1Score;
        this.p2TotalScore = p2Score;
        this.p1AddedScore = p1AddedScore;
        this.p2AddedScore = p2AddedScore;
        this.singlePoleMap = singlePoleMap;
    }

    public int getP1AddedScore() {
        return p1AddedScore;
    }

    public int getP1TotalScore() {
        return p1TotalScore;
    }

    public int getP2AddedScore() {
        return p2AddedScore;
    }

    public int getP2TotalScore() {
        return p2TotalScore;
    }

    @Override
    public int getSinglePoleBallCount() {
        return singlePoleMap.values().stream().reduce(0, Integer::sum);
    }

    public SortedMap<Ball, Integer> getSinglePoleMap() {
        return singlePoleMap;
    }

    public int getSinglePoleScore() {
        int singlePoleScore = 0;
        for (Map.Entry<Ball, Integer> entry : singlePoleMap.entrySet()) {
            singlePoleScore += entry.getKey().getValue() * entry.getValue();
        }
        return singlePoleScore;
    }

    /**
     * Part         Pos   Len   Description
     * PlayerNum    0     1               
     * Reserved     1     3
     * ThinkTime    4     4
     * 
     * @return bytes
     */
    @Override
    public byte[] toBytes() {
        byte[] res = new byte[BYTE_LENGTH];
        res[0] = (byte) justCuedPlayerNum;
        Util.int32ToBytes(thinkTime, res, 4);
        Util.int32ToBytes(p1TotalScore, res, 8);
        Util.int32ToBytes(p2TotalScore, res, 12);
        Util.int32ToBytes(p1AddedScore, res, 16);
        Util.int32ToBytes(p2AddedScore, res, 20);

        for (Map.Entry<Ball, Integer> entry : singlePoleMap.entrySet()) {
            SnookerBall ball = (SnookerBall) entry.getKey();
            if (ball.isGold()) {
                res[31] = 1;
            } else {
                singlePoles[ball.getValue() - 1] = entry.getValue().byteValue();
            }
        }
        System.arraycopy(singlePoles, 0, res, 24, singlePoles.length);
        
        return res;
    }
}
