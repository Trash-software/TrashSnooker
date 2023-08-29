package trashsoftware.trashSnooker.core.numberedGames.chineseEightBall;

import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.GameSettings;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;

import java.util.Set;

public class LisEightGame extends ChineseEightBallGame {

    protected boolean swapped;

    public LisEightGame(EntireGame entireGame, GameSettings gameSettings, GameValues gameValues, int frameIndex) {
        super(entireGame, gameSettings, gameValues, frameIndex);
    }

    @Override
    public boolean isLegalBall(Ball ball, int targetRep, boolean isSnookerFreeBall,
                               boolean isInLineHandBall) {
        return super.isLegalBall(ball, targetRep, isSnookerFreeBall, isInLineHandBall);
    }

    @Override
    public int getTargetAfterPotSuccess(Ball pottingBall, boolean isSnookerFreeBall) {
        ChineseEightBallPlayer player = getCuingPlayer();
        if (player.getBallRange() == FULL_BALL_REP || player.getBallRange() == HALF_BALL_REP) {
            int currentRemBalls = getRemRangedBallOnTable(player.getBallRange());  // 还没打掉现在这颗
            if (currentRemBalls > 2) return player.getBallRange();
            else if (currentRemBalls == 2 && !swapped)
                return getAnotherPlayer(player).getBallRange();
            else if (pottingBall.getValue() == 8) return END_REP;
            else return 8;
        }

        return super.getTargetAfterPotSuccess(pottingBall, isSnookerFreeBall);
    }

    @Override
    protected boolean canPlaceWhiteInTable(double x, double y) {
        double range = gameValues.ball.ballDiameter * 2;
        return (Algebra.distanceToPoint(x, y, getTable().breakLineX(), gameValues.table.midY) < range ||
                Algebra.distanceToPoint(x, y, getTable().firstBallPlacementX(), gameValues.table.midY) < range)
                && !isOccupied(x, y);
    }

    @Override
    public boolean isInLineHandBall() {
        return false;
    }

    @Override
    public boolean isInLineHandBallForAi() {
        return false;
    }

    @Override
    protected void endMoveAndUpdate() {
        updateScore(newPotted);

        boolean willSwitch = false;
        if (!swapped) {
            int remFull = getRemRangedBallOnTable(FULL_BALL_REP);
            int remHalf = getRemRangedBallOnTable(HALF_BALL_REP);
            if (remFull <= 1 || remHalf <= 1) {
                willSwitch = true;
            }
        }

        if (willSwitch) {
            dealSwapTarget();
        }
    }

    private void updateScore(Set<PoolBall> pottedBalls) {
        boolean baseFoul = checkStandardFouls(() -> 1);

        if (baseFoul && getEightBall().isPotted()) {  // 白球黑八一起进
            end();
            winingPlayer = getAnotherPlayer();
            return;
        }

        if (!baseFoul) {
            if (currentTarget == FULL_BALL_REP) {
                if (whiteFirstCollide.getValue() == 8) {
                    thisCueFoul.addFoul(strings.getString("targetFullHitBlack"), true);
                } else if (!isFullBall(whiteFirstCollide)) {
                    thisCueFoul.addFoul(strings.getString("targetFullHitHalf"), true);
                }
            } else if (currentTarget == HALF_BALL_REP) {
                if (whiteFirstCollide.getValue() == 8) {
                    thisCueFoul.addFoul(strings.getString("targetHalfHitBlack"), true);
                } else if (!isHalfBall(whiteFirstCollide)) {
                    thisCueFoul.addFoul(strings.getString("targetHalfHitFull"), true);
                }
            } else if (currentTarget == 8) {
                if (whiteFirstCollide.getValue() != 8) {
                    thisCueFoul.addFoul(strings.getString("targetBlackHitOther"), true);
                }
            }
        }

        if (thisCueFoul.isFoul() && !getEightBall().isPotted()) {
            switchPlayer();

            currentTarget = getTargetOfPlayer(currentPlayer);  // 在switchPlayer之后
            System.out.println(thisCueFoul.getAllReasons());

            return;
        }

        if (!pottedBalls.isEmpty()) {
            if (pottedBalls.contains(getEightBall())) {
                if (currentTarget == 8) {
                    winingPlayer = currentPlayer;
                    end();
                } else if (currentTarget == NOT_SELECTED_REP) {
                    if (isBreaking) {
                        pickupBlackBall();
                        if (thisCueFoul.isFoul()) {
                            switchPlayer();
                            System.out.println(thisCueFoul.getAllReasons());
                        } else {
                            currentPlayer.setBreakSuccess(pottedBalls.size());
                            System.out.println("开球黑八不选球");
                        }
                    } else {
                        pickupBlackBall();
                        switchPlayer();
                    }
                } else {  // 误进黑八
                    pickupBlackBall();
                    switchPlayer();
                }
                return;
            }
            if (currentTarget == NOT_SELECTED_REP) {  // 未选球
                if (isBreaking) {  // 开球进袋不算选球
                    currentPlayer.setBreakSuccess(pottedBalls.size());
                    System.out.println("开球进球不选球");
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
                int sucCount = 0;
                if (currentTarget == FULL_BALL_REP) {
                    sucCount = countFullBalls(pottedBalls);
                    if (sucCount > 0) {
                        lastPotSuccess = true;
                    }
                    currentPlayer.correctPotBalls(fullBallsOf(pottedBalls));
                } else if (currentTarget == HALF_BALL_REP) {
                    sucCount = countHalfBalls(pottedBalls);
                    if (sucCount > 0) {
                        lastPotSuccess = true;
                    }
                    currentPlayer.correctPotBalls(halfBallsOf(pottedBalls));
                }
                // 触发成就，且开球不算
                if (sucCount == 2) {
                    AchManager.getInstance().addAchievement(Achievement.POT_TWO_LEGAL, getCuingIgp());
                } else if (sucCount >= 3) {
                    AchManager.getInstance().addAchievement(Achievement.POT_THREE_LEGAL, getCuingIgp());
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

    private void dealSwapTarget() {
        if (player1.getBallRange() == FULL_BALL_REP) {
            player1.setBallRange(HALF_BALL_REP);
            player2.setBallRange(FULL_BALL_REP);
            currentTarget = getTargetOfPlayer(currentPlayer);
            swapped = true;
        } else if (player1.getBallRange() == HALF_BALL_REP) {
            player1.setBallRange(FULL_BALL_REP);
            player2.setBallRange(HALF_BALL_REP);
            currentTarget = getTargetOfPlayer(currentPlayer);
            swapped = true;
        }
    }

    @Override
    public void switchPlayer() {
        super.switchPlayer();

        currentTarget = getTargetOfPlayer(currentPlayer);
    }
}
