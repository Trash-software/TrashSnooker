package trashsoftware.trashSnooker.core.numberedGames.sidePocket;

import trashsoftware.trashSnooker.core.InGamePlayer;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;

public class SidePocketBallPlayer extends NumberedBallPlayer {

    private int target;

    public SidePocketBallPlayer(int number, InGamePlayer playerPerson, boolean breakingPlayer) {
        super(number, playerPerson, breakingPlayer);
    }

    public SidePocketBallPlayer(int number, InGamePlayer playerPerson) {
        this(number, playerPerson, false);
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public int getTarget() {
        return target;
    }
}
