package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.CuePlayParams;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.snooker.AbstractSnookerGame;
import trashsoftware.trashSnooker.core.snooker.SnookerPlayer;

import java.util.Arrays;

public class SnookerAiCue extends AiCue<AbstractSnookerGame, SnookerPlayer> {

    public SnookerAiCue(AbstractSnookerGame game, SnookerPlayer aiPlayer) {
        super(game, aiPlayer);
    }

    @Override
    protected DefenseChoice breakCue(Phy phy) {
        AiPlayStyle.SnookerBreakMethod method = 
                aiPlayer.getPlayerPerson().getAiPlayStyle().snookerBreakMethod;
        
        if (method == AiPlayStyle.SnookerBreakMethod.BACK) return backBreak(phy);
        boolean leftBreak = method == AiPlayStyle.SnookerBreakMethod.LEFT;
        
        double aimingPosX = game.getTable().firstRedX() + 
                game.getGameValues().ballDiameter * game.redRowOccupyX * 4.0;
        double yOffset = (game.getGameValues().ballDiameter + game.redGapDt * 0.6) * 7.40;
        if (leftBreak) yOffset = -yOffset;
        double aimingPosY = game.getGameValues().midY + yOffset;
                
        double dirX = aimingPosX - game.getCueBall().getX();
        double dirY = aimingPosY - game.getCueBall().getY();
        double[] unitXY = Algebra.unitVector(dirX, dirY);
        double actualPower = 28.0;
        double selectedPower = actualPowerToSelectedPower(actualPower / 100 * phy.cloth.smoothness.speedReduceFactor);
        double selectedSideSpin = leftBreak ? -0.6 : 0.6;
        double actualSideSpin = CuePlayParams.unitSideSpin(selectedSideSpin,
                aiPlayer.getInGamePlayer().getCurrentCue(game));

        double[] correctedXY = CuePlayParams.aimingUnitXYIfSpin(
                actualSideSpin,
                actualPower,
                unitXY[0],
                unitXY[1]
        );
        CuePlayParams cpp = CuePlayParams.makeIdealParams(
                correctedXY[0],
                correctedXY[1],
                0.0,
                actualSideSpin,
                0.0,
                actualPower
        );
//        System.out.println(Arrays.toString(unitXY) + Arrays.toString(correctedXY));
        return new DefenseChoice(correctedXY, selectedPower, selectedSideSpin, cpp);
    }
    
    private DefenseChoice backBreak(Phy phy) {
        return null;
    }

    @Override
    protected boolean requireHitCushion() {
        return false;
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
