package trashsoftware.trashSnooker.core.numberedGames;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.InGamePlayer;
import trashsoftware.trashSnooker.core.NeedBigBreakPlayer;
import trashsoftware.trashSnooker.core.Player;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class NumberedBallPlayer extends Player implements NeedBigBreakPlayer {

    protected int playTimes = 0;  // 这一局打了几次球
    protected boolean isBreakingPlayer = false;
    protected boolean breakSuccess = false;
    private final List<Integer> continuousPots = new ArrayList<>();
    private boolean flushed = false;

    public NumberedBallPlayer(InGamePlayer playerPerson) {
        super(playerPerson);
    }

    @Override
    protected void addScoreOfPotted(Collection<? extends Ball> pottedBalls) {
        score += pottedBalls.size();
    }

    @Override
    public void clearSinglePole() {
        continuousPots.add(singlePole.size());
        super.clearSinglePole();
    }

    public void flushSinglePoles() {
        System.out.println(getPlayerPerson().getName() +
                " Before flush " + continuousPots + ", current: " + getSinglePole());
        System.out.println("Play times " + playTimes);
        if (!singlePole.isEmpty() && !flushed) {
            continuousPots.add(singlePole.size());
            flushed = true;
            System.out.println("Flushed!");
        }
        System.out.println("After flush " + continuousPots + ", current: " + getSinglePole());
    }

    public List<Integer> getContinuousPots() {
        return continuousPots;
    }
    
    public void setBreakingPlayer() {
        isBreakingPlayer = true;
        playTimes = 1;
    }

    public boolean isBreakingPlayer() {
        return isBreakingPlayer;
    }

    @Override
    public boolean isBreakSuccess() {
        return breakSuccess;
    }

    public void setBreakSuccess(int legalPotCount) {
        this.breakSuccess = true;
        AchManager.getInstance().addAchievement(Achievement.POOL_BREAK_POT, inGamePlayer);
        AchManager.getInstance().addAchievement(
                Achievement.POOL_BREAK_MASTER,
                legalPotCount,
                getInGamePlayer());
    }

    public int getPlayTimes() {
        return playTimes;
    }

    public void incrementPlayTimes() {
        playTimes++;
    }
}
