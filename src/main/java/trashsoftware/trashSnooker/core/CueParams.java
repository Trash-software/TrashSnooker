package trashsoftware.trashSnooker.core;

import org.jetbrains.annotations.Nullable;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.util.Util;

/**
 * 记录一杆的出杆参数
 */
public class CueParams {

    private final double selectedPower;
    private final double selectedFrontBackSpin;
    private final double selectedSideSpin;
    private double absolutePower;
    private double absoluteFrontBackSpin;
    private double absoluteSideSpin;
    private final double cueAngleDeg;
    private final PlayerHand handSkill;

    private CueParams(double selectedPower,
                      double absolutePower,
                      double selectedFrontBackSpin,
                      double absoluteFrontBackSpin,
                      double selectedSideSpin,
                      double absoluteSideSpin,
                      double cueAngleDeg,
                      PlayerHand handSkill) {
        this.selectedPower = selectedPower;
        this.selectedFrontBackSpin = selectedFrontBackSpin;
        this.selectedSideSpin = selectedSideSpin;
        this.absolutePower = absolutePower;
        this.absoluteFrontBackSpin = absoluteFrontBackSpin;
        this.absoluteSideSpin = absoluteSideSpin;
        this.handSkill = handSkill;
        this.cueAngleDeg = cueAngleDeg;

        if (this.absolutePower < Values.MIN_SELECTED_POWER)
            this.absolutePower = Values.MIN_SELECTED_POWER;
    }

    public static CueParams createBySelected(double selectedPower,
                                             double selectedFrontBackSpin,
                                             double selectedSideSpin,
                                             double cueAngleDeg,
                                             Game<?, ?> game,
                                             InGamePlayer inGamePlayer,
                                             @Nullable PlayerHand handSkill) {
//        Cue cue = inGamePlayer.getCurrentCue(game);
        Cue cue = inGamePlayer.getCueSelection().getSelected().getNonNullInstance();
        double cuePowerMul = cue.getPowerMultiplier();

        double actualFbSpin = CuePlayParams.unitFrontBackSpin(selectedFrontBackSpin, handSkill, cue);
        double actualSideSpin = CuePlayParams.unitSideSpin(selectedSideSpin, cue);

        double mul = Util.powerMultiplierOfCuePoint(actualSideSpin, actualFbSpin);
//        double handMul = handSkill == null ? 1.0 : handSkill.po;
        double handMul = 1.0;
        
        double actualPower = selectedPower * handMul * mul * cuePowerMul /
                game.getGameValues().ball.ballWeightRatio;

        return new CueParams(selectedPower,
                actualPower,
                selectedFrontBackSpin,
                actualFbSpin,
                selectedSideSpin,
                actualSideSpin,
                cueAngleDeg,
                handSkill);
    }

    public static CueParams createByActual(double actualPower,
                                           double actualFrontBackSpin,
                                           double actualSideSpin,
                                           double cueAngleDeg,
                                           Game<?, ?> game,
                                           InGamePlayer inGamePlayer,
                                           PlayerHand handSkill) {
//        Cue cue = inGamePlayer.getCurrentCue(game);
        Cue cue = inGamePlayer.getCueSelection().getSelected().getNonNullInstance();
        double cuePowerMul = cue.getPowerMultiplier();

        double mul = Util.powerMultiplierOfCuePoint(actualSideSpin, actualFrontBackSpin);
//        double handMul = handSkill == null ? 1.0 : PlayerPerson.HandBody.getPowerMulOfHand(handSkill);
        double handMul = 1.0;

        double selectedPower = actualPower / handMul / mul / cuePowerMul *
                game.getGameValues().ball.ballWeightRatio;
        
        double selectedFrontBackSpin = CuePlayParams.getSelectedFrontBackSpin(actualFrontBackSpin,
                handSkill,
                cue);
        double selectedSideSpin = CuePlayParams.getSelectedSideSpin(actualSideSpin,
                cue);

        return new CueParams(selectedPower,
                actualPower,
                selectedFrontBackSpin,
                actualFrontBackSpin,
                selectedSideSpin,
                actualSideSpin,
                cueAngleDeg,
                handSkill);
    }

    public static double selectedPowerToActualPower(Game<?, ?> game,
                                                    InGamePlayer igp,
                                                    double selectedPower,
                                                    double unitCuePointX,
                                                    double unitCuePointY,
                                                    PlayerHand handSkill) {
        double mul = Util.powerMultiplierOfCuePoint(unitCuePointX, unitCuePointY);
//        double handMul = handSkill == null ? 1.0 : PlayerPerson.HandBody.getPowerMulOfHand(handSkill);
        double handMul = 1.0;
        return selectedPower * handMul * mul *
                igp.getCueSelection().getSelected().getNonNullInstance().getPowerMultiplier() /
                game.getGameValues().ball.ballWeightRatio;
    }

    public static double actualPowerToSelectedPower(Game<?, ?> game,
                                                    InGamePlayer igp,
                                                    double actualPower,
                                                    double unitCuePointX,
                                                    double unitCuePointY,
                                                    PlayerHand handSkill) {
        double mul = Util.powerMultiplierOfCuePoint(unitCuePointX, unitCuePointY);
//        double handMul = handSkill == null ? 1.0 : PlayerPerson.HandBody.getPowerMulOfHand(handSkill);
        double handMul = 1.0;
        return actualPower / handMul / mul /
                igp.getCueSelection().getSelected().getNonNullInstance().getPowerMultiplier() *
                game.getGameValues().ball.ballWeightRatio;
    }

    @Override
    public String toString() {
        return "CueParams{" +
                "selectedPower=" + selectedPower +
                ", selectedFrontBackSpin=" + selectedFrontBackSpin +
                ", selectedSideSpin=" + selectedSideSpin +
                ", actualPower=" + absolutePower +
                ", actualFrontBackSpin=" + absoluteFrontBackSpin +
                ", actualSideSpin=" + absoluteSideSpin +
                ", handSkill=" + handSkill +
                '}';
    }

    public double selectedPower() {
        return selectedPower;
    }

    public double actualPower() {
        return absolutePower;
    }

    public double selectedFrontBackSpin() {
        return selectedFrontBackSpin;
    }

    public double actualFrontBackSpin() {
        return absoluteFrontBackSpin;
    }

    public double selectedSideSpin() {
        return selectedSideSpin;
    }

    public double actualSideSpin() {
        return absoluteSideSpin;
    }

    public void setAbsolutePower(double absolutePower) {
        this.absolutePower = absolutePower;
    }

    public void setAbsoluteFrontBackSpin(double absoluteFrontBackSpin) {
        this.absoluteFrontBackSpin = absoluteFrontBackSpin;
    }

    public void setAbsoluteSideSpin(double absoluteSideSpin) {
        this.absoluteSideSpin = absoluteSideSpin;
    }

    public double getCueAngleDeg() {
        return cueAngleDeg;
    }

    public PlayerHand getHandSkill() {
        return handSkill;
    }
}
