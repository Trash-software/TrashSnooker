package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.numberedGames.sidePocket.SidePocketBallPlayer;
import trashsoftware.trashSnooker.core.numberedGames.sidePocket.SidePocketGame;

import java.util.List;

public class SidePocketAiCueBallPlacer extends 
        AiCueBallPlacer<SidePocketGame, SidePocketBallPlayer> {
    
    public SidePocketAiCueBallPlacer(SidePocketGame game, SidePocketBallPlayer player) {
        super(game, player);
    }

    @Override
    protected double[] breakPosition() {
        return null;
    }

    @Override
    protected List<double[]> legalPositions() {
        return null;
    }
}
