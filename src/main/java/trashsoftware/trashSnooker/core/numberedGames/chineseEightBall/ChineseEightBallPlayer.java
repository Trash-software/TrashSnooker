package trashsoftware.trashSnooker.core.numberedGames.chineseEightBall;

import trashsoftware.trashSnooker.core.InGamePlayer;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;

import java.util.*;

public class ChineseEightBallPlayer extends NumberedBallPlayer {
    protected final Map<LetBall, Integer> letBalls = new TreeMap<>(
            Map.of(
                    LetBall.FRONT, 0,
                    LetBall.MID, 0,
                    LetBall.BACK, 0
            )
    );  // 被让的球
    protected final Map<LetBall, List<PoolBall>> ballsAlreadyLet = new TreeMap<>(
            Map.of(
                    LetBall.FRONT, new ArrayList<>(),
                    LetBall.MID, new ArrayList<>(),
                    LetBall.BACK, new ArrayList<>()
            )
    );
    private int ballRange = 0;  // 0=未选球，8=8，16=1~7，17=9~15

    public ChineseEightBallPlayer(InGamePlayer playerPerson, Map<LetBall, Integer> letBalls) {
        super(playerPerson);

        if (letBalls != null) {
            this.letBalls.putAll(letBalls);
        }
    }

    public Map<LetBall, Integer> getLettedBalls() {
        return letBalls;
    }

    public int getBallRange() {
        return ballRange;
    }

    public void setBallRange(int ballRange, int initScore) {
        this.ballRange = ballRange;
        if (initScore != -1) {
            this.score = initScore;
        }
    }
    
    public void forceSetScore(int score) {
        this.score = score;
    }
    
    public void letBall(LetBall letBall, PoolBall ball) {
        ballsAlreadyLet.get(letBall).add(ball);
    }
    
    public boolean letFulfilled(LetBall letBall) {
        return ballsAlreadyLet.get(letBall).size() == letBalls.get(letBall);
    }
}
