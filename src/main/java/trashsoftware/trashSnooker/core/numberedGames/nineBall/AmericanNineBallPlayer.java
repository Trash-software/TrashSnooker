package trashsoftware.trashSnooker.core.numberedGames.nineBall;

import trashsoftware.trashSnooker.core.InGamePlayer;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;

public class AmericanNineBallPlayer extends NumberedBallPlayer {

    private int target;
    private boolean goldNine;

    public AmericanNineBallPlayer(InGamePlayer playerPerson) {
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
