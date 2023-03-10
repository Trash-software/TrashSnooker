package trashsoftware.trashSnooker.core;

import javafx.scene.layout.Pane;
import org.json.JSONObject;
import trashsoftware.trashSnooker.util.DataLoader;

public class InGamePlayer {

    private final PlayerPerson playerPerson;
    //    private final PersonRecord personRecord;
    private final Cue breakCue;
    private final Cue playCue;
    private final PlayerType playerType;
    private final int playerNumber;
    private final double handFeelEffort;

    public InGamePlayer(PlayerPerson playerPerson,
                        Cue breakCue,
                        Cue playCue,
                        PlayerType playerType,
                        int playerNumber,
                        double handFeelEffort) {
        this.playerPerson = playerPerson;
        this.breakCue = breakCue;
        this.playCue = playCue;
//        this.personRecord = PersonRecord.loadRecord(playerPerson.getPlayerId());
        this.playerType = playerType;
        this.playerNumber = playerNumber;
        this.handFeelEffort = handFeelEffort;
    }

    public InGamePlayer(PlayerPerson playerPerson, Cue cue, PlayerType playerType, int playerNumber,
                        double handFeelEffort) {
        this(playerPerson, cue, cue, playerType, playerNumber, handFeelEffort);
    }

    public static InGamePlayer fromJson(JSONObject jsonObject) {
        DataLoader loader = DataLoader.getInstance();
        PlayerPerson person = loader.getPlayerPerson(jsonObject.getString("person"));
        Cue breakCue = loader.getCueById(jsonObject.getString("breakCue"));
        Cue playCue = loader.getCueById(jsonObject.getString("playCue"));
        PlayerType playerType = PlayerType.valueOf(jsonObject.getString("playerType"));
        int number = jsonObject.getInt("playerNumber");
        double handFeelEffort = jsonObject.getDouble("handFeelEffort");

        return new InGamePlayer(
                person,
                breakCue,
                playCue,
                playerType,
                number,
                handFeelEffort
        );
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();

        object.put("person", playerPerson.getPlayerId());
        object.put("breakCue", breakCue.getCueId());
        object.put("playCue", playCue.getCueId());
        object.put("playerType", playerType.name());
        object.put("playerNumber", playerNumber);
        object.put("handFeelEffort", handFeelEffort);

        return object;
    }
    
    public void hideAllCues(Pane pane) {
        playCue.getCueModel(pane).hide();
        breakCue.getCueModel(pane).hide();
    }

    public Cue getCurrentCue(Game<?, ?> game) {
        if (game instanceof NeedBigBreak) {
            if (((NeedBigBreak) game).isBreaking()) {
                return breakCue;
            }
        }
        return playCue;
    }

    public double getHandFeelEffort() {
        return handFeelEffort;
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
