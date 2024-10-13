package trashsoftware.trashSnooker.recorder;

import trashsoftware.trashSnooker.core.GamePlayStage;
import trashsoftware.trashSnooker.core.InGamePlayer;
import trashsoftware.trashSnooker.core.PlayerHand;
import trashsoftware.trashSnooker.core.PlayerPerson;

public class CueRecord {
    public final InGamePlayer cuePlayer;
    public final double selectedPower;
    public final double actualPower;
    public final double aimUnitX;
    public final double aimUnitY;
    public final double intendedVerPoint;
    public final double intendedHorPoint;
    public final double actualVerPoint;
    public final double actualHorPoint;
    public final double cueAngle;
    public final boolean isBreaking;
    public final GamePlayStage playStage;
    public final PlayerHand.Hand hand;
    
    public CueRecord(InGamePlayer cuePlayer,
                     boolean isBreaking,
                     double selectedPower,
                     double actualPower,
                     double aimUnitX,
                     double aimUnitY,
                     double intendedVerPoint,
                     double intendedHorPoint,
                     double actualVerPoint,
                     double actualHorPoint,
                     double cueAngle,
                     GamePlayStage playStage,
                     PlayerHand.Hand hand) {
        this.cuePlayer = cuePlayer;
        this.selectedPower = selectedPower;
        this.actualPower = actualPower;
        this.aimUnitX = aimUnitX;
        this.aimUnitY = aimUnitY;
        this.intendedVerPoint = intendedVerPoint;
        this.intendedHorPoint = intendedHorPoint;
        this.actualVerPoint = actualVerPoint;
        this.actualHorPoint = actualHorPoint;
        this.cueAngle = cueAngle;
        this.isBreaking = isBreaking;
        this.playStage = playStage;
        this.hand = hand;
    }
}
