package trashsoftware.trashSnooker.core.ai;

import org.jetbrains.annotations.NotNull;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.movement.WhitePrediction;

import java.util.*;

// todo: assume pickup, 大力K球奖励, 开球固定式, 袋角权重

public abstract class AiCue<G extends Game<? extends Ball, P>, P extends Player> {

    public static final double ATTACK_DIFFICULTY_THRESHOLD = 8000.0;
    public static final double NO_DIFFICULTY_ANGLE_RAD = 0.3;
    private static final double[] FRONT_BACK_SPIN_POINTS =
            {0.0, -0.27, 0.27, -0.54, 0.54, -0.81, 0.81};
    protected G game;
    protected P aiPlayer;

    public AiCue(G game, P aiPlayer) {
        this.game = game;
        this.aiPlayer = aiPlayer;
    }

    public abstract AiCueResult makeCue();

    /**
     * 开球
     */
    protected abstract DefenseChoice breakCue();

    protected double selectedPowerToActualPower(double selectedPower) {
        return selectedPower * aiPlayer.getInGamePlayer().getCurrentCue(game).powerMultiplier;
    }

    protected double actualPowerToSelectedPower(double actualPower) {
        return actualPower / aiPlayer.getInGamePlayer().getCurrentCue(game).powerMultiplier;
    }

    private IntegratedAttackChoice createIntAttackChoices(double selectedPower,
                                                          double selectedFrontBackSpin,
                                                          AttackChoice attackChoice,
                                                          GameValues values,
                                                          int nextTarget,
                                                          List<Ball> nextStepLegalBalls) {
//        System.out.print(selectedPower);

        CuePlayParams params = CuePlayParams.makeIdealParams(
                attackChoice.cueDirectionUnitVector[0],
                attackChoice.cueDirectionUnitVector[1],
                CuePlayParams.unitFrontBackSpin(selectedFrontBackSpin,
                        aiPlayer.getInGamePlayer(), game),
                0,
                5.0,
                selectedPowerToActualPower(selectedPower)
        );
        // 直接能打到的球，必不会在打到目标球之前碰库
        WhitePrediction wp = game.predictWhite(params, 0.0, true);
        if (wp.getFirstCollide() == null) {
            // 连球都碰不到，没吃饭？
//            System.out.println("too less");
            return null;
        }

        double targetCanMove = values.estimatedMoveDistance(wp.getBallInitSpeed());
        if (targetCanMove - values.ballDiameter * 1.5 <= attackChoice.targetHoleDistance) {
            // 确保球不会停在袋口
            // 如果小于，说明力量太轻或低杆太多，打不到
//            System.out.println("little less " + targetCanMove + ", " + attackChoice.targetHoleDistance);
            return null;
        }
        if (wp.willCueBallPot()) {
            // 进白球也太蠢了吧
            return null;
        }
        double[] whiteStopPos = wp.getWhitePath().get(wp.getWhitePath().size() - 1);
        List<AttackChoice> nextStepAttackChoices =
                getAttackChoices(nextTarget,
                        aiPlayer,
                        wp.getFirstCollide(),
                        nextStepLegalBalls,
                        whiteStopPos);
        return new IntegratedAttackChoice(
                attackChoice,
                nextStepAttackChoices,
                nextTarget,
                params,
                selectedPower,
                selectedFrontBackSpin,
                wp.getSecondCollide(),
                wp.getWhiteSpeedWhenHitSecondBall());
    }

    private IntegratedAttackChoice attack(AttackChoice choice, 
                                          int nextTarget, 
                                          List<Ball> nextStepLegalBalls) {
        double powerLimit = aiPlayer.getPlayerPerson().getControllablePowerPercentage();
        final double tick = 300.0 / aiPlayer.getPlayerPerson().getAiPlayStyle().position;

        GameValues values = game.getGameValues();

        double comfortablePower = powerLimit * 0.35;
        int maxIterations = (int) Math.round((powerLimit - comfortablePower) / tick) * 2;  // 注：仅适用于倍数<=0.5的情况

        List<IntegratedAttackChoice> choiceList = new ArrayList<>();
        for (double spin : FRONT_BACK_SPIN_POINTS) {
            int multiplier = 0;
            for (int i = 0; i < maxIterations; i++) {
                int flag = i % 2 == 0 ? 1 : -1;
                double selectedPower = comfortablePower + tick * multiplier * flag;
                if (selectedPower <= tick) continue;  // 同上一条注释：<=0.5
                IntegratedAttackChoice iac =
                        createIntAttackChoices(selectedPower, spin, choice, values,
                                nextTarget, nextStepLegalBalls);
                if (iac != null) choiceList.add(iac);
                if (flag == -1) multiplier++;
            }
        }
        if (choiceList.isEmpty()) return null;
        choiceList.sort(IntegratedAttackChoice::normalCompareTo);
        for (IntegratedAttackChoice iac : choiceList) {
            if (!iac.nextStepAttackChoices.isEmpty()) {
                return choiceList.get(0);
            }
        }
        choiceList.sort(IntegratedAttackChoice::compareToWhenNoAvailNextBall);
        return choiceList.get(0);
    }

    protected AiCueResult makeDefenseCue(DefenseChoice choice) {
        return new AiCueResult(aiPlayer.getPlayerPerson().getAiPlayStyle(),
                choice.cueDirectionUnitVector[0],
                choice.cueDirectionUnitVector[1],
                0,
                choice.selectedPower);
    }

    protected AiCueResult regularCueDecision() {
        if (game.isBreaking()) {
            DefenseChoice breakChoice = breakCue();
            if (breakChoice != null) return makeDefenseCue(breakChoice);
        }

        List<AttackChoice> attackAttackChoices = getCurrentAttackChoices();
        System.out.println("进攻机会:" + attackAttackChoices.size());
//        System.out.println(attackAttackChoices);
        if (!attackAttackChoices.isEmpty()) {
            double bestPrice = 0.0;
            IntegratedAttackChoice best = null;
            for (int i = 0; i < Math.min(2, attackAttackChoices.size()); i++) {
                AttackChoice choice = attackAttackChoices.get(i);
                int nextTargetIfThisSuccess = game.getTargetAfterPotSuccess(choice.ball,
                        game.isDoingSnookerFreeBll());
                List<Ball> nextStepLegalBalls =
                        game.getAllLegalBalls(nextTargetIfThisSuccess,
                                false);  // 这颗进了下一颗怎么可能是自由球
                IntegratedAttackChoice iac = attack(choice, nextTargetIfThisSuccess, nextStepLegalBalls);
                if (iac != null && iac.price > bestPrice) {
                    best = iac;
                    bestPrice = iac.price;
                }
            }
            if (best != null) {
                return new AiCueResult(aiPlayer.getPlayerPerson().getAiPlayStyle(),
                        best.attackChoice.cueDirectionUnitVector[0],
                        best.attackChoice.cueDirectionUnitVector[1],
                        best.selectedFrontBackSpin,
                        best.selectedPower);
            }
        }
        DefenseChoice defenseChoice = getBestDefenseChoice();
        if (defenseChoice != null) {
            return makeDefenseCue(defenseChoice);
        }
        return randomAngryCue();
    }

    private AiCueResult randomAngryCue() {
        System.out.println("Shit! No way to deal this!");
        Random random = new Random();
        double directionRad = random.nextDouble() * Math.PI * 2;
        double power = random.nextDouble() *
                (aiPlayer.getPlayerPerson().getMaxPowerPercentage() - 20.0) + 20.0;
        double[] directionVec = Algebra.unitVectorOfAngle(directionRad);
        return new AiCueResult(
                aiPlayer.getPlayerPerson().getAiPlayStyle(),
                directionVec[0],
                directionVec[1],
                0.0,
                power
        );
    }

    private List<AttackChoice> getAttackChoices(int attackTarget,
                                                Ball lastPottingBall,
                                                boolean isSnookerFreeBall,
                                                double[] whitePos) {
        return getAttackChoices(attackTarget,
                aiPlayer,
                lastPottingBall,
                game.getAllLegalBalls(attackTarget, isSnookerFreeBall),
                whitePos
        );
    }

    private List<AttackChoice> getAttackChoices(int attackTarget,
                                                Player attackingPlayer,
                                                Ball lastPottingBall,
                                                List<Ball> legalBalls,
                                                double[] whitePos) {
        List<AttackChoice> attackChoices = new ArrayList<>();
        for (Ball ball : legalBalls) {
            if (ball.isPotted() || ball == lastPottingBall) continue;  // todo: 潜在bug：斯诺克清彩阶段自由球
            List<double[][]> dirHoles = game.directionsToAccessibleHoles(ball);
            double[] ballPos = new double[]{ball.getX(), ball.getY()};

            for (double[][] dirHole : dirHoles) {
                double collisionPointX = ballPos[0] - game.getGameValues().ballDiameter * dirHole[0][0];
                double collisionPointY = ballPos[1] - game.getGameValues().ballDiameter * dirHole[0][1];

                if (game.pointToPointCanPassBall(whitePos[0], whitePos[1],
                        collisionPointX, collisionPointY, game.getCueBall(), ball)) {
                    // 从白球处看得到进球点
                    AttackChoice attackChoice = AttackChoice.createChoice(
                            game,
                            attackingPlayer,
                            whitePos, 
                            ball,
                            lastPottingBall,
                            attackTarget,
                            collisionPointX, collisionPointY,
                            dirHole
                    );
                    if (attackChoice != null &&
                            attackChoice.difficulty <
                                    ATTACK_DIFFICULTY_THRESHOLD *
                                            aiPlayer.getPlayerPerson().getAiPlayStyle().attackPrivilege /
                                            100.0) {
                        attackChoices.add(attackChoice);
                    }
                }
            }
        }
        Collections.sort(attackChoices);
        return attackChoices;
    }

    protected List<AttackChoice> getCurrentAttackChoices() {
        return getAttackChoices(game.getCurrentTarget(),
                null,
                game.isDoingSnookerFreeBll(),
                new double[]{game.getCueBall().getX(), game.getCueBall().getY()});
    }

    private DefenseChoice directDefense(List<Ball> legalBalls,
                                        double origDegreesTick,
                                        double origPowerTick,
                                        double actualPowerLow,
                                        double actualPowerHigh) {
        double realPowerTick = origPowerTick / 2;
        double realRadTick = Math.toRadians(origDegreesTick / 2) *
                aiPlayer.getPlayerPerson().getAiPlayStyle().defense / 100;
        Ball cueBall = game.getCueBall();
        double[] whitePos = new double[]{cueBall.getX(), cueBall.getY()};
        NavigableSet<Double> availableRads = new TreeSet<>();
        for (Ball ball : legalBalls) {
            double[] directionVec = new double[]{ball.getX() - whitePos[0], ball.getY() - whitePos[1]};
            double distance = Math.hypot(directionVec[0], directionVec[1]);
            double alpha = Algebra.thetaOf(directionVec);  // 白球到目标球球心连线的绝对角
            double theta = Math.asin(game.getGameValues().ballDiameter / distance);  // 中心连线与薄边连线的夹角

            int offsetTicks = (int) (theta / realRadTick);
//            System.out.println(offsetTicks + " radians offsets to " + ball + realRadTick + theta);

            double halfOfTick = realRadTick / 2;

            for (int i = -offsetTicks; i <= offsetTicks; i++) {
                double angle = Algebra.normalizeAngle(alpha + i * realRadTick);
                double[] vec = Algebra.unitVectorOfAngle(angle);
                PredictedPos leftPP = game.getPredictedHitBall(vec[0], vec[1]);
                if (leftPP.getTargetBall() == null || leftPP.getTargetBall() == ball) {
                    // 如果与已有的角度太接近就不考虑了
                    Double floorRad = availableRads.floor(angle);
                    Double ceilRad = availableRads.ceiling(angle);
                    if (floorRad != null && angle - floorRad < halfOfTick) continue;
                    if (ceilRad != null && ceilRad - angle < halfOfTick) continue;
                    availableRads.add(angle);
                }
            }
        }
        if (availableRads.isEmpty()) return null;

        System.out.println(availableRads.size() + " defense angles");
        Set<Ball> legalSet = new HashSet<>(legalBalls);
        DefenseChoice best = null;
        double selPowLow = actualPowerToSelectedPower(actualPowerLow);
        double selPowHigh = actualPowerToSelectedPower(actualPowerHigh);
        for (double selectedPower = selPowLow;
             selectedPower < selPowHigh;
             selectedPower += realPowerTick) {
            for (Double rad : availableRads) {
                DefenseChoice choice = defenseChoiceOfAngleAndPower(
                        rad, selectedPower, legalSet
                );
                if (choice != null) {
                    if (best == null || choice.price > best.price) {
                        best = choice;
                    }
                }
            }
        }
        return best;
    }

    private DefenseChoice solveSnookerDefense(List<Ball> legalBalls,
                                              double degreesTick, double powerTick) {
        Set<Ball> legalSet = new HashSet<>(legalBalls);
        DefenseChoice best = null;

        for (double selectedPower = 5.0;
             selectedPower < aiPlayer.getPlayerPerson().getControllablePowerPercentage();
             selectedPower += powerTick) {
            for (double deg = 0.0; deg < 360; deg += degreesTick) {
                DefenseChoice choice = defenseChoiceOfAngleAndPower(
                        Math.toRadians(deg), selectedPower, legalSet
                );
                if (choice != null) {
                    if (best == null || choice.price > best.price) {
                        best = choice;
                    }
                }
            }
        }
        return best;
    }

    protected DefenseChoice defenseChoiceOfAngleAndPower(double rad, double selectedPower,
                                                         Set<Ball> legalSet) {
        double[] unitXY = Algebra.unitVectorOfAngle(rad);
        CuePlayParams cpp = CuePlayParams.makeIdealParams(
                unitXY[0],
                unitXY[1],
                0.0,
                0.0,
                5.0,
                selectedPowerToActualPower(selectedPower)
        );
        WhitePrediction wp = game.predictWhite(cpp, 100000.0, false);
        double[] whiteStopPos = wp.getWhitePath().get(wp.getWhitePath().size() - 1);
        Ball firstCollide = wp.getFirstCollide();
        if (firstCollide != null && legalSet.contains(firstCollide)) {
            int opponentTarget = game.getTargetAfterPotFailed();
            List<Ball> opponentBalls = game.getAllLegalBalls(opponentTarget, false);
            List<AttackChoice> attackChoices = getAttackChoices(
                    opponentTarget,
                    game.getAnotherPlayer(aiPlayer),
                    null,
                    opponentBalls,
                    whiteStopPos
            );
            double opponentPrice = 0.0;
            for (AttackChoice ac : attackChoices) {
                opponentPrice += ac.price;
            }
            return new DefenseChoice(
                    firstCollide,
                    -opponentPrice,  // 对手的price越小，防守越成功
                    false,
                    unitXY,
                    selectedPower,
                    cpp
            );
        }
        return null;
    }

    protected DefenseChoice getBestDefenseChoice() {
        return getBestDefenseChoice(
                5.0,
                selectedPowerToActualPower(
                        aiPlayer.getPlayerPerson().getControllablePowerPercentage())
        );
    }

    protected DefenseChoice getBestDefenseChoice(double actualPowerLow, double actualPowerHigh) {
        int curTarget = game.getCurrentTarget();
        boolean isSnookerFreeBall = game.isDoingSnookerFreeBll();
        List<Ball> legalBalls = game.getAllLegalBalls(curTarget,
                isSnookerFreeBall);

        double degreesTick = 100.0 / aiPlayer.getPlayerPerson().getAiPlayStyle().defense;
        double powerTick = 1000.0 / aiPlayer.getPlayerPerson().getAiPlayStyle().defense;
        DefenseChoice normalDefense = directDefense(legalBalls, degreesTick, powerTick,
                15.0, actualPowerHigh);  // todo: low power
        if (normalDefense != null) return normalDefense;

        System.err.println("AI cannot find defense!");
        return solveSnookerDefense(legalBalls, degreesTick, powerTick);
    }

    public static class AttackChoice implements Comparable<AttackChoice> {
        protected Ball ball;
        protected Game<?, ?> game;
        protected Player attackingPlayer;
        protected double angleRad;  // 范围 [0, PI/2)
        protected double targetHoleDistance;
        protected double whiteCollisionDistance;
        protected double difficulty;
        protected double[] targetHoleVec;
        protected double[] holePos;  // 洞口瞄准点的坐标，非洞底
        protected double[] cueDirectionUnitVector;
        int attackTarget;
        double price;

        private AttackChoice() {
        }

        /**
         * @param lastAiPottedBall 如果这杆为走位预测，则该值为AI第一步想打的球。如这杆就是第一杆，则为null
         */
        protected static AttackChoice createChoice(Game<?, ?> game,
                                                   Player attackingPlayer,
                                                   double[] whitePos, 
                                                   Ball ball,
                                                   Ball lastAiPottedBall,
                                                   int attackTarget,
                                                   double collisionPointX, double collisionPointY,
                                                   double[][] dirHole) {
            double cueDirX = collisionPointX - whitePos[0];
            double cueDirY = collisionPointY - whitePos[1];
            double[] cueDirUnit = Algebra.unitVector(cueDirX, cueDirY);
            double[] targetToHole = dirHole[0];
            double[] holePos = dirHole[1];

            double[] whiteToColl =
                    new double[]{collisionPointX - whitePos[0], collisionPointY - whitePos[1]};
            double theta1 = Algebra.thetaOf(whiteToColl);
            double theta2 = Algebra.thetaOf(targetToHole);
            double angle = Math.abs(theta1 - theta2);
            if (angle < 0) angle = -angle;

            if (angle >= Math.PI / 2) {
                return null;  // 不可能打进的球
            }
            double whiteDistance = Math.hypot(whiteToColl[0], whiteToColl[1]);
            if (whiteDistance < game.getGameValues().ballDiameter) {
                return null;  // 白球和目标球挤在一起了
            }
            double targetHoleDistance = Math.hypot(ball.getX() - holePos[0],
                    ball.getY() - holePos[1]);

            AttackChoice attackChoice = new AttackChoice();
            attackChoice.game = game;
            attackChoice.ball = ball;
            attackChoice.targetHoleVec = targetToHole;
            attackChoice.holePos = holePos;
            attackChoice.angleRad = angle;
            attackChoice.targetHoleDistance = targetHoleDistance;
            attackChoice.whiteCollisionDistance = whiteDistance;
            attackChoice.cueDirectionUnitVector = cueDirUnit;
            attackChoice.attackTarget = attackTarget;
            attackChoice.calculateDifficulty();

            attackChoice.price = 10000.0 / attackChoice.difficulty *
                    game.priceOfTarget(attackTarget, ball, attackingPlayer, lastAiPottedBall);

//            System.out.println(attackChoice.price + " " + attackChoice.difficulty);

            return attackChoice;
        }

        private void calculateDifficulty() {
            // 把[0, PI/2) 展开至 [-没有难度, PI/2)
            double angleScaleRatio = (Math.PI / 2) / (Math.PI / 2 - NO_DIFFICULTY_ANGLE_RAD);
            double lowerLimit = -NO_DIFFICULTY_ANGLE_RAD * angleScaleRatio;
            double scaledAngle = angleRad * angleScaleRatio + lowerLimit;
            if (scaledAngle < 0) scaledAngle = 0;

            double difficultySore = 1 / Algebra.powerTransferOfAngle(scaledAngle);
            double totalDt = whiteCollisionDistance + targetHoleDistance;
            double holeAngleMultiplier = 1;
            if (isMidHole()) {
                double midHoleOffset = Math.abs(targetHoleVec[1]);  // 单位向量的y值绝对值越大，这球越简单
                holeAngleMultiplier /= Math.pow(midHoleOffset, 1.45);  // 次幂可以调，越小，ai越愿意打中袋
            }
            double tooCloseMul = (whiteCollisionDistance -
                    game.getGameValues().ballDiameter) / game.getGameValues().ballDiameter;
            
            // 太近了角度不好瞄
            if (tooCloseMul < 0.1) {
                holeAngleMultiplier *= 5;
            } else if (tooCloseMul < 1) {
                holeAngleMultiplier /= tooCloseMul * 2;
            }
            difficulty = totalDt * difficultySore * holeAngleMultiplier;
        }

        private boolean isMidHole() {
            return holePos[0] == game.getGameValues().topMidHoleOpenCenter[0];
        }

        @Override
        public int compareTo(@NotNull AiCue.AttackChoice o) {
            return -Double.compare(this.price, o.price);
        }

        @Override
        public String toString() {
            return "AttackChoice{" +
                    "ball=" + ball +
                    ", game=" + game +
                    ", angleRad=" + angleRad +
                    ", targetHoleDistance=" + targetHoleDistance +
                    ", whiteCollisionDistance=" + whiteCollisionDistance +
                    ", difficulty=" + difficulty +
                    ", price=" + price +
                    ", targetHoleVec=" + Arrays.toString(targetHoleVec) +
                    ", holePos=" + Arrays.toString(holePos) +
                    ", cueDirectionUnitVector=" + Arrays.toString(cueDirectionUnitVector) +
                    '}';
        }
    }

    public class IntegratedAttackChoice {

        final AttackChoice attackChoice;
        int nextStepTarget;
        final List<AttackChoice> nextStepAttackChoices;
        final double selectedPower;
        final double selectedFrontBackSpin;
        CuePlayParams params;
        Ball whiteSecondCollide;
        double speedWhenWhiteCollidesOther;
        private double price;

        protected IntegratedAttackChoice(AttackChoice attackChoice,
                                         List<AttackChoice> nextStepAttackChoices,
                                         int nextStepTarget,
                                         CuePlayParams params,
                                         double selectedPower,
                                         double selectedFrontBackSpin,
                                         Ball whiteSecondCollide,
                                         double speedWhenWhiteCollidesOther) {
            this.attackChoice = attackChoice;
            this.nextStepAttackChoices = nextStepAttackChoices;
            this.nextStepTarget = nextStepTarget;
            this.selectedPower = selectedPower;
            this.selectedFrontBackSpin = selectedFrontBackSpin;
            this.whiteSecondCollide = whiteSecondCollide;
            this.speedWhenWhiteCollidesOther = speedWhenWhiteCollidesOther;
            this.params = params;

            generatePrice();
        }

        int normalCompareTo(IntegratedAttackChoice o2) {
            double price2 = o2.price;
            return -Double.compare(price, price2);
        }

        int compareToWhenNoAvailNextBall(IntegratedAttackChoice o2) {
            return -Double.compare(this.priceWhenNoAvailNextBall(), o2.priceWhenNoAvailNextBall());
        }

        double priceWhenNoAvailNextBall() {
            if (whiteSecondCollide == null) return attackChoice.price * 0.5;  // k不到球，相当于没走位
            
            double targetMultiplier;
            if (game.isLegalBall(whiteSecondCollide, nextStepTarget, false)) 
                targetMultiplier = 1.0;  // k到目标球优先
            else targetMultiplier = 0.5;
            
            double speedThreshold = Values.MAX_POWER_SPEED / 10.0;
            double price = this.price * targetMultiplier;
            if (speedWhenWhiteCollidesOther < speedThreshold) {
                price *= speedWhenWhiteCollidesOther / speedThreshold;
            }
            return price;
        }

        private void generatePrice() {
            price = attackChoice.price;
            double mul = 0.5;
            for (AttackChoice next : nextStepAttackChoices) {
                price += next.price * mul;
                mul /= 4;
            }
            if (whiteSecondCollide != null) price *= 0.5;
        }
    }

    public static class DefenseChoice implements Comparable<DefenseChoice> {

        protected Ball ball;
        protected double price;
        protected boolean collideOtherBall;
        protected double[] cueDirectionUnitVector;
        double selectedPower;
        CuePlayParams cuePlayParams;

        protected DefenseChoice(Ball ball, double price, boolean collideOtherBall,
                                double[] cueDirectionUnitVector,
                                double selectedPower,
                                CuePlayParams cuePlayParams) {
            this.ball = ball;
            this.price = price;
            this.collideOtherBall = collideOtherBall;
            this.cueDirectionUnitVector = cueDirectionUnitVector;
            this.selectedPower = selectedPower;
            this.cuePlayParams = cuePlayParams;
        }

        @Override
        public int compareTo(@NotNull AiCue.DefenseChoice o) {
            if (this.collideOtherBall && !o.collideOtherBall) {
                return 1;
            } else if (!this.collideOtherBall && o.collideOtherBall) {
                return -1;
            }
            return -Double.compare(this.price, o.price);
        }
    }
}
