package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.CuePlayParams;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.snooker.AbstractSnookerGame;
import trashsoftware.trashSnooker.core.snooker.SnookerPlayer;

public class SnookerAiCue extends AiCue<AbstractSnookerGame, SnookerPlayer> {

    public SnookerAiCue(AbstractSnookerGame game, SnookerPlayer aiPlayer) {
        super(game, aiPlayer);
    }

    @Override
    protected DefenseChoice breakCue() {
        double aimingPosX = game.getTable().firstRedX() + 
                game.getGameValues().ballDiameter * game.redRowOccupyX * 4.0;
        double aimingPosY = game.getGameValues().midY + 
                (game.getGameValues().ballDiameter + game.redGapDt * 0.6) * 6.60;
        double dirX = aimingPosX - game.getCueBall().getX();
        double dirY = aimingPosY - game.getCueBall().getY();
        double[] unitXY = Algebra.unitVector(dirX, dirY);
        double selectedPower = actualPowerToSelectedPower(34.0);
        CuePlayParams cpp = CuePlayParams.makeIdealParams(
                unitXY[0],
                unitXY[1],
                0.0,
                0.0,
                5.0,
                selectedPowerToActualPower(selectedPower)
        );
        return new DefenseChoice(unitXY, selectedPower, cpp);
    }

    @Override
    protected DefenseChoice standardDefense() {
        return null;
    }

    @Override
    protected DefenseChoice solveSnooker() {
        return null;
    }

    @Override
    public AiCueResult makeCue(Phy phy) {
        int behind = -game.getScoreDiff(aiPlayer);
        int rem = game.getRemainingScore();
        if (behind > rem) {
            if (behind > rem + 12) {
                // 超太多了，认输
                return null;
            } else {
                if (rem == 7) return null;  // 只剩一颗球还防个屁
                else if (behind > rem + 8 && rem <= 27) return null; // 清彩阶段，落后多了就认输
            }
            // 其他情况还可以挣扎
            if (game.getCurrentTarget() == 1) {
                if (game.remainingRedCount() == 1) {
                    System.out.println("Last red, make snooker");
                    DefenseChoice def = getBestDefenseChoice(phy);
                    if (def != null) return makeDefenseCue(def);
                }
            } else if (game.getCurrentTarget() != AbstractSnookerGame.RAW_COLORED_REP) {
                System.out.println("Ordered colors, make snooker");
                DefenseChoice def = getBestDefenseChoice(phy);
                if (def != null) return makeDefenseCue(def);
            }
        }
        return regularCueDecision(phy);
    }

}
