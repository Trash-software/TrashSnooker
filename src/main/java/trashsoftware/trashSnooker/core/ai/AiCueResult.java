package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.attempt.CueType;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.person.CuePlayerHand;
import trashsoftware.trashSnooker.core.person.PlayerPerson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class AiCueResult {

    public static final double DEFAULT_AI_PRECISION = 1.2;
    public static final double AI_PRECISION_MULTIPLIER = 11500.0;
//    public static final double DEFAULT_AI_PRECISION = 1.0;
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
    private List<double[]> targetStopRange = new ArrayList<>();
    private double totalPsyFactor;
    private final FinalChoice choice;
    private final GameValues gameValues;

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
                       GameValues gameValues,
                       double frameImportance) {
        this.unitX = unitX;
        this.unitY = unitY;
        this.choice = finalChoice;
        this.gameValues = gameValues;

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

        applyRandomAimingError(inGamePlayer, gamePlayStage);
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

    public List<double[]> getTargetStopRange() {
        return targetStopRange;
    }

    public void setTargetStopRange(List<double[]> targetStopRange) {
        this.targetStopRange = targetStopRange;
    }

    private void applyRandomAimingError(InGamePlayer igp, GamePlayStage gamePlayStage) {
        Random random = new Random();
        double rad = Algebra.thetaOf(unitX, unitY);

        PlayerPerson person = igp.getPlayerPerson();
        AiPlayStyle aps = person.getAiPlayStyle();

//        double precisionFactor = aiPrecisionFactor;

        double mistake = random.nextDouble() * 100;
        double maxPrecision = 100.0;
        double mistakeFactor = 0.0;
        if (mistake > igp.getPlayerPerson().getAiPlayStyle().stability) {
            maxPrecision = person.getPrecisionPercentage() * 0.9;
            mistakeFactor = random.nextBoolean() ? -0.6 : 0.6;
            System.out.println("Mistake");
        }

        double attackPrecision = Math.min(maxPrecision, person.getPrecisionPercentage());
        double doublePrecision = Math.min(maxPrecision, aps.doubleAbility);
        double defensePrecision = Math.min(maxPrecision, aps.defense);
        double solvePrecision = Math.min(maxPrecision, person.getSolving());

        totalPsyFactor = igp.getPsyMul(gamePlayStage, frameImportance);
        attackPrecision *= totalPsyFactor;
        defensePrecision *= totalPsyFactor;
        doublePrecision *= totalPsyFactor;
        solvePrecision *= totalPsyFactor;

        System.out.println("Final psy mul: " + totalPsyFactor + ", precision (atk): " + attackPrecision);

        double aimPointSdMm, whiteAimDt;
        if (choice == null) {
            // random angry cue 可能进这个分支
            aimPointSdMm = 25;
            whiteAimDt = 1000;
        } else {
            double[] aimedPos = choice.getAimedPos();
            whiteAimDt = Math.hypot(aimedPos[0] - whiteOrigPos[0], aimedPos[1] - whiteOrigPos[1]) - gameValues.ball.ballDiameter;
            whiteAimDt = Math.max(whiteAimDt, gameValues.ball.ballRadius);  // 安全起见

            // 在预想的点处的线距离标准差
            aimPointSdMm = switch (cueType) {
                case ATTACK -> {
                    if (choice instanceof FinalChoice.IntegratedAttackChoice iac) {
                        double fixedOffset = (105 - attackPrecision) * 0.125 / Math.pow(person.getLongPrecision(), 2);  // 类似视线误差这种
                        double normalOffset = (105 - attackPrecision) * 0.125;
                        double personAngle = person.getAnglePrecision();
                        if (personAngle != 1.0) {
                            double angle = iac.attackParams.getAttackChoice().getAngleRad();
                            double angleFactor = Algebra.powerTransferOfAngle(angle);  // 直球是1，极限薄球是0
                            double div = Algebra.shiftRangeSafe(1.0, 0.0,
                                    1.0, personAngle, angleFactor);
                            normalOffset /= div;
                        }
                        yield normalOffset + fixedOffset;
                    } else {
                        System.err.println("Choice is " + choice + " when direct attacking");
                        yield 25;
                    }
                }
                case DOUBLE_POT -> {
                    double fixedOffset = (105 - attackPrecision) * 0.125 / Math.pow(person.getLongPrecision(), 2);  // 类似视线误差这种
                    yield fixedOffset + (105 - doublePrecision) * 0.125;
                }
                case DEFENSE -> (105 - defensePrecision) * 0.25;  // 防守为0的可能会歪一整颗球的半径
                case SOLVE -> (105 - solvePrecision) * 0.25;
                case BREAK -> (105 - Math.max(attackPrecision, defensePrecision)) * 0.25;
                case PASS_POT -> 25;  // 没做
            };
        }
        
        // 手对瞄准还是有影响的
        aimPointSdMm /= person.handBody.getHandAimingSkill(getCuePlayerHand().playerHand);
        
//        aimPointSdMm *= 0.8333;  // 一个莫名其妙的统一修正值
        // 距离和瞄准难度不是线性关系，远的没那么难瞄，但距离远了确实也看不那么清，所以这里来个pow折中一下
        // 次数越大，长台越难
        double dtMul = Math.pow(whiteAimDt / Values.MAX_DISTANCE / 2.2, 0.5);
//        System.out.println("aimSdMm=" + aimPointSdMm + ", dtMul=" + dtMul);
        aimPointSdMm *= dtMul;

        aimPointSdMm /= aiPrecisionFactor;

        // 照理来说，手应该不太影响瞄？
//        double handSdMul = 1.0;
//        aimPointSdMm *= handSdMul;

        // 手感差时偏差大
        double handFeelMul = 1.0 / igp.getHandFeelEffort();
        aimPointSdMm *= handFeelMul;

        // 抬高杆尾会难以瞄准
        double visionFarness = Math.tan(Math.toRadians(cueParams.getCueAngleDeg()));
        aimPointSdMm *= visionFarness / CueParams.TAN_OF_CUE_ANGLE_DEG;
        
        aimPointSdMm = Math.min(aimPointSdMm, 50);  // 再歪歪不出一颗球远

        double sdRad = Math.atan2(aimPointSdMm, whiteAimDt);

        double radDeviation = (random.nextGaussian() + mistakeFactor) * sdRad;
//        double radDeviation = 2 * sdRad;
        double afterRandom = rad + radDeviation;

        double[] vecAfterRandom = Algebra.unitVectorOfAngle(afterRandom);
        unitX = vecAfterRandom[0];
        unitY = vecAfterRandom[1];
    }
    
    private static double regulateErrorByDistance(double radError, double distance) {
        // 实际上瞄点的误差应该是平移误差，这里的sd是角度误差，导致AI长台太差，近台太准，所以需要修正
        // 但距离远了确实也看不那么清，所以这里来个pow折中一下
        // todo: 什么时候和AttackParam里面那套算法统一一下
        radError = Math.clamp(radError, 0, Math.PI - 1e-8);
        double dtDiv = Math.pow(distance / Values.MAX_DISTANCE * 3.3, 0.33);  // 随手设成长台判定2.2的1.5倍
        double mmError = Math.tan(radError) * distance;
        mmError /= dtDiv;
        return Math.atan2(mmError, distance);
    }

    private void applyRandomAimingError2(InGamePlayer igp, GamePlayStage gamePlayStage) {
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

        totalPsyFactor = igp.getPsyMul(gamePlayStage, frameImportance);
        attackPrecision *= totalPsyFactor;
        defensePrecision *= totalPsyFactor;

        System.out.println("Final psy mul: " + totalPsyFactor + ", precision (atk): " + attackPrecision);
        
        double sd;
        double distanceToAimPoint = 100;  // 随便预设的值
        if (cueType == CueType.ATTACK) {
            sd = (101 - attackPrecision) / precisionFactor;  // 再歪也歪不了太多吧？
            // 处理AI球员长台/大角度球的能力修正
            if (choice instanceof FinalChoice.IntegratedAttackChoice iac) {
                double whiteTarDt = iac.attackParams.getAttackChoice().whiteCollisionDistance;
                double longFactor = whiteTarDt / Values.MAX_DISTANCE * 2.2;
                double personLong = person.getLongPrecision();
                if (personLong != 1.0) {
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
                distanceToAimPoint = whiteTarDt;
            } else {
                System.err.println("Choice is " + choice + ", cannot apply attack long precision and angle precision");
            }

            System.out.println("Precision factor: " + precisionFactor + ", Random offset: " + sd);
        } else if (cueType == CueType.DOUBLE_POT) {
//            sd = 0.000000000001;  // 测试用
            sd = (101 - person.getAiPlayStyle().doubleAbility) / precisionFactor * 1.1;
            if (choice instanceof FinalChoice.IntegratedAttackChoice iac) {
                double[] colPos = iac.attackParams.attackChoice.collisionPos;
                distanceToAimPoint = Math.hypot(whiteOrigPos[0] - colPos[0], whiteOrigPos[1] - colPos[1]);
            } else {
                System.err.println("Choice is " + choice + ", which is not a double attack");
            }
        } else if (cueType == CueType.BREAK || gamePlayStage == GamePlayStage.BREAK) {
            sd = (101 - Math.max(attackPrecision,
                    defensePrecision)) / precisionFactor;
            distanceToAimPoint = 2000;  // 这无所谓
        } else if (cueType == CueType.SOLVE) {
            sd = (101 - person.getSolving()) / precisionFactor * 5.0;
            double[] cushionPos = choice.wp.getWhiteFirstCushionPos();
            distanceToAimPoint = Math.hypot(whiteOrigPos[0] - cushionPos[0], whiteOrigPos[1] - cushionPos[1]);
//            System.out.println("Solving sd: " + sd);
        } else if (cueType == CueType.PASS_POT) {
            // AI还不会传球
            sd = 90 / precisionFactor;
        } else {
            sd = (101 - defensePrecision) / precisionFactor;
            distanceToAimPoint = Math.hypot(whiteOrigPos[0] - choice.wp.getWhiteCollisionX(), 
                    whiteOrigPos[1] - choice.wp.getWhiteCollisionY());
        }
        System.out.println("SD before: " + sd);
        sd = regulateErrorByDistance(sd, distanceToAimPoint);
        System.out.println("SD after: " + sd + ", dt is: " + distanceToAimPoint);
        
        final double initSd = sd;

//        double handSdMul = PlayerPerson.HandBody.getSdOfHand(getHandSkill());
        double handSdMul = 1.0;
        sd *= handSdMul;

        // 手感差时偏差大
        double handFeelMul = 1.0 / igp.getHandFeelEffort();
        sd *= handFeelMul;
        
        // 心态
//        totalPsyFactor = igp.getPsyMul(gamePlayStage, frameImportance);
//        sd /= totalPsyFactor;
//        System.out.println("Final psy mul: " + totalPsyFactor + ", init sd -> sd: " + initSd + " -> " + sd);
        
        double radDeviation = random.nextGaussian() * sd * mistakeFactor;
//        double radDeviation = sd * 3;
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
