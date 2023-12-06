package trashsoftware.trashSnooker.core.scoreResult;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.recorder.GameReplay;
import trashsoftware.trashSnooker.util.Util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class NineBallScoreResultFactory implements ScoreFactory {
    @Override
    public int byteLength() {
        return NineBallScoreResult.BYTE_LENGTH;
    }

    @Override
    public ScoreResult fromBytes(GameReplay replay, byte[] bytes) {
        return new NineBallScoreResult(
                Util.bytesToInt32(bytes, 4),
                bytes[0] & 0xff,
                readRems(replay, bytes, 8),
                bytes[1] & 0xff
        );
    }

    private LinkedHashMap<PoolBall, Boolean> readRems(GameReplay replay, byte[] bytes, int beginIndex) {
        LinkedHashMap<PoolBall, Boolean> res = new LinkedHashMap<>();
        for (int i = 0; i < 9; i++) {
            int index = i + beginIndex;
            boolean pot = bytes[index] == 1;
            Ball ball = replay.getBallByValue(i + 1);
            res.put((PoolBall) ball, pot);
        }
        return res;
    }
}
