package trashsoftware.trashSnooker.core.career.achievement;

import javafx.scene.layout.Pane;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.career.CareerSave;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;

import java.util.Map;

public abstract class AchManager {

    private static AchManager instance;

    AchManager() {
    }

    public static AchManager getInstance() {
        if (instance == null) {
            instance = new InvalidAchManager();
        }
        return instance;
    }

    public static void setCareerInstance(CareerSave careerSave) {
        instance = CareerAchManager.loadFromDisk(careerSave);
    }
    
    public static void newCareerInstance(CareerSave careerSave) {
        instance = new CareerAchManager(careerSave);
    }
    
    public void removePendingAch(Achievement achievement) {
    }
    
    public abstract JSONObject toJson();

    public abstract void updateAfterCueFinish(Pane owner, Game<?, ?> game, ScoreResult scoreResult,
                                              PotAttempt potAttempt, DefenseAttempt defenseAttempt);
    
    public abstract void updateAfterMatchEnds(EntireGame entireGame);

    public abstract Map<Achievement, AchCompletion> getCompletedAchievements();

    public abstract boolean completed(Achievement achievement);
    
    /**
     * 直接添加一个成就
     */
    public abstract void addAchievement(Achievement achievement, InGamePlayer igp);
    
    public void showAchievementPopup() {
    }
    
    public int getNCompletedAchievements() {
        return getCompletedAchievements().size();
    }
}
