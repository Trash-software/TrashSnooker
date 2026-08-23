package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.attempt.CueType;
import trashsoftware.trashSnooker.core.person.CuePlayerHand;
import trashsoftware.trashSnooker.core.person.PlayerPerson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class AiCueResult {

    public static final double DEFAULT_AI_PRECISION = 15000.0;
    protected static double aiPrecisionFactor = DEFAULT_AI_PRECISION;  // 越大，大家越准
    private final CueParams cueParams;
    private final CueType cueType;
    private final double[] whiteOrigPos;
    private final double[] targetOrigPos;
    private final double[][] targetDirHole;
    private final Ball targetBall;
    //    private final PlayerPerson.HandSkill handSkill;
    private final double frameImportance;
    //    private final boolean rua;
    private double unitX, unitY;
    private List<double[]> whitePath = new ArrayList<>();
    private List<double[]> whiteStopRange = new ArrayList<>();
    private double totalPsyFactor;
    private final FinalChoice choice;  // 供记录

    public AiCueResult(InGamePlayer inGamePlayer,
                       GamePlayStage gamePlayStage,
                       CueType cueType,
                       double[] whiteOrigPos,
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
        this.whiteOrigPos = whiteOrigPos;
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

    public CuePlayerHand getCuePlayerHand() {
        return cueParams.getCuePlayerHand();
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

    public List<double[]> getWhiteStopRange() {
        return whiteStopRange;
    }

    public void setWhiteStopRange(List<double[]> whiteStopRange) {
        this.whiteStopRange = whiteStopRange;
    }

    private void applyRandomError(InGamePlayer igp, GamePlayStage gamePlayStage) {
        Random random = new Random();
        double rad = Algebra.thetaOf(unitX, unitY);

        PlayerPerson person = igp.getPlayerPerson();

        double precisionFactor = aiPrecisionFactor;

        double mistake = random.nextDouble() * 100;
        double mistakeFactor = 1.0;
        double maxPrecision = 100.0;
        if (mistake > igp.getPlayerPerson().getAiPlayStyle().stability) {
            mistakeFactor = 2.0;
            maxPrecision = 90.0;
            System.out.println("Mistake");
        }
        
        double attackPrecision = Math.min(maxPrecision, person.getPrecisionPercentage());
        double defensePrecision = Math.min(maxPrecision, person.getAiPlayStyle().defense);

        // AI还不会传球
        double sd;
        if (cueType == CueType.ATTACK) {
            sd = (105 - attackPrecision) / precisionFactor;  // 再歪也歪不了太多吧？
            // 处理AI球员长台/大角度球的能力修正
            if (choice instanceof FinalChoice.IntegratedAttackChoice iac) {
                double personLong = person.getLongPrecision();
                if (personLong != 1.0) {
                    double whiteTarDt = iac.attackParams.getAttackChoice().whiteCollisionDistance;
                    double longFactor = whiteTarDt / Values.MAX_DISTANCE * 2.2;
                    if (longFactor > 1.0) {
                        // 是长台
                        double div = Algebra.shiftRangeSafe(1.0, 2.2,
                                1.0, personLong, longFactor);
                        sd /= div;
                    }
                }

                double personAngle = person.getAnglePrecision();
                if (personAngle != 1.0) {
                    double angle = iac.attackParams.getAttackChoice().getAngleRad();
                    double angleFactor = Algebra.powerTransferOfAngle(angle);  // 直球是1，极限薄球是0
                    double div = Algebra.shiftRangeSafe(1.0, 0.0,
                            1.0, personAngle, angleFactor);
                    sd /= div;
                }
            } else {
                System.err.println("Choice is " + choice + ", cannot apply attack long precision and angle precision");
            }

            System.out.println("Precision factor: " + precisionFactor + ", Random offset: " + sd);
        } else if (cueType == CueType.DOUBLE_POT) {
//            sd = 0.000000000001;  // 测试用
            sd = (105 - person.getAiPlayStyle().doubleAbility) / precisionFactor * 1.25;
        } else if (cueType == CueType.BREAK || gamePlayStage == GamePlayStage.BREAK) {
            sd = (105 - Math.max(attackPrecision,
                    defensePrecision)) / precisionFactor;
        } else if (cueType == CueType.SOLVE) {
            sd = (105 - person.getSolving()) / precisionFactor * 5.0;
//            System.out.println("Solving sd: " + sd);
        } else {
            sd = (105 - defensePrecision) / precisionFactor;
        }
        
        final double initSd = sd;

//        double handSdMul = PlayerPerson.HandBody.getSdOfHand(getHandSkill());
        double handSdMul = 1.0;
        sd *= handSdMul;

        // 手感差时偏差大
        double handFeelMul = 1.0 / igp.getHandFeelEffort();
        sd *= handFeelMul;
        
        // 心态
        totalPsyFactor = igp.getPsyMul(gamePlayStage, frameImportance);
        sd /= totalPsyFactor;
        System.out.println("Final psy mul: " + totalPsyFactor + ", init sd -> sd: " + initSd + " -> " + sd);
        
        double radDeviation = random.nextGaussian() * sd * mistakeFactor;
        if (whiteOrigPos == null || targetOrigPos == null) {
            System.err.println("No white orig pos or target orig pos, random angry cue?");
        } else {
            double maxRadDeviation = Math.atan2(targetBall.getRadius() * 2,
                    Math.hypot(targetOrigPos[0] - whiteOrigPos[0], targetOrigPos[1] - whiteOrigPos[1]));
            radDeviation = Math.clamp(radDeviation, -maxRadDeviation, maxRadDeviation);
        }

        double afterRandom = rad + radDeviation;
//        afterRandom = Math.min(afterRandom, maxPrecision);

        double[] vecAfterRandom = Algebra.unitVectorOfAngle(afterRandom);
        unitX = vecAfterRandom[0];
        unitY = vecAfterRandom[1];
    }

    public double getTotalPsyFactor() {
        return totalPsyFactor;
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
