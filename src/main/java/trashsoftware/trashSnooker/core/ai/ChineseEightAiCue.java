package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.CuePlayParams;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallPlayer;
import trashsoftware.trashSnooker.core.phy.Phy;

import java.util.Collection;
import java.util.List;

public class ChineseEightAiCue extends AiCue<ChineseEightBallGame, ChineseEightBallPlayer> {

    public ChineseEightAiCue(ChineseEightBallGame game, ChineseEightBallPlayer aiPlayer) {
        super(game, aiPlayer);
    }
    
    public static boolean isCenterBreak(ChineseEightBallPlayer player) {
        return player.getPlayerPerson().getAiPlayStyle().cebSideBreak;
    }

    @Override
    protected boolean requireHitCushion() {
        return true;
    }

    @Override
    protected DefenseChoice standardDefense() {
        return null;
    }

    @Override
    protected DefenseChoice breakCue(Phy phy) {
        // todo: 小力开球和大力开球
//        if (aiPlayer.getPlayerPerson().getControllablePowerPercentage() < 80.0) {
//            
//        } else {
//            
//        }
        return centerBreak();
    }
    
    private DefenseChoice centerBreak() {
        double dirX = game.getTable().breakPointX() - game.getCueBall().getX();
        double dirY = game.getGameValues().midY - game.getCueBall().getY();
        double[] unitXY = Algebra.unitVector(dirX, dirY);
        double selectedPower = aiPlayer.getPlayerPerson().getMaxPowerPercentage();
        CuePlayParams cpp = CuePlayParams.makeIdealParams(
                unitXY[0],
                unitXY[1],
                0.0,
                0.0,
                0.0,
                selectedPowerToActualPower(selectedPower, 0, 0, 
                        aiPlayer.getPlayerPerson().handBody.getPrimary())
        );
        return new DefenseChoice(unitXY, selectedPower, 0.0, cpp, 
                aiPlayer.getPlayerPerson().handBody.getPrimary());
    }

    @Override
    public AiCueResult makeCue(Phy phy) {
        if (game.getCurrentTarget() == ChineseEightBallGame.NOT_SELECTED_REP) {
            AiCueResult selection = selectBallCue(phy);
            if (selection != null) return selection;
        }
        return regularCueDecision(phy);
    }
    
    private double ballAlivePrice(Ball ball) {
        List<double[][]> dirHolePoints = game.directionsToAccessibleHoles(ball);
        double price = 0.0;
        final double diameter = game.getGameValues().ballDiameter;
        OUT_LOOP:
        for (double[][] dirHolePoint : dirHolePoints) {
            for (Ball other : game.getAllBalls()) {
                if (ball != other && !other.isPotted() && !other.isWhite()) {
                    double obstaclePotPointDt = 
                            Math.hypot(other.getX() - dirHolePoint[2][0], other.getY() - dirHolePoint[2][1]);
                    if (obstaclePotPointDt <= diameter) {
                        continue OUT_LOOP;
                    }
                }
            }
            double potDifficulty = AiCue.AttackChoice.holeDifficulty(
                    game,
                    dirHolePoint[1][0] == game.getGameValues().midX,
                    dirHolePoint[0]
            ) * Math.hypot(ball.getX() - dirHolePoint[1][0], ball.getY() - dirHolePoint[1][1]);
            price += 10000.0 / potDifficulty;
        }
        return price;
    }
    
    private double priceOfSet(Collection<Ball> balls) {
        double price = 0.0;
        for (Ball ball : balls) {
            price += ballAlivePrice(ball);
        }
        return price;
    }
    
    private AiCueResult selectBallCue(Phy phy) {
        List<Ball> fullBalls = game.getAllLegalBalls(ChineseEightBallGame.FULL_BALL_REP, false);
        List<Ball> halfBalls = game.getAllLegalBalls(ChineseEightBallGame.HALF_BALL_REP, false);
        double fullPrice = priceOfSet(fullBalls);
        double halfPrice = priceOfSet(halfBalls);
        System.out.printf("Selecting ball. Full price: %f, half price: %f\n", fullPrice, halfPrice);
        
        double[] whitePos = new double[]{game.getCueBall().getX(), game.getCueBall().getY()};
        List<AttackChoice> fullChoices = getAttackChoices(
                game,
                ChineseEightBallGame.FULL_BALL_REP,
                aiPlayer,
                null,
                fullBalls,
                whitePos,
                false,
                false
        );
        List<AttackChoice> halfChoices = getAttackChoices(
                game,
                ChineseEightBallGame.HALF_BALL_REP,
                aiPlayer,
                null,
                halfBalls,
                whitePos,
                false,
                false
        );
        
        IntegratedAttackChoice fullAttack = attackGivenChoices(fullChoices, phy);
        IntegratedAttackChoice halfAttack = attackGivenChoices(halfChoices, phy);
        
        if (fullAttack == null) {
            if (halfAttack != null) {
                return makeAttackCue(halfAttack);
            } else {
                return null;
            }
        } else {
            if (halfAttack == null) {
                return makeAttackCue(fullAttack);
            } else {
                double fullTotalPrice = fullPrice * fullAttack.price;
                double halfTotalPrice = halfPrice * halfAttack.price;
                if (fullTotalPrice > halfTotalPrice) {
                    if (fullAttack.price < halfAttack.price) {
                        System.out.println("全球比半球好，所以要强行进攻全球");
                    }
                    return makeAttackCue(fullAttack);
                } else {
                    if (fullAttack.price > halfAttack.price) {
                        System.out.println("半比全球好，所以要强行进攻半球");
                    }
                    return makeAttackCue(halfAttack);
                }
            }
        }
    }
}
