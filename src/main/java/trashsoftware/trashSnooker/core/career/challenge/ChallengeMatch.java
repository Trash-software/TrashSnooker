package trashsoftware.trashSnooker.core.career.challenge;

import javafx.application.Platform;
import trashsoftware.trashSnooker.core.person.PlayerPerson;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.CareerMatch;
import trashsoftware.trashSnooker.core.career.HumanCareer;

public class ChallengeMatch extends CareerMatch {

    public final HumanCareer playerCareer;
    public final ChallengeSet challengeSet;
    private int score;
    
    public ChallengeMatch(HumanCareer career, ChallengeSet challengeSet) {
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
