package trashsoftware.trashSnooker.core.numberedGames.chineseEightBall;

import trashsoftware.trashSnooker.core.InGamePlayer;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;

public class ChineseEightBallPlayer extends NumberedBallPlayer {
    private int ballRange = 0;  // 0=未选球，8=8，16=1~7，17=9~15

    public ChineseEightBallPlayer(int number, InGamePlayer playerPerson, boolean breakingPlayer) {
        super(number, playerPerson, breakingPlayer);
    }

    public ChineseEightBallPlayer(int number, InGamePlayer playerPerson) {
        this(number, playerPerson, false);
    }

    public void setBallRange(int ballRange) {
        this.ballRange = ballRange;
    }

    public int getBallRange() {
        return ballRange;
    }
}
