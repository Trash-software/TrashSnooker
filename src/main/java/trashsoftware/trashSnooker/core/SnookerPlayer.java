package trashsoftware.trashSnooker.core;

public class SnookerPlayer extends Player {

    private final AbstractSnookerGame game;

    public SnookerPlayer(int number, AbstractSnookerGame game) {
        super(number);

        this.game = game;
    }

    public void potFreeBall(int freeBallScore) {
        score += freeBallScore;
        Ball freeBall = game.getBallOfValue(freeBallScore);
        if (singlePole.containsKey(freeBall)) {
            singlePole.put(freeBall, singlePole.get(freeBall) + 1);
        } else {
            singlePole.put(freeBall, 1);
        }
    }
}
