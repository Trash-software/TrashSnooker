package trashsoftware.trashSnooker.fxml;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcType;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import trashsoftware.trashSnooker.core.*;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

public class GameView implements Initializable {
    public static final Paint BACKGROUND = Color.GREEN;
    public static final Paint HOLE_PAINT = Color.BLACK;
    public static final Paint TABLE_PAINT = Color.SADDLEBROWN;
    public static final Paint WHITE = Color.WHITE;
    public static final Paint BLACK = Color.BLACK;
    public static final Paint CUE_POINT = Color.RED;

    public double scale = 0.32;
    @FXML
    Canvas gameCanvas;
    @FXML
    Canvas ballCanvas;
    @FXML
    Slider powerSlider;
    @FXML
    Label powerLabel;
    @FXML
    Button cueButton;
    @FXML
    Label singlePoleLabel;
    @FXML
    Canvas singlePoleCanvas;
    @FXML
    Label player1ScoreLabel, player2ScoreLabel;
    @FXML
    Canvas player1TarCanvas, player2TarCanvas;
    @FXML
    MenuItem withdrawMenu;
    private double canvasWidth = Values.SNOOKER_OUTER_WIDTH * scale;
    private double innerWidth = Values.SNOOKER_INNER_WIDTH * scale;
    private double canvasHeight = Values.SNOOKER_OUTER_HEIGHT * scale;
    private double innerHeight = Values.SNOOKER_INNER_HEIGHT * scale;
    private double topLeftX = (canvasWidth - innerWidth) / 2;  // 桌面左上角
    private double topLeftY = (canvasHeight - innerHeight) / 2;
    private double halfY = topLeftY + innerHeight / 2;
    private double cornerHoleDia = Values.CORNER_HOLE_DIAMETER * scale;
    private double midHoleDia = Values.MID_HOLE_DIAMETER * scale;
    private double innerBorder = Values.CORNER_HOLE_TANGENT * scale;
    private double ballDiameter = Values.BALL_DIAMETER * scale;
    private double ballRadius = ballDiameter / 2;
    private double cornerArcDiameter = Values.CORNER_ARC_DIAMETER * scale;
    private double cueCanvasWH = 80.0;
    private double cueAreaRadius = 36.0;
    private double cueRadius = 4.0;
    private GraphicsContext graphicsContext;
    private GraphicsContext ballCanvasGc;
    private Stage stage;

    private SnookerGame game;
    private double frameTimeMs = 20.0;
    //    private double cursorX, cursorY;
    private double cursorDirectionUnitX, cursorDirectionUnitY;
    private double mouseX, mouseY;
    private double cuePointX, cuePointY;  // 杆法的击球点
    private boolean isDragging;
    private double lastDragAngle;
    private Timeline timeline;

    private double minRealPredictLength = 400.0;
    private double maxRealPredictLength = 1200.0;
    private double minPredictLengthPotDt = 2000.0;
    private double maxPredictLengthPotDt = 500.0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ballCanvas.setWidth(cueCanvasWH);
        ballCanvas.setHeight(cueCanvasWH);

        player1TarCanvas.setHeight(ballDiameter * 1.2);
        player1TarCanvas.setWidth(ballDiameter * 1.2);
        player2TarCanvas.setHeight(ballDiameter * 1.2);
        player2TarCanvas.setWidth(ballDiameter * 1.2);
        singlePoleCanvas.setHeight(ballDiameter * 1.2);
        singlePoleCanvas.setWidth(ballDiameter * 7 * 1.2);

        singlePoleCanvas.getGraphicsContext2D().setTextAlign(TextAlignment.CENTER);
        singlePoleCanvas.getGraphicsContext2D().setStroke(WHITE);
        player1TarCanvas.getGraphicsContext2D().setTextAlign(TextAlignment.CENTER);
        player1TarCanvas.getGraphicsContext2D().setStroke(WHITE);
        player2TarCanvas.getGraphicsContext2D().setTextAlign(TextAlignment.CENTER);
        player2TarCanvas.getGraphicsContext2D().setStroke(WHITE);

        graphicsContext = gameCanvas.getGraphicsContext2D();
        ballCanvasGc = ballCanvas.getGraphicsContext2D();
        addListeners();
        restoreCuePoint();

        startGame();
        setupCanvas();
        startAnimation();

        drawTargetBoard();
    }

    public void setStage(Stage stage) {
        this.stage = stage;

        this.stage.setOnHidden(e -> {
            game.quitGame();
            timeline.stop();
        });
    }

    public void finishCue(Player cuePlayer) {
        drawScoreBoard(cuePlayer);
        drawTargetBoard();
        restoreCuePoint();
        setButtonsCueEnd();

        if (game.isEnded()) {
            showEndMessage();
        } else if (game.canReposition()) {
            askReposition();
        }
    }

    private void showEndMessage() {
        Platform.runLater(() ->
                AlertShower.showInfo(stage,
                String.format("玩家1  %d : %d  玩家2", game.getPlayer1().getScore(), game.getPlayer2().getScore()),
                String.format("%s%d胜利。", "玩家", game.getWiningPlayer().getNumber())));
    }

    private void askReposition() {
        Platform.runLater(() -> {
            if (AlertShower.askConfirmation(stage, "是否复位？", "对方犯规")) {
                game.reposition();
                drawScoreBoard(game.getNextCuePlayer());
                drawTargetBoard();
            } else {
                if (game.getWhiteBall().isPotted()) game.setBallInHand();
            }
        });
    }

    private void onCanvasClicked(MouseEvent mouseEvent) {
        if (isDragging) {
            onDragEnd(mouseEvent);
            return;
        }

        if (mouseEvent.getClickCount() == 1) {
            onSingleClick(mouseEvent);
        }
    }

    private void setCuePoint(double x, double y) {
        if (Algebra.distanceToPoint(x, y, cueCanvasWH / 2, cueCanvasWH / 2) < cueAreaRadius - cueRadius) {
            cuePointX = x;
            cuePointY = y;
        }
    }

    private void onCueBallCanvasClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            setCuePoint(mouseEvent.getX(), mouseEvent.getY());
        } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            restoreCuePoint();
        }
    }

    private void onCueBallCanvasDragged(MouseEvent mouseEvent) {
        setCuePoint(mouseEvent.getX(), mouseEvent.getY());
    }

    private void onSingleClick(MouseEvent mouseEvent) {
        System.out.println("Clicked!");
        if (game.getWhiteBall().isPotted()) {
            game.placeWhiteBall(realX(mouseEvent.getX()), realY(mouseEvent.getY()));
        } else if (!game.isMoving()) {
            Ball whiteBall = game.getWhiteBall();
            double[] unit = Algebra.unitVector(
                    new double[]{
                            realX(mouseEvent.getX()) - whiteBall.getX(),
                            realY(mouseEvent.getY()) - whiteBall.getY()
                    });
            cursorDirectionUnitX = unit[0];
            cursorDirectionUnitY = unit[1];
        }
    }

    private void onMouseMoved(MouseEvent mouseEvent) {
        mouseX = mouseEvent.getX();
        mouseY = mouseEvent.getY();
    }

    private void onDragStarted(MouseEvent mouseEvent) {
        Ball white = game.getWhiteBall();
        if (white.isPotted()) return;
        isDragging = true;
        double xDiffToWhite = realX(mouseEvent.getX()) - white.getX();
        double yDiffToWhite = realY(mouseEvent.getY()) - white.getY();
        lastDragAngle = Algebra.thetaOf(new double[]{xDiffToWhite, yDiffToWhite});
        if (cursorDirectionUnitX == 0.0 && cursorDirectionUnitY == 0.0) {
            double[] unitVec = Algebra.unitVector(xDiffToWhite, yDiffToWhite);
            cursorDirectionUnitX = unitVec[0];
            cursorDirectionUnitY = unitVec[1];
        }
        stage.getScene().setCursor(Cursor.CLOSED_HAND);
    }

    private void onDragging(MouseEvent mouseEvent) {
        if (!isDragging) {
            return;
        }
        Ball white = game.getWhiteBall();
        if (white.isPotted()) return;
        double xDiffToWhite = realX(mouseEvent.getX()) - white.getX();
        double yDiffToWhite = realY(mouseEvent.getY()) - white.getY();
        double distanceToWhite = Math.hypot(xDiffToWhite, yDiffToWhite);  // 光标离白球越远，移动越慢
        double currentAngle =
                Algebra.thetaOf(new double[]{xDiffToWhite, yDiffToWhite});
        double changedAngle =
                Algebra.normalizeAngle
                        (currentAngle - lastDragAngle) / (distanceToWhite / 500.0);

        double aimingAngle = Algebra.thetaOf(new double[]{cursorDirectionUnitX, cursorDirectionUnitY});
        double resultAngle = aimingAngle + changedAngle;
        double[] newUnitVector = Algebra.angleToUnitVector(resultAngle);
        cursorDirectionUnitX = newUnitVector[0];
        cursorDirectionUnitY = newUnitVector[1];

        lastDragAngle = currentAngle;

//        System.out.printf("changed:%f, cur:%f, xDiff:%f, yDiff:%f\n",
//                Math.toDegrees(changedAngle),
//                Math.toDegrees(currentAngle),
//                xDiffToWhite,
//                yDiffToWhite);
    }

    private void onDragEnd(MouseEvent mouseEvent) {
        isDragging = false;
        lastDragAngle = 0.0;
        stage.getScene().setCursor(Cursor.DEFAULT);
    }

    private void startGame() {
        game = new SnookerGame(this);
    }

    @FXML
    void terminateAction() {
        game.forcedTerminate();
    }

    @FXML
    void testAction() {
        game.collisionTest();
    }

    @FXML
    void tieTestAction() {
        game.tieTest();
        drawTargetBoard();
        drawScoreBoard(game.getNextCuePlayer());
    }

    @FXML
    void clearRedBallsAction() {
        game.clearRedBallsTest();
        drawTargetBoard();
    }

    @FXML
    void withdrawAction() {
        Player curPlayer = game.getNextCuePlayer();
        int diff = game.getScoreDiff(curPlayer);
        String behindText = diff <= 0 ? "落后" : "领先";
        if (AlertShower.askConfirmation(
                stage,
                String.format("%s%d分，台面剩余%d分，真的要认输吗？", behindText, Math.abs(diff), game.getRemainingScore()),
                String.format("%s%d, 确认要认输吗？", "玩家", curPlayer.getNumber()))) {
            game.withdraw(curPlayer);
            showEndMessage();
        }
    }

    @FXML
    void cueAction() {
        if (game.isEnded()) return;
        if (cursorDirectionUnitX == 0.0 && cursorDirectionUnitY == 0.0) return;

        setButtonsCueStart();

        double power = Math.max(powerSlider.getValue(), 0.01);
        double vx = cursorDirectionUnitX * power * Values.MAX_POWER_SPEED / 100.0;  // 常量，最大力白球速度：3米/秒
        double vy = cursorDirectionUnitY * power * Values.MAX_POWER_SPEED / 100.0;

        double[] spins = calculateSpins(vx, vy);

        game.cue(vx, vy, spins[0], spins[1], spins[2]);

        cursorDirectionUnitX = 0.0;
        cursorDirectionUnitY = 0.0;
    }

    @FXML
    void newGameAction() {
        Platform.runLater(() -> {
            if (AlertShower.askConfirmation(stage, "真的要开始新游戏吗？", "请确认")) {
                startGame();
                drawTargetBoard();
                drawScoreBoard(game.getNextCuePlayer());
            }
        });
    }

    @FXML
    void settingsAction() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("settingsView.fxml")
        );
        Parent root = loader.load();

        Stage newStage = new Stage();
        newStage.initOwner(stage);

        Scene scene = new Scene(root);
        newStage.setScene(scene);

        SettingsView view = loader.getController();
        view.setup(newStage, this);

        newStage.show();
    }

    void setDifficulty(SettingsView.Difficulty difficulty) {
        if (difficulty == SettingsView.Difficulty.EASY) {
            minRealPredictLength = 800.0;
            maxRealPredictLength = 2400.0;
        } else if (difficulty == SettingsView.Difficulty.MEDIUM) {
            minRealPredictLength = 400.0;
            maxRealPredictLength = 1200.0;
        } else if (difficulty == SettingsView.Difficulty.HARD) {
            minRealPredictLength = 200.0;
            maxRealPredictLength = 600.0;
        }
    }

    private void setButtonsCueStart() {
        withdrawMenu.setDisable(true);
    }

    private void setButtonsCueEnd() {
        withdrawMenu.setDisable(false);
    }

    private double[] calculateSpins(double vx, double vy) {
        double speed = Math.hypot(vx, vy);

        double frontBackSpin = cueCanvasWH / 2 - cuePointY;  // 高杆正，低杆负
        double leftRightSpin = cuePointX - cueCanvasWH / 2;  // 右塞正（逆时针），左塞负

        double side = (speed / Values.MAX_POWER_SPEED) * (leftRightSpin / cueAreaRadius) * Values.MAX_SIDE_SPIN_SPEED;
        // 旋转产生的总目标速度
        double spinSpeed = (speed / Values.MAX_POWER_SPEED) * (frontBackSpin / cueAreaRadius) * Values.MAX_SPIN_SPEED;
        double spinX = vx * (spinSpeed / speed);
        double spinY = vy * (spinSpeed / speed);
//        System.out.printf("x %f, y %f, total %f, side %f\n", spinX, spinY, spinSpeed, side);

        return new double[]{spinX, spinY, side};
    }

    private void restoreCuePoint() {
        cuePointX = cueCanvasWH / 2;
        cuePointY = cueCanvasWH / 2;
    }

    private void startAnimation() {
        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        KeyFrame keyFrame = new KeyFrame(new Duration(frameTimeMs), e -> oneFrame());
        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    private void oneFrame() {
        draw();
        drawCueBall();
    }

    private void setupCanvas() {
        gameCanvas.setWidth(canvasWidth);
        gameCanvas.setHeight(canvasHeight);
    }

    private void addListeners() {
        powerSlider.valueProperty().addListener(((observable, oldValue, newValue) ->
                powerLabel.setText(String.valueOf(Math.round(newValue.doubleValue())))));

        powerSlider.setValue(50.0);

        gameCanvas.setOnMouseClicked(this::onCanvasClicked);
        gameCanvas.setOnDragDetected(this::onDragStarted);
        gameCanvas.setOnMouseDragged(this::onDragging);
        gameCanvas.setOnMouseMoved(this::onMouseMoved);

        ballCanvas.setOnMouseClicked(this::onCueBallCanvasClicked);
        ballCanvas.setOnMouseDragged(this::onCueBallCanvasDragged);
    }

    private void drawPottedWhiteBall() {
        if (!game.isMoving() && game.isBallInHand()) {
            double x = realX(mouseX);
            if (x < Values.LEFT_X + Values.BALL_RADIUS) x = Values.LEFT_X + Values.BALL_RADIUS;
            else if (x >= Values.RIGHT_X - Values.BALL_RADIUS) x = Values.RIGHT_X - Values.BALL_RADIUS;

            double y = realY(mouseY);
            if (y < Values.TOP_Y + Values.BALL_RADIUS) y = Values.TOP_Y + Values.BALL_RADIUS;
            else if (y >= Values.BOT_Y - Values.BALL_RADIUS) y = Values.BOT_Y - Values.BALL_RADIUS;

            game.forcedDrawWhiteBall(
                    x,
                    y,
                    graphicsContext,
                    scale
            );
        }
    }

    private void drawTable() {
//        graphicsContext.setLineWidth(1.0);
        graphicsContext.setFill(TABLE_PAINT);
        graphicsContext.fillRoundRect(0, 0, canvasWidth, canvasHeight, 20.0, 20.0);
        graphicsContext.setFill(BACKGROUND);
        graphicsContext.fillRect(
                canvasX(Values.LEFT_X - Values.CORNER_HOLE_TANGENT),
                canvasY(Values.TOP_Y - Values.CORNER_HOLE_TANGENT),
                (Values.SNOOKER_INNER_WIDTH + Values.CORNER_HOLE_TANGENT * 2) * scale,
                (Values.SNOOKER_INNER_HEIGHT + Values.CORNER_HOLE_TANGENT * 2) * scale);
        graphicsContext.setStroke(BLACK);

        // 库边
        graphicsContext.strokeLine(
                canvasX(Values.LEFT_CORNER_HOLE_AREA_RIGHT_X),
                canvasY(Values.TOP_Y),
                canvasX(Values.MID_HOLE_AREA_LEFT_X),
                canvasY(Values.TOP_Y));
        graphicsContext.strokeLine(
                canvasX(Values.LEFT_CORNER_HOLE_AREA_RIGHT_X),
                canvasY(Values.BOT_Y),
                canvasX(Values.MID_HOLE_AREA_LEFT_X),
                canvasY(Values.BOT_Y));
        graphicsContext.strokeLine(
                canvasX(Values.MID_HOLE_AREA_RIGHT_X),
                canvasY(Values.TOP_Y),
                canvasX(Values.RIGHT_CORNER_HOLE_AREA_LEFT_X),
                canvasY(Values.TOP_Y));
        graphicsContext.strokeLine(
                canvasX(Values.MID_HOLE_AREA_RIGHT_X),
                canvasY(Values.BOT_Y),
                canvasX(Values.RIGHT_CORNER_HOLE_AREA_LEFT_X),
                canvasY(Values.BOT_Y));
        graphicsContext.strokeLine(
                canvasX(Values.LEFT_X),
                canvasY(Values.TOP_CORNER_HOLE_AREA_DOWN_Y),
                canvasX(Values.LEFT_X),
                canvasY(Values.BOT_CORNER_HOLE_AREA_UP_Y));
        graphicsContext.strokeLine(
                canvasX(Values.RIGHT_X),
                canvasY(Values.TOP_CORNER_HOLE_AREA_DOWN_Y),
                canvasX(Values.RIGHT_X),
                canvasY(Values.BOT_CORNER_HOLE_AREA_UP_Y));

        // 开球线
        double breakLineX = canvasX(Values.BREAK_LINE_X);
        graphicsContext.setStroke(WHITE);
        graphicsContext.strokeLine(breakLineX, topLeftY, breakLineX, topLeftY + innerHeight);

        // 开球半圆
        double breakArcRadius = Values.BREAK_ARC_RADIUS * scale;
        graphicsContext.strokeArc(breakLineX - breakArcRadius, halfY - breakArcRadius,
                breakArcRadius * 2, breakArcRadius * 2,
                90.0, 180.0,
                ArcType.OPEN);

        // 置球点
        drawBallPoints();

        // 袋口
        graphicsContext.setStroke(BLACK);
        drawMidHoleArcs();
        drawCornerHoleLinesArcs();

        graphicsContext.setFill(HOLE_PAINT);
        drawHole(Values.TOP_LEFT_HOLE_XY, Values.CORNER_HOLE_RADIUS);
        drawHole(Values.BOT_LEFT_HOLE_XY, Values.CORNER_HOLE_RADIUS);
        drawHole(Values.TOP_RIGHT_HOLE_XY, Values.CORNER_HOLE_RADIUS);
        drawHole(Values.BOT_RIGHT_HOLE_XY, Values.CORNER_HOLE_RADIUS);
        drawHole(Values.TOP_MID_HOLE_XY, Values.MID_HOLE_RADIUS);
        drawHole(Values.BOT_MID_HOLE_XY, Values.MID_HOLE_RADIUS);
    }

    private void drawBallPoints() {
        graphicsContext.setFill(WHITE);
        double pointRadius = 2.0;
        double pointDiameter = pointRadius * 2;
        for (double[] xy : Values.POINTS_RANK_HIGH_TO_LOW) {
            graphicsContext.fillOval(canvasX(xy[0]) - pointRadius, canvasY(xy[1]) - pointRadius,
                    pointDiameter, pointDiameter);
        }
    }

    private void drawHole(double[] realXY, double holeRadius) {
        graphicsContext.fillOval(canvasX(realXY[0] - holeRadius), canvasY(realXY[1] - holeRadius),
                holeRadius * 2 * scale, holeRadius * 2 * scale);
    }

    private void drawCornerHoleLinesArcs() {
        // 左上底袋
        drawCornerHoleArc(Values.TOP_LEFT_HOLE_SIDE_ARC_XY, 225);
        drawCornerHoleArc(Values.TOP_LEFT_HOLE_END_ARC_XY, 0);

        // 左下底袋
        drawCornerHoleArc(Values.BOT_LEFT_HOLE_SIDE_ARC_XY, 90);
        drawCornerHoleArc(Values.BOT_LEFT_HOLE_END_ARC_XY, 315);

        // 右上底袋
        drawCornerHoleArc(Values.TOP_RIGHT_HOLE_SIDE_ARC_XY, 270);
        drawCornerHoleArc(Values.TOP_RIGHT_HOLE_END_ARC_XY, 135);

        // 右下底袋
        drawCornerHoleArc(Values.BOT_RIGHT_HOLE_SIDE_ARC_XY, 45);
        drawCornerHoleArc(Values.BOT_RIGHT_HOLE_END_ARC_XY, 180);

        // 袋内直线
        for (double[][] line : Values.ALL_CORNER_LINES) {
            drawCornerHoleLine(line);
        }
    }

    private void drawCornerHoleLine(double[][] lineRealXYs) {
        graphicsContext.strokeLine(
                canvasX(lineRealXYs[0][0]),
                canvasY(lineRealXYs[0][1]),
                canvasX(lineRealXYs[1][0]),
                canvasY(lineRealXYs[1][1])
        );
    }

    private void drawCornerHoleArc(double[] arcRealXY, double startAngle) {
        graphicsContext.strokeArc(
                canvasX(arcRealXY[0] - Values.CORNER_ARC_RADIUS),
                canvasY(arcRealXY[1] - Values.CORNER_ARC_RADIUS),
                cornerArcDiameter,
                cornerArcDiameter,
                startAngle,
                45,
                ArcType.OPEN);
    }

    private void drawMidHoleArcs() {
        double arcDiameter = Values.MID_ARC_RADIUS * 2 * scale;
        double x1 = canvasX(Values.TOP_MID_HOLE_XY[0] - Values.MID_ARC_RADIUS * 2 - Values.MID_HOLE_RADIUS);
        double x2 = canvasX(Values.BOT_MID_HOLE_XY[0] + Values.MID_ARC_RADIUS);
        double y1 = canvasY(Values.TOP_MID_HOLE_XY[1] - Values.MID_ARC_RADIUS);
        double y2 = canvasY(Values.BOT_MID_HOLE_XY[1] - Values.MID_ARC_RADIUS);
        graphicsContext.strokeArc(x1, y1, arcDiameter, arcDiameter, 270, 90, ArcType.OPEN);
        graphicsContext.strokeArc(x2, y1, arcDiameter, arcDiameter, 180, 90, ArcType.OPEN);
        graphicsContext.strokeArc(x1, y2, arcDiameter, arcDiameter, 0, 90, ArcType.OPEN);
        graphicsContext.strokeArc(x2, y2, arcDiameter, arcDiameter, 90, 90, ArcType.OPEN);
    }

    private void drawBalls() {
        game.drawBalls(graphicsContext, scale);
    }

    private void drawScoreBoard(Player cuePlayer) {
        Platform.runLater(() -> {
            player1ScoreLabel.setText(String.valueOf(game.getPlayer1().getScore()));
            player2ScoreLabel.setText(String.valueOf(game.getPlayer2().getScore()));
            drawSinglePoleBalls(cuePlayer.getSinglePole());
            singlePoleLabel.setText(String.valueOf(cuePlayer.getSinglePoleScore()));
        });
    }

    private void drawTargetBoard() {
        Platform.runLater(() -> {
            if (game.getNextCuePlayer().getNumber() == 1) {
                drawTargetBall(player1TarCanvas, game.getCurrentTarget(), game.isDoingFreeBall());
                wipeCanvas(player2TarCanvas);
            } else {
                drawTargetBall(player2TarCanvas, game.getCurrentTarget(), game.isDoingFreeBall());
                wipeCanvas(player1TarCanvas);
            }
        });
    }

    private void drawSinglePoleBalls(TreeMap<Ball, Integer> singlePoleBalls) {
        GraphicsContext gc = singlePoleCanvas.getGraphicsContext2D();
        gc.setFill(WHITE);
        gc.fillRect(0, 0, singlePoleCanvas.getWidth(), singlePoleCanvas.getHeight());
        double x = 0;
        double y = ballDiameter * 0.1;
        double textY = ballDiameter * 0.8;
        for (Map.Entry<Ball, Integer> ballCount : singlePoleBalls.entrySet()) {
            gc.setFill(ballCount.getKey().getColor());
            gc.fillOval(x + ballDiameter * 0.1, y, ballDiameter, ballDiameter);
            gc.strokeText(String.valueOf(ballCount.getValue()), x + ballDiameter * 0.6, textY);
            x += ballDiameter * 1.2;
        }
    }

    private void wipeCanvas(Canvas canvas) {
        canvas.getGraphicsContext2D().setFill(WHITE);
        canvas.getGraphicsContext2D().fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void drawTargetBall(Canvas canvas, int value, boolean isFreeBall) {
        if (value == 0) {
            if (isFreeBall) throw new RuntimeException("自由球打彩球？你他妈懂不懂规则？");
            drawTargetColoredBall(canvas);
        } else {
            Color color = Values.getColorOfTarget(value);
            canvas.getGraphicsContext2D().setFill(color);
            canvas.getGraphicsContext2D().fillOval(ballDiameter * 0.1, ballDiameter * 0.1, ballDiameter, ballDiameter);
            if (isFreeBall)
                canvas.getGraphicsContext2D().strokeText("F", ballDiameter * 0.6, ballDiameter * 0.8);
        }
    }

    private void drawTargetColoredBall(Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double x = ballDiameter * 0.1;
        double y = ballDiameter * 0.1;

        double deg = 0.0;
        for (Color color : Values.COLORED_LOW_TO_HIGH) {
            gc.setFill(color);
            gc.fillArc(x, y, ballDiameter, ballDiameter, deg, 60.0, ArcType.ROUND);
            deg += 60.0;
        }
    }

    private void drawCursor() {
        if (game.isEnded()) return;
        if (game.isMoving()) return;
        if (cursorDirectionUnitX == 0.0 && cursorDirectionUnitY == 0.0) return;

        Ball whiteBall = game.getWhiteBall();
        double whiteX = canvasX(whiteBall.getX());
        double whiteY = canvasY(whiteBall.getY());

        if (whiteBall.isPotted()) return;

//        double xDiff = cursorX - whiteX;
//        double yDiff = cursorY - whiteY;
//        double mag = Math.hypot(cursorDirectionUnitX, cursorDirectionUnitY);
//        double unitX = cursorDirectionUnitX / mag;
//        double unitY = cursorDirectionUnitY / mag;
        PredictedPos predictedPos = game.getPredictedHitBall(cursorDirectionUnitX, cursorDirectionUnitY);

        graphicsContext.setStroke(WHITE);
        if (predictedPos == null) {
            graphicsContext.strokeLine(whiteX, whiteY,
                    whiteX + cursorDirectionUnitX * 1000.0, whiteY + cursorDirectionUnitY * 1000.0);
        } else {
            double[] targetPos = predictedPos.getPredictedWhitePos();
            double[] ballPos = new double[]{predictedPos.getTargetBall().getX(), predictedPos.getTargetBall().getY()};
            double tarCanvasX = canvasX(targetPos[0]);
            double tarCanvasY = canvasY(targetPos[1]);
            graphicsContext.strokeLine(whiteX, whiteY, tarCanvasX, tarCanvasY);
            graphicsContext.strokeOval(tarCanvasX - ballRadius, tarCanvasY - ballRadius,
                    ballDiameter, ballDiameter);  // 绘制预测撞击点的白球

            double potDt = Algebra.distanceToPoint(targetPos[0], targetPos[1], whiteBall.getX(), whiteBall.getY());
            // 白球行进距离越长，预测线越短
            double predictLineTotalLen;
            if (potDt >= minPredictLengthPotDt) predictLineTotalLen = minRealPredictLength;
            else if (potDt < maxPredictLengthPotDt) predictLineTotalLen = maxRealPredictLength;
            else {
                double potDtRange = minPredictLengthPotDt - maxPredictLengthPotDt;
                double lineLengthRange = maxRealPredictLength - minRealPredictLength;
                double potDtInRange = (potDt - maxPredictLengthPotDt) / potDtRange;
                predictLineTotalLen = maxRealPredictLength - potDtInRange * lineLengthRange;
            }

            double whiteUnitX = (targetPos[0] - whiteBall.getX()) / potDt;
            double whiteUnitY = (targetPos[1] - whiteBall.getY()) / potDt;
            double ang = (targetPos[0] - ballPos[0]) / (targetPos[1] - ballPos[1]);
            double predictTarY = (ang * whiteUnitX + whiteUnitY) / (ang * ang + 1);
            double predictTarX = ang * predictTarY;

            double predictWhiteX = whiteUnitX - predictTarX;
            double predictWhiteY = whiteUnitY - predictTarY;

            double predictTarMag = Math.hypot(predictTarX, predictTarY);
            double predictWhiteMag = Math.hypot(predictWhiteX, predictWhiteY);
            double totalMag = predictTarMag + predictWhiteMag;
            double multiplier = predictLineTotalLen / totalMag;

            double lineX = predictTarX * multiplier * scale;
            double lineY = predictTarY * multiplier * scale;
            double whiteLineX = predictWhiteX * multiplier * scale;
            double whiteLineY = predictWhiteY * multiplier * scale;

            graphicsContext.strokeLine(tarCanvasX, tarCanvasY, tarCanvasX + whiteLineX, tarCanvasY + whiteLineY);

            graphicsContext.setStroke(predictedPos.getTargetBall().getColor().brighter().brighter());
            graphicsContext.strokeLine(tarCanvasX, tarCanvasY, tarCanvasX + lineX, tarCanvasY + lineY);
        }
    }

    private void draw() {
        drawTable();
        drawBalls();
        drawCursor();
        drawPottedWhiteBall();
    }

    private void drawCueBall() {
        double cueAreaDia = cueAreaRadius * 2;
        double padding = (cueCanvasWH - cueAreaDia) / 2;
        ballCanvasGc.setStroke(BLACK);
        ballCanvasGc.setFill(Values.WHITE);
        ballCanvasGc.fillOval(padding, padding, cueAreaDia, cueAreaDia);
        ballCanvasGc.strokeOval(padding, padding, cueAreaDia, cueAreaDia);

        ballCanvasGc.setFill(CUE_POINT);
        ballCanvasGc.fillOval(cuePointX - cueRadius, cuePointY - cueRadius, cueRadius * 2, cueRadius * 2);
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
}
