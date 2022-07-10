package trashsoftware.trashSnooker.core.numberedGames.chineseEightBall;

import trashsoftware.trashSnooker.core.InGamePlayer;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;

public class ChineseEightBallPlayer extends NumberedBallPlayer {
    private int ballRange = 0;  // 0=未选球，8=8，16=1~7，17=9~15

    public ChineseEightBallPlayer(InGamePlayer playerPerson) {
        super(playerPerson);
    }

    public void setBallRange(int ballRange) {
        this.ballRange = ballRange;
    }

    public int getBallRange() {
        return ballRange;
    }
}
