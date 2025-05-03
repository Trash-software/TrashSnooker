package trashsoftware.trashSnooker.core.ai;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.movement.WhitePrediction;
import trashsoftware.trashSnooker.core.phy.Phy;

import java.util.ArrayList;
import java.util.List;

public abstract class FinalChoice {

    public static class IntegratedAttackChoice extends FinalChoice implements Comparable<IntegratedAttackChoice> {

        public final boolean isPureAttack;
        final Game<?, ?> game;
        final AiCue.KickPriceCalculator kickPriceCalculator;
        final AttackParam attackParams;
        final List<AttackChoice> nextStepAttackChoices;  // Sorted from good to bad
        final WhitePrediction whitePrediction;
        final GamePlayStage stage;
        final Phy phy;
        protected double price;
        int nextStepTarget;
        CuePlayParams params;
        double priceOfKick = 0.0;

        // debug用的
        double positionErrorTolerance;
        double penalty;

        protected IntegratedAttackChoice(
                Game<?, ?> game,
                AttackParam attackParams,
                List<AttackChoice> nextStepAttackChoices,
                int nextStepTarget,
                CuePlayParams params,
                WhitePrediction whitePrediction,
                Phy phy,
                GamePlayStage stage,
                AiCue.KickPriceCalculator kickPriceCalculator
        ) {
            this.game = game;
            this.attackParams = attackParams;
            this.nextStepAttackChoices = nextStepAttackChoices;
            this.nextStepTarget = nextStepTarget;
            this.whitePrediction = whitePrediction;
            this.phy = phy;
            this.stage = stage;
            this.params = params;
            this.kickPriceCalculator = kickPriceCalculator;
            isPureAttack = true;

            generatePrice();
        }

        /**
         * 由defense转来的，连攻带防，但不能与纯进攻的一起比较，因为price完全是防守的price
         */
        protected IntegratedAttackChoice(Game<?, ?> game,
                                         AttackParam attackParams,
                                         int nextStepTarget,
                                         CuePlayParams params,
                                         Phy phy,
                                         GamePlayStage stage,
                                         double price) {
            this.game = game;
            this.attackParams = attackParams;
            this.nextStepAttackChoices = new ArrayList<>();
            this.whitePrediction = null;  // fixme: 可以有
            this.params = params;
            this.nextStepTarget = nextStepTarget;
            this.phy = phy;
            this.stage = stage;
            this.kickPriceCalculator = null;

            this.price = price;

            isPureAttack = false;
        }

        @Override
        public int compareTo(@NotNull FinalChoice.IntegratedAttackChoice o) {
            return normalCompareTo(o);
        }

        boolean betterThan(@Nullable IntegratedAttackChoice other) {
            if (other == null) return true;
            return normalCompareTo(other) < 0;
        }

        int normalCompareTo(IntegratedAttackChoice o2) {
            return -Double.compare(this.price, o2.price);
        }

        private void generatePrice() {
            price = attackParams.price;  // 这颗球本身的价值
            // 走位粗糙的人，下一颗权重低
            double mul = 0.5 *
                    attackParams.attackChoice.attackingPlayer.getPlayerPerson().getAiPlayStyle().position / 100;
            AttackChoice firstChoice = nextStepAttackChoices.isEmpty() ? null : nextStepAttackChoices.get(0);
            for (AttackChoice next : nextStepAttackChoices) {
                double positionPrice = next.defaultRef.price * mul;
                AttackChoice nextAttack = next.defaultRef.attackChoice;
                if (nextAttack instanceof AttackChoice.DirectAttackChoice dac && dac.angleRad < 0.075) {  // 4.3度的样子
                    positionPrice *= 0.75;
                }

                price += positionPrice;
                mul /= 4;
            }
//            if (whitePrediction.getSecondCollide() != null) price *= kickBallMul;
            if (kickPriceCalculator != null && whitePrediction.getSecondCollide() != null) {
                double dtFromCol = whitePrediction.whitePathLenBtw1st2ndCollision();
//                System.out.println(dtFromCol);
                priceOfKick = kickPriceCalculator.priceOfKick(whitePrediction.getSecondCollide(),
                        whitePrediction.getWhiteSpeedWhenHitSecondBall(),
                        dtFromCol);
                price *= priceOfKick;
            }

            if (whitePrediction.isWhiteHitsHoleArcs()) price *= AiCue.WHITE_HIT_CORNER_PENALTY;

            if (stage != GamePlayStage.NO_PRESSURE && firstChoice != null) {
                // 正常情况下少走点库
//                int cushions = whitePrediction.getWhiteCushionCountAfter();
//                double cushionDiv = Math.max(2, cushions) / 4.0 + 0.5;  // Math.max(x, cushions) / y + (1 - x / y)
//                price /= cushionDiv;
                
                boolean isDirect = attackParams.attackChoice instanceof AttackChoice.DoubleAttackChoice;

                // todo: 新的算法
                WhitePrediction[] tolerances = Analyzer.toleranceAnalysis(
                        game,
                        attackParams.attackChoice.attackingPlayer, 
                        params,
                        phy,
                        0.0,
                        true,
                        true,
                        !isDirect,
                        true,
                        false
                );
                
                double acceptablePotProb = firstChoice.defaultRef.potProb - 0.15;
                double tolerancePenalty = 1.0;
                for (WhitePrediction tor : tolerances) {
                    if (whitePrediction.getSecondCollide() != tor.getSecondCollide()) {
                        tolerancePenalty *= 2.0;
                    }
                    
                    double[] sp = tor.stopPoint();
//                    boolean canHit = game.pointToPointCanPassBall(sp[0], sp[1],
//                            firstChoice.collisionPos[0], firstChoice.collisionPos[1],
//                            game.getCueBall(), firstChoice.ball, 
//                            true, 
//                            true);
//                    if (!canHit) {
//                        tolerancePenalty *= 3.0;
//                    }
                    AttackChoice torChoice = Analyzer.choiceFromDifferentWhitePos(
                            game,
                            sp,
                            firstChoice
                    );
                    if (torChoice == null) {
                        tolerancePenalty *= 3.0;
                    } else {
                        if (torChoice.defaultRef.potProb < acceptablePotProb) {
                            tolerancePenalty += (acceptablePotProb - torChoice.defaultRef.potProb) * 10.0;
                        }
                    }
                }
                
                penalty = tolerancePenalty;
                price /= penalty;
            }
        }
    }

    public static class DefenseChoice extends FinalChoice implements Comparable<DefenseChoice> {

        final double penalty;
        final double stabilityScore;
        protected PlayerHand handSkill;
        protected Ball ball;
//        protected double snookerScore;
//        protected double opponentAttackChance;
        protected DefenseResult defenseResult;
//        protected double opponentAvailPrice;
        protected double price;  // price还是越大越好
        protected double[] cueDirectionUnitVector;  // selected

        CueParams cueParams;

        CuePlayParams cuePlayParams;
        WhitePrediction wp;
//        AttackChoice opponentEasiestChoice;

        boolean whiteCollidesOther;
        boolean targetCollidesOther;

        protected DefenseChoice(Ball ball,
                                double nativePrice,
                                DefenseResult defenseResult,
                                double penalty,
                                double stabilityScore,
                                double[] cueDirectionUnitVector,
                                CueParams cueParams,
                                WhitePrediction wp,
                                CuePlayParams cuePlayParams,
                                boolean whiteCollidesOther,
                                boolean targetCollidesOther) {
            this.ball = ball;
//            this.opponentAttackChance = opponentAttackChance;
            this.defenseResult = defenseResult;
            this.penalty = penalty;
            this.stabilityScore = stabilityScore;

//            this.collideOtherBall = collideOtherBall;
            this.cueDirectionUnitVector = cueDirectionUnitVector;
            this.cueParams = cueParams;
            this.cuePlayParams = cuePlayParams;
            this.wp = wp;
//            this.handSkill = handSkill;
//            this.opponentEasiestChoice = opponentEasiestChoice;

            this.whiteCollidesOther = whiteCollidesOther;
            this.targetCollidesOther = targetCollidesOther;

            generatePrice(nativePrice);
        }

        /**
         * 暴力开球用的
         */
        protected DefenseChoice(double[] cueDirectionUnitVector,
                                CueParams cueParams,
                                CuePlayParams cuePlayParams) {
            this(null,
                    1.0,
                    new DefenseResult(0, new ArrayList<>(), false),
                    1.0,
                    1.0,
                    cueDirectionUnitVector,
                    cueParams,
                    null,
                    cuePlayParams,
                    true,
                    true);
        }

        private void generatePrice(double nativePrice) {
//            double totalPen = penalty * stabilityScore;
//            this.price = snookerScore / totalPen 
//                    - opponentAttackChance * totalPen / nativePrice;
            this.price = nativePrice * 100
                    + stabilityScore
                    + defenseResult.snookerScore
                    - defenseResult.opponentAvailPrice
                    - defenseResult.opponentAttackPrice
                    - penalty;

//            if (wp != null && wp.isHitWallBeforeHitBall()) {
//                // 应该是在解斯诺克
//                this.price /= (wp.getDistanceTravelledBeforeCollision() / 1000);  // 不希望白球跑太远
//            }

        }

        @Override
        public int compareTo(@NotNull DefenseChoice o) {
            return -Double.compare(this.price, o.price);
        }

        @Override
        public String toString() {
            return "DefenseChoice{" +
                    "price=" + price +
                    ", penalty=" + penalty +
                    ", stabilityScore=" + stabilityScore +
//                    ", handSkill=" + handSkill +
                    ", ball=" + ball +
                    ", snookerScore=" + defenseResult.snookerScore +
                    ", opponentAttackChance=" + defenseResult.opponentAttackPrice +
                    ", opponentAvailPrice=" + defenseResult.opponentAvailPrice +
//                    ", cueDirectionUnitVector=" + Arrays.toString(cueDirectionUnitVector) +
//                    ", cueParams=" + cueParams +
//                    ", cuePlayParams=" + cuePlayParams +
//                    ", wp=" + wp +
//                    ", opponentEasiestChoice=" + opponentEasiestChoice +
//                    ", opponentChances=" + defenseResult + 
                    ", whiteCollidesOther=" + whiteCollidesOther +
                    ", targetCollidesOther=" + targetCollidesOther +
                    '}';
        }
    }
}
