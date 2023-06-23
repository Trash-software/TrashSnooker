package trashsoftware.trashSnooker.core.career.achievement;

import javafx.scene.layout.Pane;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;

import java.util.Map;

public class InvalidAchManager extends AchManager {
    @Override
    public void updateAfterCueFinish(Pane owner, Game<?, ?> game, ScoreResult scoreResult, PotAttempt potAttempt, DefenseAttempt defenseAttempt) {
    }

    @Override
    public void updateAfterMatchEnds(EntireGame entireGame) {
    }

    @Override
    public JSONObject toJson() {
        return new JSONObject();
    }

    @Override
    public Map<Achievement, AchCompletion> getCompletedAchievements() {
        return Map.of();
    }

    @Override
    public boolean completed(Achievement achievement) {
        return true;
    }

    @Override
    public void addAchievement(Achievement achievement, InGamePlayer igp) {
    }
}
