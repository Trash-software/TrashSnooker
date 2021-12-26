package trashsoftware.trashSnooker.core.numberedGames.chineseEightBall;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.ai.AiCue;
import trashsoftware.trashSnooker.core.ai.ChineseEightAiCue;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallGame;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.util.Util;

import java.util.*;
import java.util.stream.Collectors;

public class ChineseEightBallGame extends NumberedBallGame<ChineseEightBallPlayer>
        implements NeedBigBreak {
    
    public static final int NOT_SELECTED_REP = 0;
    public static final int FULL_BALL_REP = 16;
    public static final int HALF_BALL_REP = 17;

    private static final int[] FULL_BALL_SLOTS = {0, 2, 3, 7, 9, 10, 12};
    private static final int[] HALF_BALL_SLOTS = {1, 5, 6, 7, 11, 13, 14};

    private ChineseEightBallPlayer winingPlayer;

    private final PoolBall eightBall;
    private final PoolBall[] allBalls = new PoolBall[16];
    private boolean isBreaking = true;
    private String foulReason;

    public ChineseEightBallGame(GameView parent, GameSettings gameSettings, int frameIndex) {
        super(parent, gameSettings, GameValues.CHINESE_EIGHT_VALUES, frameIndex);

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

        allBalls[0] = (PoolBall) cueBall;
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
    protected AiCue<?, ?> createAiCue(ChineseEightBallPlayer aiPlayer) {
        return new ChineseEightAiCue(this, aiPlayer);
    }

    @Override
    public List<Ball> getAllLegalBalls(int targetRep, boolean isSnookerFreeBall) {
        List<Ball> res = new ArrayList<>();
        for (Ball ball : getAllBalls()) {
            if (!ball.isPotted() && !ball.isWhite()) {
                if (targetRep == NOT_SELECTED_REP) {
                    if (ball.getValue() != 8) res.add(ball);
                } else if (targetRep == FULL_BALL_REP) {
                    if (ball.getValue() < 8) res.add(ball);
                } else if (targetRep == HALF_BALL_REP) {
                    if (ball.getValue() > 8) res.add(ball);
                } else if (targetRep == 8) {
                    if (ball.getValue() == 8) res.add(ball);
                }
            }
        }
        return res;
    }

    @Override
    public int getTargetAfterPotSuccess(Ball pottingBall, boolean isSnookerFreeBall) {
        return 0;
    }

    @Override
    public double priceOfTarget(int targetRep, Ball ball) {
        return 1.0;
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
        player1 = new ChineseEightBallPlayer(1, gameSettings.getPlayer1());
        player2 = new ChineseEightBallPlayer(2, gameSettings.getPlayer2());
    }

    @Override
    protected boolean canPlaceWhiteInTable(double x, double y) {
        if (isJustAfterBreak()) {
            return x < breakLineX() && !isOccupied(x, y);
        } else {
            return !isOccupied(x, y);
        }
    }

    @Override
    public PoolBall[] getAllBalls() {
        return allBalls;
    }

    private boolean isTargetSelected() {
        return player1.getBallRange() != 0;
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

    private boolean allFullBalls(Set<PoolBall> balls) {
        for (PoolBall ball : balls) {
            if (isHalfBall(ball)) return false;
        }
        return true;
    }

    private boolean allHalfBalls(Set<PoolBall> balls) {
        for (PoolBall ball : balls) {
            if (isFullBall(ball)) return false;
        }
        return true;
    }
    
    private Collection<PoolBall> fullBallsOf(Set<PoolBall> balls) {
        return balls.stream().filter(this::isFullBall).collect(Collectors.toSet());
    }

    private Collection<PoolBall> halfBallsOf(Set<PoolBall> balls) {
        return balls.stream().filter(this::isHalfBall).collect(Collectors.toSet());
    }

    private boolean hasFullBalls(Set<PoolBall> balls) {
        for (PoolBall ball : balls) {
            if (isFullBall(ball)) return true;
        }
        return false;
    }

    private boolean hasHalfBalls(Set<PoolBall> balls) {
        for (PoolBall ball : balls) {
            if (isHalfBall(ball)) return true;
        }
        return false;
    }

    private int getTargetOfPlayer(Player playerX) {
        ChineseEightBallPlayer player = (ChineseEightBallPlayer) playerX;
        if (player.getBallRange() == 0) return 0;
        if (player.getBallRange() == FULL_BALL_REP) {
            if (hasFullBallOnTable()) return FULL_BALL_REP;
            else return 8;
        }
        if (player.getBallRange() == HALF_BALL_REP) {
            if (hasHalfBallOnTable()) return HALF_BALL_REP;
            else return 8;
        }
        throw new RuntimeException("不可能");
    }

    private boolean isJustAfterBreak() {
        return finishedCuesCount == 1;
    }

    private void updateScore(Set<PoolBall> pottedBalls) {
        boolean foul = false;
        if (!collidesWall && pottedBalls.isEmpty()) {
            foul = true;
            foulReason = "没有任何球接触库边或落袋";
        }
        if (lastCueFoul && isJustAfterBreak()) {
            // 开球后直接造成的自由球
            if (lastCueVx < 0) {
                foul = true;
                foulReason = "开球直接造成的自由球必须向前击打";
            }
        }

        if (cueBall.isPotted()) {
            if (eightBall.isPotted()) {  // 白球黑八一起进
                ended = true;
                winingPlayer = getAnotherPlayer();
                return;
            }
            foul = true;
            foulReason = "白球落袋";
        }

        if (whiteFirstCollide == null) {
            foul = true;
            foulReason = "空杆";
        } else {
            if (currentTarget == FULL_BALL_REP) {
                if (!isFullBall(whiteFirstCollide)) {
                    foul = true;
                    foulReason = "目标球为全色球，但击打了半色球";
                }
            } else if (currentTarget == HALF_BALL_REP) {
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
            cueBall.pot();
            ballInHand = true;
            switchPlayer();
            currentTarget = getTargetOfPlayer(currentPlayer);  // 在switchPlayer之后
            System.out.println(foulReason);
            return;
        }

        if (!pottedBalls.isEmpty()) {
            if (pottedBalls.contains(eightBall)) {
                ended = true;
                if (currentTarget == 8) {
                    winingPlayer = currentPlayer;
                } else {  // 误进黑八 todo: 检查开球
                    winingPlayer = getAnotherPlayer();
                }
                return;
            }
            if (currentTarget == NOT_SELECTED_REP) {  // 未选球
                if (isBreaking) {  // 开球进袋不算选球
                    return;
                }
                if (allFullBalls(pottedBalls)) {
                    currentPlayer.setBallRange(FULL_BALL_REP);
                    getAnotherPlayer().setBallRange(HALF_BALL_REP);
                    currentTarget = getTargetOfPlayer(currentPlayer);
                    lastPotSuccess = true;
                    currentPlayer.correctPotBalls(fullBallsOf(pottedBalls));
                }
                if (allHalfBalls(pottedBalls)) {
                    currentPlayer.setBallRange(HALF_BALL_REP);
                    getAnotherPlayer().setBallRange(FULL_BALL_REP);
                    currentTarget = getTargetOfPlayer(currentPlayer);
                    lastPotSuccess = true;
                    currentPlayer.correctPotBalls(fullBallsOf(pottedBalls));
                }
            } else {
                if (currentTarget == FULL_BALL_REP) {
                    if (hasFullBalls(pottedBalls)) {
                        lastPotSuccess = true;
                    }
                    currentPlayer.correctPotBalls(fullBallsOf(pottedBalls));
                } else if (currentTarget == HALF_BALL_REP) {
                    if (hasHalfBalls(pottedBalls)) {
                        lastPotSuccess = true;
                    }
                    currentPlayer.correctPotBalls(halfBallsOf(pottedBalls));
                }
            }
        }
        if (lastPotSuccess) {
            potSuccess();
        } else {
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
