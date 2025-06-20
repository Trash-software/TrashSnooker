package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallPlayer;
import trashsoftware.trashSnooker.core.person.CuePlayerHand;
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
            selfBallAlivePrices.put(ball, ballAlivePrice(game, ball));
        }
        Ball eight = game.getBallByValue(8);
        selfBallAlivePrices.put(eight, ballAlivePrice(game, eight));
    }

    public static boolean isCenterBreak(ChineseEightBallPlayer player) {
        return !player.getPlayerPerson().getAiPlayStyle().cebSideBreak;
    }

    @Override
    protected FinalChoice.DefenseChoice standardDefense() {
        return null;
    }

    @Override
    protected boolean currentMustAttack() {
        if (game.getRemainingBallsOfPlayer(aiPlayer) <= 2 &&
                game.getRemainingBallsOfPlayer(game.getAnotherPlayer(aiPlayer)) > 2) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean supportAttackWithDefense(int targetRep) {
        return true;
    }

    @Override
    protected FinalChoice.DefenseChoice breakCue(Phy phy) {
        // todo: 小力开球和大力开球
//        if (aiPlayer.getPlayerPerson().getControllablePowerPercentage() < 80.0) {
//            
//        } else {
//            
//        }
        return centerBreak();
    }

    private FinalChoice.DefenseChoice centerBreak() {
        double dirX = game.getTable().firstBallPlacementX() - game.getCueBall().getX();
        double dirY = game.getGameValues().table.midY - game.getCueBall().getY();
        double[] unitXY = Algebra.unitVector(dirX, dirY);
        double selectedPower = aiPlayer.getPlayerPerson().getPrimaryHand().getMaxPowerPercentage();
        CueParams cueParams = CueParams.createBySelected(
                selectedPower,
                0.0,
                0.0,
                8.0,
                game,
                aiPlayer.getInGamePlayer(),
                CuePlayerHand.makeDefault(aiPlayer.getInGamePlayer())
        );
        CuePlayParams cpp = CuePlayParams.makeIdealParams(
                unitXY[0],
                unitXY[1],
                cueParams
        );
        return new FinalChoice.DefenseChoice(unitXY, cueParams, cpp);
    }

    @Override
    protected KickPriceCalculator kickPriceCalculator() {
        return (kickedBall, kickSpeed, dtFromFirst) -> {
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
        };
    }

    @Override
    public AiCueResult makeCue(Phy phy) {
        if (!game.isBreaking() && game.getCurrentTarget() == ChineseEightBallGame.NOT_SELECTED_REP) {
            AiCueResult selection = selectBallCue(phy);
            if (selection != null) return selection;
        }
        return regularCueDecision(phy);
    }

    public static double avgPriceOfSet(Game<?, ?> game, Collection<Ball> balls) {
        double price = 0.0;
        for (Ball ball : balls) {
            price += ballAlivePrice(game, ball);
        }
        return price / balls.size();
    }

    private AiCueResult selectBallCue(Phy phy) {
        // todo: 这个price其实反过来，用difficulty更好
        // todo: 因为球数也会影响，哪怕是平均，都没有difficulty准确
        List<Ball> fullBalls = game.getAllLegalBalls(ChineseEightBallGame.FULL_BALL_REP, false, false);
        List<Ball> halfBalls = game.getAllLegalBalls(ChineseEightBallGame.HALF_BALL_REP, false, false);
        double fullPrice = avgPriceOfSet(game, fullBalls);
        double halfPrice = avgPriceOfSet(game, halfBalls);
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
        List<AttackChoice> fullChoices = Analyzer.getAttackChoices(
                game,
                ChineseEightBallGame.FULL_BALL_REP,
                aiPlayer,
                null,
                fullLegals,
                whitePos,
                false,
                true
        );
        List<AttackChoice> halfChoices = Analyzer.getAttackChoices(
                game,
                ChineseEightBallGame.HALF_BALL_REP,
                aiPlayer,
                null,
                halfLegals,
                whitePos,
                false,
                true
        );

        FinalChoice.IntegratedAttackChoice fullAttack = attackGivenChoices(fullChoices, phy, false);
        FinalChoice.IntegratedAttackChoice halfAttack = attackGivenChoices(halfChoices, phy, false);

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
