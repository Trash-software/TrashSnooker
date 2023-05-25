package trashsoftware.trashSnooker.core.career.challenge;

import javafx.application.Platform;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.Career;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.CareerMatch;

public class ChallengeMatch extends CareerMatch {

    public final Career playerCareer;
    public final ChallengeSet challengeSet;
    
    public ChallengeMatch(Career career, ChallengeSet challengeSet) {
        this.playerCareer = career;
        this.challengeSet = challengeSet;
    }
    
    @Override
    public void finish(PlayerPerson winnerPerson, int p1Wins, int p2Wins) {
        if (p1Wins > p2Wins) {
            playerCareer.completeChallenge(challengeSet);
            System.out.println("Career completed challenge: " + challengeSet.getId());
        }
        CareerManager.getInstance().saveToDisk();
        Platform.runLater(guiFinishCallback);
    }
}
