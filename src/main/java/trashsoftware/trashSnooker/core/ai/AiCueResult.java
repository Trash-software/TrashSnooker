package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.*;

import java.util.Random;

public class AiCueResult {
    
    public double aiPrecisionFactor = 10500.0;  // 越大，大家越准
    
    private double unitX, unitY;
    private final double selectedFrontBackSpin;  // 球手想要的高低杆，范围(-1.0, 1.0)，高杆正低杆负
    private final double selectedSideSpin;
    private final double selectedPower;
    private final boolean attack;
    private final double[] targetOrigPos;
    private final double[][] targetDirHole;
    private final Ball targetBall;
    
    public AiCueResult(PlayerPerson playerPerson,
                       GamePlayStage gamePlayStage,
                       boolean attack,
                       double[] targetOrigPos,
                       double[][] targetDirHole,
                       Ball targetBall,
                       double unitX, 
                       double unitY,
                       double selectedFrontBackSpin,
                       double selectedSideSpin,
                       double selectedPower) {
        this.unitX = unitX;
        this.unitY = unitY;
        this.selectedFrontBackSpin = selectedFrontBackSpin;
        this.selectedSideSpin = selectedSideSpin;
        this.selectedPower = selectedPower;
        this.attack = attack;
        this.targetOrigPos = targetOrigPos;
        this.targetDirHole = targetDirHole;
        this.targetBall = targetBall;
        
        applyRandomError(playerPerson, gamePlayStage);
    }

    public Ball getTargetBall() {
        return targetBall;
    }

    public double[] getTargetOrigPos() {
        return targetOrigPos;
    }

    public boolean isAttack() {
        return attack;
    }

    public double[][] getTargetDirHole() {
        return targetDirHole;
    }

    private void applyRandomError(PlayerPerson playerPerson, GamePlayStage gamePlayStage) {
        Random random = new Random();
        double rad = Algebra.thetaOf(unitX, unitY);
        
        double precisionFactor = aiPrecisionFactor;
        if (gamePlayStage == GamePlayStage.THIS_BALL_WIN || 
                gamePlayStage == GamePlayStage.ENHANCE_WIN) {
            precisionFactor *= (playerPerson.psy / 100);
        }
        
        double mistake = random.nextDouble() * 100;
        double mistakeFactor = 1.0;
        if (mistake > playerPerson.getAiPlayStyle().stability) {
            mistakeFactor = 2.0;
            System.out.println("Mistake");
        }
        
        double sd = (100 - playerPerson.getAiPlayStyle().precision) / precisionFactor;  // 再歪也歪不了太多吧？
        System.out.println("Precision factor: " + precisionFactor + ", Random offset: " + sd);
        double afterRandom = random.nextGaussian() * sd * mistakeFactor + rad;
        
        double[] vecAfterRandom = Algebra.unitVectorOfAngle(afterRandom);
        unitX = vecAfterRandom[0];
        unitY = vecAfterRandom[1];
    }

    public double getSelectedFrontBackSpin() {
        return selectedFrontBackSpin;
    }

    public double getSelectedSideSpin() {
        return selectedSideSpin;
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
