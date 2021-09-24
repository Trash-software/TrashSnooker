package trashsoftware.trashSnooker.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import trashsoftware.trashSnooker.core.numberedGames.sidePocket.SidePocketGame;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.core.snooker.MiniSnookerGame;
import trashsoftware.trashSnooker.core.snooker.SnookerGame;
import trashsoftware.trashSnooker.fxml.GameView;

import java.util.*;

public abstract class Game {
    public static final long calculateMs = 1;
    public static final double calculationsPerSec = 1000.0 / calculateMs;
    public static final double calculationsPerSecSqr = calculationsPerSec * calculationsPerSec;
    public static final double speedReducer = 120.0 / calculationsPerSecSqr;
    public static final double spinReducer = 3800.0 / calculationsPerSecSqr;  // 数值越大，旋转衰减越大
    public static final double spinEffect = 1800.0 / calculateMs;  // 数值越小影响越大
    public static final double sideSpinReducer = 100.0 / calculationsPerSecSqr;

    protected static final double MIN_PLACE_DISTANCE = 0.0;  // 5.0 防止物理运算卡bug
    protected static final double MIN_GAP_DISTANCE = 3.0;

    protected final Set<Ball> newPotted = new HashSet<>();
    protected final GameView parent;
    protected Player player1;
    protected Player player2;
    protected final Map<Ball, double[]> recordedPositions = new HashMap<>();  // 记录上一杆时球的位置，复位用
    protected int recordedTarget;  // 记录上一杆时的目标球，复位用
    protected Player currentPlayer;

    /**
     * {@link Game#getCurrentTarget()}
     */
    protected int currentTarget;
    protected Ball whiteFirstCollide;  // 这一杆白球碰到的第一颗球
    protected boolean collidesWall;
    protected boolean ended;
    protected boolean ballInHand = true;
    protected boolean lastCueFoul = false;

    protected final Ball whiteBall;

    private Timer physicsTimer;
    private PhysicsCalculator physicsCalculator;

    protected final GameSettings gameSettings;
    protected GameValues gameValues;

    protected Game(GameView parent, GameSettings gameSettings, GameValues gameValues) {
        this.parent = parent;
        this.gameValues = gameValues;
        this.gameSettings = gameSettings;

        initPlayers();
        currentPlayer = gameSettings.isPlayer1Breaks() ? player1 : player2;
        whiteBall = createWhiteBall();
    }

    protected abstract void initPlayers();

    protected abstract Ball createWhiteBall();

    public static Game createGame(GameView gameView, GameSettings gameSettings, GameType gameType) {
        if (gameType == GameType.SNOOKER) {
            return new SnookerGame(gameView, gameSettings);
        } else if (gameType == GameType.MINI_SNOOKER) {
            return new MiniSnookerGame(gameView, gameSettings);
        } else if (gameType == GameType.CHINESE_EIGHT) {
            return new ChineseEightBallGame(gameView, gameSettings);
        } else if (gameType == GameType.SIDE_POCKET) {
            return new SidePocketGame(gameView, gameSettings);
        }

        throw new RuntimeException("Unexpected game type " + gameType);
    }

    public GameValues getGameValues() {
        return gameValues;
    }

    /**
     * @param vx       x speed, in real, mm/s
     * @param vy       y speed, in real, mm/s
     * @param xSpin    由旋转产生的横向最大速度，mm/s
     * @param ySpin    由旋转产生的纵向最大速度，mm/s
     * @param sideSpin 由侧旋产生的最大速度，mm/s
     */
    public void cue(double vx, double vy, double xSpin, double ySpin, double sideSpin) {
        whiteFirstCollide = null;
        collidesWall = false;
        newPotted.clear();
        recordPositions();
        recordedTarget = currentTarget;

        whiteBall.setVx(vx / calculationsPerSec);
        whiteBall.setVy(vy / calculationsPerSec);
        xSpin = xSpin == 0.0d ? vx / 1000.0 : xSpin;  // 避免完全无旋转造成的NaN
        ySpin = ySpin == 0.0d ? vy / 1000.0 : ySpin;
        whiteBall.setSpin(
                xSpin / calculationsPerSec,
                ySpin / calculationsPerSec,
                sideSpin / calculationsPerSec);
        startMoving();
    }

    public void forcedTerminate() {
        if (isMoving()) {
            for (Ball ball : getAllBalls()) ball.clearMovement();
        }
    }

    public boolean isMoving() {
        return physicsCalculator != null;
    }

    public Ball getWhiteBall() {
        return whiteBall;
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

    public void drawBalls(GraphicsContext graphicsContext, double scale) {
        for (Ball ball : getAllBalls()) {
            if (!ball.isPotted()) {
                drawBall(ball, graphicsContext, scale);
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
        if (physicsTimer != null) physicsTimer.cancel();
    }

    public void placeWhiteBall(double realX, double realY) {
        if (canPlaceWhite(realX, realY)) {
            whiteBall.setX(realX);
            whiteBall.setY(realY);
            whiteBall.pickup();
            ballInHand = false;
        }
    }

    protected abstract boolean canPlaceWhite(double x, double y);

    public abstract Ball[] getAllBalls();

    public PredictedPos getPredictedHitBall(double xUnitDirection, double yUnitDirection) {
        double oneDiameterX = gameValues.ballDiameter * xUnitDirection;
        double oneDiameterY = gameValues.ballDiameter * yUnitDirection;

        double x = whiteBall.x + oneDiameterX;
        double y = whiteBall.y + oneDiameterY;

        // 1球直径级别
        List<PredictedPos> ballsNearPath = new ArrayList<>();
        double near = gameValues.ballRadius + gameValues.ballDiameter;
        Ball lastAdded = null;
        while (x >= gameValues.leftX &&
                x < gameValues.rightX &&
                y >= gameValues.topY &&
                y < gameValues.botY) {

            double[] whitePos = new double[]{x, y};
            for (Ball ball : getAllBalls()) {
                if (!ball.isWhite()) {
                    if (ball != lastAdded && ball.currentDtToPoint(whitePos) < near) {
                        lastAdded = ball;
                        ballsNearPath.add(new PredictedPos(ball, whitePos));
                    }
                }
            }

            x += oneDiameterX;
            y += oneDiameterY;
        }

        double dx = Values.PREDICTION_INTERVAL * xUnitDirection;
        double dy = Values.PREDICTION_INTERVAL * yUnitDirection;

        for (PredictedPos pos : ballsNearPath) {
            double[] whitePos = pos.getPredictedWhitePos();
            double[] curPos = new double[]{whitePos[0] - oneDiameterX, whitePos[1] - oneDiameterY};
            double exitPosX = whitePos[0] + oneDiameterX;
            double exitPosY = whitePos[1] + oneDiameterY;
            while (
//                    ballInTable(curPos[0] - dx, curPos[1] - dx) &&
                    Algebra.distanceToPoint(curPos[0], curPos[1], exitPosX, exitPosY) >= Values.PREDICTION_INTERVAL) {
                if (pos.getTargetBall().currentDtToPoint(curPos) < gameValues.ballDiameter) {
                    return new PredictedPos(pos.getTargetBall(), new double[]{curPos[0] - dx, curPos[1] - dy});
                }
                curPos[0] += dx;
                curPos[1] += dy;
            }
        }

        return null;
    }

    /**
     * @param xUnitDirection 击球正方向的x单位长度
     * @param yUnitDirection 击球正方向的y单位长度
     * @return 球杆延瞄球线离最近的障碍物的距离
     */
    public double cueBackDistanceToObstacle(double xUnitDirection, double yUnitDirection) {

        // todo: 检测袋角区域
        return getDistanceFromWall(whiteBall.getX(), whiteBall.getY(), -xUnitDirection, -yUnitDirection,
                0, Values.MAX_LENGTH);
    }

    private double getDistanceFromWall(double whiteX, double whiteY, double xUnitRev, double yUnitRev,
                                       double distanceLow, double distanceHigh) {
        double outDistance = (distanceLow + distanceHigh) / 2;
        double inDistance = outDistance - Values.PREDICTION_INTERVAL;
        double outX = whiteX + outDistance * xUnitRev;
        double outY = whiteY + outDistance * yUnitRev;
        double inX = whiteX + inDistance * xUnitRev;
        double inY = whiteY + inDistance * yUnitRev;

//        System.out.println(x + " " + y);
        if (pointInTable(outX, outY, 0)) {  // 外点都在台内，说明距离太小
            return getDistanceFromWall(whiteX, whiteY, xUnitRev, yUnitRev, outDistance, distanceHigh);
        } else if (pointInTable(inX, inY, 0)) {  // 外点在台外，内点在台内，OK
            return inDistance;
        } else {
            return getDistanceFromWall(whiteX, whiteY, xUnitRev, yUnitRev, distanceLow, inDistance);
        }
    }

    private boolean pointInTable(double x, double y, double radius) {
        return x >= gameValues.leftX + radius &&
                x < gameValues.rightX - radius &&
                y >= gameValues.topY + radius &&
                y < gameValues.botY - radius;
    }

    public boolean isBallInHand() {
        return ballInHand;
    }

    public void setBallInHand() {
        ballInHand = true;
    }

    public void collisionTest() {
        Ball ball1 = getAllBalls()[0];
        Ball ball2 = getAllBalls()[1];

        ball1.pickup();
        ball2.pickup();

        ball1.setX(1000);
        ball1.setY(1000);

        ball1.setVx(0.5);
        ball1.setVy(0.5);

        ball2.setX(2000);
        ball2.setY(1500);

        ball2.setVx(-1);
        ball2.setVy(-0.3);

        startMoving();
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
        for (Ball ball : getAllBalls()) {
            if (!ball.isPotted()) {
                recordedPositions.put(ball, new double[]{ball.getX(), ball.getY()});
            }
        }
    }

    protected boolean isOccupied(double x, double y) {
        for (Ball ball : getAllBalls()) {
            if (!ball.isPotted()) {
                double dt = Algebra.distanceToPoint(x, y, ball.x, ball.y);
                if (dt < gameValues.ballDiameter + MIN_GAP_DISTANCE) return true;
            }
        }
        return false;
    }

    protected abstract void drawBall(Ball ball, GraphicsContext graphicsContext, double scale);

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

    private void startMoving() {
        physicsTimer = new Timer();
        physicsCalculator = new PhysicsCalculator();
        physicsTimer.scheduleAtFixedRate(physicsCalculator, calculateMs, calculateMs);
    }

    private void endMove() {
        System.out.println("Move end");
        physicsTimer.cancel();
        physicsCalculator = null;
        physicsTimer = null;

        Player player = currentPlayer;
        endMoveAndUpdate();
        parent.finishCue(player);
    }

    protected abstract void endMoveAndUpdate();

    protected abstract void updateTargetPotSuccess(boolean isSnookerFreeBall);

    protected abstract void updateTargetPotFailed();

    protected void switchPlayer() {
        currentPlayer.clearSinglePole();
        currentPlayer = getAnotherPlayer();
    }

    protected Player getAnotherPlayer() {
        return currentPlayer == player1 ? player2 : player1;
    }

    public class PhysicsCalculator extends TimerTask {

        @Override
        public void run() {
            boolean noBallMoving = true;
//            long st = System.nanoTime();
            for (Ball ball : getAllBalls()) {
                ball.prepareMove();
            }

            for (Ball ball : getAllBalls()) {
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
                    if (noHit) tryHitBall(ball);
                    if (noHit) ball.normalMove();
                }
            }
            if (noBallMoving) {
                endMove();
            }
//            System.out.print((System.nanoTime() - st) + " ");
        }

        private boolean tryHitThreeBalls(Ball ball) {
            if (ball.isWhite()) {
                return true;  // 略过白球同时碰到两颗球的情况：无法处理
            }

            boolean noHit = true;
            for (Ball secondBall : getAllBalls()) {
                if (ball != secondBall) {
                    for (Ball thirdBall : getAllBalls()) {
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

        private boolean tryHitBall(Ball ball) {
            boolean noHit = true;
            for (Ball otherBall : getAllBalls()) {
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
