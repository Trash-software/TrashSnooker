package trashsoftware.trashSnooker.recorder;

import trashsoftware.trashSnooker.core.InGamePlayer;

public class CueRecord {
    public final InGamePlayer cuePlayer;
    public final double selectedPower;
    public final double aimUnitX;
    public final double aimUnitY;
    public final double wantedUnitVerSpin;
    public final double wantedUnitSideSpin;
    public final double cueAngle;
    public final boolean isBreaking;
    
    public CueRecord(InGamePlayer cuePlayer,
                     double selectedPower,
                     double aimUnitX,
                     double aimUnitY,
                     double wantedUnitVerSpin,
                     double wantedUnitSideSpin,
                     double cueAngle,
                     boolean isBreaking) {
        this.cuePlayer = cuePlayer;
        this.selectedPower = selectedPower;
        this.aimUnitX = aimUnitX;
        this.aimUnitY = aimUnitY;
        this.wantedUnitVerSpin = wantedUnitVerSpin;
        this.wantedUnitSideSpin = wantedUnitSideSpin;
        this.cueAngle = cueAngle;
        this.isBreaking = isBreaking;
    }
}
