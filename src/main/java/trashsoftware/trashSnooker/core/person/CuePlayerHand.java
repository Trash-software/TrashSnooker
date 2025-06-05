package trashsoftware.trashSnooker.core.person;

import trashsoftware.trashSnooker.core.InGamePlayer;
import trashsoftware.trashSnooker.core.cue.CueBrand;

public class CuePlayerHand {

    public PlayerHand playerHand;
    public CueBrand cueBrand;
    public PlayerHand.CueExtension extension;

    public CuePlayerHand(PlayerHand playerHand,
                         CueBrand cueBrand,
                         PlayerHand.CueExtension extension) {
        this.playerHand = playerHand;
        this.cueBrand = cueBrand;
        this.extension = extension;
    }
    
    public static CuePlayerHand makeDefault(InGamePlayer igp) {
        return new CuePlayerHand(igp.getPlayerPerson().getPrimaryHand(), 
                igp.getCueSelection().getSelected().brand, 
                PlayerHand.CueExtension.NO);
    }
    
    public PlayerHand.CueHand toCueHand() {
        return new PlayerHand.CueHand(playerHand.hand, cueBrand, extension);
    }
    
    public double getMaxPowerPercentage() {
        return playerHand.getMaxPowerPercentage() * extension.factor;
    }

    public double getControllablePowerPercentage() {
        return playerHand.getControllablePowerPercentage() * extension.factor;
    }
    
    public double getPowerSd(double selectedPower) {
        return playerHand.getPowerSd(selectedPower / extension.factor);  // 相当于更大的力去打
    }
    
    public double getMaxSpinPercentage() {
        return playerHand.getMaxSpinPercentage();  // 不影响
    }
    
    public double[] getCuePointMuSigmaXY() {
        double[] muSig = playerHand.getCuePointMuSigmaXY();
        return new double[]{
                muSig[0],
                muSig[1] / extension.factor,
                muSig[2],
                muSig[3] / extension.factor
        };
    }
    
    public double getPowerControl() {
        return playerHand.getPowerControl() * extension.factor;
    }
    
    public double getErrorMultiplierOfPower(double origSelPower) {
        return playerHand.getErrorMultiplierOfPower(origSelPower / extension.factor);
    }
}
