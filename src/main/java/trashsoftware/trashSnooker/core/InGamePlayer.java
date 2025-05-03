package trashsoftware.trashSnooker.core;

import javafx.scene.layout.Pane;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.InventoryManager;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.CueBrand;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.fxml.FastGameView;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;

public class InGamePlayer {

    private final PlayerPerson playerPerson;
    //    private final PersonRecord personRecord;
//    private final Cue breakCue;
//    private final Cue playCue;
    private final PlayerType playerType;
    private final GameRule gameRule;
    private final int playerNumber;  // 1 or 2
    private final double handFeelEffort;

    private final CueSelection cueSelection;
    private LetScoreOrBall letScoreOrBall;

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
            // fast game
            for (CueBrand pri : playerPerson.getPrivateCues()) {
                cueSelection.getAvailableCues().add(new CueSelection.CueAndBrand(pri));
            }
            for (CueBrand cue : DataLoader.getInstance().getCues().values()) {
                if (playerPerson.getSex() != PlayerPerson.Sex.F && cue.getCueId().toLowerCase().startsWith("girl")) {
                    continue;
                }
                if (cue.available && !cueSelection.hasThisBrand(cue)) {
                    CueSelection.CueAndBrand cab = new CueSelection.CueAndBrand(cue);
                    cueSelection.getAvailableCues().add(cab);
                }
            }
            cueSelection.select(cueSelection.getAvailableCues().get(0));
        }
        FastGameView.selectSuggestedCue(cueSelection, gameRule, playerPerson);
    }

    public static InGamePlayer fromJson(JSONObject jsonObject) {
        DataLoader loader = DataLoader.getInstance();
        PlayerPerson person = loader.getPlayerPerson(jsonObject.getString("person"));

        GameRule gameRule = GameRule.valueOf(jsonObject.getString("gameRule"));
        
        PlayerType playerType = PlayerType.valueOf(jsonObject.getString("playerType"));
        
        InventoryManager inventoryManager;
        // 严重警告：
        // 注意！！！
        // 只有生涯模式的玩家才是instanceId
        // 其余都是brandId
        if (CareerManager.getCurrentSave() == null || playerType == PlayerType.COMPUTER) {
            inventoryManager = null;
        } else {
            CareerManager careerManager = CareerManager.getInstance();
            inventoryManager = careerManager.getInventory();
        }
        
        int number = jsonObject.getInt("playerNumber");
        double handFeelEffort = jsonObject.getDouble("handFeelEffort");

        InGamePlayer igp = new InGamePlayer(
                person,
                playerType,
                inventoryManager,
                gameRule,
                number,
                handFeelEffort
        );

        if (jsonObject.has("let")) {
            try {
                JSONObject letObj = jsonObject.getJSONObject("let");
                igp.letScoreOrBall = LetScoreOrBall.fromJson(letObj);
            } catch (JSONException e) {
                EventLogger.warning(e);
            }
        }
        
        return igp;
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();

        object.put("person", playerPerson.getPlayerId());
        object.put("gameRule", gameRule.name());
        object.put("playerType", playerType.name());
        object.put("playerNumber", playerNumber);
        object.put("handFeelEffort", handFeelEffort);
        if (letScoreOrBall != null) {
            object.put("let", letScoreOrBall.toJson());
        }

        return object;
    }

    public void setLetScoreOrBall(LetScoreOrBall letScoreOrBall) {
        this.letScoreOrBall = letScoreOrBall;
    }

    public LetScoreOrBall getLetScoreOrBall() {
        return letScoreOrBall;
    }

    public void hideAllCues(Pane pane) {
//        playCue.getCueModel(pane).hide();
//        breakCue.getCueModel(pane).hide();
    }

    public CueSelection getCueSelection() {
        return cueSelection;
    }
    
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

    public PlayerPerson getPlayerPerson() {
        return playerPerson;
    }
}
