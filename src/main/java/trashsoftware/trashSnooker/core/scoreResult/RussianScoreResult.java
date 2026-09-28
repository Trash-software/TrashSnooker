package trashsoftware.trashSnooker.core.scoreResult;

import trashsoftware.trashSnooker.core.russian.RussianBall;
import trashsoftware.trashSnooker.util.Util;

import java.util.Map;

public class RussianScoreResult extends ScoreResult {
    public static final int BYTE_LENGTH = 40;

    private final int p1TotalScore;  // 是已经加过这一杆的分之后的
    private final int p2TotalScore;
    private final int p1AddedScore;
    private final int p2AddedScore;
    private final Map<RussianBall, Boolean> remBalls;
    private final int singlePoleBalls;

    public RussianScoreResult(int thinkTime,
                              int p1Score, int p2Score,
                              int p1AddedScore, int p2AddedScore,
                              int justCuedPlayerNum,
                              Map<RussianBall, Boolean> remBalls,
                              int singlePoleBalls) {
        super(thinkTime, justCuedPlayerNum);

        this.p1TotalScore = p1Score;
        this.p2TotalScore = p2Score;
        this.p1AddedScore = p1AddedScore;
        this.p2AddedScore = p2AddedScore;
        this.remBalls = remBalls;
        this.singlePoleBalls = singlePoleBalls;
    }

    @Override
    public byte[] toBytes() {
        byte[] res = new byte[BYTE_LENGTH];
        res[0] = (byte) justCuedPlayerNum;
        res[1] = (byte) singlePoleBalls;

        Util.int32ToBytes(thinkTime, res, 4);
        Util.int32ToBytes(p1TotalScore, res, 8);
        Util.int32ToBytes(p2TotalScore, res, 12);
        Util.int32ToBytes(p1AddedScore, res, 16);
        Util.int32ToBytes(p2AddedScore, res, 20);

        putBallToArray(remBalls, res, 24);
        return res;
    }

    private void putBallToArray(Map<RussianBall, Boolean> remBalls, byte[] arr, int resBeginIndex) {
        for (Map.Entry<RussianBall, Boolean> entry : remBalls.entrySet()) {
            int index = entry.getKey().getValue() + resBeginIndex;  // 这里不-1，要记0号球
            arr[index] = entry.getValue() ? (byte) 1 : (byte) 0;
        }
    }

    @Override
    public int getSinglePoleBallCount() {
        return singlePoleBalls;
    }

    public Map<RussianBall, Boolean> getRemBalls() {
        return remBalls;
    }

    public int getP1TotalScore() {
        return p1TotalScore;
    }

    public int getP2TotalScore() {
        return p2TotalScore;
    }

    public int getP1AddedScore() {
        return p1AddedScore;
    }

    public int getP2AddedScore() {
        return p2AddedScore;
    }
}
