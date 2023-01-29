package trashsoftware.trashSnooker.core.career.aiMatch;

import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.ai.AiPlayStyle;
import trashsoftware.trashSnooker.core.career.Career;

import java.util.ArrayList;
import java.util.List;

public class SnookerAiVsAi extends AiVsAi {

    private final List<Integer> p1SinglePoles = new ArrayList<>();
    private final List<Integer> p2SinglePoles = new ArrayList<>();

    public SnookerAiVsAi(Career p1, Career p2, int totalFrames) {
        super(p1, p2, totalFrames, 2.5);
    }

    public static double playerSnookerWinningScore(PlayerPerson playerPerson,
                                                   AiPlayStyle aps,
                                                   PlayerPerson.ReadableAbility ability,
                                                   boolean isFinalFrame) {
        return ability.aiming * ability.cuePrecision / 100 +
                ability.normalPower * ability.powerControl / 10000 * 0.4 + 
                ability.spin * ability.spinControl / 100 * 0.7 +
                aps.position / 100 +
                aps.defense / 100 * 0.5 +
                aps.stability / 100 +
                playerPerson.getSolving() / 100 * 0.2 +
                (isFinalFrame ? (playerPerson.psy / 20) : (playerPerson.psy / 200));
    }

    @Override
    protected void simulateOneFrame(boolean isFinalFrame) {
        double p1s = playerSnookerWinningScore(p1.getPlayerPerson(), aps1, ability1, isFinalFrame);
        double p2s = playerSnookerWinningScore(p2.getPlayerPerson(), aps2, ability2, isFinalFrame);
        double total = p1s + p2s;

//        System.out.println("p1 sc " + p1s + ", p2 sc " + p2s);
        
        double p1WinLimit = ((p1s / total) - 0.5) * gameTypeDifficulty * 2;  // x2是因为正态分布标准差[-1,1]
        
        double gau = random.nextGaussian();
        
        if (gau < p1WinLimit) {
            p1WinFrames++;
        } else {
            p2WinFrames++;
        }
    }

    private int getP1Score() {
        return p1SinglePoles.stream().reduce(0, Integer::sum);
    }

    private int getP2Score() {
        return p2SinglePoles.stream().reduce(0, Integer::sum);
    }
}
