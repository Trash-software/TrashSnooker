package trashsoftware.trashSnooker.core;

public class CuePlayParams {

    public final double vx;
    public double vy;

    public double xSpin;
    public double ySpin;
    public double sideSpin;

    /**
     * @param vx       x speed, in real, mm/s
     * @param vy       y speed, in real, mm/s
     * @param xSpin    由旋转产生的横向最大速度，mm/s
     * @param ySpin    由旋转产生的纵向最大速度，mm/s
     * @param sideSpin 由侧旋产生的最大速度，mm/s
     */
    public CuePlayParams(double vx, double vy, double xSpin, double ySpin,
                         double sideSpin) {
        this.vx = vx;
        this.vy = vy;
        this.xSpin = xSpin;
        this.ySpin = ySpin;
        this.sideSpin = sideSpin;
    }
}
