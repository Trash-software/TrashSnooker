package trashsoftware.trashSnooker.core.career.challenge;

import javafx.application.Platform;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.Career;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.CareerMatch;

public class ChallengeMatch extends CareerMatch {

    public final Career playerCareer;
    public final ChallengeSet challengeSet;
    private int score;
    
    public ChallengeMatch(Career career, ChallengeSet challengeSet) {
        this.playerCareer = career;
        this.challengeSet = challengeSet;
    }

    public void setScore(int score) {
        System.out.println("scored " + score);
        this.score = score;
    }

    @Override
    public void finish(PlayerPerson winnerPerson, int p1Wins, int p2Wins) {
        playerCareer.completeChallenge(challengeSet, p1Wins > p2Wins, score);
        System.out.println("Career challenge: " + challengeSet.getId() + " " + (p1Wins > p2Wins) + ": " + score);
        
        CareerManager.getInstance().saveToDisk();
        if (guiFinishCallback != null) Platform.runLater(guiFinishCallback);
    }
}
