package trashsoftware.trashSnooker.core.ai;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.jetbrains.annotations.NotNull;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.movement.WhitePrediction;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.snooker.AbstractSnookerGame;
import trashsoftware.trashSnooker.util.ConfigLoader;
import trashsoftware.trashSnooker.util.Util;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// todo: 黑八半场自由球只能向下打，

public abstract class AiCue<G extends Game<?, P>, P extends Player> {

    public static final double ATTACK_DIFFICULTY_THRESHOLD = 18000.0;  // 越大，AI越倾向于进攻

    public static final double PURE_ATTACK_PROB = 0.4;  // 进攻权重为99的球员只要prob高于这个值他就会进攻。越小，AI越倾向于无脑进攻
    public static final double DEFENSIVE_ATTACK_PROB = 0.2;  // 这个值是线性的，进攻权重为99的球员高于这个值就会尝试性进攻

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

    protected int nThreads;
    protected G game;
    protected P aiPlayer;
    private double opponentPureAtkProb;  // 对手会直接进攻的界限
    private double opponentDefAtkProb;

    public AiCue(G game, P aiPlayer) {
        this.game = game;
        this.aiPlayer = aiPlayer;

        this.nThreads = Math.max(1,
                Math.min(32,
                        ConfigLoader.getInstance().getInt("nThreads", 4)));

        P opponent = game.getAnotherPlayer(aiPlayer);
        opponentPureAtkProb = attackProbThreshold(0.4, opponent.getPlayerPerson().getAiPlayStyle());  // todo
        opponentDefAtkProb = defensiveAttackProbThreshold(opponent.getPlayerPerson().getAiPlayStyle());
    }

    public static <G extends Game<?, ?>> List<AttackChoice> getAttackChoices(
            G game,
            int attackTarget,
            Player attackingPlayer,
            Ball lastPottingBall,
            List<Ball> legalBalls,
            double[] whitePos,
            boolean isPositioning
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
//                        if (countLowChoices) {
                        attackChoices.add(attackChoice);
//                        } else if (attackChoice.difficulty <
//                                attackThreshold *
//                                        attackingPlayer.getPlayerPerson().getAiPlayStyle().attackPrivilege /
//                                        100.0) {
//                            attackChoices.add(attackChoice);
//                        }
                    }
                }
            }
        }
        Collections.sort(attackChoices);
        return attackChoices;
    }

    private static double attackProbThreshold(double base, AiPlayStyle aps) {
        if (aps.attackPrivilege == 100) return 0.000001;  // 管他娘的
        else {
            double room = 1.0 - base;
            double playerNotWantAttack = 1 - aps.attackPrivilege / 100;
            playerNotWantAttack = Math.pow(playerNotWantAttack, 0.75);  // 无奈之举。次幂越小，进攻权重低的球手越不进攻
            return base + playerNotWantAttack * room;
        }
    }

    private static double defensiveAttackProbThreshold(AiPlayStyle aps) {
        double room = 1 - DEFENSIVE_ATTACK_PROB;
        return DEFENSIVE_ATTACK_PROB + (1 - aps.attackPrivilege / 100) * room;
    }

    protected static double selectedPowerToActualPower(Game<?, ?> game,
                                                       InGamePlayer aiIgp,
                                                       double selectedPower,
                                                       double unitCuePointX, double unitCuePointY,
                                                       PlayerPerson.HandSkill handSkill) {
        double mul = Util.powerMultiplierOfCuePoint(unitCuePointX, unitCuePointY);
        double handMul = handSkill == null ? 1.0 : PlayerPerson.HandBody.getPowerMulOfHand(handSkill);
        return selectedPower * handMul * mul *
                aiIgp.getCurrentCue(game).powerMultiplier /
                game.getGameValues().ball.ballWeightRatio;
    }

    protected static DefenseChoice analyseDefense(
            AiCue<?, ?> aiCue,
            CuePlayParams cpp,
            PlayerPerson.HandSkill handSkill,
            Phy phy,
            Game<?, ?> copy,
            Set<Ball> legalSet,
            Player aiPlayer,
            double[] unitXY,
            double selectedPower,
            double selectedFrontBackSpin,
            double selectedSideSpin,
            boolean attackAble  // 可不可以进
    ) {
        WhitePrediction wp = copy.predictWhite(cpp, phy, 10000000.0,
                true, true, false,
                false);  // 这里不用clone，因为整个game都是clone的
        double[] whiteStopPos = wp.getWhitePath().get(wp.getWhitePath().size() - 1);
        Ball firstCollide = wp.getFirstCollide();
        if (firstCollide != null && legalSet.contains(firstCollide)) {
            if (wp.willCueBallPot()) {
                wp.resetToInit();
                return null;
            }
            if (!attackAble && wp.willFirstBallPot()) {
                wp.resetToInit();
                return null;
            }

            int opponentTarget = copy.getTargetAfterPotFailed();
            List<Ball> opponentBalls = copy.getAllLegalBalls(opponentTarget, false,
                    copy.isInLineHandBall());

            Game.SeeAble seeAble = copy.countSeeAbleTargetBalls(
                    whiteStopPos[0], whiteStopPos[1],
                    opponentBalls,
                    1
            );
            double penalty = 1.0;
//            double opponentAttackPrice = 1.0;
            double opponentAttackPrice = AttackChoice.priceOfDistance(seeAble.avgTargetDistance);
//            double opponentAttackPrice2 = AttackChoice.priceOfDistance(seeAble.avgTargetDistance);
            double snookerPrice = 1.0;

            if (wp.isFirstBallCollidesOther()) {
                penalty *= 16;
            }
            if (wp.getSecondCollide() != null) {
                penalty *= 30;
            }

            AttackChoice oppoEasiest = null;
            if (seeAble.seeAbleTargets == 0) {
                // 这个权重如果太大，AI会不计惩罚地去做斯诺克
                // 如果太小，AI会不做斯诺克
                snookerPrice = Math.sqrt(seeAble.maxShadowAngle) * 50.0;
            } else {
                List<AttackChoice> attackChoices = getAttackChoices(
                        copy,
                        opponentTarget,
                        copy.getAnotherPlayer(aiPlayer),
                        null,
                        opponentBalls,
                        whiteStopPos,
                        false
                );

                for (AttackChoice ac : attackChoices) {
                    // ac.defaultRef.price一般在1以下
                    // 这个权重一般最后的合也就几十，可能二三十最多了
//                    opponentAttackPrice += ac.defaultRef.price * 3.0;
//                    opponentAttackPrice2 += ac.price;

                    double potProb = ac.defaultRef.potProb;
                    if (potProb > aiCue.opponentPureAtkProb) {
                        opponentAttackPrice += 15.0 * potProb;  // 大卖
                    } else if (potProb > aiCue.opponentDefAtkProb) {
                        opponentAttackPrice += 5.0 * potProb;  // 小卖
                    } else {
                        opponentAttackPrice += potProb;
                    }

                    if (oppoEasiest == null || potProb > oppoEasiest.defaultRef.potProb) {
                        oppoEasiest = ac;
                    }
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
//            System.out.printf("%f %f %f %f\n", snookerPrice, opponentAttackPrice, opponentAttackPrice2, penalty);
            return new DefenseChoice(
                    firstCollide,
                    snookerPrice,
                    opponentAttackPrice,
                    penalty,
                    unitXY,
                    selectedPower,
                    selectedFrontBackSpin,
                    selectedSideSpin,
                    wp,
                    cpp,
                    handSkill,
                    oppoEasiest,
                    wp.getSecondCollide() != null,
                    wp.isFirstBallCollidesOther()
            );
        }
        wp.resetToInit();
        return null;
    }

    protected static double selectedSideSpinToActual(double selectedSideSpin, Cue cue) {
        return CuePlayParams.unitSideSpin(selectedSideSpin, cue);
    }

    public abstract AiCueResult makeCue(Phy phy);

    /**
     * 开球
     */
    protected abstract DefenseChoice breakCue(Phy phy);

    protected abstract DefenseChoice standardDefense();

    protected abstract boolean supportAttackWithDefense(int targetRep);

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
            price += 20000.0 / potDifficulty;
        }
        return price;
    }

    protected DefenseChoice solveSnooker(Phy phy) {
        int curTarget = game.getCurrentTarget();
        boolean isSnookerFreeBall = game.isDoingSnookerFreeBll();
        List<Ball> legalBalls = game.getAllLegalBalls(curTarget,
                isSnookerFreeBall, 
                false);  // 不可能，没有谁会给自己摆杆斯诺克

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
        return selectedPowerToActualPower(game, aiPlayer.getInGamePlayer(),
                selectedPower, unitCuePointX, unitCuePointY,
                handSkill);
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

    protected double selectedFrontBackSpinToActual(double selectedFrontBackSpin) {
        return CuePlayParams.unitFrontBackSpin(selectedFrontBackSpin,
                aiPlayer.getInGamePlayer(),
                game.getCuingPlayer().getInGamePlayer().getCurrentCue(game));
    }

    protected double selectedSideSpinToActual(double selectedSideSpin) {
        return selectedSideSpinToActual(selectedSideSpin,
                game.getCuingPlayer().getInGamePlayer().getCurrentCue(game));
    }

    private IntegratedAttackChoice attack(AttackChoice choice,
                                          int nextTarget,
                                          List<Ball> nextStepLegalBalls,
                                          Phy phy) {
        double powerLimit = aiPlayer.getPlayerPerson().getControllablePowerPercentage();
        final double tick = 300.0 / aiPlayer.getPlayerPerson().getAiPlayStyle().position;

//        GameValues values = game.getGameValues();

//        double comfortablePower = powerLimit * 0.35;
//        int maxIterations = (int) Math.round((powerLimit - comfortablePower) / tick) * 2;  // 注：仅适用于倍数<=0.5的情况

        List<IntegratedAttackChoice> choiceList = new ArrayList<>();
        AiPlayStyle aps = aiPlayer.getPlayerPerson().getAiPlayStyle();
//        double likeShow = aiPlayer.getPlayerPerson().getAiPlayStyle().likeShow;  // 喜欢大力及杆法的程度
        GamePlayStage stage = game.getGamePlayStage(choice.ball, false);

        long t0 = System.currentTimeMillis();

        double pureAttackThreshold = attackProbThreshold(PURE_ATTACK_PROB, aps);  // 进球概率高于这个值，AI就纯进攻
        double defensiveAttackThreshold = defensiveAttackProbThreshold(aps);

        List<AttackParam> pureAttacks = new ArrayList<>();
        List<AttackParam> defensiveAttacks = new ArrayList<>();  // 连打带防

        for (double selectedPower = tick; selectedPower <= powerLimit; selectedPower += tick) {
            for (double[] spins : SPIN_POINTS) {
                AttackParam acp = new AttackParam(
                        choice, game, aiPlayer, selectedPower, spins[0], spins[1]
                );
                if (acp.potProb > pureAttackThreshold) {
                    pureAttacks.add(acp);
                } else if (acp.potProb > defensiveAttackThreshold) {
                    defensiveAttacks.add(acp);
                }
            }
        }

        long t1 = System.currentTimeMillis();
        System.out.println("Ai list attack params in " + (t1 - t0) + " ms, " +
                pureAttacks.size() + " pure, " + defensiveAttacks.size() + " defensive.");

        List<AttackThread> attackThreads = new ArrayList<>();
        for (AttackParam pureAttack : pureAttacks) {
            AttackThread thread = new AttackThread(
                    pureAttack,
                    game.getGameValues(),
                    nextTarget,
                    nextStepLegalBalls,
                    phy,
                    stage
            );
            attackThreads.add(thread);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
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
        System.out.println("Ai calculated attacks of given choice in " + (t2 - t1) + " ms");

        for (AttackThread thread : attackThreads) {
            if (thread.result != null) choiceList.add(thread.result);
        }

        if (!choiceList.isEmpty()) {
            choiceList.sort(IntegratedAttackChoice::normalCompareTo);
            for (IntegratedAttackChoice iac : choiceList) {
                if (!iac.nextStepAttackChoices.isEmpty()) {
                    return choiceList.get(0);  // 没问题
                }
            }

            IntegratedAttackChoice iac = choiceList.get(0);
            if (iac.nextStepTarget == Game.END_REP) return iac;
            
            if (iac.priceOfKick >= 1.0) {  // 能K正确的球
                if (Math.random() < 0.5 + (iac.priceOfKick - 1) * 0.25) {
                    // priceOfKick是1，概率为0.5
                    // priceOfKick是2，概率为0.75
                    // 大于等于3，必定k
                    return iac;
                }
            }

//            if (iac.nextStepTarget != Game.END_REP && iac.whitePrediction.getSecondCollide() == null)
//                return null;  // 打进了没位，打什么打
//
//            return iac;  // 这是最烂的结果
        }

        if (defensiveAttacks.isEmpty() || !supportAttackWithDefense(choice.attackTarget))
            return null;

        // 研究连打带跑
        List<DefensiveAttackThread> defensiveThreads = new ArrayList<>();
        Game[] gameClonesPool = new Game[nThreads];
        for (int i = 0; i < gameClonesPool.length; i++) {
            gameClonesPool[i] = game.clone();
        }

        List<Ball> legalBalls = game.getAllLegalBalls(game.getCurrentTarget(),
                game.isDoingSnookerFreeBll(),
                game.isInLineHandBall());
        Set<Ball> legalSet = new HashSet<>(legalBalls);

        for (AttackParam choiceWithParam : defensiveAttacks) {
            DefensiveAttackThread dat = new DefensiveAttackThread(
                    choiceWithParam,
                    choice.whitePos,
                    legalSet,
                    phy,
                    gameClonesPool,
                    game.getGameValues()
            );
            defensiveThreads.add(dat);
        }

        ExecutorService executorService2 = Executors.newFixedThreadPool(nThreads);
        for (DefensiveAttackThread thread : defensiveThreads) {
            executorService2.execute(thread);
        }

        executorService2.shutdown();

        try {
            if (!executorService2.awaitTermination(1, TimeUnit.MINUTES))
                throw new RuntimeException("AI thread not terminated.");  // Wait for all threads complete.
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        DefensiveAttackThread bestDefense = null;

        for (DefensiveAttackThread thread : defensiveThreads) {
            if (thread.result != null && notViolateCushionRule(thread.result)) {
                if (bestDefense == null || thread.result.compareTo(bestDefense.result) < 0) {
                    bestDefense = thread;
                }
            }
        }
        if (bestDefense != null) {
//            if (bestDefense.result.opponentEasiestChoice != null) {
//                if (bestDefense.attackParam.potProb <= bestDefense.result.opponentEasiestChoice.defaultRef.potProb) {
//                    return null;  // 对手的进球率都比这颗球高，打锤子。注：是按球手自己的技术生成的
//                }
//            }

            System.out.println("Best defensive: " + bestDefense.result);
            return new IntegratedAttackChoice(
                    bestDefense.attackParam,
                    nextTarget,
                    bestDefense.result.cuePlayParams,
                    stage,
                    bestDefense.result.price
            );
        } else {
            System.out.println("No defensive attacks");
        }

        return null;
    }

    protected AiCueResult makeAttackCue(IntegratedAttackChoice iac) {
        AttackParam attackParam = iac.attackParams;
        AttackChoice attackChoice = attackParam.attackChoice;
        return new AiCueResult(aiPlayer.getInGamePlayer(),
                game.getGamePlayStage(attackChoice.ball, true),
                AiCueResult.CueType.ATTACK,
                attackChoice.targetOrigPos,
                attackChoice.dirHole,
                attackChoice.ball,
                attackChoice.cueDirectionUnitVector[0],
                attackChoice.cueDirectionUnitVector[1],
                attackParam.selectedFrontBackSpin,
                attackParam.selectedSideSpin,
                attackParam.selectedPower,
                attackChoice.handSkill,
                game.frameImportance(aiPlayer.getInGamePlayer().getPlayerNumber()),
                game.getEntireGame().rua(aiPlayer.getInGamePlayer()));
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
                choice.handSkill,
                game.frameImportance(aiPlayer.getInGamePlayer().getPlayerNumber()),
                game.getEntireGame().rua(aiPlayer.getInGamePlayer()));
    }

    protected AiCueResult regularCueDecision(Phy phy) {
        if (game.isBreaking()) {
            DefenseChoice breakChoice = breakCue(phy);
            if (breakChoice != null) return makeDefenseCue(breakChoice, AiCueResult.CueType.BREAK);
        }

        if (!aiOnlyDefense) {
            IntegratedAttackChoice attackChoice = standardAttack(phy);
            if (attackChoice != null) {
                return makeAttackCue(attackChoice);
            }
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

    protected IntegratedAttackChoice standardAttack(Phy phy) {
        List<AttackChoice> attackChoices =
                (aiOnlyDefense && game.getCurrentTarget() != 1) ?
                        new ArrayList<>() : getCurrentAttackChoices();
        return attackGivenChoices(attackChoices, phy);
    }

    protected IntegratedAttackChoice attackGivenChoices(List<AttackChoice> attackChoices,
                                                        Phy phy) {
        System.out.println("Simple attack choices:" + attackChoices.size());
//        System.out.println(attackAttackChoices);
        if (!attackChoices.isEmpty()) {
            IntegratedAttackChoice best = null;
            for (int i = 0; i < Math.min(2, attackChoices.size()); i++) {
                AttackChoice choice = attackChoices.get(i);
                int nextTargetIfThisSuccess = game.getTargetAfterPotSuccess(choice.ball,
                        game.isDoingSnookerFreeBll());
                List<Ball> nextStepLegalBalls =
                        game.getAllLegalBalls(nextTargetIfThisSuccess,
                                false,
                                false);  // 这颗进了下一颗怎么可能是自由球/手中球

                if (game.getGameType() == GameRule.CHINESE_EIGHT) {
                    // 避免AI打自己较自己的可能（并不确定会发生）
                    nextStepLegalBalls.remove(choice.ball);
                }

                IntegratedAttackChoice iac = attack(choice, nextTargetIfThisSuccess, nextStepLegalBalls, phy);
                if (best == null || iac != null) {
                    if (best == null) {
                        best = iac;
                    } else {
                        if (best.isPureAttack) {
                            if (iac.isPureAttack && iac.price > best.price) {
                                best = iac;  // 只有在iac是更好的纯攻时才替换
                            }
                        } else {
                            if (iac.isPureAttack) {
                                best = iac;  // 纯攻比打带跑好
                            } else if (iac.price > best.price) {
                                best = iac;  // iac是更好的打带跑
                            }
                        }
                    }
                }
            }
            if (best != null) {
                System.out.printf("Best int attack choice: %s, dir %f, %f, power %f, spins %f, %f, pot prob, %f \n",
                        best.isPureAttack ? "pure" : "defensive",
                        best.attackParams.attackChoice.cueDirectionUnitVector[0],
                        best.attackParams.attackChoice.cueDirectionUnitVector[1],
                        best.attackParams.selectedPower,
                        best.attackParams.selectedFrontBackSpin,
                        best.attackParams.selectedSideSpin,
                        best.attackParams.potProb);
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
                handSkill,
                game.frameImportance(aiPlayer.getInGamePlayer().getPlayerNumber()),
                game.getEntireGame().rua(aiPlayer.getInGamePlayer())
        );
    }

    private List<AttackChoice> getAttackChoices(int attackTarget,
                                                Ball lastPottingBall,
                                                boolean isSnookerFreeBall,
                                                double[] whitePos,
                                                boolean isPositioning,
                                                boolean isLineInHandBall) {
        return getAttackChoices(game,
                attackTarget,
                aiPlayer,
                lastPottingBall,
                game.getAllLegalBalls(attackTarget, isSnookerFreeBall, isLineInHandBall),
                whitePos,
                isPositioning
        );
    }

    protected List<AttackChoice> getCurrentAttackChoices() {
        return getAttackChoices(game.getCurrentTarget(),
                null,
                game.isDoingSnookerFreeBll(),
                new double[]{game.getCueBall().getX(), game.getCueBall().getY()},
                false,
                game.isInLineHandBallForAi());
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

        Set<Ball> legalSet = new HashSet<>(legalBalls);
        DefenseChoice best = null;
        double selPowLow = actualPowerToSelectedPower(actualPowerLow, 0, 0, null);
        double selPowHigh = actualPowerToSelectedPower(actualPowerHigh, 0, 0, null);

        List<DefenseThread> defenseThreads = new ArrayList<>();
        Game[] gameClonesPool = new Game[nThreads];
        for (int i = 0; i < gameClonesPool.length; i++) {
            gameClonesPool[i] = game.clone();
        }

        for (double selectedPower = selPowLow;
             selectedPower < selPowHigh;
             selectedPower += realPowerTick) {

            for (Double rad : availableRads) {
                DefenseThread thread = new DefenseThread(
                        rad, whitePos, selectedPower, legalSet, phy, gameClonesPool
                );
                defenseThreads.add(thread);
            }
        }

        System.out.println(availableRads.size() + " defense angles, " + defenseThreads.size() + " defenses");

        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        for (DefenseThread thread : defenseThreads) {
            executorService.execute(thread);
        }

        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(1, TimeUnit.MINUTES))
                throw new RuntimeException("AI thread not terminated.");  // Wait for all threads complete.
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (DefenseThread thread : defenseThreads) {
            if (thread.result != null && notViolateCushionRule(thread.result)) {
                if (best == null || thread.result.compareTo(best) < 0) {
                    best = thread.result;
                }
//                if (thread.result != null) {
//                    System.out.println("defense: " + thread.result);
//                }
            }
        }

        if (best != null) {
            System.out.println("Best defense: " + best);
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
//        DefenseChoice best = null;

        Ball cueBall = game.getCueBall();
        double[] whitePos = new double[]{cueBall.getX(), cueBall.getY()};

        List<DefenseThread> defenseThreads = new ArrayList<>();
        Game[] gameClonesPool = new Game[nThreads];
        for (int i = 0; i < gameClonesPool.length; i++) {
            gameClonesPool[i] = game.clone();
        }

        for (double selectedPower = 5.0;
             selectedPower < aiPlayer.getPlayerPerson().getControllablePowerPercentage();
             selectedPower += powerTick) {
            for (double deg = 0.0; deg < 360; deg += degreesTick) {
                DefenseThread thread = new DefenseThread(
                        Math.toRadians(deg), whitePos, selectedPower, legalSet, phy, gameClonesPool
                );
                defenseThreads.add(thread);
            }
        }

        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        for (DefenseThread thread : defenseThreads) {
            executorService.execute(thread);
        }

        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(1, TimeUnit.MINUTES))
                throw new RuntimeException("AI thread not terminated.");  // Wait for all threads complete.
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<DefenseChoice> legalChoices = new ArrayList<>();
        for (DefenseThread thread : defenseThreads) {
            if (thread.result != null && notViolateCushionRule(thread.result)) {
                legalChoices.add(thread.result);
//                if (best == null || thread.result.compareTo(best) < 0) {
//                    best = thread.result;
//                }
            }
        }
        if (legalChoices.isEmpty()) return null;
        Collections.sort(legalChoices);
        Collections.reverse(legalChoices);
        
        if (legalChoices.size() == 1) return legalChoices.get(0);
        else if (legalChoices.size() == 2) return Math.random() > 0.3 ? legalChoices.get(0) : legalChoices.get(1);
        
        List<DefenseChoice> bests = legalChoices.subList(0, Math.min(3, legalChoices.size()));
        double rnd = Math.random();
        if (rnd > 0.4) return bests.get(0);
        else if (rnd > 0.1) return bests.get(1);
        else return bests.get(2);
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
                isSnookerFreeBall,
                game.isInLineHandBallForAi());

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
        //        protected double difficulty;
        protected boolean isPositioning;  // 是走位预测还是进攻
        protected double[] whitePos;
        protected double[][] dirHole;
        protected double[] targetOrigPos;
        protected double[] targetHoleVec;
        protected double[] holePos;  // 洞口瞄准点的坐标，非洞底
        protected double[] cueDirectionUnitVector;
        protected PlayerPerson.HandSkill handSkill;
        protected AttackParam defaultRef;
        int attackTarget;
        double targetPrice;
//        transient double difficulty;
//        transient double price;

        private AttackChoice() {
        }

        /**
         * @param lastAiPottedBall 如果这杆为走位预测，则该值为AI第一步想打的球。如这杆就是第一杆，则为null
         */
        public static AttackChoice createChoice(Game<?, ?> game,
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
//            attackChoice.calculateDifficulty();

            attackChoice.targetPrice = game.priceOfTarget(attackTarget, ball, attackingPlayer, lastAiPottedBall);

            // 随便创建一个，用于评估难度
            attackChoice.defaultRef = new AttackParam(
                    attackChoice,
                    game,
                    attackingPlayer,
                    30.0,
                    0.0,
                    0.0
            );

            return attackChoice;
        }

        static double priceOfDistance(double distance) {
            double difficulty = distance * 2;
            return 10000.0 / difficulty;  // Refer to the class's constructor
        }

        protected static double holeProjWidth(GameValues values,
                                              boolean isMidHole,
                                              double[] targetHoleVec) {
            if (isMidHole) {
                return Math.abs(targetHoleVec[1]) * values.table.midHoleDiameter;
            } else {
                double holeMax = values.table.cornerHoleDiameter;
                // 要的只是个0-90之间的角度（弧度），与45度对称即可（15===75），都转到第一象限来
                double rad = Math.atan2(Math.abs(targetHoleVec[0]), Math.abs(targetHoleVec[1]));
                double angleTo45 = rad > Algebra.QUARTER_PI ?
                        rad - Algebra.QUARTER_PI :
                        Algebra.QUARTER_PI - rad;
                return holeMax * Math.cos(angleTo45);
            }
        }
        
        protected static double allowedDeviationOfHole(GameValues values,
                                                       boolean isMidHole,
                                                       double[] targetHoleVec) {
            double holeWidth = AttackChoice.holeProjWidth(values,
                    isMidHole,
                    targetHoleVec);
            // 从这个角度看袋允许的偏差
            return holeWidth - values.ball.ballDiameter * 0.95;  // 0.95是随手写的
        }

        protected static double holeDifficulty(Game<?, ?> game,
                                               boolean isMidHole,
                                               double[] targetHoleVec) {
            
            double allowedDev = allowedDeviationOfHole(game.getGameValues(), isMidHole, targetHoleVec);
            if (isMidHole) {
                return game.getGameValues().midHoleBestAngleWidth / allowedDev;
            } else {
                return game.getGameValues().cornerHoldBestAngleWidth / allowedDev;
            }
            
//            if (isMidHole) {
////                double midHoleOffset = Math.abs(targetHoleVec[1]);  // 单位向量的y值绝对值越大，这球越简单
////                return 1 / Math.pow(midHoleOffset, 1.4);  // 次幂可以调，越小，ai越愿意打中袋
//                // 基本上就是往中袋的投影占比
//                double holeProjWidth = Math.abs(targetHoleVec[1]) * game.getGameValues().table.midHoleDiameter;
//                double errorToleranceWidth = holeProjWidth - game.getGameValues().ball.ballRadius;
//                errorToleranceWidth = Math.max(errorToleranceWidth, 0.00001);
//                return game.getGameValues().midHoleBestAngleWidth / errorToleranceWidth;
//            } else {
//                // 底袋，45度时难度系数为1，0度或90度时难度系数最大，为 sqrt(2)/2 * 袋直径 - 球半径 的倒数
//                double easy = 1 - Math.abs(Math.abs(targetHoleVec[0]) - Math.abs(targetHoleVec[1]));  // [0,1]
//                double range = 1 - game.getGameValues().cornerHoleAngleRatio;
//                return 1 / (easy * range + game.getGameValues().cornerHoleAngleRatio);
//            }
        }

        public Ball getBall() {
            return ball;
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
//            copied.difficulty = difficulty;
//            copied.price = price;
            copied.targetPrice = targetPrice;
            copied.handSkill = handSkill;
            copied.defaultRef = defaultRef;

            return copied;
        }

        private boolean isMidHole() {
            return holePos[0] == game.getGameValues().table.midX;
        }

        @Override
        public int compareTo(@NotNull AiCue.AttackChoice o) {
            return -Double.compare(this.defaultRef.price, o.defaultRef.price);
        }

        @Override
        public String toString() {
            return "AttackChoice{" +
                    "ball=" + ball +
                    ", game=" + game +
                    ", angleRad=" + angleRad +
                    ", targetHoleDistance=" + targetHoleDistance +
                    ", whiteCollisionDistance=" + whiteCollisionDistance +
//                    ", difficulty=" + difficulty +
                    ", price=" + targetPrice +
                    ", targetHoleVec=" + Arrays.toString(targetHoleVec) +
                    ", holePos=" + Arrays.toString(holePos) +
                    ", cueDirectionUnitVector=" + Arrays.toString(cueDirectionUnitVector) +
                    '}';
        }
    }

    public static class FinalChoice {

    }

    public static class DefenseChoice extends FinalChoice implements Comparable<DefenseChoice> {

        private final double penalty;
        protected PlayerPerson.HandSkill handSkill;
        protected Ball ball;
        protected double snookerPrice;
        protected double opponentAttackPrice;
        protected double price;  // price还是越大越好
        protected double[] cueDirectionUnitVector;  // selected

        double selectedPower;
        double selectedFrontBackSpin;
        double selectedSideSpin;

        CuePlayParams cuePlayParams;
        WhitePrediction wp;
        AttackChoice opponentEasiestChoice;

        boolean whiteCollidesOther;
        boolean targetCollidesOther;

        protected DefenseChoice(Ball ball,
                                double snookerPrice,
                                double opponentAttackPrice,
                                double penalty,
                                double[] cueDirectionUnitVector,
                                double selectedPower,
                                double selectedFrontBackSpin,
                                double selectedSideSpin,
                                WhitePrediction wp,
                                CuePlayParams cuePlayParams,
                                PlayerPerson.HandSkill handSkill,
                                AttackChoice opponentEasiestChoice,
                                boolean whiteCollidesOther,
                                boolean targetCollidesOther) {
            this.ball = ball;
            this.snookerPrice = snookerPrice;
            this.opponentAttackPrice = opponentAttackPrice;
            this.penalty = penalty;

//            this.collideOtherBall = collideOtherBall;
            this.cueDirectionUnitVector = cueDirectionUnitVector;
            this.selectedPower = selectedPower;
            this.selectedFrontBackSpin = selectedFrontBackSpin;
            this.selectedSideSpin = selectedSideSpin;
            this.cuePlayParams = cuePlayParams;
            this.wp = wp;
            this.handSkill = handSkill;
            this.opponentEasiestChoice = opponentEasiestChoice;

            this.whiteCollidesOther = whiteCollidesOther;
            this.targetCollidesOther = targetCollidesOther;

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
                    0.0,
                    selectedSideSpin,
                    null,
                    cuePlayParams,
                    handSkill,
                    null,
                    true,
                    true);
        }

        private void generatePrice() {
            this.price = snookerPrice / penalty - opponentAttackPrice * penalty;

            if (wp != null && wp.isHitWallBeforeHitBall()) {
                // 应该是在解斯诺克
                this.price /= (wp.getDistanceTravelledBeforeCollision() / 1000);  // 不希望白球跑太远
            }

        }

        @Override
        public int compareTo(@NotNull AiCue.DefenseChoice o) {
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

    public static class AttackParam {

        double potProb;  // 正常情况下能打进的概率
        double price;  // 对于球手来说的价值
        AttackChoice attackChoice;

        double selectedPower;
        double selectedFrontBackSpin;
        double selectedSideSpin;

        private AttackParam(AttackParam base, AttackChoice replacement) {
            this.attackChoice = replacement;
            this.price = base.price;
            this.potProb = base.potProb;
            this.selectedPower = base.selectedPower;
            this.selectedFrontBackSpin = base.selectedFrontBackSpin;
            this.selectedSideSpin = base.selectedSideSpin;
        }

        protected AttackParam(AttackChoice attackChoice,
                              Game<?, ?> game,
                              Player aiPlayer,
                              double selectedPower,
                              double selectedFrontBackSpin,
                              double selectedSideSpin) {
            this.attackChoice = attackChoice;
            this.selectedPower = selectedPower;
            this.selectedFrontBackSpin = selectedFrontBackSpin;
            this.selectedSideSpin = selectedSideSpin;

            GameValues gameValues = game.getGameValues();

            PlayerPerson playerPerson = attackChoice.attackingPlayer.getPlayerPerson();
            AiPlayStyle aps = playerPerson.getAiPlayStyle();
            double handSdMul = PlayerPerson.HandBody.getSdOfHand(attackChoice.handSkill);

//            double[] muSigXy = playerPerson.getCuePointMuSigmaXY();
//            double sideSpinSd = muSigXy[1];  // 左右打点的标准差，mm
            double powerErrorFactor = playerPerson.getErrorMultiplierOfPower(selectedPower);
            double powerSd = (100.0 - playerPerson.getPowerControl()) / 100.0;
            powerSd *= attackChoice.attackingPlayer.getInGamePlayer().getPlayCue().powerMultiplier;
            powerSd *= handSdMul;
            powerSd *= powerErrorFactor;  // 力量的标准差

            double actualPower = selectedPowerToActualPower(game,
                    aiPlayer.getInGamePlayer(),
                    selectedPower,
                    selectedSideSpin,
                    selectedFrontBackSpin,
                    attackChoice.handSkill);

            // 不考虑出杆: 球员决定进不进攻的时候肯定不会想自己出杆会歪
//            double[] muSigXy = playerPerson.getCuePointMuSigmaXY();
//            double cueSd = muSigXy[1];  // 左右打点的标准差，mm
//            double unitCueSd = cueSd / gameValues.ball.ballRadius;

//            double sideSpinWithMaxError = selectedSideSpin >= 0 ?
//                    selectedSideSpin + unitCueSd :
//                    selectedSideSpin - unitCueSd;

            // 和杆还是有关系的，拿着大头杆打斯诺克就不会去想很难的球
            double actualSideSpin = selectedSideSpinToActual(selectedSideSpin, 
                    aiPlayer.getInGamePlayer().getPlayCue());

            // dev=deviation, 由于力量加上塞造成的1倍标准差偏差角，应为小于PI的正数
            double[] devOfLowPower = CuePlayParams.unitXYWithSpins(actualSideSpin,
                    actualPower * (1 - powerSd), 1, 0);
            double radDevOfLowPower = Algebra.thetaOf(devOfLowPower);
            if (radDevOfLowPower > Math.PI)
                radDevOfLowPower = Algebra.TWO_PI - radDevOfLowPower;

            double[] devOfHighPower = CuePlayParams.unitXYWithSpins(actualSideSpin,
                    actualPower * (1 + powerSd), 1, 0);
            double radDevOfHighPower = Algebra.thetaOf(devOfHighPower);
            if (radDevOfHighPower > Math.PI)
                radDevOfHighPower = Algebra.TWO_PI - radDevOfHighPower;

            double sideDevRad = (radDevOfHighPower - radDevOfLowPower) / 2;
            
            // 太小的力有惩罚
            if (actualPower < 15.0) {
                double penalty = Algebra.shiftRange(0, 15, 2, 1, actualPower);
                sideDevRad *= penalty;
            }

            // 瞄准的1倍标准差偏差角
            double aimingSd = (100 - aps.precision) * handSdMul /
                    AiCueResult.DEFAULT_AI_PRECISION;  // 这里用default是因为，我们不希望把AI精确度调低之后它就觉得打不进，一直防守

            // 白球的偏差标准差
            double whiteBallDevRad = sideDevRad + aimingSd;
            // 白球在撞击点时的偏差标准差，毫米
            // 这里sin和tan应该差不多，都不准确，tan稍微好一点点
            double sdCollisionMm = Math.tan(whiteBallDevRad) * attackChoice.whiteCollisionDistance;

            // 目标球出发角的大致偏差，标准差。 todo: 目前的算法导致了AI认为近乎90度的薄球不难
            double tarDevSdRad = Math.asin(sdCollisionMm / gameValues.ball.ballDiameter);

            // todo: 1 / cos是权宜之计
            tarDevSdRad *= 1 / Math.cos(attackChoice.angleRad);
            if (tarDevSdRad > Algebra.HALF_PI) {
                tarDevSdRad = Algebra.HALF_PI;
            }

            // 目标球到袋时的大致偏差标准差，mm
            double tarDevHoleSdMm = Math.tan(tarDevSdRad) * attackChoice.targetHoleDistance;

            // 角度球的瞄准难度
            double targetAimingOffset =
                    Math.cos(Math.PI / 2 - attackChoice.angleRad) * attackChoice.targetHoleDistance;
            // 举个例子，瞄准为90的AI，白球在右顶袋打蓝球右底袋时，这个值在24毫米左右
            double targetDifficultyMm = targetAimingOffset * (100 - aps.precision) / 750;

            tarDevHoleSdMm += targetDifficultyMm;
//            tarDevHoleSdMm *= targetDifficulty;
            
            // 从这个角度看袋允许的偏差
            double allowedDev = AttackChoice.allowedDeviationOfHole(
                    gameValues,
                    attackChoice.isMidHole(),
                    attackChoice.targetHoleVec
            );
            NormalDistribution nd = new NormalDistribution(0.0, Math.max(tarDevHoleSdMm * 2, 0.00001));

            potProb = nd.cumulativeProbability(allowedDev) - nd.cumulativeProbability(-allowedDev);
            if (potProb < 0) potProb = 0.0;  // 虽然我不知道为什么prob会是负的

//            double threshold = attackProbThreshold(PURE_ATTACK_PROB, aiPlayer.getPlayerPerson().getAiPlayStyle());
//            double room = 1 - threshold;
//            price = (potProb - threshold) / room * attackChoice.targetPrice;  // 对于他自己来说的把握
//            if (price < 0) price = 0;
            price = potProb * attackChoice.targetPrice;

//            System.out.println("Est dev: " + tarDevHoleSdMm +
//                    ", allow dev: " + allowedDev +
//                    ", prob: " + potProb +
//                    ", price: " + price +
//                    ", power: " + selectedPower +
//                    ", spins: " + selectedFrontBackSpin + ", " + selectedSideSpin +
//                    ", side dev: " + Math.toDegrees(sideDevRad));
        }

        protected AttackParam copyWithCorrectedChoice(AttackChoice corrected) {
            return new AttackParam(this, corrected);
        }
    }

    public class IntegratedAttackChoice extends FinalChoice {

        public final boolean isPureAttack;
        final AttackParam attackParams;
        final List<AttackChoice> nextStepAttackChoices;
        private final WhitePrediction whitePrediction;
        private final GamePlayStage stage;
        protected double price;
        int nextStepTarget;
        CuePlayParams params;
        double priceOfKick = 0.0;

        protected IntegratedAttackChoice(
//                AttackChoice attackChoice,
                AttackParam attackParams,
                List<AttackChoice> nextStepAttackChoices,
                int nextStepTarget,
                CuePlayParams params,
                WhitePrediction whitePrediction,
                GamePlayStage stage
        ) {
            this.attackParams = attackParams;
            this.nextStepAttackChoices = nextStepAttackChoices;
            this.nextStepTarget = nextStepTarget;
            this.whitePrediction = whitePrediction;
            this.stage = stage;
            this.params = params;
            isPureAttack = true;

            generatePrice();
        }

        /**
         * 由defense转来的，连攻带防，但不能与纯进攻的一起比较
         */
        protected IntegratedAttackChoice(AttackParam attackParams,
                                         int nextStepTarget,
                                         CuePlayParams params,
                                         GamePlayStage stage,
                                         double price) {
            this.attackParams = attackParams;
            this.nextStepAttackChoices = new ArrayList<>();
            this.whitePrediction = null;
            this.params = params;
            this.nextStepTarget = nextStepTarget;
            this.stage = stage;

            this.price = price;

            isPureAttack = false;
        }

        int normalCompareTo(IntegratedAttackChoice o2) {
            return -Double.compare(this.price, o2.price);
        }

        private void generatePrice() {
            price = attackParams.price;  // 这颗球本身的价值
            // 走位粗糙的人，下一颗权重低
            double mul = 0.5 *
                    attackParams.attackChoice.attackingPlayer.getPlayerPerson().getAiPlayStyle().position / 100;
            for (AttackChoice next : nextStepAttackChoices) {
                price += next.defaultRef.price * mul;
                mul /= 4;
            }
//            if (whitePrediction.getSecondCollide() != null) price *= kickBallMul;
            if (whitePrediction.getSecondCollide() != null) {
                priceOfKick = priceOfKick(whitePrediction.getSecondCollide(),
                        whitePrediction.getWhiteSpeedWhenHitSecondBall());
                price *= priceOfKick;
            }

            if (whitePrediction.isWhiteCollidesHoleArcs()) price *= 0.5;

            if (stage != GamePlayStage.NO_PRESSURE) {
                // 正常情况下少走点库
//                int cushions = whitePrediction.getWhiteCushionCountAfter();
//                double cushionDiv = Math.max(2, cushions) / 4.0 + 0.5;  // Math.max(x, cushions) / y + (1 - x / y)
//                price /= cushionDiv;
                // 正常情况下少跑点
                AiPlayStyle aps = aiPlayer.getPlayerPerson().getAiPlayStyle();
                double divider = aps.likeShow * 50.0;
                double dtTravel = whitePrediction.getDistanceTravelledAfterCollision();
                double penalty = dtTravel < 1000 ? 0 : (dtTravel - 1000) / divider;
                price /= 1 + penalty;
            }
        }
    }

    protected class AttackThread implements Runnable {

        AttackParam attackParams;
        GameValues values;
        int nextTarget;
        List<Ball> nextStepLegalBalls;
        Phy phy;
        GamePlayStage stage;

        IntegratedAttackChoice result;

        protected AttackThread(AttackParam attackParams,
                               GameValues values,
                               int nextTarget,
                               List<Ball> nextStepLegalBalls,
                               Phy phy,
                               GamePlayStage stage) {
            this.attackParams = attackParams;
            this.values = values;
            this.nextTarget = nextTarget;
            this.nextStepLegalBalls = nextStepLegalBalls;
            this.phy = phy;
            this.stage = stage;
        }

        @Override
        public void run() {
            //        System.out.print(selectedPower);
            double actualFbSpin = selectedFrontBackSpinToActual(attackParams.selectedFrontBackSpin);
            double actualSideSpin = selectedSideSpinToActual(attackParams.selectedSideSpin);
            double actualPower = selectedPowerToActualPower(attackParams.selectedPower,
                    actualSideSpin,
                    actualFbSpin,
                    attackParams.attackChoice.handSkill);

            double[] correctedDirection = CuePlayParams.aimingUnitXYIfSpin(
                    actualSideSpin,
                    actualPower,
                    attackParams.attackChoice.cueDirectionUnitVector[0],
                    attackParams.attackChoice.cueDirectionUnitVector[1]
            );

            // 考虑上塞之后的修正出杆
            AttackChoice correctedChoice = attackParams.attackChoice.copyWithNewDirection(correctedDirection);

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
                    true, false, true, true);
            if (wp.getFirstCollide() == null) {
                // 连球都碰不到，没吃饭？
//            System.out.println("too less");
                return;
            }
//            if (wp.getWhiteCushionCountAfter() == 0 && attackParams.selectedSideSpin != 0.0) {
//                // 不吃库的球加个卵的塞
//                return;
//            }

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
                            true);
            AttackParam correctedParams = attackParams.copyWithCorrectedChoice(
                    correctedChoice
            );
            result = new IntegratedAttackChoice(
                    correctedParams,
                    nextStepAttackChoices,
                    nextTarget,
                    params,
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
        Game[] gameClonesPool;

        DefenseChoice result;

        protected DefenseThread(double rad,
                                double[] whitePos,
                                double selectedPower,
                                Set<Ball> legalSet,
                                Phy phy,
                                Game[] gameClonesPool) {
            this.rad = rad;
            this.whitePos = whitePos;
            this.selectedPower = selectedPower;
            this.legalSet = legalSet;
            this.phy = phy;
            this.gameClonesPool = gameClonesPool;
        }

        @Override
        public void run() {
            int threadIndex = (int) (Thread.currentThread().getId() % nThreads);
            Game<?, P> copy = gameClonesPool[threadIndex];

            double[] unitXY = Algebra.unitVectorOfAngle(rad);

            PlayerPerson.HandSkill handSkill = CuePlayParams.getPlayableHand(
                    whitePos[0],
                    whitePos[1],
                    unitXY[0],  // fixme: 这里存疑
                    unitXY[1],
                    copy.getGameValues().table,
                    copy.getCuingPlayer().getPlayerPerson()
            );

            CuePlayParams cpp = CuePlayParams.makeIdealParams(
                    unitXY[0],
                    unitXY[1],
                    0.0,
                    0.0,
                    0.0,
                    selectedPowerToActualPower(selectedPower, 0, 0, handSkill)
            );
            result = analyseDefense(
                    AiCue.this,
                    cpp,
                    handSkill,
                    phy,
                    copy,
                    legalSet,
                    aiPlayer,
                    unitXY,
                    selectedPower,
                    0.0,
                    0.0,
                    false
            );
        }
    }

    class DefensiveAttackThread implements Runnable {

        double[] whitePos;
        Set<Ball> legalSet;
        Phy phy;
        Game[] gameClonesPool;

        AttackParam attackParam;
        GameValues values;

        DefenseChoice result;

        protected DefensiveAttackThread(
                AttackParam attackParam,
                double[] whitePos,
                Set<Ball> legalSet,
                Phy phy,
                Game[] gameClonesPool,
                GameValues values) {
            this.attackParam = attackParam;
            this.whitePos = whitePos;
            this.legalSet = legalSet;
            this.phy = phy;
            this.gameClonesPool = gameClonesPool;
            this.values = values;
        }

        @Override
        public void run() {
            int threadIndex = (int) (Thread.currentThread().getId() % nThreads);
            Game<?, P> copy = gameClonesPool[threadIndex];

            double actualFbSpin = selectedFrontBackSpinToActual(attackParam.selectedFrontBackSpin);
            double actualSideSpin = selectedSideSpinToActual(attackParam.selectedSideSpin);
            double actualPower = selectedPowerToActualPower(attackParam.selectedPower,
                    actualSideSpin,
                    actualFbSpin,
                    attackParam.attackChoice.handSkill);

            double[] correctedDirection = CuePlayParams.aimingUnitXYIfSpin(
                    actualSideSpin,
                    actualPower,
                    attackParam.attackChoice.cueDirectionUnitVector[0],
                    attackParam.attackChoice.cueDirectionUnitVector[1]
            );

            // 考虑上塞之后的修正出杆
            AttackChoice correctedChoice = attackParam.attackChoice.copyWithNewDirection(correctedDirection);
            attackParam = new AttackParam(attackParam, correctedChoice);

            CuePlayParams params = CuePlayParams.makeIdealParams(
                    correctedChoice.cueDirectionUnitVector[0],
                    correctedChoice.cueDirectionUnitVector[1],
                    actualFbSpin,
                    actualSideSpin,
                    0.0,
                    actualPower
            );

            result = analyseDefense(
                    AiCue.this,
                    params,
                    attackParam.attackChoice.handSkill,
                    phy,
                    copy,
                    legalSet,
                    aiPlayer,
                    correctedChoice.cueDirectionUnitVector,
                    attackParam.selectedPower,
                    attackParam.selectedFrontBackSpin,
                    attackParam.selectedSideSpin,
                    true
            );

            if (result != null) {
                double targetCanMove = values.estimatedMoveDistance(phy, result.wp.getBallInitSpeed());
                double wantItMove = correctedChoice.targetHoleDistance * 1.5;
                if (targetCanMove - values.ball.ballDiameter * 1.5 <= wantItMove) {
                    // 确保球不会停在袋口
                    // 如果小于，说明力量太轻或低杆太多，打不到
                    // 同时，连攻带防攻不进停袋口也是很蠢的
                    result = null;
                }
//                else {
//                    System.out.println(targetCanMove);
//                }
            }
        }
    }
}
