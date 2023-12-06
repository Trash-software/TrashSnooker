package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;

import java.util.*;

public abstract class Player {

//    protected final int number;
    protected final InGamePlayer inGamePlayer;
    protected final List<Ball> cumulatedPotted = new ArrayList<>();
    protected final TreeMap<Ball, Integer> singlePole = new TreeMap<>();
//    protected final List<PotAttempt> attempts = new ArrayList<>();
//    protected final List<DefenseAttempt> defenseAttempts = new ArrayList<>();
    protected final List<CueAttempt> attemptList = new ArrayList<>();
    protected int score;
    protected int lastAddedScore;
    private boolean withdrawn = false;

    public Player(InGamePlayer inGamePlayer) {
//        this.number = number;
        this.inGamePlayer = inGamePlayer;
    }
    
    protected abstract void addScoreOfPotted(Collection<? extends Ball> pottedBalls);

    public PlayerPerson getPlayerPerson() {
        return inGamePlayer.getPlayerPerson();
    }

    public InGamePlayer getInGamePlayer() {
        return inGamePlayer;
    }
    
    public void addAttempt(CueAttempt attempt) {
        attemptList.add(attempt);
    }

    public List<CueAttempt> getAttempts() {
        return attemptList;
    }
    
    public List<PotAttempt> getRecentSinglePoleAttempts() {
        List<PotAttempt> result = new ArrayList<>();
        for (int i = attemptList.size() - 1; i >= 0; i--) {
            CueAttempt ca = attemptList.get(i);
            if (ca instanceof PotAttempt pa) {
                if (pa.isSuccess()) {
                    result.add(pa);
                } else {
                    if (result.isEmpty()) {
                        result.add(pa);  // 是最后一杆，说明这个单杆终结了
                    } else {
                        // 不属于这次单杆
                        break;
                    }
                }
            } else {
                break;
            }
        }
        Collections.reverse(result);
        return result;
    }

    public int getScore() {
        return score;
    }

    /**
     * Override这个method一定记得call super
     */
    public void correctPotBalls(Game<?, ?> game, Collection<? extends Ball> pottedBalls) {
        game.newPottedLegal.addAll(pottedBalls);
        for (Ball ball : pottedBalls) {
            if (singlePole.containsKey(ball)) {
                singlePole.put(ball, singlePole.get(ball) + 1);
            } else {
                singlePole.put(ball, 1);
            }
        }
        this.cumulatedPotted.addAll(pottedBalls);
        int curScore = score;
        addScoreOfPotted(pottedBalls);
        lastAddedScore = score - curScore;
        
        int singlePoleCount = getSinglePoleCount();

        if (singlePoleCount >= 3) {
            AchManager.getInstance().addAchievement(Achievement.THREE_BALLS_IN_A_ROW, inGamePlayer);
        }
        AchManager.getInstance().addAchievement(Achievement.POSITIONING_MASTER, singlePoleCount, inGamePlayer);
    }

    public TreeMap<Ball, Integer> getSinglePole() {
        return singlePole;
    }
    
    public int getSinglePoleCount() {
        int singlePoleCount = 0;
        for (int c : singlePole.values()) {
            singlePoleCount += c;
        }
        return singlePoleCount;
    }

    public int getLastAddedScore() {
        return lastAddedScore;
    }

    public void clearSinglePole() {
        singlePole.clear();
    }

    public void addScore(int score) {
        this.score += score;
        this.lastAddedScore = score;
    }

    public boolean isWithdrawn() {
        return withdrawn;
    }

    public void withdraw() {
        withdrawn = true;
    }

    public List<Ball> getAllPotted() {
        return cumulatedPotted;
    }
}
