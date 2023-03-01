package trashsoftware.trashSnooker.core.numberedGames.sidePocket;

import trashsoftware.trashSnooker.core.InGamePlayer;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;

public class SidePocketPlayer extends NumberedBallPlayer {

    private int target;

    public SidePocketPlayer(InGamePlayer playerPerson) {
        super(playerPerson);
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public int getTarget() {
        return target;
    }
}
