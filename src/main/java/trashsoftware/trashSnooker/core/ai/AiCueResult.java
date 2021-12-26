package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.CuePlayParams;

public class AiCueResult {
    
    private final CuePlayParams cuePlayParams;
    private double unitX, unitY;
    private double selectedFrontBackSpin;  // 球手想要的高低杆，范围(-1.0, 1.0)，高杆正低杆负
    private double selectedPower;
    
    public AiCueResult(CuePlayParams cuePlayParams, 
                       double unitX, double unitY, 
                       double selectedFrontBackSpin,
                       double selectedPower) {
        this.cuePlayParams = cuePlayParams;
        this.unitX = unitX;
        this.unitY = unitY;
        this.selectedFrontBackSpin = selectedFrontBackSpin;
        this.selectedPower = selectedPower;
    }

    public CuePlayParams getCuePlayParams() {
        return cuePlayParams;
    }

    public double getSelectedFrontBackSpin() {
        return selectedFrontBackSpin;
    }

    public double getUnitX() {
        return unitX;
    }

    public double getUnitY() {
        return unitY;
    }

    public double getSelectedPower() {
        return selectedPower;
    }
}
