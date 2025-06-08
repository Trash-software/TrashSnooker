package trashsoftware.trashSnooker.fxml;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Shape;
import javafx.scene.shape.Shape3D;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.json.JSONObject;
import trashsoftware.trashSnooker.audio.AudioPlayerManager;
import trashsoftware.trashSnooker.audio.SoundInfo;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.ai.AiCueBallPlacer;
import trashsoftware.trashSnooker.core.ai.AiCueResult;
import trashsoftware.trashSnooker.core.ai.AttackChoice;
import trashsoftware.trashSnooker.core.ai.FinalChoice;
import trashsoftware.trashSnooker.core.attempt.CueType;
import trashsoftware.trashSnooker.core.attempt.DefenseAttempt;
import trashsoftware.trashSnooker.core.attempt.PotAttempt;
import trashsoftware.trashSnooker.core.career.Career;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.CareerMatch;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;
import trashsoftware.trashSnooker.core.career.challenge.ChallengeMatch;
import trashsoftware.trashSnooker.core.career.championship.PlayerVsAiMatch;
import trashsoftware.trashSnooker.core.career.championship.SnookerChampionship;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.metrics.*;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.movement.MovementFrame;
import trashsoftware.trashSnooker.core.movement.WhitePrediction;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallGame;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallPlayer;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.LetBall;
import trashsoftware.trashSnooker.core.numberedGames.nineBall.AmericanNineBallGame;
import trashsoftware.trashSnooker.core.person.CuePlayerHand;
import trashsoftware.trashSnooker.core.person.HandBody;
import trashsoftware.trashSnooker.core.person.PlayerHand;
import trashsoftware.trashSnooker.core.person.PlayerPerson;
import trashsoftware.trashSnooker.core.scoreResult.ChineseEightScoreResult;
import trashsoftware.trashSnooker.core.scoreResult.NineBallScoreResult;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;
import trashsoftware.trashSnooker.core.scoreResult.SnookerScoreResult;
import trashsoftware.trashSnooker.core.snooker.AbstractSnookerGame;
import trashsoftware.trashSnooker.core.snooker.SnookerPlayer;
import trashsoftware.trashSnooker.core.table.ChineseEightTable;
import trashsoftware.trashSnooker.core.table.NumberedBallTable;
import trashsoftware.trashSnooker.fxml.alert.AlertShower;
import trashsoftware.trashSnooker.fxml.drawing.*;
import trashsoftware.trashSnooker.fxml.projection.BallProjection;
import trashsoftware.trashSnooker.fxml.projection.CushionProjection;
import trashsoftware.trashSnooker.fxml.projection.ObstacleProjection;
import trashsoftware.trashSnooker.fxml.widgets.GamePane;
import trashsoftware.trashSnooker.recorder.*;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.PointInPoly;
import trashsoftware.trashSnooker.util.Util;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class GameView implements Initializable {
    public static final Color GLOBAL_BACKGROUND = Color.WHITESMOKE;  // 似乎正好是javafx默认背景色

    public static final Color WHITE = Color.WHITE;
    public static final Color BLACK = Color.BLACK;
    public static final Color CUE_AIMING_CROSS = Color.LIGHTGRAY;
    public static final Color CUE_POINT = Color.RED;
    public static final Color INTENT_CUE_POINT = Color.NAVY;
    public static final Color CUE_TIP_COLOR = Color.LIGHTSEAGREEN;
    public static final Color REST_METAL_COLOR = Color.GOLDENROD;
    public static final Font POOL_NUMBER_FONT = new Font(8.0);
    public static final double HAND_DT_TO_MAX_PULL = 30.0;
    public static final double MIN_CUE_BALL_DT = 30.0;  // 运杆时杆头离白球的最小距离
    public static final double MAX_CUE_ANGLE = 75.0;
    private static final double DEFAULT_POWER = 35.0;
    private static final double WHITE_PREDICT_LEN_AFTER_WALL = 1000.0;  // todo: 根据球员
    private static final long DEFAULT_REPLAY_GAP = 1000;
    //    public static double scale;

    /**
     * 这两个是管物理运算的存档率的，也就是movement和回放文件的帧率
     */
    public static int productionFrameRate = ConfigLoader.getInstance().getProductionFrameRate();
    public static double frameTimeMs = 1000.0 / productionFrameRate;

    private static double uiFrameTimeMs = 10.0;
    private static double defaultMaxPredictLength = 800;
    private final List<Node> disableWhenCuing = new ArrayList<>();  // 出杆/播放动画时不准按的东西
    private final Map<Cue, CueModel> cueModelMap = new HashMap<>();
    @FXML
    MenuBar menuBar;
    @FXML
    Pane bottomPane;
    @FXML
    GamePane gamePane;  // 球和桌子画在这里
    @FXML
    Pane contentPane;
    @FXML
    HBox mainFramePane;
    @FXML
    Canvas cueAngleCanvas;
    @FXML
    Label cueAngleLabel;
    @FXML
    VBox leftVBox, cueSettingsPane;
    @FXML
    Pane leftToolbarPane;
    @FXML
    Canvas cuePointCanvas;
    @FXML
    Pane powerSliderPane;
    @FXML
    Slider powerSlider;
    @FXML
    Label sliderZeroLabel, sliderCtrlLabel, sliderMaxLabel;
    @FXML
    Label powerLabel;
    @FXML
    Button cueButton;
    @FXML
    Button changeCueButton;
    @FXML
    Button replayNextCueButton, replayLastCueButton;
    @FXML
    CheckBox replayAutoPlayBox;
    @FXML
    Label replayCueNumLabel;
    @FXML
    VBox gameButtonBox, replayButtonBox;
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
    Label timerLabel;
    @FXML
    Label fpsLabel;
    @FXML
    Pane player1TarPane, player2TarPane;
    @FXML
    Canvas player1TarCanvas, player2TarCanvas;
    @FXML
    MenuItem repairMenu;
    @FXML
    Menu gameMenu;
    @FXML
    MenuItem withdrawMenu, replaceBallInHandMenu, letOtherPlayMenu, repositionMenu, pushOutMenu;
    @FXML
    SeparatorMenuItem gameMenuSep1;
    @FXML
    CheckMenuItem aiHelpPlayMenuItem;
    @FXML
    Menu debugMenu;
    @FXML
    MenuItem debugModeMenu;
    //            , saveGameMenu, newGameMenu;
    @FXML
    ToggleGroup player1SpeedToggle, player2SpeedToggle;
    @FXML
    Menu player1SpeedMenu, player2SpeedMenu;
    @FXML
    CheckMenuItem aiAutoPlayMenuItem;
    @FXML
    CheckMenuItem drawAiPathItem;
    CheckMenuItem predictPlayerPathItem = new CheckMenuItem();
    @FXML
    CheckMenuItem aimingExtensionMenu, potInspectionMenu;
    @FXML
    CheckMenuItem traceWhiteItem, traceTargetItem, traceAllItem;
    @FXML
    VBox handSelectionBox;
    @FXML
    ToggleGroup handSelectionToggleGroup;
    @FXML
    RadioButton handSelectionLeft, handSelectionRight, handSelectionRest;
    boolean debugMode = false;
    boolean devMode = true;
    PredictionQuality predictionQuality = PredictionQuality.fromKey(
            ConfigLoader.getInstance().getString("performance", "veryHigh"));
    //    private Timeline timeline;
    VideoCapture videoCapture;  // 录制视频用
    GameLoop gameLoop;
    int cueTimeCounter;
    Timer cueTimer;
    //    AnimationTimer animationTimer;
    private final double minPredictLengthPotDt = 2000;  // 多远的距离会让瞄准线最短
    private final double maxPredictLengthPotDt = 100;
    private double ballDiameter;
    private double ballRadius;
    private double cueCanvasWH = 80.0;
    private double cueAreaRadius = 36.0;
    //    private double cueRadius = 4.0;
    private GraphicsContext cuePointCanvasGc;
    private GraphicsContext cueAngleCanvasGc;
    @FXML
    Pane windowRootPane;
    private Stage stage;
    private InGamePlayer player1;
    private InGamePlayer player2;
    private EntireGame game;
    private GameReplay replay;
    GameValues gameValues;
    private double cursorDirectionUnitX, cursorDirectionUnitY;
    private double targetPredictionUnitX, targetPredictionUnitY;
    private double mouseX, mouseY;
    private Ball predictedTargetBall;
    private Movement movement;
    private Movement tracedMovement;
    private boolean playingMovement = false;
    private int lastMovementPlayingIndex = 0;
    private int movementPlayingIndex = 0;  // 第几帧
    private double movementPercentageInIndex = 0.0;  // 这一帧放了多少
    private GamePlayStage currentPlayStage;  // 只对game有效, 并且不要直接access这个变量, 请用getter
    private PotAttempt lastPotAttempt;
    private DefenseAttempt curDefAttempt;
    // 用于播放运杆动画时持续显示预测线（含出杆随机偏移）
    private ObstacleProjection obstacleProjection;
    // 杆法的击球点。注意: cuePointY用在击球点的canvas上，值越大杆法越低，而unitFrontBackSpin相反
    private double cuePointX, cuePointY;  // 杆法的击球点
    private double intentCuePointX = -1, intentCuePointY = -1;  // 计划的杆法击球点
    private double cueAngleDeg = 5.0;
    private double cueRollRotateDeg1 = 180.0;  // 转杆，并不重要
    private double cueRollRotateDeg2 = 180.0;  // 球员2的转杆
    private double cueAngleBaseVer = 10.0;
    private double cueAngleBaseHor = 10.0;
    private CueAnimationPlayer cueAnimationPlayer;
    private boolean isDragging;
    private double lastDragAngle;
    //    private double predictionMultiplier = 2000.0;
    private double maxRealPredictLength = defaultMaxPredictLength;
    private boolean enablePsy = true;  // 由游戏决定心理影响
    private boolean aiCalculating;
    private boolean aiAutoPlay = true;
    private boolean printPlayStage = false;
    private boolean tableGraphicsChanged = true;
    private boolean showTipBrokenMsg;
    private Ball debuggingBall;
    private long replayStopTime;
    private long replayGap = DEFAULT_REPLAY_GAP;
    private boolean drawStandingPos = false;
    //    private boolean drawTargetRefLine = false;
    private boolean miscued = false;
    private CuePlayerHand currentHand;
    private List<PlayerHand.CueHand> lastPlayableHands;
    private final Map<PlayerHand.Hand, PlayerHand.CueHand> handCueHandMap = new TreeMap<>();
    private CareerMatch careerMatch;
    private PredictionDrawing cursorDrawer;
    private PotInspection potInspection;
    private CueSelection.CueAndBrand lastUsedCue;

    private ResourceBundle strings;
    private double p1PlaySpeed = 1.0;
    private double p2PlaySpeed = 1.0;
    private boolean aiHelpPlay = false;
    private boolean absoluteDragAngle = false;
    private List<double[]> aiWhitePath;  // todo: debug用的
    private List<double[]> suggestedPlayerWhitePath;

    private double[][] cueAbleArea;  // 不会呲杆的打点，暂时与障碍无关

    private static double pullDtOf(PlayerHand person, double personPower) {
        return (person.getMaxPullDt() - person.getMinPullDt()) *
                personPower + person.getMinPullDt();
    }

    private static double extensionDtOf(PlayerHand person, double personPower) {
        return (person.getMaxExtension() - person.getMinExtension()) *
                personPower + person.getMinExtension();
    }

    private static double[] handPosition(double handDt,
                                         double cueAngleDeg,
                                         double whiteX, double whiteY,
                                         double trueAimingX, double trueAimingY) {
        double cueAngleCos = Math.cos(Math.toRadians(cueAngleDeg));
        double handX = whiteX - handDt * trueAimingX * cueAngleCos;
        double handY = whiteY - handDt * trueAimingY * cueAngleCos;
        return new double[]{handX, handY};
    }

    private static double aimingOffsetOfPlayer(PlayerPerson person, double selectedPower) {
        double playerAimingOffset = person.getAimingOffset();
        // 这个比较固定，不像出杆扭曲那样，发暴力时歪得夸张
        return (playerAimingOffset * (selectedPower / 100.0) + playerAimingOffset) / 8.0;
    }

    private static double getPersonPower(double selectedPower, CuePlayerHand person) {
        return selectedPower / person.getMaxPowerPercentage();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.strings = resources;

//        gameScene.widthProperty().bind(gamePane.widthProperty());
//        gameScene.heightProperty().bind(gamePane.heightProperty());

        cueModelMap.clear();

        menuListeners();

        player1SpeedToggle.selectToggle(player1SpeedToggle.getToggles().get(3));
        player2SpeedToggle.selectToggle(player2SpeedToggle.getToggles().get(3));
        drawAiPathItem.selectedProperty().addListener((observable, oldValue, newValue) ->
                tableGraphicsChanged = true);

        cuePointCanvasGc = cuePointCanvas.getGraphicsContext2D();
        cuePointCanvasGc.setTextAlign(TextAlignment.CENTER);
        cuePointCanvasGc.setFont(App.FONT);

        cueAngleCanvasGc = cueAngleCanvas.getGraphicsContext2D();

        boolean aa = ConfigLoader.getInstance().getAntiAliasing().canvasAA;
        cuePointCanvasGc.setImageSmoothing(aa);
        cueAngleCanvasGc.setImageSmoothing(aa);

        player1TarCanvas.getGraphicsContext2D().setImageSmoothing(aa);
        player2TarCanvas.getGraphicsContext2D().setImageSmoothing(aa);
        singlePoleCanvas.getGraphicsContext2D().setImageSmoothing(aa);

        // todo: 喊mac用户来测试
        player1TarCanvas.getGraphicsContext2D().setFont(App.FONT);
        player2TarCanvas.getGraphicsContext2D().setFont(App.FONT);

        addListeners();

        disableWhenCuing.addAll(List.of(
                cueButton,
                changeCueButton,
                cuePointCanvas,
                cueAngleCanvas,
                handSelectionLeft,
                handSelectionRight,
                handSelectionRest
        ));

        powerSlider.setShowTickLabels(true);

        ConfigLoader configLoader = ConfigLoader.getInstance();
        String mouseDragMethod = configLoader.getString("mouseDragMethod");
        absoluteDragAngle = "position".equals(mouseDragMethod);
    }

    private void menuListeners() {
        player1SpeedToggle.selectedToggleProperty().addListener((observableValue, toggle, t1) -> {
            if (t1 != null)
                player1SpeedMenu.setText(strings.getString("p1PlaySpeedMenu") + " " + t1.getUserData() + "x");
        });
        player2SpeedToggle.selectedToggleProperty().addListener((observableValue, toggle, t1) -> {
            if (t1 != null)
                player2SpeedMenu.setText(strings.getString("p2PlaySpeedMenu") + " " + t1.getUserData() + "x");
        });

        potInspectionMenu.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                potInspection = new PotInspection();
                cursorDirectionUnitX = 0.0;
                cursorDirectionUnitY = 0.0;
                aimingChanged();
            } else {
                potInspection = null;
            }
            tableGraphicsChanged = true;
        });

        ConfigLoader cl = ConfigLoader.getInstance();

        aimingExtensionMenu.selectedProperty().setValue(
                cl.getBoolean("aimingExtension", false)
        );
        drawAiPathItem.selectedProperty().setValue(
                cl.getBoolean("drawAiPath", false)
        );
        traceWhiteItem.selectedProperty().setValue(
                cl.getBoolean("traceWhite", false)
        );
        traceTargetItem.selectedProperty().setValue(
                cl.getBoolean("traceTarget", false)
        );
        traceAllItem.selectedProperty().setValue(
                cl.getBoolean("traceAll", false)
        );

        aimingExtensionMenu.selectedProperty().addListener((observable, oldValue, newValue) -> {
            cl.put("aimingExtension", newValue);
            cl.save();
            tableGraphicsChanged = true;
        });
        drawAiPathItem.selectedProperty().addListener((observable, oldValue, newValue) -> {
            cl.put("drawAiPath", newValue);
            cl.save();
            tableGraphicsChanged = true;
        });
        traceWhiteItem.selectedProperty().addListener((observable, oldValue, newValue) -> {
            cl.put("traceWhite", newValue);
            cl.save();
            tableGraphicsChanged = true;
        });
        traceTargetItem.selectedProperty().addListener((observable, oldValue, newValue) -> {
            cl.put("traceTarget", newValue);
            cl.save();
            tableGraphicsChanged = true;
        });
        traceAllItem.selectedProperty().addListener((observable, oldValue, newValue) -> {
            cl.put("traceAll", newValue);
            cl.save();
            tableGraphicsChanged = true;
        });
    }

    public void changeScale(double newScale) {
        gamePane.updateScale(newScale, getActiveHolder());
        ballDiameter = gameValues.ball.ballDiameter * gamePane.getScale();
        ballRadius = ballDiameter / 2;
        
        setTargetScales();
        drawTargetBoard(true);

        tableGraphicsChanged = true;
        createPathPrediction();
    }
    
    private void setTargetScales() {
        player1TarCanvas.setHeight(ballDiameter * 1.2);
        player1TarCanvas.setWidth(ballDiameter * 2.4);
        player2TarCanvas.setHeight(ballDiameter * 1.2);
        player2TarCanvas.setWidth(ballDiameter * 2.4);
        singlePoleCanvas.setHeight(ballDiameter * 1.3);
        ((Pane) singlePoleCanvas.getParent()).setPrefHeight(singlePoleCanvas.getHeight());

        if (gameValues.rule.snookerLike())
            singlePoleCanvas.setWidth(ballDiameter * 8 * 1.2);  // 考虑可能的金球
        else if (gameValues.rule == GameRule.CHINESE_EIGHT || gameValues.rule == GameRule.LIS_EIGHT)
            singlePoleCanvas.setWidth(ballDiameter * 8 * 1.2);
        else if (gameValues.rule == GameRule.AMERICAN_NINE)
            singlePoleCanvas.setWidth(ballDiameter * 9 * 1.2);
        else throw new RuntimeException("nmsl");

        singlePoleCanvas.getGraphicsContext2D().setTextAlign(TextAlignment.CENTER);
        singlePoleCanvas.getGraphicsContext2D().setStroke(WHITE);
        player1TarCanvas.getGraphicsContext2D().setTextAlign(TextAlignment.CENTER);
        player1TarCanvas.getGraphicsContext2D().setStroke(WHITE);
        player2TarCanvas.getGraphicsContext2D().setTextAlign(TextAlignment.CENTER);
        player2TarCanvas.getGraphicsContext2D().setStroke(WHITE);
    }

    private void generateScales(GameValues gameValues) {
        gamePane.setupPane(gameValues);
        double scale = gamePane.getScale();

//        double topLeftY = (canvasHeight - innerHeight) / 2;
        ballDiameter = gameValues.ball.ballDiameter * scale;
        ballRadius = ballDiameter / 2;
//        cornerArcDiameter = values.cornerArcDiameter * scale;

        double[] actualResolution = ConfigLoader.getInstance().getEffectiveResolution();
        double zoomRatio = actualResolution[1] / 864;
        if (zoomRatio < 1.0) {
            cueCanvasWH *= zoomRatio;
            cueAreaRadius *= zoomRatio;
//            cueRadius *= zoomRatio;

            cueAngleBaseHor *= zoomRatio;
            cueAngleBaseVer *= zoomRatio;
        }
        powerSlider.setPrefHeight(powerSlider.getPrefHeight() * zoomRatio);

        cuePointCanvas.setWidth(cueCanvasWH);
        cuePointCanvas.setHeight(cueCanvasWH);
        cueAngleCanvas.setWidth(cueCanvasWH);
        cueAngleCanvas.setHeight(cueCanvasWH);

        setTargetScales();
    }

    public void checkScale() {
        System.out.println("Game pane scale: " + gamePane.getWidth() + " " + gamePane.getHeight());
        System.out.println("Left scale: " + leftToolbarPane.getWidth());
    }

    private void setupDebug() {
        System.out.println("Debug: " + devMode);
        debugMenu.setVisible(devMode);

        if (devMode) {
            drawAiPathItem.setSelected(true);
        }

        setupCheckMenus();
    }

    private void setupCheckMenus() {
        aiAutoPlayMenuItem.selectedProperty().addListener((observableValue, aBoolean, t1) -> aiAutoPlay = t1);
        aiAutoPlayMenuItem.setSelected(aiAutoPlay);

        predictPlayerPathItem.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
            if (t1) {
                if (game.getGame().isEnded()
                        || cueAnimationPlayer != null || playingMovement || aiCalculating) return;
                Player currentPlayer = game.getGame().getCuingPlayer();
                if (currentPlayer.getInGamePlayer().getPlayerType() == PlayerType.PLAYER) {
                    predictPlayerPath(currentPlayer);
                }
            } else {
                suggestedPlayerWhitePath = null;
            }
        });

        if (replay != null) {
            // disable all
            aiAutoPlayMenuItem.setVisible(false);
            drawAiPathItem.setVisible(false);
            predictPlayerPathItem.setVisible(false);
        }
    }

    public Pane getContentPane() {
        return contentPane;
    }

    public void setupReplay(Stage stage, GameReplay replay) {
        this.replay = replay;
        this.gameValues = replay.gameValues;
        this.devMode = false;
        this.stage = stage;

        this.player1 = replay.getP1();
        this.player2 = replay.getP2();

        this.windowRootPane = (Pane) stage.getScene().getRoot();

        gameButtonBox.setVisible(false);
        gameButtonBox.setManaged(false);

        productionFrameRate = replay.getFrameRate();
        frameTimeMs = 1000.0 / productionFrameRate;

        setupNameLabels(player1.getPlayerPerson(), player2.getPlayerPerson());
        totalFramesLabel.setText(String.format("(%d)", replay.getItem().totalFrames));

        generateScales(replay.gameValues);
        restoreCuePoint();
        restoreCueAngle();
        drawTargetBoard(true);
        drawScoreBoard(null, true);

        potInspectionMenu.setDisable(true);
        repairMenu.setDisable(true);

        setupPowerSlider();
        powerSlider.setDisable(true);
        gameMenu.getItems().clear();
        setUiFrameStart();
        setupDebug();
        setupHandSelection();

        setupBalls();

        setOnHidden();
    }

    public void setup(Stage stage, EntireGame entireGame) {
        this.game = entireGame;
        this.gameValues = entireGame.gameValues;
        this.player1 = entireGame.getPlayer1();
        this.player2 = entireGame.getPlayer2();
        this.stage = stage;
        this.devMode = entireGame.gameValues.isDevMode();
        this.drawStandingPos = devMode;
        this.aiAutoPlay = !this.devMode;

        this.windowRootPane = (Pane) stage.getScene().getRoot();

        setFrameRate(ConfigLoader.getInstance().getFrameRate());
        setKeyboardActions();

        generateScales(entireGame.gameValues);
        restoreCuePoint();
        restoreCueAngle();
//        setupCanvas();
//        drawTargetBoard(true);

        setupHandSelection();
        startNextFrame();
//        game.startNextFrame();  // fixme: 问题 game.game不是null的时候就渲染不出球
//        drawScoreBoard(game.getGame().getCuingPlayer(), true);
//        cursorDrawer = new PredictionDrawing();

        replayButtonBox.setVisible(false);
        replayButtonBox.setManaged(false);

        setupNameLabels(player1.getPlayerPerson(), player2.getPlayerPerson());
        totalFramesLabel.setText(String.format("(%d)", entireGame.totalFrames));

        updatePlayStage();

        setupPowerSlider();
        InGamePlayer igp = game.getGame().getCuingPlayer().getInGamePlayer();
        updatePowerSlider(igp, CuePlayerHand.makeDefault(igp));
        recalculateUiRestrictions();

        setupGameMenu();
        setupAiHelper();
//        setUiFrameStart();
        setupDebug();

//        setupBalls();

        stage.setOnCloseRequest(e -> {
            if (!game.isFinished()) {
                e.consume();

                AlertShower.askConfirmation(stage,
                        strings.getString("closeWindowConcede"),
                        strings.getString("notEndExitWarning"),
                        () -> {
//                            game.quitGame(careerMatch != null);
//                            timeline.stop();
//                            gameLoop.cancel();
                            System.out.println("Close request");
                            gameLoop.stop();
                            stage.close();
                        },
                        null);
            }
        });

        setOnHidden();

        startAnimation();
    }

    private void setupGameMenu() {
        gameMenu.getItems().clear();
        gameMenu.setDisable(replay != null);
        if (careerMatch != null) {
            gameMenu.getItems().addAll(aiHelpPlayMenuItem, gameMenuSep1);
        }

        gameMenu.getItems().addAll(withdrawMenu, replaceBallInHandMenu);

        GameRule rule = gameValues.rule;
//        if (rule.hasRule(Rule.FOUL_LET_OTHER_PLAY)) {
        gameMenu.getItems().add(letOtherPlayMenu);
//        }
        if (rule.hasRule(Rule.FOUL_AND_MISS)) {
            gameMenu.getItems().add(repositionMenu);
        }
        if (rule.hasRule(Rule.PUSH_OUT)) {
            gameMenu.getItems().add(pushOutMenu);
        }
    }

    private void setupAiHelper() {
        aiHelpPlayMenuItem.selectedProperty().addListener(((observableValue, aBoolean, t1) -> {
            if (t1) {
                double playerGoodness;
                if (careerMatch != null) {
                    playerGoodness = CareerManager.getInstance().getPlayerGoodness();
                } else {
                    playerGoodness = ConfigLoader.getInstance().getDouble("fastGameAiming", 1.0);
                }
                if (playerGoodness == 0.0) {
                    AlertShower.showInfo(
                            stage,
                            "",
                            strings.getString("aiHelpPlayerUnavailable"),
                            3000
                    );
                    aiHelpPlayMenuItem.setSelected(false);
                    aiHelpPlay = false;
                } else {
                    AlertShower.askConfirmation(
                            stage,
                            strings.getString("aiHelpPlayerPrompt"),
                            strings.getString("aiHelpPlayerTitle"),
                            () -> aiHelpPlay = true,
                            () -> aiHelpPlayMenuItem.setSelected(false));
                }
            } else {
                aiHelpPlay = false;
            }
        }));
    }

    private void setupHandSelection() {
        if (replay != null) {
            handSelectionBox.setVisible(false);
            handSelectionBox.setManaged(false);
        } else {
            handSelectionToggleGroup.selectedToggleProperty().addListener((observableValue, toggle, t1) -> {
                if (game == null || game.getGame() == null) return;
                PlayerHand.Hand selected = PlayerHand.Hand.valueOf(String.valueOf(t1.getUserData()));
                PlayerHand.CueHand cueHand = handCueHandMap.get(selected);
                PlayerPerson person = game.getGame().getCuingPlayer().getPlayerPerson();
                if (cueHand == null) {
                    currentHand = CuePlayerHand.makeDefault(game.getGame().getCuingIgp());
                } else {
                    currentHand = person.handBody.getHandSkillByHand(cueHand);
                }
                createPathPrediction();
            });

            handSelectionToggleGroup.selectToggle(handSelectionRight);
        }
    }

    private void updateHandSelectionToggleByData(PlayerHand.Hand hand) {
        Toggle toggle = switch (hand) {
            case LEFT -> handSelectionLeft;
            case RIGHT -> handSelectionRight;
            default -> handSelectionRest;
        };

        handSelectionToggleGroup.selectToggle(toggle);
    }

    private void updateHandButton(RadioButton radioButton,
                                  PlayerHand.Hand thisHand,
                                  List<PlayerHand.CueHand> playAbles) {
        for (PlayerHand.CueHand cueHand : playAbles) {
            if (cueHand.hand == thisHand) {
                String text;
                if (cueHand.extension == PlayerHand.CueExtension.NO) {
                    text = cueHand.hand.shownName(strings);
                } else {
                    text = String.format("%s (%s)",
                            cueHand.hand.shownName(strings),
                            cueHand.extension.getReadable(strings));
                }
                radioButton.setText(text);
                radioButton.setDisable(false);
                handCueHandMap.put(thisHand, cueHand);
                return;
            }
        }
        radioButton.setDisable(true);
    }

    private void updateHandSelection(boolean forceChangeHand) {
        Ball cueBall = game.getGame().getCueBall();
        InGamePlayer igp = game.getGame().getCuingPlayer().getInGamePlayer();
        if (igp.getPlayerType() == PlayerType.COMPUTER) return;
        PlayerPerson playingPerson = igp.getPlayerPerson();

        List<PlayerHand.CueHand> playAbles = HandBody.getPlayableHands(
                cueBall.getX(), cueBall.getY(),
                cursorDirectionUnitX, cursorDirectionUnitY,
                cueAngleDeg,
                gameValues.table,
                playingPerson,
                igp.getCueSelection().getSelected().brand
        );

        updateHandButton(handSelectionLeft, PlayerHand.Hand.LEFT, playAbles);
        updateHandButton(handSelectionRight, PlayerHand.Hand.RIGHT, playAbles);
        updateHandButton(handSelectionRest, PlayerHand.Hand.REST, playAbles);

        boolean anyChange = forceChangeHand ||
                lastPlayableHands == null ||
                playAbles.size() != lastPlayableHands.size();  // 是null就必true
        if (!anyChange) {
            for (int i = 0; i < playAbles.size(); i++) {
                PlayerHand.CueHand ch1 = playAbles.get(i);
                PlayerHand.CueHand ch2 = lastPlayableHands.get(i);
                if (ch1.hand != ch2.hand || ch1.extension != ch2.extension) {
                    anyChange = true;
                    break;
                }
            }
        }

        if (anyChange) {
            // 如果这次update改变了任何“一只手”的可用性，刷新为可用的第一顺位手
            handSelectionToggleGroup.selectToggle(
                    handButtonOfHand(playAbles.getFirst())
            );
            // 防止由于toggle没变的原因导致不触发换手
            currentHand = playingPerson.handBody.getHandSkillByHand(playAbles.getFirst());

            lastPlayableHands = playAbles;
        }
        updatePowerSlider(igp, currentHand, false);
    }

    private RadioButton handButtonOfHand(PlayerHand.CueHand hand) {
        return switch (hand.hand) {
            case LEFT -> handSelectionLeft;
            case RIGHT -> handSelectionRight;
            case REST -> handSelectionRest;
        };
    }

    private void setupNameLabels(PlayerPerson p1, PlayerPerson p2) {
        String p1n = p1.getName();
        String p2n = p2.getName();

        if (careerMatch != null && careerMatch.getChampionship() != null) {
            p1n += " (" + careerMatch.getChampionship().getCareerSeedMap().get(p1.getPlayerId()) + ")";
            p2n += " (" + careerMatch.getChampionship().getCareerSeedMap().get(p2.getPlayerId()) + ")";
        }

        player1Label.setText(p1n);
        player2Label.setText(p2n);
    }

    public void setupCareerMatch(Stage stage,
                                 CareerMatch careerMatch) {
        this.careerMatch = careerMatch;

        setup(stage, careerMatch.getGame());

        double playerGoodness = CareerManager.getInstance().getPlayerGoodness();
        setAimingLengthFactor(playerGoodness);

        JSONObject careerCache = CareerManager.getInstance().getCache();
        if (careerCache.has("playerCue")) {
            String cueInsId = careerCache.getString("playerCue");
            if (game.getPlayer1().isHuman()) {
                game.getPlayer1().getCueSelection().selectByInstanceId(cueInsId);
            }
            if (game.getPlayer2().isHuman()) {
                game.getPlayer2().getCueSelection().selectByInstanceId(cueInsId);
            }
        }
    }

    public void setAimingLengthFactor(double aimingLengthFactor) {
        maxRealPredictLength = defaultMaxPredictLength * aimingLengthFactor;
    }

    public void setFrameRate(int frameRate) {
        uiFrameTimeMs = 1000.0 / frameRate;
    }

    private void keyboardAction(KeyEvent e) {
        if (replay != null || aiCalculating || playingMovement || cueAnimationPlayer != null) {
            return;
        }
        switch (e.getCode()) {
            case SPACE -> {
                if (!cueButton.isDisabled()) {
                    cueButton.fire();
                }
            }
            case LEFT -> turnDirectionDeg(-0.5);
            case RIGHT -> turnDirectionDeg(0.5);
            case COMMA -> turnDirectionDeg(-0.01);
            case PERIOD -> turnDirectionDeg(0.01);
            case A -> setCuePoint(cuePointX - 1, cuePointY, true);
            case D -> setCuePoint(cuePointX + 1, cuePointY, true);
            case W -> setCuePoint(cuePointX, cuePointY - 1, true);
            case S -> setCuePoint(cuePointX, cuePointY + 1, true);
            case Q -> setCueAngleDeg(cueAngleDeg + 1);
            case E -> setCueAngleDeg(cueAngleDeg - 1);
            case Z -> {
                if (getActiveHolder().getCuingIgp().getPlayerNumber() == 1) {
                    cueRollRotateDeg1 += 3;
                } else {
                    cueRollRotateDeg2 += 3;
                }
            }
            case X -> {
                if (getActiveHolder().getCuingIgp().getPlayerNumber() == 1) {
                    cueRollRotateDeg1 -= 3;
                } else {
                    cueRollRotateDeg2 -= 3;
                }
            }
        }
    }

    private void keyboardReleaseAction(KeyEvent e) {
        if (replay != null || aiCalculating || playingMovement || cueAnimationPlayer != null) {
            return;
        }
    }

    private void setKeyboardActions() {
        powerSlider.setBlockIncrement(1.0);

        windowRootPane.setOnKeyPressed(this::keyboardAction);
        windowRootPane.setOnKeyReleased(this::keyboardReleaseAction);

        for (Toggle toggle : handSelectionToggleGroup.getToggles()) {
            RadioButton rb = (RadioButton) toggle;
            rb.setOnKeyPressed(this::keyboardAction);
            rb.setOnKeyReleased(this::keyboardReleaseAction);
        }
    }

    private void turnDirectionDeg(double deg) {
        double rad = Math.toRadians(deg);
        double cur = Algebra.thetaOf(cursorDirectionUnitX, cursorDirectionUnitY);
        cur += rad;
        double[] nd = Algebra.unitVectorOfAngle(cur);
        cursorDirectionUnitX = nd[0];
        cursorDirectionUnitY = nd[1];
        recalculateUiRestrictions();
    }

    private void setOnHidden() {
        this.stage.setOnHidden(e -> {
            System.out.println("Hide");
//            DataLoader.getInstance().invalidate();
//            basePane.getChildren().clear();
            stopCueTimer();

            if (replay != null) {
                try {
                    replay.close();
                } catch (IOException ex) {
                    EventLogger.error(ex);
                }
                if (videoCapture != null && !videoCapture.isFinished()) {
                    videoCapture.fail();
                }
            } else {
                if (!game.getGame().getRecorder().isFinished()) {
                    game.getGame().getRecorder().stopRecording(false);
                }
                if (!game.isFinished() && !game.getGame().isEnded()) {
                    Player p1 = game.getGame().getPlayer1();
                    InGamePlayer winner = getFrameWinner();

                    if (careerMatch != null) {
//                        game.saveTo(PlayerVsAiMatch.getMatchSave());
                        boolean matchFinish = game.playerWinsAframe(winner);
                        if (matchFinish) {
                            if (careerMatch instanceof ChallengeMatch) {
                                ((ChallengeMatch) careerMatch).setScore(p1.getScore());
                                careerMatch.finish(game.getGame().getPlayer2().getPlayerPerson(),
                                        0, 1);
                            } else {
                                careerMatch.finish(winner.getPlayerPerson(),
                                        game.getP1Wins(),
                                        game.getP2Wins());  // 这里没用endFrame或者withdraw，因为不想影响数据库
                            }
                        } else {
                            careerMatch.saveMatch();
                            careerMatch.saveAndExit();
                        }
                        CareerManager.getInstance().getInventory().saveToDisk();
                    } else {
                        if (game.getGame().isStarted() || (game.getP1Wins() + game.getP2Wins() > 0)) {
                            boolean matchFinish = game.playerWinsAframe(winner);
                            if (!matchFinish) game.generalSave();  // 这局SL了，还有下一局
                        } else {
                            // 上来第一局没开球就sl，删记录了
                            game.quitGameDeleteRecord();
                            game.getGame().getRecorder().deleteRecord();
                        }
                    }
//                    else {
//                        game.generalSave();
//                    }
                }
            }
//            timeline.stop();
//            gameLoop.cancel();
            gameLoop.stop();
        });
    }

    private InGamePlayer getFrameWinner() {
        InGamePlayer winner;
        if (careerMatch != null) {
            winner = player1.getPlayerType() == PlayerType.PLAYER ?
                    player2 : player1;
        } else {
            winner =
                    (game.getGame().getCuingPlayer() == game.getGame().getPlayer1() ?
                            game.getGame().getPlayer2() : game.getGame().getPlayer1())
                            .getInGamePlayer();
        }
        return winner;
    }

    private void setUiFrameStart() {
        InGamePlayer igp;
        if (replay != null) {
            if (replay.getCueRecord() != null) {
                igp = replay.getCueRecord().cuePlayer;
            } else {
                return;
            }
        } else {
            igp = game.getGame().getCuingPlayer().getInGamePlayer();
            withdrawMenu.setDisable(!igp.isHuman());
        }

//        PlayerPerson playerPerson = igp.getPlayerPerson();

        updatePowerSlider(igp, CuePlayerHand.makeDefault(igp));
        if (replay == null) {
            cueButton.setDisable(false);
            if (igp.getPlayerType() == PlayerType.COMPUTER) {
                cueButton.setText(strings.getString("aiCueText"));
            } else {
                cueButton.setText(strings.getString("cueText"));
                enableDisabledUi();
            }
        }
        updateScoreDiffLabels();
    }

    private void updateScoreDiffLabels() {
        if (gameValues.rule.snookerLike()) {
            if (replay != null) {

            } else {
                AbstractSnookerGame asg = (AbstractSnookerGame) game.getGame();
                snookerScoreDiffLabel.setText(String.format(strings.getString("scoreDiff"),
                        asg.getScoreDiffAbs()));
                snookerScoreRemainingLabel.setText(String.format(strings.getString("scoreRem"),
                        asg.getRemainingScore(asg.isDoingSnookerFreeBll())));
            }
        }
    }

    public void finishCueReplay() {
        replayStopTime = gameLoop.currentTimeMillis();

        drawScoreBoard(null, true);
        drawTargetBoard(true);
        restoreCuePoint();
        restoreCueAngle();
        updateScoreDiffLabels();

//        replayNextCueButton.setDisable(false);
        replayNextCueButton.setText(strings.getString("replayNextCue"));
        replayNextCueButton.setOnAction(this::replayNextCueAction);
        replayLastCueButton.setDisable(false);

        tableGraphicsChanged = true;
    }

    private void replayLoadNext() {
        if (videoCapture != null) {
            if (replay.getCueIndex() >= videoCapture.endCueIndex) {
                videoCapture.success();
                gameLoop.stop();
                stage.close();
                return;
            }
        }
        if (replay.loadNext()) {
            drawTargetBoard(false);
            replayCue();
        } else {
            System.out.println("Replay finished!");
            if (videoCapture != null) {
                videoCapture.success();
                gameLoop.stop();
                stage.close();
            }
        }
        showReplayCueStep();
    }

    void updatePowerSlider() {
        InGamePlayer igp = getActiveHolder().getCuingIgp();
        CuePlayerHand cph;
        if (currentHand != null) cph = currentHand;
        else cph = CuePlayerHand.makeDefault(igp);
        updatePowerSlider(igp, cph, false);
    }

    private void updatePowerSlider(InGamePlayer igp, CuePlayerHand cuingHand) {
        updatePowerSlider(igp, cuingHand, true);
    }

    private void updatePowerSlider(InGamePlayer igp, CuePlayerHand cuingHand, boolean resetPower) {
        if (resetPower) {
            powerSlider.setValue(DEFAULT_POWER);
        }
        double maxPower = CuePlayParams.powerWithCueAngle(
                igp.getPlayerPerson().handBody,
                igp.getCueSelection().getSelected().brand,
                cuingHand.getMaxPowerPercentage(),
                cueAngleDeg
        );
        if (powerSlider.getValue() > maxPower) {
            powerSlider.setValue(maxPower);
        }
        double ctrlPower = CuePlayParams.powerWithCueAngle(
                igp.getPlayerPerson().handBody,
                igp.getCueSelection().getSelected().brand,
                cuingHand.getControllablePowerPercentage(),
                cueAngleDeg
        );

        double sliderHeight = powerSlider.getHeight() - 12;  // 可能的上下margin
        if (sliderHeight == -12) return;
        sliderCtrlLabel.setVisible(true);
        sliderMaxLabel.setVisible(true);
        sliderZeroLabel.setVisible(true);

        double sliderY = powerSlider.getLayoutY() + 6;
        double labelH = sliderCtrlLabel.getHeight() / 2;
        sliderCtrlLabel.setText(String.format("- %.1f", ctrlPower));
        sliderMaxLabel.setText(String.format("- %.1f", maxPower));
        sliderZeroLabel.setText("- 0.0");

        sliderCtrlLabel.setLayoutY(sliderHeight * (100 - ctrlPower) / 100 - labelH + sliderY);
        sliderMaxLabel.setLayoutY(sliderHeight * (100 - maxPower) / 100 - labelH + sliderY);
        sliderZeroLabel.setLayoutY(sliderHeight - labelH + sliderY);
    }

    public void finishCue(Player justCuedPlayer, Player nextCuePlayer) {
//        updateCuePlayerSinglePole(justCuedPlayer);
        if (cueAnimationPlayer != null) {
            endCueAnimation();
        }
        oneFrame();  // 处理一下比如说斯诺克捡起彩球这种，不然在AI算出来下一步之前，球不会显示
        drawScoreBoard(justCuedPlayer, true);
        drawTargetBoard(true);
        restoreCuePoint();
        restoreCueAngle();
        updateScoreDiffLabels();

        if (game.getGame() instanceof NeedBigBreak nbb) {
            if (nbb.isJustAfterBreak()) {
                tryAutoChangeBreakCueBack(justCuedPlayer);
            }
        }

        Platform.runLater(() -> updatePowerSlider(nextCuePlayer.getInGamePlayer(),
                CuePlayerHand.makeDefault(nextCuePlayer.getInGamePlayer())));
        setButtonsCueEnd(nextCuePlayer);
        obstacleProjection = null;
        printPlayStage = true;

        if (curDefAttempt != null) {
            curDefAttempt.setAfterScoreUpdate(
                    game.getGame().getNewPottedLegal(),
                    !game.getGame().isThisCueFoul()
            );
            if (curDefAttempt.isBreaking() && curDefAttempt.defensePlayer instanceof NeedBigBreakPlayer nbp) {
                curDefAttempt.setSuccess(nbp.isBreakSuccess());
            }
        }

        ScoreResult scoreResult = game.getGame().makeScoreResult(justCuedPlayer);

        try {
            game.getGame().getRecorder().recordScore(scoreResult);
            game.getGame().getRecorder().recordNextTarget(makeTargetRecord(nextCuePlayer));
            game.getGame().getRecorder().writeCueToStream();
        } catch (RecordingException re) {
            EventLogger.error(re);
            game.getGame().abortRecording();
        }

        InGamePlayer justCuedIgp = justCuedPlayer.getInGamePlayer();
        InGamePlayer opponentIgp = game.getGame().getAnotherIgp(justCuedIgp);

        justCuedIgp.updatePsyStatusAfterSelfCue(
                game.getGame().frameImportance(justCuedIgp.getPlayerNumber()),
                lastPotAttempt,
                curDefAttempt,
                game.getGame().isThisCueFoul(),
                gamePlayStage()
        );
        opponentIgp.updatePsyStatusAfterOpponentCue(
                game.getGame().frameImportance(opponentIgp.getPlayerNumber()),
                lastPotAttempt
        );

        AchManager.getInstance().updateAfterCueFinish(gamePane, game.getGame(), scoreResult,
                lastPotAttempt, curDefAttempt, gamePlayStage());
        Platform.runLater(() -> AchManager.getInstance().showAchievementPopup());

        FoulInfo foulInfo = game.getGame().getThisCueFoul();
        if (foulInfo.isFoul()) {
            String foulReason0 = game.getGame().getFoulReason();
            if (game.getGame() instanceof AbstractSnookerGame) {
                if (foulInfo.isMiss()) {
                    foulReason0 = strings.getString("foulAndMiss") + foulReason0;
                }
            }
            String foulReason = foulReason0;
            String headerReason = foulInfo.getHeaderReason(strings);

            Platform.runLater(() -> {
                AlertShower.showInfo(
                        stage,
                        foulReason,
                        headerReason,
                        3000
                );
                if (game.getGame().isEnded()) {
                    endFrame();
                } else {
                    finishCueNextStep(nextCuePlayer);
                }
            });
        } else {
            if (game.getGame().isEnded()) {
                endFrame();
            } else {
                finishCueNextStep(nextCuePlayer);
            }
        }
    }

    private void autoAimEasiestNextBall(Player nextCuePlayer) {
        if (game.getGame().getCueBall().isPotted()) return;
        Ball tgt = game.getGame().getEasiestTarget(nextCuePlayer);
        if (tgt == null) return;

        double dx = tgt.getX() - game.getGame().getCueBall().getX();
        double dy = tgt.getY() - game.getGame().getCueBall().getY();

        double[] unit = Algebra.unitVector(dx, dy);
        cursorDirectionUnitX = unit[0];
        cursorDirectionUnitY = unit[1];
        recalculateUiRestrictions(true);

        tableGraphicsChanged = true;
        aimingChanged();
    }

    @Deprecated
    private void predictPlayerPath(Player humanPlayer) {
        Thread thread = new Thread(() -> {
            System.out.println("ai predicting human player path");
            long st = System.currentTimeMillis();
            if (game.getGame().isBallInHand()) {
                return;
            }
            AiCueResult cueResult = game.getGame().aiCue(humanPlayer, game.predictPhy);
            System.out.println("ai predicting human player path in " + (System.currentTimeMillis() - st) +
                    " ms, result: " + cueResult);
            if (cueResult != null) {
                suggestedPlayerWhitePath = cueResult.getWhitePath();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void finishCueNextStep(Player nextCuePlayer) {
        cuePointCanvas.setDisable(false);
        cueAngleCanvas.setDisable(false);

        cursorDrawer.synchronizeGame();  // 刷新白球预测的线程池

        if (showTipBrokenMsg) {
            Platform.runLater(() -> AlertShower.showInfo(stage,
                    strings.getString("tipBrokenTip"),
                    strings.getString("tipBroken")));

            showTipBrokenMsg = false;
        }

        Ball.enableGearOffset();
        aiWhitePath = null;
        miscued = false;
        if (nextCuePlayer.getInGamePlayer().getPlayerType() == PlayerType.PLAYER && !aiHelpPlay) {
            boolean autoAim = true;

            if (game.getGame().canReposition()) {
                System.out.println("Solvable snooker");
                autoAim = false;  // 把autoAim交给askReposition的不复位分支
                repositionMenu.setDisable(false);
                askReposition();
            }
            if (game.getGame() instanceof NeedBigBreak nbb) {
                if (nbb.isJustAfterBreak()) {
                    if (nbb.wasIllegalBreak()) {
                        letOtherPlayMenu.setDisable(false);
                    }
                }
            }
            if (game.getGame().getThisCueFoul().isFoul()) {
                if (game.getGame().getGameValues().rule.hasRule(Rule.FOUL_LET_OTHER_PLAY)) {
                    letOtherPlayMenu.setDisable(false);
                }
            }
            if (game.getGame() instanceof AmericanNineBallGame g) {
                if (g.currentlyCanPushOut()) {
                    pushOutMenu.setDisable(false);
                }
                if (g.lastCueWasPushOut()) {
                    letOtherPlayMenu.setDisable(false);
                }
            }

            if (autoAim) autoAimEasiestNextBall(nextCuePlayer);
            if (predictPlayerPathItem.isSelected()) {
                predictPlayerPath(nextCuePlayer);
            }
        } else {
            if (!game.isFinished() &&
                    aiAutoPlay) {
                Platform.runLater(() -> aiCue(nextCuePlayer));
            }
        }
        updatePlayStage();
        recalculateUiRestrictions();

        tableGraphicsChanged = true;

        startCueTimer();
    }

    private void updateChampionshipBreaks(SnookerChampionship sc,
                                          PlayerVsAiMatch pva,
                                          SnookerPlayer player,
                                          int nFrameFrom1) {
        for (Integer breakScore : player.getSinglePolesInThisGame()) {
            if (pva.metaMatchInfo == null) {
                System.err.println("Match too old! No meta info!");
            } else {
                sc.updateBreakScore(player.getPlayerPerson().getPlayerId(), pva.stage, breakScore,
                        player.getMaximumType(),
                        false,
                        pva.metaMatchInfo.toString(), nFrameFrom1);
            }
        }
    }

    private void startNextFrame() {
        game.startNextFrame();
        setupBalls();

        predictedTargetBall = null;
        aiWhitePath = null;
        suggestedPlayerWhitePath = null;
        cursorDirectionUnitX = 0.0;
        cursorDirectionUnitY = 0.0;

        drawScoreBoard(game.getGame().getCuingPlayer(), true);
        drawTargetBoard(true);
        setUiFrameStart();

        endCueAnimation();
        if (cursorDrawer == null) {
            cursorDrawer = new PredictionDrawing();
        } else {
            cursorDrawer.synchronizeGame();
        }

        Player breakPlayer = game.getGame().getCuingPlayer();
        updateHandSelection(true);
        updatePowerSlider(breakPlayer.getInGamePlayer(), CuePlayerHand.makeDefault(breakPlayer.getInGamePlayer()));
        if (breakPlayer.getInGamePlayer().isHuman()) {
            changeCueButton.setDisable(false);
            tryAutoChangeBreakCue(breakPlayer);
        } else {
            changeCueButton.setDisable(true);
        }

        InGamePlayer igp1 = game.getPlayer1(), igp2 = game.getPlayer2();
        igp1.adjustPsyStatusFrameBegin(game.getGame().frameImportance(igp1.getPlayerNumber()));
        igp2.adjustPsyStatusFrameBegin(game.getGame().frameImportance(igp2.getPlayerNumber()));

        tableGraphicsChanged = true;

        AchManager.getInstance().showAchievementPopup();
    }

    private void endFrame() {
        stopCueTimer();
        hideCue();
        tracedMovement = null;
        tableGraphicsChanged = true;
        Player p1 = game.getGame().getPlayer1();
        Player wonPlayer = game.getGame().getWiningPlayer();

        InGamePlayer wonIgp = wonPlayer.getInGamePlayer();
        InGamePlayer lostIgp = game.getGame().getAnotherIgp(wonIgp);
        // 在game.playerWinsAframe()之前，否则影响frameImportance的判断
        wonIgp.updatePsyStatusAfterFrame(game.getGame().frameImportance(wonIgp.getPlayerNumber()), true, game);
        lostIgp.updatePsyStatusAfterFrame(game.getGame().frameImportance(lostIgp.getPlayerNumber()), false, game);

        boolean entireGameEnd = game.playerWinsAframe(wonPlayer.getInGamePlayer());
        drawScoreBoard(game.getGame().getCuingPlayer(), false);
        game.getGame().getRecorder().stopRecording(true);

        if (gameValues.isTraining()) {
            boolean success = wonPlayer.getInGamePlayer().getPlayerNumber() == 1;
            String title = success ? strings.getString("challengeSuccess") :
                    strings.getString("challengeFailed");

            final String content;
            if (careerMatch != null) {
                ((ChallengeMatch) careerMatch).setScore(p1.getScore());
                content = ((ChallengeMatch) careerMatch).challengeSet.getName();
                careerMatch.finish(wonPlayer.getPlayerPerson(), success ? 1 : 0, success ? 0 : 1);
                CareerManager.getInstance().getInventory().saveToDisk();
            } else {
                content = gameValues.getTrainType().toString();
            }
            Platform.runLater(() ->
            {
                AlertShower.showInfo(stage,
                        content,
                        title);
                AlertShower.askConfirmation(stage,
                        strings.getString("finishAndCloseHint"),
                        strings.getString("finishAndClose"),
                        this::closeWindowAction,
                        null);
            });
        } else {
            int frameNFrom1 = game.getP1Wins() + game.getP2Wins();  // 上面已经更新了

            if (careerMatch != null) {
                if (careerMatch instanceof PlayerVsAiMatch pva) {
                    if (gameValues.rule.snookerLike()) {
                        SnookerChampionship sc = (SnookerChampionship) pva.getChampionship();
                        SnookerPlayer sp1 = (SnookerPlayer) game.getGame().getPlayer1();
                        SnookerPlayer sp2 = (SnookerPlayer) game.getGame().getPlayer2();
                        updateChampionshipBreaks(sc, pva, sp1, frameNFrom1);
                        updateChampionshipBreaks(sc, pva, sp2, frameNFrom1);
                    }
                }
                careerMatch.saveMatch();
                CareerManager.getInstance().getInventory().saveToDisk();
            } else {
                game.generalSave();
            }

            Platform.runLater(() -> {
                AlertShower.showInfo(stage,
                        String.format("%s  %d (%d) : (%d) %d  %s",
                                game.getPlayer1().getPlayerPerson().getName(),
                                game.getGame().getPlayer1().getScore(),
                                game.getP1Wins(),
                                game.getP2Wins(),
                                game.getGame().getPlayer2().getScore(),
                                game.getPlayer2().getPlayerPerson().getName()),
                        String.format(strings.getString("winsAFrame"), wonPlayer.getPlayerPerson().getName()),
                        3000);

                if (entireGameEnd) {
                    if (careerMatch != null) {
                        careerMatch.finish(wonPlayer.getPlayerPerson(), game.getP1Wins(), game.getP2Wins());
                    }
                    AchManager.getInstance().updateAfterMatchEnds(game);
                    AchManager.getInstance().showAchievementPopup();

                    AlertShower.showInfo(stage,
                            String.format("%s (%d) : (%d) %s",
                                    game.getPlayer1().getPlayerPerson().getName(),
                                    game.getP1Wins(),
                                    game.getP2Wins(),
                                    game.getPlayer2().getPlayerPerson().getName()),
                            String.format(strings.getString("winsAMatch"), wonPlayer.getPlayerPerson().getName()));

                    AlertShower.askConfirmation(stage,
                            strings.getString("finishAndCloseHint"),
                            strings.getString("finishAndClose"),
                            this::closeWindowAction,
                            null);
                } else {
                    AchManager.getInstance().showAchievementPopup();
                    AlertShower.askConfirmation(
                            stage,
                            strings.getString("ifStartNextFrameContent"),
                            strings.getString("ifStartNextFrame"),
                            strings.getString("yes"),
                            strings.getString("saveAndExit"),
                            this::startNextFrame,
                            () -> {
                                if (careerMatch != null) {
                                    careerMatch.saveMatch();
                                    careerMatch.saveAndExit();
                                } else {
                                    game.generalSave();
                                }
                                stage.hide();
                            }
                    );
                }
            });
        }
    }

    private GameHolder getActiveHolder() {
        if (replay != null) return replay;
        else return game.getGame();
    }

    private void setupBalls() {
        GameHolder gameHolder = getActiveHolder();
        gamePane.setupBalls(gameHolder, !gameHolder.getGameValues().isTraining());
    }

    private void tryAutoChangeBreakCue(Player breaker) {
        if (game.getGame() instanceof NeedBigBreak nbb && nbb.isBreaking()) {

            ConfigLoader cl = ConfigLoader.getInstance();
            if (cl.getBoolean("autoChangeBreakCue", false)) {
                InGamePlayer cuingIgp = breaker.getInGamePlayer();
                if (!cuingIgp.isHuman()) return;
                CueSelection cueSelection = cuingIgp.getCueSelection();
                CueSelection.CueAndBrand bestBreakCue = null;
                for (CueSelection.CueAndBrand cab : cueSelection.getAvailableCues()) {
                    if (cab.brand.isBreakCue()) {
                        bestBreakCue = cab;
                    }
                }
                if (bestBreakCue == null) return;

                lastUsedCue = cueSelection.getSelected();
                cueSelection.select(bestBreakCue);
            }
        }
    }

    private void tryAutoChangeBreakCueBack(Player breakedPlayer) {
        InGamePlayer breakedIgp = breakedPlayer.getInGamePlayer();
        if (!breakedIgp.isHuman()) return;

        ConfigLoader cl = ConfigLoader.getInstance();
        if (cl.getBoolean("autoChangeBreakCue", false)) {
            CueSelection cueSelection = breakedIgp.getCueSelection();
            if (lastUsedCue != null &&
                    cueSelection.getSelected().brand.isBreakCue() &&
                    cueSelection.hasThisBrand(lastUsedCue.brand)) {
                cueSelection.select(lastUsedCue);
            }
        }
    }

    private void askReposition() {
        Platform.runLater(() ->
                AlertShower.askConfirmation(stage,
                        strings.getString("ifReposition"),
                        strings.getString("oppoFoul"),
                        this::repositionAction,
                        this::notReposition));
    }

    private void notReposition() {
        AbstractSnookerGame asg = (AbstractSnookerGame) game.getGame();
        asg.notReposition();

        letOtherPlayMenu.setDisable(false);
        autoAimEasiestNextBall(game.getGame().getCuingPlayer());
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
        return getCuingCue().getCueTipWidth() / gameValues.ball.ballDiameter;  // 故意用的杆头而不是皮头的粗细
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

    private void setCuePoint(double x, double y, boolean byButton) {
        if (Algebra.distanceToPoint(x, y, cueCanvasWH / 2, cueCanvasWH / 2) <
                cueAreaRadius) {
            double ratioCueAndBall = getRatioOfCueAndBall();

            double curX = getCuePointRelX(cuePointX), curY = getCuePointRelY(cuePointY);
            double newX = getCuePointRelX(x), newY = getCuePointRelY(y);
            double[] curPos = new double[]{curX, curY};
            double[] newPos = new double[]{newX, newY};

            boolean curInArea = true;
            boolean newInArea = true;
            if (cueAbleArea != null) {
                curInArea = PointInPoly.pointInPoly(curPos, cueAbleArea);
                newInArea = PointInPoly.pointInPoly(newPos, cueAbleArea);
//                System.out.println(curInArea + " " + newInArea + Arrays.toString(newPos));
            } else {
                System.out.println("Cue able area is null!");
            }

            if (obstacleProjection == null
                    || obstacleProjection.cueAble(
                    newX, newY, ratioCueAndBall)) {

                if (byButton) {
                    if (newInArea || !curInArea) {
                        cuePointX = x;
                        cuePointY = y;
                    }
                } else {
                    cuePointX = x;
                    cuePointY = y;
                }

                recalculateUiRestrictions();
            } else if (byButton) {
                // obstacleProjection 一定!= null
                boolean curCueAble = curInArea && obstacleProjection.cueAble(
                        getCuePointRelX(cuePointX), getCuePointRelY(cuePointY), ratioCueAndBall);
                boolean newCueAble = newInArea && obstacleProjection.cueAble(
                        getCuePointRelX(x), getCuePointRelY(y), ratioCueAndBall);
                if (newCueAble || !curCueAble) {
                    // 只要不是从可以打的地方调到打不了的地方，都允许
                    cuePointX = x;
                    cuePointY = y;
                    recalculateUiRestrictions();
                }
            }
        }
    }

    private void setCueAnglePoint(double x, double y) {
        double relX = x - cueAngleBaseHor;
        double relY = cueAngleCanvas.getHeight() - cueAngleBaseVer - y;
        double rad = Math.atan2(relY, relX);
        double deg = Math.toDegrees(rad);
        setCueAngleDeg(deg);
    }

    private void setCueAngleDeg(double newDeg) {
        cueAngleDeg = Math.min(MAX_CUE_ANGLE, Math.max(0, newDeg));
        recalculateUiRestrictions();
        setCueAngleLabel();
    }

    private void restoreCueAngle() {
        cueAngleDeg = 5.0;
        recalculateUiRestrictions();
        setCueAngleLabel();
    }

    private void setCueAngleLabel() {
        cueAngleLabel.setText(String.format("%.1f°", cueAngleDeg));
    }

    private void onCuePointCanvasClicked(MouseEvent mouseEvent) {
        if (replay != null || playingMovement || aiCalculating || cueAnimationPlayer != null)
            return;

        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            setCuePoint(mouseEvent.getX(), mouseEvent.getY(), false);
        } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            restoreCuePoint();
        }
    }

    private void onCuePointCanvasDragged(MouseEvent mouseEvent) {
        if (replay != null || playingMovement || aiCalculating || cueAnimationPlayer != null)
            return;

        setCuePoint(mouseEvent.getX(), mouseEvent.getY(), true);
    }

    private void onCueAngleCanvasClicked(MouseEvent mouseEvent) {
        if (replay != null || playingMovement || aiCalculating || cueAnimationPlayer != null)
            return;

        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            setCueAnglePoint(mouseEvent.getX(), mouseEvent.getY());
        } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            restoreCueAngle();
        }
    }

    private void onCueAngleCanvasDragged(MouseEvent mouseEvent) {
        if (replay != null || playingMovement || aiCalculating || cueAnimationPlayer != null)
            return;

        setCueAnglePoint(mouseEvent.getX(), mouseEvent.getY());
    }

    private void inspectionClick(MouseEvent mouseEvent) {
        double realX = gamePane.realX(mouseEvent.getX());
        double realY = gamePane.realY(mouseEvent.getY());

        for (Ball ball : game.getGame().getAllBalls()) {
            if (!ball.isPotted()) {
                if (Algebra.distanceToPoint(realX, realY, ball.getX(), ball.getY()) < gameValues.ball.ballRadius) {
                    potInspection.setSrcBall(ball);
                    break;
                }
            }
        }
    }

    private void debugClick(MouseEvent mouseEvent) {
        double realX = gamePane.realX(mouseEvent.getX());
        double realY = gamePane.realY(mouseEvent.getY());
        if (debuggingBall == null) {
            for (Ball ball : game.getGame().getAllBalls()) {
                if (!ball.isPotted()) {
                    if (Algebra.distanceToPoint(realX, realY, ball.getX(), ball.getY()) < gameValues.ball.ballRadius) {
                        debuggingBall = ball;
                        ball.setPotted(true);
                        cursorDrawer.synchronizeGame();
                        break;
                    }
                }
            }
        } else {
            double[] ballRealPos = gamePane.getRealPlaceCanPlaceBall(mouseX, mouseY);
            if (game.getGame().isInTable(ballRealPos[0], ballRealPos[1]) &&
                    !game.getGame().isOccupied(ballRealPos[0], ballRealPos[1])) {
                debuggingBall.setX(ballRealPos[0]);
                debuggingBall.setY(ballRealPos[1]);
                debuggingBall.setPotted(false);
                debuggingBall = null;
                cursorDrawer.synchronizeGame();
            }
        }
    }

    private void onSingleClick(MouseEvent mouseEvent) {
//        System.out.println("Clicked!");
        if (replay != null) return;
        if (playingMovement) return;
        if (aiCalculating) return;
        if (cueAnimationPlayer != null) return;
        if (game.getGame().getCuingPlayer().getInGamePlayer().getPlayerType() ==
                PlayerType.COMPUTER) {
            if (debugMode) {
                debugClick(mouseEvent);
            }
            return;
        }

        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            if (potInspectionMenu.isSelected()) {
                inspectionClick(mouseEvent);
            } else if (debugMode) {
                debugClick(mouseEvent);
            } else if (game.getGame().getCueBall().isPotted()) {
                // 放置手中球
                double[] ballRealPos = gamePane.getRealPlaceCanPlaceBall(mouseEvent.getX(), mouseEvent.getY());

                game.getGame().placeWhiteBall(ballRealPos[0], ballRealPos[1]);
                game.getGame().getRecorder().writeBallInHandPlacement();

                replaceBallInHandMenu.setDisable(false);
                cursorDrawer.synchronizeGame();
                startCueTimer();
            } else if (!game.getGame().isCalculating() && movement == null) {
                Ball whiteBall = game.getGame().getCueBall();
                double[] unit = Algebra.unitVector(
                        new double[]{
                                gamePane.realX(mouseEvent.getX()) - whiteBall.getX(),
                                gamePane.realY(mouseEvent.getY()) - whiteBall.getY()
                        });
                cursorDirectionUnitX = unit[0];
                cursorDirectionUnitY = unit[1];
                recalculateUiRestrictions();
                System.out.println("New direction: " + cursorDirectionUnitX + ", " + cursorDirectionUnitY);
            }
        } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            if (potInspection != null) {
                // 右键清除
                potInspection.setSrcBall(null);
                tableGraphicsChanged = true;
            }
        }
    }

    private void onMouseMoved(MouseEvent mouseEvent) {
        mouseX = mouseEvent.getX();
        mouseY = mouseEvent.getY();

        if (potInspection != null && potInspection.getSrcBall() != null) {
            tableGraphicsChanged = true;
        }
    }

    private void onDragStarted(MouseEvent mouseEvent) {
        if (playingMovement) return;
        if (replay != null) return;
        if (aiCalculating) return;
        if (cueAnimationPlayer != null) return;
        if (game.getGame().getCuingPlayer().getInGamePlayer().getPlayerType() ==
                PlayerType.COMPUTER) return;

        Ball white = game.getGame().getCueBall();
        if (white.isPotted()) return;
        isDragging = true;
        double xDiffToWhite = gamePane.realX(mouseEvent.getX()) - white.getX();
        double yDiffToWhite = gamePane.realY(mouseEvent.getY()) - white.getY();
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
        if (!isDragging) return;

        Ball white = game.getGame().getCueBall();
        if (white.isPotted()) return;

        double xDiffToWhite = gamePane.realX(mouseEvent.getX()) - white.getX();
        double yDiffToWhite = gamePane.realY(mouseEvent.getY()) - white.getY();
        double[] unitDir = Algebra.unitVector(xDiffToWhite, yDiffToWhite);

        if (absoluteDragAngle) {
            cursorDirectionUnitX = unitDir[0];
            cursorDirectionUnitY = unitDir[1];

            lastDragAngle = Algebra.thetaOf(unitDir);  // 没用，只是为了统一
        } else {
            double distanceToWhite = Math.hypot(xDiffToWhite, yDiffToWhite);  // 光标离白球越远，移动越慢
            double aimingAngle = Algebra.thetaOf(cursorDirectionUnitX, cursorDirectionUnitY);
            double curAngle = Algebra.thetaOf(unitDir);
            double delta = Algebra.normalizeAngle(curAngle - lastDragAngle);
            double change = delta / Math.max(1, distanceToWhite / 500);  // 避免鬼畜地转
            double resultAngle = aimingAngle + change;
            double[] newUnitVector = Algebra.unitVectorOfAngle(resultAngle);
            cursorDirectionUnitX = newUnitVector[0];
            cursorDirectionUnitY = newUnitVector[1];

            lastDragAngle = curAngle;
        }
        recalculateUiRestrictions();
    }

    private void onDragEnd(MouseEvent mouseEvent) {
        isDragging = false;
        lastDragAngle = 0.0;
        stage.getScene().setCursor(Cursor.DEFAULT);
    }

    @FXML
    public void closeWindowAction() {
        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    @FXML
    void terminateAction() {
        game.getGame().forcedTerminate();
        movement = null;
        playingMovement = false;
        if (cueAnimationPlayer != null) {
            endCueAnimation();
        }
        finishCueNextStep(game.getGame().getCuingPlayer());
//        setButtonsCueEnd(game.getGame().getCuingPlayer());
    }

    @FXML
    void debugModeAction() {
        debugMode = !debugMode;
        if (debugMode) {
            debugModeMenu.setText("normal mode");
        } else {
            debugModeMenu.setText("debug mode");
        }
    }

    @FXML
    void testAction() {
        movement = game.getGame().collisionTest();
        playMovement();
    }

    @FXML
    void tieTestAction() {
        game.getGame().tieTest();
        drawTargetBoard(true);
        drawScoreBoard(game.getGame().getCuingPlayer(), true);
    }

    @FXML
    void clearRedBallsAction() {
        game.getGame().clearRedBallsTest();
        drawTargetBoard(true);
    }

    @FXML
    void p1AddScoreAction() {
        game.getGame().getPlayer1().addScore(10);
        drawScoreBoard(game.getGame().getPlayer2(), false);
    }

    @FXML
    void p2AddScoreAction() {
        game.getGame().getPlayer2().addScore(10);
        drawScoreBoard(game.getGame().getPlayer2(), false);
    }

    @FXML
    void withdrawAction() {
        if (game.getGame() instanceof AbstractSnookerGame asg) {
            SnookerPlayer curPlayer = asg.getCuingPlayer();
            int diff = asg.getScoreDiff(curPlayer);
            String behindText = diff <= 0 ? strings.getString("scoreBehind") : strings.getString("scoreAhead");
            AlertShower.askConfirmation(
                    stage,
                    String.format(strings.getString("confirmWithdrawContent"), behindText, Math.abs(diff),
                            asg.getRemainingScore(asg.isDoingSnookerFreeBll())),
                    String.format(strings.getString("confirmWithdraw"), curPlayer.getPlayerPerson().getName()),
                    () -> withdraw(curPlayer),
                    null
            );
        } else {
            Player curPlayer = game.getGame().getCuingPlayer();
            AlertShower.askConfirmation(
                    stage,
                    "......",
                    String.format(strings.getString("confirmWithdraw"), curPlayer.getPlayerPerson().getName()),
                    () -> withdraw(curPlayer),
                    null
            );
        }
    }

    private void withdraw(Player curPlayer) {
        game.getGame().withdraw(curPlayer);
        endFrame();
    }

    private void showReplayCueStep() {
        replayCueNumLabel.setText(String.format(strings.getString("cueNumFmt"), replay.getCueIndex()));
    }

    @FXML
    void replayFastForwardAction(ActionEvent event) {
        playingMovement = false;
        movement = null;
        cueAnimationPlayer = null;
        finishCueReplay();
//        hideCue();
    }

    @FXML
    void replayNextCueAction(ActionEvent event) {
        replayLoadNext();
    }

    @FXML
    void replayLastCueAction() {
        replay.loadLast();
        playingMovement = false;
        movement = null;
        cueAnimationPlayer = null;

        showReplayCueStep();

        drawTargetBoard(false);
        drawScoreBoard(null, false);
        restoreCuePoint();
        restoreCueAngle();
        updateScoreDiffLabels();
    }

    @FXML
    void clearTraceAction() {
        tracedMovement = null;
        tableGraphicsChanged = true;
    }

    @FXML
    void pushOutAction() {
        if (game.getGame() instanceof AmericanNineBallGame g) {
            cursorDirectionUnitX = 0.0;
            cursorDirectionUnitY = 0.0;
            hideCue();
            tableGraphicsChanged = true;

            g.pushOut();

            cursorDrawer.synchronizeGame();

            drawScoreBoard(game.getGame().getCuingPlayer(), true);
            drawTargetBoard(true);
            InGamePlayer igp = game.getGame().getCuingPlayer().getInGamePlayer();
            updatePowerSlider(igp, CuePlayerHand.makeDefault(igp));
            draw();
        } else {
            System.err.println("Game rule no push out!");
        }
    }

    @FXML
    void repositionAction() {
        cursorDirectionUnitX = 0.0;
        cursorDirectionUnitY = 0.0;
        hideCue();
        tableGraphicsChanged = true;

        game.getGame().reposition();
        repositionMenu.setDisable(true);
        letOtherPlayMenu.setDisable(true);

        updateHandSelection(true);
        cursorDrawer.synchronizeGame();
        drawScoreBoard(game.getGame().getCuingPlayer(), true);
        drawTargetBoard(true);
        InGamePlayer igp = game.getGame().getCuingPlayer().getInGamePlayer();
        updatePowerSlider(igp, CuePlayerHand.makeDefault(igp));
        draw();
        if (game.getGame() instanceof AbstractSnookerGame asg) {
            if (asg.isNoHitThreeWarning()) {
                showThreeNoHitWarning();
            }
        }
        startCueTimer();
        if (aiAutoPlay &&
                game.getGame().getCuingPlayer().getInGamePlayer().getPlayerType() == PlayerType.COMPUTER) {
//                                Platform.runLater(() -> aiCue(game.getGame().getCuingPlayer()));
            aiCue(game.getGame().getCuingPlayer());
        }
    }

    @FXML
    void letOtherPlayAction() {
        letOtherPlayMenu.setDisable(true);
        repositionMenu.setDisable(true);
        restoreCuePoint();
        restoreCueAngle();
        cursorDirectionUnitX = 0.0;
        cursorDirectionUnitY = 0.0;

        hideCue();

        game.getGame().letOtherPlay();

        curDefAttempt = null;
        lastPotAttempt = null;

        Player willPlayPlayer = game.getGame().getCuingPlayer();
        updatePowerSlider(willPlayPlayer.getInGamePlayer(), CuePlayerHand.makeDefault(willPlayPlayer.getInGamePlayer()));
        setButtonsCueEnd(willPlayPlayer);
        drawScoreBoard(willPlayPlayer, true);
        drawTargetBoard(true);
        updateScoreDiffLabels();

        cursorDrawer.synchronizeGame();

        if (aiAutoPlay && willPlayPlayer.getInGamePlayer().getPlayerType() == PlayerType.COMPUTER) {
            aiCue(willPlayPlayer, false);  // 老子给你让杆，你复位？豁哥哥
        }
    }

    @FXML
    void replaceBallInHandAction() {
        restoreCuePoint();
        restoreCueAngle();
        cursorDirectionUnitX = 0.0;
        cursorDirectionUnitY = 0.0;
        hideCue();
        game.getGame().forceSetBallInHand();

        cursorDrawer.synchronizeGame();

        startCueTimer();
    }

    @FXML
    void cueAction() {
        if (game.getGame().isEnded() || cueAnimationPlayer != null) return;

        if (replay != null) {
            return;
        }

        suggestedPlayerWhitePath = null;
        tableGraphicsChanged = true;

        replaceBallInHandMenu.setDisable(true);
        letOtherPlayMenu.setDisable(true);
        repositionMenu.setDisable(true);
        pushOutMenu.setDisable(true);

        Player player = game.getGame().getCuingPlayer();
        if (player.getInGamePlayer().getPlayerType() == PlayerType.COMPUTER || aiHelpPlay) {
            setButtonsCueStart();
            aiCue(player);
        } else {
            if (cursorDirectionUnitX == 0.0 && cursorDirectionUnitY == 0.0) return;
            setButtonsCueStart();
            playerCue(player);
        }
    }

    @FXML
    void changeCueAction() {
        InGamePlayer cuingIgp = game.getGame().getCuingIgp();
        if (cuingIgp != null) {
            CueSelection selection = cuingIgp.getCueSelection();
            Consumer<CueSelection.CueAndBrand> callback = sel -> {
                cursorDirectionUnitX = 0;
                cursorDirectionUnitY = 0;
                recalculateUiRestrictions();
                tableGraphicsChanged = true;
                sel.getCueInstance().setLastSelectTime();
                if (careerMatch != null) {
                    CareerManager cm = CareerManager.getInstance();
                    cm.getCache().put("playerCue",
                            sel.getCueInstance().getInstanceId());
                    cm.saveCache();
                }
                draw();
            };

            if (careerMatch != null && cuingIgp.isHuman()) {
                CueSelectionView.showCueSelectionView(selection,
                        stage,
                        this::hideCue,
                        callback,
                        CareerManager.getInstance().getHumanPlayerCareer());
            } else {
                CueSelectionView.showCueSelectionView(selection,
                        stage,
                        this::hideCue,
                        callback);
            }
        }
    }

    @FXML
    void repairAction() {
        AlertShower.askConfirmation(stage,
                strings.getString("repairDes"),
                strings.getString("repairMenu"),
                strings.getString("confirm"),
                strings.getString("cancel"),
                this::repair,
                null
        );
    }

    private void repair() {
        if (aiCalculating) {
            AlertShower.showInfo(stage,
                    "",
                    strings.getString("repairAiCalculating"));
            return;
        }

        try {
            game.getGame().forcedTerminate();
            game.getGame().validateBalls();
            if (movement != null ||
                    playingMovement ||
                    cueAnimationPlayer != null) {
                movement = null;
                playingMovement = false;
                cueAnimationPlayer = null;

                game.getGame().finishMove(this);
            }
        } catch (Exception e) {
            AlertShower.showInfo(stage,
                    "",
                    strings.getString("repairFailed"));
            recalculateUiRestrictions();
        }
    }

    /**
     * @return 正在击球的球员正在使用的杆
     */
    private Cue getCuingCue() {
        InGamePlayer cuing = getActiveHolder().getCuingIgp();
        if (game != null) {
            if (game.gameValues.rule.poolLike() && !cuing.isHuman()) {
                if (game.getGame().isBreaking()) {
                    cuing.getCueSelection().selectByBrand(DataLoader.getInstance().getStdBreakCueBrand());
                } else {
                    FastGameView.selectSuggestedCue(cuing.getCueSelection(),
                            game.gameValues.rule,
                            cuing.getPlayerPerson());
                }
            }
        }
        if (cuing == null) {
            if (replay != null) {
                // 回放的第一杆，这肯定是前期渲染，所以说不重要
            } else {
                EventLogger.warning("Cuing cue is null");
            }
            return Cue.getPlaceHolderCue();
        }
        return cuing.getCueSelection().getSelected().getNonNullInstance();
    }

    private CuePlayParams applyRandomCueError(Player player) {
        Random random = new Random();
        return applyCueError(player,
                random.nextGaussian(), random.nextGaussian(), random.nextGaussian(), true,
                currentHand);
    }

    /**
     * 三个factor都是指几倍标准差
     */
    private CuePlayParams applyCueError(Player player,
                                        double powerFactor,
                                        double frontBackSpinFactor,
                                        double sideSpinFactor,
                                        boolean mutate,
                                        CuePlayerHand cuePlayerHand) {
        Cue cue = getCuingCue();
        double ballDia = gameValues.ball.ballDiameter;
        // 越大的球，打点的比例偏差越小
        double hitPointBaseScale = BallMetrics.SNOOKER_BALL.ballDiameter / ballDia;
        // 皮头越大的杆，打点的偏差越大
        hitPointBaseScale *= cue.getCueTipWidth() / 10.0;  // 把10作为标准

        frontBackSpinFactor *= hitPointBaseScale;
        sideSpinFactor *= hitPointBaseScale;

        PlayerPerson playerPerson = player.getPlayerPerson();

        // 用架杆影响打点精确度
//        double handSdMul = PlayerPerson.HandBody.getSdOfHand(handSkill);
        double handSdMul = 1.0;
        frontBackSpinFactor *= handSdMul;
        sideSpinFactor *= handSdMul;
        // power的包含在下面了

        CueParams desiredParams = CueParams.createBySelected(
                getSelectedPower(),
                getSelectedFrontBackSpin(),
                getSelectedSideSpin(),
                cueAngleDeg,
                game.getGame(),
                player.getInGamePlayer(),
                cuePlayerHand
        );
//        System.out.println("FB spin: " + desiredParams.selectedFrontBackSpin());

        double maxSelPower = cuePlayerHand.getMaxPowerPercentage();

        final double origSelPower = desiredParams.selectedPower();
        double selPower = origSelPower;
//        double power = desiredParams.actualPower();

        // 因为力量控制导致的力量偏差
        powerFactor = powerFactor * cuePlayerHand.getPowerSd(selPower);  // 用力越大误差越大
        powerFactor *= cue.getPowerMultiplier();  // 发力范围越大的杆控力越粗糙
        if (enablePsy) {
            double psyPowerMul = getPsyControlMultiplier(playerPerson);
            powerFactor /= psyPowerMul;
        }
        double powerMul = 1 + powerFactor;
        double maxDev = 1.5;
        if (powerMul > maxDev) {
            powerMul = maxDev;
        } else if (powerMul < 1 / maxDev) {
            powerMul = 1 / maxDev;
        }

//        power *= powerMul;
        selPower *= powerMul;

        if (selPower > maxSelPower) {
            selPower = maxSelPower;  // 控不了力也不可能打出怪力吧
        }
        if (mutate)
            System.out.println("Want power: " + origSelPower + ", actual power: " + selPower);

//        if (mutate) {
        intentCuePointX = cuePointX;
        intentCuePointY = cuePointY;
        // 因为出杆质量而导致的打点偏移
//        }

        // todo: 高低杆偏差稍微小点，斯登大点

        double cpx = cuePointX;
        double cpy = cuePointY;

        int counter = 0;
        while (counter < 10) {
            double xError = sideSpinFactor;
            double yError = frontBackSpinFactor;
            double[] muSigXy = cuePlayerHand.getCuePointMuSigmaXY();
            double xSig = muSigXy[1];
            double ySig = -muSigXy[3];

            double mulWithPower = cuePlayerHand.getErrorMultiplierOfPower(origSelPower);

            xError = xError * xSig + muSigXy[0];
            yError = yError * ySig + muSigXy[2];
            xError = xError * mulWithPower * cueAreaRadius / 160;  // xError可以大点，因为球员数据里给得太保守了
            yError = yError * mulWithPower * cueAreaRadius / 240;
            cpx = intentCuePointX + xError;
            cpy = intentCuePointY + yError;

            if (obstacleProjection == null || obstacleProjection.cueAble(
                    getCuePointRelX(cpx), getCuePointRelY(cpy),
                    getRatioOfCueAndBall())) {
                if (mutate) {
                    cuePointX = cpx;
                    cuePointY = cpy;
                }
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
            System.out.print("intent: " + intentCuePointX + ", " + intentCuePointY + "; ");
            System.out.println("actual: " + cuePointX + ", " + cuePointY);
        }

//        System.out.println(cpx + " " + cuePointX);
//        double unitSideSpin = getUnitSideSpin(cpx);

        boolean slidedCue = false;
        if (mutate) {
            if (isMiscue()) {
//                power /= 4;
//                unitSideSpin *= 10;
                System.out.println("Miscued!");
                AchManager.getInstance().addAchievement(Achievement.MISCUED, game.getGame().getCuingIgp());
                slidedCue = true;
            }
            miscued = slidedCue;
        }

//        double[] unitXYWithSpin = getUnitXYWithSpins(unitSideSpin, power);

        return generateCueParams(selPower,
                getSelectedFrontBackSpin(cpy),
                getSelectedSideSpin(cpx),
                cueAngleDeg,
                slidedCue);
    }

    private boolean isMiscue() {
//        return Algebra.distanceToPoint(cuePointX, cuePointY, cueCanvasWH / 2, cueCanvasWH / 2)
//                > cueAreaRadius - cueRadius;
        if (cueAbleArea == null) {
            EventLogger.error("Cue able area is null");
            return false;
        } else {
            return !PointInPoly.pointInPoly(
                    new double[]{getCuePointRelX(cuePointX), getCuePointRelY(cuePointY)},
                    cueAbleArea);
        }
    }

    private CueRecord makeCueRecord(Player cuePlayer, CuePlayParams paramsWithError) {
        return new CueRecord(cuePlayer.getInGamePlayer(),
                game.getGame().isBreaking(),
                paramsWithError.cueParams.selectedPower(),
                paramsWithError.cueParams.actualPower(),
                cursorDirectionUnitX,
                cursorDirectionUnitY,
                getCuePointRelY(intentCuePointY),
                getCuePointRelX(intentCuePointX),
                getCuePointRelY(cuePointY),
                getCuePointRelX(cuePointX),
                cueAngleDeg,
                gamePlayStage(),
                currentHand.toCueHand());
    }

    private TargetRecord makeTargetRecord(Player willCuePlayer) {
        return new TargetRecord(willCuePlayer.getInGamePlayer().getPlayerNumber(),
                game.getGame().getCurrentTarget(),
                game.getGame().isDoingSnookerFreeBll());
    }

    private void disableUiWhenCuing() {
        for (Node control : disableWhenCuing) {
            control.setDisable(true);
        }
    }

    private void enableDisabledUi() {
        for (Node control : disableWhenCuing) {
            control.setDisable(false);
        }
    }

    private void playerCue(Player player) {
        updateBeforeCue();
        stopCueTimer();
        if (game.gameValues.rule.snookerLike()) {
            AbstractSnookerGame asg = (AbstractSnookerGame) game.getGame();
            System.out.println(asg.getCurrentTarget() + " " + predictedTargetBall);
            if (asg.getCurrentTarget() == 0) {
                // 判断斯诺克打彩球时的实际目标球
                GridPane container = new GridPane();
                container.setHgap(10.0);
                container.setVgap(10.0);
                ToggleGroup toggleGroup = new ToggleGroup();
                Map<RadioButton, Integer> buttonVal = new HashMap<>();

                for (int i = 2; i <= 7; i++) {
                    RadioButton rb = new RadioButton(AbstractSnookerGame.ballValueToColorName(i, strings));
                    toggleGroup.getToggles().add(rb);

                    // 是弹出框，只运行一次，不用担心
                    Pane tarPane = new Pane();
                    Canvas tarCan = new Canvas();
                    tarPane.getChildren().add(tarCan);
                    tarCan.setHeight(ballDiameter * 1.2);
                    tarCan.setWidth(ballDiameter * 1.2);
                    tarCan.getGraphicsContext2D().setFill(GLOBAL_BACKGROUND);
                    tarCan.getGraphicsContext2D().fillRect(0, 0, tarCan.getWidth(), tarCan.getHeight());
                    drawSnookerTargetBall(tarCan, i, asg.getIndicatedTarget(), false,
                            player.getInGamePlayer().getPlayerNumber() == 1);

                    container.add(tarPane, 0, i - 2);
                    container.add(rb, 1, i - 2);

                    buttonVal.put(rb, i);

                    if (i == 7) toggleGroup.selectToggle(rb);
                }

                if (predictedTargetBall == null) {
                    AlertShower.askConfirmation(
                            stage,
                            "",
                            strings.getString("askSnookerTarget"),
                            strings.getString("confirm"),
                            strings.getString("cancel"),
                            () -> {
                                RadioButton selected = (RadioButton) toggleGroup.getSelectedToggle();
                                asg.setIndicatedTarget(buttonVal.get(selected), true);
                                aimingChanged();
                                playerCueEssential(player);
                                tableGraphicsChanged = true;
                            },
                            null,
                            container
                    );
                    return;
                } else {
                    asg.setIndicatedTarget(predictedTargetBall.getValue(), false);
                    aimingChanged();
                }
            }
        }
        playerCueEssential(player);
    }

    private void playerCueEssential(Player player) {
        disableUiWhenCuing();

        // 判断是否为进攻杆
        CuePlayerHand usedHand = currentHand;
        PotAttempt currentAttempt = null;
        boolean snookered = game.getGame().isSnookered();

        CuePlayParams params = applyRandomCueError(player);
        if (careerMatch != null) {
            reduceCueHp(getCuingCue(), player, params);
        }

        if (!snookered && predictedTargetBall != null) {
            List<double[][]> holeDirectionsAndHoles =
                    game.getGame().directionsToAccessibleHoles(predictedTargetBall);
            for (double[][] directionHole : holeDirectionsAndHoles) {
                double pottingDirection = Algebra.thetaOf(directionHole[0]);
                double aimingDirection =
                        Algebra.thetaOf(targetPredictionUnitX, targetPredictionUnitY);

                double angleBtw = Math.abs(pottingDirection - aimingDirection);

                if (angleBtw <= Game.MAX_ATTACK_DECISION_ANGLE) {
                    AttackChoice ac = AttackChoice.DirectAttackChoice.createChoice(
                            game.getGame(),
                            game.predictPhy,
                            game.getGame().getCuingPlayer(),
                            new double[]{game.getGame().getCueBall().getX(),
                                    game.getGame().getCueBall().getY()},
                            predictedTargetBall,
                            null,
                            game.getGame().getCurrentTarget(),
                            false,
                            directionHole,
                            new double[]{predictedTargetBall.getX(), predictedTargetBall.getY()}
                    );
                    currentAttempt = new PotAttempt(
                            CueType.ATTACK,
                            gameValues,
                            params,
                            game.getGame().getCuingPlayer().getPlayerPerson(),
                            predictedTargetBall,
                            ac
                    );
                    System.out.printf("Angle is %f, attacking!\n",
                            Math.toDegrees(Math.abs(pottingDirection - aimingDirection)));
                    break;
                }
            }
        }

        double whiteStartingX = game.getGame().getCueBall().getX();
        double whiteStartingY = game.getGame().getCueBall().getY();

        // 先开始放动画
        beginCueAnimationOfHumanPlayer(whiteStartingX, whiteStartingY);

        final var attempt = currentAttempt;
        Thread thread = new Thread(() ->
                playerCueCalculations(params, player, attempt, usedHand, snookered));
        thread.start();
    }

    private void reduceCueHp(Cue cue, Player player, CuePlayParams params) {
        if (!cue.isPermanent()) return;

        double reduce = Math.pow(params.cueParams.actualPower() / 100, 1.5) * 8;
        double cuePointReduce = Math.hypot(params.cueParams.actualFrontBackSpin(),
                params.cueParams.actualSideSpin() * 0.5);

        reduce *= Algebra.shiftRangeSafe(0,
                1,
                0.5,
                1.0,
                cuePointReduce);
        reduce *= gameValues.ball.ballWeightRatio;

        if (miscued) {
            double factor = Math.random();
            factor += 0.5;
            factor *= 400;  // 呲杆时力度和旋转分别都被除以了4。乘以上面shift就是800。但是800太夸张，就减半了
            reduce *= factor;
        }

        showTipBrokenMsg = cue.getCueTip().reduceHp(reduce);
        if (showTipBrokenMsg) {
            System.out.println("Tip broken!");
        }
    }

    /**
     * 会在updateScore那些之前发生
     */
    private void playerCueCalculations(CuePlayParams params,
                                       Player player,
                                       PotAttempt currentAttempt,
                                       CuePlayerHand usedHand,
                                       boolean snookered) {
        Movement calculatedMovement = game.getGame().cue(params, game.playPhy);
        CueRecord cueRecord = makeCueRecord(player, params);  // 必须在randomCueError之后
        TargetRecord thisTarget = makeTargetRecord(player);

        try {
            game.getGame().getRecorder().recordCue(cueRecord, thisTarget);
            game.getGame().getRecorder().recordMovement(calculatedMovement);
        } catch (RecordingException re) {
            EventLogger.error(re);
            game.getGame().abortRecording();
        }

        if (currentAttempt != null) {
            boolean success = currentAttempt.getTargetBall().isPotted() && !game.getGame().isThisCueFoul();
            if (curDefAttempt != null && curDefAttempt.defensePlayer != player) {
                // 大力开球之后下一杆对手打进了不归这里管
                if (!(curDefAttempt.getAttemptBase().type == CueType.BREAK &&
                        game.getGame() instanceof NeedBigBreak)) {
                    // 如进攻成功，则上一杆防守失败了
                    curDefAttempt.setSuccess(!success);
                    if (success) {
                        System.out.println(curDefAttempt.defensePlayer.getPlayerPerson().getName() +
                                " defense failed!");
                    }
                    game.getGame().recordAttemptForAchievement(curDefAttempt, player);
                }
            }
            if (lastPotAttempt != null && lastPotAttempt.getPlayerPerson() == player.getPlayerPerson()) {
                // 如上一杆也是进攻，则这一杆进不进就是上一杆走位成不成功
                lastPotAttempt.setPositionSuccess(success);
                currentAttempt.setPositionToThis(lastPotAttempt);
            }
            currentAttempt.setAfterFinish(
                    usedHand,
                    calculatedMovement);
            currentAttempt.setSuccess(success);
            player.addAttempt(currentAttempt);

            if (success) {
                System.out.println("Pot success!");
                if (miscued) {
                    AchManager.getInstance().addAchievement(Achievement.MISCUE_POT, player.getInGamePlayer());
                }
            } else {
                System.out.println("Pot failed!");
            }
            lastPotAttempt = currentAttempt;
            curDefAttempt = null;
            game.getGame().recordAttemptForAchievement(lastPotAttempt, player);
        } else {
//            if (curDefAttempt != null && curDefAttempt.getAttemptBase().type == CueType.BREAK &&
//                    curDefAttempt.defensePlayer == player) {
//                // 上一杆的开了球还接着打，应该是开球成功了吧？
//                curDefAttempt.setSuccess(true);
//            }

            // 防守
            if (lastPotAttempt != null && lastPotAttempt.getPlayerPerson() == player.getPlayerPerson()) {
                // 如上一杆是本人进攻，则走位失败
                lastPotAttempt.setPositionSuccess(false);
            }

            CueType type;
            if (game.getGame().isBreaking()) {
                type = CueType.BREAK;
            } else if (snookered) {
                type = CueType.SOLVE;
            } else {
                type = CueType.DEFENSE;
            }
            curDefAttempt = new DefenseAttempt(type, player, params);
            curDefAttempt.setAfterFinish(
                    usedHand,
                    calculatedMovement);

            player.addAttempt(curDefAttempt);
            System.out.println("Defense!" + (snookered ? " Solving" : ""));
            lastPotAttempt = null;
            game.getGame().recordAttemptForAchievement(null, player);
        }

        // 放到这里来更新是为了避免上面这一堆运算的时间导致潜在bug
        // 说白了，为了线程安全
        movement = calculatedMovement;
        tracedMovement = movement;
        movementPlayingIndex = 0;
        lastMovementPlayingIndex = 0;
        movementPercentageInIndex = 0.0;
    }

    private void aiCueCalculations(CuePlayParams realParams,
                                   Player player,
                                   AiCueResult cueResult) {
        Movement calculatedMovement = game.getGame().cue(realParams, game.playPhy);

        CueRecord cueRecord = makeCueRecord(player, realParams);  // 必须在randomCueError之后
        TargetRecord thisTarget = makeTargetRecord(player);
        try {
            game.getGame().getRecorder().recordCue(cueRecord, thisTarget);
            game.getGame().getRecorder().recordMovement(calculatedMovement);
        } catch (RecordingException re) {
            EventLogger.error(re);
            game.getGame().abortRecording();
        }

        if (cueResult.isAttack()) {
            FinalChoice.IntegratedAttackChoice iac = (FinalChoice.IntegratedAttackChoice) cueResult.getChoice();
            AttackChoice ac = iac.getAttackParams().getAttackChoice();
            PotAttempt currentAttempt = new PotAttempt(
                    cueResult.getCueType(),
                    gameValues,
                    realParams,
                    game.getGame().getCuingPlayer().getPlayerPerson(),
                    cueResult.getTargetBall(),
                    ac
            );
            boolean success = currentAttempt.getTargetBall().isPotted();
//                     && !game.getGame().isLastCueFoul() todo: 想办法
            if (curDefAttempt != null && curDefAttempt.defensePlayer != player) {
                // 如进攻成功，则上一杆防守失败了
                // 大力开球之后下一杆对手打进了不归这里管
                if (!(curDefAttempt.getAttemptBase().type == CueType.BREAK &&
                        game.getGame() instanceof NeedBigBreak)) {
                    curDefAttempt.setSuccess(!success);
                    if (success) {
                        System.out.println(curDefAttempt.defensePlayer.getPlayerPerson().getName() +
                                " player defense failed!");
                    }
                }
            }
            if (lastPotAttempt != null && lastPotAttempt.getPlayerPerson() == player.getPlayerPerson()) {
                // 如上一杆也是进攻，则这一杆进不进就是上一杆走位成不成功
                lastPotAttempt.setPositionSuccess(success);
                currentAttempt.setPositionToThis(lastPotAttempt);
            }
            currentAttempt.setAfterFinish(
                    cueResult.getCuePlayerHand(),
                    calculatedMovement);
            currentAttempt.setSuccess(success);
            player.addAttempt(currentAttempt);
            if (success) {
                System.out.println("AI Pot success!");
            } else {
                System.out.println("AI Pot failed!");
            }
            lastPotAttempt = currentAttempt;
            curDefAttempt = null;
        } else {
            curDefAttempt = new DefenseAttempt(cueResult.getCueType(),
                    player,
                    realParams);
            curDefAttempt.setAfterFinish(
                    cueResult.getCuePlayerHand(),
                    calculatedMovement);

            player.addAttempt(curDefAttempt);
            System.out.println("AI Defense!" + (curDefAttempt.isSolvingSnooker() ? " Solving" : ""));

            if (lastPotAttempt != null && lastPotAttempt.getPlayerPerson() == player.getPlayerPerson()) {
                // 如上一杆是本人进攻，则走位失败
                lastPotAttempt.setPositionSuccess(false);
            }
            lastPotAttempt = null;
        }

        movement = calculatedMovement;
        tracedMovement = movement;
        movementPlayingIndex = 0;
        lastMovementPlayingIndex = 0;
        movementPercentageInIndex = 0.0;
    }

    private void updateBeforeCue() {
        Toggle sel1 = player1SpeedToggle.getSelectedToggle();
        if (sel1 != null) {
            double newSpeed = Double.parseDouble(sel1.getUserData().toString());

            if (newSpeed != p1PlaySpeed) {
                p1PlaySpeed = newSpeed;
                replayGap = (long) (DEFAULT_REPLAY_GAP / p1PlaySpeed);
                System.out.println("New speed 1 " + p1PlaySpeed);
            }
        }
        Toggle sel2 = player2SpeedToggle.getSelectedToggle();
        if (sel2 != null) {
            double newSpeed = Double.parseDouble(sel2.getUserData().toString());
            if (newSpeed != p2PlaySpeed) {
                p2PlaySpeed = newSpeed;
                replayGap = (long) (DEFAULT_REPLAY_GAP / p2PlaySpeed);
                System.out.println("New speed 2 " + p2PlaySpeed);
            }
        }

        tracedMovement = null;
        tableGraphicsChanged = true;
    }

    private void aiCue(Player player) {
        aiCue(player, true);
    }

    private void aiCue(Player player, boolean aiHasRightToReposition) {
        boolean aiHelpPlayerPlaying = player.getInGamePlayer().isHuman() && aiHelpPlay;
        if (aiHelpPlayerPlaying) {
            if (careerMatch != null) {
                double playerGoodness = CareerManager.getInstance().getPlayerGoodness();

                AiCueResult.setAiPrecisionFactor(playerGoodness *
                        Career.AI_HELPER_PRECISION_FACTOR);  // 暗削自动击球
            } else {
                AiCueResult.setAiPrecisionFactor(ConfigLoader.getInstance().getDouble("fastGameAiming", 1.0));
            }
        } else {
            if (careerMatch != null) {
                AiCueResult.setAiPrecisionFactor(CareerManager.getInstance().getAiGoodness());
            } else {
                AiCueResult.setAiPrecisionFactor(ConfigLoader.getInstance().getDouble("fastGameAiStrength", 1.0));
            }
        }

        updateBeforeCue();
        disableUiWhenCuing();
        Ball.disableGearOffset();  // AI真不会这个，禁用了。在finishCueNextStep里重新启用
        cueButton.setText(strings.getString("aiThinking"));
//        cueButton.setDisable(true);
        aiCalculating = true;
        Thread aiCalculation = new Thread(() -> {
            System.out.println("ai cue");
            long st = System.currentTimeMillis();
            if (game.getGame().isBallInHand()) {
                System.out.println("AI is trying to place ball");
                long placeSt = System.currentTimeMillis();
                AiCueBallPlacer<?, ?> cbp = AiCueBallPlacer.createAiCueBallPlacer(game.getGame(), player);
                double[] pos = cbp.getPositionToPlaceCueBall();
                if (pos == null) {
                    // Ai不知道摆哪了，认输
                    aiCalculating = false;
                    withdraw(player);
                    return;
                }
                long placeEnd = System.currentTimeMillis();
                System.out.println("AI placed cue ball at " + Arrays.toString(pos) + ", time: " + (placeEnd - placeSt));

                game.getGame().placeWhiteBall(pos[0], pos[1]);
                game.getGame().getRecorder().writeBallInHandPlacement();
                if (cbp.getBallSpecified() != null) {
                    game.getGame().setSpecifiedTarget(cbp.getBallSpecified());
                } else {
                    game.getGame().setSpecifiedTarget(null);
                }
                aiCalculating = false;
                Platform.runLater(() -> {
                    cueButton.setText(strings.getString("aiCueText"));
                    finishCueNextStep(player);
                });
                return;
            }
            if (gameValues.rule.snookerLike()) {
                AbstractSnookerGame asg = (AbstractSnookerGame) game.getGame();
                if (aiHasRightToReposition && asg.canReposition()) {
                    if (asg.aiConsiderReposition(game.predictPhy, lastPotAttempt)) {
                        Platform.runLater(() -> {
                            AlertShower.showInfo(
                                    stage,
                                    strings.getString("aiAskReposition"),
                                    strings.getString("reposition"),
                                    3000
                            );
                            cueButton.setText(strings.getString("cueText"));
                            aiCalculating = false;
                            asg.reposition();
                            drawScoreBoard(game.getGame().getCuingPlayer(), true);
                            drawTargetBoard(true);
                            draw();
                            endCueAnimation();
                            finishCueNextStep(game.getGame().getCuingPlayer());

                            if (asg.isNoHitThreeWarning()) {
                                showThreeNoHitWarning();
                            }
                        });
                        return;
                    } else {
                        asg.notReposition();
                    }
                }
            }

            AiCueResult cueResult0 = null;
            try {
                cueResult0 = game.getGame().aiCue(player, game.predictPhy);
            } catch (Exception e) {
                EventLogger.error(e);
            }
            final AiCueResult cueResult = cueResult0;
            System.out.println("Ai calculation ends in " + (System.currentTimeMillis() - st) + " ms");
            stopCueTimer();
//            System.out.println(cueResult);
            if (cueResult == null) {
                aiCalculating = false;
                withdraw(player);
                return;
            }
            aiWhitePath = cueResult.getWhitePath();  // todo
            tableGraphicsChanged = true;
            if (game.gameValues.rule.snookerLike()) {
                AbstractSnookerGame asg = (AbstractSnookerGame) game.getGame();
                if (cueResult.getTargetBall() != null) {
                    asg.setIndicatedTarget(cueResult.getTargetBall().getValue(), true);
                } else {
                    // ai在乱打
                    System.out.println("AI angry cues");
                    asg.setIndicatedTarget(2, true);
                }
            }

            Platform.runLater(() -> {
                cueButton.setText(strings.getString("isCuing"));
                cursorDirectionUnitX = cueResult.getUnitX();
                cursorDirectionUnitY = cueResult.getUnitY();
                System.out.printf("Ai direction: %f, %f\n", cursorDirectionUnitX, cursorDirectionUnitY);
                currentHand = cueResult.getCuePlayerHand();
                updateHandSelectionToggleByData(currentHand.playerHand.hand);
                updatePowerSlider(player.getInGamePlayer(), cueResult.getCuePlayerHand());
                powerSlider.setValue(cueResult.getCueParams().selectedPower());
                cuePointX = cueCanvasWH / 2 + cueResult.getCueParams().selectedSideSpin() * cueAreaRadius;
                cuePointY = cueCanvasWH / 2 - cueResult.getCueParams().selectedFrontBackSpin() * cueAreaRadius;
                cueAngleDeg = cueResult.getCueParams().getCueAngleDeg();
                setCueAngleLabel();

                recalculateObstacles();
                aimingChanged();

                CuePlayParams realParams = applyRandomCueError(player);
                if (aiHelpPlayerPlaying && careerMatch != null) {
                    reduceCueHp(getCuingCue(), player, realParams);
                }

                double whiteStartingX = game.getGame().getCueBall().getX();
                double whiteStartingY = game.getGame().getCueBall().getY();

                aiCalculating = false;

                beginCueAnimation(game.getGame().getCuingPlayer().getInGamePlayer(),
                        whiteStartingX, whiteStartingY, cueResult.getCueParams().selectedPower(),
                        cueResult.getUnitX(), cueResult.getUnitY());

                Thread thread = new Thread(() -> aiCueCalculations(
                        realParams,
                        player,
                        cueResult
                ));
                thread.start();
            });
        });
        aiCalculation.setDaemon(true);
        aiCalculation.start();
    }

    private void replayCue() {
        updateBeforeCue();
        if (replay.getCurrentFlag() == ActualRecorder.FLAG_CUE) {
            CueRecord cueRecord = replay.getCueRecord();
            if (cueRecord == null) return;

            replayNextCueButton.setText(strings.getString("replayFastForward"));
            replayNextCueButton.setOnAction(this::replayFastForwardAction);
            replayLastCueButton.setDisable(true);

            movement = replay.getMovement();
            tracedMovement = movement;
            movementPlayingIndex = 0;
            lastMovementPlayingIndex = 0;
            movementPercentageInIndex = 0.0;
            System.out.println(movement.getMovementMap().get(replay.getCueBall()).size());

            cursorDirectionUnitX = cueRecord.aimUnitX;
            cursorDirectionUnitY = cueRecord.aimUnitY;

            intentCuePointX = getCuePointCanvasX(cueRecord.intendedHorPoint);
            intentCuePointY = getCuePointCanvasY(cueRecord.intendedVerPoint);
            cuePointX = getCuePointCanvasX(cueRecord.actualHorPoint);
            cuePointY = getCuePointCanvasY(cueRecord.actualVerPoint);
            cueAngleDeg = cueRecord.cueAngle;
            currentHand = cueRecord.cuePlayer.getPlayerPerson().handBody.getHandSkillByHand(cueRecord.cueHand);

            miscued = isMiscue();

            Platform.runLater(() -> {
                updatePowerSlider(cueRecord.cuePlayer, currentHand);
                powerSlider.setValue(cueRecord.selectedPower);
                aimingChanged();
            });

            Ball cueBall = replay.getCueBall();
            MovementFrame cueBallPos = movement.getStartingPositions().get(cueBall);
            beginCueAnimation(cueRecord.cuePlayer, cueBallPos.x, cueBallPos.y,
                    cueRecord.selectedPower, cueRecord.aimUnitX, cueRecord.aimUnitY);
        } else if (replay.getCurrentFlag() == ActualRecorder.FLAG_HANDBALL) {
            System.out.println("Ball in hand!");
//            drawScoreBoard(null);
            drawTargetBoard(true);
            restoreCuePoint();
            restoreCueAngle();
            updateScoreDiffLabels();
        }
    }

    private void showThreeNoHitWarning() {
        AlertShower.showInfo(
                stage,
                strings.getString("snookerThreeWarning"),
                strings.getString("warning")
        );
    }

    private CuePlayParams generateCueParams() {
        return generateCueParams(getSelectedPower());
    }

    private CuePlayParams[] generateCueParamsSd1(int nPoints) {
        Player player = game.getGame().getCuingPlayer();
        double sd = 1;
        double corner = Math.sqrt(2) / 2 * sd;
        CuePlayParams[] res = new CuePlayParams[nPoints + 1];
        res[0] = generateCueParams();
        if (nPoints == 0) return res;

        if (nPoints == 8) {
            res[1] = applyCueError(player, -sd, sd, 0, false, currentHand);  // 又小又低
            res[2] = applyCueError(player, -corner, corner, -corner, false, currentHand);  // 偏小，左下
            res[3] = applyCueError(player, 0, 0, -sd, false, currentHand);  // 最左
            res[4] = applyCueError(player, corner, -corner, -corner, false, currentHand);  // 偏大，左上
            res[5] = applyCueError(player, sd, -sd, 0, false, currentHand);  // 又大又高
            res[6] = applyCueError(player, corner, -corner, corner, false, currentHand);  // 偏大，右上
            res[7] = applyCueError(player, 0, 0, sd, false, currentHand);  // 最右
            res[8] = applyCueError(player, -corner, corner, corner, false, currentHand);  // 偏小，右下
        } else if (nPoints == 4) {
            // 这里就没用corner了，因为我们宁愿画大点去吓玩家
            res[1] = applyCueError(player, -sd, sd, -sd, false, currentHand);  // 小，左下
            res[2] = applyCueError(player, sd, -sd, -sd, false, currentHand);  // 大，左上
            res[3] = applyCueError(player, sd, -sd, sd, false, currentHand);  // 大，右上
            res[4] = applyCueError(player, -sd, sd, sd, false, currentHand);  // 小，右下
        } else if (nPoints == 2) {
            // 这里就没用corner了，因为我们宁愿画大点去吓玩家
            res[1] = applyCueError(player, sd, -sd, -sd, false, currentHand);  // 大，左上
            res[2] = applyCueError(player, -sd, sd, sd, false, currentHand);  // 小，右下
        } else {
            throw new RuntimeException(nPoints + " points white prediction not supported");
        }
        return res;
    }

    private CuePlayParams generateCueParams(double selectedPower) {
        return generateCueParams(selectedPower, getSelectedSideSpin(), cueAngleDeg);
    }

    private CuePlayParams generateCueParams(double selectedPower, double selectedSideSpin,
                                            double cueAngleDeg) {
        return generateCueParams(selectedPower, getSelectedFrontBackSpin(), selectedSideSpin, cueAngleDeg, false);
    }

    private CuePlayParams generateCueParams(double selectedPower,
                                            double selectedFrontBackSpin,
                                            double selectedSideSpin,
                                            double cueAngleDeg,
                                            boolean slideCue) {
        CueParams cueParams = CueParams.createBySelected(
                selectedPower,
                selectedFrontBackSpin,
                selectedSideSpin,
                cueAngleDeg,
                game.getGame(),
                game.getGame().getCuingIgp(),
                currentHand
        );
        return CuePlayParams.makeIdealParams(
                cursorDirectionUnitX,
                cursorDirectionUnitY,
                cueParams,
                slideCue);
    }

    private void setButtonsCueStart() {
        withdrawMenu.setDisable(true);
        cueButton.setDisable(true);
    }

    private void setButtonsCueEnd(Player nextCuePlayer) {
        cueButton.setDisable(false);

        if (nextCuePlayer.getInGamePlayer().getPlayerType() == PlayerType.PLAYER) {
            cueButton.setText(strings.getString("cueText"));
            withdrawMenu.setDisable(false);
        } else {
            cueButton.setText(strings.getString("aiCueText"));
            withdrawMenu.setDisable(true);
        }
    }

    /**
     * @return 力量槽选的力量
     */
    private double getSelectedPower() {
        return Math.max(powerSlider.getValue(), 0.01);
    }

    private double getSelectedSideSpin() {
        return getSelectedSideSpin(cuePointX);
    }

    private double getSelectedFrontBackSpin() {
        return getSelectedFrontBackSpin(cuePointY);
    }

    private double getSelectedSideSpin(double cpx) {
        return (cpx - cueCanvasWH / 2) / cueAreaRadius;
    }

    private double getSelectedFrontBackSpin(double cpy) {
        return (cueCanvasWH / 2 - cpy) / cueAreaRadius;
    }

    private void restoreCuePoint() {
        cuePointX = cueCanvasWH / 2;
        cuePointY = cueCanvasWH / 2;
        intentCuePointX = -1;
        intentCuePointY = -1;

        if (game != null && game.getGame() != null) recalculateUiRestrictions();
    }

    /**
     * 一定要在Stage.show()之后
     */
    public void startVideoCapture(VideoCapture videoCapture) {
        this.videoCapture = videoCapture;

        this.replayGap = videoCapture.getGapMsBtwCue();

        replayAutoPlayBox.setSelected(true);
        replayButtonBox.setVisible(false);

        if (videoCapture.getVideoParams().area() == VideoConverter.Area.GAME_PANE) {
            Bounds ballPanePos = gamePane.localToScene(gamePane.getBoundsInLocal());
            Bounds contentPanePos = contentPane.localToScene(contentPane.getBoundsInLocal());
            double anchorX = contentPanePos.getMaxX() - gamePane.getWidth();
//                    - contentPanePos.getMinX();
            double anchorY = ballPanePos.getMinY();
//                    - contentPanePos.getMinY();

//            Bounds gamePaneInContent = gamePane.localToParent(gamePane.getBoundsInLocal());
//            double anchorX = gamePaneInContent.getMinX();
//            double anchorY = gamePaneInContent.getMinY();

            Rectangle2D screenshotCrop = new Rectangle2D(
                    (int) anchorX,
                    (int) anchorY,
                    (int) gamePane.getWidth(),
                    (int) gamePane.getHeight());
            videoCapture.setupScreenshotParams(screenshotCrop);
        }

        if (gameLoop != null) {
            throw new RuntimeException("Game loop not null");
        }
        gameLoop = new VideoCaptureLoop(videoCapture, this::videoOneFrame, videoCapture.getFps());
//        gameLoop = new AnimationGameLoop(this::videoOneFrame, fpsLabel);
        gameLoop.start();
    }

    public void startAnimation() {
//        frameAnimation = new AnimationFrame(this::oneFrame, uiFrameTimeMs);
        if (gameLoop != null) {
            throw new RuntimeException("Game loop not null");
        }
        gameLoop = new AnimationGameLoop(this::oneFrame, fpsLabel);
        gameLoop.start();
    }

    private void startCueTimer() {
        if (replay != null) return;
        stopCueTimer();

        cueTimeCounter = 0;
        cueTimer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    timerLabel.setText(Util.timeToReadable(cueTimeCounter * 1000L));
                    cueTimeCounter++;
                });
            }
        };
        cueTimer.schedule(timerTask, 0, 1000);
    }

    private void stopCueTimer() {
        if (replay != null) return;
        if (cueTimer != null) {
            cueTimer.cancel();
            cueTimer = null;
        }
    }

    private void videoOneFrame() {
        oneFrame();
        videoCapture.recordFrame(contentPane);
    }

    private void aimingChanged() {
        if (replay != null) {

        } else {
            if (game.getGame() instanceof AbstractSnookerGame asg) {
                if (predictedTargetBall != null) {
                    asg.setIndicatedTarget(predictedTargetBall.getValue(), false);
                } else {
                    asg.setIndicatedTarget(asg.getCurrentTarget(), false);
                }
                drawTargetBoard(true);
            }
        }
//        drawCueBallCanvas();
        drawCueAngleCanvas();
    }

    private void oneFrame() {
        if (aiCalculating) return;

        draw();
        drawCueBallCanvas();
//        drawCueAngleCanvas();
        drawCue();
    }

    private void setupPowerSlider() {
        powerSlider.setShowTickMarks(false);
        powerSlider.setShowTickLabels(false);
        powerSlider.setSnapToTicks(false);

        powerSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            if (game != null) {
                double maxPower;
                if (currentHand != null) {
                    maxPower = currentHand.getMaxPowerPercentage();
                } else {
                    maxPower = game.getGame().getCuingPlayer().getPlayerPerson().getPrimaryHand().getMaxPowerPercentage();
                }
                InGamePlayer playingPlayer = game.getGame().getCuingIgp();
                maxPower = CuePlayParams.powerWithCueAngle(
                        playingPlayer.getPlayerPerson().handBody,
                        playingPlayer.getCueSelection().getSelected().brand,
                        maxPower,
                        cueAngleDeg
                );
                if (newValue.doubleValue() > maxPower) {
                    powerSlider.setValue(maxPower);
                    return;
                }
                createPathPrediction();
            }
            powerLabel.setText(String.format("%.1f", newValue.doubleValue()));
        }));

        powerSlider.setValue(DEFAULT_POWER);
    }

    private void addListeners() {
        gamePane.getTableCanvas().setOnMouseClicked(this::onCanvasClicked);
        gamePane.getTableCanvas().setOnDragDetected(this::onDragStarted);
        gamePane.getTableCanvas().setOnMouseDragged(this::onDragging);
        gamePane.getTableCanvas().setOnMouseMoved(this::onMouseMoved);

        cuePointCanvas.setOnMousePressed(this::onCuePointCanvasClicked);
        cuePointCanvas.setOnMouseDragged(this::onCuePointCanvasDragged);

        cueAngleCanvas.setOnMouseClicked(this::onCueAngleCanvasClicked);
        cueAngleCanvas.setOnMouseDragged(this::onCueAngleCanvasDragged);
    }

    private void drawBallInHand() {
        if (replay != null) return;
        if (game.getGame().isCalculating() || movement != null) return;

        if (debugMode && debuggingBall != null) {
            drawBallInHandEssential(debuggingBall);
        }

        if (game.getGame().isBallInHand()
                && game.getGame().getCuingPlayer().getInGamePlayer().getPlayerType() == PlayerType.PLAYER) {
            drawBallInHandEssential(game.getGame().getCueBall());
        }
    }

    private void drawBallInHandEssential(Ball ball) {
        gamePane.drawBallInHandEssential(ball, game.getGame().getTable(), mouseX, mouseY);
    }

    private void drawTraceOfBall(Ball ball, List<MovementFrame> frames) {
        if (frames == null) return;
        int end = Math.min(movementPlayingIndex, frames.size());
        if (end > 1) {
            gamePane.getLineGraphics().setStroke(ball.getTraceColor());
            double x = gamePane.canvasX(frames.getFirst().x);
            double y = gamePane.canvasY(frames.getFirst().y);
            for (int i = 1; i < end; i++) {
                MovementFrame frame = frames.get(i);
                if (frame.potted) break;

                double nx = gamePane.canvasX(frame.x);
                double ny = gamePane.canvasY(frame.y);
                gamePane.getLineGraphics().strokeLine(
                        x,
                        y,
                        nx,
                        ny
                );
                x = nx;
                y = ny;
            }
        }
    }

    private void drawTraces() {
        boolean white = traceWhiteItem.isSelected();
        boolean target = traceTargetItem.isSelected();
        boolean all = traceAllItem.isSelected();

        if (tracedMovement != null) {
            if (all) {
                for (Map.Entry<Ball, List<MovementFrame>> entry : tracedMovement.getMovementMap().entrySet()) {
                    drawTraceOfBall(entry.getKey(), entry.getValue());
                }
            } else {
                if (white) {
                    Ball cueBall = getActiveHolder().getCueBall();
                    drawTraceOfBall(cueBall, tracedMovement.getMovementMap().get(cueBall));
                }
                if (target) {
                    Ball targetBall = tracedMovement.getWhiteFirstCollide();
                    if (targetBall != null) {
                        drawTraceOfBall(targetBall, tracedMovement.getMovementMap().get(targetBall));
                    }
                }
            }
        }
    }

    private void drawBalls() {
//        System.out.println(playingMovement + " " + movement);
        if (playingMovement) {
            // 处理倍速
            long msSinceAnimationBegun = gameLoop.msSinceAnimationBegun();

            double appliedSpeed = getCurPlaySpeedMultiplier();
            movementPlayingIndex = (int) (msSinceAnimationBegun * appliedSpeed / frameTimeMs);

            boolean isLast = false;
            int movementSize = movement.getNFrames();
            if (movementPlayingIndex >= movementSize) {
                movementPlayingIndex = movementSize - 1;
                isLast = true;
            }
            if (movement.isCongested()) {
                movementPlayingIndex = movementSize - 1;
                isLast = true;
                Platform.runLater(() -> AlertShower.showInfo(
                        stage,
                        strings.getString("physicalCongestion"),
                        strings.getString("bugged")
                ));
            }

            double uiFrameSinceThisAniFrame = (msSinceAnimationBegun - (movementPlayingIndex * frameTimeMs / appliedSpeed));
            movementPercentageInIndex = uiFrameSinceThisAniFrame / frameTimeMs * appliedSpeed;
            double frameRateRatio = gameLoop.lastAnimationFrameMs() / frameTimeMs * appliedSpeed;
//            System.out.printf("%d %f %f %f\n", index, uiFrameSinceThisAniFrame, rate, frameRateRatio);

            GameHolder holder = getActiveHolder();
            for (Map.Entry<Ball, List<MovementFrame>> entry :
                    movement.getMovementMap().entrySet()) {
                List<MovementFrame> list = entry.getValue();
                MovementFrame frame = list.get(movementPlayingIndex);

                if (!frame.potted) {
                    MovementFrame nextFrame = null;
                    if (movementPlayingIndex + 1 < list.size()) {
                        nextFrame = list.get(movementPlayingIndex + 1);
                    }
                    double x, y;
                    if (nextFrame == null || nextFrame.potted) {
                        x = frame.x;
                        y = frame.y;
                    } else {
                        x = Algebra.rateBetween(frame.x, nextFrame.x, movementPercentageInIndex);
                        y = Algebra.rateBetween(frame.y, nextFrame.y, movementPercentageInIndex);
                    }
                    double frameDeg = frame.frameDegChange * frameRateRatio;

                    entry.getKey().model.sphere.setVisible(true);
                    holder.getTable().forceDrawBall(
                            gamePane,
                            entry.getKey(),
                            x, y,
                            frame.xAxis, frame.yAxis, frame.zAxis,
                            frameDeg);
                } else {
                    entry.getKey().model.sphere.setVisible(false);
                }
//                if (Math.random() < 0.05) throw new RuntimeException();

            }
            // 放音效，只放这一动画帧权重最高的音效
            int mediaType = MovementFrame.NORMAL;
            double mediaValue = 0.0;
            for (int fi = lastMovementPlayingIndex + 1; fi <= movementPlayingIndex; fi++) {
//                System.out.println(fi);
                for (Map.Entry<Ball, List<MovementFrame>> entry :
                        movement.getMovementMap().entrySet()) {
                    List<MovementFrame> list = entry.getValue();
                    MovementFrame frame = list.get(fi);
                    if (!frame.potted) {
                        int old = mediaType;
                        mediaType = MovementFrame.replaceMovementType(mediaType, frame.movementType);
                        if (old != mediaType) {
                            mediaValue = frame.movementValue;
                        }
                    }
                }
            }
            switch (mediaType) {
                case MovementFrame.COLLISION -> {
//                    System.out.println("Collision sound: " + mediaValue);
                    AudioPlayerManager.getInstance().play(
                            SoundInfo.bySpeed(SoundInfo.SoundType.BALL_COLLISION, mediaValue),
                            gameValues,
                            null,
                            videoCapture == null ? null : videoCapture.getSoundRecorder(),
                            (int) gameLoop.currentTimeMillis()
                    );
                }
                case MovementFrame.POCKET_BACK -> {
//                    System.out.println("Pocket back sound: " + mediaValue);
                    AudioPlayerManager.getInstance().play(
                            SoundInfo.bySpeed(SoundInfo.SoundType.POCKET_BACK, mediaValue),
                            gameValues,
                            null,
                            videoCapture == null ? null : videoCapture.getSoundRecorder(),
                            (int) gameLoop.currentTimeMillis()
                    );
                }
                case MovementFrame.EDGE_CUSHION,
                     MovementFrame.CUSHION_LINE,
                     MovementFrame.CUSHION_ARC -> {
//                    System.out.println("Cushion sound: " + mediaValue);
                    AudioPlayerManager.getInstance().play(
                            SoundInfo.bySpeed(SoundInfo.SoundType.CUSHION, mediaValue),
                            gameValues,
                            null,
                            videoCapture == null ? null : videoCapture.getSoundRecorder(),
                            (int) gameLoop.currentTimeMillis()
                    );
                }
            }
            lastMovementPlayingIndex = movementPlayingIndex;

            if (isLast) {
                playingMovement = false;
                movement = null;
                if (replay != null) finishCueReplay();
                else {
                    game.getGame().finishMove(this);  // 这本不应该在UI线程，但是懒得改
                }
            }
        } else {
            if (movement == null) {
//                if (tableGraphicsChanged) {  // 有些bug比较难修，算了
                if (replay != null) {
                    gamePane.drawStoppedBalls(replay.getTable(), replay.getAllBalls(), replay.getCurrentPositions());
                } else {
                    if (cueAnimationPlayer != null) {
                        // 正在进行物理运算，运杆的动画在放
                    } else {
                        gamePane.drawStoppedBalls(game.getGame().getTable(), game.getGame().getAllBalls(), null);
                    }
                }
                if (replay != null && !replay.finished() &&
                        gameLoop.currentTimeMillis() - replayStopTime > replayGap &&
                        replayAutoPlayBox.isSelected()) {
                    System.out.println("replay auto next cue");
                    replayNextCueAction(null);
                }
            } else {
                // 已经算出，但还在放运杆动画
                for (Map.Entry<Ball, MovementFrame> entry : movement.getStartingPositions().entrySet()) {
                    MovementFrame frame = entry.getValue();
                    if (!frame.potted) {
                        entry.getKey().model.sphere.setVisible(true);
                        getActiveHolder().getTable().forceDrawBall(
                                gamePane,
                                entry.getKey(),
                                frame.x, frame.y,
                                frame.xAxis, frame.yAxis, frame.zAxis,
                                frame.frameDegChange * getCurPlaySpeedMultiplier());
                    } else {
                        entry.getKey().model.sphere.setVisible(false);
                    }
                }
            }
        }
    }

    private void drawScoreBoard(Player cuePlayer, boolean showNextCue) {
        // TODO
        if (replay != null) {
            Platform.runLater(() -> {
                player1FramesLabel.setText(String.valueOf(replay.getItem().p1Wins));
                player2FramesLabel.setText(String.valueOf(replay.getItem().p2Wins));

                wipeCanvas(singlePoleCanvas);

                ScoreResult sr = replay.getScoreResult();
                if (sr == null) return;  // 还没开始

                if (gameValues.rule.snookerLike()) {
                    SnookerScoreResult ssr = (SnookerScoreResult) sr;
                    player1ScoreLabel.setText(String.valueOf(ssr.getP1TotalScore()));
                    player2ScoreLabel.setText(String.valueOf(ssr.getP2TotalScore()));
                    drawSnookerSinglePoles(ssr.getSinglePoleMap());
                    singlePoleLabel.setText(String.valueOf(ssr.getSinglePoleScore()));
                } else if (gameValues.rule == GameRule.CHINESE_EIGHT || gameValues.rule == GameRule.LIS_EIGHT) {
                    ChineseEightScoreResult csr = (ChineseEightScoreResult) sr;
//                    List<PoolBall> rems = ChineseEightTable.filterRemainingTargetOfPlayer(replay.getCueRecord().targetRep, replay);
                    List<PoolBall> rems = replay.getCueRecord().cuePlayer.getPlayerNumber() == 1 ?
                            csr.getP1Rems() : csr.getP2Rems();
                    singlePoleLabel.setText(csr.getSinglePoleBallCount() == 0 ?
                            "" :
                            String.valueOf(csr.getSinglePoleBallCount()));
                    drawChineseEightAllTargets(rems);
                } else if (gameValues.rule == GameRule.AMERICAN_NINE) {
                    NineBallScoreResult nsr = (NineBallScoreResult) sr;
                    Map<PoolBall, Boolean> rems = nsr.getRemBalls();
                    singlePoleLabel.setText(nsr.getSinglePoleBallCount() == 0 ?
                            "" :
                            String.valueOf(nsr.getSinglePoleBallCount()));
                    drawPoolBallAllTargets(rems);
                }
            });
        } else {
            Platform.runLater(() -> {
                player1FramesLabel.setText(String.valueOf(game.getP1Wins()));
                player2FramesLabel.setText(String.valueOf(game.getP2Wins()));

                wipeCanvas(singlePoleCanvas);

                if (gameValues.rule.snookerLike()) {
                    AbstractSnookerGame asg = (AbstractSnookerGame) game.getGame();
                    SnookerPlayer snookerPlayer = (SnookerPlayer) cuePlayer;

                    player1ScoreLabel.setText(String.valueOf(asg.getPlayer1().getScore()));
                    player2ScoreLabel.setText(String.valueOf(asg.getPlayer2().getScore()));

                    drawSnookerSinglePoles(snookerPlayer.getSinglePole());

                    int singlePoleScore = snookerPlayer.getSinglePoleScore();
//                    System.out.println("Single pole: " + singlePoleScore);

                    String singlePoleText;
                    if (singlePoleScore < 3) {  // 至少一红一彩再显吧？
                        singlePoleText = String.valueOf(singlePoleScore);
                    } else {
                        int possible = asg.getPossibleBreak(singlePoleScore);
                        if (possible == singlePoleScore) {
                            singlePoleText = String.format(strings.getString("breakScore"),
                                    singlePoleScore);
                        } else {
                            singlePoleText = singlePoleScore +
                                    String.format(" (%s)",
                                            String.format(strings.getString("possibleBreak"),
                                                    asg.getPossibleBreak(singlePoleScore)));
                        }
                    }

                    singlePoleLabel.setText(singlePoleText);
                } else if (gameValues.rule.poolLike()) {
                    String singlePole = "";
                    if (cuePlayer == game.getGame().getCuingPlayer()) {
                        // 进攻成功了
                        drawNumberedAllTargets((NumberedBallGame<?>) game.getGame(),
                                (NumberedBallPlayer) cuePlayer);
                        int sp = cuePlayer.getSinglePoleCount();
                        if (sp > 0) singlePole = String.valueOf(sp);
                    } else {
                        // 进攻失败了
                        drawNumberedAllTargets((NumberedBallGame<?>) game.getGame(),
                                (NumberedBallPlayer) game.getGame().getCuingPlayer());
                    }
                    singlePoleLabel.setText(singlePole);

                    if (gameValues.rule == GameRule.CHINESE_EIGHT) {
                        // 让球
                        ChineseEightBallPlayer p1 = (ChineseEightBallPlayer) game.getGame().getPlayer1();
                        ChineseEightBallPlayer p2 = (ChineseEightBallPlayer) game.getGame().getPlayer2();

                        Map<LetBall, Integer> p1Letted = p1.getLettedBalls();
                        Map<LetBall, Integer> p2Letted = p2.getLettedBalls();
                        int p1Sum = p1Letted.values().stream().reduce(0, Integer::sum);
                        int p2Sum = p2Letted.values().stream().reduce(0, Integer::sum);
                        if (p1Sum > 0) {
                            if (p2Sum > 0) {
                                EventLogger.warning("Both players %s: %s and %s: %s have been let ball. This is not reasonable"
                                        .formatted(p1.getPlayerPerson().getPlayerId(),
                                                p1Letted,
                                                p2.getPlayerPerson().getPlayerId(),
                                                p2Letted));
                            }
                            player1ScoreLabel.setText(getLetBallText(p1Letted));
                        }
                        if (!p2Letted.isEmpty()) {
                            player2ScoreLabel.setText(getLetBallText(p2Letted));
                        }
                    }
                }
            });
        }
    }

    private String getLetBallText(Map<LetBall, Integer> letBalls) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<LetBall, Integer> entry : letBalls.entrySet()) {
            if (entry.getValue() > 0) {
                builder.append(entry.getKey().getShown(entry.getValue(), strings)).append(' ');
            }
        }
        return builder.toString();
    }

    private void drawTargetBoard(boolean showNextTarget) {
        Platform.runLater(() -> {
            if (gameValues.rule.snookerLike())
                drawSnookerTargetBoard(showNextTarget);
            else if (gameValues.rule.poolLike())
                drawPoolTargetBoard(showNextTarget);
        });
    }

    private void drawSnookerTargetBoard(boolean showNextCue) {
        int tar;
        boolean p1;
        boolean snookerFreeBall;
        int indicatedTarget;
//        Ball specifiedTarget = null;
        if (replay != null) {
            CueRecord cueRecord = replay.getCueRecord();
            TargetRecord target = showNextCue ? replay.getNextTarget() : replay.getThisTarget();
            if (cueRecord == null || target == null) return;
            p1 = target.playerNum == 1;
            tar = target.targetRep;
            indicatedTarget = target.targetRep;
            snookerFreeBall = target.isSnookerFreeBall;
            System.out.println("Target: " + tar + ", player: " + target.playerNum);
        } else {
            AbstractSnookerGame game1 = (AbstractSnookerGame) game.getGame();
            p1 = game1.getCuingPlayer().getInGamePlayer().getPlayerNumber() == 1;
            tar = game1.getCurrentTarget();
            snookerFreeBall = game1.isDoingFreeBall();
            indicatedTarget = game1.getIndicatedTarget();
        }
        if (p1) {
            wipeCanvas(player1TarCanvas);
            drawSnookerTargetBall(player1TarCanvas, tar, indicatedTarget, snookerFreeBall, true);
            wipeCanvas(player2TarCanvas);
        } else {
            wipeCanvas(player2TarCanvas);
            drawSnookerTargetBall(player2TarCanvas, tar, indicatedTarget, snookerFreeBall, false);
            wipeCanvas(player1TarCanvas);
        }
    }

    private void drawPoolTargetBoard(boolean showNextCue) {
        int tar;
        boolean p1;
        if (replay != null) {
            CueRecord cueRecord = replay.getCueRecord();
            TargetRecord target = showNextCue ? replay.getNextTarget() : replay.getThisTarget();
            if (cueRecord == null || target == null) return;
            p1 = target.playerNum == 1;
            tar = target.targetRep;
        } else {
            NumberedBallGame<?> game1 = (NumberedBallGame<?>) game.getGame();
            p1 = game1.getCuingPlayer().getInGamePlayer().getPlayerNumber() == 1;
            tar = game1.getCurrentTarget();
        }

        if (p1) {
            drawPoolTargetBall(player1TarCanvas, tar, true);
            wipeCanvas(player2TarCanvas);
        } else {
            drawPoolTargetBall(player2TarCanvas, tar, false);
            wipeCanvas(player1TarCanvas);
        }
    }

    private void drawChineseEightAllTargets(List<PoolBall> targets) {
        double x = ballDiameter * 0.6;
        double y = ballDiameter * 0.6;

        for (PoolBall ball : targets) {
            NumberedBallTable.drawPoolBallEssential(
                    x, y, ballDiameter, ball.getColor(), ball.getValue(),
                    singlePoleCanvas.getGraphicsContext2D());
            x += ballDiameter * 1.2;
        }
    }

    private void drawPoolBallAllTargets(Map<PoolBall, Boolean> balls) {
        double x = ballDiameter * 0.6;
        double y = ballDiameter * 0.6;

        for (Map.Entry<PoolBall, Boolean> ballPot : balls.entrySet()) {
            PoolBall ball = ballPot.getKey();
            boolean greyOut = ballPot.getValue();
            NumberedBallTable.drawPoolBallEssential(
                    x, y, ballDiameter, ball.getColor(), ball.getValue(),
                    singlePoleCanvas.getGraphicsContext2D(),
                    greyOut);
            x += ballDiameter * 1.2;
        }
    }

    private void drawNumberedAllTargets(NumberedBallGame<?> frame, NumberedBallPlayer player) {
        // 别想了，不会每一帧都画一遍，只有
        if (frame instanceof ChineseEightBallGame) {  // 李式八球也instanceof中八
            int ballRange = ((ChineseEightBallPlayer) player).getBallRange();
            List<PoolBall> targets = ChineseEightTable.filterRemainingTargetOfPlayer(
                    ballRange, frame
            );
            drawChineseEightAllTargets(targets);
        } else if (frame instanceof AmericanNineBallGame) {
            Map<PoolBall, Boolean> balls = ((AmericanNineBallGame) frame).getBalls();
            drawPoolBallAllTargets(balls);
        }
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
        Pane pane = (Pane) canvas.getParent();
        pane.getChildren().removeIf(n -> n instanceof Shape || n instanceof Shape3D);

        canvas.getGraphicsContext2D().setFill(GLOBAL_BACKGROUND);
        canvas.getGraphicsContext2D().fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void drawSnookerTargetBall(Canvas canvas,
                                       int value,
                                       int indicatedTarget,
                                       boolean isFreeBall,
                                       boolean isP1) {
//        System.out.println("Real target: " + indicatedTarget);
        if (value == 0) {
            if (isFreeBall) throw new RuntimeException("自由球打彩球？你他妈懂不懂规则？");
            drawTargetColoredBall(canvas, indicatedTarget, isP1);
        } else {
            double x = isP1 ? ballDiameter * 0.1 : ballDiameter * 1.3;

            BallsGroupPreset bgp = gameValues.getBallsGroupPreset();
            if (bgp == null) {
                Color color = Values.getColorOfTarget(value);
                canvas.getGraphicsContext2D().setFill(color);
                canvas.getGraphicsContext2D().fillOval(x, ballDiameter * 0.1, ballDiameter, ballDiameter);
            } else {
                BallModel bm = getActiveHolder().getBallByValue(value).model;
                Pane tarPane = (Pane) canvas.getParent();
                CustomSphere ballShape = bm.getStaticSphere();
                ballShape.getTransforms().removeIf(transform -> transform instanceof Translate);
//                ballShape.getTransforms().clear();
                ballShape.getTransforms().add(new Translate(x, ballDiameter * 0.8));
                tarPane.getChildren().add(ballShape);
            }

            if (isFreeBall)
                canvas.getGraphicsContext2D().strokeText("F", x + ballDiameter * 0.5, ballDiameter * 0.8);
        }
    }

    /**
     * @param value see {@link Game#getCurrentTarget()}
     */
    private void drawPoolTargetBall(Canvas canvas, int value, boolean isP1) {
        System.out.println(value);
        if (value == 0) {
            drawTargetColoredBall(canvas, 0, isP1);
        } else {
            double x = isP1 ? ballDiameter * 0.6 : ballDiameter * 1.8;

            BallsGroupPreset bgp = gameValues.getBallsGroupPreset();
            Ball ball = getActiveHolder().getBallByValue(value);
            boolean chineseEightDivision = gameValues.rule.eightBallLike() &&
                    (value == ChineseEightBallGame.FULL_BALL_REP || value == ChineseEightBallGame.HALF_BALL_REP);

            // fixme: 差一个分支
            if (bgp == null) {
                NumberedBallTable.drawPoolBallEssential(
                        x,
                        ballDiameter * 0.6,
                        ballDiameter,
                        PoolBall.poolBallBaseColor(value),
                        value,
                        canvas.getGraphicsContext2D()
                );
            } else if (chineseEightDivision) {
                BallModel bm;
                if (value == ChineseEightBallGame.HALF_BALL_REP) {
                    bm = getActiveHolder().getBallByValue(9).model;
                } else {
                    bm = getActiveHolder().getBallByValue(1).model;
                }
                Pane tarPane = (Pane) canvas.getParent();
                CustomSphere ballShape = bm.getStaticSphere();
                ballShape.getTransforms().removeIf(transform -> transform instanceof Translate);
//                ballShape.getTransforms().clear();
                ballShape.getTransforms().add(new Translate(x, ballDiameter * 0.6));
                tarPane.getChildren().add(ballShape);
            } else {
                BallModel bm = ball.model;
                Pane tarPane = (Pane) canvas.getParent();
                CustomSphere ballShape = bm.getStaticSphere();
                ballShape.getTransforms().removeIf(transform -> transform instanceof Translate);
//                ballShape.getTransforms().clear();
                ballShape.getTransforms().add(new Translate(x, ballDiameter * 0.6));
                tarPane.getChildren().add(ballShape);
            }
        }
    }

    private void drawTargetColoredBall(Canvas canvas, int indicatedTarget, boolean isP1) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double y = ballDiameter * 0.1;
        double x = isP1 ? ballDiameter * 0.1 : ballDiameter * 1.3;

        double deg = 0.0;
        for (Color color : Values.COLORED_LOW_TO_HIGH) {
            gc.setFill(color);
            gc.fillArc(x, y, ballDiameter, ballDiameter, deg, 60.0, ArcType.ROUND);
            deg += 60.0;
        }

        if (indicatedTarget > 1) {
            if (game.getGame() instanceof AbstractSnookerGame asg) {
                double leftX = isP1 ?
                        ballDiameter * 1.3 :
                        ballDiameter * 0.1;
                double sphereCenterX = isP1 ?
                        ballDiameter * 1.8 :
                        ballDiameter * 0.6;

                BallsGroupPreset bgp = gameValues.getBallsGroupPreset();
                if (bgp == null) {
                    Color color = Values.getColorOfTarget(indicatedTarget);
                    canvas.getGraphicsContext2D().setFill(color);
                    canvas.getGraphicsContext2D().fillOval(leftX, ballDiameter * 0.1, ballDiameter, ballDiameter);
                } else {
                    BallModel bm = getActiveHolder().getBallByValue(indicatedTarget).model;
                    Pane tarPane = (Pane) canvas.getParent();
                    CustomSphere ballShape = bm.getStaticSphere();
                    ballShape.getTransforms().removeIf(transform -> transform instanceof Translate);
//                    ballShape.getTransforms().clear();
                    ballShape.getTransforms().add(new Translate(sphereCenterX, ballDiameter * 0.6));
                    tarPane.getChildren().add(ballShape);
                }
            }
        }
//        else {
//            gc.setFill(GLOBAL_BACKGROUND);
//            gc.fillRect(canvas.getWidth() / 2, canvas.getHeight(), canvas.getWidth() / 2, canvas.getHeight());
//        }
    }

    private double getCurPlaySpeedMultiplier() {
//        return (replay != null || game.getGame().getCuingPlayer().getInGamePlayer().getPlayerType() == PlayerType.COMPUTER) ?
//                p1PlaySpeed : 1;
        InGamePlayer player = getActiveHolder().getCuingIgp();
        return player.getPlayerNumber() == 1 ? p1PlaySpeed : p2PlaySpeed;
    }

    private double getPredictionLineTotalLength(
            WhitePrediction prediction,
            double potDt,
            PlayerPerson playerPerson) {
//        Cue cue = game.getGame().getCuingPlayer().getInGamePlayer().getCurrentCue(game.getGame());
        Cue cue = getCuingCue();

        // 最大的预测长度
        double origMaxLength = playerPerson.getPrecisionPercentage() / 100 *
                cue.getAccuracyMultiplier() * maxRealPredictLength;
        // 只计算距离的最小长度
        double minLength = origMaxLength / 2.2 * playerPerson.getLongPrecision();

        double potDt2 = Math.max(potDt, maxPredictLengthPotDt);
        double dtRange = minPredictLengthPotDt - maxPredictLengthPotDt;
        double lengthRange = origMaxLength - minLength;
        double potDtInRange = (potDt2 - maxPredictLengthPotDt) / dtRange;
        double predictLength = origMaxLength - potDtInRange * lengthRange;

        double side = Math.abs(cuePointX - cueCanvasWH / 2) / cueCanvasWH;  // 0和0.5之间
        double afterSide = predictLength * (1 - side);  // 加塞影响瞄准
        double mul = 1 - Math.sin(Math.toRadians(cueAngleDeg)); // 抬高杆尾影响瞄准
        double res = afterSide * mul;

        // 这是对靠近库边球瞄准线的惩罚
        // 因为游戏里贴库球瞄准线有库边作参照物，比现实中简单得多，所以要惩罚回来
        double dtToClosetCushion =
                gameValues.table.dtToClosetCushion(
                        prediction.getFirstBallX(),
                        prediction.getFirstBallY()) - gameValues.ball.ballRadius;
        double threshold = gameValues.table.closeCushionPenaltyThreshold();

        // 越接近1说明打底袋的角度越差，但是中袋角度越好
        // 但是又因为离库近的球必定不适合打中袋，所以二次补偿回来的也还将就
        // 唯一的问题就是中袋袋口附近90度的球，预测线会很短；但是那种球拿脚都打得进，所以也无所谓了
        double directionBadness =
                Math.abs(Math.abs(prediction.getBallDirectionX()) -
                        Math.abs(prediction.getBallDirectionY()));
        if (dtToClosetCushion < threshold) {
            double cushionBadness = 1 - dtToClosetCushion / threshold;
            double badness = cushionBadness + directionBadness - 1.0;
            badness = Math.max(badness, 0.0);  // 必须要都很差才算差
            double minimum = 0.25;
            double mul2 = (1 - badness) * (1 - minimum) + minimum;
//            System.out.println("Close to wall, " + directionBadness + ", " + cushionBadness + ", " + mul2);
            res *= mul2;
        }

        if (enablePsy) {
            res *= getPsyAccuracyMultiplier(playerPerson);
        }
        return res;
    }

    private void updatePlayStage() {
        if (game != null)
            currentPlayStage = game.getGame().getGamePlayStage(predictedTargetBall, printPlayStage);
    }

    private GamePlayStage gamePlayStage() {
        if (replay != null)
            return replay.getCueRecord().playStage;
        else
            return currentPlayStage;
    }

    private double getPsyAccuracyMultiplier(PlayerPerson playerPerson) {
        GamePlayStage stage = gamePlayStage();
        switch (stage) {
            case THIS_BALL_WIN:
            case ENHANCE_WIN:
                return playerPerson.psyNerve / 100;
            default:
                return 1.0;
        }
    }

    private double getPsyControlMultiplier(PlayerPerson playerPerson) {
        GamePlayStage stage = gamePlayStage();
        switch (stage) {
            case THIS_BALL_WIN:
            case NEXT_BALL_WIN:
                return playerPerson.psyNerve / 100;
            default:
                return 1.0;
        }
    }

    private void drawStandingPos() {
        Ball cueBall = game.getGame().getCueBall();
        PlayerPerson playerPerson = game.getGame().getCuingPlayer().getPlayerPerson();
        double[] pos1, pos2 = null;
        if (currentHand.playerHand.hand == PlayerHand.Hand.REST) {
            pos1 = HandBody.restStandingPosition(
                    cueBall.getX(), cueBall.getY(),
                    cursorDirectionUnitX, cursorDirectionUnitY,
                    cueAngleDeg,
                    playerPerson,
                    currentHand.cueBrand.getWoodPartLength() + currentHand.cueBrand.getExtensionLength(currentHand.extension)
            );
        } else {
//            System.out.println(currentHand.extension);
            double[][] standingPos = HandBody.personStandingPosition(
                    cueBall.getX(), cueBall.getY(),
                    cursorDirectionUnitX, cursorDirectionUnitY,
                    cueAngleDeg,
                    playerPerson,
                    currentHand.playerHand.hand,
                    currentHand.cueBrand.getWoodPartLength() + currentHand.cueBrand.getExtensionLength(currentHand.extension)
            );
            pos1 = standingPos[0];
            pos2 = standingPos[1];
        }

        double canvasX1 = gamePane.canvasX(pos1[0]);
        double canvasY1 = gamePane.canvasY(pos1[1]);
        gamePane.getGraphicsContext().setStroke(WHITE);
        gamePane.getGraphicsContext().strokeOval(canvasX1 - 5, canvasY1 - 5, 10, 10);
        if (pos2 != null) {
            double canvasX2 = gamePane.canvasX(pos2[0]);
            double canvasY2 = gamePane.canvasY(pos2[1]);
            gamePane.getGraphicsContext().strokeOval(canvasX2 - 5, canvasY2 - 5, 10, 10);
        }
    }

    private void drawInspection() {
        assert potInspection != null;
        Ball srcBall = potInspection.getSrcBall();

        if (srcBall != null) {
            double x = srcBall.getX();
            double y = srcBall.getY();
            double endX = gamePane.realX(mouseX);
            double endY = gamePane.realY(mouseY);

            double[] direction = new double[]{endX - x, endY - y};
            double[] unitDirection = Algebra.unitVector(direction);
            double length = Math.hypot(direction[0], direction[1]);

            double[][] rect = createPolygon(
                    x,
                    y,
                    unitDirection[0],
                    unitDirection[1],
                    length,
                    gameValues.ball.ballRadius
            );

            double[] orth = new double[]{-direction[1], -direction[0]};  // 因为y轴反转了
//            double[] orth = Algebra.normalVector(direction);
            double startDeg = Math.toDegrees(Algebra.thetaOf(orth));

            double wh = gameValues.ball.ballDiameter * gamePane.getScale();

            gamePane.getLineGraphics().setFill(srcBall.getColorWithOpa());
            gamePane.getLineGraphics().fillPolygon(rect[0], rect[1], 4);
            gamePane.getLineGraphics().fillArc(
                    gamePane.canvasX(endX - gameValues.ball.ballRadius),
                    gamePane.canvasY(endY - gameValues.ball.ballRadius),
                    wh,
                    wh,
                    startDeg,
                    180,
                    ArcType.ROUND
            );

            // draw destination ball
            gamePane.getLineGraphics().setStroke(srcBall.getColor());
            gamePane.getLineGraphics().strokeOval(
                    gamePane.canvasX(endX - gameValues.ball.ballRadius),
                    gamePane.canvasY(endY - gameValues.ball.ballRadius),
                    wh,
                    wh
            );

            // draw hit point
            gamePane.getLineGraphics().setStroke(game.getGame().getCueBall().getColor());
            gamePane.getLineGraphics().strokeOval(
                    gamePane.canvasX(x - gameValues.ball.ballDiameter * unitDirection[0] - gameValues.ball.ballRadius),
                    gamePane.canvasY(y - gameValues.ball.ballDiameter * unitDirection[1] - gameValues.ball.ballRadius),
                    wh,
                    wh
            );
        }
    }

    private void drawWhitePathSingle(WhitePrediction prediction) {
        gamePane.drawWhitePathSingle(game.getGame().getCueBall(), prediction);
    }

    private void drawCursor() {
        if (replay != null) return;
        if (game.getGame().isEnded()) return;
        if (isPlayingMovement()) return;
        if (movement != null) return;
        if (cueAnimationPlayer != null) return;
        if (cursorDirectionUnitX == 0.0 && cursorDirectionUnitY == 0.0) return;
        if (game.getGame().getCueBall().isPotted()) return;

        if (drawStandingPos) drawStandingPos();
        if (cursorDrawer == null || cursorDrawer.center == null) return;

//        PlayerPerson playerPerson = game.getGame().getCuingPlayer().getPlayerPerson();

        WhitePrediction center = cursorDrawer.center;
        List<double[]> outPoints = cursorDrawer.outPoints;

        if (outPoints != null && outPoints.size() >= 3) {
            // 画白球的落位范围
            gamePane.drawWhiteStopArea(outPoints);
        }

        // 画白球路线
        drawWhitePathSingle(center);
        if (center.getFirstCollide() != null) {
            gamePane.getLineGraphics().strokeOval(
                    gamePane.canvasX(center.getWhiteCollisionX()) - ballRadius,
                    gamePane.canvasY(center.getWhiteCollisionY()) - ballRadius,
                    ballDiameter,
                    ballDiameter);  // 绘制预测撞击点的白球

            // 画瞄准线
            if (!center.isHitWallBeforeHitBall() && predictedTargetBall != null) {
                gamePane.getLineGraphics().setFill(cursorDrawer.fill);
                gamePane.getLineGraphics().fillPolygon(
                        cursorDrawer.targetPredictionPoly[0],
                        cursorDrawer.targetPredictionPoly[1],
                        4
                );
            }
        }
    }

    private void createPathPrediction() {
        if (replay != null) return;
        if (game.getGame().isEnded()) return;
        if (isPlayingMovement()) return;
        if (movement != null) return;
        if (cursorDirectionUnitX == 0.0 && cursorDirectionUnitY == 0.0) return;
        if (game.getGame().getCueBall().isPotted()) return;
        if (aiCalculating) return;
        if (isGameCalculating()) return;
        if (cursorDrawer != null && cursorDrawer.running) {
            return;
        }

        PlayerPerson playerPerson = game.getGame().getCuingPlayer().getPlayerPerson();
        cursorDrawer.predict(playerPerson);

        tableGraphicsChanged = true;
    }

    private void draw() {
        drawBalls();
        if (tableGraphicsChanged) {  // drawBalls在movement最后一帧会触发一系列反应，让该值改变
            gamePane.drawTable(getActiveHolder());
            if (potInspection == null) {
                drawCursor();
            } else {
                drawInspection();
            }
            if (drawAiPathItem.isSelected()) gamePane.drawPredictedWhitePath(aiWhitePath);
            if (predictPlayerPathItem.isSelected())
                gamePane.drawPredictedWhitePath(suggestedPlayerWhitePath);
        }
        drawBallInHand();
        if (potInspection == null) {
            drawTraces();
        }
        tableGraphicsChanged = false;  // 一定在drawBalls之后
    }

    private void playMovement() {
        playingMovement = true;
        gameLoop.beginNewAnimation();
    }

    private void beginCueAnimationOfHumanPlayer(double whiteStartingX, double whiteStartingY) {
        beginCueAnimation(game.getGame().getCuingPlayer().getInGamePlayer(),
                whiteStartingX, whiteStartingY, getSelectedPower(),
                cursorDirectionUnitX, cursorDirectionUnitY);
    }

    private void beginCueAnimation(InGamePlayer cuingPlayer,
                                   double whiteStartingX, double whiteStartingY,
                                   double selectedPower, double directionX, double directionY) {
        PlayerPerson playerPerson = cuingPlayer.getPlayerPerson();
        double personPower = getPersonPower(currentHand);  // 球手的用力程度
        double errMulWithPower = currentHand.getErrorMultiplierOfPower(selectedPower);
        double maxPullDt = pullDtOf(currentHand.playerHand, personPower);
        double handDt = maxPullDt + HAND_DT_TO_MAX_PULL;

        double[] handXY = handPosition(handDt, cueAngleDeg, whiteStartingX, whiteStartingY, directionX, directionY);
        double handBallDistance = Math.hypot(whiteStartingX - handXY[0], whiteStartingY - handXY[1]);

        Cue cue;
        if (replay != null) {
            cue = replay.getCurrentCue();
        } else {
            cue = getCuingCue();
//            cue = cuingPlayer.getCurrentCue(game.getGame());
        }

        double[] restCuePointing = null;
        if (currentHand != null && currentHand.playerHand.hand == PlayerHand.Hand.REST) {
            double trueAimingAngle = Algebra.thetaOf(cursorDirectionUnitX, cursorDirectionUnitY);
            double restCueAngleOffset = playerPerson.handBody.isLeftHandRest() ? -0.1 : 0.1;
            double restAngleWithOffset = trueAimingAngle - restCueAngleOffset;
            restCuePointing = Algebra.unitVectorOfAngle(restAngleWithOffset);
            maxPullDt *= 0.75;
        }

//        double speedRatio = selectedPower *
//                PlayerPerson.HandBody.getPowerMulOfHand(currentHand) / 100.0;
        double speedRatio = selectedPower / 100.0;
        SoundInfo soundInfo;
        if (miscued) {
            soundInfo = SoundInfo.bySpeed(SoundInfo.SoundType.MISCUE_SOUND, speedRatio);
        } else {
            soundInfo = SoundInfo.bySpeed(SoundInfo.SoundType.CUE_SOUND, speedRatio);
        }

        // 出杆速度与白球球速算法相同
        try {
            cueAnimationPlayer = new CueAnimationPlayer(
                    MIN_CUE_BALL_DT,
                    handBallDistance,
                    maxPullDt,
                    selectedPower,
                    errMulWithPower,
                    handXY[0],
                    handXY[1],
                    directionX,
                    directionY,
                    cue,
                    cuingPlayer,
                    currentHand,
                    restCuePointing,
                    soundInfo
            );
        } catch (RuntimeException re) {
            EventLogger.error(re);
            endCueAnimation();
            playMovement();
        }
    }

    private void endCueAnimation() {
        if (cueAnimationPlayer != null && replay == null) {
            cueAnimationPlayer.hideTime = gameLoop.currentTimeMillis();
            cueAnimationPlayer.cueAnimationRec.setAfterCueMs(
                    (int) ((cueAnimationPlayer.hideTime - cueAnimationPlayer.touchTime) * cueAnimationPlayer.playSpeedMultiplier));
            try {
                game.getGame().getRecorder().recordCueAnimation(cueAnimationPlayer.cueAnimationRec);
            } catch (RecordingException re) {
                EventLogger.error(re);
                game.getGame().abortRecording();
            }
        }
        hideCue();
        cursorDirectionUnitX = 0.0;
        cursorDirectionUnitY = 0.0;
        cueAnimationPlayer = null;
    }

    private void drawCue() {
        boolean notPlay = false;
        Ball cueBall;
        if (replay != null) {
            cueBall = replay.getCueBall();

        } else {
            if (game.getGame().isEnded()) notPlay = true;
            cueBall = game.getGame().getCueBall();
        }

        if (notPlay) return;
        if (cueAnimationPlayer == null) {
            // 绘制人类玩家瞄球时的杆

            if (replay != null) return;
            if (movement != null) return;
            if (cursorDirectionUnitX == 0.0 && cursorDirectionUnitY == 0.0) {
                return;
            }

            if (cueBall.isPotted()) return;

            double aimingOffset = aimingOffsetOfPlayer(
                    game.getGame().getCuingPlayer().getPlayerPerson(),
                    getSelectedPower());
            double trueAimingAngle = Algebra.thetaOf(cursorDirectionUnitX, cursorDirectionUnitY);
            double angleWithOffset = trueAimingAngle - aimingOffset;
            double[] cuePointing = Algebra.unitVectorOfAngle(angleWithOffset);

            PlayerPerson person = game.getGame().getCuingPlayer().getPlayerPerson();
            double personPower = getPersonPower(currentHand);  // 球手的用力程度
            double maxPullDt = pullDtOf(currentHand.playerHand, personPower);
            double handDt = maxPullDt + HAND_DT_TO_MAX_PULL;
//            System.out.println(handDt);
            double[] handXY = handPosition(handDt,
                    cueAngleDeg,
                    cueBall.getX(), cueBall.getY(),
                    cursorDirectionUnitX, cursorDirectionUnitY);

            if (aimingExtensionMenu.isSelected()) {
                // 放这里是因为方向是现成的
                double[] touchXY = getCueHitPoint(handXY[0],
                        handXY[1],
                        cuePointing[0],
                        cuePointing[1]);
                gamePane.drawAimingExtension(touchXY, cuePointing, handDt);
            }

            if (currentHand != null && currentHand.playerHand.hand == PlayerHand.Hand.REST) {
                // 画架杆，要在画杆之前，让杆覆盖在架杆之上
                double restCueAngleOffset = person.handBody.isLeftHandRest() ? -0.1 : 0.1;
                double restAngleWithOffset = trueAimingAngle - restCueAngleOffset;
                double[] restCuePointing = Algebra.unitVectorOfAngle(restAngleWithOffset);

                Cue restCue = DataLoader.getInstance().getRestCue();
                drawCueWithDtToHand(handXY[0], handXY[1],
                        restCuePointing[0],
                        restCuePointing[1],
                        0.0,
                        restCue,
                        true);
            } else {
                getCueModel(DataLoader.getInstance().getRestCue()).hide();
            }

            drawCueWithDtToHand(
                    handXY[0],
                    handXY[1],
                    cuePointing[0],
                    cuePointing[1],
                    MIN_CUE_BALL_DT -
                            maxPullDt - HAND_DT_TO_MAX_PULL +
                            gameValues.ball.ballRadius,
//                    game.getGame().getCuingPlayer().getInGamePlayer().getCurrentCue(game.getGame()),
                    getCuingCue(),
                    false);
        } else {
//            System.out.println("Drawing!");
            if (currentHand != null && currentHand.playerHand.hand == PlayerHand.Hand.REST) {
//                if (cueAnimationPlayer.restCuePointing == null) {
//                    System.err.println("RPNull");
//                    return;
//                }
                // 画架杆，要在画杆之前，让杆覆盖在架杆之上
                Cue restCue = DataLoader.getInstance().getRestCue();
                drawCueWithDtToHand(
                        cueAnimationPlayer.handX,
                        cueAnimationPlayer.handY,
                        cueAnimationPlayer.restCuePointing[0],
                        cueAnimationPlayer.restCuePointing[1],
                        0.0,
                        restCue,
                        true);
            } else {
                getCueModel(DataLoader.getInstance().getRestCue()).hide();
            }

            double[] pointingVec = Algebra.unitVectorOfAngle(cueAnimationPlayer.pointingAngle);
            drawCueWithDtToHand(
                    cueAnimationPlayer.handX,
                    cueAnimationPlayer.handY,
                    pointingVec[0],
                    pointingVec[1],
                    cueAnimationPlayer.cueDtToWhite -
                            cueAnimationPlayer.handBallDistance +
                            gameValues.ball.ballRadius,
                    cueAnimationPlayer.cue,
                    false);
            cueAnimationPlayer.nextFrame();
        }
    }

    /**
     * 将杆画在加了塞的位置
     */
    private double[] getCueHitPoint(double cueBallRealX, double cueBallRealY,
                                    double pointingUnitX, double pointingUnitY) {
        double originalTouchX = gamePane.canvasX(cueBallRealX);
        double originalTouchY = gamePane.canvasY(cueBallRealY);
        double sideRatio = getSelectedSideSpin() * 0.7;
        double sideXOffset = -pointingUnitY *
                sideRatio * gameValues.ball.ballRadius * gamePane.getScale();
        double sideYOffset = pointingUnitX *
                sideRatio * gameValues.ball.ballRadius * gamePane.getScale();
        return new double[]{
                originalTouchX + sideXOffset,
                originalTouchY + sideYOffset
        };
    }

    private void hideCue() {
        List<Cue> toHide = new ArrayList<>();
        toHide.add(DataLoader.getInstance().getRestCue());
        for (InGamePlayer igp : new InGamePlayer[]{getActiveHolder().getP1(), getActiveHolder().getP2()}) {
            for (CueSelection.CueAndBrand cab : igp.getCueSelection().getAvailableCues()) {
                Cue ins = cab.getCueInstance();
                if (ins != null) {
                    toHide.add(ins);
                }
            }
        }

        for (Cue cue : toHide) {
            CueModel cm = cueModelMap.get(cue);
            if (cm != null) cm.hide();
        }
    }

    private void drawCueWithDtToHand(double handX,
                                     double handY,
                                     double pointingUnitX,
                                     double pointingUnitY,
                                     double realDistance,
                                     Cue cue,
                                     boolean isRest) {
//        System.out.println(distance);
//        Ball cb = getActiveHolder().getCueBall();
        double[] touchXY = getCueHitPoint(handX, handY, pointingUnitX, pointingUnitY);

        double cueAngleCos = Math.cos(Math.toRadians(cueAngleDeg));
        double cosDistance = cueAngleCos * realDistance;

        double correctedTipX = touchXY[0] - pointingUnitX * cosDistance * gamePane.getScale();
        double correctedTipY = touchXY[1] - pointingUnitY * cosDistance * gamePane.getScale();

        Bounds ballPanePos = gamePane.localToScene(gamePane.getBoundsInLocal());
        Bounds contentPanePos = contentPane.localToScene(contentPane.getBoundsInLocal());
        double anchorX = ballPanePos.getMinX() - contentPanePos.getMinX();
        double anchorY = ballPanePos.getMinY() - contentPanePos.getMinY();

        Parent basePane = stage.getScene().getRoot();
        if (!basePane.getTransforms().isEmpty()) {
            for (Transform transform : basePane.getTransforms()) {
                if (transform instanceof Scale scale) {
                    anchorX /= scale.getX();
                    anchorY /= scale.getY();
                    break;
                }
            }
        }

        CueModel cueModel = getCueModel(cue);
        cueModel.show(
                anchorX + correctedTipX,
                anchorY + correctedTipY,
                pointingUnitX,
                pointingUnitY,
                isRest ? 0.0 : cueAngleDeg,
                gamePane.getScale(),
                currentHand.extension);
        if (!isRest) {
            if (getActiveHolder().getCuingIgp().getPlayerNumber() == 1) {
                cueModel.setCueRotation(cueRollRotateDeg1);
            } else {
                cueModel.setCueRotation(cueRollRotateDeg2);
            }

        }
    }

    private CueModel getCueModel(Cue cue) {
        CueModel cueModel = cueModelMap.get(cue);
        if (cueModel == null) {
            cueModel = CueModel.createCueModel(cue);
            cueModel.setDisable(true);
            contentPane.getChildren().add(cueModel);  // fixme
//            basePane.getChildren().add(cueModel);  // fixme
            cueModelMap.put(cue, cueModel);
        }

        return cueModel;
    }

    private void calculateCueAbleArea() {
        cueAbleArea = getCuingCue().getCueAbleArea(gameValues.ball, 32);
    }

    private void recalculateObstacles() {
        if (game == null || game.getGame() == null) return;

        Cue currentCue = getCuingCue();
        CueBackPredictor.Result backPre =
                game.getGame().getObstacleDtHeight(cursorDirectionUnitX, cursorDirectionUnitY,
                        currentCue.getCueTipWidth());

        obstacleProjection = ObstacleProjection.createProjection(
                backPre,
                cursorDirectionUnitX,
                cursorDirectionUnitY,
                cueAngleDeg,
                game.getGame().getCueBall(),
                gameValues,
                currentCue.getCueTipWidth()
        );

        calculateCueAbleArea();
    }

    private void recalculateUiRestrictions() {
        recalculateUiRestrictions(false);
    }

    private void recalculateUiRestrictions(boolean forceChangeHand) {
        if (game == null || game.getGame() == null) return;

        recalculateObstacles();

        // 启用/禁用手
        updateHandSelection(forceChangeHand);

        // 如果打点不可能，把出杆键禁用了
        // 自动调整打点太麻烦了
        setCueButtonForPoint();

        // 只有玩家可以换球杆
        InGamePlayer cuingIgp = game.getGame().getCuingIgp();
        changeCueButton.setDisable(!cuingIgp.isHuman());

        createPathPrediction();

        aimingChanged();
    }

    private void setCueButtonForPoint() {
        cueButton.setDisable(obstacleProjection != null &&
                !obstacleProjection.cueAble(
                        getCuePointRelX(cuePointX), getCuePointRelY(cuePointY),
                        getRatioOfCueAndBall()));
    }

    private void drawCueAngleCanvas() {
        double angleCanvasWh = cueAngleCanvas.getWidth();
        double arcRadius = angleCanvasWh * 0.75;
        cueAngleCanvasGc.setFill(GLOBAL_BACKGROUND);
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
        cuePointCanvasGc.setFill(GLOBAL_BACKGROUND);
        cuePointCanvasGc.fillRect(0, 0, cuePointCanvas.getWidth(), cuePointCanvas.getHeight());

        cuePointCanvasGc.setFill(Values.WHITE);
        cuePointCanvasGc.fillOval(padding, padding, cueAreaDia, cueAreaDia);

        if (obstacleProjection instanceof CushionProjection projection) {
            // 影响来自裤边
            double lineYLeft = padding + (projection.getLineYLeft() + 1) * cueAreaRadius;
            double lineYRight = padding + (projection.getLineYRight() + 1) * cueAreaRadius;

            double[] xs = new double[]{
                    padding,
                    padding,
                    cueCanvasWH - padding,
                    cueCanvasWH - padding
            };
            double[] ys = new double[]{
                    cueCanvasWH,
                    lineYLeft,
                    lineYRight,
                    cueCanvasWH
            };
            cuePointCanvasGc.setFill(Color.GRAY);
            cuePointCanvasGc.fillPolygon(xs, ys, 4);

//            if (lineY < cueCanvasWH - padding) {
//                cuePointCanvasGc.setFill(Color.GRAY);
//                cuePointCanvasGc.fillRect(0, lineY, cueCanvasWH, cueCanvasWH - lineY);
//            }
        } else if (obstacleProjection instanceof BallProjection projection) {
            // 后斯诺
            cuePointCanvasGc.setFill(Color.GRAY);
            cuePointCanvasGc.fillOval(padding + cueAreaRadius * projection.getCenterHor(),
                    padding + cueAreaRadius * projection.getCenterVer(),
                    cueAreaDia,
                    cueAreaDia);
        }

        // 画个十字
        cuePointCanvasGc.setStroke(CUE_AIMING_CROSS);
        cuePointCanvasGc.strokeLine(padding, cueCanvasWH / 2, padding + cueAreaDia, cueCanvasWH / 2);
        cuePointCanvasGc.strokeLine(cueCanvasWH / 2, padding, cueCanvasWH / 2, padding + cueAreaDia);

        // 球的边界
        cuePointCanvasGc.setStroke(BLACK);
        cuePointCanvasGc.strokeOval(padding, padding, cueAreaDia, cueAreaDia);

        // 画可用的打点
        if (cueAbleArea != null) {
            double[] xs = new double[cueAbleArea.length];
            double[] ys = new double[cueAbleArea.length];
            for (int i = 0; i < cueAbleArea.length; i++) {
                xs[i] = cueCanvasWH / 2 + cueAbleArea[i][0] * cueAreaRadius;
                ys[i] = cueCanvasWH / 2 + cueAbleArea[i][1] * cueAreaRadius;
            }

            cuePointCanvasGc.setStroke(Color.DIMGRAY);
            cuePointCanvasGc.setLineDashes(2);

            cuePointCanvasGc.strokePolygon(xs, ys, cueAbleArea.length);
        }
        cuePointCanvasGc.setLineDashes();

        double cueRadius = getCuingCue().getCueTip().getRadius();
        double cueRelRadius = cueRadius / gameValues.ball.ballRadius * cueAreaRadius;
        // 画打点
        if (intentCuePointX >= 0 && intentCuePointY >= 0) {
            cuePointCanvasGc.setFill(INTENT_CUE_POINT);
            cuePointCanvasGc.fillOval(intentCuePointX - cueRelRadius,
                    intentCuePointY - cueRelRadius,
                    cueRelRadius * 2,
                    cueRelRadius * 2);
        }

        cuePointCanvasGc.setFill(CUE_POINT);
        cuePointCanvasGc.fillOval(cuePointX - cueRelRadius,
                cuePointY - cueRelRadius,
                cueRelRadius * 2,
                cueRelRadius * 2);

        if (miscued) {
            cuePointCanvasGc.setFill(BLACK);
            cuePointCanvasGc.fillText(strings.getString("miscued"), cueCanvasWH / 2, cueCanvasWH / 2);
        }
    }

    private double getPersonPower(CuePlayerHand person) {
        return getPersonPower(getSelectedPower(), person);
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

    /**
     * Param全是real position
     *
     * @return x点的集合，y点的集合
     */
    private double[][] createPolygon(double startX,
                                     double startY,
                                     double directionX,
                                     double directionY,
                                     double length,
                                     double widthHalf) {
        double lineX = directionX * length;
        double lineY = directionY * length;
        double xShift = directionY * widthHalf;
        double yShift = -directionX * widthHalf;

        double leftStartX = gamePane.canvasX(startX - xShift);
        double rightStartX = gamePane.canvasX(startX + xShift);
        double leftStartY = gamePane.canvasY(startY - yShift);
        double rightStartY = gamePane.canvasY(startY + yShift);

        double leftEndX = gamePane.canvasX(startX - xShift + lineX);
        double rightEndX = gamePane.canvasX(startX + xShift + lineX);
        double leftEndY = gamePane.canvasY(startY - yShift + lineY);
        double rightEndY = gamePane.canvasY(startY + yShift + lineY);

        return new double[][]{
                {
                        leftStartX,
                        leftEndX,
                        rightEndX,
                        rightStartX
                },
                {
                        leftStartY,
                        leftEndY,
                        rightEndY,
                        rightStartY
                }
        };
    }

    class CueAnimationPlayer {
        final double[] restCuePointing;
        private final CueAnimationRec cueAnimationRec;
        private final long holdMs;  // 拉至满弓的停顿时间
        private final long endHoldMs;  // 出杆完成后的停顿时间
        private final double maxPullDistance;
        private final double handBallDistance;
        private final double cueBeforeSpeed;  // 出杆前半段的速度，毫米/ms
        private final double cueMaxSpeed;  // 杆速最快时每毫秒运动的距离，毫米/ms
        //        private final double
        private final double maxExtension;  // 杆的最大延伸距离，始终为负
        //        private final double cueBallX, cueBallY;
        private final double handX, handY;  // 手架的位置，作为杆的摇摆中心点
        private final double errMulWithPower;
        private final double aimingOffset;  // 针对瞄偏打正的球手，杆头向右拐的正
        private final Cue cue;
        private final InGamePlayer igp;
        private final PlayerPerson playerPerson;
        private final CuePlayerHand playerHand;
        private final double playSpeedMultiplier;
        private final long beginTime;
        private long heldMs = 0;
        private long endHeldMs = 0;
        private double cueDtToWhite;  // 杆的动画离白球的真实距离，未接触前为正
        private boolean touched;  // 是否已经接触白球
        private boolean reachedMaxPull;
        private double pointingAngle;
        private boolean ended = false;
        private CuePlayType.DoubleAction doubleAction;
        private double doubleStopDt;  // 如果二段出杆，在哪里停（离白球的距离）
        private double doubleHoldMs;  // 二段出杆停的计时器
        private long touchTime;
        private long hideTime;

        private double framesPlayed = 0;
        private final SoundInfo soundInfo;

        CueAnimationPlayer(double initDistance,
                           double handBallDistance,
                           double maxPullDt,
                           double selectedPower,
                           double errMulWithPower,
                           double handX,
                           double handY,
                           double pointingUnitX,
                           double pointingUnitY,
                           Cue cue,
                           InGamePlayer igp,
                           CuePlayerHand handSkill,
                           double[] restCuePointing,
                           SoundInfo soundInfo) {

            cueAnimationRec = new CueAnimationRec(cue);
            this.soundInfo = soundInfo;
            this.playerHand = handSkill;

            playerPerson = igp.getPlayerPerson();
            double personPower = getPersonPower(selectedPower, handSkill);

            if (selectedPower < Values.MIN_SELECTED_POWER)
                selectedPower = Values.MIN_SELECTED_POWER;

            CuePlayType cuePlayType = handSkill.playerHand.getCuePlayType();
            if (cuePlayType.willApplySpecial(selectedPower, handSkill.playerHand.hand)) {
                CuePlayType.SpecialAction sa = cuePlayType.getSpecialAction();
                if (sa instanceof CuePlayType.DoubleAction) {
                    doubleAction = (CuePlayType.DoubleAction) sa;
                    doubleStopDt = doubleAction.stoppingDtToWhite(selectedPower);
                }
            }

            this.handBallDistance = handBallDistance;
            double initDistance1 = Math.min(initDistance, maxPullDt);
            this.maxPullDistance = maxPullDt;
            this.cueDtToWhite = initDistance1;
            this.maxExtension = -extensionDtOf(handSkill.playerHand, personPower);

            this.cueMaxSpeed = selectedPower *
//                    PlayerPerson.HandBody.getPowerMulOfHand(handSkill) *
                    Values.MAX_POWER_SPEED / 100_000.0;
            this.cueBeforeSpeed = cueMaxSpeed *
                    Algebra.shiftRangeSafe(0, 100, 1, 0.5,
                            handSkill.getMaxSpinPercentage());  // 杆法差的人白用功比较多（无用的杆速快）

            System.out.println("Animation max speed: " + cueMaxSpeed +
                    ", before speed: " + cueBeforeSpeed +
                    ", init distance: " + cueDtToWhite +
                    ", max pull distance: " + maxPullDistance);

            this.errMulWithPower = errMulWithPower;

            this.aimingOffset = aimingOffsetOfPlayer(playerPerson, selectedPower);
            double correctPointingAngle = Algebra.thetaOf(pointingUnitX, pointingUnitY);
            pointingAngle = correctPointingAngle - aimingOffset;

            this.handX = handX;
            this.handY = handY;
            this.restCuePointing = restCuePointing;

            System.out.println(cueDtToWhite + ", " + this.cueMaxSpeed + ", " + maxExtension);

            this.cue = cue;
            this.igp = igp;
//            this.playSpeedMultiplier = (igp.getPlayerType() == PlayerType.COMPUTER || replay != null) ?
//                    p1PlaySpeed : 1;
            this.playSpeedMultiplier = igp.getPlayerNumber() == 1 ? p1PlaySpeed : p2PlaySpeed;

            this.holdMs = cuePlayType.getPullHoldMs();
            this.endHoldMs = cuePlayType.getEndHoldMs();

            beginTime = gameLoop.currentTimeMillis();
        }

        void nextFrame() {
            try {
                for (int i = 0; i < playSpeedMultiplier; i++) {
                    if (framesPlayed % 1.0 == 0.0) {
                        if (ended) return;
                        calculateOneFrame();
                    }
                }
                framesPlayed += playSpeedMultiplier;
            } catch (RuntimeException re) {
                // 防止动画的问题导致游戏卡死
                EventLogger.error(re);
                endCueAnimation();
                playMovement();
            }
        }

        private void calculateOneFrame() {
            if (reachedMaxPull && (heldMs < holdMs || movement == null)) {
                // 后停，至少要停指定的时间。或者是物理运算还没算好时，也用后停来拖时间
                heldMs += gameLoop.lastAnimationFrameMs();
            } else if (endHeldMs > 0) {
                endHeldMs += gameLoop.lastAnimationFrameMs();
                if (endHeldMs >= endHoldMs) {
                    ended = true;
                    endCueAnimation();
                }
            } else if (reachedMaxPull) {
                double lastCueDtToWhite = cueDtToWhite;

                if (doubleAction != null) {
                    double deltaD = gameLoop.lastAnimationFrameMs() * doubleAction.speedMul / 2.5;
                    double nextTickDt = cueDtToWhite - deltaD;
                    if (cueDtToWhite > doubleStopDt) {
                        if (nextTickDt <= doubleStopDt) {
                            // 就是这一帧停
                            doubleHoldMs += gameLoop.lastAnimationFrameMs();
                            if (doubleHoldMs >= doubleAction.holdMs) {
                                // 中停结束了，但是为了代码简单我们还是让它多停一帧
                                doubleAction = null;
                            }
                        } else {
                            // 还没到
                            cueDtToWhite = nextTickDt;
                        }
                        return;
                    } else {
                        System.err.println("Wired. Why there is double cue action but never reached");
                    }
                }
                // 正常出杆
                if (cueDtToWhite > maxPullDistance * 0.4) {
                    cueDtToWhite -= cueBeforeSpeed * gameLoop.lastAnimationFrameMs();
                } else if (!touched) {
                    cueDtToWhite -= cueMaxSpeed * gameLoop.lastAnimationFrameMs();
                } else {
                    cueDtToWhite -= cueBeforeSpeed * gameLoop.lastAnimationFrameMs();
                }
                double wholeDtPercentage = 1 - (cueDtToWhite - maxExtension) /
                        (maxPullDistance - maxExtension);  // 出杆完成的百分比
                wholeDtPercentage = Math.max(0, Math.min(wholeDtPercentage, 0.9999));
//                System.out.println(wholeDtPercentage);

                List<Double> stages = playerHand.playerHand.getCuePlayType().getSequence();
                double stage = stages.isEmpty() ?
                        0 :
                        stages.get((int) (wholeDtPercentage * stages.size()));
                double baseSwingMag = playerHand.playerHand.getCueSwingMag();
                if (enablePsy) {
                    double psyFactor = 1.0 - getPsyAccuracyMultiplier(playerPerson);
                    baseSwingMag *= (1.0 + psyFactor * 5);
                }
                double frameRateRatio = gameLoop.lastAnimationFrameMs() / frameTimeMs;
                if (!touched) {
                    double changeRatio = (lastCueDtToWhite - cueDtToWhite) / maxPullDistance;
                    pointingAngle += aimingOffset * changeRatio;
                }
                double swingMag = baseSwingMag * errMulWithPower / 2000;
//                System.out.println(swingMag);
                swingMag = Math.min(swingMag, 0.015);  // 别扭得太夸张
                if (stage < 0) {  // 向左扭
                    pointingAngle = pointingAngle + swingMag * frameRateRatio;
                } else if (stage > 0) {  // 向右扭
                    pointingAngle = pointingAngle - swingMag * frameRateRatio;
                }

                if (!touched) {
                    if (cueDtToWhite < 0 && lastCueDtToWhite >= 0) {
                        // 就是这一帧碰球！
                        touched = true;
                        touchTime = gameLoop.currentTimeMillis();
                        cueAnimationRec.setBeforeCueMs((int) ((touchTime - beginTime) * playSpeedMultiplier));

                        AudioPlayerManager.getInstance().play(
                                soundInfo,
                                gameValues,
                                getCuingCue(),
                                videoCapture == null ? null : videoCapture.getSoundRecorder(),
                                (int) gameLoop.currentTimeMillis()
                        );
                        playMovement();
                    }
                } else if (cueDtToWhite <= maxExtension) {
                    endHeldMs += gameLoop.lastAnimationFrameMs();  // 出杆结束了
                }
            } else {
                cueDtToWhite += gameLoop.lastAnimationFrameMs() / 3.0 *
                        playerHand.playerHand.getCuePlayType().getPullSpeedMul();  // 往后拉
                if (cueDtToWhite >= maxPullDistance) {
                    reachedMaxPull = true;
                }
            }
        }
    }

    private class PredictionDrawing {
        final int nPoints = predictionQuality.nPoints;

        final int nThreads = Math.min(nPoints, ConfigLoader.getInstance().getInt("nThreads", nPoints));
        Game[] gamePool = new Game[nPoints];
        ExecutorService threadPool;
        ExecutorCompletionService<PredictionResult> ecs;

        WhitePrediction[] clockwise = new WhitePrediction[nPoints];
        WhitePrediction center;
        List<double[]> outPoints;
        LinearGradient fill;

        //        double lineX, lineY;
        double tarX, tarY;
        //        double leftStartX, rightStartX, leftStartY, rightStartY;
//        double leftEndX, rightEndX, leftEndY, rightEndY;
        double[][] targetPredictionPoly;

        boolean running;

        PredictionDrawing() {
            if (predictionQuality.nPoints > 0) {
                threadPool = Executors.newFixedThreadPool(nThreads,
                        r -> {
                            Thread t = Executors.defaultThreadFactory().newThread(r);
                            t.setDaemon(true);
                            return t;
                        });
                ecs = new ExecutorCompletionService<>(threadPool);
            }

            synchronizeGame();
        }

        private void synchronizeGame() {
//            predictionPool[0] = game.getGame();
            for (int i = 0; i < gamePool.length; i++) {
                gamePool[i] = game.getGame().clone();
            }
            center = null;
            outPoints = null;
            fill = null;
            Arrays.fill(clockwise, null);
        }

        private void predict(PlayerPerson playerPerson) {
            running = true;
//            long t0 = System.currentTimeMillis();

            CuePlayParams[] possibles = generateCueParamsSd1(nPoints);
            center = game.getGame().predictWhite(
                    possibles[0],
                    game.whitePhy,
                    WHITE_PREDICT_LEN_AFTER_WALL * playerPerson.getSolving() / 100,
                    predictionQuality != PredictionQuality.NONE,
                    predictionQuality.secondCollision,
                    false,
                    false,
                    true,
                    false);
            if (center == null) {
                predictedTargetBall = null;
                running = false;
                return;
            }

            if (nPoints > 0) {
                for (int i = 0; i < clockwise.length; i++) {
                    final int ii = i;
                    ecs.submit(() -> new PredictionResult(ii, gamePool[ii].predictWhite(
                            possibles[ii + 1],
                            game.whitePhy,
                            WHITE_PREDICT_LEN_AFTER_WALL * playerPerson.getSolving() / 100,
                            predictionQuality != PredictionQuality.NONE,
                            predictionQuality.secondCollision,
                            false,
                            false,
                            true,
                            false
                    )));
                }
                try {
                    for (int i = 0; i < clockwise.length; i++) {
                        Future<PredictionResult> res = ecs.take();
                        PredictionResult pr = res.get(1, TimeUnit.SECONDS);
                        if (pr.wp == null) {
                            throw new InterruptedException();
                        }
                        clockwise[pr.index] = pr.wp;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    synchronizeGame();
                    running = false;
                    return;
                } catch (TimeoutException e) {
                    System.err.println("Cannot finish white prediction in time.");
                    synchronizeGame();
                    running = false;
                    return;
                }
            }

//            long t1 = System.currentTimeMillis();
//            System.out.println("Prediction time: " + (t1 - t0));

            if (nPoints >= 4) {
                double[][] predictionStops = new double[clockwise.length + 1][];
                predictionStops[0] = center.stopPoint();
                for (int i = 0; i < clockwise.length; i++) {
                    predictionStops[i + 1] = clockwise[i].stopPoint();
                }
                if (nPoints == 8) {
                    outPoints = GraphicsUtil.processPoints(gameValues.table, predictionStops);
                } else if (nPoints == 4) {
                    double[][] corners = new double[4][];
                    for (int i = 0; i < corners.length; i++) {
                        corners[i] = clockwise[i].stopPoint();
                    }
                    outPoints = GraphicsUtil.populatePoints(gameValues.table,
                            center.getWhitePath(),
                            corners);
                }
            } else if (nPoints == 2) {
                outPoints = GraphicsUtil.populatePoints(gameValues.table,
                        center.getWhitePath(),
                        clockwise[0].stopPoint(),
                        clockwise[1].stopPoint());
            }

            if (center.getFirstCollide() == null) {
                predictedTargetBall = null;
            } else {
                // 弹库的球就不给预测线了
                if (center.isHitWallBeforeHitBall()) {
                    predictedTargetBall = null;
                } else {
                    predictedTargetBall = center.getFirstCollide();
                    double potDt = Algebra.distanceToPoint(
                            center.getWhiteCollisionX(), center.getWhiteCollisionY(),
                            center.whiteX, center.whiteY);
                    // 白球行进距离越长，预测线越短
                    double predictLineTotalLen = getPredictionLineTotalLength(
                            center,
                            potDt,
                            game.getGame().getCuingPlayer().getPlayerPerson());

                    targetPredictionUnitY = center.getBallDirectionY();
                    targetPredictionUnitX = center.getBallDirectionX();
                    double whiteUnitXBefore = center.getWhiteDirectionXBeforeCollision();
                    double whiteUnitYBefore = center.getWhiteDirectionYBeforeCollision();

                    double theta = Algebra.thetaBetweenVectors(
                            center.getBallDirectionXRaw(),
                            center.getBallDirectionYRaw(),  // 防止齿轮/投掷效应的影响
                            whiteUnitXBefore,
                            whiteUnitYBefore
                    );

                    // 画预测线
                    // 角度越大，目标球预测线越短
                    double pureMultiplier = Algebra.powerTransferOfAngle(theta);
                    double multiplier = Math.pow(pureMultiplier, 1 / playerPerson.getAnglePrecision());

                    // 击球的手的multiplier
//                    double handMul = PlayerPerson.HandBody.getPrecisionOfHand(currentHand);
                    double handMul = playerPerson.handBody.getHandGeneralMultiplier(currentHand);

                    double totalLen = predictLineTotalLen * multiplier * handMul;
                    totalLen = Math.max(totalLen, 1.0);  // 至少也得一毫米吧哈哈哈哈哈哈

                    Ball targetBall = center.getFirstCollide();
                    tarX = targetBall.getX();
                    tarY = targetBall.getY();

                    // 画宽线
                    targetPredictionPoly = createPolygon(
                            tarX,
                            tarY,
                            targetPredictionUnitX,
                            targetPredictionUnitY,
                            totalLen,
                            gameValues.ball.ballRadius
                    );

                    Stop[] stops = new Stop[]{
                            new Stop(0, targetBall.getColorWithOpa()),
                            new Stop(1, targetBall.getColorTransparent())
                    };

                    double sx, sy, ex, ey;
                    if (targetPredictionUnitX < 0) {
                        sx = -targetPredictionUnitX;
                        ex = 0;
                    } else {
                        sx = 0;
                        ex = targetPredictionUnitX;
                    }
                    if (targetPredictionUnitY < 0) {
                        sy = -targetPredictionUnitY;
                        ey = 0;
                    } else {
                        sy = 0;
                        ey = targetPredictionUnitY;
                    }

                    fill = new LinearGradient(
                            sx, sy, ex, ey,
                            true,
                            CycleMethod.NO_CYCLE,
                            stops
                    );
                }
            }

            running = false;
        }

        class PredictionResult {
            final int index;
            final WhitePrediction wp;

            PredictionResult(int index, WhitePrediction wp) {
                this.index = index;
                this.wp = wp;
            }
        }
    }
}
