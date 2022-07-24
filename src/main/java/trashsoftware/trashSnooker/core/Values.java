package trashsoftware.trashSnooker.core;

import javafx.scene.paint.Color;

public class Values {
    public static final double SNOOKER_OUTER_WIDTH = 3820.0;
    public static final double SNOOKER_OUTER_HEIGHT = 2035.0;

    public static final double MAX_LENGTH = Math.hypot(SNOOKER_OUTER_WIDTH, SNOOKER_OUTER_HEIGHT);  // 对角线长度

//    public static final double WALL_BOUNCE_RATIO = 0.92;
//    public static final double BALL_BOUNCE_RATIO = 0.97;
//    public static final double WALL_SPIN_PRESERVE_RATIO = 0.75;

    // 袋角直线的法向量
//    public static final double[] NORMAL_45 = {1, 1};  // 左下至右上
//    public static final double[] NORMAL_315 = {1, -1};  // 左上至右下

    public static final double MIN_SELECTED_POWER = 1.0;
    // 击球最大速度，mm/s
    public static final double MAX_POWER_SPEED = 6400.0;
    // 由旋转产生的理论最大瞬时速度（该速度不可能达到），mm/s
    public static final double MAX_SPIN_SPEED = 6400.0;
    // 因为ball的spin值是球想要达到的速度，范围
    public static final double HIGH_CUE_FACTOR = 1.6;
    // 由侧旋产生的最大瞬时速度，mm/s
    public static final double MAX_SIDE_SPIN_SPEED = 1600.0;
    public static final int DETAILED_PHYSICAL = 12;
    // 每两次物理碰撞运算之间的最大间隔距离
    public static final double PREDICTION_INTERVAL = MAX_POWER_SPEED / 1000.0 / DETAILED_PHYSICAL;
    
    public static final double MAX_SPIN_DIFF = Math.max(MAX_SPIN_SPEED, MAX_POWER_SPEED);

    public static final Color WHITE = Color.SNOW;
    public static final Color RED = Color.RED;
    public static final Color YELLOW = Color.GOLD;
    public static final Color GREEN = Color.DARKGREEN;
    public static final Color BROWN = Color.CHOCOLATE;
    public static final Color BLUE = Color.CORNFLOWERBLUE;
    public static final Color PINK = Color.PALEVIOLETRED;
    public static final Color BLACK = Color.BLACK;
    public static final Color COLORED = Color.GRAY;

    // 十六球的颜色
    public static final Color PURPLE = Color.PURPLE;
    public static final Color ORANGE = Color.ORANGE;
    public static final Color DARK_RED = Color.DARKRED;

    public static final Color[] COLORED_LOW_TO_HIGH =
            {YELLOW, GREEN, BROWN, BLUE, PINK, BLACK};

    public static final Color BALL_CONTOUR = Color.BLACK;

    public static Color getColorOfTarget(int value) {
        if (value == 0) return COLORED;
        else return Ball.snookerColor(value);
    }
}
