package trashsoftware.trashSnooker.core.career.championship;

import javafx.application.Platform;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.Career;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.ChampionshipData;
import trashsoftware.trashSnooker.core.career.ChampionshipStage;
import trashsoftware.trashSnooker.util.Util;

import java.io.File;

public class PlayerVsAiMatch {
    public static final String ACTIVE_MATCH_SAVE = "matchSave.json";
    
    public final Career p1;
    public final Career p2;
    public final String matchId;
    public final ChampionshipStage stage;
    private final Championship championship;
    private final MatchTreeNode resultNode;
    private PvEMatchEndCallback callback;
    private Runnable guiFinishCallback;
    private Runnable guiAbortionCallback;
    private EntireGame game;

    public PlayerVsAiMatch(Career p1,
                           Career p2,
                           Championship championship,
                           ChampionshipStage stage,
                           MatchTreeNode resultNode) {
        this.p1 = p1;
        this.p2 = p2;
        this.championship = championship;
        this.stage = stage;
        this.matchId = resultNode.getMatchId();
        this.resultNode = resultNode;
    }
    
    public static File getMatchSave() {
        return new File(CareerManager.getInstance().getCareerSave().getDir(), ACTIVE_MATCH_SAVE);
    }
    
    public static PlayerVsAiMatch loadSaved(MatchTreeNode root, Championship championship) {
        JSONObject load = Util.readJson(getMatchSave());
        if (load == null) return null;
        PlayerVsAiMatch match = fromJson(load, root, championship);
        if (match.game.isFinished()) return null;
        return match;
    }
    
    private static PlayerVsAiMatch fromJson(JSONObject jsonObject, MatchTreeNode root, Championship championship) {
        String p1 = jsonObject.getString("p1");
        String p2 = jsonObject.getString("p2");
        MatchTreeNode resultNode = root.findNodeByPlayers(p1, p2);
        if (resultNode == null) throw new RuntimeException("Cannot recover");
        
        ChampionshipStage stage = ChampionshipStage.valueOf(jsonObject.getString("stage"));
        if (stage != resultNode.getStage()) throw new RuntimeException("broken save");
        
        EntireGame game = EntireGame.fromJson(jsonObject.getJSONObject("game"));
        PlayerVsAiMatch res = new PlayerVsAiMatch(
                CareerManager.getInstance().findCareerByPlayerId(p1),
                CareerManager.getInstance().findCareerByPlayerId(p2),
                championship,
                stage,
                resultNode
        );
        res.setGame(game);
        return res;
    }
    
    private JSONObject toJson() {
        JSONObject egObj = game.toJson();

        JSONObject match = new JSONObject();
        match.put("game", egObj);
        match.put("p1", p1.getPlayerPerson().getPlayerId());
        match.put("p2", p2.getPlayerPerson().getPlayerId());
        match.put("stage", stage.name());
        
        return match;
    }

    public Championship getChampionship() {
        return championship;
    }

    public void saveMatch() {
        Util.writeJson(toJson(), getMatchSave());
    }

    public void setGame(EntireGame game) {
        this.game = game;
    }

    public EntireGame getGame() {
        return game;
    }

    public void setEndCallback(PvEMatchEndCallback callback) {
        this.callback = callback;
    }

    public void setGuiCallback(Runnable guiCallback, Runnable guiAbortionCallback) {
        this.guiFinishCallback = guiCallback;
        this.guiAbortionCallback = guiAbortionCallback;
    }

    public ChampionshipStage getStage() {
        return resultNode.getStage();
    }
    
    public Career getAiCareer() {
        return p1.isHumanPlayer() ? p2 : p1;
    }
    
    public void finish(PlayerPerson winnerPerson, int p1Wins, int p2Wins) {
        if (resultNode.isFinished()) return;
        System.out.println(winnerPerson + " wins");
        if (winnerPerson.getPlayerId().equals(p1.getPlayerPerson().getPlayerId())) {
            resultNode.setWinner(p1, p1Wins, p2Wins);
        } else {
            resultNode.setWinner(p2, p1Wins, p2Wins);
        }
        
        getMatchSave().delete();
        
        callback.matchNormalFinish();
        Platform.runLater(guiFinishCallback);
    }
    
    public void saveAndExit() {
        Platform.runLater(guiAbortionCallback);
    }
}
