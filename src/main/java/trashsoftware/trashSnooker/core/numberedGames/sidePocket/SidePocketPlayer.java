package trashsoftware.trashSnooker.core.numberedGames.sidePocket;

import trashsoftware.trashSnooker.core.InGamePlayer;
import trashsoftware.trashSnooker.core.Player;

public class SidePocketPlayer extends Player {

    private int target;

    public SidePocketPlayer(int number, InGamePlayer playerPerson) {
        super(number, playerPerson);
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public int getTarget() {
        return target;
    }
}
