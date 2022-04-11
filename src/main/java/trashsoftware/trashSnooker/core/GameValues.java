package trashsoftware.trashSnooker.core;

import javafx.scene.paint.Color;

public class GameValues {

    public static final GameValues SNOOKER_VALUES = new Builder()
            .tableColor(Color.GREEN, Color.SADDLEBROWN)
            .tableDimension(3820.0, 3569.0,
                    2035.0, 1788.0,
                    33.34)
            .ballSize(52.5)
            .holeSizeCurved(85.0, 90.0)
            .resistanceAndCushionBounce(1.0, 0.9, 0.75)
            .ballBounce(0.97)
            .build();

    public static final GameValues MINI_SNOOKER_VALUES = new Builder()
            .tableColor(Color.GREEN, Color.SADDLEBROWN)
            .tableDimension(2830.0, 2540.0,
                    1550.0, 1270.0,
                    42.0)
            .ballSize(52.5)
            .holeSizeCurved(85.0, 90.0)
            .resistanceAndCushionBounce(1.05, 0.85, 0.7)  // 裤边太高影响反弹
            .ballBounce(0.97)
            .build();

    public static final GameValues CHINESE_EIGHT_VALUES = new Builder()
            .tableColor(Color.GREEN, Color.SADDLEBROWN)
            .tableDimension(2830.0, 2540.0,
                    1550.0, 1270.0,
                    42.0)
            .ballSize(57.15)
            .holeSizeCurved(85.0, 90.0)
            .resistanceAndCushionBounce(1.05, 0.9, 0.75)
            .ballBounce(0.96)
            .build();

    public static final GameValues SIDE_POCKET = new Builder()
            .tableColor(Color.STEELBLUE, Color.BLACK)
            .tableDimension(2905.0, 2540.0,
                    1635.0, 1270.0,
                    42.0)
            .ballSize(57.15)
            .holeSizeStraight(105.0, 105.0,
                    5.0, 20.0)
            .resistanceAndCushionBounce(1.0, 0.8, 0.9)
            .ballBounce(0.96)
            .build();

    public Color tableColor;
    public Color tableBorderColor;

    public double outerWidth;
    public double outerHeight;
    public double innerWidth;
    public double innerHeight;
    public double leftX, rightX, topY, botY, midX, midY;
    public double maxLength;  // 对角线长度
    public double cushionHeight;
    public double speedReduceMultiplier = 1.0;  // 台泥的阻力系数，值越大阻力越大

    public double ballDiameter;
    public double ballRadius;
    public double cornerHoleDiameter, cornerHoleRadius;
    public double midHoleDiameter, midHoleRadius;

    public double midArcRadius;

    public double ballWeightRatio;

    public double cornerHoleDt, cornerHoleTan, cornerArcHeight, cornerArcWidth, cornerArcRadius, cornerArcDiameter,
            cornerLineLonger, cornerLineShorter,  // 底袋角直线的占地长宽
            midLineWidth, midLineHeight;  // 中袋角直线占地长宽

    public double[] topLeftHoleXY;
    public double[] botLeftHoleXY;
    public double[] topRightHoleXY;
    public double[] botRightHoleXY;
    public double[] topMidHoleXY;
    public double[] botMidHoleXY;

    // 袋的正中央，最易进球的位置
    public double[] topLeftHoleOpenCenter;
    public double[] botLeftHoleOpenCenter;
    public double[] topRightHoleOpenCenter;
    public double[] botRightHoleOpenCenter;
    public double[] topMidHoleOpenCenter;
    public double[] botMidHoleOpenCenter;

    public double[][] allHoles;
    public double[][] allHoleOpenCenters;

    public double leftCornerHoleAreaRightX;  // 左顶袋右袋角
    public double midHoleAreaLeftX;  // 中袋左袋角
    public double midHoleAreaRightX;  // 中袋右袋角
    public double rightCornerHoleAreaLeftX;  // 右顶袋左袋角
    public double topCornerHoleAreaDownY;  // 上底袋下袋角
    public double botCornerHoleAreaUpY;  // 下底袋上袋角

    public double midHoleLineLeftX, midHoleLineRightX;  // 中袋袋角直线左右极限（仅直袋口）

    // 中袋袋角弧线
    public double[] topMidHoleLeftArcXy;
    public double[] topMidHoleRightArcXy;
    public double[] botMidHoleLeftArcXy;
    public double[] botMidHoleRightArcXy;

    // 中袋袋角直线
    public double[][] topMidHoleLeftLine;
    public double[][] topMidHoleRightLine;
    public double[][] botMidHoleLeftLine;
    public double[][] botMidHoleRightLine;

    // 底袋袋角弧线
    public double[] topLeftHoleSideArcXy;  // 左上底袋边库袋角
    public double[] topLeftHoleEndArcXy;  // 左上底袋底库袋角
    public double[] botLeftHoleSideArcXy;
    public double[] botLeftHoleEndArcXy;

    public double[] topRightHoleSideArcXy;
    public double[] topRightHoleEndArcXy;
    public double[] botRightHoleSideArcXy;
    public double[] botRightHoleEndArcXy;

    // 底袋袋角直线
    public double[][] topLeftHoleSideLine;
    public double[][] topLeftHoleEndLine;
    public double[][] botLeftHoleSideLine;
    public double[][] botLeftHoleEndLine;

    public double[][] topRightHoleSideLine;
    public double[][] topRightHoleEndLine;
    public double[][] botRightHoleSideLine;
    public double[][] botRightHoleEndLine;

    public double[][] allCornerArcs;

    public double[][][] allCornerLines;
    public double[][][] allMidHoleLines;

    private boolean straightHole;
    
    public double tableResistanceRatio;
    public double ballBounceRatio;
    public double wallBounceRatio;
    public double wallSpinPreserveRatio;

    public boolean isStraightHole() {
        return straightHole;
    }

    private void build() {
        topLeftHoleXY = new double[]
                {leftX - cornerHoleDt, topY - cornerHoleDt};
        botLeftHoleXY = new double[]
                {leftX - cornerHoleDt, botY + cornerHoleDt};
        topRightHoleXY = new double[]
                {rightX + cornerHoleDt, topY - cornerHoleDt};
        botRightHoleXY = new double[]
                {rightX + cornerHoleDt, botY + cornerHoleDt};
        topMidHoleXY = new double[]
                {midX, topY - midHoleRadius};
        botMidHoleXY = new double[]
                {midX, botY + midHoleRadius};

        allHoles = new double[][]{
                topLeftHoleXY,
                botLeftHoleXY,
                topRightHoleXY,
                botRightHoleXY,
                topMidHoleXY,
                botMidHoleXY
        };

        topLeftHoleOpenCenter = new double[]{leftX + ballRadius, topY + ballRadius};
        botLeftHoleOpenCenter = new double[]{leftX + ballRadius, botY - ballRadius};
        topRightHoleOpenCenter = new double[]{rightX - ballRadius, topY + ballRadius};
        botRightHoleOpenCenter = new double[]{rightX - ballRadius, botY - ballRadius};
        topMidHoleOpenCenter = new double[]{midX, topY - ballRadius * 0.75};  // 特殊：中心点其实在台外
        botMidHoleOpenCenter = new double[]{midX, botY + ballRadius * 0.75};

        allHoleOpenCenters = new double[][]{
                topLeftHoleOpenCenter,
                botLeftHoleOpenCenter,
                topRightHoleOpenCenter,
                botRightHoleOpenCenter,
                topMidHoleOpenCenter,
                botMidHoleOpenCenter
        };

        if (straightHole) {
            leftCornerHoleAreaRightX = leftX + cornerLineLonger;
            rightCornerHoleAreaLeftX = rightX - cornerLineLonger;
            midHoleAreaLeftX = midX - midHoleRadius - midLineWidth;
            midHoleAreaRightX = midX + midHoleRadius + midLineWidth;
            topCornerHoleAreaDownY = topY + cornerLineLonger;
            botCornerHoleAreaUpY = botY - cornerLineLonger;
        } else {
            leftCornerHoleAreaRightX = leftX + cornerHoleRadius + cornerArcWidth;  // 左顶袋右袋角
            midHoleAreaLeftX = midX - midHoleDiameter;  // 中袋左袋角
            midHoleAreaRightX = midHoleAreaLeftX + midHoleDiameter * 2;  // 中袋右袋角
            rightCornerHoleAreaLeftX = rightX - cornerHoleRadius - cornerArcWidth;  // 右顶袋左袋角
            topCornerHoleAreaDownY = topY + cornerHoleRadius + cornerArcWidth;  // 上底袋下袋角
            botCornerHoleAreaUpY = botY - cornerHoleRadius - cornerArcWidth;  // 下底袋上袋角
        }

        // 中袋袋角弧线
        topMidHoleLeftArcXy =
                new double[]{topMidHoleXY[0] - midHoleDiameter, topMidHoleXY[1]};
        topMidHoleRightArcXy =
                new double[]{topMidHoleXY[0] + midHoleDiameter, topMidHoleXY[1]};
        botMidHoleLeftArcXy =
                new double[]{botMidHoleXY[0] - midHoleDiameter, botMidHoleXY[1]};
        botMidHoleRightArcXy =
                new double[]{botMidHoleXY[0] + midHoleDiameter, botMidHoleXY[1]};

        // 底袋袋角弧线
        topLeftHoleSideArcXy =  // 左上底袋边库袋角
                new double[]{leftX + cornerHoleRadius + cornerArcWidth, topY - cornerArcRadius};
        topLeftHoleEndArcXy =  // 左上底袋底库袋角
                new double[]{leftX - cornerArcRadius, topY + cornerHoleRadius + cornerArcWidth};
        botLeftHoleSideArcXy =
                new double[]{leftX + cornerHoleRadius + cornerArcWidth, botY + cornerArcRadius};
        botLeftHoleEndArcXy =
                new double[]{leftX - cornerArcRadius, botY - cornerHoleRadius - cornerArcWidth};

        topRightHoleSideArcXy =
                new double[]{rightX - cornerHoleRadius - cornerArcWidth, topY - cornerArcRadius};
        topRightHoleEndArcXy =
                new double[]{rightX + cornerArcRadius, topY + cornerHoleRadius + cornerArcWidth};
        botRightHoleSideArcXy =
                new double[]{rightX - cornerHoleRadius - cornerArcWidth, botY + cornerArcRadius};
        botRightHoleEndArcXy =
                new double[]{rightX + cornerArcRadius, botY - cornerHoleRadius - cornerArcWidth};

        // 中袋袋角直线
        midHoleLineLeftX = midX - midHoleRadius - midLineWidth;
        midHoleLineRightX = midX + midHoleRadius + midLineWidth;
        topMidHoleLeftLine =
                new double[][]{{midX - midHoleRadius, topY - midLineHeight},
                        {midHoleLineLeftX, topY}};
        topMidHoleRightLine =
                new double[][]{{midX + midHoleRadius, topY - midLineHeight},
                        {midHoleLineRightX, topY}};
        botMidHoleLeftLine =
                new double[][]{{midX - midHoleRadius, botY + midLineHeight},
                        {midHoleLineLeftX, botY}};
        botMidHoleRightLine =
                new double[][]{{midX + midHoleRadius, botY + midLineHeight},
                        {midHoleLineRightX, botY}};

        // 底袋袋角直线
        topLeftHoleSideLine =
                new double[][]{{leftX, topY - cornerHoleTan}, {leftX + cornerLineLonger, topY - cornerArcHeight}};
        topLeftHoleEndLine =
                new double[][]{{leftX - cornerHoleTan, topY}, {leftX - cornerArcHeight, topY + cornerLineLonger}};
        botLeftHoleSideLine =
                new double[][]{{leftX, botY + cornerHoleTan}, {leftX + cornerLineLonger, botY + cornerArcHeight}};
        botLeftHoleEndLine =
                new double[][]{{leftX - cornerHoleTan, botY}, {leftX - cornerArcHeight, botY - cornerLineLonger}};

        topRightHoleSideLine =
                new double[][]{{rightX, topY - cornerHoleTan}, {rightX - cornerLineLonger, topY - cornerArcHeight}};
        topRightHoleEndLine =
                new double[][]{{rightX + cornerHoleTan, topY}, {rightX + cornerArcHeight, topY + cornerLineLonger}};
        botRightHoleSideLine =
                new double[][]{{rightX, botY + cornerHoleTan}, {rightX - cornerLineLonger, botY + cornerArcHeight}};
        botRightHoleEndLine =
                new double[][]{{rightX + cornerHoleTan, botY}, {rightX + cornerArcHeight, botY - cornerLineLonger}};

        allCornerArcs = new double[][]{
                topLeftHoleSideArcXy,
                topLeftHoleEndArcXy,
                topRightHoleSideArcXy,
                topRightHoleEndArcXy,
                botLeftHoleSideArcXy,
                botLeftHoleEndArcXy,
                botRightHoleSideArcXy,
                botRightHoleEndArcXy
        };

        allCornerLines = new double[][][]{
                topLeftHoleSideLine,  // "\"
                topLeftHoleEndLine,  // "\"
                botRightHoleSideLine,  // "\"
                botRightHoleEndLine,  // "\"
                topRightHoleSideLine,  // "/"
                topRightHoleEndLine,  // "/"
                botLeftHoleSideLine,  // "/"
                botLeftHoleEndLine  // "/"
        };

        allMidHoleLines = new double[][][]{
                topMidHoleLeftLine,
                topMidHoleRightLine,
                botMidHoleLeftLine,
                botMidHoleRightLine
        };
    }

    public double diagonalLength() {
        return Math.hypot(innerWidth, innerHeight);
    }

    public double speedReducerPerInterval() {
        return (Game.speedReducer * speedReduceMultiplier / ballWeightRatio);  // 重的球减速慢
    }

    /**
     * @param speed 初始速度，mm/s
     * @return 预估的直线移动距离，mm。
     */
    public double estimatedMoveDistance(double speed) {
        double acceleration = speedReducerPerInterval() * Game.calculationsPerSecSqr;
        double t = speed / acceleration;  // 加速时间，秒
        return acceleration / 2 * t * t;  // S = 1/2at^2
    }

    public static class Builder {
        private final GameValues values = new GameValues();

        public Builder tableColor(Color color, Color borderColor) {
            values.tableColor = color;
            values.tableBorderColor = borderColor;
            return this;
        }

        public Builder tableDimension(double outerWidth, double innerWidth,
                                      double outerHeight, double innerHeight,
                                      double cushionHeight) {
            values.outerWidth = outerWidth;
            values.innerWidth = innerWidth;
            values.outerHeight = outerHeight;
            values.innerHeight = innerHeight;
            values.cushionHeight = cushionHeight;
            values.leftX = (outerWidth - innerWidth) / 2;
            values.rightX = innerWidth + values.leftX;
            values.topY = (outerHeight - innerHeight) / 2;
            values.botY = innerHeight + values.topY;
            values.midX = outerWidth / 2;
            values.midY = outerHeight / 2;
            values.maxLength = Math.hypot(outerHeight, outerWidth);
            return this;
        }

        public Builder ballSize(double diameter) {
            values.ballDiameter = diameter;
            values.ballRadius = diameter / 2;
            values.ballWeightRatio = Math.pow(diameter, 3) / Math.pow(52.5, 3);
            return this;
        }

        public Builder holeSizeStraight(double cornerHoleDiameter, double midHoleDiameter,
                                        double cornerHoleOpenAngle, double midHoleOpenAngle) {
            values.straightHole = true;
            values.cornerHoleDiameter = cornerHoleDiameter;
            values.midHoleDiameter = midHoleDiameter;
            values.cornerHoleRadius = cornerHoleDiameter / 2;
            values.midHoleRadius = midHoleDiameter / 2;

            values.cornerHoleDt = values.cornerHoleRadius / Math.sqrt(2);
            values.cornerHoleTan = values.cornerHoleRadius * Math.sqrt(2);

            values.cornerLineShorter = values.cornerHoleTan;  // 底袋角直线的占地长宽
            values.cornerLineLonger = Math.tan(Math.toRadians(45.0 + cornerHoleOpenAngle)) * values.cornerLineShorter;

            double midLineTan = Math.tan(Math.toRadians(midHoleOpenAngle));
            values.midLineHeight = values.midHoleRadius;
            values.midLineWidth = values.midLineHeight * midLineTan;
            return this;
        }

        /**
         * @param cornerHoleDiameter 底袋洞口直径
         * @param midHoleDiameter    中袋洞口直径
         * @return Builder
         */
        public Builder holeSizeCurved(double cornerHoleDiameter, double midHoleDiameter) {
            values.straightHole = false;
            values.cornerHoleDiameter = cornerHoleDiameter;
            values.midHoleDiameter = midHoleDiameter;
            values.cornerHoleRadius = cornerHoleDiameter / 2;
            values.midHoleRadius = midHoleDiameter / 2;

            values.midArcRadius = values.midHoleRadius;

            values.cornerHoleDt = values.cornerHoleRadius / Math.sqrt(2);
            values.cornerHoleTan = values.cornerHoleRadius * Math.sqrt(2);

            values.cornerArcHeight = values.cornerHoleTan - values.cornerHoleRadius;
            values.cornerArcWidth = values.cornerArcHeight * Math.tan(Math.toRadians(67.5));
            values.cornerArcRadius = values.cornerArcWidth + values.cornerArcHeight;
            values.cornerArcDiameter = values.cornerArcRadius * 2;
            values.cornerLineLonger = values.cornerHoleRadius;  // 底袋角直线的占地长宽
            values.cornerLineShorter = values.cornerHoleRadius;
            return this;
        }
        
        public Builder resistanceAndCushionBounce(double tableResistance, 
                                                  double wallBounce,
                                                  double wallSpinPreserve) {
            values.tableResistanceRatio = tableResistance;
            values.wallBounceRatio = wallBounce;
            values.wallSpinPreserveRatio = wallSpinPreserve;
            return this;
        }
        
        public Builder ballBounce(double ballBounceRatio) {
            values.ballBounceRatio = ballBounceRatio;
            return this;
        }

        public GameValues build() {
            values.build();
            return values;
        }
    }
}
