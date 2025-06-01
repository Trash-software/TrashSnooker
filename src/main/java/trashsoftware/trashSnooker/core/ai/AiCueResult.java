package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.attempt.CueType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class AiCueResult {

    public static final double DEFAULT_AI_PRECISION = 12500.0;
    protected static double aiPrecisionFactor = DEFAULT_AI_PRECISION;  // 越大，大家越准
    private final CueParams cueParams;
    private final CueType cueType;
    private final double[] targetOrigPos;
    private final double[][] targetDirHole;
    private final Ball targetBall;
    //    private final PlayerPerson.HandSkill handSkill;
    private final double frameImportance;
    //    private final boolean rua;
    private double unitX, unitY;
    private List<double[]> whitePath = new ArrayList<>();
    private final FinalChoice choice;  // 供记录

    public AiCueResult(InGamePlayer inGamePlayer,
                       GamePlayStage gamePlayStage,
                       CueType cueType,
                       double[] targetOrigPos,
                       double[][] targetDirHole,
                       Ball targetBall,
                       double unitX,
                       double unitY,
                       CueParams cueParams,
                       FinalChoice finalChoice,
                       double frameImportance) {
        this.unitX = unitX;
        this.unitY = unitY;
        this.choice = finalChoice;

        if (Double.isNaN(unitX) || Double.isNaN(unitY)) {
            throw new RuntimeException("Direction is NaN");
        }

        this.cueParams = cueParams;
        this.cueType = cueType;
        this.targetOrigPos = targetOrigPos;
        this.targetDirHole = targetDirHole;
        this.targetBall = targetBall;
//        this.handSkill = handSkill;
        this.frameImportance = frameImportance;

        applyRandomError(inGamePlayer, gamePlayStage);
    }

    public static void setAiPrecisionFactor(double aiGoodness) {
        aiPrecisionFactor = DEFAULT_AI_PRECISION * aiGoodness;
    }

    /**
     * 返回这一整局球员的心理因子，是除数。1为无影响，数值越大，影响越大。
     */
    public static double calculateFramePsyDivisor(double frameImportance,
                                                  double psy) {
        if (psy >= 90) return 1.0;
        return 1 + Algebra.shiftRange(
                0, 100,
                0, 1.8,
                frameImportance * (90 - psy));
    }

    public FinalChoice getChoice() {
        return choice;
    }

    public Ball getTargetBall() {
        return targetBall;
    }

    public double[] getTargetOrigPos() {
        return targetOrigPos;
    }

    public boolean isAttack() {
        return cueType == CueType.ATTACK || cueType == CueType.DOUBLE_POT;
    }

    public double[][] getTargetDirHole() {
        return targetDirHole;
    }

    public PlayerHand getHandSkill() {
        return cueParams.getHandSkill();
    }

    public CueType getCueType() {
        return cueType;
    }

    public List<double[]> getWhitePath() {
        return whitePath;
    }

    public void setWhitePath(List<double[]> whitePath) {
        this.whitePath = whitePath;
    }

    private void applyRandomError(InGamePlayer igp, GamePlayStage gamePlayStage) {
        Random random = new Random();
        double rad = Algebra.thetaOf(unitX, unitY);

        PlayerPerson person = igp.getPlayerPerson();

        double precisionFactor = aiPrecisionFactor;
        if (gamePlayStage == GamePlayStage.THIS_BALL_WIN ||
                gamePlayStage == GamePlayStage.ENHANCE_WIN) {
            precisionFactor *= (person.psyNerve / 100) * (1.0 - frameImportance * -0.5);
            System.out.println(gamePlayStage + ", precision: " + precisionFactor);
        } else if (gamePlayStage == GamePlayStage.BREAK) {
            precisionFactor *= 5.0;
        }

        // rua不rua
//        precisionFactor /= calculateFramePsyDivisor(frameImportance, );
        precisionFactor *= igp.getPsyStatus();

//        if (rua) {
//            // 打rua了，精度进一步降低
//            System.out.println("Ai player ruaed!");
//            precisionFactor *= (person.psyRua / 100);
//        }

//        precisionFactor /= calculateFramePsyDivisor(frameImportance, person.avgPsy());

        double mistake = random.nextDouble() * 100;
        double mistakeFactor = 1.0;
        double maxPrecision = 100.0;
        if (mistake > igp.getPlayerPerson().getAiPlayStyle().stability) {
            mistakeFactor = 2.0;
            maxPrecision = 90.0;
            System.out.println("Mistake");
        }

        // AI还不会传球
        double sd;
        if (cueType == CueType.ATTACK) {
            sd = (100 - person.getAiPlayStyle().precision) / precisionFactor;  // 再歪也歪不了太多吧？
            System.out.println("Precision factor: " + precisionFactor + ", Random offset: " + sd);
        } else if (cueType == CueType.DOUBLE_POT) {
//            sd = 0.000000000001;  // 测试用
            sd = (100 - person.getAiPlayStyle().doubleAbility) / precisionFactor;
        } else if (cueType == CueType.BREAK || gamePlayStage == GamePlayStage.BREAK) {
            sd = (100 - Math.max(person.getAiPlayStyle().precision,
                    person.getAiPlayStyle().defense)) / precisionFactor;
        } else if (cueType == CueType.SOLVE) {
            sd = (100 - person.getSolving()) / precisionFactor * 6.0;
//            System.out.println("Solving sd: " + sd);
        } else {
            sd = (100 - person.getAiPlayStyle().defense) / precisionFactor;
        }

//        double handSdMul = PlayerPerson.HandBody.getSdOfHand(getHandSkill());
        double handSdMul = 1.0;
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

    @Override
    public String toString() {
        return "AiCueResult{" +
                "cueParams=" + cueParams +
                ", cueType=" + cueType +
                ", targetOrigPos=" + Arrays.toString(targetOrigPos) +
                ", targetDirHole=" + Arrays.toString(targetDirHole) +
                ", targetBall=" + targetBall +
                ", frameImportance=" + frameImportance +
                ", unitX=" + unitX +
                ", unitY=" + unitY +
                ", whitePath=" + whitePath +
                '}';
    }

    public CueParams getCueParams() {
        return cueParams;
    }

    public double getUnitX() {
        return unitX;
    }

    public double getUnitY() {
        return unitY;
    }
}
