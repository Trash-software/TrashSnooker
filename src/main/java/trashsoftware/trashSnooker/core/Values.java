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

    public static final double WALL_BOUNCE_RATIO = 0.92;
    public static final double BALL_BOUNCE_RATIO = 0.97;
    public static final double WALL_SPIN_PRESERVE_RATIO = 0.75;

    // 置球点
    public static final double[] YELLOW_POINT_XY = {BREAK_LINE_X, MID_Y + BREAK_ARC_RADIUS};
    public static final double[] GREEN_POINT_XY = {BREAK_LINE_X, MID_Y - BREAK_ARC_RADIUS};
    public static final double[] BROWN_POINT_XY = {BREAK_LINE_X, MID_Y};
    public static final double[] BLUE_POINT_XY = {MID_X, MID_Y};
    public static final double[] BLACK_POINT_XY = {RIGHT_X - 324.0, MID_Y};
    public static final double[] PINK_POINT_XY = {(BLACK_POINT_XY[0] + BLUE_POINT_XY[0]) / 2, MID_Y};

    public static final double[][] POINTS_RANK_HIGH_TO_LOW = {
            BLACK_POINT_XY,
            PINK_POINT_XY,
            BLUE_POINT_XY,
            BROWN_POINT_XY,
            GREEN_POINT_XY,
            YELLOW_POINT_XY
    };

    public static final double CORNER_HOLE_DT = CORNER_HOLE_RADIUS / Math.sqrt(2);
    public static final double CORNER_HOLE_TANGENT = CORNER_HOLE_RADIUS * Math.sqrt(2);
    public static final double CORNER_ARC_HEIGHT = CORNER_HOLE_TANGENT - CORNER_HOLE_RADIUS;
    public static final double CORNER_ARC_WIDTH = CORNER_ARC_HEIGHT * Math.tan(Math.toRadians(67.5));
    public static final double CORNER_ARC_RADIUS = CORNER_ARC_HEIGHT + CORNER_ARC_WIDTH;
    public static final double CORNER_ARC_DIAMETER = CORNER_ARC_RADIUS * 2;
    public static final double CORNER_LINE_WH = CORNER_HOLE_RADIUS;  // 底袋角直线的占地长宽

    public static final double[] TOP_LEFT_HOLE_XY =
            {LEFT_X - CORNER_HOLE_DT, TOP_Y - CORNER_HOLE_DT};
    public static final double[] BOT_LEFT_HOLE_XY =
            {LEFT_X - CORNER_HOLE_DT, BOT_Y + CORNER_HOLE_DT};
    public static final double[] TOP_RIGHT_HOLE_XY =
            {RIGHT_X + CORNER_HOLE_DT, TOP_Y - CORNER_HOLE_DT};
    public static final double[] BOT_RIGHT_HOLE_XY =
            {RIGHT_X + CORNER_HOLE_DT, BOT_Y + CORNER_HOLE_DT};
    public static final double[] TOP_MID_HOLE_XY =
            {MID_X, TOP_Y - CORNER_HOLE_RADIUS};
    public static final double[] BOT_MID_HOLE_XY =
            {MID_X, BOT_Y + CORNER_HOLE_RADIUS};

    public static final double LEFT_CORNER_HOLE_AREA_RIGHT_X = LEFT_X + CORNER_HOLE_RADIUS + CORNER_ARC_WIDTH;  // 左顶袋右袋角
    public static final double MID_HOLE_AREA_LEFT_X = MID_X - MID_HOLE_DIAMETER;  // 中袋左袋角
    public static final double MID_HOLE_AREA_RIGHT_X = MID_HOLE_AREA_LEFT_X + MID_HOLE_DIAMETER * 2;  // 中袋右袋角
    public static final double RIGHT_CORNER_HOLE_AREA_LEFT_X = RIGHT_X - CORNER_HOLE_RADIUS - CORNER_ARC_WIDTH;  // 右顶袋左袋角
    public static final double TOP_CORNER_HOLE_AREA_DOWN_Y = TOP_Y + CORNER_HOLE_RADIUS + CORNER_ARC_WIDTH;  // 上底袋下袋角
    public static final double BOT_CORNER_HOLE_AREA_UP_Y = BOT_Y - CORNER_HOLE_RADIUS - CORNER_ARC_WIDTH;  // 下底袋上袋角

    // 中袋袋角弧线
    public static final double[] TOP_MID_HOLE_LEFT_ARC_XY =
            {TOP_MID_HOLE_XY[0] - MID_HOLE_DIAMETER, TOP_MID_HOLE_XY[1]};
    public static final double[] TOP_MID_HOLE_RIGHT_ARC_XY =
            {TOP_MID_HOLE_XY[0] + MID_HOLE_DIAMETER, TOP_MID_HOLE_XY[1]};
    public static final double[] BOT_MID_HOLE_LEFT_ARC_XY =
            {BOT_MID_HOLE_XY[0] - MID_HOLE_DIAMETER, BOT_MID_HOLE_XY[1]};
    public static final double[] BOT_MID_HOLE_RIGHT_ARC_XY =
            {BOT_MID_HOLE_XY[0] + MID_HOLE_DIAMETER, BOT_MID_HOLE_XY[1]};

    // 底袋袋角弧线
    public static final double[] TOP_LEFT_HOLE_SIDE_ARC_XY =  // 左上底袋边库袋角
            {LEFT_X + CORNER_HOLE_RADIUS + CORNER_ARC_WIDTH, TOP_Y - CORNER_ARC_RADIUS};
    public static final double[] TOP_LEFT_HOLE_END_ARC_XY =  // 左上底袋底库袋角
            {LEFT_X - CORNER_ARC_RADIUS, TOP_Y + CORNER_HOLE_RADIUS + CORNER_ARC_WIDTH};
    public static final double[] BOT_LEFT_HOLE_SIDE_ARC_XY =
            {LEFT_X + CORNER_HOLE_RADIUS + CORNER_ARC_WIDTH, BOT_Y + CORNER_ARC_RADIUS};
    public static final double[] BOT_LEFT_HOLE_END_ARC_XY =
            {LEFT_X - CORNER_ARC_RADIUS, BOT_Y - CORNER_HOLE_RADIUS - CORNER_ARC_WIDTH};

    public static final double[] TOP_RIGHT_HOLE_SIDE_ARC_XY =
            {RIGHT_X - CORNER_HOLE_RADIUS - CORNER_ARC_WIDTH, TOP_Y - CORNER_ARC_RADIUS};
    public static final double[] TOP_RIGHT_HOLE_END_ARC_XY =
            {RIGHT_X + CORNER_ARC_RADIUS, TOP_Y + CORNER_HOLE_RADIUS + CORNER_ARC_WIDTH};
    public static final double[] BOT_RIGHT_HOLE_SIDE_ARC_XY =
            {RIGHT_X - CORNER_HOLE_RADIUS - CORNER_ARC_WIDTH, BOT_Y + CORNER_ARC_RADIUS};
    public static final double[] BOT_RIGHT_HOLE_END_ARC_XY =
            {RIGHT_X + CORNER_ARC_RADIUS, BOT_Y - CORNER_HOLE_RADIUS - CORNER_ARC_WIDTH};

    // 底袋袋角直线
    public static final double[][] TOP_LEFT_HOLE_SIDE_LINE =
            {{LEFT_X, TOP_Y - CORNER_HOLE_TANGENT}, {LEFT_X + CORNER_LINE_WH, TOP_Y - CORNER_ARC_HEIGHT}};
    public static final double[][] TOP_LEFT_HOLE_END_LINE =
            {{LEFT_X - CORNER_HOLE_TANGENT, TOP_Y}, {LEFT_X - CORNER_ARC_HEIGHT, TOP_Y + CORNER_LINE_WH}};
    public static final double[][] BOT_LEFT_HOLE_SIDE_LINE =
            {{LEFT_X, BOT_Y + CORNER_HOLE_TANGENT}, {LEFT_X + CORNER_LINE_WH, BOT_Y + CORNER_ARC_HEIGHT}};
    public static final double[][] BOT_LEFT_HOLE_END_LINE =
            {{LEFT_X - CORNER_HOLE_TANGENT, BOT_Y}, {LEFT_X - CORNER_ARC_HEIGHT, BOT_Y - CORNER_LINE_WH}};

    public static final double[][] TOP_RIGHT_HOLE_SIDE_LINE =
            {{RIGHT_X, TOP_Y - CORNER_HOLE_TANGENT}, {RIGHT_X - CORNER_LINE_WH, TOP_Y - CORNER_ARC_HEIGHT}};
    public static final double[][] TOP_RIGHT_HOLE_END_LINE =
            {{RIGHT_X + CORNER_HOLE_TANGENT, TOP_Y}, {RIGHT_X + CORNER_ARC_HEIGHT, TOP_Y + CORNER_LINE_WH}};
    public static final double[][] BOT_RIGHT_HOLE_SIDE_LINE =
            {{RIGHT_X, BOT_Y + CORNER_HOLE_TANGENT}, {RIGHT_X - CORNER_LINE_WH, BOT_Y + CORNER_ARC_HEIGHT}};
    public static final double[][] BOT_RIGHT_HOLE_END_LINE =
            {{RIGHT_X + CORNER_HOLE_TANGENT, BOT_Y}, {RIGHT_X + CORNER_ARC_HEIGHT, BOT_Y - CORNER_LINE_WH}};

    public static final double[][] ALL_CORNER_ARCS = {
            TOP_LEFT_HOLE_SIDE_ARC_XY,
            TOP_LEFT_HOLE_END_ARC_XY,
            TOP_RIGHT_HOLE_SIDE_ARC_XY,
            TOP_RIGHT_HOLE_END_ARC_XY,
            BOT_LEFT_HOLE_SIDE_ARC_XY,
            BOT_LEFT_HOLE_END_ARC_XY,
            BOT_RIGHT_HOLE_SIDE_ARC_XY,
            BOT_RIGHT_HOLE_END_ARC_XY
    };

    public static final double[][][] ALL_CORNER_LINES = {
            TOP_LEFT_HOLE_SIDE_LINE,  // "\"
            TOP_LEFT_HOLE_END_LINE,  // "\"
            BOT_RIGHT_HOLE_SIDE_LINE,  // "\"
            BOT_RIGHT_HOLE_END_LINE,  // "\"
            TOP_RIGHT_HOLE_SIDE_LINE,  // "/"
            TOP_RIGHT_HOLE_END_LINE,  // "/"
            BOT_LEFT_HOLE_SIDE_LINE,  // "/"
            BOT_LEFT_HOLE_END_LINE  // "/"
    };

    // 袋角直线的法向量
    public static final double[] NORMAL_45 = {1, 1};  // 左下至右上
    public static final double[] NORMAL_315 = {1, -1};  // 左上至右下

    // 击球最大速度，mm/s
    public static final double MAX_POWER_SPEED = 5000.0;
    // 由旋转产生的理论最大瞬时速度（该速度不可能达到），mm/s
    public static final double MAX_SPIN_SPEED = 6500.0;
    // 由侧旋产生的最大瞬时速度，mm/s
    public static final double MAX_SIDE_SPIN_SPEED = 1500.0;
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

    public static final Color[] COLORED_LOW_TO_HIGH =
            {YELLOW, GREEN, BROWN, BLUE, PINK, BLACK};

    public static final Color BALL_CONTOUR = Color.BLACK;

    public static Color getColorOfTarget(int value) {
        if (value == 0) return COLORED;
        else return Ball.generateColor(value);
    }
}
