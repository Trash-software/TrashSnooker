package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.*;

import java.util.Random;

public class AiCueResult {

    private final double selectedFrontBackSpin;  // 球手想要的高低杆，范围(-1.0, 1.0)，高杆正低杆负
    private final double selectedSideSpin;
    private final double selectedPower;
    private final CueType cueType;
    private final double[] targetOrigPos;
    private final double[][] targetDirHole;
    private final Ball targetBall;
    private final PlayerPerson.HandSkill handSkill;
    public static final double DEFAULT_AI_PRECISION = 10500.0;
    private static double aiPrecisionFactor = DEFAULT_AI_PRECISION;  // 越大，大家越准
    private double unitX, unitY;

    public AiCueResult(InGamePlayer inGamePlayer,
                       GamePlayStage gamePlayStage,
                       CueType cueType,
                       double[] targetOrigPos,
                       double[][] targetDirHole,
                       Ball targetBall,
                       double unitX,
                       double unitY,
                       double selectedFrontBackSpin,
                       double selectedSideSpin,
                       double selectedPower,
                       PlayerPerson.HandSkill handSkill) {
        this.unitX = unitX;
        this.unitY = unitY;
        this.selectedFrontBackSpin = selectedFrontBackSpin;
        this.selectedSideSpin = selectedSideSpin;
        this.selectedPower = selectedPower;
        this.cueType = cueType;
        this.targetOrigPos = targetOrigPos;
        this.targetDirHole = targetDirHole;
        this.targetBall = targetBall;
        this.handSkill = handSkill;

        applyRandomError(inGamePlayer, gamePlayStage);
    }
    
    public static void setAiPrecisionFactor(double aiGoodness) {
        aiPrecisionFactor = DEFAULT_AI_PRECISION * aiGoodness;
    }

    public Ball getTargetBall() {
        return targetBall;
    }

    public double[] getTargetOrigPos() {
        return targetOrigPos;
    }

    public boolean isAttack() {
        return cueType == CueType.ATTACK;
    }

    public double[][] getTargetDirHole() {
        return targetDirHole;
    }

    public PlayerPerson.HandSkill getHandSkill() {
        return handSkill;
    }

    public CueType getCueType() {
        return cueType;
    }

    private void applyRandomError(InGamePlayer igp, GamePlayStage gamePlayStage) {
        Random random = new Random();
        double rad = Algebra.thetaOf(unitX, unitY);
        
        PlayerPerson person = igp.getPlayerPerson();

        double precisionFactor = aiPrecisionFactor;
        if (gamePlayStage == GamePlayStage.THIS_BALL_WIN ||
                gamePlayStage == GamePlayStage.ENHANCE_WIN) {
            precisionFactor *= (person.psy / 100);
            System.out.println(gamePlayStage + ", precision: " + precisionFactor);
        } else if (gamePlayStage == GamePlayStage.BREAK) {
            precisionFactor *= 5.0;
        }

        double mistake = random.nextDouble() * 100;
        double mistakeFactor = 1.0;
        double maxPrecision = 100.0;
        if (mistake > igp.getPlayerPerson().getAiPlayStyle().stability) {
            mistakeFactor = 2.0;
            maxPrecision = 90.0;
            System.out.println("Mistake");
        }

        double sd;
        if (cueType == CueType.ATTACK) {
            sd = (100 - person.getAiPlayStyle().precision) / precisionFactor;  // 再歪也歪不了太多吧？
            System.out.println("Precision factor: " + precisionFactor + ", Random offset: " + sd);
        } else if (cueType == CueType.BREAK || gamePlayStage == GamePlayStage.BREAK) {
            sd = (100 - Math.max(person.getAiPlayStyle().precision,
                    person.getAiPlayStyle().defense)) / precisionFactor;
        } else if (cueType == CueType.SOLVE) {
            sd = (100 - person.getSolving()) / precisionFactor * 10.0;
//            System.out.println("Solving sd: " + sd);
        } else {
            sd = (100 - person.getAiPlayStyle().defense) / precisionFactor;
        }

        double handSdMul = PlayerPerson.HandBody.getSdOfHand(handSkill);
        sd *= handSdMul;
        
        // 手感差时偏差大
        double handFeelMul = 1.0 / igp.getHandFeelEffort();
        sd *= handFeelMul;

        double afterRandom = random.nextGaussian() * sd * mistakeFactor + rad;
        afterRandom = Math.min(afterRandom, maxPrecision);

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

    public enum CueType {
        ATTACK,
        DEFENSE,
        BREAK,
        SOLVE
    }
}
