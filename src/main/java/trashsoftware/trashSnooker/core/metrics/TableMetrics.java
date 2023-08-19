package trashsoftware.trashSnooker.core.metrics;

import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.fxml.App;

import java.util.ArrayList;
import java.util.List;

public class TableMetrics {

    public static final String SNOOKER = "SNOOKER";
    public static final String CHINESE_EIGHT = "CHINESE_EIGHT";
    public static final String POOL_TABLE_10 = "POOL_TABLE_10";
    public static final String AMERICAN_NINE = "AMERICAN_NINE";
    public static final String POOL_TABLE_8 = "POOL_TABLE_8";
    public static final String POOL_TABLE_7 = "POOL_TABLE_7";
    public static final String POOL_TABLE_6 = "POOL_TABLE_6";

    public static final Color GREEN_TABLE_POCKET_LEATHER = Color.valueOf("#BB9977");

    private static final String[] NAMES = {
            SNOOKER,
            CHINESE_EIGHT,
            POOL_TABLE_10,
            AMERICAN_NINE,
            POOL_TABLE_8,
            POOL_TABLE_7,
            POOL_TABLE_6
    };
    public final String tableName;
    public final TableBuilderFactory factory;
    public PocketSize pocketSize;
    public PocketDifficulty pocketDifficulty;

    public Color tableColor;
    public Color gravityAreaColor;
    public Color tableBorderColor;
    public Color cornerPocketBaseColor;
    public Color midPocketBaseColor;
    public double pocketBaseThickness;  // 袋底的厚度，0就是指和外面对齐
    public boolean leatherPocket;

    public double outerWidth;
    public double outerHeight;
    public double innerWidth;
    public double innerHeight;
    public double leftX, rightX, topY, botY, midX, midY;
    //    public double leftClothX, rightClothX, topClothY, botClothY;  // 绿色部分的最大
    public double maxLength;  // 对角线长度
    public double cushionClothWidth;  // 库的视觉宽度
    public double cushionHeight;
    public double speedReduceMultiplier;  // 台泥的阻力系数，值越大阻力越大

    // 角袋入口处的宽度，因为袋的边可能有角度
    public double cornerHoleDiameter, cornerHoleRadius;
    public double midHoleDiameter, midHoleRadius;
    public double cornerPocketGravityRadius;  // 袋口处的坡宽度。球进入这个区域后会开始有往袋里掉的意思
    public double midPocketGravityRadius;
    public double midArcRadius;
    public double cornerHoleDt, cornerHoleTan,
            cornerArcHeight, cornerArcWidth, cornerArcRadius, cornerArcDiameter,
            cornerLineLonger, cornerLineShorter,  // 底袋角直线的占地长宽
            midLineWidth, midLineHeight;  // 中袋角直线占地长宽
    public double cornetHoleGraphicalDt;
    public double cornerHoleDrift;  // 对于有角度的袋，这个值是袋角伸进洞里多远
    public double arcBounceAngleRate;  // 袋角弧线的反射角系数，[0-1]之间，越接近1，越平坦

    public Pocket topLeft;
    public Pocket topMid;
    public Pocket topRight;
    public Pocket botRight;
    public Pocket botMid;
    public Pocket botLeft;
    public Pocket[] pockets;

    public double leftCornerHoleAreaRightX;  // 左顶袋右袋角
    public double midHoleAreaLeftX;  // 中袋左袋角
    public double midHoleAreaRightX;  // 中袋右袋角
    public double rightCornerHoleAreaLeftX;  // 右顶袋左袋角
    public double topCornerHoleAreaDownY;  // 上底袋下袋角
    public double botCornerHoleAreaUpY;  // 下底袋上袋角
    public double midHoleLineLeftX, midHoleLineRightX;  // 中袋袋角直线左右极限（仅直袋口）
    public double topMidLineBotY, botMidLineTopY;  // 上中袋袋角直线最下端/下中袋袋角直线最上端
    // 中袋袋角弧线
    public Cushion.CushionArc topMidHoleLeftArcXy;
    public Cushion.CushionArc topMidHoleRightArcXy;
    public Cushion.CushionArc botMidHoleLeftArcXy;
    public Cushion.CushionArc botMidHoleRightArcXy;

    // 全台6个库的向量，顺时针走向
    public Cushion.EdgeCushion topLeftCushion;
    public Cushion.EdgeCushion topRightCushion;
    public Cushion.EdgeCushion botLeftCushion;
    public Cushion.EdgeCushion botRightCushion;
    public Cushion.EdgeCushion leftCushion;
    public Cushion.EdgeCushion rightCushion;

    // 中袋袋角直线
    public Cushion.CushionLine topMidHoleLeftLine;
    public Cushion.CushionLine topMidHoleRightLine;
    public Cushion.CushionLine botMidHoleLeftLine;
    public Cushion.CushionLine botMidHoleRightLine;
    // 底袋袋角弧线
    public Cushion.CushionArc topLeftHoleSideArcXy;  // 左上底袋边库袋角
    public Cushion.CushionArc topLeftHoleEndArcXy;  // 左上底袋底库袋角
    public Cushion.CushionArc botLeftHoleSideArcXy;
    public Cushion.CushionArc botLeftHoleEndArcXy;
    public Cushion.CushionArc topRightHoleSideArcXy;
    public Cushion.CushionArc topRightHoleEndArcXy;
    public Cushion.CushionArc botRightHoleSideArcXy;
    public Cushion.CushionArc botRightHoleEndArcXy;
    // 底袋袋角直线
    public Cushion.CushionLine topLeftHoleSideLine;
    public Cushion.CushionLine topLeftHoleEndLine;
    public Cushion.CushionLine botLeftHoleSideLine;
    public Cushion.CushionLine botLeftHoleEndLine;
    public Cushion.CushionLine topRightHoleSideLine;
    public Cushion.CushionLine topRightHoleEndLine;
    public Cushion.CushionLine botRightHoleSideLine;
    public Cushion.CushionLine botRightHoleEndLine;
    public Cushion.CushionArc[] allCornerArcs;
    public Cushion.CushionArc[] allMidArcs;
    public Cushion.CushionLine[] allCornerLines;
    public Cushion.CushionLine[] allMidHoleLines;
    public List<double[]> tableStars;  // 颗星
    //    public double ballHoleRatio;
//    public double cornerHoleAngleRatio;  // 打底袋最差的角度和最好的角度差多少
//    public double midHoleBestAngleWidth;  // 中袋对正的容错空间
    public double tableResistanceRatio;
    //    public double ballBounceRatio;
    public double wallBounceRatio;
    public double wallSpinPreserveRatio;
    public double wallSpinEffectRatio;
    public double cushionPowerSpinFactor;  // 比如在美式桌上，大力翻袋反射角会很奇怪
    public double cornerHoleOpenAngle, midHoleOpenAngle;

    public double midPocketThroatWidth;
    private double midPocketMouthWidth;  // 中袋口的宽度

    private TableMetrics(TableBuilderFactory factory, String tableName) {
        this.tableName = tableName;
        this.factory = factory;
    }

    public static TableMetrics.TableBuilderFactory fromOrdinal(int ordinal) {
        return TableBuilderFactory.values()[ordinal];
    }

    public static TableMetrics.TableBuilderFactory fromFactoryName(String typeName) {
        for (TableBuilderFactory tbf : TableBuilderFactory.values()) {
            if (tbf.key.equals(typeName)) return tbf;
        }
        throw new RuntimeException("No such table " + typeName);
    }

    public int getOrdinal() {
        return factory.ordinal();
    }

    public int getHoleSizeOrdinal() {
        for (int i = 0; i < factory.supportedHoles.length; i++) {
            PocketSize size = factory.supportedHoles[i];
            if (pocketSize == size) {
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

    private List<double[]> createStars() {
        List<double[]> stars = new ArrayList<>();

        if (!tableName.equals(AMERICAN_NINE)) return stars;

        double dt = topY / 3 * 2;
        // 长边颗星
        double[] ys = {topY - dt, botY + dt};
        double xStarGap = innerWidth / 8.0;
        for (double y : ys) {
            for (int i = 0; i < 9; i++) {
                if (i % 4 != 0) {
                    double x = leftX + xStarGap * i;
                    stars.add(new double[]{x, y});
                }
            }
        }

        // 短边颗星
        double[] xs = {leftX - dt, rightX + dt};
        double yStarGap = innerHeight / 4.0;
        for (double x : xs) {
            for (int i = 0; i < 5; i++) {
                if (i % 4 != 0) {
                    double y = topY + yStarGap * i;
                    stars.add(new double[]{x, y});
                }
            }
        }

        return stars;
    }

    private void build() {
//        double[] topLeftHoleXY = new double[]
//                {leftX - cornerHoleDt, topY - cornerHoleDt};
//        double[] botLeftHoleXY = new double[]
//                {leftX - cornerHoleDt, botY + cornerHoleDt};
//        double[] topRightHoleXY = new double[]
//                {rightX + cornerHoleDt, topY - cornerHoleDt};
//        double[] botRightHoleXY = new double[]
//                {rightX + cornerHoleDt, botY + cornerHoleDt};
        double[] topMidHoleXY = new double[]
                {midX, topY - midHoleRadius};
        double[] botMidHoleXY = new double[]
                {midX, botY + midHoleRadius};

        // 仅为了让袋口看起来好看一点用
        double[] topLeftHoleGraXY = new double[]
                {leftX - cornetHoleGraphicalDt, topY - cornetHoleGraphicalDt};
        double[] botLeftHoleGraXY = new double[]
                {leftX - cornetHoleGraphicalDt, botY + cornetHoleGraphicalDt};
        double[] topRightHoleGraXY = new double[]
                {rightX + cornetHoleGraphicalDt, topY - cornetHoleGraphicalDt};
        double[] botRightHoleGraXY = new double[]
                {rightX + cornetHoleGraphicalDt, botY + cornetHoleGraphicalDt};
        double[] topMidHoleGraXY = new double[]
                {midX, topY - (cushionClothWidth + midHoleRadius) / 2};
        double[] botMidHoleGraXY = new double[]
                {midX, botY + (cushionClothWidth + midHoleRadius) / 2};

//        leftClothX = leftX - cornerHoleTan;
//        rightClothX = rightX + cornerHoleTan;
//        topClothY = topY - cornerHoleTan;
//        botClothY = botY + cornerHoleTan;

//        allHoles = new double[][]{
//                topLeftHoleXY,
//                botLeftHoleXY,
//                topRightHoleXY,
//                botRightHoleXY,
//                topMidHoleXY,
//                botMidHoleXY
//        };

        double[] topLeftSlateXY = new double[]{
                leftX - cushionClothWidth,
                topY - cushionClothWidth
        };
        double[] topRightSlateXY = new double[]{
                rightX + cushionClothWidth,
                topY - cushionClothWidth
        };
        double[] botLeftSlateXY = new double[]{
                leftX - cushionClothWidth,
                botY + cushionClothWidth
        };
        double[] botRightSlateXY = new double[]{
                rightX + cushionClothWidth,
                botY + cushionClothWidth
        };

        double[] topMidFallCenter = new double[]{
                midX,
                topY - cushionClothWidth - pocketDifficulty.midCenterToSlate
        };
        double[] botMidFallCenter = new double[]{
                midX,
                botY + cushionClothWidth + pocketDifficulty.midCenterToSlate
        };

        double midGraphicalRadius = leatherPocket ?
                midPocketBackInnerRadius() :
                midHoleRadius;
        double cornerGraphicalRadius = leatherPocket ?
                cornerPocketBackInnerRadius() :
                cornerHoleRadius;

        topLeft = new Pocket(
                false,
                topLeftSlateXY,
                pocketDifficulty.cornerPocketFallRadius,
                topLeftHoleGraXY,
                cornerGraphicalRadius,
                pocketDifficulty.cornerPocketGravityZone
        );
        topRight = new Pocket(
                false,
                topRightSlateXY,
                pocketDifficulty.cornerPocketFallRadius,
                topRightHoleGraXY,
                cornerGraphicalRadius,
                pocketDifficulty.cornerPocketGravityZone
        );
        botLeft = new Pocket(
                false,
                botLeftSlateXY,
                pocketDifficulty.cornerPocketFallRadius,
                botLeftHoleGraXY,
                cornerGraphicalRadius,
                pocketDifficulty.cornerPocketGravityZone
        );
        botRight = new Pocket(
                false,
                botRightSlateXY,
                pocketDifficulty.cornerPocketFallRadius,
                botRightHoleGraXY,
                cornerGraphicalRadius,
                pocketDifficulty.cornerPocketGravityZone
        );
        topMid = new Pocket(
                true,
                topMidFallCenter,
                pocketDifficulty.midPocketFallRadius,
                topMidHoleGraXY,
                midGraphicalRadius,
                pocketDifficulty.midPocketGravityZone
        );
        botMid = new Pocket(
                true,
                botMidFallCenter,
                pocketDifficulty.midPocketFallRadius,
                botMidHoleGraXY,
                midGraphicalRadius,
                pocketDifficulty.midPocketGravityZone
        );
        pockets = new Pocket[]{
                topLeft, topMid, topRight, botRight, botMid, botLeft
        };

        leftCornerHoleAreaRightX = leftX + cornerArcWidth + cornerLineLonger - cornerHoleDrift;  // 左顶袋右袋角
        rightCornerHoleAreaLeftX = rightX - cornerArcWidth - cornerLineLonger + cornerHoleDrift;  // 右顶袋左袋角
        topCornerHoleAreaDownY = topY + cornerArcWidth + cornerLineLonger - cornerHoleDrift;  // 上底袋下袋角
        botCornerHoleAreaUpY = botY - cornerArcWidth - cornerLineLonger + cornerHoleDrift;  // 下底袋上袋角

        midHoleAreaLeftX = midX - midPocketThroatWidth / 2 - midLineWidth - midArcRadius;
        midHoleAreaRightX = midX + midPocketThroatWidth / 2 + midLineWidth + midArcRadius;

        // 中袋袋角弧线，圆心的位置
        topMidHoleLeftArcXy =
                new Cushion.CushionArc(
                        new double[]{midHoleAreaLeftX, topY - midArcRadius}
                );
        topMidHoleRightArcXy =
                new Cushion.CushionArc(
                        new double[]{midHoleAreaRightX, topY - midArcRadius}
                );
        botMidHoleLeftArcXy =
                new Cushion.CushionArc(
                        new double[]{midHoleAreaLeftX, botY + midArcRadius}
                );
        botMidHoleRightArcXy =
                new Cushion.CushionArc(
                        new double[]{midHoleAreaRightX, botY + midArcRadius}
                );

        // 底袋袋角弧线
        topLeftHoleSideArcXy =  // 左上底袋边库袋角
                new Cushion.CushionArc(
                        new double[]{leftX + cornerArcWidth + cornerLineLonger - cornerHoleDrift, topY - cornerArcRadius}
                );
        topLeftHoleEndArcXy =  // 左上底袋底库袋角
                new Cushion.CushionArc(
                        new double[]{leftX - cornerArcRadius, topY + cornerArcWidth + cornerLineLonger - cornerHoleDrift}
                );
        botLeftHoleSideArcXy =
                new Cushion.CushionArc(
                        new double[]{leftX + cornerArcWidth + cornerLineLonger - cornerHoleDrift, botY + cornerArcRadius}
                );
        botLeftHoleEndArcXy =
                new Cushion.CushionArc(
                        new double[]{leftX - cornerArcRadius, botY - cornerArcWidth - cornerLineLonger + cornerHoleDrift}
                );

        topRightHoleSideArcXy =
                new Cushion.CushionArc(
                        new double[]{rightX - cornerArcWidth - cornerLineLonger + cornerHoleDrift, topY - cornerArcRadius}
                );
        topRightHoleEndArcXy =
                new Cushion.CushionArc(
                        new double[]{rightX + cornerArcRadius, topY + cornerArcWidth + cornerLineLonger - cornerHoleDrift}
                );
        botRightHoleSideArcXy =
                new Cushion.CushionArc(
                        new double[]{rightX - cornerArcWidth - cornerLineLonger + cornerHoleDrift, botY + cornerArcRadius}
                );
        botRightHoleEndArcXy =
                new Cushion.CushionArc(
                        new double[]{rightX + cornerArcRadius, botY - cornerArcWidth - cornerLineLonger + cornerHoleDrift}
                );

        // 库
        topLeftCushion = new Cushion.EdgeCushion("TOP_LEFT", new double[][]{
                {topLeftHoleSideArcXy.getCenter()[0], topY}, {topMidHoleLeftArcXy.getCenter()[0], topY}
        });
        topRightCushion = new Cushion.EdgeCushion("TOP_RIGHT", new double[][]{
                {topMidHoleRightArcXy.getCenter()[0], topY}, {topRightHoleSideArcXy.getCenter()[0], topY}
        });
        rightCushion = new Cushion.EdgeCushion("RIGHT", new double[][]{
                {rightX, topRightHoleEndArcXy.getCenter()[1]}, {rightX, botRightHoleEndArcXy.getCenter()[1]}
        });
        botRightCushion = new Cushion.EdgeCushion("BOT_RIGHT", new double[][]{
                {botRightHoleSideArcXy.getCenter()[0], botY}, {botMidHoleRightArcXy.getCenter()[0], botY}
        });
        botLeftCushion = new Cushion.EdgeCushion("BOT_LEFT", new double[][]{
                {botMidHoleLeftArcXy.getCenter()[0], botY}, {botRightHoleSideArcXy.getCenter()[0], botY}
        });
        leftCushion = new Cushion.EdgeCushion("LEFT", new double[][]{
                {leftX, botLeftHoleEndArcXy.getCenter()[1]}, {leftX, topLeftHoleEndArcXy.getCenter()[1]}
        });

        // 中袋袋角直线
        midHoleLineLeftX = midX - midHoleRadius - midLineWidth;
        midHoleLineRightX = midX + midHoleRadius + midLineWidth;
        topMidLineBotY = topMidHoleXY[1] + midLineHeight;
        botMidLineTopY = botMidHoleXY[1] - midLineHeight;

        topMidHoleLeftLine =
                new Cushion.CushionLine(
                        new double[][]{{midX - midHoleRadius, topMidHoleXY[1]},
                                {midHoleLineLeftX, topMidLineBotY}}
                );
        topMidHoleRightLine =
                new Cushion.CushionLine(
                        new double[][]{{midX + midHoleRadius, topMidHoleXY[1]},
                                {midHoleLineRightX, topMidLineBotY}}
                );
        botMidHoleLeftLine =
                new Cushion.CushionLine(
                        new double[][]{{midX - midHoleRadius, botMidHoleXY[1]},
                                {midHoleLineLeftX, botMidLineTopY}}
                );
        botMidHoleRightLine =
                new Cushion.CushionLine(
                        new double[][]{{midX + midHoleRadius, botMidHoleXY[1]},
                                {midHoleLineRightX, botMidLineTopY}}
                );

        // 底袋袋角直线
        topLeftHoleEndLine =
                new Cushion.CushionLine(
                        new double[][]{
                                {leftX - cornerArcHeight, topY + cornerLineLonger - cornerHoleDrift},  // 浅
                                {leftX - cornerHoleTan, topY - cornerHoleDrift}  // 深
                        }
                );
        topLeftHoleSideLine =
                new Cushion.CushionLine(
                        new double[][]{
                                {leftX - cornerHoleDrift, topY - cornerHoleTan},  // 深
                                {leftX + cornerLineLonger - cornerHoleDrift, topY - cornerArcHeight}  // 浅
                        }
                );

        topRightHoleSideLine =
                new Cushion.CushionLine(
                        new double[][]{
                                {rightX - cornerLineLonger + cornerHoleDrift, topY - cornerArcHeight},  // 浅
                                {rightX + cornerHoleDrift, topY - cornerHoleTan}  // 深
                        }
                );
        topRightHoleEndLine =
                new Cushion.CushionLine(
                        new double[][]{
                                {rightX + cornerHoleTan, topY - cornerHoleDrift},  // 深
                                {rightX + cornerArcHeight, topY + cornerLineLonger - cornerHoleDrift}  // 浅
                        }
                );

        botRightHoleEndLine =
                new Cushion.CushionLine(
                        new double[][]{
                                {rightX + cornerArcHeight, botY - cornerLineLonger + cornerHoleDrift},  // 浅
                                {rightX + cornerHoleTan, botY + cornerHoleDrift}  // 深
                        }
                );
        botRightHoleSideLine =
                new Cushion.CushionLine(
                        new double[][]{
                                {rightX + cornerHoleDrift, botY + cornerHoleTan},  // 深
                                {rightX - cornerLineLonger + cornerHoleDrift, botY + cornerArcHeight}  // 浅
                        }
                );

        botLeftHoleSideLine =
                new Cushion.CushionLine(
                        new double[][]{
                                {leftX + cornerLineLonger - cornerHoleDrift, botY + cornerArcHeight},  // 浅
                                {leftX - cornerHoleDrift, botY + cornerHoleTan}  // 深
                        }
                );
        botLeftHoleEndLine =
                new Cushion.CushionLine(
                        new double[][]{
                                {leftX - cornerHoleTan, botY + cornerHoleDrift},  // 深
                                {leftX - cornerArcHeight, botY - cornerLineLonger + cornerHoleDrift}  // 浅
                        }
                );

        allCornerArcs = new Cushion.CushionArc[]{
                topLeftHoleSideArcXy,
                topLeftHoleEndArcXy,
                topRightHoleSideArcXy,
                topRightHoleEndArcXy,
                botLeftHoleSideArcXy,
                botLeftHoleEndArcXy,
                botRightHoleSideArcXy,
                botRightHoleEndArcXy
        };
        allMidArcs = new Cushion.CushionArc[]{
                topMidHoleLeftArcXy,
                topMidHoleRightArcXy,
                botMidHoleLeftArcXy,
                botMidHoleRightArcXy
        };

        allCornerLines = new Cushion.CushionLine[]{
                topLeftHoleSideLine,  // "\"
                topLeftHoleEndLine,  // "\"
                botRightHoleSideLine,  // "\"
                botRightHoleEndLine,  // "\"
                topRightHoleSideLine,  // "/"
                topRightHoleEndLine,  // "/"
                botLeftHoleSideLine,  // "/"
                botLeftHoleEndLine  // "/"
        };

        allMidHoleLines = new Cushion.CushionLine[]{
                topMidHoleLeftLine,
                topMidHoleRightLine,
                botMidHoleLeftLine,
                botMidHoleRightLine
        };

        pocketBaseThickness = Math.min(pocketBaseThickness, topLeftHoleGraXY[0]);

        tableStars = createStars();
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

    public double cornerPocketBackInnerRadius() {
        return factory.supportedHoles[0].cornerHoleDiameter / 2;
    }

    public double midPocketBackInnerRadius() {
        return factory.supportedHoles[0].midHoleDiameter / 2;
    }

    public enum TableBuilderFactory {
        SNOOKER("snookerTable",
                PocketSize.SNOOKER_HOLES,
                PocketDifficulty.GREEN_TABLE_DIFFICULTIES) {
            @Override
            public Builder create() {
                return new Builder(this, TableMetrics.SNOOKER)
                        .tableColorLeather(Color.GREEN, Color.SADDLEBROWN, GREEN_TABLE_POCKET_LEATHER, 36.0)
                        .tableDimension(3568.7,  // 140.5"
                                1788.0,
                                124,
                                47.625,
                                33.34)
//                        .supportedHoles(SNOOKER_HOLES)
                        .resistanceAndCushionBounce(1.0,
                                0.86,
                                0.85,
                                0.8,
                                0.35);
            }
        },
        CHINESE_EIGHT("chineseEightTable",
                PocketSize.CHINESE_EIGHT_HOLES,
                PocketDifficulty.GREEN_TABLE_DIFFICULTIES) {
            @Override
            public Builder create() {
                return new Builder(this, TableMetrics.CHINESE_EIGHT)
                        .tableColorLeather(Color.GREEN, Color.SADDLEBROWN, GREEN_TABLE_POCKET_LEATHER, 36.0)
                        .tableDimension(2540.0,  // 100"
                                1270.0,
                                124,
                                51.0,
                                42.0)
//                        .supportedHoles(CHINESE_EIGHT_HOLES)
                        .resistanceAndCushionBounce(1.05,
                                0.84,
                                0.8,
                                0.8,
                                0.35);
            }
        },
        POOL_TABLE_10("poolTable10",
                PocketSize.SIDE_POCKET_HOLES,
                PocketDifficulty.BLUE_TABLE_DIFFICULTIES) {
            @Override
            public Builder create() {
                return new Builder(this, TableMetrics.POOL_TABLE_10)
                        .tableColorHard(Color.STEELBLUE, Color.BLACK.brighter().brighter(), Color.SLATEGREY)
                        .tableDimension(2844.8,  // 112"
                                1422.4,
                                182.5,
                                51.0,
                                42.0)
//                        .supportedHoles(SIDE_POCKET_HOLES)
                        .resistanceAndCushionBounce(1.0,
                                0.8,
                                1.15,
                                0.9,
                                0.8);
            }
        },
        POOL_TABLE_9("poolTable9",
                PocketSize.SIDE_POCKET_HOLES,
                PocketDifficulty.BLUE_TABLE_DIFFICULTIES) {
            @Override
            public Builder create() {
                return new Builder(this, TableMetrics.AMERICAN_NINE)
                        .tableColorHard(Color.STEELBLUE, Color.BLACK.brighter().brighter(), Color.SLATEGREY)
                        .tableDimension(2540.0,
                                1270.0,
                                182.5,
                                51.0,
                                42.0)
//                        .supportedHoles(SIDE_POCKET_HOLES)
                        .resistanceAndCushionBounce(1.0,
                                0.8,
                                1.15,
                                0.9,
                                0.8);
            }
        },
        POOL_TABLE_8("poolTable8",
                PocketSize.SIDE_POCKET_HOLES,
                PocketDifficulty.BLUE_TABLE_DIFFICULTIES) {
            @Override
            public Builder create() {
                return new Builder(this, TableMetrics.POOL_TABLE_8)
                        .tableColorHard(Color.STEELBLUE, Color.BLACK.brighter().brighter(), Color.SLATEGREY)
                        .tableDimension(2235.2,  // 88"
                                1117.6,
                                182.5,
                                51.0,
                                42.0)
//                        .supportedHoles(SIDE_POCKET_HOLES)
                        .resistanceAndCushionBounce(1.0,
                                0.8,
                                1.15,
                                0.9,
                                0.8);
            }
        },
        POOL_TABLE_7("poolTable7",
                PocketSize.SIDE_POCKET_HOLES,
                PocketDifficulty.BLUE_TABLE_DIFFICULTIES) {
            @Override
            public Builder create() {
                return new Builder(this, TableMetrics.POOL_TABLE_7)
                        .tableColorHard(Color.STEELBLUE,
                                Color.BLACK.brighter().brighter(),
                                Color.SLATEGREY,
                                Color.BLACK.brighter().brighter())
                        .tableDimension(1981.2,  // 78"
                                990.6,
                                165.1,
                                51.0,
                                42.0)
//                        .supportedHoles(SIDE_POCKET_HOLES)
                        .resistanceAndCushionBounce(1.0,
                                0.7,
                                1.15,
                                0.9,
                                0.8);
            }
        },
        POOL_TABLE_6("poolTable6",
                PocketSize.SIDE_POCKET_HOLES,
                PocketDifficulty.BLUE_TABLE_DIFFICULTIES) {
            @Override
            public Builder create() {
                return new Builder(this, TableMetrics.POOL_TABLE_6)
                        .tableColorHard(Color.STEELBLUE,
                                Color.BLACK.brighter().brighter(),
                                Color.SLATEGREY,
                                Color.BLACK.brighter().brighter())
                        .tableDimension(1828.8,  // 72"
                                914.4,
                                152.4,
                                51.0,
                                42.0)
//                        .supportedHoles(SIDE_POCKET_HOLES)
                        .resistanceAndCushionBounce(1.0,
                                0.7,
                                1.15,
                                0.9,
                                0.8);
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
//        private double cornerPocketOut;

        public Builder(TableBuilderFactory factory, String tableName) {
            this.values = new TableMetrics(factory, tableName);
        }

        private static double findMidPocketThroatWidth(double mouthWidth,
                                                       double arcRadius,
                                                       double lineAngle,
                                                       double lineExtraHeight,
                                                       double lineExtraWidth) {
            // 通过二分搜索凑出合适的洞底直径
            double radiusLow = 0;
            double radiusHigh = mouthWidth / 2;
            double tan = Math.tan(Math.toRadians(lineAngle));
            while (radiusHigh - radiusLow > 0.1) {
                double radiusAvg = (radiusHigh + radiusLow) / 2;
                double calculatedLineHeight = radiusAvg - arcRadius + lineExtraHeight;
                double calculatedLineWidth = calculatedLineHeight * tan + lineExtraWidth;
                double calculatedMouthWidth = radiusAvg * 2 + calculatedLineWidth * 2;
                if (calculatedMouthWidth > mouthWidth) {
                    radiusHigh = radiusAvg;
                } else {
                    radiusLow = radiusAvg;
                }

            }
            return radiusHigh + radiusLow;
        }

        Builder tableColorHard(Color color, Color borderColor, Color pocketBaseColor) {
            return tableColorHard(color, borderColor, pocketBaseColor, pocketBaseColor);
        }

        Builder tableColorHard(Color color, Color borderColor, Color cornerPocketBaseColor, Color midPocketColor) {
            values.tableColor = color;
            values.gravityAreaColor = color.deriveColor(0, 1, 0.9, 1);
            values.tableBorderColor = borderColor;
            values.cornerPocketBaseColor = cornerPocketBaseColor;
            values.midPocketBaseColor = midPocketColor;
            values.pocketBaseThickness = 0.0;
            values.leatherPocket = false;
            return this;
        }

        Builder tableColorLeather(Color color,
                                  Color borderColor,
                                  Color pocketBaseColor,
                                  double pocketBaseThickness) {
            values.tableColor = color;
            values.gravityAreaColor = color.deriveColor(0, 1, 0.9, 1);
            values.tableBorderColor = borderColor;
            values.cornerPocketBaseColor = pocketBaseColor;
            values.midPocketBaseColor = pocketBaseColor;
            values.pocketBaseThickness = pocketBaseThickness;
            values.leatherPocket = true;
            return this;
        }

        Builder tableDimension(double innerWidth,
                               double innerHeight,
                               double borderWidth,
                               double cushionClothWidth,
                               double cushionHeight) {
            double outerWidth = innerWidth + borderWidth * 2;
            double outerHeight = innerHeight + borderWidth * 2;

            values.outerWidth = outerWidth;
            values.innerWidth = innerWidth;
            values.outerHeight = outerHeight;
            values.innerHeight = innerHeight;
            values.cushionClothWidth = cushionClothWidth;
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

        public Builder pocketDifficulty(PocketDifficulty pocketDifficulty) {
            values.pocketDifficulty = pocketDifficulty;
            return pocketDifficulty(
                    pocketDifficulty.cornerPocketGravityZone,
                    pocketDifficulty.cornerPocketArcSize,
                    pocketDifficulty.cornerPocketAngle,
                    pocketDifficulty.midPocketGravityZone,
                    pocketDifficulty.midPocketArcSize,
                    pocketDifficulty.midPocketAngle
            );
        }

        private Builder pocketDifficulty(double cornerPocketGravityZone,
                                         double cornerPocketArcSize,
                                         double cornerPocketAngle,
                                         double midPocketGravityZone,
                                         double midPocketArcSize,
                                         double midPocketAngle) {
            values.cornerPocketGravityRadius = cornerPocketGravityZone;
            values.cornerHoleOpenAngle = cornerPocketAngle;
            values.midPocketGravityRadius = midPocketGravityZone;
            values.midHoleOpenAngle = midPocketAngle;
            this.cornerHoleArcSizeMul = cornerPocketArcSize;
//            this.cornerPocketOut = cornerPocketOut;

//            if (!values.straightHole) {
            this.midHoleArcSizeMul = midPocketArcSize;
//            }
            return this;
        }

        private void setCornerHoleSize(double cornerHoleDiameter) {
            double cornerHoleRadius = cornerHoleDiameter / 2;

            values.cornerHoleDiameter = cornerHoleDiameter;
            values.cornerHoleRadius = cornerHoleRadius;

            double holeDtOrig = cornerHoleRadius / Math.sqrt(2);
            values.cornetHoleGraphicalDt = holeDtOrig;
            values.cornerHoleDt = holeDtOrig;
            values.cornerHoleTan = cornerHoleRadius * Math.sqrt(2);

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
//            values.cornerHoleDrift = (values.cornerLineLonger - values.cornerLineShorter) * (1 - cornerPocketOut);
            values.cornerHoleDrift = values.cornerLineLonger - values.cornerLineShorter;
        }

        private void setupMidHoleByThroat(PocketSize pocketSize) {
            // 目前仅支持圆袋角
            values.midPocketThroatWidth = pocketSize.midThroatWidth;
            values.midPocketMouthWidth = values.midPocketThroatWidth;  // 没有直线，一样的

            values.midHoleDiameter = pocketSize.midHoleDiameter;
            values.midHoleRadius = values.midHoleDiameter / 2;

            values.midArcRadius = pocketSize.midArcRadius * midHoleArcSizeMul;
        }

        private void setupMidHoleByMouth(PocketSize pocketSize) {
            double midHoleMouthWidth = pocketSize.midHoleDiameter;
            values.midPocketMouthWidth = midHoleMouthWidth;
            values.midArcRadius = pocketSize.midArcRadius * midHoleArcSizeMul;

            double midArcCos = Math.cos(Math.toRadians(values.midHoleOpenAngle)) * values.midArcRadius;
            double midArcSin = Math.sin(Math.toRadians(values.midHoleOpenAngle)) * values.midArcRadius;
            double midLineExtraWidth = values.midArcRadius - midArcCos;  // 如果中袋为0度，这个也是0

            if (values.midHoleOpenAngle == 0.0) {
                values.midHoleDiameter = midHoleMouthWidth;
            } else {
                // 几何解不出来，通过凑数来寻找近似解
                values.midHoleDiameter = findMidPocketThroatWidth(
                        midHoleMouthWidth, values.midArcRadius, values.midHoleOpenAngle, midArcSin, midLineExtraWidth);
//                System.out.println("Solved mid dia: " + values.midHoleDiameter);
            }
            values.midPocketThroatWidth = values.midHoleDiameter;
            values.midHoleRadius = values.midHoleDiameter / 2;

            values.midLineHeight = values.midHoleRadius - values.midArcRadius + midArcSin;
            double midLineTan = Math.tan(Math.toRadians(values.midHoleOpenAngle));
            values.midLineWidth = values.midLineHeight * midLineTan + midLineExtraWidth;
        }

        public Builder holeSize(PocketSize pocketSize) {
            if (cornerHoleArcSizeMul == 0) {
                throw new RuntimeException("Method 'pocketDifficulty' must be called prior to this method");
            }
            values.pocketSize = pocketSize;

            if (pocketSize.midHoleThroatSpecified) {
                setupMidHoleByThroat(pocketSize);
            } else {
                setupMidHoleByMouth(pocketSize);
            }
            setCornerHoleSize(pocketSize.cornerHoleDiameter);

            return this;
        }

        Builder resistanceAndCushionBounce(double tableResistance,
                                           double wallBounce,
                                           double wallSpinEffect,
                                           double wallSpinPreserve,
                                           double cushionPowerSpin) {
            values.tableResistanceRatio = tableResistance;
            values.speedReduceMultiplier = tableResistance;
            values.wallBounceRatio = wallBounce;
            values.wallSpinEffectRatio = wallSpinEffect;
            values.wallSpinPreserveRatio = wallSpinPreserve;
            values.cushionPowerSpinFactor = cushionPowerSpin;
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
