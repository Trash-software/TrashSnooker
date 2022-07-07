package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.recorder.GameReplay;
import trashsoftware.trashSnooker.util.PersonRecord;

public class InGamePlayer {

    private final PlayerPerson playerPerson;
    private final PersonRecord personRecord;
    private final Cue breakCue;
    private final Cue playCue;
    private final PlayerType playerType;

    public InGamePlayer(PlayerPerson playerPerson, Cue breakCue, Cue playCue, PlayerType playerType) {
        this.playerPerson = playerPerson;
        this.breakCue = breakCue;
        this.playCue = playCue;
        this.personRecord = PersonRecord.loadRecord(playerPerson.getName());
        this.playerType = playerType;
    }

    public InGamePlayer(PlayerPerson playerPerson, Cue cue, PlayerType playerType) {
        this(playerPerson, cue, cue, playerType);
    }

    public Cue getCurrentCue(Game game) {
        if (game instanceof NeedBigBreak) {
            if (((NeedBigBreak) game).isBreaking()) {
                return breakCue;
            }
        }
        return playCue;
    }

    public PlayerType getPlayerType() {
        return playerType;
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
