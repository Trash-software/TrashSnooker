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
import trashsoftware.trashSnooker.audio.GameAudio;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.ai.AiCueBallPlacer;
import trashsoftware.trashSnooker.core.ai.AiCueResult;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.movement.MovementFrame;
import trashsoftware.trashSnooker.core.movement.WhitePrediction;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallGame;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallPlayer;
import trashsoftware.trashSnooker.core.scoreResult.ChineseEightScoreResult;
import trashsoftware.trashSnooker.core.scoreResult.SnookerScoreResult;
import trashsoftware.trashSnooker.core.snooker.AbstractSnookerGame;
import trashsoftware.trashSnooker.core.snooker.SnookerPlayer;
import trashsoftware.trashSnooker.core.table.ChineseEightTable;
import trashsoftware.trashSnooker.core.table.NumberedBallTable;
import trashsoftware.trashSnooker.fxml.alert.AlertShower;
import trashsoftware.trashSnooker.fxml.projection.BallProjection;
import trashsoftware.trashSnooker.fxml.projection.CushionProjection;
import trashsoftware.trashSnooker.fxml.projection.ObstacleProjection;
import trashsoftware.trashSnooker.recorder.CueRecord;
import trashsoftware.trashSnooker.recorder.GameRecorder;
import trashsoftware.trashSnooker.recorder.GameReplay;

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

    public static final double MAX_CUE_ANGLE = 60;
    //    private double minRealPredictLength = 300.0;
    private static final double DEFAULT_MAX_PREDICT_LENGTH = 1000.0;
    private final double minPredictLengthPotDt = 2000.0;
    private final double maxPredictLengthPotDt = 100.0;
    public double scale = 0.32;
    public double frameTimeMs = 20.0;
    @FXML
    Canvas gameCanvas;
    @FXML
    Canvas cueAngleCanvas;
    @FXML
    Label cueAngleLabel;
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
    Label player1FramesLabel, totalFramesLabel, player2FramesLabel;
    @FXML
    Label snookerScoreDiffLabel, snookerScoreRemainingLabel;
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
    private GraphicsContext cueAngleCanvasGc;
    private Stage stage;
    private InGamePlayer player1;
    private InGamePlayer player2;
    private EntireGame game;
    private GameReplay replay;
    //    private Game game;
    private GameType gameType;
    //    private double cursorX, cursorY;
    private double cursorDirectionUnitX, cursorDirectionUnitY;
    private double targetPredictionUnitX, targetPredictionUnitY;
    private Ball predictedTargetBall;
    //    private PotAttempt currentAttempt;
    private Movement movement;
    private boolean playingMovement = false;
    private DefenseAttempt curDefAttempt;
    // 用于播放运杆动画时持续显示预测线（含出杆随机偏移）
    private SavedPrediction predictionOfCue;
    private ObstacleProjection obstacleProjection;
    private double mouseX, mouseY;

    // 杆法的击球点。注意: cuePointY用在击球点的canvas上，值越大杆法越低，而unitFrontBackSpin相反
    private double cuePointX, cuePointY;  // 杆法的击球点
    private double intentCuePointX = -1, intentCuePointY = -1;  // 计划的杆法击球点
    private double cueAngleDeg = 5.0;
    private double cueAngleBaseVer = 10.0;
    private double cueAngleBaseHor = 10.0;
    private CueAnimationPlayer cueAnimationPlayer;
    private boolean isDragging;
    private double lastDragAngle;
    private Timeline timeline;
    private double predictionMultiplier = 2000.0;
    private double maxRealPredictLength = DEFAULT_MAX_PREDICT_LENGTH;
    private double whitePredictLenAfterWall = 1000.0;
    private boolean enablePsy = true;  // 由游戏决定心理影响
    private boolean aiCalculating;
    private boolean aiAutoPlay = true;
    private boolean printPlayStage = false;

    private long replayStopTime;
    private long replayGap = 1000;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        graphicsContext = gameCanvas.getGraphicsContext2D();
        ballCanvasGc = ballCanvas.getGraphicsContext2D();
        cueAngleCanvasGc = cueAngleCanvas.getGraphicsContext2D();

        graphicsContext.setTextAlign(TextAlignment.CENTER);

        addListeners();
        restoreCuePoint();
        restoreCueAngle();

        powerSlider.setShowTickLabels(true);
    }

    private void generateScales() {
        GameValues values = gameType.gameValues;
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
        if (gameType.snookerLike)
            singlePoleCanvas.setWidth(ballDiameter * 7 * 1.2);
        else if (gameType == GameType.CHINESE_EIGHT)
            singlePoleCanvas.setWidth(ballDiameter * 8 * 1.2);
        else if (gameType == GameType.SIDE_POCKET)
            singlePoleCanvas.setWidth(ballDiameter * 9 * 1.2);

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

    public void setupReplay(Stage stage, GameReplay replay) {
        this.stage = stage;
        this.replay = replay;
        this.gameType = replay.gameType;
        this.player1 = replay.getP1();
        this.player2 = replay.getP2();

        player1Label.setText(player1.getPlayerPerson().getName());
        player2Label.setText(player2.getPlayerPerson().getName());
        totalFramesLabel.setText(String.format("(%d)", replay.getItem().totalFrames));

        setupPowerSlider();
        generateScales();
        setUiFrameStart();

        setOnHidden();

        while (replay.loadNext() && replay.getCurrentFlag() == GameRecorder.FLAG_HANDBALL) {
            // do nothing
        }
        replayCue();
    }

    public void setup(Stage stage, GameType gameType, int totalFrames,
                      InGamePlayer player1, InGamePlayer player2) {
        this.stage = stage;
        this.gameType = gameType;
        this.player1 = player1;
        this.player2 = player2;

        player1Label.setText(player1.getPlayerPerson().getName());
        player2Label.setText(player2.getPlayerPerson().getName());
        totalFramesLabel.setText(String.format("(%d)", totalFrames));

        startGame(totalFrames);

        setupPowerSlider();
        generateScales();
        setUiFrameStart();

        stage.setOnCloseRequest(e -> {
            if (!game.isFinished()) {
                e.consume();

                AlertShower.askConfirmation(stage,
                        "游戏未结束，是否退出？",
                        "请确认",
                        () -> {
                            game.quitGame();
                            timeline.stop();
                            stage.close();
                        },
                        null);
            }
        });

        setOnHidden();
    }

    private void setOnHidden() {
        this.stage.setOnHidden(e -> {
            if (replay != null) {
                try {
                    replay.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                if (!game.getGame().getRecorder().isFinished()) {
                    game.getGame().getRecorder().stopRecording();
                }
                game.quitGame();
            }
            timeline.stop();
        });
    }

    private void setUiFrameStart() {
        PlayerPerson playerPerson;
        if (replay != null) {
            if (replay.getCueRecord() != null) {
                playerPerson = replay.getCueRecord().cuePlayer.getPlayerPerson();
            } else {
                return;
            }
        } else {
            playerPerson = game.getGame().getCuingPlayer().getPlayerPerson();
        }

        powerSlider.setMajorTickUnit(
                playerPerson.getControllablePowerPercentage()
        );
        if (replay == null) cueButton.setDisable(false);
        updateScoreDiffLabels();
    }

    private void updateScoreDiffLabels() {
        if (gameType.snookerLike) {
            if (replay != null) {

            } else {
                AbstractSnookerGame asg = (AbstractSnookerGame) game.getGame();
                snookerScoreDiffLabel.setText("分差 " + asg.getScoreDiff());
                snookerScoreRemainingLabel.setText("台面剩余 " + asg.getRemainingScore());
            }
        }
    }

    public void finishCueReplay() {
        replayStopTime = System.currentTimeMillis();
        if (replay.loadNext()) {
            drawScoreBoard(null);
            drawTargetBoard();
            restoreCuePoint();
            restoreCueAngle();
            updateScoreDiffLabels();
        } else {
            System.out.println("Replay finished!");

        }
    }

    public void finishCue(Player justCuedPlayer, Player nextCuePlayer) {
//        updateCuePlayerSinglePole(justCuedPlayer);
        oneFrame();
        drawScoreBoard(justCuedPlayer);
        drawTargetBoard();
        restoreCuePoint();
        restoreCueAngle();
        updateScoreDiffLabels();
        Platform.runLater(() -> {
            powerSlider.setValue(40.0);
            powerSlider.setMajorTickUnit(
                    nextCuePlayer.getPlayerPerson().getControllablePowerPercentage());
        });
        setButtonsCueEnd(nextCuePlayer);
        obstacleProjection = null;
        printPlayStage = true;

        game.getGame().getRecorder().recordScore(game.getGame().makeScoreResult(justCuedPlayer));
        game.getGame().getRecorder().writeCueToStream();

        if (game.getGame().isEnded()) {
            endFrame();
        } else {
            if (game.getGame().isLastCueFoul()) {
                String foulReason = game.getGame().getFoulReason();
                Platform.runLater(() -> {
                    AlertShower.showInfo(
                            stage,
                            foulReason,
                            "犯规"
                    );
                    finishCueNextStep(nextCuePlayer);
                });
            } else {
                finishCueNextStep(nextCuePlayer);
            }
        }
    }

    private void finishCueNextStep(Player nextCuePlayer) {
        if (nextCuePlayer.getInGamePlayer().getPlayerType() == PlayerType.PLAYER) {
            if ((game.getGame() instanceof AbstractSnookerGame) &&
                    ((AbstractSnookerGame) game.getGame()).canReposition()) {
                askReposition();
            }
        } else {
            if (!game.isFinished() &&
                    aiAutoPlay) {
                Platform.runLater(() -> aiCue(nextCuePlayer));
            }
        }
    }

    private void endFrame() {
        Player wonPlayer = game.getGame().getWiningPlayer();

        boolean entireGameEnd = game.playerWinsAframe(wonPlayer.getInGamePlayer());

        game.getGame().getRecorder().stopRecording();
        game.getPlayer1().getPersonRecord().writeToFile();
        game.getPlayer2().getPersonRecord().writeToFile();

        Platform.runLater(() -> {
            AlertShower.showInfo(stage,
                    String.format("%s  %d (%d) : (%d) %d  %s",
                            game.getPlayer1().getPlayerPerson().getName(),
                            game.getGame().getPlayer1().getScore(),
                            game.getP1Wins(),
                            game.getP2Wins(),
                            game.getGame().getPlayer2().getScore(),
                            game.getPlayer2().getPlayerPerson().getName()),
                    String.format("%s 赢得一局。", wonPlayer.getPlayerPerson().getName()));

            if (entireGameEnd) {
                AlertShower.showInfo(stage,
                        String.format("%s (%d) : (%d) %s",
                                game.getPlayer1().getPlayerPerson().getName(),
                                game.getP1Wins(),
                                game.getP2Wins(),
                                game.getPlayer2().getPlayerPerson().getName()),
                        String.format("%s 胜利。", wonPlayer.getPlayerPerson().getName()));
            } else {
                AlertShower.askConfirmation(
                        stage,
                        "是否开始下一局？",
                        "开始下一局？",
                        "是",
                        "保存并退出",
                        () -> {
                            game.startNextFrame();
                            drawScoreBoard(game.getGame().getCuingPlayer());
                            drawTargetBoard();
                            setUiFrameStart();
//                            if (game.getGame().getCuingPlayer().getInGamePlayer().getPlayerType() == PlayerType.COMPUTER) {
//                                ss
//                            }
                        },
                        () -> {
                            game.save();
                            stage.hide();
                        }
                );
            }
        });
    }

    private void askReposition() {
        Platform.runLater(() ->
                AlertShower.askConfirmation(stage, "是否复位？", "对方犯规",
                        () -> {
                            ((AbstractSnookerGame) game.getGame()).reposition();
                            drawScoreBoard(game.getGame().getCuingPlayer());
                            drawTargetBoard();
                            if (aiAutoPlay &&
                                    game.getGame().getCuingPlayer().getInGamePlayer().getPlayerType() == PlayerType.COMPUTER) {
                                aiCue(game.getGame().getCuingPlayer());
                            }
                        }, null));
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

    private double getRatioOfCueAndBall() {
        return game.getGame().getCuingPlayer().getInGamePlayer()
                .getCurrentCue(game.getGame()).getCueTipWidth() /
                game.getGame().getGameValues().ballDiameter;
    }

    private double getCuePointRelX(double x) {
        return (x - cueCanvasWH / 2) / cueAreaRadius;
    }

    private double getCuePointRelY(double y) {
        return (y - cueCanvasWH / 2) / cueAreaRadius;
    }

    private double getCuePointCanvasX(double x) {
        return x * cueAreaRadius + cueCanvasWH / 2;
    }

    private double getCuePointCanvasY(double y) {
        return y * cueAreaRadius + cueCanvasWH / 2;
    }

    private void setCuePoint(double x, double y) {
        if (Algebra.distanceToPoint(x, y, cueCanvasWH / 2, cueCanvasWH / 2) <
                cueAreaRadius - cueRadius) {
            if (obstacleProjection == null ||
                    obstacleProjection.cueAble(
                            getCuePointRelX(x), getCuePointRelY(y), getRatioOfCueAndBall())) {
                cuePointX = x;
                cuePointY = y;
                recalculateUiRestrictions();
            }
        }
    }

    private void setCueAnglePoint(double x, double y) {
        double relX = x - cueAngleBaseHor;
        double relY = cueAngleCanvas.getHeight() - cueAngleBaseVer - y;
        double rad = Math.atan2(relY, relX);
        double deg = Math.toDegrees(rad);
        cueAngleDeg = Math.min(MAX_CUE_ANGLE, Math.max(0, deg));
        recalculateUiRestrictions();
        setCueAngleLabel();
    }

    private void restoreCueAngle() {
        cueAngleDeg = 5.0;
        setCueAngleLabel();
    }

    private void setCueAngleLabel() {
        cueAngleLabel.setText(String.format("%.1f°", cueAngleDeg));
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

    private void onCueAngleCanvasClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            setCueAnglePoint(mouseEvent.getX(), mouseEvent.getY());
        } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            restoreCueAngle();
        }
    }

    private void onCueAngleCanvasDragged(MouseEvent mouseEvent) {
        setCueAnglePoint(mouseEvent.getX(), mouseEvent.getY());
    }

    private void onSingleClick(MouseEvent mouseEvent) {
        System.out.println("Clicked!");
        if (replay != null) return;
        if (aiCalculating) return;
        if (game.getGame().getCuingPlayer().getInGamePlayer().getPlayerType() ==
                PlayerType.COMPUTER) return;

        if (game.getGame().getCueBall().isPotted()) {
            game.getGame().placeWhiteBall(realX(mouseEvent.getX()), realY(mouseEvent.getY()));
            game.getGame().getRecorder().writeBallInHandPlacement();
        } else if (!game.getGame().isCalculating() && movement == null) {
            Ball whiteBall = game.getGame().getCueBall();
            double[] unit = Algebra.unitVector(
                    new double[]{
                            realX(mouseEvent.getX()) - whiteBall.getX(),
                            realY(mouseEvent.getY()) - whiteBall.getY()
                    });
            cursorDirectionUnitX = unit[0];
            cursorDirectionUnitY = unit[1];
            recalculateUiRestrictions();
            System.out.println("New direction: " + cursorDirectionUnitX + ", " + cursorDirectionUnitY);
        }
    }

    private void onMouseMoved(MouseEvent mouseEvent) {
        mouseX = mouseEvent.getX();
        mouseY = mouseEvent.getY();
    }

    private void onDragStarted(MouseEvent mouseEvent) {
        Ball white = game.getGame().getCueBall();
        if (white.isPotted()) return;
        isDragging = true;
        double xDiffToWhite = realX(mouseEvent.getX()) - white.getX();
        double yDiffToWhite = realY(mouseEvent.getY()) - white.getY();
        lastDragAngle = Algebra.thetaOf(new double[]{xDiffToWhite, yDiffToWhite});
        if (cursorDirectionUnitX == 0.0 && cursorDirectionUnitY == 0.0) {
            double[] unitVec = Algebra.unitVector(xDiffToWhite, yDiffToWhite);
            cursorDirectionUnitX = unitVec[0];
            cursorDirectionUnitY = unitVec[1];
            recalculateUiRestrictions();
        }
        stage.getScene().setCursor(Cursor.CLOSED_HAND);
    }

    private void onDragging(MouseEvent mouseEvent) {
        if (!isDragging) {
            return;
        }
        Ball white = game.getGame().getCueBall();
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
        recalculateUiRestrictions();

        lastDragAngle = currentAngle;
    }

    private void onDragEnd(MouseEvent mouseEvent) {
        isDragging = false;
        lastDragAngle = 0.0;
        stage.getScene().setCursor(Cursor.DEFAULT);
    }

    private void startGame(int totalFrames) {
        game = new EntireGame(this, player1, player2, gameType, totalFrames);
        powerSlider.setMajorTickUnit(
                game.getGame().getCuingPlayer().getPlayerPerson().getControllablePowerPercentage());
    }

    @FXML
    void saveGameAction() {
        game.save();
    }

    @FXML
    void terminateAction() {
        game.getGame().forcedTerminate();
        movement = null;
        playingMovement = false;
        setButtonsCueEnd(game.getGame().getCuingPlayer());
    }

    @FXML
    void testAction() {
        movement = game.getGame().collisionTest();
        playMovement();
    }

    @FXML
    void tieTestAction() {
        game.getGame().tieTest();
        drawTargetBoard();
        drawScoreBoard(game.getGame().getCuingPlayer());
    }

    @FXML
    void clearRedBallsAction() {
        game.getGame().clearRedBallsTest();
        drawTargetBoard();
    }

    @FXML
    void p1AddScoreAction() {
        game.getGame().getPlayer1().addScore(10);
    }

    @FXML
    void p2AddScoreAction() {
        game.getGame().getPlayer2().addScore(10);
    }

    @FXML
    void withdrawAction() {
        if (game.getGame() instanceof AbstractSnookerGame) {
            Player curPlayer = game.getGame().getCuingPlayer();
            int diff = ((AbstractSnookerGame) game.getGame()).getScoreDiff((SnookerPlayer) curPlayer);
            String behindText = diff <= 0 ? "落后" : "领先";
            AlertShower.askConfirmation(
                    stage,
                    String.format("%s%d分，台面剩余%d分，真的要认输吗？", behindText, Math.abs(diff),
                            ((AbstractSnookerGame) game.getGame()).getRemainingScore()),
                    String.format("%s, 确认要认输吗？", curPlayer.getPlayerPerson().getName()),
                    () -> withdraw(curPlayer),
                    null
            );
        } else {
            Player curPlayer = game.getGame().getCuingPlayer();
            AlertShower.askConfirmation(
                    stage,
                    "......",
                    String.format("%s, 确认要认输吗？", curPlayer.getPlayerPerson().getName()),
                    () -> withdraw(curPlayer),
                    null
            );
        }
    }

    private void withdraw(Player curPlayer) {
        game.getGame().withdraw(curPlayer);
        endFrame();
    }

    @FXML
    void cueAction() {
        if (game.getGame().isEnded() || cueAnimationPlayer != null) return;

        if (replay != null) {
            return;
        }

        Player player = game.getGame().getCuingPlayer();
        if (player.getInGamePlayer().getPlayerType() == PlayerType.COMPUTER) {
            setButtonsCueStart();
            aiCue(player);
        } else {

            if (cursorDirectionUnitX == 0.0 && cursorDirectionUnitY == 0.0) return;
            setButtonsCueStart();
            playerCue(player);
        }
    }

    private CuePlayParams applyRandomCueError(Player player) {
        Random random = new Random();
        return applyCueError(player,
                random.nextGaussian(), random.nextGaussian(), random.nextGaussian(), true);
    }

    /**
     * 三个factor都是指几倍标准差
     */
    private CuePlayParams applyCueError(Player player,
                                        double powerFactor, double frontBackSpinFactor, double sideSpinFactor,
                                        boolean mutate) {
        Cue cue = player.getInGamePlayer().getCurrentCue(game.getGame());
        PlayerPerson playerPerson = player.getPlayerPerson();

        double power = getPowerPercentage();
        final double wantPower = power;
        // 因为力量控制导致的力量偏差
        powerFactor = powerFactor * (100.0 - playerPerson.getPowerControl()) / 100.0;
        powerFactor *= cue.powerMultiplier;  // 发力范围越大的杆控力越粗糙
        powerFactor *= wantPower / 40;  // 用力越大误差越大
        if (enablePsy) {
            double psyPowerMul = getPsyControlMultiplier(playerPerson);
            powerFactor /= psyPowerMul;
        }
        power += power * powerFactor;
//        System.out.println("Want power: " + wantPower + ", actual power: " + power);

//        if (mutate) {
        intentCuePointX = cuePointX;
        intentCuePointY = cuePointY;
        // 因为出杆质量而导致的打点偏移
//        }

        double cpx = cuePointX;
        double cpy = cuePointY;

        int counter = 0;
        while (counter < 10) {
            double xError = sideSpinFactor;
            double yError = frontBackSpinFactor;
            double[] muSigXy = playerPerson.getCuePointMuSigmaXY();
            double xSig = muSigXy[1];
            double ySig = -muSigXy[3];

            double mulWithPower = getErrorMultiplierOfPower(playerPerson, getSelectedPower());

            xError = xError * xSig + muSigXy[0];
            yError = yError * ySig + muSigXy[2];
            xError = xError * mulWithPower * cueAreaRadius / 200;
            yError = yError * mulWithPower * cueAreaRadius / 200;
            cpx = intentCuePointX + xError;
            cpy = intentCuePointY + yError;
            if (mutate) {
                cuePointX = cpx;
                cuePointY = cpy;
            }

            if (obstacleProjection == null || obstacleProjection.cueAble(
                    getCuePointRelX(cpx), getCuePointRelY(cpy),
                    getRatioOfCueAndBall())) {
                break;
            }

            counter++;
        }
        if (mutate && counter == 10) {
            System.out.println("Failed to find a random cueAble position");
            cuePointX = intentCuePointX;
            cuePointY = intentCuePointY;
        }

        if (mutate) {
            System.out.println("intent: " + intentCuePointX + ", " + intentCuePointY);
            System.out.println("actual: " + cuePointX + ", " + cuePointY);
        }

//        System.out.println(cpx + " " + cuePointX);
        double unitSideSpin = getUnitSideSpin(cpx);

        if (mutate) {
            if (Algebra.distanceToPoint(cuePointX, cuePointY, cueCanvasWH / 2, cueCanvasWH / 2)
                    > cueAreaRadius - cueRadius) {
                power /= 3;
                unitSideSpin *= 10;
                System.out.println("滑杆了！");
            }
        }

//        double[] unitXYWithSpin = getUnitXYWithSpins(unitSideSpin, power);

        return generateCueParams(power, getUnitFrontBackSpin(cpy), unitSideSpin, cueAngleDeg);
    }

    private CueRecord makeCueRecord(Player cuePlayer, CuePlayParams paramsWithError) {
        System.out.println("Target " + game.getGame().getCurrentTarget());
        return new CueRecord(cuePlayer.getInGamePlayer(),
                game.getGame().getCurrentTarget(),
                game.getGame().isBreaking(),
                game.getGame().isDoingSnookerFreeBll(),
                getSelectedPower(),
                paramsWithError.power,
                cursorDirectionUnitX,
                cursorDirectionUnitY,
                getCuePointRelY(intentCuePointY),
                getCuePointRelX(intentCuePointX),
                getCuePointRelY(cuePointY),
                getCuePointRelX(cuePointX),
                cueAngleDeg);

    }

    private void playerCue(Player player) {
        // 判断是否为进攻杆
        PotAttempt currentAttempt = null;
        if (predictedTargetBall != null) {
            List<double[][]> holeDirectionsAndHoles =
                    game.getGame().directionsToAccessibleHoles(predictedTargetBall);
            for (double[][] directionHole : holeDirectionsAndHoles) {
                double pottingDirection = Algebra.thetaOf(directionHole[0]);
                double aimingDirection =
                        Algebra.thetaOf(targetPredictionUnitX, targetPredictionUnitY);

                double angleBtw = Math.abs(pottingDirection - aimingDirection);
//                if (angleBtw > Math.PI) {
//                    angleBtw = Math.PI * 2 - angleBtw;
//                    System.out.println("旧bug：判断进攻杆");
//                }

                if (angleBtw <= Game.MAX_ATTACK_DECISION_ANGLE) {
                    currentAttempt = new PotAttempt(
                            gameType,
                            game.getGame().getCuingPlayer().getPlayerPerson(),
                            predictedTargetBall,
                            new double[]{game.getGame().getCueBall().getX(),
                                    game.getGame().getCueBall().getY()},
                            new double[]{predictedTargetBall.getX(), predictedTargetBall.getY()},
                            directionHole[1]
                    );
                    System.out.printf("Angle is %f, attacking!\n",
                            Math.toDegrees(Math.abs(pottingDirection - aimingDirection)));
                    break;
                }
            }
        }

        CuePlayParams params = applyRandomCueError(player);
        CueRecord cueRecord = makeCueRecord(player, params);  // 必须在randomCueError之后
        game.getGame().getRecorder().recordCue(cueRecord);

        double[] unitXYWithSpin = getUnitXYWithSpins(params.sideSpin, params.power);

        double whiteStartingX = game.getGame().getCueBall().getX();
        double whiteStartingY = game.getGame().getCueBall().getY();

        PredictedPos predictionWithRandom = game.getGame().getPredictedHitBall(
                whiteStartingX, whiteStartingY,
                unitXYWithSpin[0], unitXYWithSpin[1]);
        if (predictionWithRandom == null) {
            predictionOfCue = new SavedPrediction(whiteStartingX, whiteStartingY,
                    unitXYWithSpin[0], unitXYWithSpin[1]);
        } else {
            predictionOfCue = new SavedPrediction(whiteStartingX, whiteStartingY,
                    predictionWithRandom.getTargetBall(),
                    predictionWithRandom.getTargetBall().getX(),
                    predictionWithRandom.getTargetBall().getY(),
                    predictionWithRandom.getPredictedWhitePos()[0],
                    predictionWithRandom.getPredictedWhitePos()[1]);
        }

        movement = game.getGame().cue(params);
        game.getGame().getRecorder().recordMovement(movement);

        if (currentAttempt != null) {
            boolean success = currentAttempt.getTargetBall().isPotted();
            if (curDefAttempt != null && curDefAttempt.defensePlayer != player) {
                // 如进攻成功，则上一杆防守失败了
                curDefAttempt.setSuccess(!success);
                if (success) {
                    System.out.println(curDefAttempt.defensePlayer.getPlayerPerson().getName() +
                            " defense failed!");
                }
            }
            currentAttempt.setSuccess(success);
            player.addAttempt(currentAttempt);
            if (success) {
                System.out.println("Pot success!");
            } else {
                System.out.println("Pot failed!");
            }
            curDefAttempt = null;
        } else {
            // 防守
            curDefAttempt = new DefenseAttempt(player);
            player.addDefenseAttempt(curDefAttempt);
            System.out.println("Defense!");
        }

        beginCueAnimationOfHumanPlayer(whiteStartingX, whiteStartingY);
    }

    private void aiCue(Player player) {
        cueButton.setText("正在思考");
        cueButton.setDisable(true);
        aiCalculating = true;
        Thread aiCalculation = new Thread(() -> {
            System.out.println("ai cue");
            long st = System.currentTimeMillis();
            if (game.getGame().isBallInHand()) {
                System.out.println("AI is trying to place ball");
                double[] pos = AiCueBallPlacer.createAiCueBallPlacer(game.getGame(), player)
                        .getPositionToPlaceCueBall();
                game.getGame().placeWhiteBall(pos[0], pos[1]);
                game.getGame().getRecorder().writeBallInHandPlacement();
                Platform.runLater(this::draw);
            }
            AiCueResult cueResult = game.getGame().aiCue(player);
            System.out.println("Ai calculation ends in " + (System.currentTimeMillis() - st) + " ms");
            if (cueResult == null) {
                aiCalculating = false;
                withdraw(player);
                return;
            }
            Platform.runLater(() -> {
                cueButton.setText("正在击球");
                cursorDirectionUnitX = cueResult.getUnitX();
                cursorDirectionUnitY = cueResult.getUnitY();
                powerSlider.setValue(cueResult.getSelectedPower());
                cuePointX = cueCanvasWH / 2;
                cuePointY = cueCanvasWH / 2 - cueResult.getSelectedFrontBackSpin() * cueAreaRadius;
                cueAngleDeg = 5.0;

                CuePlayParams realParams = applyRandomCueError(player);
                CueRecord cueRecord = makeCueRecord(player, realParams);  // 必须在randomCueError之后
                game.getGame().getRecorder().recordCue(cueRecord);

                double whiteStartingX = game.getGame().getCueBall().getX();
                double whiteStartingY = game.getGame().getCueBall().getY();
                movement = game.getGame().cue(realParams);
                game.getGame().getRecorder().recordMovement(movement);

                aiCalculating = false;
                if (cueResult.isAttack()) {
                    PotAttempt currentAttempt = new PotAttempt(
                            gameType,
                            game.getGame().getCuingPlayer().getPlayerPerson(),
                            cueResult.getTargetBall(),
                            new double[]{whiteStartingX, whiteStartingY},
                            cueResult.getTargetOrigPos(), 
                            cueResult.getTargetDirHole()[1]
                    );
                    boolean success = currentAttempt.getTargetBall().isPotted();
                    if (curDefAttempt != null && curDefAttempt.defensePlayer != player) {
                        // 如进攻成功，则上一杆防守失败了
                        curDefAttempt.setSuccess(!success);
                        if (success) {
                            System.out.println(curDefAttempt.defensePlayer.getPlayerPerson().getName() +
                                    " player defense failed!");
                        }
                    }
                    currentAttempt.setSuccess(success);
                    player.addAttempt(currentAttempt);
                    if (success) {
                        System.out.println("AI Pot success!");
                    } else {
                        System.out.println("AI Pot failed!");
                    }
                    curDefAttempt = null;
                } else {
                    curDefAttempt = new DefenseAttempt(player);
                    player.addDefenseAttempt(curDefAttempt);
                    System.out.println("AI Defense!");
                }
                
                beginCueAnimation(game.getGame().getCuingPlayer().getInGamePlayer(),
                        whiteStartingX, whiteStartingY, cueResult.getSelectedPower(),
                        cueResult.getUnitX(), cueResult.getUnitY());
            });
        });
        aiCalculation.start();
    }

    private void replayCue() {
        if (replay.getCurrentFlag() == GameRecorder.FLAG_CUE) {
            CueRecord cueRecord = replay.getCueRecord();
            if (cueRecord == null) return;
            movement = replay.getMovement();
            System.out.println(movement.getMovementMap().get(replay.getCueBall()).size());

            cursorDirectionUnitX = cueRecord.aimUnitX;
            cursorDirectionUnitY = cueRecord.aimUnitY;

            intentCuePointX = getCuePointCanvasX(cueRecord.intendedHorPoint);
            intentCuePointY = getCuePointCanvasY(cueRecord.intendedVerPoint);
            cuePointX = getCuePointCanvasX(cueRecord.actualHorPoint);
            cuePointY = getCuePointCanvasY(cueRecord.actualVerPoint);
            cueAngleDeg = cueRecord.cueAngle;

            powerSlider.setValue(cueRecord.actualPower);
            Platform.runLater(() -> powerSlider.setMajorTickUnit(
                    cueRecord.cuePlayer.getPlayerPerson().getControllablePowerPercentage()));

            Ball cueBall = replay.getCueBall();
            MovementFrame cueBallPos = movement.getStartingPositions().get(cueBall);
            beginCueAnimation(cueRecord.cuePlayer, cueBallPos.x, cueBallPos.y,
                    cueRecord.selectedPower, cueRecord.aimUnitX, cueRecord.aimUnitY);
        } else if (replay.getCurrentFlag() == GameRecorder.FLAG_HANDBALL) {
            System.out.println("Ball in hand!");
            drawTargetBoard();
            replayStopTime = System.currentTimeMillis();
            if (!replay.loadNext()) {
                System.out.println("Finished with a hand ball?");
            }
        }
    }

    @FXML
    void newGameAction() {
        Platform.runLater(() -> {
            AlertShower.askConfirmation(
                    stage,
                    "真的要开始新游戏吗？", "请确认",
                    () -> {
                        startGame(game.totalFrames);
                        drawTargetBoard();
                        drawScoreBoard(game.getGame().getCuingPlayer());
                    },
                    null
            );

//            if (NativeAlertShower.askConfirmation(stage, "真的要开始新游戏吗？", "请确认")) {
//                startGame(game.totalFrames);
//                drawTargetBoard();
//                drawScoreBoard(game.getGame().getCuingPlayer());
//            }
        });
    }

    @FXML
    void settingsAction() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("settingsView.fxml")
        );
        Parent root = loader.load();
        root.setStyle(App.FONT_STYLE);

        Stage newStage = new Stage();
        newStage.initOwner(stage);

        Scene scene = new Scene(root);
        newStage.setScene(scene);

        SettingsView view = loader.getController();
        view.setup(newStage, this);

        newStage.show();
    }

    private CuePlayParams generateCueParams() {
        return generateCueParams(getPowerPercentage());
    }

    private CuePlayParams[] generateCueParamsSd1() {
        Player player = game.getGame().getCuingPlayer();
        double sd = 1;
        double corner = Math.sqrt(2) / 2 * sd;
        CuePlayParams[] res = new CuePlayParams[9];
        res[0] = generateCueParams();
//        res[1] = applyCueError(player, -sd, -sd, 0, false);  // 又小又低
//        res[2] = applyCueError(player, -corner, -corner, -corner, false);  // 偏小，左下
//        res[3] = applyCueError(player, 0, 0, -sd, false);  // 最左
//        res[4] = applyCueError(player, corner, corner, -corner, false);  // 偏大，左上
//        res[5] = applyCueError(player, sd, sd, 0, false);  // 又大又高
//        res[6] = applyCueError(player, corner, corner, corner, false);  // 偏大，右上
//        res[7] = applyCueError(player, 0, 0, sd, false);  // 最右
//        res[8] = applyCueError(player, -corner, -corner, corner, false);  // 偏小，右下

        res[1] = applyCueError(player, -sd, sd, 0, false);  // 又小又低
        res[2] = applyCueError(player, -corner, corner, -corner, false);  // 偏小，左下
        res[3] = applyCueError(player, 0, 0, -sd, false);  // 最左
        res[4] = applyCueError(player, corner, -corner, -corner, false);  // 偏大，左上
        res[5] = applyCueError(player, sd, -sd, 0, false);  // 又大又高
        res[6] = applyCueError(player, corner, -corner, corner, false);  // 偏大，右上
        res[7] = applyCueError(player, 0, 0, sd, false);  // 最右
        res[8] = applyCueError(player, -corner, corner, corner, false);  // 偏小，右下
        return res;
    }

    private CuePlayParams generateCueParams(double power) {
        return generateCueParams(power, getUnitSideSpin(), cueAngleDeg);
    }

    private CuePlayParams generateCueParams(double power, double unitSideSpin,
                                            double cueAngleDeg) {
        return generateCueParams(power, getUnitFrontBackSpin(), unitSideSpin, cueAngleDeg);
    }

    private CuePlayParams generateCueParams(double power,
                                            double unitFrontBackSpin,
                                            double unitSideSpin,
                                            double cueAngleDeg) {
        return CuePlayParams.makeIdealParams(cursorDirectionUnitX, cursorDirectionUnitY,
                unitFrontBackSpin, unitSideSpin,
                cueAngleDeg, power);
    }

    void setDifficulty(SettingsView.Difficulty difficulty) {
        if (difficulty == SettingsView.Difficulty.EASY) {
//            minRealPredictLength = 600.0;
            maxRealPredictLength = DEFAULT_MAX_PREDICT_LENGTH * 1.5;
        } else if (difficulty == SettingsView.Difficulty.MEDIUM) {
//            minRealPredictLength = 300.0;
            maxRealPredictLength = DEFAULT_MAX_PREDICT_LENGTH;
        } else if (difficulty == SettingsView.Difficulty.HARD) {
//            minRealPredictLength = 150.0;
            maxRealPredictLength = DEFAULT_MAX_PREDICT_LENGTH * 0.5;
        }
    }

    private void setButtonsCueStart() {
        withdrawMenu.setDisable(true);
        cueButton.setDisable(true);
    }

    private void setButtonsCueEnd(Player nextCuePlayer) {
        cueButton.setDisable(false);

        if (nextCuePlayer.getInGamePlayer().getPlayerType() == PlayerType.PLAYER) {
            cueButton.setText("击球");
            withdrawMenu.setDisable(false);
        } else {
            cueButton.setText("电脑击球");
            withdrawMenu.setDisable(true);
        }
    }

    private double getUnitFrontBackSpin() {
        return getUnitFrontBackSpin(cuePointY);
    }

    private double getUnitFrontBackSpin(double cpy) {
        Cue cue;
        if (replay != null) {
            cue = replay.getCurrentCue();
        } else {
            cue = game.getGame().getCuingPlayer().getInGamePlayer().getCurrentCue(game.getGame());
        }
        return CuePlayParams.unitFrontBackSpin((cueCanvasWH / 2 - cpy) / cueAreaRadius,
                game.getGame().getCuingPlayer().getInGamePlayer(),
                cue
        );
    }

    private double getUnitSideSpin() {
        return getUnitSideSpin(cuePointX);
    }

    private double getUnitSideSpin(double cpx) {
        Cue cue;
        if (replay != null) {
            cue = replay.getCurrentCue();
        } else {
            cue = game.getGame().getCuingPlayer().getInGamePlayer().getCurrentCue(game.getGame());
        }
        return CuePlayParams.unitSideSpin((cpx - cueCanvasWH / 2) / cueAreaRadius,
                cue);
    }

    /**
     * 返回受到侧塞影响的白球单位向量
     */
    private double[] getUnitXYWithSpins(double unitSideSpin, double powerPercentage) {
        return CuePlayParams.unitXYWithSpins(unitSideSpin, powerPercentage,
                cursorDirectionUnitX, cursorDirectionUnitY);
    }

    /**
     * @return 力量槽选的力量
     */
    private double getSelectedPower() {
        return Math.max(powerSlider.getValue(), 0.01);
    }

    private double getPowerPercentage() {
        return getPowerPercentage(getSelectedPower());
    }

    private double getPowerPercentage(double selectedPower) {
        return selectedPower / game.getGame().getGameValues().ballWeightRatio *
                game.getGame().getCuingPlayer().getInGamePlayer().getCurrentCue(
                        game.getGame()).powerMultiplier;
    }

    private double[] calculateSpins(double vx, double vy) {
        double speed = Math.hypot(vx, vy);

        double frontBackSpin = getUnitFrontBackSpin();  // 高杆正，低杆负
        double leftRightSpin = getUnitSideSpin();  // 右塞正（顶视逆时针），左塞负
        if (frontBackSpin > 0) {
            // 高杆补偿
            frontBackSpin *= 1.25;
        }

        // 小力高低杆补偿
        double spinRatio = Math.pow(speed / Values.MAX_POWER_SPEED, 0.45);
        double sideSpinRatio = Math.pow(speed / Values.MAX_POWER_SPEED, 0.75);

        double side = sideSpinRatio * leftRightSpin * Values.MAX_SIDE_SPIN_SPEED;
        // 旋转产生的总目标速度
        double spinSpeed = spinRatio * frontBackSpin * Values.MAX_SPIN_SPEED;

        // (spinX, spinY)是一个向量，指向球因为旋转想去的方向
        double spinX = vx * (spinSpeed / speed);
        double spinY = vy * (spinSpeed / speed);
//        System.out.printf("x %f, y %f, total %f, side %f\n", spinX, spinY, spinSpeed, side);

        double mbummeMag = cueAngleDeg / 90.0 / 1000;  // 扎杆强度
        double[] norm = Algebra.normalVector(vx, vy);  // 法线，扎杆就在这个方向
        double mbummeX = side * -norm[0] * mbummeMag;
        double mbummeY = side * -norm[1] * mbummeMag;

        if (cueAngleDeg > 5) {
            // 扎杆在一定程度上减弱其他杆法
            double mul = (95 - cueAngleDeg) / 90.0;
            side *= mul;
            spinX *= mul;
            spinY *= mul;
        }

//        System.out.printf("spin x: %f, spin y: %f, mx: %f, my: %f\n",
//                spinX, spinY, mbummeX, mbummeY);

        return new double[]{spinX + mbummeX, spinY + mbummeY, side};
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
        if (aiCalculating) return;
        draw();
        drawCueBallCanvas();
        drawCueAngleCanvas();
        drawCue();
    }

    private void setupCanvas() {
        gameCanvas.setWidth(canvasWidth);
        gameCanvas.setHeight(canvasHeight);
    }

    private void setupPowerSlider() {
        powerSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            if (game != null) {
                double playerMaxPower = game.getGame().getCuingPlayer().getPlayerPerson().getMaxPowerPercentage();
                if (newValue.doubleValue() > playerMaxPower) {
                    powerSlider.setValue(playerMaxPower);
                    return;
                }
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

        cueAngleCanvas.setOnMouseClicked(this::onCueAngleCanvasClicked);
        cueAngleCanvas.setOnMouseDragged(this::onCueAngleCanvasDragged);
    }

    private void drawBallInHand() {
        if (replay != null) return;

        if (!game.getGame().isCalculating() &&
                movement == null &&
                game.getGame().isBallInHand() &&
                game.getGame().getCuingPlayer().getInGamePlayer().getPlayerType() == PlayerType.PLAYER) {
            GameValues values = game.getGame().getGameValues();

            double x = realX(mouseX);
            if (x < values.leftX + values.ballRadius) x = values.leftX + values.ballRadius;
            else if (x >= values.rightX - values.ballRadius) x = values.rightX - values.ballRadius;

            double y = realY(mouseY);
            if (y < values.topY + values.ballRadius) y = values.topY + values.ballRadius;
            else if (y >= values.botY - values.ballRadius) y = values.botY - values.ballRadius;

            game.getGame().getTable().forcedDrawWhiteBall(
                    this,
                    x,
                    y,
                    graphicsContext,
                    scale
            );
        }
    }

    private void drawTable() {
        GameValues values = gameType.gameValues;

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
        if (replay != null) {
            replay.table.drawTableMarks(this, graphicsContext, scale);
        } else {
            game.getGame().getTable().drawTableMarks(this, graphicsContext, scale);
        }
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
        if (playingMovement) {
            boolean isLast = false;
            for (Map.Entry<Ball, Deque<MovementFrame>> entry :
                    movement.getMovementMap().entrySet()) {
                MovementFrame frame = entry.getValue().removeFirst();
                if (!frame.potted) {
                    if (replay != null) {
                        replay.table.forceDrawBall(this, entry.getKey(), frame.x, frame.y, graphicsContext, scale);
                    } else {
                        game.getGame().getTable().forceDrawBall(this, entry.getKey(), frame.x, frame.y, graphicsContext, scale);
                    }
                }
                if (entry.getValue().isEmpty()) {
                    isLast = true;
                }
                switch (frame.movementType) {
                    case MovementFrame.COLLISION:
                        break;
                    case MovementFrame.CUSHION:
                        GameAudio.hitCushion(gameType.gameValues, frame.movementValue);
                        break;
                    case MovementFrame.POT:
                        GameAudio.pot(gameType.gameValues, frame.movementValue);
                        break;
                }
            }
            if (isLast) {
                playingMovement = false;
                movement = null;
                if (replay != null) finishCueReplay();
                else game.getGame().finishMove();
            }
        } else {
            if (movement == null) {
                if (replay != null) {
                    replay.getTable().drawStoppedBalls(this, replay.getAllBalls(), replay.getLastPositions(), graphicsContext, scale);

                    if (!replay.finished() && System.currentTimeMillis() - replayStopTime > replayGap) {
                        replayCue();
                    }
                } else {
                    game.getGame().getTable().drawStoppedBalls(this, game.getGame().getAllBalls(), null, graphicsContext, scale);
                }
            } else {
                // 已经算出，但还在放运杆动画
                for (Map.Entry<Ball, MovementFrame> entry : movement.getStartingPositions().entrySet()) {
                    MovementFrame frame = entry.getValue();
                    if (!frame.potted) {
                        if (replay != null)
                            replay.getTable().forceDrawBall(this, entry.getKey(), frame.x, frame.y, graphicsContext, scale);
                        else
                            game.getGame().getTable().forceDrawBall(this, entry.getKey(), frame.x, frame.y, graphicsContext, scale);
                    }
                }
            }
        }
    }

    private void drawScoreBoard(Player cuePlayer) {
        // TODO
        if (replay != null) {
            Platform.runLater(() -> {
                player1FramesLabel.setText(String.valueOf(replay.getItem().p1Wins));
                player2FramesLabel.setText(String.valueOf(replay.getItem().p2Wins));

                singlePoleCanvas.getGraphicsContext2D().setFill(WHITE);
                singlePoleCanvas.getGraphicsContext2D().fillRect(0, 0,
                        singlePoleCanvas.getWidth(), singlePoleCanvas.getHeight());

                if (gameType.snookerLike) {
                    SnookerScoreResult ssr = (SnookerScoreResult) replay.getScoreResult();
                    player1ScoreLabel.setText(String.valueOf(ssr.getP1TotalScore()));
                    player2ScoreLabel.setText(String.valueOf(ssr.getP2TotalScore()));
                    drawSnookerSinglePoles(ssr.getSinglePoleMap());
                    singlePoleLabel.setText(String.valueOf(ssr.getSinglePoleScore()));
                } else if (gameType == GameType.CHINESE_EIGHT) {
                    ChineseEightScoreResult csr = (ChineseEightScoreResult) replay.getScoreResult();
//                    List<PoolBall> rems = ChineseEightTable.filterRemainingTargetOfPlayer(replay.getCueRecord().targetRep, replay);
                    List<PoolBall> rems = replay.getCueRecord().cuePlayer.getPlayerNumber() == 1 ?
                            csr.getP1Rems() : csr.getP2Rems();
                    System.out.println(rems.size());
                    drawChineseEightAllTargets(rems);
                }
            });
        } else {
            Platform.runLater(() -> {
                player1ScoreLabel.setText(String.valueOf(game.getGame().getPlayer1().getScore()));
                player2ScoreLabel.setText(String.valueOf(game.getGame().getPlayer2().getScore()));
                player1FramesLabel.setText(String.valueOf(game.getP1Wins()));
                player2FramesLabel.setText(String.valueOf(game.getP2Wins()));

                singlePoleCanvas.getGraphicsContext2D().setFill(WHITE);
                singlePoleCanvas.getGraphicsContext2D().fillRect(0, 0,
                        singlePoleCanvas.getWidth(), singlePoleCanvas.getHeight());

                if (game.gameType.snookerLike) {
                    drawSnookerSinglePoles(cuePlayer.getSinglePole());
                    singlePoleLabel.setText(String.valueOf(cuePlayer.getSinglePoleScore()));
                } else if (gameType == GameType.CHINESE_EIGHT ||
                        gameType == GameType.SIDE_POCKET) {
                    if (cuePlayer == game.getGame().getCuingPlayer()) {
                        // 进攻成功了
                        drawNumberedAllTargets((NumberedBallGame) game.getGame(),
                                (NumberedBallPlayer) cuePlayer);
                    } else {
                        // 进攻失败了
                        drawNumberedAllTargets((NumberedBallGame) game.getGame(),
                                (NumberedBallPlayer) game.getGame().getCuingPlayer());
                    }
                }
            });
        }
    }

    private void drawTargetBoard() {
        Platform.runLater(() -> {
            if (gameType.snookerLike)
                drawSnookerTargetBoard();
            else if (gameType == GameType.CHINESE_EIGHT)
                drawPoolTargetBoard();
        });
    }

    private void drawSnookerTargetBoard() {
        int tar;
        boolean p1;
        boolean snookerFreeBall;
        if (replay != null) {
            CueRecord cueRecord = replay.getCueRecord();
            if (cueRecord == null) return;
            p1 = cueRecord.cuePlayer.getPlayerNumber() == 1;
            tar = cueRecord.targetRep;
            snookerFreeBall = cueRecord.isSnookerFreeBall;
            System.out.println(p1 + " " + tar);
        } else {
            AbstractSnookerGame game1 = (AbstractSnookerGame) game.getGame();
            p1 = game1.getCuingPlayer().getInGamePlayer().getPlayerNumber() == 1;
            tar = game1.getCurrentTarget();
            snookerFreeBall = game1.isDoingFreeBall();
        }
        if (p1) {
            drawSnookerTargetBall(player1TarCanvas, tar, snookerFreeBall);
            wipeCanvas(player2TarCanvas);
        } else {
            drawSnookerTargetBall(player2TarCanvas, tar, snookerFreeBall);
            wipeCanvas(player1TarCanvas);
        }
    }

    private void drawPoolTargetBoard() {
        int tar;
        boolean p1;
        if (replay != null) {
            CueRecord cueRecord = replay.getCueRecord();
            if (cueRecord == null) return;
            p1 = cueRecord.cuePlayer.getPlayerNumber() == 1;
            tar = cueRecord.targetRep;
        } else {
            NumberedBallGame<?> game1 = (NumberedBallGame<?>) game.getGame();
            p1 = game1.getCuingPlayer().getInGamePlayer().getPlayerNumber() == 1;
            tar = game1.getCurrentTarget();
        }

        if (p1) {
            drawPoolTargetBall(player1TarCanvas, tar);
            wipeCanvas(player2TarCanvas);
        } else {
            drawPoolTargetBall(player2TarCanvas, tar);
            wipeCanvas(player1TarCanvas);
        }
    }

    private void drawChineseEightAllTargets(List<PoolBall> targets) {
        double x = ballDiameter * 0.6;
        double y = ballDiameter * 0.6;

        if (gameType == GameType.CHINESE_EIGHT) {
            for (PoolBall ball : targets) {
                NumberedBallTable.drawPoolBallEssential(
                        x, y, ballDiameter, ball.getColor(), ball.getValue(),
                        singlePoleCanvas.getGraphicsContext2D());
                x += ballDiameter * 1.2;
            }
        }
    }

    private void drawNumberedAllTargets(NumberedBallGame frame, NumberedBallPlayer player) {
        if (frame instanceof ChineseEightBallGame) {
            int ballRange = ((ChineseEightBallPlayer) player).getBallRange();
            List<PoolBall> targets = ChineseEightTable.filterRemainingTargetOfPlayer(
                    ballRange, frame
            );
            drawChineseEightAllTargets(targets);
        }
//        double x = ballDiameter * 0.6;
//        double y = ballDiameter * 0.6;
//        if (frame.getCurrentTarget() != 0) {
//            if (frame instanceof ChineseEightBallGame) {
//                int target = ((ChineseEightBallPlayer) player).getBallRange();
//                List<PoolBall> targets = ChineseEightTable.filterRemainingTargetOfPlayer(
//                        target, frame
//                );
//                for (PoolBall ball: targets) {
//                    NumberedBallTable.drawPoolBallEssential(
//                            x, y, ballDiameter, ball.getColor(), ball.getValue(),
//                            singlePoleCanvas.getGraphicsContext2D());
//                    x += ballDiameter * 1.2;
//                }
//            }
//        }
    }

    private void drawSnookerSinglePoles(SortedMap<Ball, Integer> singlePoleBalls) {
        GraphicsContext gc = singlePoleCanvas.getGraphicsContext2D();
        double x = 0;
        double y = ballDiameter * 0.1;
        double textY = ballDiameter * 0.8;
        for (Map.Entry<Ball, Integer> ballCount : singlePoleBalls.entrySet()) {
            // 这里是利用了TreeMap和comparable的特性
            Ball ball = ballCount.getKey();
            gc.setFill(ball.getColor());
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
            NumberedBallTable.drawPoolBallEssential(
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
        Cue cue = game.getGame().getCuingPlayer().getInGamePlayer().getCurrentCue(game.getGame());

        // 最大的预测长度
        double origMaxLength = playerPerson.getPrecisionPercentage() / 100 *
                cue.accuracyMultiplier * maxRealPredictLength;
        // 只计算距离的最小长度
        double minLength = origMaxLength / 2.5 * playerPerson.getLongPrecision();

        double potDt2 = Math.max(potDt, maxPredictLengthPotDt);
        double dtRange = minPredictLengthPotDt - maxPredictLengthPotDt;
        double lengthRange = origMaxLength - minLength;
        double potDtInRange = (potDt2 - maxPredictLengthPotDt) / dtRange;
        double predictLength = origMaxLength - potDtInRange * lengthRange;

        double side = Math.abs(cuePointX - cueCanvasWH / 2) / cueCanvasWH;  // 0和0.5之间
        double afterSide = predictLength * (1 - side);  // 加塞影响瞄准
        double mul = 1 - Math.sin(Math.toRadians(cueAngleDeg)); // 抬高杆尾影响瞄准
        double res = afterSide * mul;
        if (enablePsy) {
            res *= getPsyAccuracyMultiplier(playerPerson);
        }
        return res;
    }

    private GamePlayStage gamePlayStage() {
        GamePlayStage playStage = game.getGame().getGamePlayStage(predictedTargetBall,
                printPlayStage);
        printPlayStage = false;
        return playStage;
    }

    private double getPsyAccuracyMultiplier(PlayerPerson playerPerson) {
        GamePlayStage stage = gamePlayStage();
        switch (stage) {
            case THIS_BALL_WIN:
            case ENHANCE_WIN:
                return playerPerson.psy / 100;
            default:
                return 1.0;
        }
    }

    private double getPsyControlMultiplier(PlayerPerson playerPerson) {
        GamePlayStage stage = gamePlayStage();
        switch (stage) {
            case THIS_BALL_WIN:
            case NEXT_BALL_WIN:
                return playerPerson.psy / 100;
            default:
                return 1.0;
        }
    }

    private void drawCursor() {
        if (replay != null) return;
        if (game.getGame().isEnded()) return;
        if (isPlayingMovement()) return;
        if (movement != null) return;
        if (cursorDirectionUnitX == 0.0 && cursorDirectionUnitY == 0.0) return;

//        long st = System.currentTimeMillis();
        CuePlayParams[] possibles = generateCueParamsSd1();
        WhitePrediction[] clockwise = new WhitePrediction[8];
        WhitePrediction center = game.getGame().predictWhite(
                possibles[0], Phy.PREDICT, whitePredictLenAfterWall, false, false, true);
        if (center == null) return;

//        PredictedPos coll = game.getGame().getPredictedHitBall(
//                center.whiteX, center.whiteY, cursorDirectionUnitX, cursorDirectionUnitY
//        );
//        if (coll != null)
//            SnookerTable.drawBallBase(
//                    canvasX(coll.getPredictedWhitePos()[0]),
//                    canvasY(coll.getPredictedWhitePos()[1]),
//                    52.5 * scale,
//                    Color.GRAY,
//                    graphicsContext
//            );

//        List<double[][]> g = game.getGame().directionsToAccessibleHoles(center.getFirstCollide());
//        for (double[][] gg : g) {
//            System.out.print(gameType.gameValues.getHoleOpenCenter(gg[1]) + ", ");
//        }
//        System.out.println();

//        Ball fc = center.getFirstCollide();
//        if (fc != null) {
//            Table.drawBallBase(canvasX(fc.getX()), canvasY(fc.getY()), 20, Color.GRAY, graphicsContext);
//        }
//        center.resetToInit();

        for (int i = 1; i < possibles.length; i++) {
            clockwise[i - 1] = game.getGame().predictWhite(
                    possibles[i], Phy.PREDICT, whitePredictLenAfterWall,
                    false, false, true
            );
        }

        double[][] predictionStops = new double[clockwise.length + 1][];
        predictionStops[0] = center.stopPoint();
        for (int i = 0; i < clockwise.length; i++) {
            predictionStops[i + 1] = clockwise[i].stopPoint();
        }
        List<double[]> outPoints = Algebra.grahamScanEnclose(predictionStops);

        graphicsContext.setStroke(WHITE.darker());
        for (int i = 1; i < outPoints.size(); i++) {
            connectEndPoint(outPoints.get(i - 1), outPoints.get(i), graphicsContext);
        }
        connectEndPoint(outPoints.get(outPoints.size() - 1), outPoints.get(0), graphicsContext);

        graphicsContext.setStroke(WHITE);
//        for (WhitePrediction predict : predictions) {
        double lastX = canvasX(game.getGame().getCueBall().getX());
        double lastY = canvasY(game.getGame().getCueBall().getY());
        for (double[] pos : center.getWhitePath()) {
            double canvasX = canvasX(pos[0]);
            double canvasY = canvasY(pos[1]);
            graphicsContext.strokeLine(lastX, lastY, canvasX, canvasY);
            lastX = canvasX;
            lastY = canvasY;
        }
//            graphicsContext.setStroke(Color.GRAY);
//        }
//        graphicsContext.setStroke(WHITE);
        if (center.getFirstCollide() != null) {
            graphicsContext.strokeOval(canvasX(center.getWhiteCollisionX()) - ballRadius,
                    canvasY(center.getWhiteCollisionY()) - ballRadius,
                    ballDiameter, ballDiameter);  // 绘制预测撞击点的白球

            // 弹库的球就不给预测线了
            if (!center.isHitWallBeforeHitBall()) {
                predictedTargetBall = center.getFirstCollide();
                double potDt = Algebra.distanceToPoint(
                        center.getWhiteCollisionX(), center.getWhiteCollisionY(),
                        center.whiteX, center.whiteY);
                // 白球行进距离越长，预测线越短
                double predictLineTotalLen = getPredictionLineTotalLength(potDt,
                        game.getGame().getCuingPlayer().getPlayerPerson());

                targetPredictionUnitY = center.getBallDirectionY();
                targetPredictionUnitX = center.getBallDirectionX();
                double whiteUnitXBefore = center.getWhiteDirectionXBeforeCollision();
                double whiteUnitYBefore = center.getWhiteDirectionYBeforeCollision();

                double theta = Algebra.thetaBetweenVectors(
                        targetPredictionUnitX, targetPredictionUnitY,
                        whiteUnitXBefore, whiteUnitYBefore
                );

                // 角度越大，目标球预测线越短
                double pureMultiplier = Algebra.powerTransferOfAngle(theta);
                PlayerPerson playerPerson = game.getGame().getCuingPlayer().getPlayerPerson();
                double multiplier = Math.pow(pureMultiplier, 1 / playerPerson.getAnglePrecision());

                double totalLen = predictLineTotalLen * multiplier;

                double lineX = targetPredictionUnitX * totalLen * scale;
                double lineY = targetPredictionUnitY * totalLen * scale;

                double tarCanvasX = canvasX(center.getFirstCollide().getX());
                double tarCanvasY = canvasY(center.getFirstCollide().getY());

                graphicsContext.setStroke(center.getFirstCollide().getColor().brighter().brighter());
                graphicsContext.strokeLine(tarCanvasX, tarCanvasY,
                        tarCanvasX + lineX, tarCanvasY + lineY);
            }
        }
    }

    private void connectEndPoint(WhitePrediction wp1, WhitePrediction wp2, GraphicsContext gc) {
        double[] pos1 = wp1.stopPoint();
        double[] pos2 = wp2.stopPoint();
        connectEndPoint(pos1, pos2, gc);
    }

    private void connectEndPoint(double[] pos1, double[] pos2, GraphicsContext gc) {
        gc.strokeLine(canvasX(pos1[0]), canvasY(pos1[1]), canvasX(pos2[0]), canvasY(pos2[1]));
    }

    private double[] predictionWidthDeviation(WhitePrediction[] predictions) {
        WhitePrediction center = predictions[0];
        double[] centerPos = center.stopPoint();
        double[] unitVec = Algebra.unitVector(center.lastVector());
        double[] normalVec = Algebra.normalVector(unitVec);
        double min = gameType.gameValues.innerWidth / 2;
        double max = -gameType.gameValues.innerWidth / 2;
        for (int i = 1; i < predictions.length; i++) {
            WhitePrediction wp = predictions[i];
            double[] stopPoint = wp.stopPoint();
            double[] connection = new double[]{centerPos[0] - stopPoint[0], centerPos[1] - stopPoint[1]};
            double proj = Algebra.projectionLengthOn(connection, normalVec);
            if (proj > max) max = proj;
            if (proj < min) min = proj;
        }
        return new double[]{min, max};
    }

    private void draw() {
        drawTable();
        drawBalls();
        drawCursor();
        drawBallInHand();
    }

    private void playMovement() {
        playingMovement = true;
    }

    private double getErrorMultiplierOfPower(PlayerPerson playerPerson, double selectedPower) {
        double ctrlAblePwr = playerPerson.getControllablePowerPercentage();
        double mul = 1;
        if (selectedPower > ctrlAblePwr) {
            // 超过正常发力范围，打点准确度大幅下降
            // 一般来说，球手的最大力量大于可控力量15%左右
            // 打点准确度最大应下降5倍
            mul += (selectedPower - ctrlAblePwr) / 3;
        }
        return mul * selectedPower / ctrlAblePwr;
    }

    private void beginCueAnimationOfHumanPlayer(double whiteStartingX, double whiteStartingY) {
        beginCueAnimation(game.getGame().getCuingPlayer().getInGamePlayer(),
                whiteStartingX, whiteStartingY, getSelectedPower(),
                cursorDirectionUnitX, cursorDirectionUnitY);
    }

    private void beginCueAnimation(InGamePlayer cuingPlayer, double whiteStartingX, double whiteStartingY,
                                   double selectedPower, double directionX, double directionY) {
        PlayerPerson playerPerson = cuingPlayer.getPlayerPerson();
        double personPower = selectedPower / playerPerson.getMaxPowerPercentage();  // 球手的用力程度
        double errMulWithPower = getErrorMultiplierOfPower(playerPerson, selectedPower);
        double maxPullDt =
                (playerPerson.getMaxPullDt() - playerPerson.getMinPullDt()) *
                        personPower + playerPerson.getMinPullDt();
        double handDt = maxPullDt + HAND_DT_TO_MAX_PULL;
        double handX = whiteStartingX - handDt * directionX;
        double handY = whiteStartingY - handDt * directionY;

        Cue cue;
        if (replay != null) {
            cue = replay.getCurrentCue();
        } else {
            cue = cuingPlayer.getCurrentCue(game.getGame());
        }

        // 出杆速度与白球球速算法相同
        cueAnimationPlayer = new CueAnimationPlayer(
                60.0,
                maxPullDt,
                selectedPower,
                errMulWithPower,
                handX,
                handY,
                directionX,
                directionY,
                cue,
                playerPerson
        );
    }

    private void endCueAnimation() {
//        System.out.println("End!");
        cursorDirectionUnitX = 0.0;
        cursorDirectionUnitY = 0.0;
        cueAnimationPlayer = null;
    }

    private void drawCue() {
        boolean notPlay = false;
        Ball cueBall;
        InGamePlayer igp;
        if (replay != null) {
            cueBall = replay.getCueBall();

        } else {
            if (game.getGame().isEnded()) notPlay = true;
            cueBall = game.getGame().getCueBall();
        }

        if (notPlay) return;
        if (cueAnimationPlayer == null) {
            if (movement != null) return;
            if (cursorDirectionUnitX == 0.0 && cursorDirectionUnitY == 0.0) return;

            if (cueBall.isPotted()) return;

            drawCueWithDtToHand(cueBall.getX(), cueBall.getY(),
                    cursorDirectionUnitX,
                    cursorDirectionUnitY,
                    60.0 + HAND_DT_TO_MAX_PULL,
                    game.getGame().getCuingPlayer().getInGamePlayer().getCurrentCue(game.getGame()));
        } else {
//            System.out.println("Drawing!");
            drawCueWithDtToHand(
                    cueAnimationPlayer.handX,
                    cueAnimationPlayer.handY,
                    cueAnimationPlayer.pointingUnitX,
                    cueAnimationPlayer.pointingUnitY,
                    cueAnimationPlayer.cueDtToWhite -
                            cueAnimationPlayer.maxPullDistance - HAND_DT_TO_MAX_PULL +
                            gameType.gameValues.ballRadius,
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
                sideRatio * gameType.gameValues.ballRadius * scale;
        double sideYOffset = pointingUnitX *
                sideRatio * gameType.gameValues.ballRadius * scale;
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

    private void recalculateUiRestrictions() {
        Cue currentCue = game.getGame().getCuingPlayer().getInGamePlayer()
                .getCurrentCue(game.getGame());
        CueBackPredictor.Result backPre =
                game.getGame().getObstacleDtHeight(cursorDirectionUnitX, cursorDirectionUnitY,
                        currentCue.getCueTipWidth());
        if (backPre != null) {
            if (backPre.obstacle == null) {
                // 影响来自裤边
                obstacleProjection = new CushionProjection(
                        game.getGame().getGameValues(),
                        game.getGame().getCueBall(),
                        backPre.distance,
                        cueAngleDeg,
                        currentCue.getCueTipWidth());
            } else {
                // 后斯诺
                obstacleProjection = new BallProjection(
                        backPre.obstacle, game.getGame().getCueBall(),
                        cursorDirectionUnitX, cursorDirectionUnitY,
                        cueAngleDeg);
            }
        } else {
            obstacleProjection = null;
        }
        // 如果打点不可能，把出杆键禁用了
        // 自动调整打点太麻烦了
        setCueButtonForPoint();
    }

    private void setCueButtonForPoint() {
        cueButton.setDisable(obstacleProjection != null &&
                !obstacleProjection.cueAble(
                        getCuePointRelX(cuePointX), getCuePointRelY(cuePointY),
                        getRatioOfCueAndBall()));
    }

    private void drawCueAngleCanvas() {
        double angleCanvasWh = cueAngleCanvas.getWidth();
        double arcRadius = 60.0;
        cueAngleCanvasGc.setFill(WHITE);
        cueAngleCanvasGc.fillRect(0, 0,
                angleCanvasWh, angleCanvasWh);
        cueAngleCanvasGc.setStroke(Color.GRAY);
        cueAngleCanvasGc.setLineWidth(1.0);
        cueAngleCanvasGc.strokeArc(
                -arcRadius + cueAngleBaseHor,
                cueAngleBaseVer,
                arcRadius * 2,
                arcRadius * 2,
                0,
                90,
                ArcType.OPEN
        );

        cueAngleCanvasGc.setStroke(BLACK);
        cueAngleCanvasGc.setLineWidth(3.0);
        double lineWidth = angleCanvasWh - cueAngleBaseHor;

        cueAngleCanvasGc.strokeLine(cueAngleBaseHor, angleCanvasWh - cueAngleBaseVer,
                angleCanvasWh, angleCanvasWh - cueAngleBaseVer -
                        Math.tan(Math.toRadians(cueAngleDeg)) * lineWidth);

    }

    private void drawCueBallCanvas() {
        // Wipe
        double cueAreaDia = cueAreaRadius * 2;
        double padding = (cueCanvasWH - cueAreaDia) / 2;
        ballCanvasGc.setStroke(BLACK);
        ballCanvasGc.setFill(Values.WHITE);
        ballCanvasGc.fillRect(0, 0, ballCanvas.getWidth(), ballCanvas.getHeight());

        if (obstacleProjection instanceof CushionProjection) {
            // 影响来自裤边
            CushionProjection projection = (CushionProjection) obstacleProjection;
            double lineY = padding + (projection.getLineY() + 1) * cueAreaRadius;
            if (lineY < cueCanvasWH - padding) {
                ballCanvasGc.setFill(Color.GRAY);
                ballCanvasGc.fillRect(0, lineY, cueCanvasWH, cueCanvasWH - lineY);
            }
        } else if (obstacleProjection instanceof BallProjection) {
            // 后斯诺
            BallProjection projection = (BallProjection) obstacleProjection;
            ballCanvasGc.setFill(Color.GRAY);
            ballCanvasGc.fillOval(padding + cueAreaRadius * projection.getCenterHor(),
                    padding + cueAreaRadius * projection.getCenterVer(),
                    cueAreaDia,
                    cueAreaDia);
        }

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

    private boolean isGameCalculating() {
        return game.getGame().isCalculating();
    }

    private boolean isPlayingMovement() {
        return movement != null && playingMovement;
    }

    private boolean isPlayingCueAnimation() {
        return cueAnimationPlayer != null;
    }

    class CueAnimationPlayer {
        private final long holdMs;  // 拉至满弓的停顿时间
        private final long endHoldMs;  // 出杆完成后的停顿时间
        private final double initDistance, maxPullDistance;
        private final double cueMoveSpeed;  // 杆每毫秒运动的距离，毫米

        //        private final double
        private final double maxExtension;  // 杆的最大延伸距离，始终为负
        //        private final double cueBallX, cueBallY;
        private final double handX, handY;  // 手架的位置，作为杆的摇摆中心点
        private final double errMulWithPower;
        private final Cue cue;
        private final PlayerPerson playerPerson;
        private long heldMs = 0;
        private long endHeldMs = 0;
        private double cueDtToWhite;  // 杆的动画离白球的真实距离，未接触前为正
        private boolean touched;  // 是否已经接触白球
        private boolean reachedMaxPull;
        private double pointingUnitX, pointingUnitY;

        CueAnimationPlayer(double initDistance,
                           double maxPullDt,
                           double selectedPower,
                           double errMulWithPower,
                           double handX, double handY,
                           double pointingUnitX, double pointingUnitY,
                           Cue cue,
                           PlayerPerson playerPerson) {

            if (selectedPower < Values.MIN_SELECTED_POWER)
                selectedPower = Values.MIN_SELECTED_POWER;

            this.initDistance = Math.min(initDistance, maxPullDt);
            this.maxPullDistance = maxPullDt;
            this.cueDtToWhite = this.initDistance;
            this.maxExtension = -maxPullDistance *
                    (playerPerson.getMaxSpinPercentage() * 0.75 / 100);  // 杆法好的人延伸长
            this.cueMoveSpeed = selectedPower * Values.MAX_POWER_SPEED / 100_000.0;
//            this.cueBallX = cueBallInitX;
//            this.cueBallY = cueBallInitY;
            this.errMulWithPower = errMulWithPower;

            this.pointingUnitX = pointingUnitX;
            this.pointingUnitY = pointingUnitY;

            this.handX = handX;
            this.handY = handY;

            System.out.println(cueDtToWhite + ", " + this.cueMoveSpeed + ", " + maxExtension);

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
                            errMulWithPower / 2000;
                    double[] newUnit = Algebra.angleToUnitVector(newAngle);
                    pointingUnitX = newUnit[0];
                    pointingUnitY = newUnit[1];
                } else if (stage > 0) {  // 向右扭
                    double angle = Algebra.thetaOf(pointingUnitX, pointingUnitY);
                    double newAngle = angle - playerPerson.getCueSwingMag() *
                            errMulWithPower / 2000;
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
                        playMovement();
//                        game.cue(cueVx, cueVy, xSpin, ySpin, sideSpin);
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
