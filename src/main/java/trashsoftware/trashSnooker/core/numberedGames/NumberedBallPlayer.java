package trashsoftware.trashSnooker.core.numberedGames;

import trashsoftware.trashSnooker.core.InGamePlayer;
import trashsoftware.trashSnooker.core.Player;

public abstract class NumberedBallPlayer extends Player {

    protected int playTimes = 0;  // 这一局打了几次球
    private final boolean breakingPlayer;

    public NumberedBallPlayer(int number, InGamePlayer playerPerson, boolean breakingPlayer) {
        super(number, playerPerson);

        this.breakingPlayer = breakingPlayer;
        if (breakingPlayer) playTimes = 1;
    }

    public boolean isBreakingPlayer() {
        return breakingPlayer;
    }

    public int getPlayTimes() {
        return playTimes;
    }

    public void incrementPlayTimes() {
        playTimes++;
    }
}
