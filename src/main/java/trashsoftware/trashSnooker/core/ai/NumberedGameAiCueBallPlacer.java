package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.numberedGames.NumberedBallGame;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;

import java.util.List;

public abstract class NumberedGameAiCueBallPlacer<G extends NumberedBallGame<?>,
        P extends NumberedBallPlayer> extends
        AiCueBallPlacer<G, P> {

    public NumberedGameAiCueBallPlacer(G game, P player) {
        super(game, player);
    }

    @Override
    protected List<double[]> legalPositions() {
        List<double[]> legalPos;
        double sep = 24;
        do {
            legalPos = legalPositions(sep);
            sep *= 1.5;
        } while (legalPos.isEmpty() && sep < 1000);

        return legalPos;
    }

    protected abstract List<double[]> legalPositions(double smallSep);
}
