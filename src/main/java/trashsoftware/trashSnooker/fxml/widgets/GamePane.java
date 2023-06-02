package trashsoftware.trashSnooker.fxml.widgets;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.TextAlignment;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.GameHolder;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;
import trashsoftware.trashSnooker.core.movement.WhitePrediction;
import trashsoftware.trashSnooker.core.table.Table;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.fxml.drawing.CurvedPolygonDrawer;
import trashsoftware.trashSnooker.util.ConfigLoader;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class GamePane extends Pane {
    public static final Color LINE_PAINT = Color.DIMGRAY.darker().darker().darker();
    public static final Color HOLE_PAINT = Color.DIMGRAY.darker().darker().darker();
    public static final Color WHITE_PREDICTION_COLOR = Color.LIGHTGREY;

    final double cornerHoleVisualMul = 1.02;
    private final CurvedPolygonDrawer curvedPolygonDrawer = new CurvedPolygonDrawer(0.667);  // 弯的程度
    @FXML
    Canvas gameCanvas;
    GraphicsContext graphicsContext;
    private double scale;
    private GameValues gameValues;
    private double canvasWidth, canvasHeight;
    private ResourceBundle strings;

    public GamePane() {
        this(App.getStrings());
    }

    public GamePane(ResourceBundle strings) {
        super();

        this.strings = strings;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "gamePane.fxml"), strings);
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        graphicsContext = gameCanvas.getGraphicsContext2D();
        graphicsContext.setTextAlign(TextAlignment.CENTER);
        graphicsContext.setFont(App.FONT);
    }

    public void setupPane(GameValues gameValues, double scaleMul) {
        this.gameValues = gameValues;

        generateScales(scaleMul);
    }

    public void setupPane(GameValues gameValues) {
        setupPane(gameValues, 1.0);
    }

    private void generateScales(double scaleMul) {
        TableMetrics values = gameValues.table;
        double[] screenParams = ConfigLoader.getInstance().getResolution();  // 宽，高，缩放
        double graphWidth = screenParams[0] / screenParams[2] - 180;
        double graphHeight = screenParams[1] / screenParams[2] - 180;

        double scaleW = graphWidth / values.outerWidth;
        double scaleH = graphHeight / values.outerHeight;

        scale = Math.min(scaleW, scaleH) * scaleMul;

        System.out.printf("Scales: w: %.2f, h: %.2f, global: %.2f \n", scaleW, scaleH, getScale());

        canvasWidth = values.outerWidth * scale;
        canvasHeight = values.outerHeight * scale;
//        innerHeight = values.innerHeight * scale;

        setupCanvas();
    }

    public double getScale() {
        return scale;
    }

    public Canvas getGameCanvas() {
        return gameCanvas;
    }

    public void setupBalls(GameHolder gameHolder, boolean randomRotate) {
        clear();

        for (Ball ball : gameHolder.getAllBalls()) {
            ball.model.setVisualRadius(gameValues.ball.ballRadius * scale);
            ball.model.initRotation(randomRotate);
            getChildren().add(ball.model.sphere);
            ball.model.sphere.setMouseTransparent(true);
        }
    }

    public void clear() {
        getChildren().clear();
        getChildren().add(gameCanvas);
    }

    public void drawBallInHandEssential(Ball ball, Table table, double mouseX, double mouseY) {
        TableMetrics values = gameValues.table;

        double x = realX(mouseX);
        if (x < values.leftX + gameValues.ball.ballRadius)
            x = values.leftX + gameValues.ball.ballRadius;
        else if (x >= values.rightX - gameValues.ball.ballRadius)
            x = values.rightX - gameValues.ball.ballRadius;

        double y = realY(mouseY);
        if (y < values.topY + gameValues.ball.ballRadius)
            y = values.topY + gameValues.ball.ballRadius;
        else if (y >= values.botY - gameValues.ball.ballRadius)
            y = values.botY - gameValues.ball.ballRadius;

        table.forceDrawBallInHand(
                this,
                ball,
                x,
                y
        );
    }

    public double canvasX(double realX) {
        return realX * scale;
    }

    public double canvasY(double realY) {
        return realY * scale;
    }

    public double realX(double canvasX) {
        return canvasX / scale;
    }

    public double realY(double canvasY) {
        return canvasY / scale;
    }

    private void setupCanvas() {
        gameCanvas.setWidth(canvasWidth);
        gameCanvas.setHeight(canvasHeight);
        setPrefWidth(canvasWidth);
        setPrefHeight(canvasHeight);
    }
    
    private double getCornerHoleVisualRadiusUnscaled(TableMetrics metrics) {
//        return metrics.cornerHoleRadius * cornerHoleVisualMul;
        return metrics.leatherPocket ? 
                metrics.factory.supportedHoles[0].cornerHoleDiameter / 2 :
                metrics.cornerHoleRadius;
    }

    private double getMidHoleVisualRadiusUnscaled(TableMetrics metrics) {
//        return metrics.cornerHoleRadius * cornerHoleVisualMul;
        return metrics.leatherPocket ? 
                metrics.factory.supportedHoles[0].midHoleDiameter / 2 :
                metrics.midHoleRadius;
    }

    private void drawLeatherTable(TableMetrics metrics) {
//        double cornerVisualRadius = (metrics.topLeftHoleGraXY[0] - metrics.pocketBaseInside) * scale;
        double cornerVisualRadius = (getCornerHoleVisualRadiusUnscaled(metrics) + metrics.pocketBaseThickness) * scale;
        
        // 画中袋的袋底
        double leatherThickness = (cornerVisualRadius -
                getCornerHoleVisualRadiusUnscaled(metrics) * scale) * 0.8;  // 我们希望中袋和底袋厚度比例固定
        double inside = (metrics.topMidHoleGraXY[1] - getMidHoleVisualRadiusUnscaled(metrics)) 
                * scale - leatherThickness;  // 圆心在topY的地方
        inside = Math.max(inside, 0);  // 不要超过球台边缘了

        double midBaseRadius = metrics.topY * scale - inside;
        graphicsContext.setFill(metrics.midPocketBaseColor);
        graphicsContext.fillArc(
                metrics.midX * scale - midBaseRadius,
                inside,
                midBaseRadius * 2,
                midBaseRadius * 2,
                0, 180, ArcType.CHORD
        );
        graphicsContext.fillArc(
                metrics.midX * scale - midBaseRadius,
                metrics.outerHeight * scale - inside - midBaseRadius * 2,
                midBaseRadius * 2,
                midBaseRadius * 2,
                180, 180, ArcType.CHORD
        );
        
        drawCornerBases(metrics, cornerVisualRadius);

        fillTableWithoutPocketArea(metrics, 15.0 * scale);
    }

    private void drawHardTable(TableMetrics metrics) {
        double midPocketNoFill = 30.0 * scale;

        double cornerVisualRadius = metrics.topLeftHoleGraXY[0] * scale;
        drawCornerBases(metrics, cornerVisualRadius);

        double midBaseLeftX = (metrics.midX - metrics.midHoleRadius) * scale - midPocketNoFill;
        double midBaseWidth = metrics.midHoleDiameter * scale + midPocketNoFill * 2;
        double height = (metrics.topY - metrics.cushionClothWidth) * scale;
        graphicsContext.setFill(metrics.midPocketBaseColor);
        graphicsContext.fillRect(midBaseLeftX, 0, midBaseWidth, height + 1);  // +1防止那个白边
        graphicsContext.fillRect(midBaseLeftX, metrics.outerHeight * scale - height - 1, midBaseWidth, height + 1);

        fillTableWithoutPocketArea(metrics, midPocketNoFill);
        
        // 袋口保护塑料
        double plasticProtectorWidth = 9.0;
        graphicsContext.setFill(Color.BLACK);
        double midPlasticRadius = getMidHoleVisualRadiusUnscaled(metrics) + plasticProtectorWidth;  // 这种桌子中袋里面大小不变
        drawHole(metrics.topMidHoleGraXY, midPlasticRadius);
        drawHole(metrics.botMidHoleGraXY, midPlasticRadius);

        double cornerPlasticRadius = getCornerHoleVisualRadiusUnscaled(metrics) + plasticProtectorWidth;
        drawHole(metrics.topLeftHoleGraXY, cornerPlasticRadius);
        drawHole(metrics.botLeftHoleGraXY, cornerPlasticRadius);
        drawHole(metrics.topRightHoleGraXY, cornerPlasticRadius);
        drawHole(metrics.botRightHoleGraXY, cornerPlasticRadius);
    }

    private void drawCornerBases(TableMetrics metrics, double cornerVisualRadius) {
        graphicsContext.setFill(metrics.cornerPocketBaseColor);
        drawCornerBase(metrics.topLeftHoleGraXY, cornerVisualRadius, 80, 110);
        drawCornerBase(metrics.topRightHoleGraXY, cornerVisualRadius, 350, 110);
        drawCornerBase(metrics.botLeftHoleGraXY, cornerVisualRadius, 170, 110);
        drawCornerBase(metrics.botRightHoleGraXY, cornerVisualRadius, 260, 110);

        double arcRadius = metrics.topLeftHoleGraXY[0] * scale;
        double visualIn = metrics.topLeftHoleGraXY[0] * scale - cornerVisualRadius;
        double narrow = (metrics.leftX - metrics.topLeftHoleGraXY[0]) * scale;
        double wide = (metrics.leftX - metrics.cornerHoleTan) * scale - visualIn;

        double rightRight = metrics.outerWidth * scale;
        double rightLeft = metrics.rightX * scale;

        // 底袋旁边的两溜
        // 左上
        graphicsContext.fillRect(arcRadius,
                visualIn,
                narrow,
                wide);
        graphicsContext.fillRect(visualIn,
                arcRadius,
                wide,
                narrow);

        // 右上
        graphicsContext.fillRect(rightLeft,
                visualIn,
                narrow,
                wide);
        graphicsContext.fillRect(rightRight - wide - visualIn,
                arcRadius,
                wide,
                narrow);

        // 左下
        graphicsContext.fillRect(arcRadius,
                metrics.outerHeight * scale - wide - visualIn,
                narrow,
                wide);
        graphicsContext.fillRect(visualIn,
                metrics.botY * scale,
                wide,
                narrow);

        // 右下
        graphicsContext.fillRect(rightLeft,
                metrics.outerHeight * scale - wide - visualIn,
                narrow,
                wide);
        graphicsContext.fillRect(rightRight - wide - visualIn,
                metrics.botY * scale,
                wide,
                narrow);
    }

    private void drawCornerBase(double[] centerPos,
                                double visualRadius,
                                double startAngle, double arcExtent) {
        double drawingDia = visualRadius * 2;
        double x = centerPos[0] * scale - visualRadius;
        double y = centerPos[1] * scale - visualRadius;
        graphicsContext.fillArc(x,
                y,
                drawingDia,
                drawingDia,
                startAngle,
                arcExtent,
                ArcType.ROUND);
    }

    private void fillTableWithoutPocketArea(TableMetrics metrics, double midPocketNoFill) {
        double oh = metrics.outerHeight * scale;
        double topPocketDownY = metrics.topY * scale;
        double fillHeight = metrics.innerHeight * scale;
        double leftPocketRightX = metrics.leftX * scale;  // 同时也是边上一溜的宽度
        double rightPocketLeftX = metrics.rightX * scale;
        double midPocketLeftX = (metrics.midX - metrics.midHoleRadius) * scale - midPocketNoFill;
        double midPocketRightX = (metrics.midX + metrics.midHoleRadius) * scale + midPocketNoFill;
        double fillWidth = midPocketLeftX - leftPocketRightX;

        graphicsContext.setFill(metrics.tableBorderColor);
        graphicsContext.fillRect(0, topPocketDownY, leftPocketRightX, fillHeight);  // 左边一溜
        graphicsContext.fillRect(leftPocketRightX, 0, midPocketLeftX - leftPocketRightX, oh);  // 左边中间
        graphicsContext.fillRect(midPocketRightX, 0, fillWidth, oh);  // 右边中间
        graphicsContext.fillRect(rightPocketLeftX, topPocketDownY, leftPocketRightX, fillHeight);
    }

    public void drawTable(GameHolder gameHolder) {
        TableMetrics values = gameValues.table;

        graphicsContext.setFill(GameView.GLOBAL_BACKGROUND);
        graphicsContext.fillRect(0, 0, canvasWidth, canvasHeight);

        double cornerHoleVisualRadius = getCornerHoleVisualRadiusUnscaled(values);
        double midHoleVisualRadius = getMidHoleVisualRadiusUnscaled(values);

        if (values.leatherPocket) {
            drawLeatherTable(values);
        } else {
            drawHardTable(values);
        }

        graphicsContext.setFill(HOLE_PAINT);
        // 视觉上的中袋
        drawHole(values.topMidHoleGraXY, midHoleVisualRadius);
        drawHole(values.botMidHoleGraXY, midHoleVisualRadius);
        // 视觉上的袋底
        drawHole(values.topLeftHoleGraXY, cornerHoleVisualRadius);
        drawHole(values.botLeftHoleGraXY, cornerHoleVisualRadius);
        drawHole(values.topRightHoleGraXY, cornerHoleVisualRadius);
        drawHole(values.botRightHoleGraXY, cornerHoleVisualRadius);

        double actualCornerHoleRadius = values.cornerHoleRadius * scale;
        graphicsContext.setFill(values.tableColor);  // 台泥/台布
        graphicsContext.fillRoundRect(  // 防止特别小的袋口遮不完台泥的边角
                canvasX(values.leftX - values.cushionClothWidth),
                canvasY(values.topY - values.cushionClothWidth),
                (values.innerWidth + values.cushionClothWidth * 2) * scale,
                (values.innerHeight + values.cushionClothWidth * 2) * scale,
                actualCornerHoleRadius,
                actualCornerHoleRadius);

//        // 袋口附近重力区域
//        graphicsContext.setFill(values.gravityAreaColor);
//        drawHole(values.topLeftHoleXY, values.cornerHoleRadius + values.holeExtraSlopeWidth);
//        drawHole(values.botLeftHoleXY, values.cornerHoleRadius + values.holeExtraSlopeWidth);
//        drawHole(values.topRightHoleXY, values.cornerHoleRadius + values.holeExtraSlopeWidth);
//        drawHole(values.botRightHoleXY, values.cornerHoleRadius + values.holeExtraSlopeWidth);
//        drawHole(values.topMidHoleXY, values.midHoleRadius + values.holeExtraSlopeWidth);
//        drawHole(values.botMidHoleXY, values.midHoleRadius + values.holeExtraSlopeWidth);

        graphicsContext.setStroke(LINE_PAINT);
        graphicsContext.setLineWidth(1.0);

//        Color cushion = values.tableColor.darker();

        // 库边
        graphicsContext.strokeLine(
                canvasX(values.leftCornerHoleAreaRightX),
                canvasY(values.topY),
                canvasX(values.midHoleAreaLeftX),
                canvasY(values.topY));
        graphicsContext.strokeLine(
                canvasX(values.leftCornerHoleAreaRightX),
                canvasY(values.botY),
                canvasX(values.midHoleAreaLeftX),
                canvasY(values.botY));
        graphicsContext.strokeLine(
                canvasX(values.midHoleAreaRightX),
                canvasY(values.topY),
                canvasX(values.rightCornerHoleAreaLeftX),
                canvasY(values.topY));
        graphicsContext.strokeLine(
                canvasX(values.midHoleAreaRightX),
                canvasY(values.botY),
                canvasX(values.rightCornerHoleAreaLeftX),
                canvasY(values.botY));
        graphicsContext.strokeLine(
                canvasX(values.leftX),
                canvasY(values.topCornerHoleAreaDownY),
                canvasX(values.leftX),
                canvasY(values.botCornerHoleAreaUpY));
        graphicsContext.strokeLine(
                canvasX(values.rightX),
                canvasY(values.topCornerHoleAreaDownY),
                canvasX(values.rightX),
                canvasY(values.botCornerHoleAreaUpY));

        // 袋口
        graphicsContext.setStroke(LINE_PAINT);
        drawMidHoleLinesArcs(values);
        drawCornerHoleLinesArcs(values);

        graphicsContext.setFill(HOLE_PAINT);
        // 实际底袋
        drawHole(values.topLeftHoleXY, values.cornerHoleRadius);
        drawHole(values.botLeftHoleXY, values.cornerHoleRadius);
        drawHole(values.topRightHoleXY, values.cornerHoleRadius);
        drawHole(values.botRightHoleXY, values.cornerHoleRadius);
        
        // 实际中袋
        drawHole(values.topMidHoleXY, values.midHoleRadius);
        drawHole(values.botMidHoleXY, values.midHoleRadius);

//        drawHoleOutLine(values.topLeftHoleXY, values.cornerHoleShownRadius + 10, 45.0 - values.cornerHoleOpenAngle);
//        drawHoleOutLine(values.botLeftHoleXY, values.cornerHoleShownRadius, 135.0);
//        drawHoleOutLine(values.topRightHoleXY, values.cornerHoleShownRadius, -45.0);
//        drawHoleOutLine(values.botRightHoleXY, values.cornerHoleShownRadius, -135.0);
//        drawHoleOutLine(values.topMidHoleXY, values.midHoleRadius, 0.0);
//        drawHoleOutLine(values.botMidHoleXY, values.midHoleRadius, 180.0);

        graphicsContext.setLineWidth(1.0);
        gameHolder.getTable().drawTableMarks(this, graphicsContext, scale);
//        if (replay != null) {
//            replay.table.drawTableMarks(this, graphicsContext, scale);
//        } else {
//            game.getGame().getTable().drawTableMarks(this, graphicsContext, scale);
//        }
    }

    private void drawHole(double[] realXY, double holeRadius) {
        graphicsContext.fillOval(canvasX(realXY[0] - holeRadius), canvasY(realXY[1] - holeRadius),
                holeRadius * 2 * scale, holeRadius * 2 * scale);
    }

    private void drawHoleOutLine(double[] realXY, double holeRadius, double startAngle) {
        double x = canvasX(realXY[0] - holeRadius);
        double y = canvasY(realXY[1] - holeRadius);
        graphicsContext.strokeArc(
                x,
                y,
                holeRadius * 2 * scale,
                holeRadius * 2 * scale,
                startAngle,
                180,
                ArcType.OPEN
        );
    }

    private void drawCornerHoleLinesArcs(TableMetrics values) {
        // 左上底袋
        drawCornerHoleArc(values.topLeftHoleSideArcXy, 225 + values.cornerHoleOpenAngle, values);
        drawCornerHoleArc(values.topLeftHoleEndArcXy, 0, values);

        // 左下底袋
        drawCornerHoleArc(values.botLeftHoleSideArcXy, 90, values);
        drawCornerHoleArc(values.botLeftHoleEndArcXy, 315 + values.cornerHoleOpenAngle, values);

        // 右上底袋
        drawCornerHoleArc(values.topRightHoleSideArcXy, 270, values);
        drawCornerHoleArc(values.topRightHoleEndArcXy, 135 + values.cornerHoleOpenAngle, values);

        // 右下底袋
        drawCornerHoleArc(values.botRightHoleSideArcXy, 45 + values.cornerHoleOpenAngle, values);
        drawCornerHoleArc(values.botRightHoleEndArcXy, 180, values);

        // 袋内直线
        for (double[][] line : values.allCornerLines) {
            drawHoleLine(line);
        }
    }

    private void drawHoleLine(double[][] lineRealXYs) {
        graphicsContext.strokeLine(
                canvasX(lineRealXYs[0][0]),
                canvasY(lineRealXYs[0][1]),
                canvasX(lineRealXYs[1][0]),
                canvasY(lineRealXYs[1][1])
        );
    }

    private void drawCornerHoleArc(double[] arcRealXY, double startAngle, TableMetrics values) {
        graphicsContext.strokeArc(
                canvasX(arcRealXY[0] - values.cornerArcRadius),
                canvasY(arcRealXY[1] - values.cornerArcRadius),
                values.cornerArcDiameter * scale,
                values.cornerArcDiameter * scale,
                startAngle,
                45 - values.cornerHoleOpenAngle,
                ArcType.OPEN);
    }

    private void drawMidHoleLinesArcs(TableMetrics values) {
        double arcDiameter = values.midArcRadius * 2 * scale;
        double x1 = canvasX(values.topMidHoleLeftArcXy[0] - values.midArcRadius);
        double x2 = canvasX(values.topMidHoleRightArcXy[0] - values.midArcRadius);
        double y1 = canvasY(values.topMidHoleLeftArcXy[1] - values.midArcRadius);
        double y2 = canvasY(values.botMidHoleLeftArcXy[1] - values.midArcRadius);

        double arcExtent = 90 - values.midHoleOpenAngle;
        graphicsContext.strokeArc(x1, y1, arcDiameter, arcDiameter, 270, arcExtent, ArcType.OPEN);
        graphicsContext.strokeArc(x2, y1, arcDiameter, arcDiameter, 180 + values.midHoleOpenAngle, arcExtent, ArcType.OPEN);
        graphicsContext.strokeArc(x1, y2, arcDiameter, arcDiameter, 0 + values.midHoleOpenAngle, arcExtent, ArcType.OPEN);
        graphicsContext.strokeArc(x2, y2, arcDiameter, arcDiameter, 90, arcExtent, ArcType.OPEN);

        // 袋内直线
//        if (values.isStraightHole()) {
        for (double[][] line : values.allMidHoleLines) {
            drawHoleLine(line);
        }
//        }
    }

    public void drawPredictedWhitePath(List<double[]> path) {
        if (path != null && !path.isEmpty()) {
            graphicsContext.setStroke(WHITE_PREDICTION_COLOR);
            double[] pos = path.get(0);
            for (int i = 1; i < path.size(); i++) {
                double[] dd = path.get(i);
                graphicsContext.strokeLine(canvasX(pos[0]), canvasY(pos[1]), canvasX(dd[0]), canvasY(dd[1]));
                pos = dd;
            }
        }
    }

    public void drawWhitePathSingle(Ball cueBall, WhitePrediction prediction) {
        graphicsContext.setStroke(cueBall.getColor());
        double lastX = canvasX(cueBall.getX());
        double lastY = canvasY(cueBall.getY());
        for (double[] pos : prediction.getWhitePath()) {
            double canvasX = canvasX(pos[0]);
            double canvasY = canvasY(pos[1]);
            graphicsContext.strokeLine(lastX, lastY, canvasX, canvasY);
            lastX = canvasX;
            lastY = canvasY;
        }
    }

    public void drawStoppedBalls(Table table,
                                 Ball[] allBalls,
                                 HashMap<Ball, double[]> positionsPot) {
        table.drawStoppedBalls(this, allBalls, positionsPot);
    }

    public GraphicsContext getGraphicsContext() {
        return graphicsContext;
    }

    public void drawWhiteStopArea(List<double[]> actualPoints) {
        graphicsContext.setStroke(GameView.WHITE.darker());

        List<double[]> canvasPoints = actualPoints.stream()
                .map(point -> new double[]{canvasX(point[0]), canvasY(point[1])})
                .collect(Collectors.toList());

        curvedPolygonDrawer.draw(canvasPoints, graphicsContext);
    }

    private void connectEndPoint(double[] pos1, double[] pos2, GraphicsContext gc) {
        gc.strokeLine(canvasX(pos1[0]), canvasY(pos1[1]), canvasX(pos2[0]), canvasY(pos2[1]));
    }
}