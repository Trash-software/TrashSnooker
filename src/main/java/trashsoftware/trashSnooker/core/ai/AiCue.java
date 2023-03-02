package trashsoftware.trashSnooker.core.ai;

import org.jetbrains.annotations.NotNull;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.movement.WhitePrediction;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.snooker.AbstractSnookerGame;
import trashsoftware.trashSnooker.util.Util;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// todo: assume pickup, 大力K球奖励, 黑八半场自由球只能向下打

public abstract class AiCue<G extends Game<? extends Ball, P>, P extends Player> {

    public static final int N_THREADS = 8;
    public static final double ATTACK_DIFFICULTY_THRESHOLD = 18000.0;  // 越大，AI越倾向于进攻
    public static final double NO_DIFFICULTY_ANGLE_RAD = 0.3;
    public static final double EACH_BALL_SEE_PRICE = 0.5;
    public static final double WHITE_HIT_CORNER_PENALTY = 0.25;
    public static final double KICK_USELESS_BALL_MUL = 0.5;
    //    private static final double[] FRONT_BACK_SPIN_POINTS =
//            {0.0, -0.27, 0.27, -0.54, 0.54, -0.81, 0.81};
    protected static final double[][] SPIN_POINTS = {  // 高低杆，左右塞
            {0.0, 0.0}, {0.0, 0.3}, {0.0, -0.3}, {0.0, 0.6}, {0.0, -0.6},
            {-0.27, 0.0}, {-0.27, 0.25}, {-0.27, -0.25}, {-0.27, 0.5}, {-0.27, -0.5},
            {0.27, 0.0}, {0.27, 0.25}, {0.27, -0.25}, {0.27, 0.5}, {0.27, -0.5},
            {-0.54, 0.0}, {-0.54, 0.2}, {-0.54, -0.2}, {-0.54, 0.4}, {-0.54, -0.4},
            {0.54, 0.0}, {0.54, 0.2}, {0.54, -0.2}, {0.54, 0.4}, {0.54, -0.4},
            {-0.81, 0.0}, {-0.76, 0.3}, {-0.76, -0.3},
            {0.81, 0.0}, {0.76, 0.3}, {0.76, -0.3}
    };
    public static boolean aiOnlyDefense = false;

    static {
        Arrays.sort(SPIN_POINTS, Comparator.comparingDouble(a -> Math.abs(a[0]) + Math.abs(a[1])));
    }

    protected G game;
    protected P aiPlayer;

    public AiCue(G game, P aiPlayer) {
        this.game = game;
        this.aiPlayer = aiPlayer;
    }

    public static <G extends Game<?, ?>> List<AttackChoice> getAttackChoices(
            G game,
            int attackTarget,
            Player attackingPlayer,
            Ball lastPottingBall,
            List<Ball> legalBalls,
            double[] whitePos,
            boolean countLowChoices,  // 是否添加优先级很低的
            boolean isPositioning,
            double attackThreshold
    ) {
        List<AttackChoice> attackChoices = new ArrayList<>();
        for (Ball ball : legalBalls) {
            if (ball.isPotted() || ball == lastPottingBall) continue;  // todo: 潜在bug：斯诺克清彩阶段自由球
            List<double[][]> dirHoles = game.directionsToAccessibleHoles(ball);
//            System.out.println("dirHoles: " + dirHoles.size());
//            double[] ballPos = new double[]{ball.getX(), ball.getY()};

            for (double[][] dirHole : dirHoles) {
                double collisionPointX = dirHole[2][0];
                double collisionPointY = dirHole[2][1];

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
                            isPositioning,
                            collisionPointX,
                            collisionPointY,
                            dirHole
                    );
                    if (attackChoice != null) {
                        if (countLowChoices) {
                            attackChoices.add(attackChoice);
                        } else if (attackChoice.difficulty <
                                attackThreshold *
                                        attackingPlayer.getPlayerPerson().getAiPlayStyle().attackPrivilege /
                                        100.0) {
                            attackChoices.add(attackChoice);
                        }
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
    protected abstract DefenseChoice breakCue(Phy phy);

    protected abstract DefenseChoice standardDefense();

    protected double ballAlivePrice(Ball ball) {
        List<double[][]> dirHolePoints = game.directionsToAccessibleHoles(ball);
        double price = 0.0;
        final double diameter = game.getGameValues().ball.ballDiameter;
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
                    dirHolePoint[1][0] == game.getGameValues().table.midX,
                    dirHolePoint[0]
            ) * Math.hypot(ball.getX() - dirHolePoint[1][0], ball.getY() - dirHolePoint[1][1]);
            price += 10000.0 / potDifficulty;
        }
        return price;
    }

    protected DefenseChoice solveSnooker(Phy phy) {
        int curTarget = game.getCurrentTarget();
        boolean isSnookerFreeBall = game.isDoingSnookerFreeBll();
        List<Ball> legalBalls = game.getAllLegalBalls(curTarget,
                isSnookerFreeBall);

        PlayerPerson aps = aiPlayer.getPlayerPerson();
        double degreesTick = 100.0 / 2 / aps.getSolving();
        double powerTick = 1000.0 / aps.getSolving();

        System.out.println("AI solving snooker!");
        return solveSnookerDefense(legalBalls, degreesTick, powerTick, phy);
    }

    /**
     * @return 是否需要有球碰库
     */
    protected abstract boolean requireHitCushion();

    protected abstract double priceOfKick(Ball kickedBall, double kickSpeed);

    protected double selectedPowerToActualPower(double selectedPower,
                                                double unitCuePointX, double unitCuePointY,
                                                PlayerPerson.HandSkill handSkill) {
        double mul = Util.powerMultiplierOfCuePoint(unitCuePointX, unitCuePointY);
        double handMul = handSkill == null ? 1.0 : PlayerPerson.HandBody.getPowerMulOfHand(handSkill);
        return selectedPower * handMul * mul *
                aiPlayer.getInGamePlayer().getCurrentCue(game).powerMultiplier /
                game.getGameValues().ball.ballWeightRatio;
    }

    protected double actualPowerToSelectedPower(double actualPower,
                                                double unitSpinX, double unitSpinY,
                                                PlayerPerson.HandSkill handSkill) {
        double mul = Util.powerMultiplierOfCuePoint(unitSpinX, unitSpinY);
        double handMul = handSkill == null ? 1.0 : PlayerPerson.HandBody.getPowerMulOfHand(handSkill);
        return actualPower / handMul / mul /
                aiPlayer.getInGamePlayer().getCurrentCue(game).powerMultiplier *
                game.getGameValues().ball.ballWeightRatio;
    }

    private IntegratedAttackChoice attack(AttackChoice choice,
                                          int nextTarget,
                                          List<Ball> nextStepLegalBalls,
                                          Phy phy,
                                          double attackThreshold) {
        double powerLimit = aiPlayer.getPlayerPerson().getControllablePowerPercentage();
        final double tick = 300.0 / aiPlayer.getPlayerPerson().getAiPlayStyle().position;

        GameValues values = game.getGameValues();

//        double comfortablePower = powerLimit * 0.35;
//        int maxIterations = (int) Math.round((powerLimit - comfortablePower) / tick) * 2;  // 注：仅适用于倍数<=0.5的情况

        List<IntegratedAttackChoice> choiceList = new ArrayList<>();
        AiPlayStyle aps = aiPlayer.getPlayerPerson().getAiPlayStyle();
//        double likeShow = aiPlayer.getPlayerPerson().getAiPlayStyle().likeShow;  // 喜欢大力及杆法的程度
        GamePlayStage stage = game.getGamePlayStage(choice.ball, false);

        List<AttackThread> attackThreads = new ArrayList<>();

        long t0 = System.currentTimeMillis();

        for (double selectedPower = tick; selectedPower <= powerLimit; selectedPower += tick) {
            for (double[] spins : SPIN_POINTS) {
                double price = aps.priceOf(
                        spins,
                        selectedPower,
                        aiPlayer.getInGamePlayer(),
                        stage
                );
                AttackThread attackThread = new AttackThread(
                        selectedPower,
                        spins[0],
                        spins[1],
                        choice,
                        price,
                        values,
                        nextTarget,
                        nextStepLegalBalls,
                        phy,
                        stage,
                        attackThreshold
                );
                attackThreads.add(attackThread);
            }
        }

        ExecutorService executorService = Executors.newFixedThreadPool(N_THREADS);
        for (AttackThread thread : attackThreads) {
            executorService.execute(thread);
        }

        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(1, TimeUnit.MINUTES))
                throw new RuntimeException("AI thread not terminated.");  // Wait for all threads complete.
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        long t2 = System.currentTimeMillis();
        System.out.println("Ai calculated attacks of given choice in " + (t2 - t0) + " ms");

        for (AttackThread thread : attackThreads) {
            if (thread.result != null) choiceList.add(thread.result);
        }

        if (choiceList.isEmpty()) return null;
        choiceList.sort(IntegratedAttackChoice::normalCompareTo);
        for (IntegratedAttackChoice iac : choiceList) {
            if (!iac.nextStepAttackChoices.isEmpty()) {
                return choiceList.get(0);
            }
        }
//        choiceList.sort(IntegratedAttackChoice::compareToWhenNoAvailNextBall);
        IntegratedAttackChoice iac = choiceList.get(0);
        if (iac.nextStepTarget != Game.END_REP && iac.whitePrediction.getSecondCollide() == null)
            return null;  // 打进了没位，打什么打
        return iac;
    }

    protected AiCueResult makeAttackCue(IntegratedAttackChoice iac) {
        return new AiCueResult(aiPlayer.getInGamePlayer(),
                game.getGamePlayStage(iac.attackChoice.ball, true),
                AiCueResult.CueType.ATTACK,
                iac.attackChoice.targetOrigPos,
                iac.attackChoice.dirHole,
                iac.attackChoice.ball,
                iac.attackChoice.cueDirectionUnitVector[0],
                iac.attackChoice.cueDirectionUnitVector[1],
                iac.selectedFrontBackSpin,
                iac.selectedSideSpin,
                iac.selectedPower,
                iac.attackChoice.handSkill);
    }

    protected AiCueResult makeDefenseCue(DefenseChoice choice, AiCueResult.CueType cueType) {
        return new AiCueResult(aiPlayer.getInGamePlayer(),
                game.getGamePlayStage(choice.ball, true),
                cueType,
                null,
                null,
                choice.ball,
                choice.cueDirectionUnitVector[0],
                choice.cueDirectionUnitVector[1],
                0.0,  // todo
                choice.selectedSideSpin,
                choice.selectedPower,
                choice.handSkill);
    }

    protected AiCueResult regularCueDecision(Phy phy) {
        if (game.isBreaking()) {
            DefenseChoice breakChoice = breakCue(phy);
            if (breakChoice != null) return makeDefenseCue(breakChoice, AiCueResult.CueType.BREAK);
        }

        IntegratedAttackChoice attackChoice = standardAttack(phy, ATTACK_DIFFICULTY_THRESHOLD);
        if (attackChoice != null) {
            return makeAttackCue(attackChoice);
        }
        DefenseChoice stdDefense = standardDefense();
        if (stdDefense != null) {
            return makeDefenseCue(stdDefense, AiCueResult.CueType.DEFENSE);
        }
        DefenseChoice defenseChoice = getBestDefenseChoice(phy);
        if (defenseChoice != null) {
            return makeDefenseCue(defenseChoice, AiCueResult.CueType.DEFENSE);
        }
        DefenseChoice solveSnooker = solveSnooker(phy);
        if (solveSnooker != null) {
            return makeDefenseCue(solveSnooker, AiCueResult.CueType.SOLVE);
        }
        return randomAngryCue();
    }

    protected IntegratedAttackChoice standardAttack(Phy phy, double attackThreshold) {
        List<AttackChoice> attackChoices =
                (aiOnlyDefense && game.getCurrentTarget() != 1) ?
                        new ArrayList<>() : getCurrentAttackChoices(attackThreshold);
        return attackGivenChoices(attackChoices, phy, attackThreshold);
    }

    protected IntegratedAttackChoice attackGivenChoices(List<AttackChoice> attackChoices,
                                                        Phy phy,
                                                        double attackThreshold) {
        System.out.println("Simple attack choices:" + attackChoices.size());
//        System.out.println(attackAttackChoices);
        if (!attackChoices.isEmpty()) {
            double bestPrice = 0.0;
            IntegratedAttackChoice best = null;
            for (int i = 0; i < Math.min(2, attackChoices.size()); i++) {
                AttackChoice choice = attackChoices.get(i);
                int nextTargetIfThisSuccess = game.getTargetAfterPotSuccess(choice.ball,
                        game.isDoingSnookerFreeBll());
                List<Ball> nextStepLegalBalls =
                        game.getAllLegalBalls(nextTargetIfThisSuccess,
                                false);  // 这颗进了下一颗怎么可能是自由球
                IntegratedAttackChoice iac = attack(choice, nextTargetIfThisSuccess, nextStepLegalBalls, phy, attackThreshold);
                if (iac != null && iac.price > bestPrice) {
                    best = iac;
                    bestPrice = iac.price;
                }
            }
            if (best != null) {
                System.out.printf("Best int attack choice: dir %f, %f, power %f, spins %f, %f, difficulty, %f \n",
                        best.attackChoice.cueDirectionUnitVector[0], best.attackChoice.cueDirectionUnitVector[1],
                        best.selectedPower, best.selectedFrontBackSpin, best.selectedSideSpin,
                        best.attackChoice.difficulty);
                return best;
            }
        }
        return null;
    }

    private AiCueResult randomAngryCue() {
        System.out.println("Shit! No way to deal this!");
        Random random = new Random();
        double directionRad = random.nextDouble() * Math.PI * 2;
        double power = random.nextDouble() *
                (aiPlayer.getPlayerPerson().getMaxPowerPercentage() - 20.0) + 20.0;
        double[] directionVec = Algebra.unitVectorOfAngle(directionRad);
        PlayerPerson.HandSkill handSkill = CuePlayParams.getPlayableHand(
                game.getCueBall().getX(), game.getCueBall().getY(),
                directionVec[0], directionVec[1],
                game.getGameValues().table,
                game.getCuingPlayer().getPlayerPerson()
        );
        return new AiCueResult(
                aiPlayer.getInGamePlayer(),
                GamePlayStage.NORMAL,
                AiCueResult.CueType.DEFENSE,
                null,
                null,
                null,
                directionVec[0],
                directionVec[1],
                0.0,
                0.0,
                power,
                handSkill
        );
    }

    private List<AttackChoice> getAttackChoices(int attackTarget,
                                                Ball lastPottingBall,
                                                boolean isSnookerFreeBall,
                                                double[] whitePos,
                                                boolean countLowChoices,
                                                boolean isPositioning,
                                                double attackThreshold) {
        return getAttackChoices(game,
                attackTarget,
                aiPlayer,
                lastPottingBall,
                game.getAllLegalBalls(attackTarget, isSnookerFreeBall),
                whitePos,
                countLowChoices,
                isPositioning,
                attackThreshold
        );
    }

    protected List<AttackChoice> getCurrentAttackChoices(double attackThreshold) {
        return getAttackChoices(game.getCurrentTarget(),
                null,
                game.isDoingSnookerFreeBll(),
                new double[]{game.getCueBall().getX(), game.getCueBall().getY()},
                false,
                false,
                attackThreshold);
    }

    private DefenseChoice directDefense(List<Ball> legalBalls,
                                        double origDegreesTick,
                                        double origPowerTick,
                                        double actualPowerLow,
                                        double actualPowerHigh,
                                        Phy phy) {
        double realPowerTick = origPowerTick / 2;
        double realRadTick = Math.toRadians(origDegreesTick);
        Ball cueBall = game.getCueBall();
        double[] whitePos = new double[]{cueBall.getX(), cueBall.getY()};
        NavigableSet<Double> availableRads = new TreeSet<>();
        for (Ball ball : legalBalls) {
            double[] directionVec = new double[]{ball.getX() - whitePos[0], ball.getY() - whitePos[1]};
            double distance = Math.hypot(directionVec[0], directionVec[1]);
            double alpha = Algebra.thetaOf(directionVec);  // 白球到目标球球心连线的绝对角
            double theta = Math.asin(game.getGameValues().ball.ballDiameter / distance);  // 中心连线与薄边连线的夹角

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
        double selPowLow = actualPowerToSelectedPower(actualPowerLow, 0, 0, null);
        double selPowHigh = actualPowerToSelectedPower(actualPowerHigh, 0, 0, null);
        for (double selectedPower = selPowLow;
             selectedPower < selPowHigh;
             selectedPower += realPowerTick) {

            for (Double rad : availableRads) {
                DefenseChoice choice = defenseChoiceOfAngleAndPower(
                        rad, whitePos, selectedPower, legalSet, phy
                );
                if (choice != null && notViolateCushionRule(choice)) {
                    if (best == null || choice.compareTo(best) < 0) {
                        best = choice;
                    }
                }
            }
        }
        if (best != null) {
            System.out.printf("defense: %f, %f, %f, %f\n",
                    best.price, best.snookerPrice, best.opponentAttackPrice, best.penalty);
        }
        return best;
    }

    private boolean notViolateCushionRule(DefenseChoice choice) {
        if (requireHitCushion()) {
            if (choice.wp == null) return true;
            else return choice.wp.getWhiteCushionCountAfter() > 0 ||
                    choice.wp.getFirstBallCushionCount() > 0;
        } else {
            return true;
        }
    }

    private DefenseChoice solveSnookerDefense(List<Ball> legalBalls,
                                              double degreesTick, double powerTick, Phy phy) {
        Set<Ball> legalSet = new HashSet<>(legalBalls);
        DefenseChoice best = null;

        Ball cueBall = game.getCueBall();
        double[] whitePos = new double[]{cueBall.getX(), cueBall.getY()};

        for (double selectedPower = 5.0;
             selectedPower < aiPlayer.getPlayerPerson().getControllablePowerPercentage();
             selectedPower += powerTick) {
            for (double deg = 0.0; deg < 360; deg += degreesTick) {
                DefenseChoice choice = defenseChoiceOfAngleAndPower(
                        Math.toRadians(deg), whitePos, selectedPower, legalSet, phy
                );
                if (choice != null && notViolateCushionRule(choice)) {
                    if (best == null || choice.compareTo(best) < 0) {
                        best = choice;
                    }
                }
            }
        }
        return best;
    }

    protected DefenseChoice defenseChoiceOfAngleAndPower(double rad, double[] whitePos,
                                                         double selectedPower,
                                                         Set<Ball> legalSet, Phy phy) {
        double[] unitXY = Algebra.unitVectorOfAngle(rad);

        PlayerPerson.HandSkill handSkill = CuePlayParams.getPlayableHand(
                whitePos[0],
                whitePos[1],
                unitXY[0],  // fixme: 这里存疑
                unitXY[1],
                game.getGameValues().table,
                game.getCuingPlayer().getPlayerPerson()
        );

        CuePlayParams cpp = CuePlayParams.makeIdealParams(
                unitXY[0],
                unitXY[1],
                0.0,
                0.0,
                0.0,
                selectedPowerToActualPower(selectedPower, 0, 0, handSkill)
        );
        WhitePrediction wp = game.predictWhite(cpp, phy, 10000000.0,
                true, true, false);
        double[] whiteStopPos = wp.getWhitePath().get(wp.getWhitePath().size() - 1);
        Ball firstCollide = wp.getFirstCollide();
        if (firstCollide != null && legalSet.contains(firstCollide)) {
            if (wp.willFirstBallPot()) {
                wp.resetToInit();
                return null;
            }

            int opponentTarget = game.getTargetAfterPotFailed();
            List<Ball> opponentBalls = game.getAllLegalBalls(opponentTarget, false);

            Game.SeeAble seeAble = game.countSeeAbleTargetBalls(
                    whiteStopPos[0], whiteStopPos[1],
                    opponentBalls,
                    1
            );
//            System.out.println("See: " + seeAbleBalls);
//            if (seeAbleBalls == 0) {  // 斯诺克
//            final double maxPrice = EACH_BALL_SEE_PRICE * game.getAllBalls().length;
//            double defensePrice = maxPrice - EACH_BALL_SEE_PRICE * seeAbleBalls;
//            if (wp.getSecondCollide() != null) {
//                defensePrice /= 10;
//            }
//            if (wp.isFirstBallCollidesOther()) {
//                defensePrice /= 3;
//            }
////            } else {
            double penalty = 1.0;
            double opponentAttackPrice = AttackChoice.priceOfDistance(seeAble.avgTargetDistance);
            double snookerPrice = 1.0;

            if (wp.isFirstBallCollidesOther()) {
                penalty *= 20;
            }
            if (wp.getSecondCollide() != null) {
                penalty *= 20;
            }

            if (seeAble.seeAbleTargets == 0) {
                snookerPrice = Math.sqrt(seeAble.maxShadowAngle) * 300;
            } else {
                List<AttackChoice> attackChoices = getAttackChoices(
                        game,
                        opponentTarget,
                        game.getAnotherPlayer(aiPlayer),
                        null,
                        opponentBalls,
                        whiteStopPos,
                        true,
                        false,
                        ATTACK_DIFFICULTY_THRESHOLD
                );

                for (AttackChoice ac : attackChoices) {
                    opponentAttackPrice += ac.price;
                }
            }

            if (wp.getWhiteCushionCountBefore() > 2) {
                penalty *= (wp.getWhiteCushionCountBefore() - 1.5);
            }
            if (wp.getWhiteCushionCountAfter() > 3) {
                penalty *= (wp.getWhiteCushionCountAfter() - 2.5);
            }
            if (wp.getFirstBallCushionCount() > 3) {
                penalty *= (wp.getFirstBallCushionCount() - 2.5);
            }
            if (wp.isWhiteCollidesHoleArcs()) {
                penalty /= WHITE_HIT_CORNER_PENALTY;
            }
            wp.resetToInit();
//            System.out.printf("%f %f %f\n", snookerPrice, opponentAttackPrice, penalty);
            return new DefenseChoice(
                    firstCollide,
                    snookerPrice,
                    opponentAttackPrice,
                    penalty,
                    unitXY,
                    selectedPower,
                    0.0,
                    wp,
                    cpp,
                    handSkill
            );
        }
        wp.resetToInit();
        return null;
    }

    protected DefenseChoice getBestDefenseChoice(Phy phy) {
        return getBestDefenseChoice(
                5.0,
                selectedPowerToActualPower(
                        aiPlayer.getPlayerPerson().getControllablePowerPercentage(),
                        0, 0, null),
                phy
        );
    }

    protected DefenseChoice getBestDefenseChoice(double actualPowerLow, double actualPowerHigh, Phy phy) {
        int curTarget = game.getCurrentTarget();
        boolean isSnookerFreeBall = game.isDoingSnookerFreeBll();
        List<Ball> legalBalls = game.getAllLegalBalls(curTarget,
                isSnookerFreeBall);

        AiPlayStyle aps = aiPlayer.getPlayerPerson().getAiPlayStyle();
        double degreesTick = 100.0 / 2 / aps.defense;
        double powerTick = 1000.0 / aps.defense;
        return directDefense(legalBalls, degreesTick, powerTick,
                5.0, actualPowerHigh, phy);
    }

    public static class AttackChoice implements Comparable<AttackChoice> {
        protected Ball ball;
        protected Game<?, ?> game;
        protected Player attackingPlayer;
        protected double angleRad;  // 范围 [0, PI/2)
        protected double targetHoleDistance;
        protected double whiteCollisionDistance;
        protected double difficulty;
        protected boolean isPositioning;  // 是走位预测还是进攻
        protected double[] whitePos;
        protected double[][] dirHole;
        protected double[] targetOrigPos;
        protected double[] targetHoleVec;
        protected double[] holePos;  // 洞口瞄准点的坐标，非洞底
        protected double[] cueDirectionUnitVector;
        protected PlayerPerson.HandSkill handSkill;
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
                                                   boolean isPositioning,
                                                   double collisionPointX, double collisionPointY,
                                                   double[][] dirHole) {
            double cueDirX = collisionPointX - whitePos[0];
            double cueDirY = collisionPointY - whitePos[1];
            double[] cueDirUnit = Algebra.unitVector(cueDirX, cueDirY);
            double[] targetToHole = dirHole[0];
            double[] holePos = dirHole[1];

            PlayerPerson.HandSkill handSkill = CuePlayParams.getPlayableHand(
                    whitePos[0], whitePos[1],
                    cueDirUnit[0], cueDirUnit[1],
                    game.getGameValues().table,
                    attackingPlayer.getPlayerPerson()
            );

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
            if (whiteDistance < game.getGameValues().ball.ballDiameter) {
                return null;  // 白球和目标球挤在一起了
            }
            double targetHoleDistance = Math.hypot(ball.getX() - holePos[0],
                    ball.getY() - holePos[1]);

            AttackChoice attackChoice = new AttackChoice();
            attackChoice.game = game;
            attackChoice.ball = ball;
            attackChoice.isPositioning = isPositioning;
            attackChoice.targetHoleVec = targetToHole;
            attackChoice.holePos = holePos;
            attackChoice.angleRad = angle;
            attackChoice.targetHoleDistance = targetHoleDistance;
            attackChoice.whitePos = whitePos;
            attackChoice.whiteCollisionDistance = whiteDistance;
            attackChoice.cueDirectionUnitVector = cueDirUnit;
            attackChoice.dirHole = dirHole;
            attackChoice.targetOrigPos = new double[]{ball.getX(), ball.getY()};
            attackChoice.attackTarget = attackTarget;
            attackChoice.attackingPlayer = attackingPlayer;
            attackChoice.handSkill = handSkill;
            attackChoice.calculateDifficulty();

            attackChoice.price = 10000.0 / attackChoice.difficulty *
                    game.priceOfTarget(attackTarget, ball, attackingPlayer, lastAiPottedBall);

//            System.out.println(attackChoice.price + " " + attackChoice.difficulty);

            return attackChoice;
        }

        static double priceOfDistance(double distance) {
            double difficulty = distance * 2;
            return 10000.0 / difficulty;  // Refer to the class's constructor
        }

        protected static double holeDifficulty(Game<?, ?> game,
                                               boolean isMidHole,
                                               double[] targetHoleVec) {
            if (isMidHole) {
//                double midHoleOffset = Math.abs(targetHoleVec[1]);  // 单位向量的y值绝对值越大，这球越简单
//                return 1 / Math.pow(midHoleOffset, 1.4);  // 次幂可以调，越小，ai越愿意打中袋
                // 基本上就是往中袋的投影占比
                double holeProjWidth = Math.abs(targetHoleVec[1]) * game.getGameValues().table.midHoleDiameter;
                double errorToleranceWidth = holeProjWidth - game.getGameValues().ball.ballRadius;
                errorToleranceWidth = Math.max(errorToleranceWidth, 0.00001);
                return game.getGameValues().midHoleBestAngleWidth / errorToleranceWidth;
            } else {
                // 底袋，45度时难度系数为1，0度或90度时难度系数最大，为 sqrt(2)/2 * 袋直径 - 球半径 的倒数
                double easy = 1 - Math.abs(Math.abs(targetHoleVec[0]) - Math.abs(targetHoleVec[1]));  // [0,1]
                double range = 1 - game.getGameValues().cornerHoleAngleRatio;
                return 1 / (easy * range + game.getGameValues().cornerHoleAngleRatio);
            }
        }

        AttackChoice copyWithNewDirection(double[] newDirection) {
            AttackChoice copied = new AttackChoice();

            copied.game = game;
            copied.ball = ball;
            copied.isPositioning = isPositioning;
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
            copied.handSkill = handSkill;

            return copied;
        }

        private void calculateDifficulty() {
            /* 难度分4部分: 
               1. 目标球的角度在目视方向的投影长度
               2. 白球与目标球间的距离
               3. 总行程
               4. 袋的进球角度
             */

            // 就是说从白球处看击打点，需要偏多少距离才是袋，300毫米是保底: 偏30厘米以内一般不影响瞄准
            double targetDifficulty =
                    Math.cos(Math.PI / 2 - angleRad) * targetHoleDistance;
            targetDifficulty = Math.max(targetDifficulty, 300);

            double baseDifficulty = (whiteCollisionDistance + 300) * targetDifficulty / 100;
            double totalDtDifficulty = whiteCollisionDistance + targetHoleDistance;

            double holeDifficulty = holeDifficulty(game, isMidHole(), targetHoleVec);

            double penalty = 1.0;
            if (isPositioning) {
                double dtLow = game.getGameValues().ball.ballDiameter * 5;
                if (whiteCollisionDistance < dtLow) {
                    penalty *= dtLow / whiteCollisionDistance;
                }
            }

            difficulty = (baseDifficulty * holeDifficulty + totalDtDifficulty) * penalty;
        }

        private boolean isMidHole() {
            return holePos[0] == game.getGameValues().table.midX;
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

        private final double penalty;
        protected PlayerPerson.HandSkill handSkill;
        protected Ball ball;
        protected double snookerPrice;
        protected double opponentAttackPrice;
        protected double price;
        protected double[] cueDirectionUnitVector;  // selected
        double selectedSideSpin;
        double selectedPower;
        CuePlayParams cuePlayParams;
        WhitePrediction wp;

        protected DefenseChoice(Ball ball,
                                double snookerPrice,
                                double opponentAttackPrice,
                                double penalty,
                                double[] cueDirectionUnitVector,
                                double selectedPower,
                                double selectedSideSpin,
                                WhitePrediction wp,
                                CuePlayParams cuePlayParams,
                                PlayerPerson.HandSkill handSkill) {
            this.ball = ball;
            this.snookerPrice = snookerPrice;
            this.opponentAttackPrice = opponentAttackPrice;
            this.penalty = penalty;

//            this.collideOtherBall = collideOtherBall;
            this.cueDirectionUnitVector = cueDirectionUnitVector;
            this.selectedPower = selectedPower;
            this.selectedSideSpin = selectedSideSpin;
            this.cuePlayParams = cuePlayParams;
            this.wp = wp;
            this.handSkill = handSkill;

            generatePrice();
        }

        /**
         * 暴力开球用的
         */
        protected DefenseChoice(double[] cueDirectionUnitVector,
                                double selectedPower,
                                double selectedSideSpin,
                                CuePlayParams cuePlayParams, PlayerPerson.HandSkill handSkill) {
            this(null,
                    0.0,
                    0.0,
                    0.0,
                    cueDirectionUnitVector,
                    selectedPower,
                    selectedSideSpin,
                    null,
                    cuePlayParams,
                    handSkill);
        }

        private void generatePrice() {
            this.price = snookerPrice / penalty - opponentAttackPrice * penalty;
        }

        @Override
        public int compareTo(@NotNull AiCue.DefenseChoice o) {
//            if (this.collideOtherBall && !o.collideOtherBall) {
//                return 1;
//            } else if (!this.collideOtherBall && o.collideOtherBall) {
//                return -1;
//            }
            return -Double.compare(this.price, o.price);
        }
    }

    public class IntegratedAttackChoice {

        final AttackChoice attackChoice;
        final List<AttackChoice> nextStepAttackChoices;
        final double selectedPower;
        final double selectedFrontBackSpin;
        final double selectedSideSpin;
        private final double playerSelfPrice;
        //        Ball whiteSecondCollide;
//        double speedWhenWhiteCollidesOther;
//        private boolean whiteCollideHoleArcs;
        private final WhitePrediction whitePrediction;
        private final GamePlayStage stage;
        protected double price;
        int nextStepTarget;
        CuePlayParams params;

        protected IntegratedAttackChoice(AttackChoice attackChoice,
                                         List<AttackChoice> nextStepAttackChoices,
                                         int nextStepTarget,
                                         double playerSelfPrice,
                                         CuePlayParams params,
                                         double selectedPower,
                                         double selectedFrontBackSpin,
                                         double selectedSideSpin,
//                                         Ball whiteSecondCollide,
//                                         double speedWhenWhiteCollidesOther,
//                                         boolean whiteCollideHoleArcs
                                         WhitePrediction whitePrediction,
                                         GamePlayStage stage
        ) {
            this.attackChoice = attackChoice;
            this.nextStepAttackChoices = nextStepAttackChoices;
            this.nextStepTarget = nextStepTarget;
            this.playerSelfPrice = playerSelfPrice;  // 球手自己喜不喜欢这颗球
            this.selectedPower = selectedPower;
            this.selectedFrontBackSpin = selectedFrontBackSpin;
            this.selectedSideSpin = selectedSideSpin;
            this.whitePrediction = whitePrediction;
            this.stage = stage;
//            this.whiteSecondCollide = whiteSecondCollide;
//            this.speedWhenWhiteCollidesOther = speedWhenWhiteCollidesOther;
            this.params = params;
//            this.whiteCollideHoleArcs = whiteCollideHoleArcs;

            generatePrice();
        }

        int normalCompareTo(IntegratedAttackChoice o2) {
            return -Double.compare(this.price, o2.price);
        }

        int compareToWhenNoAvailNextBall(IntegratedAttackChoice o2) {
            return -Double.compare(this.priceWhenNoAvailNextBall(), o2.priceWhenNoAvailNextBall());
        }

        double priceWhenNoAvailNextBall() {
//            System.out.println(whiteSecondCollide);
            if (whitePrediction.getSecondCollide() == null)
                return attackChoice.price * 0.1;  // k不到球，相当于没走位

            double targetMultiplier;
            if (game.isLegalBall(whitePrediction.getSecondCollide(), nextStepTarget, false))
                targetMultiplier = 1.0 / KICK_USELESS_BALL_MUL;  // k到目标球优先
            else targetMultiplier = 1.0;  // k到其他球也还将就

            double speedThreshold = Values.MAX_POWER_SPEED / 8.0;
            double price = this.price * targetMultiplier;  // this.price本身已有k球惩罚，需补偿
            if (whitePrediction.getWhiteSpeedWhenHitSecondBall() < speedThreshold) {
                price *= whitePrediction.getWhiteSpeedWhenHitSecondBall() / speedThreshold;
            }
            if (whitePrediction.isWhiteCollidesHoleArcs()) price *= WHITE_HIT_CORNER_PENALTY;
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
//            if (whitePrediction.getSecondCollide() != null) price *= kickBallMul;
            if (whitePrediction.getSecondCollide() != null) {
                double priceOfKick = priceOfKick(whitePrediction.getSecondCollide(),
                        whitePrediction.getWhiteSpeedWhenHitSecondBall());
                price *= priceOfKick;
            }

            if (whitePrediction.isWhiteCollidesHoleArcs()) price *= 0.5;

            if (stage != GamePlayStage.NO_PRESSURE) {
                // 正常情况下少走点库
                int cushions = whitePrediction.getWhiteCushionCountAfter();
                double cushionDiv = Math.max(2, cushions) / 4.0 + 0.5;  // Math.max(x, cushions) / y + (1 - x / y)
                price /= cushionDiv;
            }
        }
    }

    protected class AttackThread implements Runnable {

        double selectedPower;
        double selectedFrontBackSpin;
        double selectedSideSpin;
        AttackChoice attackChoice;
        double playerSelfPrice;
        GameValues values;
        int nextTarget;
        List<Ball> nextStepLegalBalls;
        Phy phy;
        GamePlayStage stage;
        double attackThreshold;

        IntegratedAttackChoice result;

        protected AttackThread(double selectedPower,
                               double selectedFrontBackSpin,
                               double selectedSideSpin,
                               AttackChoice attackChoice,
                               double playerSelfPrice,
                               GameValues values,
                               int nextTarget,
                               List<Ball> nextStepLegalBalls,
                               Phy phy,
                               GamePlayStage stage,
                               double attackThreshold) {
            this.selectedPower = selectedPower;
            this.selectedFrontBackSpin = selectedFrontBackSpin;
            this.selectedSideSpin = selectedSideSpin;
            this.attackChoice = attackChoice;
            this.playerSelfPrice = playerSelfPrice;
            this.values = values;
            this.nextTarget = nextTarget;
            this.nextStepLegalBalls = nextStepLegalBalls;
            this.phy = phy;
            this.stage = stage;
            this.attackThreshold = attackThreshold;
        }

        @Override
        public void run() {

            //        System.out.print(selectedPower);
            double actualFbSpin = CuePlayParams.unitFrontBackSpin(selectedFrontBackSpin,
                    aiPlayer.getInGamePlayer(),
                    game.getCuingPlayer().getInGamePlayer().getCurrentCue(game));
            double actualSideSpin = CuePlayParams.unitSideSpin(selectedSideSpin,
                    game.getCuingPlayer().getInGamePlayer().getCurrentCue(game));

            double actualPower = selectedPowerToActualPower(selectedPower, actualSideSpin, actualFbSpin,
                    attackChoice.handSkill);
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
                    actualFbSpin,
                    actualSideSpin,
                    0.0,
                    actualPower
            );
            // 直接能打到的球，必不会在打到目标球之前碰库
            WhitePrediction wp = game.predictWhite(params, phy, 0.0,
                    true, false, true);
            if (wp.getFirstCollide() == null) {
                // 连球都碰不到，没吃饭？
//            System.out.println("too less");
                return;
            }
            if (wp.getWhiteCushionCountAfter() == 0 && selectedSideSpin != 0.0) {
                // 不吃库的球加个卵的塞
                return;
            }

            double targetCanMove = values.estimatedMoveDistance(phy, wp.getBallInitSpeed());
            if (targetCanMove - values.ball.ballDiameter * 1.5 <= correctedChoice.targetHoleDistance) {
                // 确保球不会停在袋口
                // 如果小于，说明力量太轻或低杆太多，打不到
//            System.out.println("little less " + targetCanMove + ", " + attackChoice.targetHoleDistance);
                return;
            }
            if (wp.willCueBallPot()) {
                // 进白球也太蠢了吧
                return;
            }
            double[] whiteStopPos = wp.getWhitePath().get(wp.getWhitePath().size() - 1);
            if (game instanceof AbstractSnookerGame) {
                AbstractSnookerGame asg = (AbstractSnookerGame) game;
                asg.pickupPottedBallsLast(correctedChoice.attackTarget);
            }
            List<AttackChoice> nextStepAttackChoices =
                    getAttackChoices(game,
                            nextTarget,
                            aiPlayer,
                            wp.getFirstCollide(),
                            nextStepLegalBalls,
                            whiteStopPos,
                            false,
                            true,
                            attackThreshold);
            result = new IntegratedAttackChoice(
                    correctedChoice,
                    nextStepAttackChoices,
                    nextTarget,
                    playerSelfPrice,
                    params,
                    selectedPower,
                    selectedFrontBackSpin,
                    selectedSideSpin,
                    wp,
                    stage
            );
        }
    }

    protected class DefenseThread implements Runnable {

        double rad;
        double[] whitePos;
        double selectedPower;
        Set<Ball> legalSet;
        Phy phy;

        DefenseChoice result;

        protected DefenseThread(double rad, double[] whitePos,
                                double selectedPower,
                                Set<Ball> legalSet, Phy phy) {
            this.rad = rad;
            this.whitePos = whitePos;
            this.selectedPower = selectedPower;
            this.legalSet = legalSet;
            this.phy = phy;
        }

        @Override
        public void run() {
            double[] unitXY = Algebra.unitVectorOfAngle(rad);

            PlayerPerson.HandSkill handSkill = CuePlayParams.getPlayableHand(
                    whitePos[0],
                    whitePos[1],
                    unitXY[0],  // fixme: 这里存疑
                    unitXY[1],
                    game.getGameValues().table,
                    game.getCuingPlayer().getPlayerPerson()
            );

            CuePlayParams cpp = CuePlayParams.makeIdealParams(
                    unitXY[0],
                    unitXY[1],
                    0.0,
                    0.0,
                    0.0,
                    selectedPowerToActualPower(selectedPower, 0, 0, handSkill)
            );
            WhitePrediction wp = game.predictWhite(cpp, phy, 10000000.0,
                    true, true, false);
            double[] whiteStopPos = wp.getWhitePath().get(wp.getWhitePath().size() - 1);
            Ball firstCollide = wp.getFirstCollide();
            if (firstCollide != null && legalSet.contains(firstCollide)) {
                if (wp.willFirstBallPot()) {
                    wp.resetToInit();
                    return;
                }

                int opponentTarget = game.getTargetAfterPotFailed();
                List<Ball> opponentBalls = game.getAllLegalBalls(opponentTarget, false);

                Game.SeeAble seeAble = game.countSeeAbleTargetBalls(
                        whiteStopPos[0], whiteStopPos[1],
                        opponentBalls,
                        1
                );
                double penalty = 1.0;
                double opponentAttackPrice = AttackChoice.priceOfDistance(seeAble.avgTargetDistance);
                double snookerPrice = 1.0;

                if (wp.isFirstBallCollidesOther()) {
                    penalty *= 20;
                }
                if (wp.getSecondCollide() != null) {
                    penalty *= 20;
                }

                if (seeAble.seeAbleTargets == 0) {
                    snookerPrice = Math.sqrt(seeAble.maxShadowAngle) * 300;
                } else {
                    List<AttackChoice> attackChoices = getAttackChoices(
                            game,
                            opponentTarget,
                            game.getAnotherPlayer(aiPlayer),
                            null,
                            opponentBalls,
                            whiteStopPos,
                            true,
                            false,
                            ATTACK_DIFFICULTY_THRESHOLD
                    );

                    for (AttackChoice ac : attackChoices) {
                        opponentAttackPrice += ac.price;
                    }
                }

                if (wp.getWhiteCushionCountBefore() > 2) {
                    penalty *= (wp.getWhiteCushionCountBefore() - 1.5);
                }
                if (wp.getWhiteCushionCountAfter() > 3) {
                    penalty *= (wp.getWhiteCushionCountAfter() - 2.5);
                }
                if (wp.getFirstBallCushionCount() > 3) {
                    penalty *= (wp.getFirstBallCushionCount() - 2.5);
                }
                if (wp.isWhiteCollidesHoleArcs()) {
                    penalty /= WHITE_HIT_CORNER_PENALTY;
                }
                wp.resetToInit();
//            System.out.printf("%f %f %f\n", snookerPrice, opponentAttackPrice, penalty);
                result = new DefenseChoice(
                        firstCollide,
                        snookerPrice,
                        opponentAttackPrice,
                        penalty,
                        unitXY,
                        selectedPower,
                        0.0,
                        wp,
                        cpp,
                        handSkill
                );
                return;
            }
            wp.resetToInit();
        }
    }
}
