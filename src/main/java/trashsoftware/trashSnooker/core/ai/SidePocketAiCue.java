package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.CuePlayParams;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
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
        return standardBreak();
    }
    
    private DefenseChoice standardBreak() {
        PoolBall cueBall = game.getCueBall();
        PoolBall oneBall = game.getBallByValue(1);
        double dirX = oneBall.getX() - cueBall.getX();
        double dirY = oneBall.getY() - cueBall.getY();
        System.out.println("Cue ball at " + cueBall.getX() + " " + cueBall.getY());
        double[] unitXY = Algebra.unitVector(dirX, dirY);

        double selectedPower = aiPlayer.getPlayerPerson().getMaxPowerPercentage();
        CuePlayParams cpp = CuePlayParams.makeIdealParams(
                unitXY[0],
                unitXY[1],
                0.0,
                0.0,
                0.0,
                selectedPowerToActualPower(selectedPower, 0, 0,
                        aiPlayer.getPlayerPerson().handBody.getPrimary())
        );
        return new DefenseChoice(unitXY, selectedPower, 0.0, cpp,
                aiPlayer.getPlayerPerson().handBody.getPrimary());
    }

    @Override
    public AiCueResult makeCue(Phy phy) {
        return regularCueDecision(phy);
    }
}
