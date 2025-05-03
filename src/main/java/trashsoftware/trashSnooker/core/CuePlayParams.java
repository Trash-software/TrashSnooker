package trashsoftware.trashSnooker.core;

import org.jetbrains.annotations.Nullable;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CuePlayParams {

    public static final double SIDE_SPIN_DEVIATION_DIVISOR = 4000.0;  // 加塞的方向偏差，这个值越大，加塞偏移越小。

    public final double vx;
    public final double vy;

    // 记录向，不参与实际运算
    public final CueParams cueParams;
//    public final boolean slideCue;  // 是否滑杆，也是记录向

    public double xSpin;
    public double ySpin;
    public double sideSpin;

    private final boolean miscued;

    /**
     * @param vx        x speed, in real, mm/s
     * @param vy        y speed, in real, mm/s
     * @param xSpin     由旋转产生的横向最大速度，mm/s
     * @param ySpin     由旋转产生的纵向最大速度，mm/s
     * @param sideSpin  由侧旋产生的最大速度，mm/s
     * @param miscued   是否滑杆
     * @param cueParams 记录
     */
    protected CuePlayParams(double vx, double vy, double xSpin, double ySpin,
                            double sideSpin, boolean miscued, CueParams cueParams) {
        this.vx = vx;
        this.vy = vy;
        this.xSpin = xSpin;
        this.ySpin = ySpin;
        this.sideSpin = sideSpin;
        this.miscued = miscued;

        this.cueParams = cueParams;
    }
    
    public CuePlayParams deviated(double newVx, double newVy) {
        return new CuePlayParams(newVx,
                newVy,
                xSpin,
                ySpin,
                sideSpin,
                miscued, 
                cueParams);
    }

    public static CuePlayParams makeIdealParams(double directionX, double directionY,
                                                CueParams cueParams) {
        return makeIdealParams(directionX, directionY,
                cueParams,
                false);
    }

    /**
     * Ideal指不带随机
     *
     * @param directionX selected x direction
     * @param directionY selected y direction
     */
    public static CuePlayParams makeIdealParams(double directionX,
                                                double directionY,
                                                CueParams cueParams,
                                                boolean slideCue) {
        double cueAngleDeg = cueParams.getCueAngleDeg();
        double directionalPower = cueParams.actualPower();
        double directionalSideSpin = cueParams.actualSideSpin();  // 参与击球方向计算的sideSpin
        if (slideCue) {
            cueParams.setAbsolutePower(cueParams.actualPower() / 4.0);
            directionalSideSpin = cueParams.actualSideSpin() * 10.0;
            cueParams.setAbsoluteSideSpin(cueParams.actualSideSpin() / 4.0);
        }

        double[] unitXYWithSpin = unitXYWithSpins(directionalSideSpin, directionalPower, directionX, directionY);

        double speed = getSpeedOfPower(cueParams.actualPower(), cueAngleDeg);
        double vx = unitXYWithSpin[0] * speed;
        double vy = unitXYWithSpin[1] * speed;

        // 用于计算旋转 的球速
        double origSpeed = getSpeedOfPower(cueParams.actualPower(), cueAngleDeg);
//        double powerGenSpeed;
//        if (cueAngleDeg < 30) {
//            powerGenSpeed = origSpeed * Algebra.shiftRangeSafe(0, 30,
//                    1, 0.75,
//                    cueAngleDeg);
//        } else {
//            // 抬得够高了，再高差不多了，反正都不舒服
//            powerGenSpeed = origSpeed * 0.75;
//        }

        // 重新计算，因为unitSideSpin有呲杆补偿
        double[] spins = calculateSpins(vx,
                vy,
                origSpeed,
                cueParams.actualFrontBackSpin(),
                cueParams.actualSideSpin(),
                cueAngleDeg);
//        System.out.println(cueParams);
//        System.out.println("spins: " + Arrays.toString(spins));
        return new CuePlayParams(vx, vy, spins[0], spins[1], spins[2], slideCue, cueParams);
    }

    public static double getSpeedOfPower(double actualPower, double cueAngleDeg) {
        double speed = actualPower * Values.MAX_POWER_SPEED / 100.0;  // 常量，最大力白球速度
        if (cueAngleDeg > 5) {
            // 出杆越陡，球速越慢。这里我们不用三角函数，因为这不是传力的问题，这是人发力的问题
            speed *= (95 - cueAngleDeg) / 90.0;
        }
        return speed;
    }

    public static double getSelectedFrontBackSpin(double actualFbSpin,
                                                  @Nullable PlayerHand hand,
                                                  Cue cue) {
        double percentage = hand == null ? 100 : hand.getMaxSpinPercentage();
        return actualFbSpin / cue.getSpinMultiplier() /
                (percentage / 100.0);
    }

    public static double unitFrontBackSpin(double unitCuePoint,
                                           @Nullable PlayerHand hand,
                                           Cue cue) {
        double percentage = hand == null ? 100 : hand.getMaxSpinPercentage();
        return unitCuePoint * cue.getSpinMultiplier() *
                percentage / 100.0;
    }

    public static double getSelectedSideSpin(double actualSideSpin, Cue cue) {
        return actualSideSpin / cue.getSpinMultiplier();
    }

    public static double unitSideSpin(double unitCuePoint, Cue cue) {
        return unitCuePoint * cue.getSpinMultiplier();
    }

    public static double[] unitXYWithSpins(double unitSideSpin, double power,
                                           double cueDirX, double cueDirY) {
        double offsetAngleRad = -unitSideSpin * power / SIDE_SPIN_DEVIATION_DIVISOR;
//        offsetAngleRad = 0;
        return Algebra.rotateVector(cueDirX, cueDirY, offsetAngleRad);
    }

    public static double[] aimingUnitXYIfSpin(double unitSideSpin, double power,
                                              double cueDirX, double cueDirY) {
        double offsetAngleRad = -unitSideSpin * power / SIDE_SPIN_DEVIATION_DIVISOR;
//        offsetAngleRad = 0;
        return Algebra.rotateVector(cueDirX, cueDirY, -offsetAngleRad);
    }

    /**
     * 返回这个位置可用的所有手，以优先级排序
     */
    public static List<PlayerHand.Hand> getPlayableHands(double whiteX, double whiteY,
                                                         double aimingX, double aimingY,
                                                         double cueAngleDeg,
                                                         TableMetrics tableMetrics,
                                                         PlayerPerson person) {
        List<PlayerHand.Hand> result = new ArrayList<>();
        result.add(PlayerHand.Hand.REST);

        double[][] standingPosLeft = personStandingPosition(whiteX, whiteY,
                aimingX, aimingY,
                cueAngleDeg,
                person, PlayerHand.Hand.LEFT);

        if (!tableMetrics.isInOuterTable(standingPosLeft[0][0], standingPosLeft[0][1]) ||
                !tableMetrics.isInOuterTable(standingPosLeft[1][0], standingPosLeft[1][1])) {
            result.add(PlayerHand.Hand.LEFT);
        }

        double[][] standingPosRight = personStandingPosition(whiteX, whiteY,
                aimingX, aimingY,
                cueAngleDeg,
                person, PlayerHand.Hand.RIGHT);

        if (!tableMetrics.isInOuterTable(standingPosRight[0][0], standingPosRight[0][1]) ||
                !tableMetrics.isInOuterTable(standingPosRight[1][0], standingPosRight[1][1])) {
            result.add(PlayerHand.Hand.RIGHT);
        }

        result.sort(Comparator.comparingInt(person.handBody::precedenceOfHand));

        return result;
    }

    public static PlayerHand getPlayableHand(double whiteX, double whiteY,
                                             double aimingX, double aimingY,
                                             double cueAngleDeg,
                                             TableMetrics tableMetrics,
                                             PlayerPerson person) {
        PlayerHand primary = person.handBody.getPrimary();
        if (primary.hand == PlayerHand.Hand.REST) {
            return primary;
        }

        double[][] standingPosPri = personStandingPosition(whiteX, whiteY,
                aimingX, aimingY,
                cueAngleDeg,
                person, primary.hand);

        if (!tableMetrics.isInOuterTable(standingPosPri[0][0], standingPosPri[0][1]) ||
                !tableMetrics.isInOuterTable(standingPosPri[1][0], standingPosPri[1][1])) {
            return primary;
        }

        PlayerHand secondary = person.handBody.getSecondary();
        if (secondary.hand == PlayerHand.Hand.REST) {
            return secondary;
        }
        double[][] standingPosSec = personStandingPosition(whiteX, whiteY,
                aimingX, aimingY,
                cueAngleDeg,
                person, secondary.hand);

        if (!tableMetrics.isInOuterTable(standingPosSec[0][0], standingPosSec[0][1]) ||
                !tableMetrics.isInOuterTable(standingPosSec[1][0], standingPosSec[1][1])) {
            return secondary;
        }

        PlayerHand third = person.handBody.getThird();
        assert third.hand == PlayerHand.Hand.REST;
        return third;
    }

    public static double[][] personStandingPosition(double whiteX, double whiteY,
                                                    double aimingX, double aimingY,
                                                    double cueAngleDeg,
                                                    PlayerPerson person,
                                                    PlayerHand.Hand hand) {
        double upBodyLength = person.handBody.height * 10 - 851;
        double heightMul = 1.75 * Math.cos(Math.toRadians(cueAngleDeg));
        double personLengthX = upBodyLength * -aimingX * heightMul;
        double personLengthY = upBodyLength * -aimingY * heightMul;

        int mul = hand == PlayerHand.Hand.LEFT ? -1 : 1;
        double widthMulMin = person.handBody.bodyWidth * 280.0 * mul;
        double widthMulMax = upBodyLength * 0.68 * mul;
        double personWidthX1 = aimingY * widthMulMin;
        double personWidthY1 = -aimingX * widthMulMin;
        double personWidthX2 = aimingY * widthMulMax;
        double personWidthY2 = -aimingX * widthMulMax;

        return new double[][]{
                {whiteX + personLengthX + personWidthX1,
                        whiteY + personLengthY + personWidthY1},
                {whiteX + personLengthX + personWidthX2,
                        whiteY + personLengthY + personWidthY2},
        };
    }

    /**
     * @param vx
     * @param vy
     * @param personPowerSpeed 考虑抬高杆尾之后，球员发力产生的杆法效果
     * @param frontBackSpin    高杆正，低杆负
     * @param sideSpin         右塞正（顶视的逆时针），左塞负
     * @param cueAngleDeg
     * @return
     */
    public static double[] calculateSpins(double vx,
                                          double vy,
                                          double personPowerSpeed,
                                          double frontBackSpin,
                                          double sideSpin,
                                          double cueAngleDeg) {
        double speed = Math.hypot(vx, vy);

//        double frontBackSpin = getUnitFrontBackSpin();  // 
//        double leftRightSpin = getUnitSideSpin();  // 
        if (frontBackSpin > 0) {
            // 高杆补偿
            double factor = Algebra.shiftRangeSafe(
                    0, 1,
                    1, Values.FRONT_SPIN_FACTOR,
                    frontBackSpin);
            frontBackSpin *= factor;
        }

        // 小力高低杆补偿，pow越小，补偿越多
//        double spinRatio = Math.pow(speed / Values.MAX_POWER_SPEED, Values.SMALL_POWER_SPIN_EXP);
//        double sideSpinRatio = Math.pow(speed / Values.MAX_POWER_SPEED, Values.SMALL_POWER_SIDE_SPIN_EXP);
        double spinRatio = Math.pow(personPowerSpeed / Values.MAX_POWER_SPEED, Values.SMALL_POWER_SPIN_EXP);
        double sideSpinRatio = Math.pow(personPowerSpeed / Values.MAX_POWER_SPEED, Values.SMALL_POWER_SIDE_SPIN_EXP);

        double side = sideSpinRatio * sideSpin * Values.MAX_SIDE_SPIN_SPEED;
        // 旋转产生的总目标速度
        double spinSpeed = spinRatio * frontBackSpin * Values.MAX_SPIN_SPEED;

        // (spinX, spinY)是一个向量，指向球因为旋转想去的方向
        double spinX = vx * (spinSpeed / speed);
        double spinY = vy * (spinSpeed / speed);
//        System.out.printf("x %f, y %f, total %f, side %f\n", spinX, spinY, spinSpeed, side);

        double cosMbu = Math.cos(Math.toRadians(cueAngleDeg));
        double mbummeMag = mbummeMag(cosMbu);  // 扎杆强度
        double[] norm = Algebra.normalVector(vx, vy);  // 法线，扎杆就在这个方向
        double mbummeX = side * -norm[0] * mbummeMag;
        double mbummeY = side * -norm[1] * mbummeMag;

        // 扎杆使侧旋转化为普通旋转
        side *= cosMbu;

//        System.out.printf("spin x: %f, spin y: %f, mx: %f, my: %f\n",
//                spinX, spinY, mbummeX, mbummeY);

        return new double[]{spinX + mbummeX, spinY + mbummeY, side};
    }
    
    public static double mbummeMag(double cosCueAngle) {
        return (1 - cosCueAngle) / 2000;
    }

    public boolean isMiscued() {
        return miscued;
    }
}
