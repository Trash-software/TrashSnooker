package trashsoftware.trashSnooker.core.career.championship;

import javafx.application.Platform;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.Career;
import trashsoftware.trashSnooker.core.career.ChampionshipData;
import trashsoftware.trashSnooker.core.career.ChampionshipStage;

public class PlayerVsAiMatch {
    public final Career p1;
    public final Career p2;
    public final ChampionshipData data;
    public final ChampionshipStage stage;
    private final MatchTreeNode resultNode;
    private PvEMatchEndCallback callback;
    private Runnable guiCallback;

    public PlayerVsAiMatch(Career p1,
                           Career p2,
                           ChampionshipData data,
                           ChampionshipStage stage,
                           MatchTreeNode resultNode) {
        this.p1 = p1;
        this.p2 = p2;
        this.data = data;
        this.stage = stage;
        this.resultNode = resultNode;
    }

    public void setEndCallback(PvEMatchEndCallback callback) {
        this.callback = callback;
    }

    public void setGuiCallback(Runnable guiCallback) {
        this.guiCallback = guiCallback;
    }

    public ChampionshipStage getStage() {
        return resultNode.getStage();
    }
    
    public void finish(PlayerPerson winnerPerson, int p1Wins, int p2Wins) {
        if (resultNode.isFinished()) return;
        if (winnerPerson.getPlayerId().equals(p1.getPlayerPerson().getPlayerId())) {
            resultNode.setWinner(p1, p1Wins, p2Wins);
        } else {
            resultNode.setWinner(p2, p1Wins, p2Wins);
        }
        
        callback.matchFinish();
        Platform.runLater(guiCallback);
    }
}
