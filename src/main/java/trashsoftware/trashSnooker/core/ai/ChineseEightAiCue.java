package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.CuePlayParams;
import trashsoftware.trashSnooker.core.Values;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallPlayer;
import trashsoftware.trashSnooker.core.phy.Phy;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChineseEightAiCue extends AiCue<ChineseEightBallGame, ChineseEightBallPlayer> {
    
    private final Map<Ball, Double> selfBallAlivePrices = new HashMap<>();

    public ChineseEightAiCue(ChineseEightBallGame game, ChineseEightBallPlayer aiPlayer) {
        super(game, aiPlayer);
        
        makeAlivePriceMap();
    }
    
    private void makeAlivePriceMap() {
        int range = aiPlayer.getBallRange();
        if (range == ChineseEightBallGame.NOT_SELECTED_REP) return;
        for (Ball ball : game.getAllLegalBalls(range, false, 
                false  // 是不是死球和线不线内无关
        )) {
            selfBallAlivePrices.put(ball, ballAlivePrice(ball));
        }
        Ball eight = game.getBallByValue(8);
        selfBallAlivePrices.put(eight, ballAlivePrice(eight));
    }
    
    public static boolean isCenterBreak(ChineseEightBallPlayer player) {
        return !player.getPlayerPerson().getAiPlayStyle().cebSideBreak;
    }

    @Override
    protected DefenseChoice standardDefense() {
        return null;
    }

    @Override
    protected boolean supportAttackWithDefense(int targetRep) {
        return true;
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
        double dirX = game.getTable().firstBallPlacementX() - game.getCueBall().getX();
        double dirY = game.getGameValues().table.midY - game.getCueBall().getY();
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
    protected double priceOfKick(Ball kickedBall, double kickSpeed, double dtFromFirst) {
        Double alivePrice = selfBallAlivePrices.get(kickedBall);
        if (alivePrice == null) return kickUselessBallPrice(dtFromFirst);

        double speedThreshold = Values.BEST_KICK_SPEED;
        double speedMul;
        if (kickSpeed > speedThreshold * 2) speedMul = 1.5;
        else if (kickSpeed > speedThreshold) speedMul = 1.0;
        else speedMul = 0.5;
        
        if (alivePrice == 0) return 2.0 * speedMul;
        double kickPriority = 20.0 / alivePrice;  // alive price 本身大，那就不k最好
        
        return Math.max(0.5, speedMul * Math.min(2.0, kickPriority));
    }

    @Override
    public AiCueResult makeCue(Phy phy) {
        if (game.getCurrentTarget() == ChineseEightBallGame.NOT_SELECTED_REP) {
            AiCueResult selection = selectBallCue(phy);
            if (selection != null) return selection;
        }
        return regularCueDecision(phy);
    }
    
    private double priceOfSet(Collection<Ball> balls) {
        double price = 0.0;
        for (Ball ball : balls) {
            price += ballAlivePrice(ball);
        }
        return price;
    }
    
    private AiCueResult selectBallCue(Phy phy) {
        List<Ball> fullBalls = game.getAllLegalBalls(ChineseEightBallGame.FULL_BALL_REP, false, false);
        List<Ball> halfBalls = game.getAllLegalBalls(ChineseEightBallGame.HALF_BALL_REP, false, false);
        double fullPrice = priceOfSet(fullBalls);
        double halfPrice = priceOfSet(halfBalls);
        System.out.printf("Selecting ball. Full price: %f, half price: %f\n", fullPrice, halfPrice);

        boolean isInLineHandBall = game.isInLineHandBallForAi();
        System.out.println("Is ai in line: " + isInLineHandBall);
        List<Ball> fullLegals;
        List<Ball> halfLegals;
        if (isInLineHandBall) {
            fullLegals = game.getAllLegalBalls(ChineseEightBallGame.FULL_BALL_REP, false, true);
            halfLegals = game.getAllLegalBalls(ChineseEightBallGame.HALF_BALL_REP, false, true);
        } else {
            fullLegals = fullBalls;
            halfLegals = halfBalls;
        }
        
        double[] whitePos = new double[]{game.getCueBall().getX(), game.getCueBall().getY()};
        List<AttackChoice> fullChoices = getAttackChoices(
                game,
                ChineseEightBallGame.FULL_BALL_REP,
                aiPlayer,
                null,
                fullLegals,
                whitePos,
                false
        );
        List<AttackChoice> halfChoices = getAttackChoices(
                game,
                ChineseEightBallGame.HALF_BALL_REP,
                aiPlayer,
                null,
                halfLegals,
                whitePos,
                false
        );
        
        IntegratedAttackChoice fullAttack = attackGivenChoices(fullChoices, phy, false);
        IntegratedAttackChoice halfAttack = attackGivenChoices(halfChoices, phy, false);
        
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
