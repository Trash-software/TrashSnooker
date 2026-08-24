package trashsoftware.trashSnooker.core.ai;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;
import trashsoftware.trashSnooker.core.movement.WhitePrediction;
import trashsoftware.trashSnooker.core.person.PlayerHand;
import trashsoftware.trashSnooker.core.person.PlayerPerson;
import trashsoftware.trashSnooker.core.phy.Phy;

import java.util.ArrayList;
import java.util.List;

public abstract class FinalChoice {

    final WhitePrediction wp;
    WhitePrediction[] tolerances;
    
    FinalChoice(WhitePrediction wp) {
        this.wp = wp;
    }

    public WhitePrediction[] getTolerances() {
        return tolerances;
    }

    public List<double[]> getWhiteStopRange() {
        if (tolerances == null) return List.of();
        else {
            List<double[]> result = new ArrayList<>();
            for (WhitePrediction tor : tolerances) {
                result.add(tor.stopPoint());
            }
            return result;
        }
    }
    
    public List<double[]> getTargetStopRange() {
        if (tolerances == null) return List.of();
        else {
            List<double[]> result = new ArrayList<>();
            for (WhitePrediction tor : tolerances) {
                double[] tarStop = tor.getFirstBallStopPoint();
                if (tarStop != null) {
                    result.add(tarStop);
                }
            }
            return result;
        }
    }

    public static class IntegratedAttackChoice extends FinalChoice implements Comparable<IntegratedAttackChoice> {

        public final boolean isPureAttack;
        public final boolean isDoubleAttack;
        final Game<?, ?> game;
        final AiCue.KickPriceCalculator kickPriceCalculator;
        final AttackParam attackParams;
        final List<AttackChoice> nextStepAttackChoices;  // Sorted from good to bad
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
                WhitePrediction wp,
                Phy phy,
                GamePlayStage stage,
                AiCue.KickPriceCalculator kickPriceCalculator,
                boolean isDoubleAttack
        ) {
            super(wp);
            
            this.game = game;
            this.attackParams = attackParams;
            this.nextStepAttackChoices = nextStepAttackChoices;
            this.nextStepTarget = nextStepTarget;
            this.phy = phy;
            this.stage = stage;
            this.params = params;
            this.kickPriceCalculator = kickPriceCalculator;
            isPureAttack = true;
            this.isDoubleAttack = isDoubleAttack;

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
                                         double price,
                                         boolean isDoubleAttack) {
            super(null);  // fixme: 可以有
            
            this.game = game;
            this.attackParams = attackParams;
            this.nextStepAttackChoices = new ArrayList<>();
            this.params = params;
            this.nextStepTarget = nextStepTarget;
            this.phy = phy;
            this.stage = stage;
            this.kickPriceCalculator = null;
            this.isDoubleAttack = isDoubleAttack;

            this.price = price;

            isPureAttack = false;
        }

        @Override
        public int compareTo(@NotNull FinalChoice.IntegratedAttackChoice o) {
            return normalCompareTo(o);
        }

        public AttackParam getAttackParams() {
            return attackParams;
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
            double playerPositionMul =
                    attackParams.attackChoice.attackingPlayer.getPlayerPerson().getAiPlayStyle().position / 100;
            double mul = 0.5 * playerPositionMul;
            if (stage == GamePlayStage.THIS_BALL_WIN || stage == GamePlayStage.ENHANCE_WIN) {
                mul *= 0.75;
            }
            
            AttackChoice firstChoice = nextStepAttackChoices.isEmpty() ? null : nextStepAttackChoices.getFirst();

            for (int i = 0; i < nextStepAttackChoices.size(); i++) {
                AttackChoice next = nextStepAttackChoices.get(i);
                double positionPrice = next.defaultRef.price * mul;
                AttackChoice nextAttack = next.defaultRef.attackChoice;
                if (nextAttack instanceof AttackChoice.DirectAttackChoice dac && dac.angleRad < 0.075) {  // 4.3度的样子
                    positionPrice *= 0.75;
                }
                if (i == 0) {
                    // 考虑下下步
                    if (nextAttack.whiteNaturalExitDirection == null) {
                        System.err.println("White natural exit direction is null.");
                    } else {

                        int nextNextTar = game.get2ndNextTarget(attackParams.attackChoice.ball, game.isDoingSnookerFreeBll());
                        if (nextNextTar != Game.END_REP) {
                            // todo: 每次都算一遍，可能有优化空间
                            double[] targetsBarycenter = game.targetsBarycenter(nextNextTar, attackParams.attackChoice.ball);
                            if (targetsBarycenter == null) {
                                System.err.println("Cannot find next next when there should be.");
                            } else {

                                double dt1 = Algebra.distanceToPoint(nextAttack.collisionPos, targetsBarycenter);
                                double dt2 = Algebra.distanceToPoint(
                                        Algebra.vectorAdd(nextAttack.collisionPos, nextAttack.whiteNaturalExitDirection),
                                        targetsBarycenter);
                                if (dt2 > dt1) {
                                    // 分离角是远离主要目标球的方向的
                                    TableMetrics metrics = game.getGameValues().table;
                                    double noPenaltyDt = metrics.maxLength / 10;
                                    double maxPenaltyDt = metrics.maxLength / 4;
                                    if (nextAttack.whiteNaturalExitCushionDistance > noPenaltyDt) {
                                        double penalty = Math.min(1, nextAttack.whiteNaturalExitCushionDistance / maxPenaltyDt);
                                        penalty *= playerPositionMul;
                                        if (penalty < 0 || penalty > 1) {
                                            System.err.println("1321983617i6fgvbsdvjhcbd");
                                        }
                                        positionPrice *= (1 - penalty);
                                    }
                                }
                            }
                        }
                    }
                }

                price += positionPrice;
                mul /= 4;
            }
//            if (whitePrediction.getSecondCollide() != null) price *= kickBallMul;
            if (kickPriceCalculator != null && wp.getSecondCollide() != null) {
                double dtFromCol = wp.whitePathLenBtw1st2ndCollision();
//                System.out.println(dtFromCol);
                priceOfKick = kickPriceCalculator.priceOfKick(wp.getSecondCollide(),
                        wp.getWhiteSpeedWhenHitSecondBall(),
                        dtFromCol,
                        Algebra.unitVector(wp.getWhiteVelocityWhenHitSecondBall()));
//                System.out.println("Kick price: " + priceOfKick);
                price *= priceOfKick;
            }

            if (wp.isWhiteHitsHoleArcs()) price *= AiCue.WHITE_HIT_CORNER_PENALTY;

            if (stage != GamePlayStage.NO_PRESSURE && firstChoice != null) {
                // 正常情况下少走点库
//                int cushions = whitePrediction.getWhiteCushionCountAfter();
//                double cushionDiv = Math.max(2, cushions) / 4.0 + 0.5;  // Math.max(x, cushions) / y + (1 - x / y)
//                price /= cushionDiv;

                boolean isDirect = attackParams.attackChoice instanceof AttackChoice.DoubleAttackChoice;

                // todo: 新的算法
                tolerances = Analyzer.toleranceAnalysis(
                        game,
                        attackParams.attackChoice.attackingPlayer,
                        params,
                        phy,
                        0.0,
                        true,
                        true,
                        !isDirect,
                        true,
                        1.0
                );

                double acceptablePotProb = firstChoice.defaultRef.potProb - 0.2;
                double tolerancePenalty = 1.0;
                for (WhitePrediction tor : tolerances) {
                    if (wp.getSecondCollide() != tor.getSecondCollide()) {
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
        @Nullable
        protected DefenseResult defenseResult;
        //        protected double opponentAvailPrice;
        protected double price;  // price还是越大越好
        protected double[] cueDirectionUnitVector;  // selected

        CueParams cueParams;

        CuePlayParams cuePlayParams;
//        AttackChoice opponentEasiestChoice;

        boolean whiteCollidesOther;
        boolean targetCollidesOther;
        boolean defensiveAttack;

        protected DefenseChoice(Ball ball,
                                double nativePrice,
                                @Nullable DefenseResult defenseResult,
                                double penalty,
                                double stabilityScore,
                                double[] cueDirectionUnitVector,
                                CueParams cueParams,
                                WhitePrediction wp,
                                CuePlayParams cuePlayParams,
                                boolean whiteCollidesOther,
                                boolean targetCollidesOther,
                                boolean defensiveAttack) {
            super(wp);
            
            this.ball = ball;
//            this.opponentAttackChance = opponentAttackChance;
            this.defenseResult = defenseResult;
            this.penalty = penalty;
            this.stabilityScore = stabilityScore;

//            this.collideOtherBall = collideOtherBall;
            this.cueDirectionUnitVector = cueDirectionUnitVector;
            this.cueParams = cueParams;
            this.cuePlayParams = cuePlayParams;
//            this.handSkill = handSkill;
//            this.opponentEasiestChoice = opponentEasiestChoice;

            this.whiteCollidesOther = whiteCollidesOther;
            this.targetCollidesOther = targetCollidesOther;
            this.defensiveAttack = defensiveAttack;

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
                    true,
                    false);
        }

        private void generatePrice(double nativePrice) {
//            double totalPen = penalty * stabilityScore;
//            this.price = snookerScore / totalPen 
//                    - opponentAttackChance * totalPen / nativePrice;
            double drPrice = defenseResult == null ? 0 :
                    (defenseResult.snookerScore
                            - defenseResult.opponentAvailPrice
                            - defenseResult.opponentAttackPrice);
            this.price = nativePrice * 100
                    + stabilityScore
                    + drPrice
                    - penalty;

//            System.out.println("prices: " + price + " " + penalty + " " + stabilityScore);

//            if (wp != null && wp.isHitWallBeforeHitBall()) {
//                // 应该是在解斯诺克
//                this.price /= (wp.getDistanceTravelledBeforeCollision() / 1000);  // 不希望白球跑太远
//            }

        }

        public boolean opponentCanPureAttack(PlayerPerson opponent) {
            double pureAttackThresh = Analyzer.attackProbThreshold(AiCue.PURE_ATTACK_PROB, opponent);
            return defenseResult == null || defenseResult.opponentPotProb > pureAttackThresh;
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
                    ", snookerScore=" + (defenseResult == null ? "null" : defenseResult.snookerScore) +
                    ", opponentAttackChance=" + (defenseResult == null ? "null" : defenseResult.opponentAttackPrice) +
                    ", opponentAvailPrice=" + (defenseResult == null ? "null" : defenseResult.opponentAvailPrice) +
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
