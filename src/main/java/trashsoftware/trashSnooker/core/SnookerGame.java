package trashsoftware.trashSnooker.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.fxml.GameView;

import java.util.*;

public class SnookerGame {
    public static final long calculateMs = 1;
    public static final double calculationsPerSec = 1000.0 / calculateMs;
    public static final double calculationsPerSecSqr = calculationsPerSec * calculationsPerSec;
    public static final double speedReducer = 120.0 / calculationsPerSecSqr;
    public static final double spinReducer = 2400.0 / calculationsPerSecSqr;
    public static final double spinEffect = 2000.0 / calculateMs;  // 数值越小影响越大
    public static final double sideSpinReducer = 120.0 / calculationsPerSecSqr;

    private static final double MIN_PLACE_DISTANCE = 5.0;
    private final Ball whiteBall = new Ball(0, this);
    private final Ball yellowBall = new Ball(2, Values.YELLOW_POINT_XY, this);
    //    private final Ball yellowBall = new Ball(2, new double[]{1400.0, Values.BOT_Y - Values.BALL_RADIUS}, this);
    private final Ball greenBall = new Ball(3, Values.GREEN_POINT_XY, this);
    //    private final Ball greenBall = new Ball(3, new double[]{1400.0, Values.TOP_Y + Values.BALL_RADIUS}, this);
    private final Ball brownBall = new Ball(4, Values.BROWN_POINT_XY, this);
    private final Ball blueBall = new Ball(5, Values.BLUE_POINT_XY, this);
    private final Ball pinkBall = new Ball(6, Values.PINK_POINT_XY, this);
    private final Ball blackBall = new Ball(7, Values.BLACK_POINT_XY, this);
    private final Ball[] coloredBalls = new Ball[]{yellowBall, greenBall, brownBall, blueBall, pinkBall, blackBall};
    private final Ball[] redBalls = new Ball[15];
    private final Ball[] allBalls = new Ball[22];
    private final Set<Ball> newPotted = new HashSet<>();
    private final GameView parent;
    private final Player player1 = new Player(1, this);
    private final Player player2 = new Player(2, this);
    private final Map<Ball, double[]> recordedPositions = new HashMap<>();  // 记录上一杆时球的位置，复位用
    private int recordedTarget;  // 记录上一杆时的目标球，复位用
    private Player currentPlayer = player1;
    private int currentTarget = 1;  // 任意彩球=0，特定球=value
    private Ball whiteFirstCollide;  // 这一杆白球碰到的第一颗球
    private boolean ended;
    private boolean lastCueFoul = false;
    private boolean doingFreeBall = false;  // 正在击打自由球
    private boolean ballInHand = true;

    private Timer physicsTimer;
    private PhysicsCalculator physicsCalculator;

    public SnookerGame(GameView parent) {
        this.parent = parent;

        double curX = Values.PINK_POINT_XY[0] + Values.BALL_DIAMETER + MIN_PLACE_DISTANCE;  // 粉球与红球堆空隙
        double rowStartY = Values.MID_Y;
        double rowOccupyX = Values.BALL_DIAMETER * Math.sin(Math.toRadians(60.0)) + 3.0;
        int ballCountInRow = 1;
        int index = 0;
        for (int row = 0; row < 5; ++row) {
            double y = rowStartY;
            for (int col = 0; col < ballCountInRow; ++col) {
                redBalls[index++] = new Ball(1, new double[]{curX, y}, this);
                y += Values.BALL_DIAMETER + 4.0;
            }
            ballCountInRow++;
            rowStartY -= Values.BALL_RADIUS + 2.0;
            curX += rowOccupyX;
        }
        System.arraycopy(redBalls, 0, allBalls, 0, 15);
        allBalls[15] = yellowBall;
        allBalls[16] = greenBall;
        allBalls[17] = brownBall;
        allBalls[18] = blueBall;
        allBalls[19] = pinkBall;
        allBalls[20] = blackBall;
        allBalls[21] = whiteBall;
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
            for (Ball ball : allBalls) ball.clearMovement();
        }
    }

    public boolean isMoving() {
        return physicsCalculator != null;
    }

    public Ball getWhiteBall() {
        return whiteBall;
    }

    public void forcedDrawWhiteBall(double realX, double realY,
                                    GraphicsContext graphicsContext,
                                    double scale) {
        drawBall(realX, realY, Values.WHITE, graphicsContext, scale);
    }

    public void drawBalls(GraphicsContext graphicsContext, double scale) {
        drawBall(whiteBall, graphicsContext, scale);
        drawBall(yellowBall, graphicsContext, scale);
        drawBall(greenBall, graphicsContext, scale);
        drawBall(brownBall, graphicsContext, scale);
        drawBall(blueBall, graphicsContext, scale);
        drawBall(pinkBall, graphicsContext, scale);
        drawBall(blackBall, graphicsContext, scale);
        for (Ball redBall : redBalls) {
            drawBall(redBall, graphicsContext, scale);
        }
    }

    public void quitGame() {
        if (physicsTimer != null) physicsTimer.cancel();
    }

    public void placeWhiteBall(double realX, double realY) {
        if (realX < Values.BREAK_LINE_X) {
            if (Algebra.distanceToPoint(realX, realY, Values.BROWN_POINT_XY[0], Values.BROWN_POINT_XY[1]) <
                    Values.BREAK_ARC_RADIUS &&
                    !isOccupied(realX, realY)) {
                whiteBall.setX(realX);
                whiteBall.setY(realY);
                whiteBall.pickup();
                ballInHand = false;
            }
        }
    }

    public Ball[] getAllBalls() {
        return allBalls;
    }

    public PredictedPos getPredictedHitBall(double xUnitDirection, double yUnitDirection) {
        double oneDiameterX = Values.BALL_DIAMETER * xUnitDirection;
        double oneDiameterY = Values.BALL_DIAMETER * yUnitDirection;

        double x = whiteBall.x + oneDiameterX;
        double y = whiteBall.y + oneDiameterY;

        // 1球直径级别
        List<PredictedPos> ballsNearPath = new ArrayList<>();
        double near = Values.BALL_RADIUS + Values.BALL_DIAMETER;
        Ball lastAdded = null;
        while (x >= Values.LEFT_X &&
                x < Values.RIGHT_X &&
                y >= Values.TOP_Y &&
                y < Values.BOT_Y) {

            double[] whitePos = new double[]{x, y};
            for (Ball ball : allBalls) {
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
                if (pos.getTargetBall().currentDtToPoint(curPos) < Values.BALL_DIAMETER) {
                    return new PredictedPos(pos.getTargetBall(), new double[]{curPos[0] - dx, curPos[1] - dy});
                }
                curPos[0] += dx;
                curPos[1] += dy;
            }
        }

        return null;
    }

    private boolean ballInTable(double x, double y) {
        return x >= Values.LEFT_X + Values.BALL_RADIUS &&
                x < Values.RIGHT_X - Values.BALL_RADIUS &&
                y >= Values.TOP_Y + Values.BALL_RADIUS &&
                y < Values.BOT_Y - Values.BALL_RADIUS;
    }

    public boolean isBallInHand() {
        return ballInHand;
    }

    public void setBallInHand() {
        ballInHand = true;
    }

    public void collisionTest() {
        Ball ball1 = redBalls[0];
        Ball ball2 = redBalls[1];

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
        for (Ball ball : redBalls) ball.pot();
        yellowBall.pot();
        greenBall.pot();
        brownBall.pot();
        blueBall.pot();
        pinkBall.pot();
        player2.addScore(-player2.getScore() + 7);
        player1.addScore(-player1.getScore());
        currentPlayer = player1;
        currentTarget = 7;
    }

    public void clearRedBallsTest() {
        for (int i = 0; i < 14; ++i) {
            redBalls[i].pot();
        }
    }

    public boolean isEnded() {
        return ended;
    }

    public Player getWiningPlayer() {
        if (player1.isWithdrawn()) return player2;
        else if (player2.isWithdrawn()) return player1;

        if (player1.getScore() > player2.getScore()) return player1;
        else if (player2.getScore() > player1.getScore()) return player2;
        else throw new RuntimeException("延分时不会结束");
    }

    public Player getNextCuePlayer() {
        return currentPlayer;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public int getCurrentTarget() {
        return currentTarget;
    }

    public int getScoreDiff(Player player) {
        Player another = player == player1 ? player2 : player1;
        return player.getScore() - another.getScore();
    }

    public int getRemainingScore() {
        if (currentTarget == 1) {
            return remainingRedCount() * 8 + 27;
        } else if (currentTarget == 0) {
            return remainingRedCount() * 8 + 34;
        } else {
            int scoreSum = 0;
            for (int sc = currentTarget; sc <= 7; sc++) {
                scoreSum += sc;
            }
            return scoreSum;
        }
    }

    public void withdraw(Player player) {
        player.withdraw();
        ended = true;
    }

    public boolean hasFreeBall() {
        if (lastCueFoul) {
            // 当从白球处无法看到任何一颗目标球的最薄边时
            List<Ball> currentTarBalls = currentTargetBalls();
            // 使用预测击球线的方法：如瞄准最薄边时，预测线显示打到的就是这颗球（不会碰到其他球），则没有自由球。
            double simulateBallDiameter = Values.BALL_DIAMETER - Values.PREDICTION_INTERVAL;
            for (Ball ball : currentTarBalls) {
                // 两球连线、预测的最薄击球点构成两个直角三角形，斜边为连线，其中一个直角边为球直的径（理想状况下）
                double xDiff = ball.x - whiteBall.x;
                double yDiff = ball.y - whiteBall.y;
                double[] vec = new double[]{xDiff, yDiff};
                double[] unitVec = Algebra.unitVector(vec);
                double dt = Math.hypot(xDiff, yDiff);  // 两球球心距离
                double theta = Math.asin(simulateBallDiameter / dt);  // 连线与预测线的夹角
                double alpha = Algebra.thetaOf(unitVec);  // 两球连线与X轴的夹角

                double leftAng = Algebra.normalizeAngle(alpha + theta);
                double rightAng = Algebra.normalizeAngle(alpha - theta);

                double[] leftUnitVec = Algebra.angleToUnitVector(leftAng);
                double[] rightUnitVec = Algebra.angleToUnitVector(rightAng);

                PredictedPos leftPP = getPredictedHitBall(leftUnitVec[0], leftUnitVec[1]);
                PredictedPos rightPP = getPredictedHitBall(rightUnitVec[0], rightUnitVec[1]);

                if ((leftPP == null || leftPP.getTargetBall().getValue() == ball.getValue()) &&
                        (rightPP == null || rightPP.getTargetBall().getValue() == ball.getValue()))
                    // 对于红球而言，能看到任意一颗红球的左侧与任意（可为另一颗）的右侧，则没有自由球
                    return false;  // 两侧都看得到或者看得穿，没有自由球
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean isDoingFreeBall() {
        return doingFreeBall;
    }

    public boolean canReposition() {
        if (lastCueFoul) {
            int minScore = Math.min(player1.getScore(), player2.getScore());
            int maxScore = Math.max(player1.getScore(), player2.getScore());
            return minScore + getRemainingScore() > maxScore;  // 超分或延分不能复位
        } else {
            return false;
        }
    }

    public void reposition() {
        System.out.println("Reposition!");
        lastCueFoul = false;
        doingFreeBall = false;
        for (Map.Entry<Ball, double[]> entry : recordedPositions.entrySet()) {
            Ball ball = entry.getKey();
            ball.setX(entry.getValue()[0]);
            ball.setY(entry.getValue()[1]);
            if (ball.isPotted()) ball.pickup();
        }
        switchPlayer();
        currentTarget = recordedTarget;
    }

    Ball getBallOfValue(int score) {
        switch (score) {
            case 1:
                return redBalls[0];
            case 2:
                return yellowBall;
            case 3:
                return greenBall;
            case 4:
                return brownBall;
            case 5:
                return blueBall;
            case 6:
                return pinkBall;
            case 7:
                return blackBall;
            default:
                throw new RuntimeException("没有这种彩球。");
        }
    }

    private List<Ball> currentTargetBalls() {
        List<Ball> result = new ArrayList<>();
        if (currentTarget == 1) {
            for (Ball ball : redBalls)
                if (!ball.isPotted()) result.add(ball);
        } else if (currentTarget == 0) {
            for (Ball ball : coloredBalls)
                if (!ball.isPotted()) result.add(ball);
        } else {
            result.add(coloredBalls[currentTarget - 2]);
        }
        return result;
    }

    private void recordPositions() {
        recordedPositions.clear();
        for (Ball ball : allBalls) {
            if (!ball.isPotted()) {
                recordedPositions.put(ball, new double[]{ball.getX(), ball.getY()});
            }
        }
    }

    private int remainingRedCount() {
        int count = 0;
        for (Ball ball : redBalls) {
            if (!ball.isPotted()) count++;
        }
        return count;
    }

    private void whiteCollide(Ball ball) {
        if (whiteFirstCollide == null) {
            whiteFirstCollide = ball;
        }
    }

    private boolean hasRed() {
        for (Ball redBall : redBalls) if (!redBall.isPotted()) return true;
        return false;
    }

    private void pickupPottedBalls(Set<Ball> pottedBalls) {
        List<Ball> balls = new ArrayList<>(pottedBalls);
        Collections.sort(balls);
        Collections.reverse(balls);  // 如有多颗彩球落袋，优先放置分值高的
        System.out.println(balls);
        if (currentTarget == 0 || currentTarget == 1) {
            for (Ball ball : balls) {
                if (ball.isColored()) pickupColorBall(ball);
            }
        } else {
            for (Ball ball : balls) {
                if (ball.getValue() >= currentTarget) pickupColorBall(ball);
            }
        }
    }

    private void pickupColorBall(Ball ball) {
        double[] placePoint = Values.POINTS_RANK_HIGH_TO_LOW[7 - ball.getValue()];
        if (isOccupied(placePoint[0], placePoint[1])) {
            boolean placed = false;
            for (double[] otherPoint : Values.POINTS_RANK_HIGH_TO_LOW) {
                if (!isOccupied(otherPoint[0], otherPoint[1])) {
                    ball.setX(otherPoint[0]);
                    ball.setY(otherPoint[1]);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                double x = placePoint[0] + MIN_PLACE_DISTANCE;
                while (x < Values.RIGHT_X - Values.BALL_RADIUS) {
                    if (!isOccupied(x, placePoint[1])) {
                        ball.setX(x);
                        ball.setY(placePoint[1]);
                        placed = true;
                        break;
                    }
                    x += MIN_PLACE_DISTANCE;
                }
                if (!placed) {
                    System.out.println("Failed to place!" + ball);
                }
            }
        } else {
            ball.setX(placePoint[0]);
            ball.setY(placePoint[1]);
        }

        ball.pickup();
    }

    private boolean isOccupied(double x, double y) {
        for (Ball ball : allBalls) {
            if (!ball.isPotted()) {
                double dt = Algebra.distanceToPoint(x, y, ball.x, ball.y);
                if (dt < Values.BALL_DIAMETER + MIN_PLACE_DISTANCE) return true;
            }
        }
        return false;
    }

    private void drawBall(Ball ball,
                          GraphicsContext graphicsContext,
                          double scale) {
        if (ball.isPotted()) return;
        drawBall(ball.x, ball.y, ball.getColor(), graphicsContext, scale);
    }

    private void drawBall(double x, double y,
                          Color color,
                          GraphicsContext graphicsContext,
                          double scale) {
        double scaledX = x * scale;
        double scaledY = y * scale;
        double ballDiameter = Values.BALL_DIAMETER * scale;
        double ballRadius = ballDiameter / 2;
        graphicsContext.setStroke(Values.BALL_CONTOUR);
        graphicsContext.strokeOval(
                scaledX - ballRadius,
                scaledY - ballRadius,
                ballDiameter,
                ballDiameter
        );

        graphicsContext.setFill(color);
        graphicsContext.fillOval(
                scaledX - ballRadius,
                scaledY - ballRadius,
                ballDiameter,
                ballDiameter);
    }

    private void startMoving() {
        physicsTimer = new Timer();
        physicsCalculator = new PhysicsCalculator();
        physicsTimer.scheduleAtFixedRate(physicsCalculator, calculateMs, calculateMs);
    }

    private void endMove() {
        System.out.println("Move end");
        boolean isFreeBall = doingFreeBall;
        doingFreeBall = false;
        physicsTimer.cancel();
        physicsCalculator = null;
        physicsTimer = null;
        Player player = currentPlayer;
        updateScore(newPotted, isFreeBall);
        if (!ended)
            pickupPottedBalls(newPotted);  // 必须在updateScore之后
        parent.finishCue(player);
    }

    private void updateTargetPotSuccess(boolean isFreeBall) {
        if (currentTarget == 1) {
            currentTarget = 0;
        } else if (currentTarget == 0) {
            if (hasRed()) currentTarget = 1;
            else currentTarget = 2;  // 最后一颗红球附带的彩球打完
        } else if (currentTarget == 7) {  // 黑球进了
            if (player1.getScore() != player2.getScore()) ended = true;
            else {
                // 延分，争黑球
                whiteBall.pot();
                if (Math.random() < 0.5) {
                    currentPlayer = player1;
                } else {
                    currentPlayer = player2;
                }
            }
        } else if (!isFreeBall) {
            currentTarget++;
        }
        // 其余情况：清彩球阶段的自由球，目标球不变
    }

    private void updateTargetPotFailed() {
        if (hasRed()) currentTarget = 1;
        else if (currentTarget == 0) currentTarget = 2;  // 最后一颗红球附带的彩球进攻失败
        // 其他情况目标球不变
    }

    private void switchPlayer() {
        currentPlayer.clearSinglePole();
        currentPlayer = getAnotherPlayer();
    }

    private int getMaxFoul(Set<Ball> pottedBalls) {
        int foul = 0;
        for (Ball ball : pottedBalls) {
            if (ball.isColored() && ball.getValue() > foul) foul = ball.getValue();
            else foul = getDefaultFoulValue();
        }
        return foul;
    }

    private int getDefaultFoulValue() {
        if (currentTarget == 1 || currentTarget == 0) return 4;
        else return currentTarget;
    }

    private Player getAnotherPlayer() {
        return currentPlayer == player1 ? player2 : player1;
    }

    private int[] scoreFoulOfFreeBall(Set<Ball> pottedBalls) {
//        System.out.println("Free ball of " + currentTarget);
        if (currentTarget == 0) throw new RuntimeException("不可能自由球打任意彩球");
        Ball target = whiteFirstCollide;  // not null

        if (currentTarget == target.getValue()) {  // 必须选择非当前真正目标球的球作为自由球，todo: 规则存疑
            return new int[]{0, getDefaultFoulValue()};
        }

        if (pottedBalls.size() == 0) {
            return new int[2];
        } else if (pottedBalls.size() == 1) {
            for (Ball onlyBall : pottedBalls) {
                if (onlyBall == target) return new int[]{currentTarget, 0};
            }
            return new int[2];
        } else {
            int foul = getDefaultFoulValue();
            for (Ball ball : pottedBalls) {
                if (ball != target) {  // 第一颗碰到的球视为目标球
                    if (ball.getValue() > foul) foul = ball.getValue();
                }
            }
            return new int[]{0, foul};
        }
    }

    private void updateScore(Set<Ball> pottedBalls, boolean isFreeBall) {
        int score = 0;
        int foul = 0;
        if (whiteFirstCollide == null) foul = getDefaultFoulValue();  // 没打到球
        else if (whiteBall.isPotted()) foul = getDefaultFoulValue();
        else if (isFreeBall) {
            int[] scoreFoul = scoreFoulOfFreeBall(pottedBalls);
            score = scoreFoul[0];
            foul = scoreFoul[1];
        } else if (currentTarget == 1) {
            if (whiteFirstCollide.isRed()) {
                for (Ball ball : pottedBalls) {
                    if (ball.isRed()) {
                        score++;  // 进了颗红球
                    } else {
                        foul = getMaxFoul(pottedBalls);  // 进了颗彩球
                    }
                }
            } else {  // 该打红球时打了彩球
                foul = Math.max(4, whiteFirstCollide.getValue());
            }
        } else {
            if (pottedBalls.size() == 1) {
                if (currentTarget == 0) {  // 任意彩球
                    for (Ball onlyBall : pottedBalls) {
                        if (onlyBall == whiteFirstCollide) score = onlyBall.getValue();
                        else foul = Math.max(4, onlyBall.getValue());
                    }
                } else {
                    for (Ball onlyBall : pottedBalls) {
                        if (onlyBall.getValue() == currentTarget) {
                            score = currentTarget;
                        } else {
                            foul = getMaxFoul(pottedBalls);
                        }
                    }
                }
            } else if (!pottedBalls.isEmpty()) {
                foul = getMaxFoul(pottedBalls);
            }
        }
        if (foul > 0) {
            getAnotherPlayer().addScore(foul);
            updateTargetPotFailed();
            switchPlayer();
            lastCueFoul = true;
            if (!whiteBall.isPotted() && hasFreeBall()) {
                doingFreeBall = true;
                System.out.println("Free ball!");
            }
        } else if (score > 0) {
            if (isFreeBall) {
                if (pottedBalls.size() != 1) throw new RuntimeException("为什么进了这么多自由球？？？");
                currentPlayer.potFreeBall(score);}
            else currentPlayer.correctPotBalls(pottedBalls);
            updateTargetPotSuccess(isFreeBall);
            lastCueFoul = false;
        } else {
            updateTargetPotFailed();
            switchPlayer();
            lastCueFoul = false;
        }
//        System.out.println("Potted: " + pottedBalls + ", first: " + whiteFirstCollide + " score: " + score + ", foul: " + foul);
    }

    public class PhysicsCalculator extends TimerTask {

        @Override
        public void run() {
            boolean noBallMoving = true;
//            long st = System.nanoTime();
            for (Ball ball : allBalls) {
                ball.prepareMove();
            }

            for (Ball ball : allBalls) {
                if (ball.isPotted()) continue;
                if (!ball.isLikelyStopped()) {
                    noBallMoving = false;
                    if (ball.willPot()) {
                        ball.pot();
                        newPotted.add(ball);
                        continue;
                    }
                    if (ball.tryHitHoleArea()) {
                        // 袋口区域
                        tryHitBall(ball);
                        continue;
                    }
                    if (ball.tryHitWall()) {
                        // 库边
                        continue;
                    }

                    boolean noHit = tryHitBall(ball);
                    if (noHit) ball.normalMove();
                }
            }
            if (noBallMoving) {
                endMove();
            }
//            System.out.print((System.nanoTime() - st) + " ");
        }

        private boolean tryHitBall(Ball ball) {
            boolean noHit = true;
            for (Ball otherBall : allBalls) {
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
