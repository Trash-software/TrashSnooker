package trashsoftware.trashSnooker.core.scoreResult;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ChineseEightScoreResult extends ScoreResult {
    public static final int BYTE_LENGTH = 20;
    
    private final List<PoolBall> p1Rems;
    private final List<PoolBall> p2Rems;
    private final int singlePoleBalls;
    
    public ChineseEightScoreResult(int thinkTime,
                                   int justCuedPlayerNum,
                                   List<PoolBall> p1Rems,
                                   List<PoolBall> p2Rems,
                                   int singlePoleBalls) {
        super(thinkTime, justCuedPlayerNum);
        
        p1Rems.sort(Ball::compareTo);
        p2Rems.sort(Ball::compareTo);
        this.p1Rems = p1Rems;
        this.p2Rems = p2Rems;
        
        this.singlePoleBalls = singlePoleBalls;
    }

    @Override
    public byte[] toBytes() {
        byte[] res = new byte[BYTE_LENGTH];
        res[0] = (byte) justCuedPlayerNum;
        res[1] = (byte) singlePoleBalls; 
        
        putBallToArray(p1Rems, res, 4);
        putBallToArray(p2Rems, res, 12);
        
        return res;
    }

    private void putBallToArray(List<PoolBall> balls, byte[] arr, int resBeginIndex) {
        int index = resBeginIndex;
        for (Ball ball : balls) {
            int val = ball.getValue();
            arr[index++] = (byte) val;
        }
    }

    @Override
    public int getSinglePoleBallCount() {
        return singlePoleBalls;
    }

    public List<PoolBall> getP1Rems() {
        return p1Rems;
    }

    public List<PoolBall> getP2Rems() {
        return p2Rems;
    }
}
