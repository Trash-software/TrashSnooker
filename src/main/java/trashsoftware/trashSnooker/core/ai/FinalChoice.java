package trashsoftware.trashSnooker.core.ai;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.movement.WhitePrediction;

import java.util.ArrayList;
import java.util.List;

public abstract class FinalChoice {

    public static class IntegratedAttackChoice extends FinalChoice implements Comparable<IntegratedAttackChoice> {

        public final boolean isPureAttack;
        final Game<?, ?> game;
        final AiCue.KickPriceCalculator kickPriceCalculator;
        final AiCue.AttackParam attackParams;
        final List<AttackChoice> nextStepAttackChoices;  // Sorted from good to bad
        final WhitePrediction whitePrediction;
        final GamePlayStage stage;
        protected double price;
        int nextStepTarget;
        CuePlayParams params;
        double priceOfKick = 0.0;

        // debug用的
        double positionErrorTolerance;
        double penalty;

        protected IntegratedAttackChoice(
                Game<?, ?> game,
                AiCue.AttackParam attackParams,
                List<AttackChoice> nextStepAttackChoices,
                int nextStepTarget,
                CuePlayParams params,
                WhitePrediction whitePrediction,
                GamePlayStage stage,
                AiCue.KickPriceCalculator kickPriceCalculator
        ) {
            this.game = game;
            this.attackParams = attackParams;
            this.nextStepAttackChoices = nextStepAttackChoices;
            this.nextStepTarget = nextStepTarget;
            this.whitePrediction = whitePrediction;
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
                                         AiCue.AttackParam attackParams,
                                         int nextStepTarget,
                                         CuePlayParams params,
                                         GamePlayStage stage,
                                         double price) {
            this.game = game;
            this.attackParams = attackParams;
            this.nextStepAttackChoices = new ArrayList<>();
            this.whitePrediction = null;  // fixme: 可以有
            this.params = params;
            this.nextStepTarget = nextStepTarget;
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

                // 根据走位目标位置的容错空间，结合白球的行进距离，对长距离低容错的路线予以惩罚
                if (firstChoice instanceof AttackChoice.DirectAttackChoice direct) {
                    double[] posTolerance = direct.leftRightTolerance();
                    positionErrorTolerance = Math.min(posTolerance[0], posTolerance[1]);
                } else {
                    positionErrorTolerance = game.getGameValues().ball.ballDiameter * 2;  // 随手输的
                }

                double dtTravel = whitePrediction.getDistanceTravelledAfterCollision();
                double ratio = dtTravel / positionErrorTolerance;
                double decisionWeight = 10.0;  // 权重

                if (Double.isInfinite(ratio) || Double.isNaN(ratio)) {
                    penalty = dtTravel / 100;
                    price /= 1 + penalty;
                } else if (ratio > decisionWeight) {
                    penalty = Math.pow((ratio - decisionWeight) / 5.0, 1.25) * 2.0;  // 随便编的
                    price /= 1 + penalty;
                }

                // 正常情况下少跑点
//                AiPlayStyle aps = aiPlayer.getPlayerPerson().getAiPlayStyle();
//                double divider = aps.likeShow * 50.0;
//                double dtTravel = whitePrediction.getDistanceTravelledAfterCollision();
//                double penalty = dtTravel < 1500 ? 0 : (dtTravel - 1500) / divider;
//                price /= 1 + penalty;
            }
        }
    }

    public static class DefenseChoice extends FinalChoice implements Comparable<DefenseChoice> {

        final double penalty;
        protected PlayerPerson.HandSkill handSkill;
        protected Ball ball;
        protected double snookerPrice;
        protected double opponentAttackPrice;
        protected double price;  // price还是越大越好
        protected double[] cueDirectionUnitVector;  // selected

        CueParams cueParams;

        CuePlayParams cuePlayParams;
        WhitePrediction wp;
        AttackChoice opponentEasiestChoice;

        boolean whiteCollidesOther;
        boolean targetCollidesOther;

        protected DefenseChoice(Ball ball,
                                double nativePrice,
                                double snookerPrice,
                                double opponentAttackPrice,
                                double penalty,
                                double[] cueDirectionUnitVector,
                                CueParams cueParams,
                                WhitePrediction wp,
                                CuePlayParams cuePlayParams,
                                AttackChoice opponentEasiestChoice,
                                boolean whiteCollidesOther,
                                boolean targetCollidesOther) {
            this.ball = ball;
            this.snookerPrice = snookerPrice;
            this.opponentAttackPrice = opponentAttackPrice;
            this.penalty = penalty;

//            this.collideOtherBall = collideOtherBall;
            this.cueDirectionUnitVector = cueDirectionUnitVector;
            this.cueParams = cueParams;
            this.cuePlayParams = cuePlayParams;
            this.wp = wp;
//            this.handSkill = handSkill;
            this.opponentEasiestChoice = opponentEasiestChoice;

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
                    0.0,
                    0.0,
                    0.0,
                    cueDirectionUnitVector,
                    cueParams,
                    null,
                    cuePlayParams,
                    null,
                    true,
                    true);
        }

        private void generatePrice(double nativePrice) {
            this.price = snookerPrice / penalty - opponentAttackPrice * penalty / nativePrice;

            if (wp != null && wp.isHitWallBeforeHitBall()) {
                // 应该是在解斯诺克
                this.price /= (wp.getDistanceTravelledBeforeCollision() / 1000);  // 不希望白球跑太远
            }

        }

        @Override
        public int compareTo(@NotNull DefenseChoice o) {
            return -Double.compare(this.price, o.price);
        }

        @Override
        public String toString() {
            return String.format("price %f, snk %f, oppo atk %f, pen %f, white col: %b, tar col: %b",
                    price,
                    snookerPrice,
                    opponentAttackPrice,
                    penalty,
                    whiteCollidesOther,
                    targetCollidesOther);
        }
    }
}
