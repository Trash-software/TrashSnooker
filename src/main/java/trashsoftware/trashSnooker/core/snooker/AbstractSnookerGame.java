package trashsoftware.trashSnooker.core.snooker;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.ArcType;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.fxml.GameView;

import java.util.*;

public abstract class AbstractSnookerGame extends Game {

    protected final double[][] pointsRankHighToLow = new double[6][];
    private final SnookerBall yellowBall;
    private final SnookerBall greenBall;
    private final SnookerBall brownBall;
    private final SnookerBall blueBall;
    private final SnookerBall pinkBall;
    private final SnookerBall blackBall;
    private final SnookerBall[] coloredBalls;
    private final SnookerBall[] redBalls = new SnookerBall[numRedBalls()];
    private final SnookerBall[] allBalls;
    private boolean doingFreeBall = false;  // 正在击打自由球

    AbstractSnookerGame(GameView parent, GameSettings gameSettings, GameValues gameValues, 
                        int frameIndex) {
        super(parent, gameSettings, gameValues, frameIndex);

        currentTarget = 1;

        yellowBall = new SnookerBall(2, yellowBallPos(), gameValues);
        greenBall = new SnookerBall(3, greenBallPos(), gameValues);
        brownBall = new SnookerBall(4, brownBallPos(), gameValues);
        blueBall = new SnookerBall(5, blueBallPos(), gameValues);
        pinkBall = new SnookerBall(6, pinkBallPos(), gameValues);
        blackBall = new SnookerBall(7, blackBallPos(), gameValues);
        coloredBalls = 
                new SnookerBall[]{yellowBall, greenBall, brownBall, blueBall, pinkBall, blackBall};

        pointsRankHighToLow[0] = blackBallPos();
        pointsRankHighToLow[1] = pinkBallPos();
        pointsRankHighToLow[2] = blueBallPos();
        pointsRankHighToLow[3] = brownBallPos();
        pointsRankHighToLow[4] = greenBallPos();
        pointsRankHighToLow[5] = yellowBallPos();

        initRedBalls();

        allBalls = new SnookerBall[redBalls.length + 7];
        System.arraycopy(redBalls, 0, allBalls, 0, redBalls.length);
        allBalls[redBalls.length] = yellowBall;
        allBalls[redBalls.length + 1] = greenBall;
        allBalls[redBalls.length + 2] = brownBall;
        allBalls[redBalls.length + 3] = blueBall;
        allBalls[redBalls.length + 4] = pinkBall;
        allBalls[redBalls.length + 5] = blackBall;
        allBalls[redBalls.length + 6] = (SnookerBall) cueBall;
    }

    protected abstract double breakLineX();

    protected abstract double breakArcRadius();
    
    protected abstract int numRedBalls();

    @Override
    public void drawTableMarks(GraphicsContext graphicsContext, double scale) {
        // 开球线
        double breakLineX = parent.canvasX(breakLineX());
        graphicsContext.setStroke(GameView.WHITE);
        graphicsContext.strokeLine(
                breakLineX,
                parent.canvasY(gameValues.topY),
                breakLineX,
                parent.canvasY(gameValues.topY + gameValues.innerHeight));

        // 开球半圆
        double breakArcRadius = breakArcRadius() * scale;
        graphicsContext.strokeArc(
                breakLineX - breakArcRadius,
                parent.canvasY(gameValues.midY) - breakArcRadius,
                breakArcRadius * 2,
                breakArcRadius * 2,
                90.0,
                180.0,
                ArcType.OPEN);

        // 置球点
        drawBallPoints(graphicsContext);
    }

    private void drawBallPoints(GraphicsContext graphicsContext) {
        graphicsContext.setFill(GameView.WHITE);
        double pointRadius = 2.0;
        double pointDiameter = pointRadius * 2;
        for (double[] xy : pointsRankHighToLow) {
            graphicsContext.fillOval(parent.canvasX(xy[0]) - pointRadius,
                    parent.canvasY(xy[1]) - pointRadius,
                    pointDiameter,
                    pointDiameter);
        }
    }

    @Override
    protected void initPlayers() {
        player1 = new SnookerPlayer(1, gameSettings.getPlayer1(), this);
        player2 = new SnookerPlayer(2, gameSettings.getPlayer2(), this);
    }

    @Override
    protected Ball createWhiteBall() {
        return new SnookerBall(0, gameValues);
    }

    @Override
    public Ball[] getAllBalls() {
        return allBalls;
    }

    public Ball getBallOfValue(int score) {
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
    
    public int getScoreDiff() {
        return Math.abs(player1.getScore() - player2.getScore());
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

    public boolean hasFreeBall() {
        if (lastCueFoul) {
            // 当从白球处无法看到任何一颗目标球的最薄边时
            List<Ball> currentTarBalls = currentTargetBalls();
            // 使用预测击球线的方法：如瞄准最薄边时，预测线显示打到的就是这颗球（不会碰到其他球），则没有自由球。
            double simulateBallDiameter = gameValues.ballDiameter - Values.PREDICTION_INTERVAL;
            for (Ball ball : currentTarBalls) {
                // 两球连线、预测的最薄击球点构成两个直角三角形，斜边为连线，其中一个直角边为球直的径（理想状况下）
                double xDiff = ball.getX() - cueBall.getX();
                double yDiff = ball.getY() - cueBall.getY();
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

    protected void updateTargetPotSuccess(boolean isFreeBall) {
        if (currentTarget == 1) {
            currentTarget = 0;
        } else if (currentTarget == 0) {
            if (hasRed()) currentTarget = 1;
            else currentTarget = 2;  // 最后一颗红球附带的彩球打完
        } else if (currentTarget == 7) {  // 黑球进了
            if (player1.getScore() != player2.getScore()) ended = true;
            else {
                // 延分，争黑球
                cueBall.pot();
                ballInHand = true;
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

    protected void updateTargetPotFailed() {
        if (hasRed()) currentTarget = 1;
        else if (currentTarget == 0) currentTarget = 2;  // 最后一颗红球附带的彩球进攻失败
        // 其他情况目标球不变
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

    private int getMaxFoul(Set<Ball> pottedBalls) {
        int foul = 0;
        for (Ball ball : pottedBalls) {
            if (ball.isColored() && ball.getValue() > foul) foul = ball.getValue();
            else foul = getDefaultFoulValue();
        }
        return foul;
    }

    private int getDefaultFoulValue() {
        if (currentTarget == 0 || currentTarget < 4) return 4;
        else return currentTarget;
    }

    protected void updateScore(Set<Ball> pottedBalls, boolean isFreeBall) {
        int score = 0;
        int foul = 0;
        if (whiteFirstCollide == null) {
            foul = getDefaultFoulValue();  // 没打到球，除了白球也不可能有球进，白球进不进也无所谓，分都一样
            if (cueBall.isPotted()) ballInHand = true;
        } else if (cueBall.isPotted()) {
            foul = Math.max(getDefaultFoulValue(), getMaxFoul(pottedBalls));
            ballInHand = true;
        } else if (isFreeBall) {
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
            if (whiteFirstCollide.getValue() == 1) {  // 该打彩球时打了红球
                foul = Math.max(4, getMaxFoul(pottedBalls));
            } else {
                if (currentTarget != 0 && whiteFirstCollide.getValue() != currentTarget) {  // 打了非目标球的彩球
                    foul = Math.max(4, Math.max(whiteFirstCollide.getValue(), currentTarget));
                }
                if (pottedBalls.size() == 1) {
                    if (currentTarget == 0) {  // 任意彩球
                        for (Ball onlyBall : pottedBalls) {
                            if (onlyBall == whiteFirstCollide) score = onlyBall.getValue();
                            else foul = Math.max(4, onlyBall.getValue());
                        }
                    } else {  // 非任意彩球
                        for (Ball onlyBall : pottedBalls) {
                            if (onlyBall.getValue() == currentTarget) {
                                score = currentTarget;
                            } else {
                                foul = Math.max(foul, getMaxFoul(pottedBalls));
                            }
                        }
                    }
                } else if (!pottedBalls.isEmpty()) {
                    foul = getMaxFoul(pottedBalls);
                }
            }
        }
        if (foul > 0) {
            getAnotherPlayer().addScore(foul);
            updateTargetPotFailed();
            switchPlayer();
            lastCueFoul = true;
            lastCueFoul = true;
            if (!cueBall.isPotted() && hasFreeBall()) {
                doingFreeBall = true;
                System.out.println("Free ball!");
            }
        } else if (score > 0) {
            if (isFreeBall) {
                if (pottedBalls.size() != 1) throw new RuntimeException("为什么进了这么多自由球？？？");
                ((SnookerPlayer) currentPlayer).potFreeBall(score);
            } else currentPlayer.correctPotBalls(pottedBalls);
            potSuccess(isFreeBall);
            lastCueFoul = false;
        } else {
            updateTargetPotFailed();
            switchPlayer();
            lastCueFoul = false;
        }
//        System.out.println("Potted: " + pottedBalls + ", first: " + whiteFirstCollide + " score: " + score + ", foul: " + foul);
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

    @Override
    public void clearRedBallsTest() {
        for (int i = 0; i < 14; ++i) {
            redBalls[i].pot();
        }
    }

    public void reposition() {
        System.out.println("Reposition!");
        lastCueFoul = false;
        ballInHand = false;
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

    public int remainingRedCount() {
        int count = 0;
        for (Ball ball : redBalls) {
            if (!ball.isPotted()) count++;
        }
        return count;
    }

    private boolean hasRed() {
        for (Ball redBall : redBalls) if (!redBall.isPotted()) return true;
        return false;
    }

    private void pickupPottedBalls(Set<Ball> pottedBalls) {
        List<Ball> balls = new ArrayList<>(pottedBalls);
        Collections.sort(balls);
        Collections.reverse(balls);  // 如有多颗彩球落袋，优先放置分值高的
//        System.out.print("Pick up");
//        System.out.println(balls);
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
        double[] placePoint = pointsRankHighToLow[7 - ball.getValue()];
        if (isOccupied(placePoint[0], placePoint[1])) {
            boolean placed = false;
            for (double[] otherPoint : pointsRankHighToLow) {
                if (!isOccupied(otherPoint[0], otherPoint[1])) {
                    ball.setX(otherPoint[0]);
                    ball.setY(otherPoint[1]);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                double x = placePoint[0] + MIN_GAP_DISTANCE;
                while (x < gameValues.rightX - gameValues.ballRadius) {
                    if (!isOccupied(x, placePoint[1])) {
                        ball.setX(x);
                        ball.setY(placePoint[1]);
                        placed = true;
                        break;
                    }
                    x += MIN_GAP_DISTANCE;
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

    @Override
    protected void endMoveAndUpdate() {
        boolean isFreeBall = doingFreeBall;
        doingFreeBall = false;

        updateScore(newPotted, isFreeBall);
        if (!ended)
            pickupPottedBalls(newPotted);  // 必须在updateScore之后
    }

    @Override
    public Player getWiningPlayer() {
        if (player1.isWithdrawn()) return player2;
        else if (player2.isWithdrawn()) return player1;

        if (player1.getScore() > player2.getScore()) return player1;
        else if (player2.getScore() > player1.getScore()) return player2;
        else throw new RuntimeException("延分时不会结束");
    }

    protected boolean canPlaceWhiteInTable(double x, double y) {
        return x <= breakLineX() &&
                Algebra.distanceToPoint(x, y, brownBallPos()[0], brownBallPos()[1]) <=
                        breakArcRadius() &&
                !isOccupied(x, y);
    }

    @Override
    public void forceDrawBall(Ball ball,
                              double absoluteX,
                              double absoluteY,
                              GraphicsContext graphicsContext,
                              double scale) {
        drawBallBase(
                parent.canvasX(absoluteX),
                parent.canvasY(absoluteY),
                gameValues.ballDiameter * scale,
                ball.getColor(),
                graphicsContext);
    }

    private void initRedBalls() {
        double curX = pinkBallPos()[0] + gameValues.ballDiameter + Game.MIN_GAP_DISTANCE;  // 粉球与红球堆空隙
        double rowStartY = gameValues.midY;
        double rowOccupyX = gameValues.ballDiameter * Math.sin(Math.toRadians(60.0)) + 
                Game.MIN_PLACE_DISTANCE * 0.8;
        
        double gapDt = Game.MIN_PLACE_DISTANCE;
        
        int ballCountInRow = 1;
        int index = 0;
        for (int row = 0; row < 5; ++row) {
            double y = rowStartY;
            for (int col = 0; col < ballCountInRow; ++col) {
                redBalls[index++] = new SnookerBall(1, new double[]{curX, y}, gameValues);
                y += gameValues.ballDiameter + gapDt;
            }
            ballCountInRow++;
            rowStartY -= gameValues.ballRadius + gapDt * 0.6;
            curX += rowOccupyX;
            if (index >= numRedBalls()) break;
        }
    }

    protected double[] yellowBallPos() {
        return new double[]{breakLineX(), gameValues.midY + breakArcRadius()};
    }

    protected double[] greenBallPos() {
        return new double[]{breakLineX(), gameValues.midY - breakArcRadius()};
    }

    protected double[] brownBallPos() {
        return new double[]{breakLineX(), gameValues.midY};
    }

    protected double[] blueBallPos() {
        return new double[]{gameValues.midX, gameValues.midY};
    }

    protected double[] pinkBallPos() {
        return new double[]{(blackBallPos()[0] + blueBallPos()[0]) / 2, gameValues.midY};
    }

    protected abstract double[] blackBallPos();
}
