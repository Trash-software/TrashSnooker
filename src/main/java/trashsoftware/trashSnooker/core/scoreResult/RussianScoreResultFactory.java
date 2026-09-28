package trashsoftware.trashSnooker.core.scoreResult;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.core.russian.RussianBall;
import trashsoftware.trashSnooker.recorder.GameReplay;
import trashsoftware.trashSnooker.util.Util;

import java.util.LinkedHashMap;

public class RussianScoreResultFactory implements ScoreFactory {
    @Override
    public int byteLength() {
        return RussianScoreResult.BYTE_LENGTH;
    }

    @Override
    public ScoreResult fromBytes(GameReplay replay, byte[] bytes) {
        return new RussianScoreResult(
                Util.bytesToInt32(bytes, 4),
                Util.bytesToInt32(bytes, 8),
                Util.bytesToInt32(bytes, 12),
                Util.bytesToInt32(bytes, 16),
                Util.bytesToInt32(bytes, 20),
                bytes[0] & 0xff,
                readRems(replay, bytes, 24),
                bytes[1] & 0xff
        );
    }

    private LinkedHashMap<RussianBall, Boolean> readRems(GameReplay replay, byte[] bytes, int beginIndex) {
        LinkedHashMap<RussianBall, Boolean> res = new LinkedHashMap<>();
        for (int i = 0; i < 16; i++) {
            int index = i + beginIndex;
            boolean pot = bytes[index] == 1;
            Ball ball = replay.getBallByValue(i);
            res.put((RussianBall) ball, pot);
        }
        return res;
    }
}
