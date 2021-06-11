package trashsoftware.trashSnooker.core.snooker;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.Player;
import trashsoftware.trashSnooker.core.PlayerPerson;

public class SnookerPlayer extends Player {

    private final AbstractSnookerGame game;

    public SnookerPlayer(int number, PlayerPerson playerPerson, AbstractSnookerGame game) {
        super(number, playerPerson);

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
