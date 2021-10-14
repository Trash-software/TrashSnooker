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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallGame;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.core.snooker.AbstractSnookerGame;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class GameView implements Initializable {
    public static final Color HOLE_PAINT = Color.DIMGRAY.darker().darker().darker();
    public static final Color WHITE = Color.WHITE;
    public static final Color BLACK = Color.BLACK;
    public static final Color CUE_POINT = Color.RED;
    public static final Color INTENT_CUE_POINT = Color.NAVY;
    public static final Color CUE_TIP_COLOR = Color.LIGHTSEAGREEN;

    public static final Font POOL_NUMBER_FONT = new Font(8.0);

    public static final double HAND_DT_TO_MAX_PULL = 30.0;

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
    Label player1Label, player2Label, player1ScoreLabel, player2ScoreLabel;
    @FXML
    Canvas player1TarCanvas, player2TarCanvas;
    @FXML
    MenuItem withdrawMenu, replaceBallInHandMenu, letOtherPlayMenu;
    private double canvasWidth;
    private double canvasHeight;
    private double innerHeight;
    private double topLeftY;
    private double ballDiameter;
    private double ballRadius;
    private double cornerArcDiameter;
    private double cueCanvasWH = 80.0;
    private double cueAreaRadius = 36.0;
    private double cueRadius = 4.0;
    private GraphicsContext graphicsContext;
    private GraphicsContext ballCanvasGc;
    private Stage stage;

    private InGamePlayer player1;
    private InGamePlayer player2;
    private Game game;
    private GameType gameType;

    private double frameTimeMs = 20.0;
    //    private double cursorX, cursorY;
    private double cursorDirectionUnitX, cursorDirectionUnitY;
    private double targetPredictionUnitX, targetPredictionUnitY;
    private Ball predictedTargetBall;
    private PotAttempt currentAttempt;

    private double mouseX, mouseY;
    private double cuePointX, cuePointY;  // 杆法的击球点
    private double intentCuePointX = -1, intentCuePointY = -1;  // 计划的杆法击球点
    private CueAnimationPlayer cueAnimationPlayer;

    private boolean isDragging;
    private double lastDragAngle;
    private Timeline timeline;

    private double minRealPredictLength = 400.0;
    private double maxRealPredictLength = 1200.0;
    private double minPredictLengthPotDt = 2000.0;
    private double maxPredictLengthPotDt = 500.0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        graphicsContext = gameCanvas.getGraphicsContext2D();
        ballCanvasGc = ballCanvas.getGraphicsContext2D();

        graphicsContext.setTextAlign(TextAlignment.CENTER);

        addListeners();
        restoreCuePoint();
    }

    private void generateScales() {
        GameValues values = game.getGameValues();
        canvasWidth = values.outerWidth * scale;
        canvasHeight = values.outerHeight * scale;
        innerHeight = values.innerHeight * scale;

        topLeftY = (canvasHeight - innerHeight) / 2;
        ballDiameter = values.ballDiameter * scale;
        ballRadius = ballDiameter / 2;
        cornerArcDiameter = values.cornerArcDiameter * scale;

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

        setupCanvas();
        startAnimation();
        drawTargetBoard();
    }

    public void setup(Stage stage, GameType gameType, InGamePlayer player1, InGamePlayer player2) {
        this.stage = stage;
        this.gameType = gameType;
        this.player1 = player1;
        this.player2 = player2;

        player1Label.setText(player1.getPlayerPerson().getName());
        player2Label.setText(player2.getPlayerPerson().getName());

        startGame();

        setupPowerSlider();

        generateScales();

        this.stage.setOnHidden(e -> {
            game.quitGame();
            timeline.stop();
        });
    }

    public void finishCue(Player cuePlayer, boolean potSuccess) {
//        updateCuePlayerSinglePole(cuePlayer);
        drawScoreBoard(cuePlayer);
        drawTargetBoard();
        restoreCuePoint();
        Platform.runLater(() -> powerSlider.setValue(40.0));
        setButtonsCueEnd();

        if (currentAttempt != null) {
            cuePlayer.getInGamePlayer().getPersonRecord().potAttempt(currentAttempt, potSuccess);
        }

        if (game.isEnded()) {
            endGame();
//            Recorder.save();
        } else if ((game instanceof AbstractSnookerGame) &&
                ((AbstractSnookerGame) game).canReposition()) {
            askReposition();
        }
    }

    private void endGame() {
        Player wonPlayer = game.getWiningPlayer();
        Player lostPlayer = wonPlayer == game.getPlayer1() ? game.getPlayer2() : game.getPlayer1();

        game.getPlayer1().getInGamePlayer().getPersonRecord()
                .generalEndGame(gameType, game.getPlayer1());
        game.getPlayer2().getInGamePlayer().getPersonRecord()
                .generalEndGame(gameType, game.getPlayer2());

        wonPlayer.getInGamePlayer().getPersonRecord()
                .wonAgainstOpponent(gameType, wonPlayer, lostPlayer.getPlayerPerson().getName());
        lostPlayer.getInGamePlayer().getPersonRecord()
                .lostAgainstOpponent(gameType, wonPlayer.getPlayerPerson().getName());

        game.getPlayer1().getInGamePlayer().getPersonRecord().writeToFile();
        game.getPlayer2().getInGamePlayer().getPersonRecord().writeToFile();

        Platform.runLater(() ->
                AlertShower.showInfo(stage,
                        String.format("%s  %d : %d  %s",
                                game.getPlayer1().getPlayerPerson().getName(),
                                game.getPlayer1().getScore(),
                                game.getPlayer2().getScore(),
                                game.getPlayer2().getPlayerPerson().getName()),
                        String.format("%s 胜利。", game.getWiningPlayer().getPlayerPerson().getName())));
    }

    private void askReposition() {
        Platform.runLater(() -> {
            if (AlertShower.askConfirmation(stage, "是否复位？", "对方犯规")) {
                ((AbstractSnookerGame) game).reposition();
                drawScoreBoard(game.getCuingPlayer());
                drawTargetBoard();
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
        if (game.getCueBall().isPotted()) {
            game.placeWhiteBall(realX(mouseEvent.getX()), realY(mouseEvent.getY()));
        } else if (!game.isMoving()) {
            Ball whiteBall = game.getCueBall();
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
        Ball white = game.getCueBall();
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
        Ball white = game.getCueBall();
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
        GameSettings gameSettings = new GameSettings.Builder()
                .player1Breaks(true)
                .players(player1, player2)
                .build();

        game = Game.createGame(this, gameSettings, gameType);
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
        drawScoreBoard(game.getCuingPlayer());
    }

    @FXML
    void clearRedBallsAction() {
        game.clearRedBallsTest();
        drawTargetBoard();
    }

    @FXML
    void withdrawAction() {
        if (game instanceof AbstractSnookerGame) {
            Player curPlayer = game.getCuingPlayer();
            int diff = ((AbstractSnookerGame) game).getScoreDiff(curPlayer);
            String behindText = diff <= 0 ? "落后" : "领先";
            if (AlertShower.askConfirmation(
                    stage,
                    String.format("%s%d分，台面剩余%d分，真的要认输吗？", behindText, Math.abs(diff),
                            ((AbstractSnookerGame) game).getRemainingScore()),
                    String.format("%s, 确认要认输吗？", curPlayer.getPlayerPerson().getName()))) {
                game.withdraw(curPlayer);
                endGame();
//                Recorder.save();
            }
        }
    }

    @FXML
    void cueAction() {
        if (game.isEnded() || cueAnimationPlayer != null) return;
        if (cursorDirectionUnitX == 0.0 && cursorDirectionUnitY == 0.0) return;

        setButtonsCueStart();

        PlayerPerson playerPerson = game.getCuingPlayer().getPlayerPerson();

        // 判断是否为进攻杆
        currentAttempt = null;
        if (predictedTargetBall != null) {
            List<double[][]> holeDirectionsAndHoles = game.directionsToAccessibleHoles(predictedTargetBall);
            for (double[][] directionHole : holeDirectionsAndHoles) {
                double pottingDirection = Algebra.thetaOf(directionHole[0]);
                double aimingDirection = Algebra.thetaOf(targetPredictionUnitX, targetPredictionUnitY);
                if (Math.abs(pottingDirection - aimingDirection) <= Game.MAX_ATTACK_DECISION_ANGLE) {
                    currentAttempt = new PotAttempt(
                            gameType,
                            game.getCuingPlayer().getPlayerPerson(),
                            new double[]{game.getCueBall().getX(), game.getCueBall().getY()},
                            new double[]{predictedTargetBall.getX(), predictedTargetBall.getY()},
                            directionHole[1]
                    );
                    System.out.printf("Angle is %f, attacking!\n",
                            Math.toDegrees(Math.abs(pottingDirection - aimingDirection)));
                    break;
                }
            }
        }

        double power = getPowerPercentage();
        double personPower = power / playerPerson.getMaxPowerPercentage();  // 球手的用力程度

        intentCuePointX = cuePointX;
        intentCuePointY = cuePointY;
        // 因为出杆质量而导致的打点偏移
        Random random = new Random();
        double xError = random.nextGaussian();
        double yError = random.nextGaussian();
        double[] muSigXy = playerPerson.getCuePointMuSigmaXY();
        xError = xError * muSigXy[1] + muSigXy[0];
        yError = yError * muSigXy[3] + muSigXy[2];
        xError = xError * personPower * cueAreaRadius / 150;
        yError = yError * personPower * cueAreaRadius / 150;
        cuePointX += xError;
        cuePointY += yError;
        System.out.println("intent: " + intentCuePointX + ", " + intentCuePointY);
        System.out.println("actual: " + cuePointX + ", " + cuePointY);

        double unitSideSpin = getUnitSideSpin();

        if (Algebra.distanceToPoint(cuePointX, cuePointY, cueCanvasWH / 2, cueCanvasWH / 2)
                > cueAreaRadius - cueRadius) {
            power /= 5;
            unitSideSpin *= 10;

            System.out.println("滑杆了！");
        }

        double[] unitXYWithSpin = getUnitXYWithSpins(unitSideSpin, power);
//        System.out.println("Ball weight " + game.getGameValues().ballWeightRatio);
        double vx = unitXYWithSpin[0] * power * Values.MAX_POWER_SPEED / 100.0;  // 常量，最大力白球速度
        double vy = unitXYWithSpin[1] * power * Values.MAX_POWER_SPEED / 100.0;

        double[] spins = calculateSpins(vx, vy, playerPerson);

        beginCueAnimation(vx, vy, spins[0], spins[1], spins[2]);
    }

    @FXML
    void newGameAction() {
        Platform.runLater(() -> {
            if (AlertShower.askConfirmation(stage, "真的要开始新游戏吗？", "请确认")) {
                startGame();
                drawTargetBoard();
                drawScoreBoard(game.getCuingPlayer());
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

    private double getUnitFrontBackSpin() {
        return (cueCanvasWH / 2 - cuePointY) / cueAreaRadius *
                game.getCuingPlayer().getInGamePlayer().getPlayCue().spinMultiplier;
    }

    private double getUnitSideSpin() {
        return (cuePointX - cueCanvasWH / 2) / cueAreaRadius *
                game.getCuingPlayer().getInGamePlayer().getPlayCue().spinMultiplier;
    }

    /**
     * 返回受到侧塞影响的白球单位向量
     */
    private double[] getUnitXYWithSpins(double unitSideSpin, double powerPercentage) {
        double offsetAngleRad = -unitSideSpin * powerPercentage / 2400;
        return Algebra.rotateVector(cursorDirectionUnitX, cursorDirectionUnitY, offsetAngleRad);
    }

    private double getPowerPercentage() {
        return Math.max(powerSlider.getValue(), 0.01) / game.getGameValues().ballWeightRatio *
                game.getCuingPlayer().getInGamePlayer().getCurrentCue(game).powerMultiplier;
    }

    private double[] calculateSpins(double vx, double vy, PlayerPerson playerPerson) {
        double speed = Math.hypot(vx, vy);

        double frontBackSpin = getUnitFrontBackSpin();  // 高杆正，低杆负
        double leftRightSpin = getUnitSideSpin();  // 右塞正（逆时针），左塞负
        if (frontBackSpin > 0) {
            // 高杆补偿
            frontBackSpin *= 1.2;
        }

        double spinRatio = Math.pow(speed / Values.MAX_POWER_SPEED, 0.5);

        double side = spinRatio * leftRightSpin * Values.MAX_SIDE_SPIN_SPEED;
        // 旋转产生的总目标速度
        double spinSpeed = spinRatio * frontBackSpin * Values.MAX_SPIN_SPEED *
                playerPerson.getMaxSpinPercentage() / 100;
        double spinX = vx * (spinSpeed / speed);
        double spinY = vy * (spinSpeed / speed);
//        System.out.printf("x %f, y %f, total %f, side %f\n", spinX, spinY, spinSpeed, side);

        return new double[]{spinX, spinY, side};
    }

    private void restoreCuePoint() {
        cuePointX = cueCanvasWH / 2;
        cuePointY = cueCanvasWH / 2;
        intentCuePointX = -1;
        intentCuePointY = -1;
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
        drawCueBallCanvas();
        drawCue();
    }

    private void setupCanvas() {
        gameCanvas.setWidth(canvasWidth);
        gameCanvas.setHeight(canvasHeight);
    }

    private void setupPowerSlider() {
        powerSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            double playerMaxPower = game.getCuingPlayer().getPlayerPerson().getMaxPowerPercentage();
            if (newValue.doubleValue() > playerMaxPower) {
                powerSlider.setValue(playerMaxPower);
                return;
            }
            powerLabel.setText(String.valueOf(Math.round(newValue.doubleValue())));
        }));

        powerSlider.setValue(40.0);
    }

    private void addListeners() {
        gameCanvas.setOnMouseClicked(this::onCanvasClicked);
        gameCanvas.setOnDragDetected(this::onDragStarted);
        gameCanvas.setOnMouseDragged(this::onDragging);
        gameCanvas.setOnMouseMoved(this::onMouseMoved);

        ballCanvas.setOnMouseClicked(this::onCueBallCanvasClicked);
        ballCanvas.setOnMouseDragged(this::onCueBallCanvasDragged);
    }

    private void drawPottedWhiteBall() {
        if (!game.isMoving() && game.isBallInHand()) {
            GameValues values = game.getGameValues();

            double x = realX(mouseX);
            if (x < values.leftX + values.ballRadius) x = values.leftX + values.ballRadius;
            else if (x >= values.rightX - values.ballRadius) x = values.rightX - values.ballRadius;

            double y = realY(mouseY);
            if (y < values.topY + values.ballRadius) y = values.topY + values.ballRadius;
            else if (y >= values.botY - values.ballRadius) y = values.botY - values.ballRadius;

            game.forcedDrawWhiteBall(
                    x,
                    y,
                    graphicsContext,
                    scale
            );
        }
    }

    private void drawTable() {
        GameValues values = game.getGameValues();

        graphicsContext.setFill(values.tableBorderColor);
        graphicsContext.fillRoundRect(0, 0, canvasWidth, canvasHeight, 20.0, 20.0);
        graphicsContext.setFill(values.tableColor);  // 台泥/台布
        graphicsContext.fillRect(
                canvasX(values.leftX - values.cornerHoleTan),
                canvasY(values.topY - values.cornerHoleTan),
                (values.innerWidth + values.cornerHoleTan * 2) * scale,
                (values.innerHeight + values.cornerHoleTan * 2) * scale);
        graphicsContext.setStroke(BLACK);
        graphicsContext.setLineWidth(2.0);

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
        graphicsContext.setStroke(BLACK);
        drawMidHoleLinesArcs(values);
        drawCornerHoleLinesArcs(values);

        graphicsContext.setFill(HOLE_PAINT);
        drawHole(values.topLeftHoleXY, values.cornerHoleRadius);
        drawHole(values.botLeftHoleXY, values.cornerHoleRadius);
        drawHole(values.topRightHoleXY, values.cornerHoleRadius);
        drawHole(values.botRightHoleXY, values.cornerHoleRadius);
        drawHole(values.topMidHoleXY, values.midHoleRadius);
        drawHole(values.botMidHoleXY, values.midHoleRadius);

        drawHoleOutLine(values.topLeftHoleXY, values.cornerHoleRadius, 45.0);
        drawHoleOutLine(values.botLeftHoleXY, values.cornerHoleRadius, 135.0);
        drawHoleOutLine(values.topRightHoleXY, values.cornerHoleRadius, -45.0);
        drawHoleOutLine(values.botRightHoleXY, values.cornerHoleRadius, -135.0);
        drawHoleOutLine(values.topMidHoleXY, values.midHoleRadius, 0.0);
        drawHoleOutLine(values.botMidHoleXY, values.midHoleRadius, 180.0);

        graphicsContext.setLineWidth(1.0);
        game.drawTableMarks(graphicsContext, scale);
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

    private void drawCornerHoleLinesArcs(GameValues values) {
        // 左上底袋
        drawCornerHoleArc(values.topLeftHoleSideArcXy, 225, values);
        drawCornerHoleArc(values.topLeftHoleEndArcXy, 0, values);

        // 左下底袋
        drawCornerHoleArc(values.botLeftHoleSideArcXy, 90, values);
        drawCornerHoleArc(values.botLeftHoleEndArcXy, 315, values);

        // 右上底袋
        drawCornerHoleArc(values.topRightHoleSideArcXy, 270, values);
        drawCornerHoleArc(values.topRightHoleEndArcXy, 135, values);

        // 右下底袋
        drawCornerHoleArc(values.botRightHoleSideArcXy, 45, values);
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

    private void drawCornerHoleArc(double[] arcRealXY, double startAngle, GameValues values) {
        graphicsContext.strokeArc(
                canvasX(arcRealXY[0] - values.cornerArcRadius),
                canvasY(arcRealXY[1] - values.cornerArcRadius),
                cornerArcDiameter,
                cornerArcDiameter,
                startAngle,
                45,
                ArcType.OPEN);
    }

    private void drawMidHoleLinesArcs(GameValues values) {
        double arcDiameter = values.midArcRadius * 2 * scale;
        double x1 = canvasX(values.topMidHoleXY[0] - values.midArcRadius * 2 - values.midHoleRadius);
        double x2 = canvasX(values.botMidHoleXY[0] + values.midArcRadius);
        double y1 = canvasY(values.topMidHoleXY[1] - values.midArcRadius);
        double y2 = canvasY(values.botMidHoleXY[1] - values.midArcRadius);
        graphicsContext.strokeArc(x1, y1, arcDiameter, arcDiameter, 270, 90, ArcType.OPEN);
        graphicsContext.strokeArc(x2, y1, arcDiameter, arcDiameter, 180, 90, ArcType.OPEN);
        graphicsContext.strokeArc(x1, y2, arcDiameter, arcDiameter, 0, 90, ArcType.OPEN);
        graphicsContext.strokeArc(x2, y2, arcDiameter, arcDiameter, 90, 90, ArcType.OPEN);

        // 袋内直线
        if (values.isStraightHole()) {
            for (double[][] line : values.allMidHoleLines) {
                drawHoleLine(line);
            }
        }
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
            if (game instanceof AbstractSnookerGame)
                drawSnookerTargetBoard((AbstractSnookerGame) game);
            else if (game instanceof ChineseEightBallGame)
                drawPoolTargetBoard((ChineseEightBallGame) game);
        });
    }

    private void drawSnookerTargetBoard(AbstractSnookerGame game1) {
        if (game1.getCuingPlayer().getNumber() == 1) {
            drawSnookerTargetBall(player1TarCanvas, game1.getCurrentTarget(), game1.isDoingFreeBall());
            wipeCanvas(player2TarCanvas);
        } else {
            drawSnookerTargetBall(player2TarCanvas, game1.getCurrentTarget(), game1.isDoingFreeBall());
            wipeCanvas(player1TarCanvas);
        }
    }

    private void drawPoolTargetBoard(ChineseEightBallGame game1) {
        if (game1.getCuingPlayer().getNumber() == 1) {
            drawPoolTargetBall(player1TarCanvas, game1.getCurrentTarget());
            wipeCanvas(player2TarCanvas);
        } else {
            drawPoolTargetBall(player2TarCanvas, game1.getCurrentTarget());
            wipeCanvas(player1TarCanvas);
        }
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

    private void drawSnookerTargetBall(Canvas canvas, int value, boolean isFreeBall) {
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

    /**
     * @param value see {@link Game#getCurrentTarget()}
     */
    private void drawPoolTargetBall(Canvas canvas, int value) {
        System.out.println(value);
        if (value == 0) {
            drawTargetColoredBall(canvas);
        } else {

            NumberedBallGame.drawPoolBallEssential(
                    ballDiameter * 0.6,
                    ballDiameter * 0.6,
                    ballDiameter,
                    Ball.poolBallBaseColor(value),
                    value,
                    canvas.getGraphicsContext2D()
            );
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

    private double getPredictionLineTotalLength(double potDt, PlayerPerson playerPerson) {
        Cue cue = game.getCuingPlayer().getInGamePlayer().getCurrentCue(game);
        double maxLen = maxRealPredictLength * cue.accuracyMultiplier;

        double predictLineTotalLen;
        if (potDt >= minPredictLengthPotDt) predictLineTotalLen = minRealPredictLength;
        else if (potDt < maxPredictLengthPotDt) predictLineTotalLen = maxLen;
        else {
            double potDtRange = minPredictLengthPotDt - maxPredictLengthPotDt;
            double lineLengthRange = maxLen - minRealPredictLength;
            double potDtInRange = (potDt - maxPredictLengthPotDt) / potDtRange;
            predictLineTotalLen = maxLen - potDtInRange * lineLengthRange;
        }
        double side = Math.abs(cuePointX - cueCanvasWH / 2) / cueCanvasWH;  // 0和0.5之间
        return predictLineTotalLen * (1 - side) * playerPerson.getPrecisionPercentage() / 100;  // 加塞影响瞄准
    }

    private void drawCursor() {
        if (game.isEnded()) return;
        if (game.isMoving()) return;
        if (cursorDirectionUnitX == 0.0 && cursorDirectionUnitY == 0.0) return;

        Ball whiteBall = game.getCueBall();
        double whiteX = canvasX(whiteBall.getX());
        double whiteY = canvasY(whiteBall.getY());

        if (whiteBall.isPotted()) return;

//        double backDistanceToWall = game.cueBackDistanceToObstacle(cursorDirectionUnitX, cursorDirectionUnitY);
//        System.out.println(backDistanceToWall);

        double[] unitXYWithSpin = getUnitXYWithSpins(getUnitSideSpin(), getPowerPercentage());
        double unitX = unitXYWithSpin[0];
        double unitY = unitXYWithSpin[1];
        PredictedPos predictedPos = game.getPredictedHitBall(unitX, unitY);

        graphicsContext.setStroke(WHITE);
        if (predictedPos == null) {
            graphicsContext.strokeLine(whiteX, whiteY,
                    whiteX + unitX * 1000.0, whiteY + unitY * 1000.0);
        } else {
            predictedTargetBall = predictedPos.getTargetBall();
            double[] targetPos = predictedPos.getPredictedWhitePos();
            double[] ballPos = new double[]{predictedPos.getTargetBall().getX(), predictedPos.getTargetBall().getY()};
            double tarCanvasX = canvasX(targetPos[0]);
            double tarCanvasY = canvasY(targetPos[1]);
            graphicsContext.strokeLine(whiteX, whiteY, tarCanvasX, tarCanvasY);
            graphicsContext.strokeOval(tarCanvasX - ballRadius, tarCanvasY - ballRadius,
                    ballDiameter, ballDiameter);  // 绘制预测撞击点的白球

            double potDt = Algebra.distanceToPoint(targetPos[0], targetPos[1], whiteBall.getX(), whiteBall.getY());
            // 白球行进距离越长，预测线越短
            double predictLineTotalLen = getPredictionLineTotalLength(potDt, game.getCuingPlayer().getPlayerPerson());

            double whiteUnitX = (targetPos[0] - whiteBall.getX()) / potDt;
            double whiteUnitY = (targetPos[1] - whiteBall.getY()) / potDt;
            double ang = (targetPos[0] - ballPos[0]) / (targetPos[1] - ballPos[1]);
            targetPredictionUnitY = (ang * whiteUnitX + whiteUnitY) / (ang * ang + 1);
            targetPredictionUnitX = ang * targetPredictionUnitY;

            double predictWhiteX = whiteUnitX - targetPredictionUnitX;
            double predictWhiteY = whiteUnitY - targetPredictionUnitY;

            double predictTarMag = Math.hypot(targetPredictionUnitX, targetPredictionUnitY);
            double predictWhiteMag = Math.hypot(predictWhiteX, predictWhiteY);
            double totalMag = predictTarMag + predictWhiteMag;
            double multiplier = predictLineTotalLen / totalMag;

            double lineX = targetPredictionUnitX * multiplier * scale;
            double lineY = targetPredictionUnitY * multiplier * scale;
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

    private void beginCueAnimation(double vx, double vy,
                                   double xSpin, double ySpin, double sideSpin) {
        double powerPercentage = getPowerPercentage();
//        double maxCueDtToWhite = powerPercentage * 1.5 + 50.0;  // 最大200毫米, 最小50毫米
        Ball cueBall = game.getCueBall();
        if (cueBall.isPotted()) {
            System.out.println("Error");
            return;
        }

        PlayerPerson playerPerson = game.getCuingPlayer().getPlayerPerson();
        double maxPullDt =
                (playerPerson.getMaxPullDt() - playerPerson.getMinPullDt()) *
                        powerPercentage / 100 + playerPerson.getMinPullDt();
        double handDt = maxPullDt + HAND_DT_TO_MAX_PULL;
        double handX = cueBall.getX() - handDt * cursorDirectionUnitX;
        double handY = cueBall.getY() - handDt * cursorDirectionUnitY;

        // 出杆速度与白球球速算法相同
        cueAnimationPlayer = new CueAnimationPlayer(
                60.0,
                maxPullDt,
                powerPercentage,
                handX,
                handY,
                cursorDirectionUnitX,
                cursorDirectionUnitY,
                vx, vy, xSpin, ySpin, sideSpin,
                game.getCuingPlayer().getInGamePlayer().getCurrentCue(game),
                game.getCuingPlayer().getPlayerPerson()
        );
    }

    private void endCueAnimation() {
//        System.out.println("End!");
        cursorDirectionUnitX = 0.0;
        cursorDirectionUnitY = 0.0;
        cueAnimationPlayer = null;
    }

    private void drawCue() {
        if (game.isEnded()) return;
        if (cueAnimationPlayer == null) {
            if (game.isMoving()) return;
            if (cursorDirectionUnitX == 0.0 && cursorDirectionUnitY == 0.0) return;

            Ball cueBall = game.getCueBall();
            if (cueBall.isPotted()) return;

            drawCueWithDtToHand(cueBall.getX(), cueBall.getY(),
                    cursorDirectionUnitX,
                    cursorDirectionUnitY,
                    60.0 + HAND_DT_TO_MAX_PULL,
                    game.getCuingPlayer().getInGamePlayer().getCurrentCue(game));
        } else {
//            System.out.println("Drawing!");
            drawCueWithDtToHand(
                    cueAnimationPlayer.handX,
                    cueAnimationPlayer.handY,
                    cueAnimationPlayer.pointingUnitX,
                    cueAnimationPlayer.pointingUnitY,
                    cueAnimationPlayer.cueDtToWhite -
                            cueAnimationPlayer.maxPullDistance - HAND_DT_TO_MAX_PULL +
                            game.getGameValues().ballRadius,
                    cueAnimationPlayer.cue);
            cueAnimationPlayer.nextFrame();
        }
    }

    private double[] getCueHitPoint(double cueBallRealX, double cueBallRealY,
                                    double pointingUnitX, double pointingUnitY) {
        double originalTouchX = canvasX(cueBallRealX);
        double originalTouchY = canvasY(cueBallRealY);
        double sideRatio = getUnitSideSpin() * 0.7;
        double sideXOffset = -pointingUnitY *
                sideRatio * game.getGameValues().ballRadius * scale;
        double sideYOffset = pointingUnitX *
                sideRatio * game.getGameValues().ballRadius * scale;
        return new double[]{
                originalTouchX + sideXOffset,
                originalTouchY + sideYOffset
        };
    }

    private void drawCueWithDtToHand(double handX,
                                     double handY,
                                     double pointingUnitX,
                                     double pointingUnitY,
                                     double realDistance,
                                     Cue cue) {
//        System.out.println(distance);
        double[] touchXY = getCueHitPoint(handX, handY, pointingUnitX, pointingUnitY);

        double correctedTipX = touchXY[0] - pointingUnitX * realDistance * scale;
        double correctedTipY = touchXY[1] - pointingUnitY * realDistance * scale;
        double correctedEndX = correctedTipX - pointingUnitX *
                cue.getTotalLength() * scale;
        double correctedEndY = correctedTipY - pointingUnitY *
                cue.getTotalLength() * scale;

        drawCueEssential(correctedTipX, correctedTipY, correctedEndX, correctedEndY,
                pointingUnitX, pointingUnitY, cue);
    }

    private void drawCueEssential(double cueTipX, double cueTipY,
                                  double cueEndX, double cueEndY,
                                  double pointingUnitX, double pointingUnitY,
                                  Cue cue) {
//        Cue cue = game.getCuingPlayer().getInGamePlayer().getCurrentCue(game);

        // 杆头，不包含皮头
        double cueFrontX = cueTipX - cue.cueTipThickness * pointingUnitX * scale;
        double cueFrontY = cueTipY - cue.cueTipThickness * pointingUnitY * scale;

        // 皮头环
        double ringFrontX = cueFrontX - cue.tipRingThickness * pointingUnitX * scale;
        double ringFrontY = cueFrontY - cue.tipRingThickness * pointingUnitY * scale;

        // 杆前段的尾部
        double cueFrontLastX = ringFrontX - cue.frontLength * pointingUnitX * scale;
        double cueFrontLastY = ringFrontY - cue.frontLength * pointingUnitY * scale;

        // 杆中段的尾部
        double cueMidLastX = cueFrontLastX - cue.midLength * pointingUnitX * scale;
        double cueMidLastY = cueFrontLastY - cue.midLength * pointingUnitY * scale;

        // 杆尾弧线
        double tailLength = cue.getEndWidth() * 0.2;
        double tailWidth = tailLength * 1.5;
        double tailX = cueEndX - tailLength * pointingUnitX * scale;
        double tailY = cueEndY - tailLength * pointingUnitY * scale;
        double[] tailLeft = new double[]{
                tailX - tailWidth * -pointingUnitY * scale / 2,
                tailY - tailWidth * pointingUnitX * scale / 2
        };
        double[] tailRight = new double[]{
                tailX + tailWidth * -pointingUnitY * scale / 2,
                tailY + tailWidth * pointingUnitX * scale / 2
        };

        double[] cueEndLeft = new double[]{
                cueEndX - cue.getEndWidth() * -pointingUnitY * scale / 2,
                cueEndY - cue.getEndWidth() * pointingUnitX * scale / 2
        };
        double[] cueEndRight = new double[]{
                cueEndX + cue.getEndWidth() * -pointingUnitY * scale / 2,
                cueEndY + cue.getEndWidth() * pointingUnitX * scale / 2
        };
        double[] cueMidLastLeft = new double[]{
                cueMidLastX - cue.getMidMaxWidth() * -pointingUnitY * scale / 2,
                cueMidLastY - cue.getMidMaxWidth() * pointingUnitX * scale / 2
        };
        double[] cueMidLastRight = new double[]{
                cueMidLastX + cue.getMidMaxWidth() * -pointingUnitY * scale / 2,
                cueMidLastY + cue.getMidMaxWidth() * pointingUnitX * scale / 2
        };
        double[] cueFrontLastLeft = new double[]{
                cueFrontLastX - cue.getFrontMaxWidth() * -pointingUnitY * scale / 2,
                cueFrontLastY - cue.getFrontMaxWidth() * pointingUnitX * scale / 2
        };
        double[] cueFrontLastRight = new double[]{
                cueFrontLastX + cue.getFrontMaxWidth() * -pointingUnitY * scale / 2,
                cueFrontLastY + cue.getFrontMaxWidth() * pointingUnitX * scale / 2
        };
        double[] cueRingLastLeft = new double[]{
                ringFrontX - cue.getRingMaxWidth() * -pointingUnitY * scale / 2,
                ringFrontY - cue.getRingMaxWidth() * pointingUnitX * scale / 2
        };
        double[] cueRingLastRight = new double[]{
                ringFrontX + cue.getRingMaxWidth() * -pointingUnitY * scale / 2,
                ringFrontY + cue.getRingMaxWidth() * pointingUnitX * scale / 2
        };
        double[] cueHeadLeft = new double[]{
                cueFrontX - cue.getCueTipWidth() * -pointingUnitY * scale / 2,
                cueFrontY - cue.getCueTipWidth() * pointingUnitX * scale / 2
        };
        double[] cueHeadRight = new double[]{
                cueFrontX + cue.getCueTipWidth() * -pointingUnitY * scale / 2,
                cueFrontY + cue.getCueTipWidth() * pointingUnitX * scale / 2
        };
        double[] cueTipLeft = new double[]{
                cueTipX - cue.getCueTipWidth() * -pointingUnitY * scale / 2,
                cueTipY - cue.getCueTipWidth() * pointingUnitX * scale / 2
        };
        double[] cueTipRight = new double[]{
                cueTipX + cue.getCueTipWidth() * -pointingUnitY * scale / 2,
                cueTipY + cue.getCueTipWidth() * pointingUnitX * scale / 2
        };

        // 杆尾弧线
        double[] tailXs = {
                tailLeft[0], tailRight[0], cueEndRight[0], cueEndLeft[0]
        };
        double[] tailYs = {
                tailLeft[1], tailRight[1], cueEndRight[1], cueEndLeft[1]
        };

        // 末段
        double[] endXs = {
                cueEndLeft[0], cueEndRight[0], cueMidLastRight[0], cueMidLastLeft[0]
        };
        double[] endYs = {
                cueEndLeft[1], cueEndRight[1], cueMidLastRight[1], cueMidLastLeft[1]
        };

        // 中段
        double[] midXs = {
                cueMidLastLeft[0], cueMidLastRight[0], cueFrontLastRight[0], cueFrontLastLeft[0]
        };
        double[] midYs = {
                cueMidLastLeft[1], cueMidLastRight[1], cueFrontLastRight[1], cueFrontLastLeft[1]
        };

        // 前段
        double[] frontXs = {
                cueFrontLastLeft[0], cueFrontLastRight[0], cueRingLastRight[0], cueRingLastLeft[0]
        };
        double[] frontYs = {
                cueFrontLastLeft[1], cueFrontLastRight[1], cueRingLastRight[1], cueRingLastLeft[1]
        };

        // 皮头环
        double[] ringXs = {
                cueRingLastLeft[0], cueRingLastRight[0], cueHeadRight[0], cueHeadLeft[0]
        };
        double[] ringYs = {
                cueRingLastLeft[1], cueRingLastRight[1], cueHeadRight[1], cueHeadLeft[1]
        };

        // 皮头
        double[] tipXs = {
                cueHeadLeft[0], cueHeadRight[0], cueTipRight[0], cueTipLeft[0]
        };
        double[] tipYs = {
                cueHeadLeft[1], cueHeadRight[1], cueTipRight[1], cueTipLeft[1]
        };

//        // 总轮廓线
//        double[] xs = {
//                cueTipLeft[0], cueTipRight[0], cueEndRight[0], cueEndLeft[0]
//        };
//        double[] ys = {
//                cueTipLeft[1], cueTipRight[1], cueEndRight[1], cueEndLeft[1]
//        };

        graphicsContext.setFill(CUE_TIP_COLOR);
        graphicsContext.fillPolygon(tipXs, tipYs, 4);
        graphicsContext.setFill(cue.tipRingColor);
        graphicsContext.fillPolygon(ringXs, ringYs, 4);
        graphicsContext.setFill(cue.frontColor);
        graphicsContext.fillPolygon(frontXs, frontYs, 4);
        graphicsContext.setFill(cue.midColor);
        graphicsContext.fillPolygon(midXs, midYs, 4);
        graphicsContext.setFill(cue.backColor);
        graphicsContext.fillPolygon(endXs, endYs, 4);
        graphicsContext.setFill(Color.BLACK.brighter());
        graphicsContext.fillPolygon(tailXs, tailYs, 4);

//        System.out.printf("Successfully drawn at (%f, %f), (%f, %f)\n",
//                cueTipX, cueTipY, cueEndX, cueEndY);

//        graphicsContext.setLineWidth(1.0);
//        graphicsContext.setStroke(Color.BLACK);
//        graphicsContext.strokePolygon(xs, ys, 4);

        // 杆尾圆弧
    }

    private void drawCueBallCanvas() {
        double cueAreaDia = cueAreaRadius * 2;
        double padding = (cueCanvasWH - cueAreaDia) / 2;
        ballCanvasGc.setStroke(BLACK);
        ballCanvasGc.setFill(Values.WHITE);
        ballCanvasGc.fillOval(padding, padding, cueAreaDia, cueAreaDia);
        ballCanvasGc.strokeOval(padding, padding, cueAreaDia, cueAreaDia);

        if (intentCuePointX >= 0 && intentCuePointY >= 0) {
            ballCanvasGc.setFill(INTENT_CUE_POINT);
            ballCanvasGc.fillOval(intentCuePointX - cueRadius, intentCuePointY - cueRadius,
                    cueRadius * 2, cueRadius * 2);
        }

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

    class CueAnimationPlayer {
        private long holdMs;  // 拉至满弓的停顿时间
        private long endHoldMs;  // 出杆完成后的停顿时间
        private long heldMs = 0;
        private long endHeldMs = 0;

//        private final double

        private final double initDistance, maxPullDistance;
        private double cueDtToWhite;  // 杆的动画离白球的真实距离，未接触前为正
        private double cueMoveSpeed;  // 杆每毫秒运动的距离，毫米
        private final double maxExtension;  // 杆的最大延伸距离，始终为负
        private boolean touched;  // 是否已经接触白球
        private boolean reachedMaxPull;

        //        private final double cueBallX, cueBallY;
        private final double handX, handY;  // 手架的位置，作为杆的摇摆中心点
        private final double cueVx, cueVy, xSpin, ySpin, sideSpin;
        private double pointingUnitX, pointingUnitY;
        private final double powerPercentage;

        private final Cue cue;
        private final PlayerPerson playerPerson;

        CueAnimationPlayer(double initDistance,
                           double maxPullDt,
                           double powerPercentage,
                           double handX, double handY,
                           double pointingUnitX, double pointingUnitY,
                           double cueVx, double cueVy,
                           double xSpin, double ySpin, double sideSpin,
                           Cue cue,
                           PlayerPerson playerPerson) {

            this.initDistance = Math.min(initDistance, maxPullDt);
            this.maxPullDistance = maxPullDt;
            this.cueDtToWhite = this.initDistance;
            this.maxExtension = -maxPullDistance *
                    (playerPerson.getMaxSpinPercentage() * 0.75 / 100);  // 杆法好的人延伸长
            this.cueMoveSpeed = powerPercentage * Values.MAX_POWER_SPEED / 100_000.0;
//            this.cueBallX = cueBallInitX;
//            this.cueBallY = cueBallInitY;
            this.powerPercentage = powerPercentage;

            this.pointingUnitX = pointingUnitX;
            this.pointingUnitY = pointingUnitY;

            this.handX = handX;
            this.handY = handY;

            System.out.println(cueDtToWhite + ", " + this.cueMoveSpeed + ", " + maxExtension);

            this.cueVx = cueVx;
            this.cueVy = cueVy;
            this.xSpin = xSpin;
            this.ySpin = ySpin;
            this.sideSpin = sideSpin;

            this.cue = cue;
            this.playerPerson = playerPerson;

            this.holdMs = playerPerson.getCuePlayType().getPullHoldMs();
            this.endHoldMs = playerPerson.getCuePlayType().getEndHoldMs();
        }

        void nextFrame() {
            if (reachedMaxPull && heldMs < holdMs) {
                heldMs += frameTimeMs;
            } else if (endHeldMs > 0) {
                endHeldMs += frameTimeMs;
                if (endHeldMs >= endHoldMs) {
                    endCueAnimation();
                }
            } else if (reachedMaxPull) {

                // 正常出杆
                if (cueDtToWhite > maxPullDistance / 2) {
                    cueDtToWhite -= cueMoveSpeed * frameTimeMs / 2;
                } else {
                    cueDtToWhite -= cueMoveSpeed * frameTimeMs;
                }
                double wholeDtPercentage = 1 - (cueDtToWhite - maxExtension) /
                        (maxPullDistance - maxExtension);  // 出杆完成的百分比
                wholeDtPercentage = Math.min(wholeDtPercentage, 0.9999);
//                System.out.println(wholeDtPercentage);

                List<Double> stages = playerPerson.getCuePlayType().getSequence();
                double stage = stages.get((int) (wholeDtPercentage * stages.size()));
                if (stage < 0) {  // 向左扭
                    double angle = Algebra.thetaOf(pointingUnitX, pointingUnitY);
                    double newAngle = angle + playerPerson.getCueSwingMag() *
                            powerPercentage / 200_000;
                    double[] newUnit = Algebra.angleToUnitVector(newAngle);
                    pointingUnitX = newUnit[0];
                    pointingUnitY = newUnit[1];
                } else if (stage > 0) {  // 向右扭
                    double angle = Algebra.thetaOf(pointingUnitX, pointingUnitY);
                    double newAngle = angle - playerPerson.getCueSwingMag() *
                            powerPercentage / 200_000;
                    double[] newUnit = Algebra.angleToUnitVector(newAngle);
                    pointingUnitX = newUnit[0];
                    pointingUnitY = newUnit[1];
                }

                if (cueDtToWhite <= maxExtension) {  // 出杆结束了
                    endHeldMs += frameTimeMs;
                } else if (Math.abs(cueDtToWhite) < cueMoveSpeed * frameTimeMs) {
                    if (!touched) {
                        touched = true;
//                        System.out.println("+++++++++++++++ Touched! +++++++++++++++");
                        game.cue(cueVx, cueVy, xSpin, ySpin, sideSpin);
                    }
                }
            } else {
                cueDtToWhite += (cueMoveSpeed / 5) * frameTimeMs *
                        playerPerson.getCuePlayType().getPullSpeedMul();  // 往后拉
                if (cueDtToWhite >= maxPullDistance) {
                    reachedMaxPull = true;
                }
            }
        }
    }
}
