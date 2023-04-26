package trashsoftware.trashSnooker.core.numberedGames.chineseEightBall;

import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.GameSettings;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;

import java.util.Arrays;
import java.util.Set;

public class LisEightGame extends ChineseEightBallGame {
    
    protected boolean swapped;
    
    public LisEightGame(EntireGame entireGame, GameSettings gameSettings, int frameIndex) {
        super(entireGame, gameSettings, frameIndex);
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
            else if (currentRemBalls == 2 && !swapped) return getAnotherPlayer(player).getBallRange();
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
//        boolean foul = false;
        if (!collidesWall && pottedBalls.isEmpty()) {
//            foul = true;
//            foulReason = strings.getString("noBallHitCushion");
            thisCueFoul.addFoul(strings.getString("noBallHitCushion"));
        }

//        System.out.println("fcc " + finishedCuesCount + " " + lastCueFoul);

        if (cueBall.isPotted()) {
            if (getEightBall().isPotted()) {  // 白球黑八一起进
                end();
                winingPlayer = getAnotherPlayer();
                return;
            }
//            foul = true;
            ballInHand = true;
//            foulReason = strings.getString("cueBallPot");
            thisCueFoul.addFoul(strings.getString("cueBallPot"));
        }

        if (whiteFirstCollide == null) {
//            foul = true;
//            foulReason = strings.getString("emptyCue");
            thisCueFoul.addFoul(strings.getString("emptyCue"), true);
        } else {
            if (currentTarget == FULL_BALL_REP) {
                if (whiteFirstCollide.getValue() == 8) {
//                    foul = true;
//                    foulReason = strings.getString("targetFullHitBlack");
                    thisCueFoul.addFoul(strings.getString("targetFullHitBlack"), true);
                } else if (!isFullBall(whiteFirstCollide)) {
//                    foul = true;
//                    foulReason = strings.getString("targetFullHitHalf");
                    thisCueFoul.addFoul(strings.getString("targetFullHitHalf"), true);
                }
            } else if (currentTarget == HALF_BALL_REP) {
                if (whiteFirstCollide.getValue() == 8) {
//                    foul = true;
//                    foulReason = strings.getString("targetHalfHitBlack");;
                    thisCueFoul.addFoul(strings.getString("targetHalfHitBlack"), true);
                } else if (!isHalfBall(whiteFirstCollide)) {
//                    foul = true;
//                    foulReason = strings.getString("targetHalfHitFull");;
                    thisCueFoul.addFoul(strings.getString("targetHalfHitFull"), true);
                }
            } else if (currentTarget == 8) {
                if (whiteFirstCollide.getValue() != 8) {
//                    foul = true;
//                    foulReason = strings.getString("targetBlackHitOther");
                    thisCueFoul.addFoul(strings.getString("targetBlackHitOther"), true);
                }
            }
        }

        if (thisCueFoul.isFoul() && !getEightBall().isPotted()) {
//            thisCueFoul = true;
//            cueBall.pot();
//            ballInHand = true;
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
//                            thisCueFoul = true;
//                            cueBall.pot();
//                            ballInHand = true;
                            switchPlayer();
                            System.out.println(thisCueFoul.getAllReasons());
                        } else {
                            currentPlayer.setBreakSuccess();
                            System.out.println("开球黑八不选球");
                        }
                    } else {
                        pickupBlackBall();
//                        thisCueFoul = true;
                        switchPlayer();
//                        switchPlayer();
//                        winingPlayer = getAnotherPlayer();
//                        end();
                    }
                } else {  // 误进黑八
                    pickupBlackBall();
//                    thisCueFoul = true;
                    switchPlayer();
//                    winingPlayer = getAnotherPlayer();
//                    end();
                }
                return;
            }
            if (currentTarget == NOT_SELECTED_REP) {  // 未选球
                if (isBreaking) {  // 开球进袋不算选球
                    currentPlayer.setBreakSuccess();
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
