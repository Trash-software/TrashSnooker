package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.CueParams;
import trashsoftware.trashSnooker.core.CuePlayParams;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.core.numberedGames.nineBall.AmericanNineBallGame;
import trashsoftware.trashSnooker.core.numberedGames.nineBall.AmericanNineBallPlayer;
import trashsoftware.trashSnooker.core.phy.Phy;

public class AmericanNineAiCue extends AiCue<AmericanNineBallGame, AmericanNineBallPlayer> {
    public AmericanNineAiCue(AmericanNineBallGame game, AmericanNineBallPlayer aiPlayer) {
        super(game, aiPlayer);
    }

    @Override
    protected KickPriceCalculator kickPriceCalculator() {
        return ((kickedBall, kickSpeed, dtFromFirst) -> kickUselessBallPrice(dtFromFirst));
    }

    @Override
    protected boolean supportAttackWithDefense(int targetRep) {
        return true;
    }

    @Override
    protected FinalChoice.DefenseChoice standardDefense() {
        return null;
    }

    @Override
    protected boolean currentMustAttack() {
        return false;
    }

    @Override
    protected FinalChoice.DefenseChoice breakCue(Phy phy) {
        return standardBreak();
    }
    
    private FinalChoice.DefenseChoice standardBreak() {
        PoolBall cueBall = game.getCueBall();
        PoolBall oneBall = game.getBallByValue(1);
        double dirX = oneBall.getX() - cueBall.getX();
        double dirY = oneBall.getY() - cueBall.getY();
        System.out.println("Cue ball at " + cueBall.getX() + " " + cueBall.getY());
        double[] unitXY = Algebra.unitVector(dirX, dirY);

        double selectedPower = aiPlayer.getPlayerPerson().getPrimaryHand().getMaxPowerPercentage();
        CueParams cueParams = CueParams.createBySelected(
                selectedPower,
                0.0,
                0.0,
                5.0,
                game,
                aiPlayer.getInGamePlayer(),
                aiPlayer.getPlayerPerson().handBody.getPrimary()
        );
        CuePlayParams cpp = CuePlayParams.makeIdealParams(
                unitXY[0],
                unitXY[1],
                cueParams
        );
        return new FinalChoice.DefenseChoice(unitXY, cueParams, cpp);
    }

    @Override
    public AiCueResult makeCue(Phy phy) {
        return regularCueDecision(phy);
    }
}
