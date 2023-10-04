package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.util.Util;

/**
 * 记录一杆的出杆参数
 */
public class CueParams {

    private final double selectedPower;
    private final double selectedFrontBackSpin;
    private final double selectedSideSpin;
    private double actualPower;
    private double actualFrontBackSpin;
    private double actualSideSpin;
    private final PlayerPerson.HandSkill handSkill;

    private CueParams(double selectedPower,
                      double actualPower,
                      double selectedFrontBackSpin,
                      double actualFrontBackSpin,
                      double selectedSideSpin,
                      double actualSideSpin,
                      PlayerPerson.HandSkill handSkill) {
        this.selectedPower = selectedPower;
        this.selectedFrontBackSpin = selectedFrontBackSpin;
        this.selectedSideSpin = selectedSideSpin;
        this.actualPower = actualPower;
        this.actualFrontBackSpin = actualFrontBackSpin;
        this.actualSideSpin = actualSideSpin;
        this.handSkill = handSkill;

        if (this.actualPower < Values.MIN_SELECTED_POWER)
            this.actualPower = Values.MIN_SELECTED_POWER;
    }

    public static CueParams createBySelected(double selectedPower,
                                             double selectedFrontBackSpin,
                                             double selectedSideSpin,
                                             Game<?, ?> game,
                                             InGamePlayer inGamePlayer,
                                             PlayerPerson.HandSkill handSkill) {
        Cue cue = inGamePlayer.getCurrentCue(game);
        double cuePowerMul = cue.powerMultiplier;

        double actualFbSpin = CuePlayParams.unitFrontBackSpin(selectedFrontBackSpin, inGamePlayer.getPlayerPerson(), cue);
        double actualSideSpin = CuePlayParams.unitSideSpin(selectedSideSpin, cue);

        double mul = Util.powerMultiplierOfCuePoint(actualSideSpin, actualFbSpin);
        double handMul = handSkill == null ? 1.0 : PlayerPerson.HandBody.getPowerMulOfHand(handSkill);

        double actualPower = selectedPower * handMul * mul * cuePowerMul /
                game.getGameValues().ball.ballWeightRatio;

        return new CueParams(selectedPower,
                actualPower,
                selectedFrontBackSpin,
                actualFbSpin,
                selectedSideSpin,
                actualSideSpin,
                handSkill);
    }

    public static CueParams createByActual(double actualPower,
                                           double actualFrontBackSpin,
                                           double actualSideSpin,
                                           Game<?, ?> game,
                                           InGamePlayer inGamePlayer,
                                           PlayerPerson.HandSkill handSkill) {
        Cue cue = inGamePlayer.getCurrentCue(game);
        double cuePowerMul = cue.powerMultiplier;

        double mul = Util.powerMultiplierOfCuePoint(actualSideSpin, actualFrontBackSpin);
        double handMul = handSkill == null ? 1.0 : PlayerPerson.HandBody.getPowerMulOfHand(handSkill);

        double selectedPower = actualPower / handMul / mul / cuePowerMul *
                game.getGameValues().ball.ballWeightRatio;
        
        double selectedFrontBackSpin = CuePlayParams.getSelectedFrontBackSpin(actualFrontBackSpin,
                inGamePlayer.getPlayerPerson(),
                cue);
        double selectedSideSpin = CuePlayParams.getSelectedSideSpin(actualSideSpin,
                cue);

        return new CueParams(selectedPower,
                actualPower,
                selectedFrontBackSpin,
                actualFrontBackSpin,
                selectedSideSpin,
                actualSideSpin,
                handSkill);
    }

    public static double selectedPowerToActualPower(Game<?, ?> game,
                                                    InGamePlayer igp,
                                                    double selectedPower,
                                                    double unitCuePointX,
                                                    double unitCuePointY,
                                                    PlayerPerson.HandSkill handSkill) {
        double mul = Util.powerMultiplierOfCuePoint(unitCuePointX, unitCuePointY);
        double handMul = handSkill == null ? 1.0 : PlayerPerson.HandBody.getPowerMulOfHand(handSkill);
        return selectedPower * handMul * mul *
                igp.getCurrentCue(game).powerMultiplier /
                game.getGameValues().ball.ballWeightRatio;
    }

    public static double actualPowerToSelectedPower(Game<?, ?> game,
                                                    InGamePlayer igp,
                                                    double actualPower,
                                                    double unitCuePointX,
                                                    double unitCuePointY,
                                                    PlayerPerson.HandSkill handSkill) {
        double mul = Util.powerMultiplierOfCuePoint(unitCuePointX, unitCuePointY);
        double handMul = handSkill == null ? 1.0 : PlayerPerson.HandBody.getPowerMulOfHand(handSkill);
        return actualPower / handMul / mul /
                igp.getCurrentCue(game).powerMultiplier *
                game.getGameValues().ball.ballWeightRatio;
    }

    @Override
    public String toString() {
        return "CueParams{" +
                "selectedPower=" + selectedPower +
                ", selectedFrontBackSpin=" + selectedFrontBackSpin +
                ", selectedSideSpin=" + selectedSideSpin +
                ", actualPower=" + actualPower +
                ", actualFrontBackSpin=" + actualFrontBackSpin +
                ", actualSideSpin=" + actualSideSpin +
                ", handSkill=" + handSkill +
                '}';
    }

    public double selectedPower() {
        return selectedPower;
    }

    public double actualPower() {
        return actualPower;
    }

    public double selectedFrontBackSpin() {
        return selectedFrontBackSpin;
    }

    public double actualFrontBackSpin() {
        return actualFrontBackSpin;
    }

    public double selectedSideSpin() {
        return selectedSideSpin;
    }

    public double actualSideSpin() {
        return actualSideSpin;
    }

    public void setActualPower(double actualPower) {
        this.actualPower = actualPower;
    }

    public void setActualFrontBackSpin(double actualFrontBackSpin) {
        this.actualFrontBackSpin = actualFrontBackSpin;
    }

    public void setActualSideSpin(double actualSideSpin) {
        this.actualSideSpin = actualSideSpin;
    }

    public PlayerPerson.HandSkill getHandSkill() {
        return handSkill;
    }
}
