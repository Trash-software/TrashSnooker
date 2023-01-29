package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.util.PersonRecord;

public class InGamePlayer {

    private final PlayerPerson playerPerson;
//    private final PersonRecord personRecord;
    private final Cue breakCue;
    private final Cue playCue;
    private final PlayerType playerType;
    private final int playerNumber;

    public InGamePlayer(PlayerPerson playerPerson,
                        Cue breakCue,
                        Cue playCue,
                        PlayerType playerType,
                        int playerNumber) {
        this.playerPerson = playerPerson;
        this.breakCue = breakCue;
        this.playCue = playCue;
//        this.personRecord = PersonRecord.loadRecord(playerPerson.getPlayerId());
        this.playerType = playerType;
        this.playerNumber = playerNumber;
    }

    public InGamePlayer(PlayerPerson playerPerson, Cue cue, PlayerType playerType, int playerNumber) {
        this(playerPerson, cue, cue, playerType, playerNumber);
    }

    public Cue getCurrentCue(Game game) {
        if (game instanceof NeedBigBreak) {
            if (((NeedBigBreak) game).isBreaking()) {
                return breakCue;
            }
        }
        return playCue;
    }

    public int getPlayerNumber() {
        return playerNumber;
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

//    public PersonRecord getPersonRecord() {
//        return personRecord;
//    }

    public PlayerPerson getPlayerPerson() {
        return playerPerson;
    }
}
