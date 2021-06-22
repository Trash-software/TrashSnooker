package trashsoftware.trashSnooker.core;

import javafx.scene.paint.Color;

public class Values {
    public static final double SNOOKER_OUTER_WIDTH = 3820.0;
    public static final double SNOOKER_INNER_WIDTH = 3569.0;
    public static final double SNOOKER_OUTER_HEIGHT = 2035.0;
    public static final double SNOOKER_INNER_HEIGHT = 1788.0;
    public static final double LEFT_X = (SNOOKER_OUTER_WIDTH - SNOOKER_INNER_WIDTH) / 2;
    public static final double TOP_Y = (SNOOKER_OUTER_HEIGHT - SNOOKER_INNER_HEIGHT) / 2;
    public static final double RIGHT_X = LEFT_X + SNOOKER_INNER_WIDTH;
    public static final double BOT_Y = TOP_Y + SNOOKER_INNER_HEIGHT;
    public static final double MID_X = SNOOKER_OUTER_WIDTH / 2;
    public static final double MID_Y = SNOOKER_OUTER_HEIGHT / 2;
    public static final double BREAK_LINE_X = LEFT_X + 737.0;
    public static final double BREAK_ARC_RADIUS = 292.0;
    public static final double BALL_DIAMETER = 52.5;
    public static final double BALL_RADIUS = BALL_DIAMETER / 2;
    public static final double CORNER_HOLE_DIAMETER = 85.0;
    public static final double MID_HOLE_DIAMETER = 85.0;
    public static final double CORNER_HOLE_RADIUS = CORNER_HOLE_DIAMETER / 2;
    public static final double MID_HOLE_RADIUS = MID_HOLE_DIAMETER / 2;
    public static final double MID_ARC_RADIUS = MID_HOLE_RADIUS;

    public static final double MAX_LENGTH = Math.hypot(SNOOKER_OUTER_WIDTH, SNOOKER_OUTER_HEIGHT);  // 对角线长度

    public static final double WALL_BOUNCE_RATIO = 0.92;
    public static final double BALL_BOUNCE_RATIO = 0.97;
    public static final double WALL_SPIN_PRESERVE_RATIO = 0.75;

    public static final double CORNER_HOLE_DT = CORNER_HOLE_RADIUS / Math.sqrt(2);
    public static final double CORNER_HOLE_TANGENT = CORNER_HOLE_RADIUS * Math.sqrt(2);
    public static final double CORNER_ARC_HEIGHT = CORNER_HOLE_TANGENT - CORNER_HOLE_RADIUS;
    public static final double CORNER_ARC_WIDTH = CORNER_ARC_HEIGHT * Math.tan(Math.toRadians(67.5));
    public static final double CORNER_ARC_RADIUS = CORNER_ARC_HEIGHT + CORNER_ARC_WIDTH;
    public static final double CORNER_ARC_DIAMETER = CORNER_ARC_RADIUS * 2;
    public static final double CORNER_LINE_WH = CORNER_HOLE_RADIUS;  // 底袋角直线的占地长宽

    // 袋角直线的法向量
    public static final double[] NORMAL_45 = {1, 1};  // 左下至右上
    public static final double[] NORMAL_315 = {1, -1};  // 左上至右下

    // 击球最大速度，mm/s
    public static final double MAX_POWER_SPEED = 5000.0;
    // 由旋转产生的理论最大瞬时速度（该速度不可能达到），mm/s
    public static final double MAX_SPIN_SPEED = 7000.0;
    // 由侧旋产生的最大瞬时速度，mm/s
    public static final double MAX_SIDE_SPIN_SPEED = 1400.0;
    public static final int DETAILED_PHYSICAL = 10;
    // 每两次物理碰撞运算之间的最大间隔距离
    public static final double PREDICTION_INTERVAL = MAX_POWER_SPEED / 1000.0 / DETAILED_PHYSICAL;

    public static final Color WHITE = Color.SNOW;
    public static final Color RED = Color.RED;
    public static final Color YELLOW = Color.GOLD;
    public static final Color GREEN = Color.DARKGREEN;
    public static final Color BROWN = Color.CHOCOLATE;
    public static final Color BLUE = Color.STEELBLUE;
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
