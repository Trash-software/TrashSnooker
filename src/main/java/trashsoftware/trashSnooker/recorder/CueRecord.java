package trashsoftware.trashSnooker.recorder;

import trashsoftware.trashSnooker.core.InGamePlayer;

public class CueRecord {
    public final InGamePlayer cuePlayer;
    public final int targetRep;
    public final boolean isSnookerFreeBall;
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
    
    public CueRecord(InGamePlayer cuePlayer,
                     int targetRep,
                     boolean isBreaking,
                     boolean isSnookerFreeBall,
                     double selectedPower,
                     double actualPower,
                     double aimUnitX,
                     double aimUnitY,
                     double intendedVerPoint,
                     double intendedHorPoint,
                     double actualVerPoint,
                     double actualHorPoint,
                     double cueAngle) {
        this.cuePlayer = cuePlayer;
        this.targetRep = targetRep;
        this.isSnookerFreeBall = isSnookerFreeBall;
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
    }
}
