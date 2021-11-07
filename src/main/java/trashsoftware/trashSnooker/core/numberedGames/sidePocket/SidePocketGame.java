package trashsoftware.trashSnooker.core.numberedGames.sidePocket;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallGame;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallPlayer;
import trashsoftware.trashSnooker.fxml.GameView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SidePocketGame extends NumberedBallGame
implements NeedBigBreak{

    private final PoolBall[] allBalls = new PoolBall[10];
    private boolean isBreaking = true;
    private String foulReason;

    public SidePocketGame(GameView parent, GameSettings gameSettings, int frameIndex) {
        super(parent, gameSettings, GameValues.SIDE_POCKET, frameIndex);

        initBalls();
    }

    private void initBalls() {
        allBalls[0] = (PoolBall) cueBall;
        for (int i = 1; i < 10; ++i) {
            allBalls[i] = new PoolBall(i, false, gameValues);
        }
        List<PoolBall> placeOrder = new ArrayList<>(List.of(allBalls).subList(1, 10));
        Collections.shuffle(placeOrder);
        for (int i = 0; i < 9; ++i) {
            PoolBall cur = placeOrder.get(i);
            if (i != 0 && cur.getValue() == 1) {
                placeOrder.set(i, placeOrder.get(0));
                placeOrder.set(0, cur);
            }
            if (i != 4 && cur.getValue() == 9) {
                placeOrder.set(i, placeOrder.get(4));
                placeOrder.set(4, cur);
            }
        }

        double curX = breakPointX();
        double rowOccupyX = gameValues.ballDiameter * Math.sin(Math.toRadians(60.0))
                + Game.MIN_PLACE_DISTANCE * 0.6;

        int[] numBallsRow = new int[]{1, 2, 3, 2, 1};
        int index = 0;
        for (int row = 0; row < 5; ++row) {
            double rowBalls = numBallsRow[row];
            double y = gameValues.midY - (gameValues.ballRadius + Game.MIN_PLACE_DISTANCE) * (rowBalls - 1);
            for (int col = 0; col < rowBalls; ++col) {
                placeOrder.get(index++).setXY(curX, y);
                y += gameValues.ballDiameter + Game.MIN_PLACE_DISTANCE;
            }

            curX += rowOccupyX;
        }
    }

    @Override
    public boolean isBreaking() {
        return isBreaking;
    }

    @Override
    protected void initPlayers() {
        player1 = new SidePocketBallPlayer(1, gameSettings.getPlayer1(), true);
        player2 = new SidePocketBallPlayer(2, gameSettings.getPlayer2());
    }

    @Override
    protected boolean canPlaceWhiteInTable(double x, double y) {
        if (isBreaking) {
            return x < breakLineX() && !isOccupied(x, y);
        } else {
            return !isOccupied(x, y);
        }
    }

    @Override
    public Ball[] getAllBalls() {
        return allBalls;
    }

    @Override
    public Player getWiningPlayer() {
        return null;
    }

    @Override
    protected void endMoveAndUpdate() {

    }

    @Override
    protected void updateTargetPotSuccess(boolean isSnookerFreeBall) {

    }

    @Override
    protected void updateTargetPotFailed() {

    }

    @Override
    protected double breakPointX() {
        return gameValues.leftX + (gameValues.innerWidth * 0.75);
    }
}
