package trashsoftware.trashSnooker.core;

import java.util.*;

public abstract class Player {

    protected final int number;
    protected final InGamePlayer playerPerson;
    protected final TreeMap<Ball, Integer> singlePole = new TreeMap<>();
    protected final List<PotAttempt> attempts = new ArrayList<>();
    protected final List<DefenseAttempt> defenseAttempts = new ArrayList<>();
    protected int score;
    private boolean withdrawn = false;

    public Player(int number, InGamePlayer playerPerson) {
        this.number = number;
        this.playerPerson = playerPerson;
    }

    public PlayerPerson getPlayerPerson() {
        return playerPerson.getPlayerPerson();
    }

    public InGamePlayer getInGamePlayer() {
        return playerPerson;
    }
    
    public void addAttempt(PotAttempt potAttempt) {
        attempts.add(potAttempt);
    }

    public List<PotAttempt> getAttempts() {
        return attempts;
    }
    
    public void addDefenseAttempt(DefenseAttempt defenseAttempt) {
        defenseAttempts.add(defenseAttempt);
    }

    public List<DefenseAttempt> getDefenseAttempts() {
        return defenseAttempts;
    }

    public int getNumber() {
        return number;
    }

    public int getScore() {
        return score;
    }

    public void correctPotBalls(Set<Ball> pottedBalls) {
        for (Ball ball : pottedBalls) {
            score += ball.getValue();
            if (singlePole.containsKey(ball)) {
                singlePole.put(ball, singlePole.get(ball) + 1);
            } else {
                singlePole.put(ball, 1);
            }
        }
    }

    public TreeMap<Ball, Integer> getSinglePole() {
        return singlePole;
    }

    public int getSinglePoleScore() {
        int singlePoleScore = 0;
        for (Map.Entry<Ball, Integer> entry : singlePole.entrySet()) {
            singlePoleScore += entry.getKey().getValue() * entry.getValue();
        }
        return singlePoleScore;
    }

    public void clearSinglePole() {
        singlePole.clear();
    }

    public void addScore(int score) {
        this.score += score;
    }

    public boolean isWithdrawn() {
        return withdrawn;
    }

    public void withdraw() {
        withdrawn = true;
    }
}
