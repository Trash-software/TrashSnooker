package trashsoftware.trashSnooker.core;

public class InGamePlayer {

    private final PlayerPerson playerPerson;
    private final Cue breakCue;
    private final Cue playCue;

    public InGamePlayer(PlayerPerson playerPerson, Cue breakCue, Cue playCue) {
        this.playerPerson = playerPerson;
        this.breakCue = breakCue;
        this.playCue = playCue;
    }

    public InGamePlayer(PlayerPerson playerPerson, Cue cue) {
        this(playerPerson, cue, cue);
    }

    public Cue getBreakCue() {
        return breakCue;
    }

    public Cue getPlayCue() {
        return playCue;
    }

    public PlayerPerson getPlayerPerson() {
        return playerPerson;
    }
}
