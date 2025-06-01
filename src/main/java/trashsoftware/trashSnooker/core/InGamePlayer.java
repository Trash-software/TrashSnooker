package trashsoftware.trashSnooker.core;

import javafx.scene.layout.Pane;
import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.attempt.DefenseAttempt;
import trashsoftware.trashSnooker.core.attempt.PotAttempt;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.InventoryManager;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.CueBrand;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.person.PlayerPerson;
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
    // rua不rua
    private double psyStatus = 1.0;

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
        igp.psyStatus = jsonObject.optDouble("psyStatus", 1.0);
        
        return igp;
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();

        object.put("person", playerPerson.getPlayerId());
        object.put("gameRule", gameRule.name());
        object.put("playerType", playerType.name());
        object.put("playerNumber", playerNumber);
        object.put("handFeelEffort", handFeelEffort);
        object.put("psyStatus", psyStatus);
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

    public double getPsyStatus() {
        return psyStatus;
    }
    
    private double cuePsyChangeBase() {
        return Algebra.shiftRangeSafe(0, 100, 
                0.05, 0, playerPerson.getPsyRua());
    }
    
    private void regularizePsyStatus() {
        psyStatus = Math.max(playerPerson.getPsyRua() / 100, Math.min(1.0, psyStatus));
        System.out.println(playerPerson.getName() + " psy status: " + psyStatus);
    }
    
    private static double stagePsyEffect(GamePlayStage playStage) {
        return switch (playStage) {
            case BREAK -> 0.2;
            case NORMAL, ENHANCE_WIN -> 1.0;
            case NEXT_BALL_WIN -> 1.5;
            case THIS_BALL_WIN -> 3.0;
            case NO_PRESSURE -> 0.1;
        };
    }
    
    public void adjustPsyStatusFrameBegin(double frameImportance) {
        double avg = (playerPerson.psyNerve + playerPerson.getPsyRua()) / 2;
        double reference = 100 - (100 - avg) * frameImportance;
        if (psyStatus > reference) {
            psyStatus = (psyStatus + reference) / 2;
            System.out.println("Changed " + playerPerson.getName() + "'s psy status to " + psyStatus);
        }
    }
    
    public void updatePsyStatusAfterSelfCue(double frameImportance, 
                                            PotAttempt potAttempt, 
                                            DefenseAttempt defenseAttempt,
                                            boolean foul,
                                            GamePlayStage playStage) {
//        double ruaFactor = Algebra.shiftRangeSafe(0, 100, 1.0, 1.0, playerPerson.psyRua);
        double baseChange = cuePsyChangeBase();
        double increase = 0;
        double decrease = (foul ? (frameImportance + 1) : 0) * baseChange;
        if (potAttempt != null) {
            if (potAttempt.isSuccess()) {
                double diff = 1.0 - psyStatus;
                increase += Math.max(0.005, diff * 0.1 * (potAttempt.isDifficultShot() ? 3.0 : 1.0));
            } else {
                double importanceFactor = stagePsyEffect(playStage);
                importanceFactor *= (frameImportance + 1) * 0.5;
                decrease += importanceFactor * baseChange * (potAttempt.isEasyShot() ? 3.0 : 1.0);
            }
        }
        psyStatus += increase;
        psyStatus -= decrease;
        regularizePsyStatus();
    }

    public void updatePsyStatusAfterOpponentCue(double frameImportance, PotAttempt potAttempt) {
        double baseChange = cuePsyChangeBase();
        double decrease = 0;
        if (potAttempt != null) {
            if (potAttempt.isSuccess()) {
                decrease += (frameImportance + 1) * baseChange * 0.08 * (potAttempt.isDifficultShot() ? 3.0 : 1.0);
            }
        }
        psyStatus -= decrease;
        regularizePsyStatus();
    }
    
    public void updatePsyStatusAfterFrame(double frameImportance, boolean won, EntireGame entireGame) {
        double baseChange = cuePsyChangeBase() * (frameImportance + 1) * 3.5;
        if (won) {
            psyStatus += baseChange;
        } else {
            int contiLoss = entireGame.playerContinuousLoses(playerNumber) + 1;
            psyStatus -= baseChange * contiLoss;  // 越输越糟糕
        }
        regularizePsyStatus();
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
