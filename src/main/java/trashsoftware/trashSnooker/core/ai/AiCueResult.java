package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.Algebra;

import java.util.Random;

public class AiCueResult {
    
    public double aiPrecisionFactor = 10000.0;
    
    private double unitX, unitY;
    private double selectedFrontBackSpin;  // 球手想要的高低杆，范围(-1.0, 1.0)，高杆正低杆负
    private double selectedPower;
    
    public AiCueResult(AiPlayStyle aiPlayStyle, 
                       double unitX, double unitY, 
                       double selectedFrontBackSpin,
                       double selectedPower) {
        this.unitX = unitX;
        this.unitY = unitY;
        this.selectedFrontBackSpin = selectedFrontBackSpin;
        this.selectedPower = selectedPower;
        
        applyRandomError(aiPlayStyle);
    }
    
    private void applyRandomError(AiPlayStyle aiPlayStyle) {
        Random random = new Random();
        double rad = Algebra.thetaOf(unitX, unitY);
        
        double sd = (100 - aiPlayStyle.precision) / aiPrecisionFactor;  // 再歪也歪不了太多吧？
        System.out.println("Random offset: " + sd);
        double afterRandom = random.nextGaussian() * sd + rad;
        
        double[] vecAfterRandom = Algebra.unitVectorOfAngle(afterRandom);
        unitX = vecAfterRandom[0];
        unitY = vecAfterRandom[1];
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
