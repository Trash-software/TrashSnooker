package trashsoftware.trashSnooker.core;

public class InGamePlayer {

    private final PlayerPerson playerPerson;
    private final PersonRecord personRecord;
    private final Cue breakCue;
    private final Cue playCue;

    public InGamePlayer(PlayerPerson playerPerson, Cue breakCue, Cue playCue) {
        this.playerPerson = playerPerson;
        this.breakCue = breakCue;
        this.playCue = playCue;
        this.personRecord = PersonRecord.loadRecord(playerPerson.getName());
    }

    public InGamePlayer(PlayerPerson playerPerson, Cue cue) {
        this(playerPerson, cue, cue);
    }

    public Cue getCurrentCue(Game game) {
        if (game instanceof NeedBigBreak) {
            if (((NeedBigBreak) game).isBreaking()) {
                return breakCue;
            }
        }
        return playCue;
    }

    public Cue getBreakCue() {
        return breakCue;
    }

    public Cue getPlayCue() {
        return playCue;
    }

    public PersonRecord getPersonRecord() {
        return personRecord;
    }

    public PlayerPerson getPlayerPerson() {
        return playerPerson;
    }
}
