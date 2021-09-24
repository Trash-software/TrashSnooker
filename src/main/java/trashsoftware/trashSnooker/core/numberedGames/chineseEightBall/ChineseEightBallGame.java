package trashsoftware.trashSnooker.core.numberedGames.chineseEightBall;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallGame;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ChineseEightBallGame extends NumberedBallGame
        implements NeedBigBreak {

    private static final int[] FULL_BALL_SLOTS = {0, 2, 3, 7, 9, 10, 12};
    private static final int[] HALF_BALL_SLOTS = {1, 5, 6, 7, 11, 13, 14};

    private ChineseEightPlayer winingPlayer;

    private final PoolBall eightBall;
    private final PoolBall[] allBalls = new PoolBall[16];
    private boolean isBreaking = true;
    private String foulReason;

    public ChineseEightBallGame(GameView parent, GameSettings gameSettings) {
        super(parent, gameSettings, GameValues.CHINESE_EIGHT_VALUES);

        eightBall = new PoolBall(8, false, gameValues);
        initBalls();
    }

    private void initBalls() {
        List<PoolBall> fullBalls = new ArrayList<>();
        List<PoolBall> halfBalls = new ArrayList<>();
        for (int i = 0; i < 7; ++i) {
            fullBalls.add(new PoolBall(i + 1, false, gameValues));
        }
        for (int i = 0; i < 7; ++i) {
            halfBalls.add(new PoolBall(i + 9, false, gameValues));
        }

        allBalls[0] = (PoolBall) whiteBall;
        for (int i = 0; i < 7; ++i) {
            allBalls[i + 1] = fullBalls.get(i);
        }
        allBalls[8] = eightBall;
        for (int i = 0; i < 7; ++i) {
            allBalls[i + 9] = halfBalls.get(i);
        }

        Collections.shuffle(fullBalls);
        Collections.shuffle(halfBalls);

        double curX = breakPointX();
        double rowStartY = gameValues.midY;
        double rowOccupyX = gameValues.ballDiameter * Math.sin(Math.toRadians(60.0))
                + Game.MIN_PLACE_DISTANCE * 0.6;
        int ballCountInRow = 1;
        int index = 0;
        for (int row = 0; row < 5; ++row) {
            double y = rowStartY;
            for (int col = 0; col < ballCountInRow; ++col) {
                if (index == 4) {
                    eightBall.setX(curX);
                    eightBall.setY(y);
                } else if (Util.arrayContains(FULL_BALL_SLOTS, index)) {
                    PoolBall ball = fullBalls.remove(fullBalls.size() - 1);
                    ball.setX(curX);
                    ball.setY(y);
                } else {
                    PoolBall ball = halfBalls.remove(halfBalls.size() - 1);
                    ball.setX(curX);
                    ball.setY(y);
                }
                index++;
                y += gameValues.ballDiameter + Game.MIN_PLACE_DISTANCE;
            }
            ballCountInRow++;
            rowStartY -= gameValues.ballRadius + Game.MIN_PLACE_DISTANCE;
            curX += rowOccupyX;
        }
    }

    @Override
    public boolean isBreaking() {
        return isBreaking;
    }

    @Override
    protected double breakPointX() {
        return gameValues.leftX + (gameValues.innerWidth * 0.75);
    }

    @Override
    protected void initPlayers() {
        player1 = new ChineseEightPlayer(1, gameSettings.getPlayer1());
        player2 = new ChineseEightPlayer(2, gameSettings.getPlayer2());
    }

    @Override
    protected boolean canPlaceWhite(double x, double y) {
        if (isTargetSelected()) {
            return !isOccupied(x, y);
        } else {
            return x < breakLineX() && !isOccupied(x, y);
        }
    }

    @Override
    public PoolBall[] getAllBalls() {
        return allBalls;
    }

    private boolean isTargetSelected() {
        return ((ChineseEightPlayer) player1).getBallRange() != 0;
    }

    @Override
    protected void endMoveAndUpdate() {
        updateScore(newPotted);
        isBreaking = false;
    }

    @Override
    public Player getWiningPlayer() {
        return winingPlayer;
    }

    private boolean isFullBall(Ball ball) {
        return ball.getValue() >= 1 && ball.getValue() <= 7;
    }

    private boolean isHalfBall(Ball ball) {
        return ball.getValue() >= 9 && ball.getValue() <= 15;
    }

    private boolean hasFullBallOnTable() {
        for (PoolBall ball : allBalls) {
            if (!ball.isPotted() && isFullBall(ball)) return true;
        }
        return false;
    }

    private boolean hasHalfBallOnTable() {
        for (PoolBall ball : allBalls) {
            if (!ball.isPotted() && isHalfBall(ball)) return true;
        }
        return false;
    }

    private boolean allFullBalls(Set<Ball> balls) {
        for (Ball ball : balls) {
            if (isHalfBall(ball)) return false;
        }
        return true;
    }

    private boolean allHalfBalls(Set<Ball> balls) {
        for (Ball ball : balls) {
            if (isFullBall(ball)) return false;
        }
        return true;
    }

    private int fullBallsCount(Set<Ball> balls) {
        int count = 0;
        for (Ball ball : balls) {
            if (isFullBall(ball)) count++;
        }
        return count;
    }

    private int halfBallsCount(Set<Ball> balls) {
        int count = 0;
        for (Ball ball : balls) {
            if (isHalfBall(ball)) count++;
        }
        return count;
    }

    private boolean hasFullBalls(Set<Ball> balls) {
        for (Ball ball : balls) {
            if (isFullBall(ball)) return true;
        }
        return false;
    }

    private boolean hasHalfBalls(Set<Ball> balls) {
        for (Ball ball : balls) {
            if (isHalfBall(ball)) return true;
        }
        return false;
    }

    private int getTargetOfPlayer(Player playerX) {
        ChineseEightPlayer player = (ChineseEightPlayer) playerX;
        if (player.getBallRange() == 0) return 0;
        if (player.getBallRange() == 16) {
            if (hasFullBallOnTable()) return 16;
            else return 8;
        }
        if (player.getBallRange() == 17) {
            if (hasHalfBallOnTable()) return 17;
            else return 8;
        }
        throw new RuntimeException("不可能");
    }

    private void updateScore(Set<Ball> pottedBalls) {
        boolean foul = false;
        if (!collidesWall && pottedBalls.isEmpty()) {
            foul = true;
            foulReason = "没有任何球接触库边或落袋";
        }

        if (whiteBall.isPotted()) {
            if (eightBall.isPotted()) {  // 白球黑八一起进
                ended = true;
                winingPlayer = (ChineseEightPlayer) getAnotherPlayer();
                return;
            }
            foul = true;
            foulReason = "白球落袋";
        }

        if (whiteFirstCollide == null) {
            foul = true;
            foulReason = "空杆";
        } else {
            if (currentTarget == 16) {
                if (!isFullBall(whiteFirstCollide)) {
                    foul = true;
                    foulReason = "目标球为全色球，但击打了半色球";
                }
            } else if (currentTarget == 17) {
                if (!isHalfBall(whiteFirstCollide)) {
                    foul = true;
                    foulReason = "目标球为半色球，但击打了全色球";
                }
            } else if (currentTarget == 8) {
                if (whiteFirstCollide.getValue() != 8) {
                    foul = true;
                    foulReason = "目标球为黑球，但击打了其他";
                }
            }
        }

        if (foul) {
            lastCueFoul = true;
            whiteBall.pot();
            ballInHand = true;
            switchPlayer();
            currentTarget = getTargetOfPlayer(currentPlayer);  // 在switchPlayer之后
            System.out.println(foulReason);
            return;
        }

        boolean potSuccess = false;

        if (!pottedBalls.isEmpty()) {
            if (pottedBalls.contains(eightBall)) {
                ended = true;
                if (currentTarget == 8) {
                    winingPlayer = (ChineseEightPlayer) currentPlayer;
                } else {  // 误进黑八 todo: 检查开球
                    winingPlayer = (ChineseEightPlayer) getAnotherPlayer();
                }
                return;
            }
            if (currentTarget == 0) {  // 未选球
                if (isBreaking) {  // 开球进袋不算选球
                    return;
                }
                if (allFullBalls(pottedBalls)) {
                    ((ChineseEightPlayer) currentPlayer).setBallRange(16);
                    ((ChineseEightPlayer) getAnotherPlayer()).setBallRange(17);
                    currentTarget = getTargetOfPlayer(currentPlayer);
                    potSuccess = true;
                    currentPlayer.addScore(fullBallsCount(pottedBalls));
                }
                if (allHalfBalls(pottedBalls)) {
                    ((ChineseEightPlayer) currentPlayer).setBallRange(17);
                    ((ChineseEightPlayer) getAnotherPlayer()).setBallRange(16);
                    currentTarget = getTargetOfPlayer(currentPlayer);
                    potSuccess = true;
                    currentPlayer.addScore(halfBallsCount(pottedBalls));
                }
            } else {
                if (currentTarget == 16) {
                    if (hasFullBalls(pottedBalls)) {
                        potSuccess = true;
                    }
                    currentPlayer.addScore(fullBallsCount(pottedBalls));
                } else if (currentTarget == 17) {
                    if (hasHalfBalls(pottedBalls)) {
                        potSuccess = true;
                    }
                    currentPlayer.addScore(halfBallsCount(pottedBalls));
                }
            }
        }
        if (!potSuccess) {
            switchPlayer();
        }
        currentTarget = getTargetOfPlayer(currentPlayer);  // 在switchPlayer之后
    }

    @Override
    protected void updateTargetPotSuccess(boolean isSnookerFreeBall) {

    }

    @Override
    protected void updateTargetPotFailed() {

    }
}
