package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.recorder.CueRecord;

public class CuePlayParams {

    public final double vx;
    public final double vy;
    // 记录向，不参与实际运算
    public final double power;
    public double xSpin;
    public double ySpin;
    public double sideSpin;

    /**
     * @param vx          x speed, in real, mm/s
     * @param vy          y speed, in real, mm/s
     * @param xSpin       由旋转产生的横向最大速度，mm/s
     * @param ySpin       由旋转产生的纵向最大速度，mm/s
     * @param sideSpin    由侧旋产生的最大速度，mm/s
     * @param actualPower 实际的力量，考虑球员力量、球杆、球的重量
     */
    public CuePlayParams(double vx, double vy, double xSpin, double ySpin,
                         double sideSpin, double actualPower) {
        this.vx = vx;
        this.vy = vy;
        this.xSpin = xSpin;
        this.ySpin = ySpin;
        this.sideSpin = sideSpin;

        this.power = actualPower;
    }

    public static CuePlayParams makeIdealParams(double directionX, double directionY,
                                                double actualFrontBackSpin, double actualSideSpin,
                                                double cueAngleDeg,
                                                double actualPower) {

        if (actualPower < Values.MIN_SELECTED_POWER) actualPower = Values.MIN_SELECTED_POWER;

        double[] unitXYWithSpin = unitXYWithSpins(actualSideSpin, actualPower, directionX, directionY);

        double vx = unitXYWithSpin[0] * actualPower * Values.MAX_POWER_SPEED / 100.0;  // 常量，最大力白球速度
        double vy = unitXYWithSpin[1] * actualPower * Values.MAX_POWER_SPEED / 100.0;

        // 重新计算，因为unitSideSpin有呲杆补偿
        double[] spins = calculateSpins(vx, vy, actualFrontBackSpin, actualSideSpin, cueAngleDeg);
        if (cueAngleDeg > 5) {
            // 出杆越陡，球速越慢
            vx *= (95 - cueAngleDeg) / 90.0;
            vy *= (95 - cueAngleDeg) / 90.0;
        }
        return new CuePlayParams(vx, vy, spins[0], spins[1], spins[2], actualPower);
    }

    public static double unitFrontBackSpin(double unitCuePoint, InGamePlayer inGamePlayer,
                                           Cue cue) {
        return unitCuePoint * cue.spinMultiplier *
                inGamePlayer.getPlayerPerson().getMaxSpinPercentage() / 100.0;
    }

    public static double unitSideSpin(double unitCuePoint, Cue cue) {
        return unitCuePoint * cue.spinMultiplier;
    }

    public static double[] unitXYWithSpins(double unitSideSpin, double power,
                                           double cueDirX, double cueDirY) {
        double offsetAngleRad = -unitSideSpin * power / 2400.0;
        return Algebra.rotateVector(cueDirX, cueDirY, offsetAngleRad);
    }

    public static double[] aimingUnitXYIfSpin(double unitSideSpin, double power,
                                              double cueDirX, double cueDirY) {
        double offsetAngleRad = -unitSideSpin * power / 2400.0;
        return Algebra.rotateVector(cueDirX, cueDirY, -offsetAngleRad);
    }

    /**
     * @param vx
     * @param vy
     * @param frontBackSpin 高杆正，低杆负
     * @param sideSpin      右塞正（顶视的逆时针），左塞负
     * @param cueAngleDeg
     * @return
     */
    public static double[] calculateSpins(double vx, double vy,
                                          double frontBackSpin, double sideSpin,
                                          double cueAngleDeg) {
        double speed = Math.hypot(vx, vy);

//        double frontBackSpin = getUnitFrontBackSpin();  // 
//        double leftRightSpin = getUnitSideSpin();  // 
        if (frontBackSpin > 0) {
            // 高杆补偿
            frontBackSpin *= Values.HIGH_CUE_FACTOR;
        }

        // 小力高低杆补偿
        double spinRatio = Math.pow(speed / Values.MAX_POWER_SPEED, 0.45);
        double sideSpinRatio = Math.pow(speed / Values.MAX_POWER_SPEED, 0.75);

        double side = sideSpinRatio * sideSpin * Values.MAX_SIDE_SPIN_SPEED;
        // 旋转产生的总目标速度
        double spinSpeed = spinRatio * frontBackSpin * Values.MAX_SPIN_SPEED;

        // (spinX, spinY)是一个向量，指向球因为旋转想去的方向
        double spinX = vx * (spinSpeed / speed);
        double spinY = vy * (spinSpeed / speed);
//        System.out.printf("x %f, y %f, total %f, side %f\n", spinX, spinY, spinSpeed, side);

        double mbummeMag = cueAngleDeg / 90.0 / 1000;  // 扎杆强度
        double[] norm = Algebra.normalVector(vx, vy);  // 法线，扎杆就在这个方向
        double mbummeX = side * -norm[0] * mbummeMag;
        double mbummeY = side * -norm[1] * mbummeMag;

        if (cueAngleDeg > 5) {
            // 扎杆在一定程度上减弱其他杆法
            double mul = (95 - cueAngleDeg) / 90.0;
            side *= mul;
            spinX *= mul;
            spinY *= mul;
        }

//        System.out.printf("spin x: %f, spin y: %f, mx: %f, my: %f\n",
//                spinX, spinY, mbummeX, mbummeY);

        return new double[]{spinX + mbummeX, spinY + mbummeY, side};
    }
}
