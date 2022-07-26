package trashsoftware.trashSnooker.core.scoreResult;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.recorder.GameReplay;
import trashsoftware.trashSnooker.util.Util;

import java.util.Map;
import java.util.TreeMap;

public class SnookerScoreFactory implements ScoreFactory {
    
    @Override
    public int byteLength() {
        return SnookerScoreResult.BYTE_LENGTH;
    }

    @Override
    public ScoreResult fromBytes(GameReplay replay, byte[] bytes) {
        TreeMap<Ball, Integer> singlePole = new TreeMap<>();
        // 这个还原很jb扯
        Ball[] allBalls = replay.getAllBalls();
        Ball redBallRep = allBalls[0];
        int nRedBalls = allBalls.length - 7;
        int redsPots = bytes[24] & 0xff;
        if (redsPots > 0) singlePole.put(redBallRep, redsPots);
        for (int i = 2; i <= 7; i++) {
            int pos = i + 23;
            Ball ball = allBalls[nRedBalls + i - 2];
            int nPots = bytes[pos] & 0xff;
            if (nPots > 0) singlePole.put(ball, nPots);
        }
        System.out.println(singlePole);
        
        return new SnookerScoreResult(
                Util.bytesToInt32(bytes, 4),
                Util.bytesToInt32(bytes, 8),
                Util.bytesToInt32(bytes, 12),
                Util.bytesToInt32(bytes, 16),
                Util.bytesToInt32(bytes, 20),
                bytes[0] & 0xff,
                singlePole
        );
    }
}
