package trashsoftware.trashSnooker.core.numberedGames.sidePocket;

import trashsoftware.trashSnooker.core.InGamePlayer;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;

public class SidePocketPlayer extends NumberedBallPlayer {

    private int target;
    private boolean goldNine;

    public SidePocketPlayer(InGamePlayer playerPerson) {
        super(playerPerson);
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public void setGoldNine() {
        this.goldNine = true;
    }

    public boolean isGoldNine() {
        return goldNine;
    }
}
