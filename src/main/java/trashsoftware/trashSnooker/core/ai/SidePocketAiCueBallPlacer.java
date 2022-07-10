package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.numberedGames.sidePocket.SidePocketPlayer;
import trashsoftware.trashSnooker.core.numberedGames.sidePocket.SidePocketGame;

import java.util.List;

public class SidePocketAiCueBallPlacer extends 
        AiCueBallPlacer<SidePocketGame, SidePocketPlayer> {
    
    public SidePocketAiCueBallPlacer(SidePocketGame game, SidePocketPlayer player) {
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
