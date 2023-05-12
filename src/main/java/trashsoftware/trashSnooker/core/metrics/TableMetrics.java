package trashsoftware.trashSnooker.core.metrics;

import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.fxml.App;

public class TableMetrics {

    public static final String SNOOKER = "SNOOKER";
    public static final String CHINESE_EIGHT = "CHINESE_EIGHT";
    public static final String SIDE_POCKET = "SIDE_POCKET";

    private static final String[] NAMES = {SNOOKER, CHINESE_EIGHT, SIDE_POCKET};
    public final String tableName;
    public final TableBuilderFactory factory;
//    private HoleSize holeSize;

    public Color tableColor;
    public Color gravityAreaColor;
    public Color tableBorderColor;

    public double outerWidth;
    public double outerHeight;
    public double innerWidth;
    public double innerHeight;
    public double leftX, rightX, topY, botY, midX, midY;
    public double leftClothX, rightClothX, topClothY, botClothY;  // 绿色部分的最大
    public double maxLength;  // 对角线长度
    public double cushionHeight;
    public double speedReduceMultiplier = 1.0;  // 台泥的阻力系数，值越大阻力越大

    // 角袋入口处的宽度，因为袋的边可能有角度
    public double cornerHoleDiameter, cornerHoleRadius;
    public double midHoleDiameter, midHoleRadius;
    public double cornerPocketGravityRadius;  // 袋口处的坡宽度。球进入这个区域后会开始有往袋里掉的意思
    public double midPocketGravityRadius;

    public double midArcRadius;

    public double cornerHoleDt, cornerHoleTan, cornerArcHeight, cornerArcWidth, cornerArcRadius, cornerArcDiameter,
            cornerLineLonger, cornerLineShorter,  // 底袋角直线的占地长宽
            midLineWidth, midLineHeight;  // 中袋角直线占地长宽

    public double cornetHoleGraphicalDt;

    public double cornerHoleDrift;  // 对于有角度的袋，这个值是袋角伸进洞里多远
    public double arcBounceAngleRate;  // 袋角弧线的反射角系数，[0-1]之间，越接近1，越平坦

    public double[] topLeftHoleXY;
    public double[] botLeftHoleXY;
    public double[] topRightHoleXY;
    public double[] botRightHoleXY;
    public double[] topMidHoleXY;
    public double[] botMidHoleXY;

    public double[] topLeftHoleGraXY;
    public double[] botLeftHoleGraXY;
    public double[] topRightHoleGraXY;
    public double[] botRightHoleGraXY;

    public double[][] allHoles;

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
    public double[][] allMidArcs;

    public double[][][] allCornerLines;
    public double[][][] allMidHoleLines;
    //    public double ballHoleRatio;
//    public double cornerHoleAngleRatio;  // 打底袋最差的角度和最好的角度差多少
//    public double midHoleBestAngleWidth;  // 中袋对正的容错空间
    public double tableResistanceRatio;
    //    public double ballBounceRatio;
    public double wallBounceRatio;
    public double wallSpinPreserveRatio;
    public double cornerHoleOpenAngle, midHoleOpenAngle;
    private boolean straightHole;

    private TableMetrics(TableBuilderFactory factory, String tableName) {
        this.tableName = tableName;
        this.factory = factory;
    }

    public static TableMetrics.TableBuilderFactory fromOrdinal(int ordinal) {
        return TableBuilderFactory.values()[ordinal];
    }

    public int getOrdinal() {
        return factory.ordinal();
    }

    public int getHoleSizeOrdinal() {
        for (int i = 0; i < factory.supportedHoles.length; i++) {
            PocketSize size = factory.supportedHoles[i];
            if (size.cornerHoleDiameter == cornerHoleDiameter && size.midHoleDiameter == midHoleDiameter) {
                return i;
            }
        }
        throw new RuntimeException("No matching holes");
    }

    public int getPocketDifficultyOrdinal() {
        for (int i = 0; i < factory.supportedDifficulties.length; i++) {
            PocketDifficulty difficulty = factory.supportedDifficulties[i];
            if (difficulty.midPocketAngle == midHoleOpenAngle
                    && difficulty.midPocketGravityZone == midPocketGravityRadius
                    && difficulty.cornerPocketAngle == cornerHoleOpenAngle
                    && difficulty.cornerPocketGravityZone == cornerPocketGravityRadius
                    && difficulty.cornerPocketArcSize * cornerHoleRadius == cornerArcRadius) {
                return i;
            }
        }
        throw new RuntimeException("No matching holes");
    }

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

        // 仅为了让袋口看起来好看一点用
        topLeftHoleGraXY = new double[]
                {leftX - cornetHoleGraphicalDt, topY - cornetHoleGraphicalDt};
        botLeftHoleGraXY = new double[]
                {leftX - cornetHoleGraphicalDt, botY + cornetHoleGraphicalDt};
        topRightHoleGraXY = new double[]
                {rightX + cornetHoleGraphicalDt, topY - cornetHoleGraphicalDt};
        botRightHoleGraXY = new double[]
                {rightX + cornetHoleGraphicalDt, botY + cornetHoleGraphicalDt};

        leftClothX = leftX - cornerHoleTan;
        rightClothX = rightX + cornerHoleTan;
        topClothY = topY - cornerHoleTan;
        botClothY = botY + cornerHoleTan;

        allHoles = new double[][]{
                topLeftHoleXY,
                botLeftHoleXY,
                topRightHoleXY,
                botRightHoleXY,
                topMidHoleXY,
                botMidHoleXY
        };

        if (straightHole) {
            leftCornerHoleAreaRightX = leftX + cornerLineLonger;
            rightCornerHoleAreaLeftX = rightX - cornerLineLonger;
            midHoleAreaLeftX = midX - midHoleRadius - midLineWidth;
            midHoleAreaRightX = midX + midHoleRadius + midLineWidth;
            topCornerHoleAreaDownY = topY + cornerLineLonger;
            botCornerHoleAreaUpY = botY - cornerLineLonger;
        } else {
            leftCornerHoleAreaRightX = leftX + cornerArcWidth + cornerLineLonger - cornerHoleDrift;  // 左顶袋右袋角
            rightCornerHoleAreaLeftX = rightX - cornerArcWidth - cornerLineLonger + cornerHoleDrift;  // 右顶袋左袋角
            midHoleAreaLeftX = midX - midHoleDiameter - midLineWidth;  // 中袋左袋角
            midHoleAreaRightX = midHoleAreaLeftX + midHoleDiameter * 2 + midLineWidth;  // 中袋右袋角
            topCornerHoleAreaDownY = topY + cornerArcWidth + cornerLineLonger - cornerHoleDrift;  // 上底袋下袋角
            botCornerHoleAreaUpY = botY - cornerArcWidth - cornerLineLonger + cornerHoleDrift;  // 下底袋上袋角
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
                new double[]{leftX + cornerArcWidth + cornerLineLonger - cornerHoleDrift, topY - cornerArcRadius};
        topLeftHoleEndArcXy =  // 左上底袋底库袋角
                new double[]{leftX - cornerArcRadius, topY + cornerArcWidth + cornerLineLonger - cornerHoleDrift};
        botLeftHoleSideArcXy =
                new double[]{leftX + cornerArcWidth + cornerLineLonger - cornerHoleDrift, botY + cornerArcRadius};
        botLeftHoleEndArcXy =
                new double[]{leftX - cornerArcRadius, botY - cornerArcWidth - cornerLineLonger + cornerHoleDrift};

        topRightHoleSideArcXy =
                new double[]{rightX - cornerArcWidth - cornerLineLonger + cornerHoleDrift, topY - cornerArcRadius};
        topRightHoleEndArcXy =
                new double[]{rightX + cornerArcRadius, topY + cornerArcWidth + cornerLineLonger - cornerHoleDrift};
        botRightHoleSideArcXy =
                new double[]{rightX - cornerArcWidth - cornerLineLonger + cornerHoleDrift, botY + cornerArcRadius};
        botRightHoleEndArcXy =
                new double[]{rightX + cornerArcRadius, botY - cornerArcWidth - cornerLineLonger + cornerHoleDrift};

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
                new double[][]{{leftX - cornerHoleDrift, topY - cornerHoleTan},
                        {leftX + cornerLineLonger - cornerHoleDrift, topY - cornerArcHeight}};
        topLeftHoleEndLine =
                new double[][]{{leftX - cornerHoleTan, topY - cornerHoleDrift},
                        {leftX - cornerArcHeight, topY + cornerLineLonger - cornerHoleDrift}};
        botLeftHoleSideLine =
                new double[][]{{leftX - cornerHoleDrift, botY + cornerHoleTan},
                        {leftX + cornerLineLonger - cornerHoleDrift, botY + cornerArcHeight}};
        botLeftHoleEndLine =
                new double[][]{{leftX - cornerHoleTan, botY + cornerHoleDrift},
                        {leftX - cornerArcHeight, botY - cornerLineLonger + cornerHoleDrift}};

        topRightHoleSideLine =
                new double[][]{{rightX + cornerHoleDrift, topY - cornerHoleTan},
                        {rightX - cornerLineLonger + cornerHoleDrift, topY - cornerArcHeight}};
        topRightHoleEndLine =
                new double[][]{{rightX + cornerHoleTan, topY - cornerHoleDrift},
                        {rightX + cornerArcHeight, topY + cornerLineLonger - cornerHoleDrift}};
        botRightHoleSideLine =
                new double[][]{{rightX + cornerHoleDrift, botY + cornerHoleTan},
                        {rightX - cornerLineLonger + cornerHoleDrift, botY + cornerArcHeight}};
        botRightHoleEndLine =
                new double[][]{{rightX + cornerHoleTan, botY + cornerHoleDrift},
                        {rightX + cornerArcHeight, botY - cornerLineLonger + cornerHoleDrift}};

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
        allMidArcs = new double[][]{
                topMidHoleLeftArcXy,
                topMidHoleRightArcXy,
                botMidHoleLeftArcXy,
                botMidHoleRightArcXy
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

    public double dtToClosetCushion(double x, double y) {
        double closedLongCushion = Math.min(x - leftX, rightX - x);
        double closedShortCushion = Math.min(y - topY, botY - y);
        return Math.min(closedLongCushion, closedShortCushion);
    }

    public double closeCushionPenaltyThreshold() {
        return innerHeight / 8;
    }

    public boolean isInOuterTable(double x, double y) {
        return x >= 0 && x < outerWidth && y >= 0 && y < outerHeight;
    }

    public enum TableBuilderFactory {
        SNOOKER("snookerTable",
                PocketSize.SNOOKER_HOLES,
                PocketDifficulty.GREEN_TABLE_DIFFICULTIES) {
            @Override
            public Builder create() {
                return new Builder(this, TableMetrics.SNOOKER)
                        .tableColor(Color.GREEN, Color.SADDLEBROWN)
                        .tableDimension(3820.0, 3568.7,
                                2035.0, 1788.0,
                                33.34)
                        .curvedHole()
//                        .supportedHoles(SNOOKER_HOLES)
                        .resistanceAndCushionBounce(1.0, 0.92, 0.8);
            }
        },
        CHINESE_EIGHT("chineseEightTable",
                PocketSize.CHINESE_EIGHT_HOLES,
                PocketDifficulty.GREEN_TABLE_DIFFICULTIES) {
            @Override
            public Builder create() {
                return new Builder(this, TableMetrics.CHINESE_EIGHT)
                        .tableColor(Color.GREEN, Color.SADDLEBROWN)
                        .tableDimension(2830.0, 2540.0,
                                1550.0, 1270.0,
                                42.0)
//                        .supportedHoles(CHINESE_EIGHT_HOLES)
                        .curvedHole()
                        .resistanceAndCushionBounce(1.05, 0.92, 0.8);
            }
        },
        SIDE_POCKET("sidePocketTable",
                PocketSize.SIDE_POCKET_HOLES,
                PocketDifficulty.BLUE_TABLE_DIFFICULTIES) {
            @Override
            public Builder create() {
                return new Builder(this, TableMetrics.SIDE_POCKET)
                        .tableColor(Color.STEELBLUE, Color.BLACK)
                        .tableDimension(2905.0, 2540.0,
                                1635.0, 1270.0,
                                42.0)
                        .straightHole()
//                        .supportedHoles(SIDE_POCKET_HOLES)
                        .resistanceAndCushionBounce(1.0, 0.85, 0.9);
            }
        };

        public final String key;
        public final PocketSize[] supportedHoles;
        public final PocketDifficulty[] supportedDifficulties;

        TableBuilderFactory(String name, PocketSize[] supportedHoles, PocketDifficulty[] supportedDifficulties) {
            this.key = name;
            this.supportedHoles = supportedHoles;
            this.supportedDifficulties = supportedDifficulties;
        }

        public PocketSize defaultHole() {
            return supportedHoles[supportedHoles.length / 2];
        }

        public PocketDifficulty defaultDifficulty() {
            return supportedDifficulties[supportedDifficulties.length / 2];
        }

        @Override
        public String toString() {
            return App.getStrings().getString(key);
        }

        public abstract Builder create();
    }

    public enum Hole {
        TOP_LEFT, TOP_MID, TOP_RIGHT,
        BOT_LEFT, BOT_MID, BOT_RIGHT
    }

    public static class Builder {
        private final TableMetrics values;
        //        private String name = "Table";
        private double cornerHoleArcSizeMul;
        private double midHoleArcSizeMul;
        private double cornerPocketOut;

        public Builder(TableBuilderFactory factory, String tableName) {
            this.values = new TableMetrics(factory, tableName);
        }

        Builder tableColor(Color color, Color borderColor) {
            values.tableColor = color;
            values.gravityAreaColor = color.deriveColor(0, 1, 0.9, 1);
            values.tableBorderColor = borderColor;
            return this;
        }

        Builder tableDimension(double outerWidth, double innerWidth,
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

        Builder curvedHole() {
            values.straightHole = false;
            return this;
        }

        public Builder pocketDifficulty(PocketDifficulty pocketDifficulty) {
            return pocketDifficulty(
                    pocketDifficulty.cornerPocketGravityZone,
                    pocketDifficulty.cornerPocketArcSize,
                    pocketDifficulty.cornerPocketAngle,
                    pocketDifficulty.cornetPocketOut,
                    pocketDifficulty.midPocketGravityZone,
                    pocketDifficulty.midPocketArcSize,
                    pocketDifficulty.midPocketAngle
            );
        }

        Builder pocketDifficulty(double cornerPocketGravityZone,
                                 double cornerPocketArcSize,
                                 double cornerPocketAngle,
                                 double cornerPocketOut,
                                 double midPocketGravityZone,
                                 double midPocketArcSize,
                                 double midPocketAngle) {
            values.cornerPocketGravityRadius = cornerPocketGravityZone;
            values.cornerHoleOpenAngle = cornerPocketAngle;
            values.midPocketGravityRadius = midPocketGravityZone;
            values.midHoleOpenAngle = midPocketAngle;
            if (!values.straightHole) {
                this.cornerHoleArcSizeMul = cornerPocketArcSize;
                this.midHoleArcSizeMul = midPocketArcSize;
                this.cornerPocketOut = cornerPocketOut;
            }
            return this;
        }

        /**
         * @param cornerHoleDiameter 底袋洞口直径
         * @param midHoleDiameter    中袋洞口直径
         * @return Builder
         */
        private Builder holeSizeCurved(double cornerHoleDiameter, double midHoleDiameter) {
            if (cornerHoleArcSizeMul == 0) {
                throw new RuntimeException("Method 'pocketDifficulty' must be called prior to this method");
            }

            values.straightHole = false;

            double cornerHoleRadius = cornerHoleDiameter / 2;

            values.cornerHoleDiameter = cornerHoleDiameter;
            values.cornerHoleRadius = cornerHoleRadius;

            values.midHoleDiameter = midHoleDiameter;
            values.midHoleRadius = midHoleDiameter / 2;

            double holeDtOrig = cornerHoleRadius / Math.sqrt(2);
            values.cornetHoleGraphicalDt = holeDtOrig;
            values.cornerHoleDt = holeDtOrig * (1 - cornerPocketOut);
            values.cornerHoleTan = cornerHoleRadius * Math.sqrt(2);

            values.midArcRadius = values.midHoleRadius;
            values.cornerArcRadius = cornerHoleRadius * cornerHoleArcSizeMul;
            values.cornerArcDiameter = values.cornerArcRadius * 2;

//            System.out.println("Arc radius: " + values.cornerArcRadius);

            double arcDegrees = 45 - values.cornerHoleOpenAngle;
            values.cornerArcWidth = values.cornerArcRadius * Math.sin(Math.toRadians(arcDegrees));
            values.cornerArcHeight = values.cornerArcRadius * (1 - Math.cos(Math.toRadians(arcDegrees)));

            values.cornerLineShorter = values.cornerHoleTan - values.cornerArcHeight;  // 底袋角直线的占地长宽
            values.cornerLineLonger = Math.tan(Math.toRadians(45.0 + values.cornerHoleOpenAngle)) * values.cornerLineShorter;

            // 我们希望洞内和袋的入口处一样宽，但是袋又有角度，怎么办？
            // 这个值就是x。角a为45度
            /*
            
            *
            *a*.
            *   *  .
            *     *   .
            *       *    .
            * * * * * *......
                         x
             */
            values.cornerHoleDrift = (values.cornerLineLonger - values.cornerLineShorter)
                    * (1 - cornerPocketOut);
//            values.cornerHoleDrift = 0.0;

            double midLineTan = Math.tan(Math.toRadians(values.midHoleOpenAngle));
            values.midLineHeight = values.midHoleRadius;
            values.midLineWidth = values.midLineHeight * midLineTan;
            return this;
        }

        Builder straightHole() {
            values.straightHole = true;
            return this;
        }

        private Builder holeSizeStraight(double cornerHoleDiameter, double midHoleDiameter) {
            values.straightHole = true;
            values.cornerHoleDiameter = cornerHoleDiameter;
            values.midHoleDiameter = midHoleDiameter;
            values.cornerHoleRadius = cornerHoleDiameter / 2;
            values.midHoleRadius = midHoleDiameter / 2;

            values.cornerHoleDt = values.cornerHoleRadius / Math.sqrt(2);
            values.cornerHoleTan = values.cornerHoleRadius * Math.sqrt(2);

            values.cornerLineShorter = values.cornerHoleTan;  // 底袋角直线的占地长宽
            values.cornerLineLonger = Math.tan(Math.toRadians(45.0 + values.cornerHoleOpenAngle)) * values.cornerLineShorter;

            double midLineTan = Math.tan(Math.toRadians(values.midHoleOpenAngle));
            values.midLineHeight = values.midHoleRadius;
            values.midLineWidth = values.midLineHeight * midLineTan;
            return this;
        }

        public Builder holeSize(PocketSize pocketSize) {
//            values.holeSize = holeSize;
            if (values.straightHole) {
                return holeSizeStraight(pocketSize.cornerHoleDiameter, pocketSize.midHoleDiameter);
            } else {
                return holeSizeCurved(pocketSize.cornerHoleDiameter, pocketSize.midHoleDiameter);
            }
        }

        Builder resistanceAndCushionBounce(double tableResistance,
                                           double wallBounce,
                                           double wallSpinPreserve) {
            values.tableResistanceRatio = tableResistance;
            values.wallBounceRatio = wallBounce;
            values.wallSpinPreserveRatio = wallSpinPreserve;
            return this;
        }

//        public Builder ballBounce(double ballBounceRatio) {
//            values.ballBounceRatio = ballBounceRatio;
//            return this;
//        }

        public TableMetrics build() {
            values.build();
            return values;
        }
    }
}
