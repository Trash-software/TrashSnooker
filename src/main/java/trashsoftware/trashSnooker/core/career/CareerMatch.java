package trashsoftware.trashSnooker.core.career;

import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.person.PlayerPerson;
import trashsoftware.trashSnooker.core.career.championship.Championship;

public abstract class CareerMatch {

    protected EntireGame game;
    protected Runnable guiFinishCallback;
    protected Runnable guiAbortionCallback;

    public void saveMatch() {

    }

    public void saveAndExit() {

    }

    public Championship getChampionship() {
        return null;
    }

    public abstract void finish(PlayerPerson winnerPerson, int p1Wins, int p2Wins);

    public EntireGame getGame() {
        return game;
    }

    public void setGame(EntireGame game) {
        this.game = game;
    }

    public void setGuiCallback(Runnable guiCallback, Runnable guiAbortionCallback) {
        this.guiFinishCallback = guiCallback;
        this.guiAbortionCallback = guiAbortionCallback;
    }
}
