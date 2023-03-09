package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.numberedGames.sidePocket.SidePocketPlayer;
import trashsoftware.trashSnooker.core.numberedGames.sidePocket.SidePocketGame;
import trashsoftware.trashSnooker.core.phy.Phy;

public class SidePocketAiCue extends AiCue<SidePocketGame, SidePocketPlayer> {
    public SidePocketAiCue(SidePocketGame game, SidePocketPlayer aiPlayer) {
        super(game, aiPlayer);
    }

    @Override
    protected double priceOfKick(Ball kickedBall, double kickSpeed) {
        return KICK_USELESS_BALL_MUL;
    }

    @Override
    protected boolean supportAttackWithDefense(int targetRep) {
        return true;
    }

    @Override
    protected DefenseChoice standardDefense() {
        return null;
    }

    @Override
    protected boolean requireHitCushion() {
        return true;
    }

    @Override
    protected DefenseChoice breakCue(Phy phy) {
        return null;
    }

    @Override
    public AiCueResult makeCue(Phy phy) {
        return null;
    }
}
