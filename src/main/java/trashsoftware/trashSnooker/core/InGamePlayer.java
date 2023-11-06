package trashsoftware.trashSnooker.core;

import javafx.scene.layout.Pane;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.InventoryManager;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.CueBrand;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.fxml.FastGameView;
import trashsoftware.trashSnooker.util.DataLoader;

public class InGamePlayer {

    private final PlayerPerson playerPerson;
    //    private final PersonRecord personRecord;
//    private final Cue breakCue;
//    private final Cue playCue;
    private final PlayerType playerType;
    private final GameRule gameRule;
    private final int playerNumber;
    private final double handFeelEffort;

    private final CueSelection cueSelection;

    public InGamePlayer(PlayerPerson playerPerson,
                        PlayerType playerType,
                        InventoryManager inventory,  // not null only if 生涯模式的玩家
                        GameRule gameRule,
                        int playerNumber,
                        double handFeelEffort) {
        this.playerPerson = playerPerson;
//        this.personRecord = PersonRecord.loadRecord(playerPerson.getPlayerId());
        this.playerType = playerType;
        this.playerNumber = playerNumber;
        this.handFeelEffort = handFeelEffort;
        this.gameRule = gameRule;

        cueSelection = new CueSelection(null);
        if (inventory != null) {
            for (Cue cue : inventory.getAllCues()) {
                CueSelection.CueAndBrand cab = new CueSelection.CueAndBrand(cue.getBrand(), cue);
                cueSelection.getAvailableCues().add(cab);
            }
        } else {
            for (CueBrand cue : DataLoader.getInstance().getCues().values()) {
                if (!cue.privacy) {
                    CueSelection.CueAndBrand cab = new CueSelection.CueAndBrand(cue);
                    cueSelection.getAvailableCues().add(cab);
                }
            }
            for (CueBrand pri : playerPerson.getPrivateCues()) {
                cueSelection.getAvailableCues().add(new CueSelection.CueAndBrand(pri));
            }
            cueSelection.select(cueSelection.getAvailableCues().get(0));
        }
        FastGameView.selectSuggestedCue(cueSelection, gameRule, playerPerson);
    }

//    public InGamePlayer(PlayerPerson playerPerson, Cue cue, PlayerType playerType, int playerNumber,
//                        double handFeelEffort) {
//        this(playerPerson, cue, cue, playerType, playerNumber, handFeelEffort);
//    }

    public static InGamePlayer fromJson(JSONObject jsonObject) {
        DataLoader loader = DataLoader.getInstance();
        PlayerPerson person = loader.getPlayerPerson(jsonObject.getString("person"));

        GameRule gameRule = GameRule.valueOf(jsonObject.getString("gameRule"));
        
        PlayerType playerType = PlayerType.valueOf(jsonObject.getString("playerType"));
        
        String breakCueId = jsonObject.getString("breakCue");
        String playCueId = jsonObject.getString("playCue");
        
        Cue breakCue;
        Cue playCue;
        
        InventoryManager inventoryManager;
        // 严重警告：
        // 注意！！！
        // 只有生涯模式的玩家才是instanceId
        // 其余都是brandId
        if (CareerManager.getCurrentSave() == null || playerType == PlayerType.COMPUTER) {
            breakCue = Cue.createForFastGame(loader.getCueById(breakCueId));
            playCue = Cue.createForFastGame(loader.getCueById(playCueId));
            inventoryManager = null;
        } else {
            CareerManager careerManager = CareerManager.getInstance();
            breakCue = careerManager.getInventory().getCueByInstanceId(breakCueId);
            playCue = careerManager.getInventory().getCueByInstanceId(playCueId);
            inventoryManager = careerManager.getInventory();
        }
        System.out.println(breakCue);
        System.out.println(playCue);
        
        int number = jsonObject.getInt("playerNumber");
        double handFeelEffort = jsonObject.getDouble("handFeelEffort");

        return new InGamePlayer(
                person,
                playerType,
                inventoryManager,
                gameRule,
                number,
                handFeelEffort
        );
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        
//        String breakCueId;
//        String playCueId;
//        if (CareerManager.getCurrentSave() == null || playerType == PlayerType.COMPUTER) {
//            breakCueId = breakCue.getBrand().cueId;
//            playCueId = playCue.getBrand().cueId;
//        } else {
//            breakCueId = breakCue.getInstanceId();
//            playCueId = playCue.getInstanceId();
//        }

        object.put("person", playerPerson.getPlayerId());
//        object.put("breakCue", breakCueId);
//        object.put("playCue", playCueId);
        object.put("gameRule", gameRule.name());
        object.put("playerType", playerType.name());
        object.put("playerNumber", playerNumber);
        object.put("handFeelEffort", handFeelEffort);

        return object;
    }
    
    public void hideAllCues(Pane pane) {
//        playCue.getCueModel(pane).hide();
//        breakCue.getCueModel(pane).hide();
    }

    public CueSelection getCueSelection() {
        return cueSelection;
    }

//    public Cue getCurrentCue(Game<?, ?> game) {
//        if (game instanceof NeedBigBreak) {
//            if (((NeedBigBreak) game).isBreaking()) {
//                return breakCue;
//            }
//        }
//        return playCue;
//    }
    
    public boolean isHuman() {
        return playerType == PlayerType.PLAYER;
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

//    public Cue getBreakCue() {
//        return breakCue;
//    }
//
//    public Cue getPlayCue() {
//        return playCue;
//    }

//    public PersonRecord getPersonRecord() {
//        return personRecord;
//    }

    public PlayerPerson getPlayerPerson() {
        return playerPerson;
    }
}
