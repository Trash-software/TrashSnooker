package trashsoftware.trashSnooker.core.snooker;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.Game;
import trashsoftware.trashSnooker.core.InGamePlayer;
import trashsoftware.trashSnooker.core.Player;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SnookerPlayer extends Player {

    private final AbstractSnookerGame game;
    private final List<Integer> singlePoleScores = new ArrayList<>();
    private boolean flushed = false;

    public SnookerPlayer(InGamePlayer playerPerson, AbstractSnookerGame game) {
        super(playerPerson);

        this.game = game;
    }

    public void potFreeBall(int freeBallScore) {
        score += freeBallScore;
        AchManager.getInstance().cumulateAchievement(Achievement.SNOOKER_CUMULATE_SCORE, 
                freeBallScore, 
                getInGamePlayer());
        lastAddedScore = freeBallScore;
        Ball freeBall = game.getBallOfValue(freeBallScore);
        if (singlePole.containsKey(freeBall)) {
            singlePole.put(freeBall, singlePole.get(freeBall) + 1);
        } else {
            singlePole.put(freeBall, 1);
        }
    }

    @Override
    protected void addScoreOfPotted(Collection<? extends Ball> pottedBalls) {
        int add = 0;
        for (Ball ball : pottedBalls) {
            add += ball.getValue();
        }
        AchManager.getInstance().cumulateAchievement(Achievement.SNOOKER_CUMULATE_SCORE, 
                add, 
                getInGamePlayer());
        score += add;
    }

    public int getSinglePoleScore() {
        int singlePoleScore = 0;
        for (Map.Entry<Ball, Integer> entry : singlePole.entrySet()) {
            singlePoleScore += entry.getKey().getValue() * entry.getValue();
        }
        return singlePoleScore;
    }

    @Override
    public void clearSinglePole() {
        singlePoleScores.add(getSinglePoleScore());
        super.clearSinglePole();
    }

    @Override
    public void correctPotBalls(Game<?, ?> game, Collection<? extends Ball> pottedBalls) {
        super.correctPotBalls(game, pottedBalls);
        
        int singlePoleScore = getSinglePoleScore();
        if (singlePoleScore >= 50) {
            AchManager.getInstance().addAchievement(Achievement.SNOOKER_BREAK_50, getInGamePlayer());
            if (singlePoleScore >= 100) {
                AchManager.getInstance().addAchievement(Achievement.SNOOKER_BREAK_100, getInGamePlayer());
                if (singlePoleScore >= 135) {
                    AchManager.getInstance().addAchievement(Achievement.SNOOKER_BREAK_100_BIG, getInGamePlayer());
                    if (singlePoleScore >= 147) {
                        AchManager.getInstance().addAchievement(Achievement.SNOOKER_BREAK_147, getInGamePlayer());
                    }
                }
            }
        }
    }

    public void flushSinglePoles() {
        System.out.println(getPlayerPerson().getName() + 
                " Before flush " + singlePoleScores + ", current: " + getSinglePole());
        if (!singlePole.isEmpty() && !flushed) {
            singlePoleScores.add(getSinglePoleScore());
            flushed = true;
            System.out.println("Flushed!");
        }
        System.out.println("After flush " + singlePoleScores + ", current: " + getSinglePole());
    }

    public List<Integer> getSinglePolesInThisGame() {
        return singlePoleScores;
    }
}
