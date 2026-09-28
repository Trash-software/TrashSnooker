package trashsoftware.trashSnooker.core.russian;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.ai.AiCue;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;
import trashsoftware.trashSnooker.core.game.VariableCueBallGame;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.scoreResult.RussianScoreResult;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;
import trashsoftware.trashSnooker.core.table.RussianTable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class RussianGame extends VariableCueBallGame<RussianBall, RussianPlayer> {

    private final LinkedHashMap<RussianBall, Boolean> pottedRecord = new LinkedHashMap<>();  // 缓存用，仅用于GameView画目标球
    protected RussianPlayer winingPlayer;
    private boolean wasIllegalBreak;
    private boolean foulBallPicked;

    public RussianGame(EntireGame entireGame, GameSettings gameSettings, GameValues gameValues, int frameIndex, int frameNumber) {
        super(entireGame, gameSettings, gameValues, new RussianTable(gameValues.table), frameIndex, frameNumber);

        initBalls();
    }

    private void initBalls() {
        allBalls = new RussianBall[16];

        allBalls[0] = getCueBall();  // 初始母球已在super里面创建

        for (int i = 1; i < allBalls.length; i++) {
            allBalls[i] = new RussianBall(i, false, gameValues);
            pottedRecord.put(allBalls[i], false);
        }

        double curX = getTable().rackFirstBallX(gameValues);
        double rowStartY = gameValues.table.midY;
        double rowOccupyX = gameValues.ball.ballDiameter * Math.sin(Math.toRadians(60.0))
                + Game.MIN_PLACE_DISTANCE * 0.6;
        int ballCountInRow = 1;
        int index = 1;
        for (int row = 0; row < 5; ++row) {
            double y = rowStartY;
            for (int col = 0; col < ballCountInRow; ++col) {
                RussianBall ball = allBalls[index];
                ball.setX(curX);
                ball.setY(y);
                index++;
                y += gameValues.ball.ballDiameter + Game.MIN_PLACE_DISTANCE;
            }
            ballCountInRow++;
            rowStartY -= gameValues.ball.ballRadius + Game.MIN_PLACE_DISTANCE;
            curX += rowOccupyX;
        }
    }

    @Override
    protected void cloneBalls(RussianBall[] allBalls) {
        RussianBall[] allBallsCopy = new RussianBall[allBalls.length];
        for (int i = 0; i < allBalls.length; i++) {
            allBallsCopy[i] = (RussianBall) allBalls[i].clone();
        }
        this.allBalls = allBallsCopy;
    }

    @Override
    public GameRule getGameType() {
        return GameRule.RUSSIAN;
    }

    @Override
    protected void initPlayers() {
        player1 = new RussianPlayer(entireGame.getPlayer1());
        player2 = new RussianPlayer(entireGame.getPlayer2());
    }

    @Override
    protected RussianBall createInitWhiteBall() {
        return new RussianBall(0, true, gameValues);
    }

    public Map<RussianBall, Boolean> getBalls() {
        for (int i = 1; i < 16; i++) {
            pottedRecord.put(allBalls[i], allBalls[i].isPotted());
        }
        return pottedRecord;
    }

    @Override
    public Movement cue(CuePlayParams params, Phy phy) {
        foulBallPicked = false;
        return super.cue(params, phy);
    }

    @Override
    protected boolean ballPickableWhenValid(Ball ball) {
        return true;
    }

    @Override
    protected void updateAskPickBall() {
        askingPickBall = !foulBallPicked && thisCueFoul != null && thisCueFoul.isFoul();
    }

    @Override
    public void pickBall(Ball validBall) {
        validBall.pot();
        getCuingPlayer().addScore(1);
        foulBallPicked = true;
        updateAskPickBall();
    }

    @Override
    protected boolean isBallPlacedInHeap(Ball ball) {
        return false;
    }

    @Override
    protected AiCue<?, ?> createAiCue(RussianPlayer aiPlayer) {
        return null;
    }

    @Override
    public ScoreResult makeScoreResult(Player justCuedPlayer) {
        return new RussianScoreResult(
                thinkTime,
                player1.getScore(),
                player2.getScore(),
                player1.getLastAddedScore(),
                player2.getLastAddedScore(),
                getCuingPlayer().getInGamePlayer().getPlayerNumber(),
                getBalls(),
                getCuingPlayer().getSinglePoleCount());
    }

    @Override
    public boolean isLegalBall(Ball ball, int targetRep, boolean isSnookerFreeBall, boolean isInLineHandBall) {
        return !ball.isPotted() && !ball.equals(getCueBall());
    }

    @Override
    public double priceOfTarget(int targetRep, Ball ball, Player attackingPlayer, Ball lastPotting) {
        return 1;
    }

    @Override
    protected boolean canPlaceWhiteInTable(double x, double y) {
        if (isBreaking()) {
            return x <= getTable().breakLineX() && !isOccupied(x, y);
        } else {
            return !isOccupied(x, y);
        }
    }

    @Override
    protected void setBreakingPlayer(Player breakingPlayer) {
        super.setBreakingPlayer(breakingPlayer);

        ((RussianPlayer) breakingPlayer).setBreakingPlayer();
    }

    @Override
    public void switchPlayer() {
        super.switchPlayer();
        currentPlayer.incrementPlayTimes();
    }

    @Override
    public Player getWiningPlayer() {
        return winingPlayer;
    }

    @Override
    public int getTargetAfterPotSuccess(Ball pottingBall, boolean isSnookerFreeBall) {
        return 0;
    }

    @Override
    public int get2ndNextTarget(Ball pottingBall, boolean isSnookerFreeBall) {
        return 0;
    }

    @Override
    public int getTargetAfterPotFailed() {
        return 0;
    }

    @Override
    protected void endMoveAndUpdate() {
        if (gameValues.hasSubRule(SubRule.RUSSIAN_FREE)) {
            updateScoreFree(newPotted);
        } else {
            System.err.println("Not implemented");
        }
    }

    private void updateScoreFree(Set<RussianBall> pottedBalls) {
        boolean baseFoul = checkStandardFouls(() -> 0);
        int score = 0;
        if (!baseFoul) {
            score = pottedBalls.size();
            currentPlayer.addScoreOfPotted(pottedBalls);
        } else {
            // 正常犯规了不扣分，等对手捡球的时候加分
            if (!pottedBalls.isEmpty()) {
                // 犯规了但是进球了，把分补给对方
                getAnotherPlayer().addScore(pottedBalls.size());
            }
        }
        

        if (getCueBall().isPotted()) {
            if (whiteFirstCollide == null || whiteFirstCollide.isPotted()) {
                setCueBall(findFirstAvailableBall());
            } else {
                setCueBall(whiteFirstCollide);
            }
        }
        
        if (player1.getScore() >= 8) {
            winingPlayer = player1;
            end();
        }
        if (player2.getScore() >= 8) {
            winingPlayer = player2;
            end();
        }

        if (score == 0) {
            switchPlayer();
        }
    }

    @Override
    public boolean cueBallSwitchable() {
        return gameValues.hasSubRule(SubRule.RUSSIAN_FREE) && !isBreaking();
    }

    @Override
    protected void updateTargetPotSuccess(boolean isSnookerFreeBall) {

    }

    @Override
    protected void updateTargetPotFailed() {

    }

    @Override
    public GamePlayStage getGamePlayStage(Ball predictedTargetBall, boolean printPlayStage) {
        Player cuing = getCuingPlayer();
        if (cuing.getScore() == 7) return GamePlayStage.THIS_BALL_WIN;
        else if (cuing.getScore() == 6) return GamePlayStage.NEXT_BALL_WIN;
        else if (cuing.getScore() <= 6) return GamePlayStage.NORMAL;
        else return GamePlayStage.NO_PRESSURE;
    }
}
