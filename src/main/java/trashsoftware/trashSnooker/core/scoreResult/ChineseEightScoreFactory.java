package trashsoftware.trashSnooker.core.scoreResult;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.recorder.GameReplay;
import trashsoftware.trashSnooker.util.Util;

import java.util.ArrayList;
import java.util.List;

public class ChineseEightScoreFactory implements ScoreFactory {
    
    public ChineseEightScoreFactory() {
        
    }
    
    @Override
    public int byteLength() {
        return ChineseEightScoreResult.BYTE_LENGTH;
    }

    @Override
    public ScoreResult fromBytes(GameReplay replay, byte[] bytes) {
        return new ChineseEightScoreResult(
                Util.bytesToInt32(bytes, 4),  // fixme: 显然不对
                bytes[0] & 0xff,
                readPlayerRems(replay, bytes, 4),
                readPlayerRems(replay, bytes, 12)
        );
    }

    private List<PoolBall> readPlayerRems(GameReplay replay, byte[] bytes, int beginIndex) {
        List<PoolBall> list = new ArrayList<>();
        for (int i = beginIndex; i < beginIndex + 8; i++) {
            int val = bytes[i] & 0xff;
            if (val == 0) break;
            Ball ball = replay.getBallByValue(val);
            list.add((PoolBall) ball);
        }
        return list;
    }
}
