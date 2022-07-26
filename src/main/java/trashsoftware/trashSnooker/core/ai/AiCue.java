package trashsoftware.trashSnooker.core.ai;

import org.jetbrains.annotations.NotNull;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.movement.WhitePrediction;
import trashsoftware.trashSnooker.core.phy.Phy;

import java.util.*;

// todo: assume pickup, 大力K球奖励, 黑八半场自由球只能向下打

public abstract class AiCue<G extends Game<? extends Ball, P>, P extends Player> {

    public static final double ATTACK_DIFFICULTY_THRESHOLD = 6000.0;  // 越大，AI越倾向于进攻
    public static final double NO_DIFFICULTY_ANGLE_RAD = 0.3;
    public static final double EACH_BALL_SEE_PRICE = 0.5;
    //    private static final double[] FRONT_BACK_SPIN_POINTS =
//            {0.0, -0.27, 0.27, -0.54, 0.54, -0.81, 0.81};
    private static final double[][] SPIN_POINTS = {  // 高低杆，左右塞
            {0.0, 0.0}, {0.0, 0.35}, {0.0, -0.35}, {0.0, 0.7}, {0.0, -0.7},
            {-0.27, 0.0}, {-0.27, 0.3}, {-0.27, -0.3}, {-0.27, 0.6}, {-0.27, -0.6},
            {0.27, 0.0}, {0.27, 0.3}, {0.27, -0.3}, {0.27, 0.6}, {0.27, -0.6},
            {-0.54, 0.0}, {-0.54, 0.25}, {-0.54, -0.25}, {-0.54, 0.5}, {-0.54, -0.5},
            {0.54, 0.0}, {0.54, 0.25}, {0.54, -0.25}, {0.54, 0.5}, {0.54, -0.5},
            {-0.81, 0.0}, {-0.76, 0.3}, {-0.76, -0.3},
            {0.81, 0.0}, {0.76, 0.3}, {0.76, -0.3}
    };

    static {
        Arrays.sort(SPIN_POINTS, Comparator.comparingDouble(a -> Math.abs(a[0]) + Math.abs(a[1])));
    }

    protected G game;
    protected P aiPlayer;

    public AiCue(G game, P aiPlayer) {
        this.game = game;
        this.aiPlayer = aiPlayer;
    }

    public static <G extends Game<?, ?>> List<AttackChoice> getAttackChoices(G game,
                                                                             int attackTarget,
                                                                             Player attackingPlayer,
                                                                             Ball lastPottingBall,
                                                                             List<Ball> legalBalls,
                                                                             double[] whitePos) {
        List<AttackChoice> attackChoices = new ArrayList<>();
        for (Ball ball : legalBalls) {
            if (ball.isPotted() || ball == lastPottingBall) continue;  // todo: 潜在bug：斯诺克清彩阶段自由球
            List<double[][]> dirHoles = game.directionsToAccessibleHoles(ball);
//            System.out.println("dirHoles: " + dirHoles.size());
            double[] ballPos = new double[]{ball.getX(), ball.getY()};

            for (double[][] dirHole : dirHoles) {
                double collisionPointX = ballPos[0] - game.getGameValues().ballDiameter * dirHole[0][0];
                double collisionPointY = ballPos[1] - game.getGameValues().ballDiameter * dirHole[0][1];

                if (game.pointToPointCanPassBall(whitePos[0], whitePos[1],
                        collisionPointX, collisionPointY, game.getCueBall(), ball, true,
                        true)) {
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
                                            attackingPlayer.getPlayerPerson().getAiPlayStyle().attackPrivilege /
                                            100.0) {
                        attackChoices.add(attackChoice);
                    }
                }
            }
        }
        Collections.sort(attackChoices);
        return attackChoices;
    }

    public abstract AiCueResult makeCue(Phy phy);

    /**
     * 开球
     */
    protected abstract DefenseChoice breakCue();

    protected abstract DefenseChoice standardDefense();

    protected abstract DefenseChoice solveSnooker();

    protected double selectedPowerToActualPower(double selectedPower) {
        return selectedPower *
                aiPlayer.getInGamePlayer().getCurrentCue(game).powerMultiplier /
                game.getGameValues().ballWeightRatio;
    }

    protected double actualPowerToSelectedPower(double actualPower) {
        return actualPower /
                aiPlayer.getInGamePlayer().getCurrentCue(game).powerMultiplier *
                game.getGameValues().ballWeightRatio;
    }

    private IntegratedAttackChoice createIntAttackChoices(double selectedPower,
                                                          double selectedFrontBackSpin,
                                                          double selectedSideSpin,
                                                          AttackChoice attackChoice,
                                                          double playerSelfPrice,
                                                          GameValues values,
                                                          int nextTarget,
                                                          List<Ball> nextStepLegalBalls,
                                                          Phy phy) {
//        System.out.print(selectedPower);
        double actualSideSpin = CuePlayParams.unitSideSpin(selectedSideSpin,
                game.getCuingPlayer().getInGamePlayer().getCurrentCue(game));
        double actualPower = selectedPowerToActualPower(selectedPower);
//        double[] correctedDirection = attackChoice.cueDirectionUnitVector;
        double[] correctedDirection = CuePlayParams.aimingUnitXYIfSpin(
                actualSideSpin,
                actualPower,
                attackChoice.cueDirectionUnitVector[0],
                attackChoice.cueDirectionUnitVector[1]
        );

        AttackChoice correctedChoice = attackChoice.copyWithNewDirection(correctedDirection);

        CuePlayParams params = CuePlayParams.makeIdealParams(
                correctedChoice.cueDirectionUnitVector[0],
                correctedChoice.cueDirectionUnitVector[1],
                CuePlayParams.unitFrontBackSpin(selectedFrontBackSpin,
                        aiPlayer.getInGamePlayer(),
                        game.getCuingPlayer().getInGamePlayer().getCurrentCue(game)),
                actualSideSpin,
                5.0,
                actualPower
        );
        // 直接能打到的球，必不会在打到目标球之前碰库
        WhitePrediction wp = game.predictWhite(params, phy, 0.0,
                true, false, true);
        if (wp.getFirstCollide() == null) {
            // 连球都碰不到，没吃饭？
//            System.out.println("too less");
            return null;
        }
        if (wp.getWhiteCushionCount() == 0 && selectedSideSpin != 0.0) {
            // 不吃库的球加个卵的塞
            return null;
        }

        double targetCanMove = values.estimatedMoveDistance(phy, wp.getBallInitSpeed());
        if (targetCanMove - values.ballDiameter * 1.5 <= correctedChoice.targetHoleDistance) {
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
                getAttackChoices(game,
                        nextTarget,
                        aiPlayer,
                        wp.getFirstCollide(),
                        nextStepLegalBalls,
                        whiteStopPos);
        return new IntegratedAttackChoice(
                correctedChoice,
                nextStepAttackChoices,
                nextTarget,
                playerSelfPrice,
                params,
                selectedPower,
                selectedFrontBackSpin,
                selectedSideSpin,
                wp.getSecondCollide(),
                wp.getWhiteSpeedWhenHitSecondBall(),
                wp.isWhiteCollidesHoleArcs());
    }

    private IntegratedAttackChoice attack(AttackChoice choice,
                                          int nextTarget,
                                          List<Ball> nextStepLegalBalls,
                                          Phy phy) {
        double powerLimit = aiPlayer.getPlayerPerson().getControllablePowerPercentage();
        final double tick = 300.0 / aiPlayer.getPlayerPerson().getAiPlayStyle().position;

        GameValues values = game.getGameValues();

//        double comfortablePower = powerLimit * 0.35;
//        int maxIterations = (int) Math.round((powerLimit - comfortablePower) / tick) * 2;  // 注：仅适用于倍数<=0.5的情况

        List<IntegratedAttackChoice> choiceList = new ArrayList<>();
        AiPlayStyle aps = aiPlayer.getPlayerPerson().getAiPlayStyle();
//        double likeShow = aiPlayer.getPlayerPerson().getAiPlayStyle().likeShow;  // 喜欢大力及杆法的程度

        for (double selectedPower = tick; selectedPower <= powerLimit; selectedPower += tick) {
            for (double[] spins : SPIN_POINTS) {
                double price = aps.priceOf(
                        spins,
                        selectedPower,
                        aiPlayer.getInGamePlayer(),
                        game.getGamePlayStage(choice.ball, false)
                );
                IntegratedAttackChoice iac = createIntAttackChoices(
                        selectedPower,
                        spins[0],
                        spins[1],
                        choice,
                        price,
                        values,
                        nextTarget,
                        nextStepLegalBalls,
                        phy
                );
                if (iac != null) choiceList.add(iac);
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
        return new AiCueResult(aiPlayer.getPlayerPerson(),
                game.getGamePlayStage(choice.ball, true),
                false,
                null,
                null,
                choice.ball,
                choice.cueDirectionUnitVector[0],
                choice.cueDirectionUnitVector[1],
                0.0,
                0.0,
                choice.selectedPower);
    }

    protected AiCueResult regularCueDecision(Phy phy) {
        if (game.isBreaking()) {
            DefenseChoice breakChoice = breakCue();
            if (breakChoice != null) return makeDefenseCue(breakChoice);
        }

        List<AttackChoice> attackAttackChoices = getCurrentAttackChoices();
//        List<AttackChoice> attackAttackChoices = new ArrayList<>();
        System.out.println("Attack choices:" + attackAttackChoices.size());
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
                IntegratedAttackChoice iac = attack(choice, nextTargetIfThisSuccess, nextStepLegalBalls, phy);
                if (iac != null && iac.price > bestPrice) {
                    best = iac;
                    bestPrice = iac.price;
                }
            }
            if (best != null) {
                System.out.printf("Best int attack choice: dir %f, %f, power %f, spins %f, %f \n",
                        best.attackChoice.cueDirectionUnitVector[0], best.attackChoice.cueDirectionUnitVector[1],
                        best.selectedPower, best.selectedFrontBackSpin, best.selectedSideSpin);
                return new AiCueResult(aiPlayer.getPlayerPerson(),
                        game.getGamePlayStage(best.attackChoice.ball, true),
                        true,
                        best.attackChoice.targetOrigPos,
                        best.attackChoice.dirHole,
                        best.attackChoice.ball,
                        best.attackChoice.cueDirectionUnitVector[0],
                        best.attackChoice.cueDirectionUnitVector[1],
                        best.selectedFrontBackSpin,
                        best.selectedSideSpin,
                        best.selectedPower);
            }
        }
        DefenseChoice stdDefense = standardDefense();
        if (stdDefense != null) {
            return makeDefenseCue(stdDefense);
        }
        DefenseChoice defenseChoice = getBestDefenseChoice(phy);
        if (defenseChoice != null) {
            return makeDefenseCue(defenseChoice);
        }
        DefenseChoice solveSnooker = solveSnooker();
        if (solveSnooker != null) {
            return makeDefenseCue(solveSnooker);
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
                aiPlayer.getPlayerPerson(),
                GamePlayStage.NORMAL,
                false,
                null,
                null,
                null,
                directionVec[0],
                directionVec[1],
                0.0,
                0.0,
                power
        );
    }

    private List<AttackChoice> getAttackChoices(int attackTarget,
                                                Ball lastPottingBall,
                                                boolean isSnookerFreeBall,
                                                double[] whitePos) {
        return getAttackChoices(game,
                attackTarget,
                aiPlayer,
                lastPottingBall,
                game.getAllLegalBalls(attackTarget, isSnookerFreeBall),
                whitePos
        );
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
                                        double actualPowerHigh,
                                        Phy phy) {
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
                PredictedPos leftPP = game.getPredictedHitBall(
                        cueBall.getX(), cueBall.getY(),
                        vec[0], vec[1]);
                if (leftPP == null || leftPP.getTargetBall() == null ||
                        leftPP.getTargetBall() == ball) {
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
                        rad, selectedPower, legalSet, phy
                );
                if (choice != null) {
                    if (best == null || choice.price > best.price) {
                        best = choice;
                    }
                }
            }
        }
        if (best != null) System.out.println(best.price);
        return best;
    }

    private DefenseChoice solveSnookerDefense(List<Ball> legalBalls,
                                              double degreesTick, double powerTick, Phy phy) {
        Set<Ball> legalSet = new HashSet<>(legalBalls);
        DefenseChoice best = null;

        for (double selectedPower = 5.0;
             selectedPower < aiPlayer.getPlayerPerson().getControllablePowerPercentage();
             selectedPower += powerTick) {
            for (double deg = 0.0; deg < 360; deg += degreesTick) {
                DefenseChoice choice = defenseChoiceOfAngleAndPower(
                        Math.toRadians(deg), selectedPower, legalSet, phy
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
                                                         Set<Ball> legalSet, Phy phy) {
        double[] unitXY = Algebra.unitVectorOfAngle(rad);
        CuePlayParams cpp = CuePlayParams.makeIdealParams(
                unitXY[0],
                unitXY[1],
                0.0,
                0.0,
                5.0,
                selectedPowerToActualPower(selectedPower)
        );
        WhitePrediction wp = game.predictWhite(cpp, phy, 10000000.0,
                true, true, false);
        double[] whiteStopPos = wp.getWhitePath().get(wp.getWhitePath().size() - 1);
        Ball firstCollide = wp.getFirstCollide();
        if (firstCollide != null && legalSet.contains(firstCollide)) {
            int opponentTarget = game.getTargetAfterPotFailed();
            List<Ball> opponentBalls = game.getAllLegalBalls(opponentTarget, false);

            int seeAbleBalls = game.countSeeAbleTargetBalls(
                    whiteStopPos[0], whiteStopPos[1],
                    legalSet,
                    1
            );
//            System.out.println("See: " + seeAbleBalls);
//            if (seeAbleBalls == 0) {  // 斯诺克
            double opponentPrice = 0.1 + EACH_BALL_SEE_PRICE * seeAbleBalls;
            if (wp.getSecondCollide() != null) {
                opponentPrice *= 10;
            }
            if (wp.isFirstBallCollidesOther()) {
                opponentPrice *= 3;
            }
            opponentPrice -= 0.1;
//            } else {
            if (seeAbleBalls != 0) {
                List<AttackChoice> attackChoices = getAttackChoices(
                        game,
                        opponentTarget,
                        game.getAnotherPlayer(aiPlayer),
                        null,
                        opponentBalls,
                        whiteStopPos
                );

                for (AttackChoice ac : attackChoices) {
                    opponentPrice += ac.price;
                }

                if (wp.getSecondCollide() != null) {
                    opponentPrice *= 3;
                }
                if (wp.isFirstBallCollidesOther()) {
                    opponentPrice *= 3;
                }
            }
//            System.out.println(opponentPrice);
//            if (opponentPrice == 0) {  // 对手没有进攻机会
//                opponentPrice = 0.01;
//                for (Ball ball : opponentBalls) {
////                    if (game.canSeeBall(whiteStopPos[0], whiteStopPos[1],
////                            ball.getX(), ball.getY(),
////                            game.getCueBall(), ball)) {
////                        // todo: 检查同样的球而不是同一颗球
////                        opponentPrice += 0.01;
////                    }
//                }
//                if (wp.getSecondCollide() != null) {
//                    opponentPrice *= 4;
//                }
//                if (wp.isFirstBallCollidesOther()) {
//                    opponentPrice *= 4;
//                }
//                opponentPrice -= 0.01;
////            System.out.println("Snooker price " + opponentPrice);
//                
//            }
            wp.resetToInit();
            return new DefenseChoice(
                    firstCollide,
                    -opponentPrice,  // 对手的price越小，防守越成功
                    false,
                    unitXY,
                    selectedPower,
                    cpp
            );
        }
        wp.resetToInit();
        return null;
    }

    protected DefenseChoice getBestDefenseChoice(Phy phy) {
        return getBestDefenseChoice(
                5.0,
                selectedPowerToActualPower(
                        aiPlayer.getPlayerPerson().getControllablePowerPercentage()),
                phy
        );
    }

    protected DefenseChoice getBestDefenseChoice(double actualPowerLow, double actualPowerHigh, Phy phy) {
        int curTarget = game.getCurrentTarget();
        boolean isSnookerFreeBall = game.isDoingSnookerFreeBll();
        List<Ball> legalBalls = game.getAllLegalBalls(curTarget,
                isSnookerFreeBall);

        double degreesTick = 100.0 / 2 / aiPlayer.getPlayerPerson().getAiPlayStyle().defense;
        double powerTick = 1000.0 / aiPlayer.getPlayerPerson().getAiPlayStyle().defense;
        DefenseChoice normalDefense = directDefense(legalBalls, degreesTick, powerTick,
                15.0, actualPowerHigh, phy);  // todo: low power
        if (normalDefense != null) return normalDefense;

        System.err.println("AI cannot find defense!");
        return solveSnookerDefense(legalBalls, degreesTick, powerTick, phy);
    }

    public static class AttackChoice implements Comparable<AttackChoice> {
        protected Ball ball;
        protected Game<?, ?> game;
        protected Player attackingPlayer;
        protected double angleRad;  // 范围 [0, PI/2)
        protected double targetHoleDistance;
        protected double whiteCollisionDistance;
        protected double difficulty;
        protected double[][] dirHole;
        protected double[] targetOrigPos;
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
            attackChoice.dirHole = dirHole;
            attackChoice.targetOrigPos = new double[]{ball.getX(), ball.getY()};
            attackChoice.attackTarget = attackTarget;
            attackChoice.attackingPlayer = attackingPlayer;
            attackChoice.calculateDifficulty();

            attackChoice.price = 10000.0 / attackChoice.difficulty *
                    game.priceOfTarget(attackTarget, ball, attackingPlayer, lastAiPottedBall);

//            System.out.println(attackChoice.price + " " + attackChoice.difficulty);

            return attackChoice;
        }

        AttackChoice copyWithNewDirection(double[] newDirection) {
            AttackChoice copied = new AttackChoice();

            copied.game = game;
            copied.ball = ball;
            copied.targetHoleVec = targetHoleVec;
            copied.holePos = holePos;
            copied.angleRad = angleRad;
            copied.targetHoleDistance = targetHoleDistance;
            copied.whiteCollisionDistance = whiteCollisionDistance;
            copied.cueDirectionUnitVector = newDirection;
            copied.dirHole = dirHole;
            copied.targetOrigPos = new double[]{ball.getX(), ball.getY()};
            copied.attackTarget = attackTarget;
            copied.attackingPlayer = attackingPlayer;
            copied.difficulty = difficulty;
            copied.price = price;

            return copied;
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

        /**
         * 暴力开球用的
         */
        protected DefenseChoice(double[] cueDirectionUnitVector,
                                double selectedPower,
                                CuePlayParams cuePlayParams) {
            this(null,
                    0.0,
                    true,
                    cueDirectionUnitVector,
                    selectedPower,
                    cuePlayParams);
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

    public class IntegratedAttackChoice {

        static final double kickBallMul = 0.5;

        final AttackChoice attackChoice;
        final List<AttackChoice> nextStepAttackChoices;
        final double selectedPower;
        final double selectedFrontBackSpin;
        final double selectedSideSpin;
        private final double playerSelfPrice;
        int nextStepTarget;
        CuePlayParams params;
        Ball whiteSecondCollide;
        double speedWhenWhiteCollidesOther;
        private double price;
        private boolean whiteCollideHoleArcs;

        protected IntegratedAttackChoice(AttackChoice attackChoice,
                                         List<AttackChoice> nextStepAttackChoices,
                                         int nextStepTarget,
                                         double playerSelfPrice,
                                         CuePlayParams params,
                                         double selectedPower,
                                         double selectedFrontBackSpin,
                                         double selectedSideSpin,
                                         Ball whiteSecondCollide,
                                         double speedWhenWhiteCollidesOther,
                                         boolean whiteCollideHoleArcs) {
            this.attackChoice = attackChoice;
            this.nextStepAttackChoices = nextStepAttackChoices;
            this.nextStepTarget = nextStepTarget;
            this.playerSelfPrice = playerSelfPrice;  // 球手自己喜不喜欢这颗球
            this.selectedPower = selectedPower;
            this.selectedFrontBackSpin = selectedFrontBackSpin;
            this.selectedSideSpin = selectedSideSpin;
            this.whiteSecondCollide = whiteSecondCollide;
            this.speedWhenWhiteCollidesOther = speedWhenWhiteCollidesOther;
            this.params = params;
            this.whiteCollideHoleArcs = whiteCollideHoleArcs;

            generatePrice();
        }

        int normalCompareTo(IntegratedAttackChoice o2) {
            return -Double.compare(this.price, o2.price);
        }

        int compareToWhenNoAvailNextBall(IntegratedAttackChoice o2) {
            return -Double.compare(this.priceWhenNoAvailNextBall(), o2.priceWhenNoAvailNextBall());
        }

        double priceWhenNoAvailNextBall() {
            System.out.println(whiteSecondCollide);
            if (whiteSecondCollide == null) return attackChoice.price * kickBallMul;  // k不到球，相当于没走位

            double targetMultiplier;
            if (game.isLegalBall(whiteSecondCollide, nextStepTarget, false))
                targetMultiplier = 1.0 / kickBallMul;  // k到目标球优先
            else targetMultiplier = 1.0;  // k到其他球也还将就

            double speedThreshold = Values.MAX_POWER_SPEED / 10.0;
            double price = this.price * targetMultiplier;  // this.price本身已有k球惩罚，需补偿
            if (speedWhenWhiteCollidesOther < speedThreshold) {
                price *= speedWhenWhiteCollidesOther / speedThreshold;
            }
            if (whiteCollideHoleArcs) price *= 0.5;
            return price;
        }

        private void generatePrice() {
            price = attackChoice.price * playerSelfPrice;
            // 走位粗糙的人，下一颗权重低
            double mul = 0.5 *
                    attackChoice.attackingPlayer.getPlayerPerson().getAiPlayStyle().position / 100;
            for (AttackChoice next : nextStepAttackChoices) {
                price += next.price * mul;
                mul /= 4;
            }
            if (whiteSecondCollide != null) price *= kickBallMul;
            if (whiteCollideHoleArcs) price *= 0.5;
        }
    }
}
