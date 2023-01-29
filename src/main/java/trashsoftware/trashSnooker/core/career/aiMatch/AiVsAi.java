package trashsoftware.trashSnooker.core.career.aiMatch;

import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.ai.AiPlayStyle;
import trashsoftware.trashSnooker.core.career.Career;

import java.util.Random;

public abstract class AiVsAi {
    
    protected final int totalFrames;
    protected final Career p1;
    protected AiPlayStyle aps1;
    protected PlayerPerson.ReadableAbility ability1;
    protected final Career p2;
    protected AiPlayStyle aps2;
    protected PlayerPerson.ReadableAbility ability2;
    
    protected int p1WinFrames;
    protected int p2WinFrames;
    
    protected Career winner;
    
    protected Random random = new Random();
    protected final double gameTypeDifficulty;
    
    public AiVsAi(Career p1, Career p2, int totalFrames, double gameTypeDifficulty) {
        this.p1 = p1;
        this.p2 = p2;
        this.aps1 = p1.getPlayerPerson().getAiPlayStyle();
        this.ability1 = PlayerPerson.ReadableAbility.fromPlayerPerson(p1.getPlayerPerson());
        this.totalFrames = totalFrames;
        this.aps2 = p2.getPlayerPerson().getAiPlayStyle();
        this.ability2 = PlayerPerson.ReadableAbility.fromPlayerPerson(p2.getPlayerPerson());
        
        this.gameTypeDifficulty = gameTypeDifficulty;
        
        assert totalFrames % 2 == 1;
    }
    
    protected abstract void simulateOneFrame(boolean isFinalFrame);
    
    public void simulate() {
        int half = totalFrames / 2 + 1;
        for (int i = 0; i < totalFrames; i++) {
            simulateOneFrame(p1WinFrames == half - 1 && p2WinFrames == half - 1);
            
            if (p1WinFrames >= half) {
                winner = p1;
                break;
            }
            if (p2WinFrames >= half) {
                winner = p2;
                break;
            }
        }
    }

    public int getP1WinFrames() {
        return p1WinFrames;
    }

    public int getP2WinFrames() {
        return p2WinFrames;
    }

    public Career getWinner() {
        return winner;
    }

    public Career getP1() {
        return p1;
    }

    public Career getP2() {
        return p2;
    }

    @Override
    public String toString() {
        return p1.getPlayerPerson().getPlayerId() + 
                " " + p1WinFrames + " : " + 
                p2WinFrames + " " + 
                p2.getPlayerPerson().getPlayerId();
    }
}
