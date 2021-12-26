package trashsoftware.trashSnooker.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import trashsoftware.trashSnooker.core.ai.AiCue;
import trashsoftware.trashSnooker.core.ai.AiCueResult;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.movement.MovementFrame;
import trashsoftware.trashSnooker.core.movement.WhitePrediction;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.core.numberedGames.sidePocket.SidePocketGame;
import trashsoftware.trashSnooker.core.snooker.MiniSnookerGame;
import trashsoftware.trashSnooker.core.snooker.SnookerGame;
import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.util.Util;

import java.util.*;

public abstract class Game<B extends Ball, P extends Player> {
    public static final double calculateMs = 1.0;
    public static final double calculationsPerSec = 1000.0 / calculateMs;
    public static final double calculationsPerSecSqr = calculationsPerSec * calculationsPerSec;
    public static final double speedReducer = 120.0 / calculationsPerSecSqr;
    public static final double spinReducer = 3400.0 / calculationsPerSecSqr;  // 数值越大，旋转衰减越大
    public static final double sideSpinReducer = 100.0 / calculationsPerSecSqr;
    public static final double spinEffect = 1900.0 / calculateMs;  // 数值越小影响越大
    // 进攻球判定角
    // 如实际角度与可通过的袋口连线的夹角小于该值，判定为进攻球
    public static final double MAX_ATTACK_DECISION_ANGLE = Math.toRadians(7.5);
    protected static final double MIN_PLACE_DISTANCE = 0.0;  // 防止物理运算卡bug
    protected static final double MIN_GAP_DISTANCE = 3.0;
    public final long frameStartTime = System.currentTimeMillis();
    public final int frameIndex;
    protected final Set<B> newPotted = new HashSet<>();
    protected final GameView parent;
    protected final Map<B, double[]> recordedPositions = new HashMap<>();  // 记录上一杆时球的位置，复位用
    protected final B cueBall;
    protected final GameSettings gameSettings;
    protected P player1;
    protected P player2;
    protected int recordedTarget;  // 记录上一杆时的目标球，复位用
    protected int finishedCuesCount = 0;  // 击球的计数器
    protected double lastCueVx;
    protected P currentPlayer;
    /**
     * {@link Game#getCurrentTarget()}
     */
    protected int currentTarget;
    protected Ball whiteFirstCollide;  // 这一杆白球碰到的第一颗球
    protected boolean collidesWall;
    protected boolean ended;
    protected boolean ballInHand = true;
    protected boolean lastCueFoul = false;
    protected boolean lastPotSuccess;
    protected GameValues gameValues;
    private B[] randomOrderBallPool1;
    private B[] randomOrderBallPool2;
    private PhysicsCalculator physicsCalculator;

    protected Game(GameView parent, GameSettings gameSettings, GameValues gameValues,
                   int frameIndex) {
        this.parent = parent;
        this.gameValues = gameValues;
        this.gameSettings = gameSettings;
        this.frameIndex = frameIndex;

        initPlayers();
        currentPlayer = gameSettings.isPlayer1Breaks() ? player1 : player2;
        setBreakingPlayer(currentPlayer);
        cueBall = createWhiteBall();
    }

    public static Game<? extends Ball, ? extends Player> createGame(GameView gameView, GameSettings gameSettings,
                                                                    GameType gameType, int frameIndex) {
        if (gameType == GameType.SNOOKER) {
            return new SnookerGame(gameView, gameSettings, frameIndex);
        } else if (gameType == GameType.MINI_SNOOKER) {
            return new MiniSnookerGame(gameView, gameSettings, frameIndex);
        } else if (gameType == GameType.CHINESE_EIGHT) {
            return new ChineseEightBallGame(gameView, gameSettings, frameIndex);
        } else if (gameType == GameType.SIDE_POCKET) {
            return new SidePocketGame(gameView, gameSettings, frameIndex);
        }

        throw new RuntimeException("Unexpected game type " + gameType);
    }

    protected static void drawBallBase(double canvasX,
                                       double canvasY,
                                       double ballCanvasDiameter,
                                       Color color,
                                       GraphicsContext graphicsContext) {
        drawBallBase(canvasX, canvasY, ballCanvasDiameter, color, graphicsContext, false);
    }

    protected static void drawBallBase(
            double canvasX,
            double canvasY,
            double canvasBallDiameter,
            Color color,
            GraphicsContext graphicsContext,
            boolean drawWhiteBorder) {
        double ballRadius = canvasBallDiameter / 2;
        graphicsContext.setStroke(Values.BALL_CONTOUR);
        graphicsContext.strokeOval(
                canvasX - ballRadius,
                canvasY - ballRadius,
                canvasBallDiameter,
                canvasBallDiameter
        );

        graphicsContext.setFill(color);
        graphicsContext.fillOval(
                canvasX - ballRadius,
                canvasY - ballRadius,
                canvasBallDiameter,
                canvasBallDiameter);

        // 16球9-15号球顶端和底端的白带
        if (drawWhiteBorder) {
            double angle = 50.0;
            graphicsContext.setFill(Values.WHITE);
            graphicsContext.fillArc(
                    canvasX - ballRadius,
                    canvasY - ballRadius,
                    canvasBallDiameter,
                    canvasBallDiameter,
                    90 - angle,
                    angle * 2,
                    ArcType.CHORD
            );
            graphicsContext.fillArc(
                    canvasX - ballRadius,
                    canvasY - ballRadius,
                    canvasBallDiameter,
                    canvasBallDiameter,
                    270 - angle,
                    angle * 2,
                    ArcType.CHORD
            );
        }
    }

    protected void setBreakingPlayer(Player breakingPlayer) {
    }

    protected abstract void initPlayers();

    protected abstract B createWhiteBall();

    protected abstract AiCue<?, ?> createAiCue(P aiPlayer);

    /**
     * 返回所有能打的球
     */
    public abstract List<Ball> getAllLegalBalls(int targetRep, boolean isSnookerFreeBall);

    /**
     * 返回目标的价值，前提条件：ball是有效目标
     */
    public abstract double priceOfTarget(int targetRep, Ball ball);
    
    public boolean isDoingSnookerFreeBll() {
        return false;
    }

    public GameValues getGameValues() {
        return gameValues;
    }

    public Movement cue(CuePlayParams params) {
        whiteFirstCollide = null;
        collidesWall = false;
        lastPotSuccess = false;
        newPotted.clear();
        recordPositions();
        recordedTarget = currentTarget;

        lastCueVx = params.vx;
        cueBall.setVx(params.vx / calculationsPerSec);
        cueBall.setVy(params.vy / calculationsPerSec);
        params.xSpin = params.xSpin == 0.0d ? params.vx / 1000.0 : params.xSpin;  // 避免完全无旋转造成的NaN
        params.ySpin = params.ySpin == 0.0d ? params.vy / 1000.0 : params.ySpin;
        cueBall.setSpin(
                params.xSpin / calculationsPerSec,
                params.ySpin / calculationsPerSec,
                params.sideSpin / calculationsPerSec);
        return physicalCalculate();
    }

    public AiCueResult aiCue(Player aiPlayer) {
        AiCue<?, ?> aiCue = createAiCue((P) aiPlayer);
        return aiCue.makeCue();
    }

    /**
     * @param params                   击球参数
     * @param lengthAfterWall          直接碰库后白球预测线的长度
     * @param checkCollisionAfterFirst 是否检查白球打到目标球后是否碰到下一颗球
     */
    public WhitePrediction predictWhite(CuePlayParams params,
                                        double lengthAfterWall,
                                        boolean checkCollisionAfterFirst) {
        double whiteX = cueBall.x;
        double whiteY = cueBall.y;
        if (cueBall.isPotted()) return null;
        cueBall.setVx(params.vx / calculationsPerSec);
        cueBall.setVy(params.vy / calculationsPerSec);
        params.xSpin = params.xSpin == 0.0d ? params.vx / 1000.0 : params.xSpin;  // 避免完全无旋转造成的NaN
        params.ySpin = params.ySpin == 0.0d ? params.vy / 1000.0 : params.ySpin;
        cueBall.setSpin(
                params.xSpin / calculationsPerSec,
                params.ySpin / calculationsPerSec,
                params.sideSpin / calculationsPerSec);
        WhitePredictor whitePredictor = new WhitePredictor();
//        long st = System.currentTimeMillis();
        WhitePrediction prediction = 
                whitePredictor.predict(lengthAfterWall, checkCollisionAfterFirst);
//        System.out.println("White prediction ms: " + (System.currentTimeMillis() - st));
        cueBall.setX(whiteX);
        cueBall.setY(whiteY);
        cueBall.pickup();
        return prediction;
    }

    public void finishMove() {
        System.out.println("Move end");
        physicsCalculator = null;

        Player player = currentPlayer;
        endMoveAndUpdate();
        finishedCuesCount++;
        parent.finishCue(player, currentPlayer);
    }

    public boolean isCalculating() {
        return physicsCalculator != null && physicsCalculator.notTerminated;
    }

    public void forcedTerminate() {
        if (physicsCalculator != null) {
            physicsCalculator.notTerminated = false;
            for (B ball : getAllBalls()) ball.clearMovement();
        }
    }

    public B getCueBall() {
        return cueBall;
    }

    public void forcedDrawWhiteBall(double realX,
                                    double realY,
                                    GraphicsContext graphicsContext,
                                    double scale) {
        drawBallBase(
                parent.canvasX(realX),
                parent.canvasY(realY),
                gameValues.ballDiameter * scale,
                Values.WHITE,
                graphicsContext);
    }

    public void drawStoppedBalls(GraphicsContext graphicsContext, double scale) {
        for (B ball : getAllBalls()) {
            if (!ball.isPotted()) {
                forceDrawBall(ball, ball.getX(), ball.getY(), graphicsContext, scale);
            }
        }
    }

    /**
     * 绘制球桌上的线条、点位等
     *
     * @param graphicsContext 画布
     * @param scale           比例
     */
    public abstract void drawTableMarks(GraphicsContext graphicsContext, double scale);

    public void quitGame() {
    }

    public void placeWhiteBall(double realX, double realY) {
        if (canPlaceWhite(realX, realY)) {
            cueBall.setX(realX);
            cueBall.setY(realY);
            cueBall.pickup();
            ballInHand = false;
        }
    }

    public final boolean canPlaceWhite(double x, double y) {
        return x >= gameValues.leftX + gameValues.ballRadius &&
                x < gameValues.rightX - gameValues.ballRadius &&
                y >= gameValues.topY + gameValues.ballRadius &&
                y < gameValues.botY - gameValues.ballRadius &&
                canPlaceWhiteInTable(x, y);
    }

    protected abstract boolean canPlaceWhiteInTable(double x, double y);

    public abstract B[] getAllBalls();

    private void reorderRandomPool() {
        if (randomOrderBallPool1 == null) {
            B[] allBalls = getAllBalls();
            randomOrderBallPool1 = Arrays.copyOf(allBalls, allBalls.length);
            randomOrderBallPool2 = Arrays.copyOf(allBalls, allBalls.length);
        }
//        Util.reverseArray(randomArrangedBalls);
        Util.shuffleArray(randomOrderBallPool1);
        Util.shuffleArray(randomOrderBallPool2);
    }

    /**
     * 返回白球后方障碍物的距离与高度
     */
    public CueBackPredictor.Result getObstacleDtHeight(double cursorPointingX,
                                                       double cursorPointingY,
                                                       double cueWidth) {
//        long st = System.currentTimeMillis();
        CueBackPredictor cueBackPredictor =
                new CueBackPredictor(this, cursorPointingX, cursorPointingY, cueWidth,
                        getCuingPlayer().getInGamePlayer()
                                .getCurrentCue(this).getTotalLength() + 300.0,
                        cueBall.x, cueBall.y);
        // System.out.println("Cue back prediction time: " + (System.currentTimeMillis() - st));
        return cueBackPredictor.predict();
    }

    public PredictedPos getPredictedHitBall(double xUnitDirection, double yUnitDirection) {
        double dx = Values.PREDICTION_INTERVAL * xUnitDirection;
        double dy = Values.PREDICTION_INTERVAL * yUnitDirection;

        double x = cueBall.x + dx;
        double y = cueBall.y + dy;
        while (x >= gameValues.leftX &&
                x < gameValues.rightX &&
                y >= gameValues.topY &&
                y < gameValues.botY) {
            for (Ball ball : getAllBalls()) {
                if (!ball.isPotted() && !ball.isWhite()) {
                    if (Algebra.distanceToPoint(
                            x, y, ball.x, ball.y
                    ) < gameValues.ballDiameter) {
                        return new PredictedPos(ball, new double[]{x, y});
                    }
                }
            }
            x += dx;
            y += dy;
        }

        return null;
    }

    public boolean pointToPointCanPassBall(double p1x, double p1y, double p2x, double p2y,
                                           Ball selfBall1, Ball selfBall2) {
        double directionX = p2x - p1x;
        double directionY = p2y - p1y;
        int distance = (int) Math.hypot(directionX, directionY) + 1;
        double[] unitXY = Algebra.unitVector(directionX, directionY);
        double unitX = unitXY[0];
        double unitY = unitXY[1];
        double x = p1x;
        double y = p1y;
        for (int i = 0; i < distance; ++i) {
            for (Ball ball : getAllBalls()) {
                if (ball != selfBall1 && ball != selfBall2) {
                    if (Algebra.distanceToPoint(x, y, ball.x, ball.y) <
                            gameValues.ballDiameter) {
                        // 这个洞被挡住了
                        return false;
                    }
                }
            }
            x += unitX;
            y += unitY;
        }
        return true;
    }

    /**
     * 返回{目标球与"从目标球处能直接看到的洞口"的连线的单位向量, 洞口坐标(注意不是洞底坐标)}。
     */
    public List<double[][]> directionsToAccessibleHoles(Ball targetBall) {
        List<double[][]> list = new ArrayList<>();
//        BIG_LOOP:
        for (double[] hole : gameValues.allHoleOpenCenters) {
//            double directionX = hole[0] - targetBall.x;
//            double directionY = hole[1] - targetBall.y;
//            int distance = (int) Math.hypot(directionX, directionY) + 1;
//            double[] unitXY = Algebra.unitVector(directionX, directionY);
//            double unitX = unitXY[0];
//            double unitY = unitXY[1];
//            double x = targetBall.x;
//            double y = targetBall.y;
//            for (int i = 0; i < distance; ++i) {
//                for (Ball ball : getAllBalls()) {
//                    if (ball != targetBall) {
//                        if (Algebra.distanceToPoint(x, y, ball.x, ball.y) < 
//                                gameValues.ballDiameter) {
//                            // 这个洞被挡住了
//                            continue BIG_LOOP;
//                        }
//                    }
//                }
//                x += unitX;
//                y += unitY;
//            }
            if (pointToPointCanPassBall(targetBall.x, targetBall.y, hole[0], hole[1], targetBall,
                    null)) {
                double directionX = hole[0] - targetBall.x;
                double directionY = hole[1] - targetBall.y;
                double[] unitXY = Algebra.unitVector(directionX, directionY);
                list.add(new double[][]{unitXY, hole});
            }
        }
        return list;
    }

    public boolean isBallInHand() {
        return ballInHand;
    }

    public void setBallInHand() {
        ballInHand = true;
    }

    public Movement collisionTest() {
        Ball ball1 = getAllBalls()[0];
        Ball ball2 = getAllBalls()[1];

        ball1.pickup();
        ball2.pickup();

        ball1.setX(800);
        ball1.setY(800);

        ball1.setVx(6);
        ball1.setVy(0.0);

        ball2.setX(2000);
        ball2.setY(800);

        ball2.setVx(1);
        ball2.setVy(0.0);

        return physicalCalculate();
    }

    public void tieTest() {
//        for (Ball ball : redBalls) ball.pot();
//        yellowBall.pot();
//        greenBall.pot();
//        brownBall.pot();
//        blueBall.pot();
//        pinkBall.pot();
//        player2.addScore(-player2.getScore() + 7);
//        player1.addScore(-player1.getScore());
//        currentPlayer = player1;
//        currentTarget = 7;
    }

    public void clearRedBallsTest() {
//        for (int i = 0; i < 14; ++i) {
//            redBalls[i].pot();
//        }
    }

    public boolean isEnded() {
        return ended;
    }

    public abstract Player getWiningPlayer();

    public Player getCuingPlayer() {
        return currentPlayer;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    /**
     * 斯诺克：
     * 任意彩球=0，特定球=value
     * <p>
     * 十六球：
     * 0=任意，1-15=value，16=黑八（小），17=黑八（大）
     */
    public int getCurrentTarget() {
        return currentTarget;
    }
    
    public abstract int getTargetAfterPotSuccess(Ball pottingBall, boolean isSnookerFreeBall);

    public void withdraw(Player player) {
        player.withdraw();
        ended = true;
    }

    private void whiteCollide(Ball ball) {
        if (whiteFirstCollide == null) {
            whiteFirstCollide = ball;
            collidesWall = false;  // 必须白球在接触首个目标球后，再有球碰库
        }
    }

    private void recordPositions() {
        recordedPositions.clear();
        for (B ball : getAllBalls()) {
            if (!ball.isPotted()) {
                recordedPositions.put(ball, new double[]{ball.getX(), ball.getY()});
            }
        }
    }

    protected boolean isOccupied(double x, double y) {
        for (B ball : getAllBalls()) {
            if (!ball.isPotted()) {
                double dt = Algebra.distanceToPoint(x, y, ball.x, ball.y);
                if (dt < gameValues.ballDiameter + MIN_GAP_DISTANCE) return true;
            }
        }
        return false;
    }

    /**
     * 在指定的位置强行绘制一颗球，无论球是否已经落袋
     */
    public abstract void forceDrawBall(Ball ball,
                                       double absoluteX,
                                       double absoluteY,
                                       GraphicsContext graphicsContext,
                                       double scale);

    private Movement physicalCalculate() {
//        physicsTimer = new Timer();
        long st = System.currentTimeMillis();
        physicsCalculator = new PhysicsCalculator();
        Movement movement = physicsCalculator.calculate();
        System.out.println("Physical calculation ends in " + (System.currentTimeMillis() - st) + " ms");

        return movement;

//        physicsTimer.scheduleAtFixedRate(physicsCalculator, calculateMs, calculateMs);
    }

    protected void potSuccess(boolean isSnookerFreeBall) {
        lastPotSuccess = true;
        updateTargetPotSuccess(isSnookerFreeBall);
    }

    protected void potSuccess() {
        potSuccess(false);
    }

    protected abstract void endMoveAndUpdate();

    protected abstract void updateTargetPotSuccess(boolean isSnookerFreeBall);

    protected abstract void updateTargetPotFailed();

    protected void switchPlayer() {
//        parent.notifyPlayerWillSwitch(currentPlayer);
        currentPlayer.clearSinglePole();
        currentPlayer = getAnotherPlayer();
    }

    protected P getAnotherPlayer() {
        return currentPlayer == player1 ? player2 : player1;
    }

    public class WhitePredictor {
        private double lenAfterWall;
        private WhitePrediction prediction;
        private double cumulatedPhysicalTime = 0.0;
        //        private double lastPhysicalTime = 0.0;
        private double dtWhenHitFirstWall = -1.0;
        private boolean notTerminated = true;
        private boolean hitWall;
        private boolean checkCollisionAfterFirst;

        WhitePrediction predict(double lenAfterWall, boolean checkCollisionAfterFirst) {
            this.lenAfterWall = lenAfterWall;
            this.checkCollisionAfterFirst = checkCollisionAfterFirst;
            prediction = new WhitePrediction(cueBall);

            while (!oneRun() && notTerminated) {
                if (cumulatedPhysicalTime >= 30000) {
                    // Must be something wrong
                    System.err.println("White prediction congestion");
                    break;
                }
            }

            notTerminated = false;
            return prediction;
        }

        /**
         * 返回白球是否已经停止
         */
        private boolean oneRun() {
            cumulatedPhysicalTime += calculateMs;
            prediction.getWhitePath().add(new double[]{cueBall.x, cueBall.y});
            cueBall.prepareMove();

            if (cueBall.isLikelyStopped()) return true;
            if (cueBall.willPot()) {
                prediction.potCueBall();
                return true;
            }

            if (prediction.getFirstCollide() == null &&
                    dtWhenHitFirstWall >= 0.0 &&
                    cueBall.getDistanceMoved() - dtWhenHitFirstWall > lenAfterWall) {
                // 解斯诺克不能太容易了
                return true;
            }

            int holeAreaResult = cueBall.tryHitHoleArea();
            if (holeAreaResult != 0) {
                // 袋口区域
                if (prediction.getFirstCollide() == null) {
                    tryHitBall();
                } else if (checkCollisionAfterFirst && !prediction.whiteCollideOtherBall()) {
                    tryPassSecondBall();
                }
                if (holeAreaResult == 2) {
                    if (!hitWall) {
                        dtWhenHitFirstWall = cueBall.getDistanceMoved();
                    }
                    hitWall = true;
                }
                return false;
            }
            if (cueBall.tryHitWall()) {
                // 库边
                if (!hitWall) {
                    dtWhenHitFirstWall = cueBall.getDistanceMoved();
                }
                hitWall = true;
                return false;
            }
            if (prediction.getFirstCollide() == null) {
                if (tryHitBall()) {
                    cueBall.normalMove();
                    return false;
                }
            } else if (checkCollisionAfterFirst && !prediction.whiteCollideOtherBall()) {
                tryPassSecondBall();
            }
            cueBall.normalMove();
            return false;
        }

        private void tryPassSecondBall() {
            for (Ball ball : getAllBalls()) {
                if (!ball.isWhite() && !ball.isPotted()) {
                    if (cueBall.predictedDtToPoint(ball.x, ball.y) <
                            gameValues.ballDiameter) {
                        prediction.setSecondCollide();
                    }
                }
            }
        }

        private boolean tryHitBall() {
            for (Ball ball : getAllBalls()) {
                if (!ball.isWhite() && !ball.isPotted()) {
                    if (cueBall.predictedDtToPoint(ball.x, ball.y) <
                            gameValues.ballDiameter) {
                        double whiteVx = cueBall.vx;
                        double whiteVy = cueBall.vy;
                        cueBall.twoMovingBallsHitCore(ball);
                        double[] ballDirectionUnitVec = Algebra.unitVector(ball.vx, ball.vy);
                        double[] whiteDirectionUnitVec = Algebra.unitVector(whiteVx, whiteVy);
                        double ballInitVMmPerS = Math.hypot(ball.vx, ball.vy) * Game.calculationsPerSec;
                        ball.vx = 0;
                        ball.vy = 0;
                        prediction.setFirstCollide(ball, hitWall,
                                ballDirectionUnitVec[0], ballDirectionUnitVec[1],
                                ballInitVMmPerS,
                                whiteDirectionUnitVec[0], whiteDirectionUnitVec[1],
                                cueBall.x, cueBall.y);
                        return false;
                    }
                }
            }
            return true;
        }
    }

    public class PhysicsCalculator {

        private Movement movement;
        private double cumulatedPhysicalTime = 0.0;
        private double lastPhysicalTime = 0.0;
        private boolean notTerminated = true;

        Movement calculate() {
            movement = new Movement(getAllBalls());
            while (!oneRun() && notTerminated) {
                if (cumulatedPhysicalTime > 30000) {
                    // Must be something wrong
                    System.err.println("Physical calculation congestion");
                    break;
                }
            }
//            endMove();
            notTerminated = false;

            for (Ball ball : getAllBalls()) {
                if (!ball.isPotted()) {
                    if (ball.getX() < 0 || ball.getX() >= gameValues.outerWidth ||
                            ball.getY() < 0 || ball.getY() >= gameValues.outerHeight) {
                        System.err.println("Ball " + ball + " at a weired position: " +
                                ball.getX() + ", " + ball.getY());
                    }
                }
                if (ball.getValue() == 6) {
                    System.out.println("Pick speed " + ball.vx + ", " + ball.vy + ", " +
                            ball.x + " " + ball.y);
                }
            }

            return movement;
        }

        private boolean oneRun() {
            boolean noBallMoving = true;
//            long st = System.nanoTime();
            reorderRandomPool();
            for (B ball : getAllBalls()) {
                ball.prepareMove();
            }

            for (B ball : getAllBalls()) {
                if (ball.isPotted()) continue;
                if (!ball.isLikelyStopped()) {
                    noBallMoving = false;
                    if (ball.willPot()) {
                        ball.pot();
                        newPotted.add(ball);
                        continue;
                    }
                    int holeAreaResult = ball.tryHitHoleArea();
                    if (holeAreaResult != 0) {
                        // 袋口区域
                        tryHitBall(ball);
                        if (holeAreaResult == 2) collidesWall = true;
                        continue;
                    }
                    if (ball.tryHitWall()) {
                        // 库边
                        collidesWall = true;
                        continue;
                    }

                    boolean noHit = tryHitThreeBalls(ball);
                    if (noHit) {
                        if (tryHitBall(ball)) {
                            ball.normalMove();
                        }
                    }
//                    ball.normalMove();
//                    if (noHit) ball.normalMove();
                }
            }
            lastPhysicalTime = cumulatedPhysicalTime;
            cumulatedPhysicalTime += calculateMs;

            if (Math.floor(cumulatedPhysicalTime / parent.frameTimeMs) !=
                    Math.floor(lastPhysicalTime / parent.frameTimeMs)) {
                for (B ball : getAllBalls()) {
                    movement.getMovementMap().get(ball).addLast(new MovementFrame(ball.x, ball.y, ball.isPotted()));
                }
            }
            return noBallMoving;
//            if (noBallMoving) {
//                endMove();
//            }
//            System.out.print((System.nanoTime() - st) + " ");
        }

        private boolean tryHitThreeBalls(B ball) {
            if (ball.isWhite()) {
                return true;  // 略过白球同时碰到两颗球的情况：无法处理
            }

            boolean noHit = true;
            for (B secondBall : randomOrderBallPool1) {
                if (ball != secondBall) {
                    for (Ball thirdBall : randomOrderBallPool2) {
                        if (ball != thirdBall && secondBall != thirdBall) {
                            if (ball.tryHitTwoBalls(secondBall, thirdBall)) {
                                // 同时撞到两颗球
                                noHit = false;
                                break;
                            }
                        }
                    }
                }
            }
            return noHit;
        }

        private boolean tryHitBall(B ball) {
            boolean noHit = true;
            for (B otherBall : randomOrderBallPool1) {
                if (ball != otherBall) {
                    if (ball.tryHitBall(otherBall)) {
                        // hit ball
                        noHit = false;
                        if (ball.isWhite()) whiteCollide(otherBall);  // 记录白球撞到的球
                        break;  // 假设一颗球在一物理帧内不会撞到两颗球
                    }
                }
            }
            return noHit;
        }
    }
}
