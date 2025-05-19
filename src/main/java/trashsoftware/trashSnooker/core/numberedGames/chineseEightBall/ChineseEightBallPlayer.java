package trashsoftware.trashSnooker.core.numberedGames.chineseEightBall;

import trashsoftware.trashSnooker.core.InGamePlayer;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;

import java.util.HashMap;
import java.util.Map;

import static trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.LetBall.BACK;
import static trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.LetBall.FRONT;

public class ChineseEightBallPlayer extends NumberedBallPlayer {
    protected final Map<LetBall, Integer> letBalls = new HashMap<>(
            Map.of(
                    LetBall.FRONT, 0,
                    LetBall.MID, 0,
                    LetBall.BACK, 0
            )
    );  // 被让的球
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
}
