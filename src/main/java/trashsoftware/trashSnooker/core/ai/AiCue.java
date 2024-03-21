package trashsoftware.trashSnooker.core.ai;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.metrics.Pocket;
import trashsoftware.trashSnooker.core.metrics.Rule;
import trashsoftware.trashSnooker.core.movement.WhitePrediction;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.phy.TableCloth;
import trashsoftware.trashSnooker.core.snooker.AbstractSnookerGame;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// todo: 黑八半场自由球只能向下打，

public abstract class AiCue<G extends Game<?, P>, P extends Player> {

//    public static final double ATTACK_DIFFICULTY_THRESHOLD = 18000.0;  // 越大，AI越倾向于进攻

    public static final double PURE_ATTACK_PROB = 0.4;  // 进攻权重为99的球员只要prob高于这个值他就会进攻。越小，AI越倾向于无脑进攻
    public static final double DEFENSIVE_ATTACK_PROB = 0.2;  // 这个值是线性的，进攻权重为99的球员高于这个值就会尝试性进攻

    public static final double NO_DIFFICULTY_ANGLE_RAD = 0.3;
    public static final double EACH_BALL_SEE_PRICE = 0.5;
    public static final double WHITE_HIT_CORNER_PENALTY = 0.05;
    public static final double KICK_USELESS_BALL_MUL = 0.5;
    //    private static final double[] FRONT_BACK_SPIN_POINTS =
//            {0.0, -0.27, 0.27, -0.54, 0.54, -0.81, 0.81};
    protected static final double[][] ATTACK_SPIN_POINTS = {  // 高低杆，左右塞
            {0.0, 0.0}, {0.0, 0.35}, {0.0, -0.35}, {0.0, 0.7}, {0.0, -0.7},
            {-0.3, 0.0}, {-0.3, 0.3}, {-0.3, -0.3}, {-0.3, 0.6}, {-0.3, -0.6},
            {0.3, 0.0}, {0.3, 0.3}, {0.3, -0.3}, {0.3, 0.6}, {0.3, -0.6},
            {-0.6, 0.0}, {-0.6, 0.25}, {-0.6, -0.25}, {-0.6, 0.5}, {-0.6, -0.5},
            {0.6, 0.0}, {0.6, 0.25}, {0.6, -0.25}, {0.6, 0.5}, {0.6, -0.5},
            {-0.9, 0.0}, {-0.8, 0.35}, {-0.8, -0.35},
            {0.9, 0.0}, {0.8, 0.35}, {0.8, -0.35}
    };
    protected static final double[][] DOUBLE_POT_SPIN_POINTS = {  // 翻袋进攻的塞
            {0.0, 0.0}, {0.0, 0.5}, {0.0, -0.5},
            {-0.4, 0.0}, {-0.4, 0.4}, {-0.4, -0.4},
            {0.4, 0.0}, {0.4, 0.4}, {0.4, -0.4},
            {-0.8, 0.0}, {-0.8, 0.25}, {-0.8, -0.25},
            {0.8, 0.0}, {0.8, 0.25}, {0.8, -0.25},
    };
    public static boolean aiOnlyDefense = false;
    public static boolean aiOnlyDouble = false;
    protected Ball presetTarget;

    static {
        Arrays.sort(ATTACK_SPIN_POINTS, Comparator.comparingDouble(a -> Math.abs(a[0]) + Math.abs(a[1])));
        Arrays.sort(DOUBLE_POT_SPIN_POINTS, Comparator.comparingDouble(a -> Math.abs(a[0]) + Math.abs(a[1])));
    }

    private final double opponentPureAtkProb;  // 对手会直接进攻的界限
    private final double opponentDefAtkProb;
    protected int nThreads;
    protected G game;
    protected P aiPlayer;

    protected FinalChoice.IntegratedAttackChoice bestAttack;  // 记录一下，不管最后有没有用它

    public AiCue(G game, P aiPlayer) {
        this.game = game;
        this.aiPlayer = aiPlayer;

        this.nThreads = Math.max(1,
                Math.min(32,
                        ConfigLoader.getInstance().getInt("nThreads", 4)));

        P opponent = game.getAnotherPlayer(aiPlayer);
        opponentPureAtkProb = attackProbThreshold(0.4, opponent.getPlayerPerson().getAiPlayStyle());
        opponentDefAtkProb = defensiveAttackProbThreshold(opponent.getPlayerPerson().getAiPlayStyle());
    }

    private static <G extends Game<?, ?>> List<AttackChoice.DoubleAttackChoice> doubleAttackChoices(
            G game,
            int attackTarget,
            Player attackingPlayer,
            Ball lastPottingBall,
            List<Ball> legalBalls,
            double[] whitePos,
            boolean isPositioning
    ) {
        List<Game.DoublePotAiming> doublePots = game.doublePotAble(
                whitePos[0],
                whitePos[1],
                legalBalls,
                1
        );
//        System.out.println(doublePots.size() + " double possibilities");

        List<AttackChoice.DoubleAttackChoice> choices = new ArrayList<>();
        for (Game.DoublePotAiming aiming : doublePots) {
            AttackChoice.DoubleAttackChoice dac = AttackChoice.DoubleAttackChoice.createChoice(
                    game,
                    game.getEntireGame().predictPhy,
                    attackingPlayer,
                    whitePos,
                    aiming,
                    lastPottingBall,
                    attackTarget,
                    isPositioning
            );
            if (dac != null) {
                choices.add(dac);
            }
        }
        return choices;
    }

//    protected List<DoubleAttackChoice> getCurrentDoubleAttackChoices() {
//        int curTarget = game.getCurrentTarget();
//        boolean isSnookerFreeBall = game.isDoingSnookerFreeBll();
//        List<Ball> legalBalls = game.getAllLegalBalls(curTarget,
//                isSnookerFreeBall,
//                game.isInLineHandBallForAi());
//
//        Ball cueBall = game.getCueBall();
//
//        List<DoubleAttackChoice> doubleAttackChoices = getDoubleAttackChoices(
//                game,
//                curTarget,
//                aiPlayer,
//                null,
//                legalBalls,
//                new double[]{cueBall.getX(), cueBall.getY()},
//                false
//        );
//        System.out.println(doubleAttackChoices.size() + " double attacks");
//        return doubleAttackChoices;
//    }

    private static <G extends Game<?, ?>> List<AttackChoice.DirectAttackChoice> directAttackChoices(
            G game,
            int attackTarget,
            Player attackingPlayer,
            Ball lastPottingBall,
            List<Ball> legalBalls,
            double[] whitePos,
            boolean isPositioning
    ) {
        List<AttackChoice.DirectAttackChoice> directAttackChoices = new ArrayList<>();
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
                    AttackChoice.DirectAttackChoice directAttackChoice = AttackChoice.DirectAttackChoice.createChoice(
                            game,
                            game.getEntireGame().predictPhy,
                            attackingPlayer,
                            whitePos,
                            ball,
                            lastPottingBall,
                            attackTarget,
                            isPositioning,
                            dirHole,
                            null
                    );
                    if (directAttackChoice != null) {
                        directAttackChoices.add(directAttackChoice);
                    }
                }
            }
        }
        return directAttackChoices;
    }

    public static <G extends Game<?, ?>> List<AttackChoice> getAttackChoices(
            G game,
            int attackTarget,
            Player attackingPlayer,
            Ball lastPottingBall,
            List<Ball> legalBalls,
            double[] whitePos,
            boolean isPositioning,
            boolean considerDoublePot
    ) {
        List<AttackChoice> attackChoices = new ArrayList<>();
        if (!aiOnlyDouble) {
            attackChoices.addAll(directAttackChoices(
                    game,
                    attackTarget,
                    attackingPlayer,
                    lastPottingBall,
                    legalBalls,
                    whitePos,
                    isPositioning
            ));
        }
        if (considerDoublePot) {
            attackChoices.addAll(doubleAttackChoices(
                    game,
                    attackTarget,
                    attackingPlayer,
                    lastPottingBall,
                    legalBalls,
                    whitePos,
                    isPositioning
            ));
        }
        Collections.sort(attackChoices);
        return attackChoices;
    }

    static double attackProbThreshold(double base, AiPlayStyle aps) {
        if (aps.attackPrivilege == 100) return 0.000001;  // 管他娘的
        else {
            double room = 1.0 - base;
            double playerNotWantAttack = 1 - aps.attackPrivilege / 100;
            playerNotWantAttack = Math.pow(playerNotWantAttack, 0.75);  // 无奈之举。次幂越小，进攻权重低的球手越不进攻
            double realAttackProb = base + playerNotWantAttack * room;
            double mul = Math.pow(aps.precision / 100.0, 1.5);  // 补偿由于AI打不准造成的进球概率低，进而不进攻的问题
            return realAttackProb * mul;  // 这里不像下面用了Math.max。原因：太菜的选手只会无脑进攻，哈哈哈
        }
    }

    static double defensiveAttackProbThreshold(AiPlayStyle aps) {
        double room = 1 - DEFENSIVE_ATTACK_PROB;
        double realProb = DEFENSIVE_ATTACK_PROB + (1 - aps.attackPrivilege / 100) * room;
        double mul = Math.pow(aps.precision / 100.0, 1.5);  // 补偿由于AI打不准造成的进球概率低，进而不进攻的问题
        double res = realProb * mul;
        return Math.max(DEFENSIVE_ATTACK_PROB / 5, res);
    }

    /**
     * 返回的都得是有效的防守。
     * 但吃库不会在这里检查。
     */
    protected static FinalChoice.DefenseChoice analyseDefense(
            AiCue<?, ?> aiCue,
            CuePlayParams cpp,
            CueParams cueParams,
            Phy phy,
            Game<?, ?> copy,
            Set<Ball> legalSet,
            Player aiPlayer,
            double[] unitXY,
            boolean attackAble,  // 可不可以进
            double nativePrice,
            boolean allowPocketCorner
    ) {
        WhitePrediction wp = copy.predictWhite(cpp, phy, 50000.0,
                true,
                true, true, false,
                false);  // 这里不用clone，因为整个game都是clone的
        double[] whiteStopPos = wp.getWhitePath().get(wp.getWhitePath().size() - 1);

        if (!allowPocketCorner && wp.isWhiteHitsHoleArcs()) {
            wp.resetToInit();
            return null;
        }

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
            double opponentAttackPrice = AttackChoice.DirectAttackChoice.priceOfDistance(seeAble.avgTargetDistance);
//            double opponentAttackPrice2 = AttackChoice.priceOfDistance(seeAble.avgTargetDistance);
            double snookerPrice = 1.0;

            if (wp.isFirstBallCollidesOther()) {
                penalty *= 16;
            }
            if (wp.getSecondCollide() != null) {
                penalty *= 30;
            }
            if (wp.isCueBallFirstBallTwiceColl()) {
                penalty *= 2;  // 二次碰撞
            }

            AttackChoice oppoEasiest = null;
            if (seeAble.seeAbleTargets == 0) {
                // 这个权重如果太大，AI会不计惩罚地去做斯诺克
                // 如果太小，AI会不做斯诺克
                snookerPrice = Math.sqrt(seeAble.maxShadowAngle) * 50.0;
            } else {
                List<AttackChoice> directAttackChoices = getAttackChoices(
                        copy,
                        opponentTarget,
                        copy.getAnotherPlayer(aiPlayer),
                        null,
                        opponentBalls,
                        whiteStopPos,
                        false,
                        false  // 防守还是别考虑对手翻袋了
                );

                for (AttackChoice ac : directAttackChoices) {
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
            if (wp.isWhiteHitsHoleArcs()) {
                penalty /= WHITE_HIT_CORNER_PENALTY;
            }
            wp.resetToInit();
//            System.out.printf("%f %f %f %f\n", snookerPrice, opponentAttackPrice, opponentAttackPrice2, penalty);
            return new FinalChoice.DefenseChoice(
                    firstCollide,
                    nativePrice,
                    snookerPrice,
                    opponentAttackPrice,
                    penalty,
                    unitXY,
                    cueParams,
                    wp,
                    cpp,
//                    handSkill,
                    oppoEasiest,
                    wp.getSecondCollide() != null,
                    wp.isFirstBallCollidesOther()
            );
        }
        wp.resetToInit();
        return null;
    }

    public abstract AiCueResult makeCue(Phy phy);

    public void setPresetTarget(Ball presetTarget) {
        this.presetTarget = presetTarget;
    }

    /**
     * 开球
     */
    protected abstract FinalChoice.DefenseChoice breakCue(Phy phy);

    protected abstract FinalChoice.DefenseChoice standardDefense();

    protected abstract boolean supportAttackWithDefense(int targetRep);

    protected abstract boolean currentMustAttack();

    public static double ballAlivePrice(Game<?, ?> game, Ball ball) {
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
            double potDifficulty = AttackChoice.DirectAttackChoice.holeDifficulty(
                    game,
                    dirHolePoint[1][0] == game.getGameValues().table.midX,
                    dirHolePoint[0]
            ) * Math.hypot(ball.getX() - dirHolePoint[1][0], ball.getY() - dirHolePoint[1][1]);
            price += 20000.0 / potDifficulty;
        }
        return price;
    }

    protected FinalChoice.DefenseChoice solveSnooker(Phy phy, boolean allowPocketCorner) {
        int curTarget = game.getCurrentTarget();
        boolean isSnookerFreeBall = game.isDoingSnookerFreeBll();
        List<Ball> legalBalls = game.getAllLegalBalls(curTarget,
                isSnookerFreeBall,
                false);  // 不可能，没有谁会给自己摆杆斯诺克

        PlayerPerson aps = aiPlayer.getPlayerPerson();
        double degreesTick = 100.0 / 2 / aps.getSolving();
        double powerTick = 1000.0 / aps.getSolving();

        int continuousFoulAndMiss = game.getContinuousFoulAndMiss();
        double chanceOfSmallPower = Math.pow(continuousFoulAndMiss, 1.35) / 18 + 0.5;  // 到第6杆时必定小力了
        if (Math.random() < chanceOfSmallPower) {
            System.out.println("AI solving snooker small power!");
            return solveSnookerDefense(legalBalls, degreesTick, powerTick, phy, true, allowPocketCorner);
        } else {
            System.out.println("AI solving snooker!");
            return solveSnookerDefense(legalBalls, degreesTick, powerTick, phy, false, allowPocketCorner);
        }
    }

    protected abstract KickPriceCalculator kickPriceCalculator();

    protected interface KickPriceCalculator {
        double priceOfKick(Ball kickedBall, double kickSpeed, double dtFromFirst);
    }

    protected double kickUselessBallPrice(double dtFromFirst) {
        // 小打小k会用到这个
        if (dtFromFirst >= 500) return KICK_USELESS_BALL_MUL;
        return Algebra.shiftRange(0,
                10 * aiPlayer.getPlayerPerson().getAiPlayStyle().position,  // 走位100的人能控制1000mm内的二次k球
                1.0,
                KICK_USELESS_BALL_MUL,
                dtFromFirst);
    }

//    private List<>

    private FinalChoice.IntegratedAttackChoice attack(AttackChoice choice,
                                                      int nextTarget,
                                                      List<Ball> nextStepLegalBalls,
                                                      Phy phy,
                                                      boolean mustAttack) {
        double powerLimit = aiPlayer.getPlayerPerson().getControllablePowerPercentage();

        double minPower;
        double tick;
        double[][] availSpins;

        if (choice instanceof AttackChoice.DirectAttackChoice) {
            tick = 300.0 / aiPlayer.getPlayerPerson().getAiPlayStyle().position;
            minPower = tick;
            availSpins = ATTACK_SPIN_POINTS;
//            ranged = false;
        } else if (choice instanceof AttackChoice.DoubleAttackChoice) {
            tick = 600.0 / aiPlayer.getPlayerPerson().getAiPlayStyle().position;
            minPower = 12.0;
            availSpins = DOUBLE_POT_SPIN_POINTS;
//            ranged = true;
        } else {
            throw new RuntimeException();
        }

        List<FinalChoice.IntegratedAttackChoice> choiceList = new ArrayList<>();
        AiPlayStyle aps = aiPlayer.getPlayerPerson().getAiPlayStyle();
//        double likeShow = aiPlayer.getPlayerPerson().getAiPlayStyle().likeShow;  // 喜欢大力及杆法的程度
        GamePlayStage stage = game.getGamePlayStage(choice.ball, false);

        long t0 = System.currentTimeMillis();

        double pureAttackThreshold = attackProbThreshold(PURE_ATTACK_PROB, aps);  // 进球概率高于这个值，AI就纯进攻
        double defensiveAttackThreshold = defensiveAttackProbThreshold(aps);

        List<AttackParam> pureAttacks = new ArrayList<>();
        List<AttackParam> defensiveAttacks = new ArrayList<>();  // 连打带防
        Cue cue = aiPlayer.getInGamePlayer().getCueSelection().getSelected().getNonNullInstance();
        double easiest = 0.0;
        for (double selectedPower = minPower; selectedPower <= powerLimit; selectedPower += tick) {
            for (double[] spins : availSpins) {
                double[] aiSpins = cue.aiCuePoint(spins, game.getGameValues().ball);
                CueParams cueParams = CueParams.createBySelected(
                        selectedPower,
                        aiSpins[0],
                        aiSpins[1],
                        game,
                        aiPlayer.getInGamePlayer(),
                        choice.handSkill
                );
                if (choice instanceof AttackChoice.DoubleAttackChoice dou) {
                    // 目标球的
                    double degreesTick = 100.0 / 2 / aps.doubleAbility;
                    double realRadTick = Math.toRadians(degreesTick);

                    double[] directionVec = new double[]{
                            dou.collisionPos[0] - choice.whitePos[0],
                            dou.collisionPos[1] - choice.whitePos[1]
                    };
                    double distance = Math.hypot(directionVec[0], directionVec[1]);
                    double alpha = Algebra.thetaOf(directionVec);  // 白球到目标球球心连线的绝对角
                    // 中心连线与最偏处的连线的夹角
                    // 0.1倍球的直径的挑战范围
                    double theta = Math.asin(game.getGameValues().ball.ballDiameter * 0.1 / distance);

                    int offsetTicks = (int) (theta / realRadTick);
//            System.out.println(offsetTicks + " radians offsets to " + ball + realRadTick + theta);

//                    double halfOfTick = realRadTick / 2;

                    for (int i = -offsetTicks; i <= offsetTicks; i++) {
                        double angle = Algebra.normalizeAngle(alpha + i * realRadTick);
                        double[] vec = Algebra.unitVectorOfAngle(angle);
                        AttackChoice.DoubleAttackChoice attempted = dou.copyWithNewDirection(vec);
                        AttackParam acp = new AttackParam(
                                attempted, game, phy, cueParams
                        );
                        if (acp.potProb > easiest) easiest = acp.potProb;
                        if (mustAttack || acp.potProb > pureAttackThreshold) {
                            pureAttacks.add(acp);
                        } else if (acp.potProb > defensiveAttackThreshold) {
                            defensiveAttacks.add(acp);
                        }
                    }
                } else {
                    AttackParam acp = new AttackParam(
                            choice, game, phy, cueParams
                    );
                    if (acp.potProb > easiest) easiest = acp.potProb;
                    if (mustAttack || acp.potProb > pureAttackThreshold) {
                        pureAttacks.add(acp);
                    } else if (acp.potProb > defensiveAttackThreshold) {
                        defensiveAttacks.add(acp);
                    }
                }
            }
        }

        long t1 = System.currentTimeMillis();
        System.out.println("Ai list attack params in " + (t1 - t0) + " ms, " +
                pureAttacks.size() + " pure, " + defensiveAttacks.size() + " defensive.");

        if (pureAttacks.isEmpty() && defensiveAttacks.isEmpty()) {
            System.out.println("Not attack because the highest prob is " + easiest);
            return null;
        }

        Game[] gameClonesPool = new Game[nThreads];
        for (int i = 0; i < gameClonesPool.length; i++) {
            gameClonesPool[i] = game.clone();
        }

        List<AttackThread> attackThreads = new ArrayList<>();
        for (AttackParam pureAttack : pureAttacks) {
            AttackThread thread = new AttackThread(
                    pureAttack,
                    game.getGameValues(),
                    nextTarget,
                    nextStepLegalBalls,
                    phy,
                    gameClonesPool,
                    aiPlayer,
                    stage,
                    kickPriceCalculator()
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
            double pureAttackThresh = attackProbThreshold(PURE_ATTACK_PROB, aps);

            choiceList.sort(FinalChoice.IntegratedAttackChoice::normalCompareTo);
            for (FinalChoice.IntegratedAttackChoice iac : choiceList) {
                if (!iac.nextStepAttackChoices.isEmpty()) {
                    AttackChoice bestNextStep = iac.nextStepAttackChoices.get(0);
                    if (bestNextStep.defaultRef.potProb >= pureAttackThresh) {
                        System.out.println("Penalty, tor = " + iac.penalty + ", " + iac.positionErrorTolerance);
                        return iac;  // 我纯进攻下一杆也得走个纯进攻的位撒
                    }
                }
            }

            FinalChoice.IntegratedAttackChoice iac = choiceList.get(0);
            if (iac.nextStepTarget == Game.END_REP || mustAttack) return iac;

            if (iac.priceOfKick >= 1.0) {  // 能K正确的球
                double kickProbLow = 0.6;
                double kickProbHigh = 0.9;
                if (Math.random() < kickProbLow + (iac.priceOfKick - 1) * (kickProbHigh - kickProbLow)) {
                    // priceOfKick是1，概率为0.6
                    // priceOfKick是2，概率为0.9
                    // 大于等于2.几，必定k
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
            return new FinalChoice.IntegratedAttackChoice(
                    game,
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

    protected AiCueResult makeAttackCue(FinalChoice.IntegratedAttackChoice iac) {
        return makeAttackCue(iac, iac.attackParams.attackChoice.cueType());
    }

    protected AiCueResult makeAttackCue(FinalChoice.IntegratedAttackChoice iac, AiCueResult.CueType cueType) {
        AttackParam attackParam = iac.attackParams;
        AttackChoice attackChoice = attackParam.attackChoice;

        AiCueResult acr = new AiCueResult(aiPlayer.getInGamePlayer(),
                game.getGamePlayStage(attackChoice.ball, true),
                cueType,
                attackChoice.targetOrigPos,
                attackChoice instanceof AttackChoice.DirectAttackChoice dac ? dac.dirHole : null,  // fixme: check
                attackChoice.ball,
                attackChoice.cueDirectionUnitVector[0],
                attackChoice.cueDirectionUnitVector[1],
                attackParam.cueParams,
                game.frameImportance(aiPlayer.getInGamePlayer().getPlayerNumber()),
                game.getEntireGame().rua(aiPlayer.getInGamePlayer()));
        List<double[]> whitePath;
        if (iac.whitePrediction != null) {
            whitePath = iac.whitePrediction.getWhitePath();
        } else {
            WhitePrediction wp = game.predictWhite(iac.params, game.getEntireGame().predictPhy, 0.0,
                    true,
                    true, false, true, true);
            whitePath = wp.getWhitePath();
        }

        acr.setWhitePath(whitePath);
        return acr;
    }

    protected AiCueResult makeDefenseCue(FinalChoice.DefenseChoice choice, AiCueResult.CueType cueType) {
        AiCueResult acr = new AiCueResult(aiPlayer.getInGamePlayer(),
                game.getGamePlayStage(choice.ball, true),
                cueType,
                null,
                null,
                choice.ball,
                choice.cueDirectionUnitVector[0],
                choice.cueDirectionUnitVector[1],
                choice.cueParams,
//                0.0,  // todo
                game.frameImportance(aiPlayer.getInGamePlayer().getPlayerNumber()),
                game.getEntireGame().rua(aiPlayer.getInGamePlayer()));
        acr.setWhitePath(choice.wp != null ? choice.wp.getWhitePath() : null);
        return acr;
    }

    protected AiCueResult regularCueDecision(Phy phy) {
        if (game.isBreaking()) {
            FinalChoice.DefenseChoice breakChoice = breakCue(phy);
            if (breakChoice != null) return makeDefenseCue(breakChoice, AiCueResult.CueType.BREAK);
        }

        if (!aiOnlyDefense) {
            FinalChoice.IntegratedAttackChoice attackChoice = standardAttack(phy, currentMustAttack());
            if (attackChoice != null) {
                return makeAttackCue(attackChoice);
            }
        }
        FinalChoice.DefenseChoice stdDefense = standardDefense();
        if (stdDefense != null) {
            return makeDefenseCue(stdDefense, AiCueResult.CueType.DEFENSE);
        }
        FinalChoice.DefenseChoice defenseChoice = getBestDefenseChoice(phy);
        if (defenseChoice != null) {
            return makeDefenseCue(defenseChoice, AiCueResult.CueType.DEFENSE);
        }
        FinalChoice.DefenseChoice solveSnooker = solveSnooker(phy, false);
        if (solveSnooker != null) {
            return makeDefenseCue(solveSnooker, AiCueResult.CueType.SOLVE);
        }
        FinalChoice.DefenseChoice solveSnooker2 = solveSnooker(phy, true);  // 只能说是逼急了，来个袋角解斯诺克
        System.out.println("Cannot solve snooker! Try pocket arc!");
        if (solveSnooker2 != null) {
            return makeDefenseCue(solveSnooker2, AiCueResult.CueType.SOLVE);
        }
        return randomAngryCue();
    }

    protected FinalChoice.IntegratedAttackChoice standardAttack(Phy phy, boolean mustAttack) {
        if (aiOnlyDefense) return null;
        List<AttackChoice> all;
        if (presetTarget == null) {
            all = getCurrentAttackChoices();
        } else {
            all = getAttackChoices(game, 
                    game.getCurrentTarget(),
                    aiPlayer,
                    null,
                    List.of(presetTarget),
                    new double[]{game.getCueBall().getX(), game.getCueBall().getY()},
                    false,
                    true);
        }

        FinalChoice.IntegratedAttackChoice iac = attackGivenChoices(all, phy, mustAttack);
        if (iac != null && iac.betterThan(bestAttack)) {
            bestAttack = iac;  // 记录一下
        }
        return iac;
    }

    protected FinalChoice.IntegratedAttackChoice attackGivenChoices(List<? extends AttackChoice> attackChoices,
                                                                    Phy phy, boolean mustAttack) {
        System.out.println("Simple attack choices:" + attackChoices.size());
//        System.out.println(attackAttackChoices);
        if (!attackChoices.isEmpty()) {
            FinalChoice.IntegratedAttackChoice best = null;
            for (int i = 0; i < Math.min(2, attackChoices.size()); i++) {
                AttackChoice choice = attackChoices.get(i);
                int nextTargetIfThisSuccess = game.getTargetAfterPotSuccess(choice.ball,
                        game.isDoingSnookerFreeBll());
                List<Ball> nextStepLegalBalls =
                        game.getAllLegalBalls(nextTargetIfThisSuccess,
                                false,
                                false);  // 这颗进了下一颗怎么可能是自由球/手中球

                if (game.getGameType() == GameRule.CHINESE_EIGHT || game.getGameType() == GameRule.LIS_EIGHT) {
                    // 避免AI打自己较自己的可能（并不确定会发生）
                    nextStepLegalBalls.remove(choice.ball);
                }

                FinalChoice.IntegratedAttackChoice iac = attack(choice,
                        nextTargetIfThisSuccess, nextStepLegalBalls, phy, mustAttack);
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
                System.out.printf("Best int attack choice: %s, %s, dir %f, %f, power %f, spins %f, %f, pot prob, %f \n",
                        best.isPureAttack ? "pure" : "defensive",
                        best.attackParams.attackChoice instanceof AttackChoice.DoubleAttackChoice dou ?
                                ("double " + dou.pocket.hole) : "direct",
                        best.attackParams.attackChoice.cueDirectionUnitVector[0],
                        best.attackParams.attackChoice.cueDirectionUnitVector[1],
                        best.attackParams.cueParams.selectedPower(),
                        best.attackParams.cueParams.selectedFrontBackSpin(),
                        best.attackParams.cueParams.selectedSideSpin(),
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
                0.0,
                game.getGameValues().table,
                game.getCuingPlayer().getPlayerPerson()
        );
        CueParams cueParams = CueParams.createBySelected(
                power,
                0.0,
                0.0,
                game,
                aiPlayer.getInGamePlayer(),
                handSkill
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
                cueParams,
                game.frameImportance(aiPlayer.getInGamePlayer().getPlayerNumber()),
                game.getEntireGame().rua(aiPlayer.getInGamePlayer())
        );
    }

    private List<AttackChoice> getAttackChoices(int attackTarget,
                                                Ball lastPottingBall,
                                                boolean isSnookerFreeBall,
                                                double[] whitePos,
                                                boolean isPositioning,
                                                boolean isLineInHandBall,
                                                boolean considerDoublePot) {
        return getAttackChoices(game,
                attackTarget,
                aiPlayer,
                lastPottingBall,
                game.getAllLegalBalls(attackTarget, isSnookerFreeBall, isLineInHandBall),
                whitePos,
                isPositioning,
                considerDoublePot
        );
    }

    protected List<AttackChoice> getCurrentAttackChoices() {
        return getAttackChoices(game.getCurrentTarget(),
                null,
                game.isDoingSnookerFreeBll(),
                new double[]{game.getCueBall().getX(), game.getCueBall().getY()},
                false,
                game.isInLineHandBallForAi(),
                true);
    }

    private FinalChoice.DefenseChoice directDefense(List<Ball> legalBalls,
                                                    double origDegreesTick,
                                                    double origPowerTick,
                                                    double actualPowerLow,
                                                    double actualPowerHigh,
                                                    Phy phy) {
        double realPowerTick = origPowerTick / 2;
        double realRadTick = Math.toRadians(origDegreesTick);
        Ball cueBall = game.getCueBall();
        double[] whitePos = new double[]{cueBall.getX(), cueBall.getY()};

        class DefenseAngle implements Comparable<DefenseAngle> {
            final double rad;
            double price;

            DefenseAngle(double rad, double price) {
                this.rad = rad;
                this.price = price;
            }

            @Override
            public int compareTo(@NotNull DefenseAngle o) {
                return Double.compare(this.rad, o.rad);
            }
        }

        NavigableSet<DefenseAngle> availableRads = new TreeSet<>();
        for (Ball ball : legalBalls) {
            double[] directionVec = new double[]{ball.getX() - whitePos[0], ball.getY() - whitePos[1]};
            double distance = Math.hypot(directionVec[0], directionVec[1]);
            double alpha = Algebra.thetaOf(directionVec);  // 白球到目标球球心连线的绝对角
            double theta = Math.asin(game.getGameValues().ball.ballDiameter / distance);  // 中心连线与薄边连线的夹角

            int offsetTicks = (int) (theta / realRadTick);
//            System.out.println(offsetTicks + " radians offsets to " + ball + realRadTick + theta);

            double halfOfTick = realRadTick / 2;

            for (int i = -offsetTicks; i <= offsetTicks; i++) {
                boolean isThin = i == -offsetTicks || i == offsetTicks;  // 是否为最薄边
                double price = isThin ? 0.7 : 1.0;  // 尽量不要擦最薄边去防守，容易空杆

                double angle = Algebra.normalizeAngle(alpha + i * realRadTick);
                DefenseAngle da = new DefenseAngle(angle, price);
                double[] vec = Algebra.unitVectorOfAngle(angle);
                PredictedPos leftPP = game.getPredictedHitBall(
                        cueBall.getX(), cueBall.getY(),
                        vec[0], vec[1]);
                if (leftPP == null || leftPP.getTargetBall() == null ||
                        leftPP.getTargetBall() == ball) {
                    // 如果与已有的角度太接近就不考虑了
                    DefenseAngle floorRad = availableRads.floor(da);
                    DefenseAngle ceilRad = availableRads.ceiling(da);
                    if ((floorRad != null && angle - floorRad.rad < halfOfTick) ||
                            (ceilRad != null && ceilRad.rad - angle < halfOfTick)) {
                        if (floorRad != null) {
                            floorRad.price = Math.max(price, floorRad.price);
                        }
                        if (ceilRad != null) {
                            ceilRad.price = Math.max(price, ceilRad.price);
                        }
                        continue;
                    }
                    availableRads.add(da);
                }
            }
        }
        if (availableRads.isEmpty()) return null;

        Set<Ball> legalSet = new HashSet<>(legalBalls);
        FinalChoice.DefenseChoice best = null;
        double selPowLow = CueParams.actualPowerToSelectedPower(
                game,
                aiPlayer.getInGamePlayer(),
                actualPowerLow,
                0,
                0,
                null);
        double selPowHigh = CueParams.actualPowerToSelectedPower(
                game,
                aiPlayer.getInGamePlayer(),
                actualPowerHigh,
                0,
                0,
                null);

        List<DefenseThread> defenseThreads = new ArrayList<>();
        Game[] gameClonesPool = new Game[nThreads];
        for (int i = 0; i < gameClonesPool.length; i++) {
            gameClonesPool[i] = game.clone();
        }

        for (double selectedPower = selPowLow;
             selectedPower < selPowHigh;
             selectedPower += realPowerTick) {

            for (DefenseAngle da : availableRads) {
                DefenseThread thread = new DefenseThread(
                        da.rad, da.price, whitePos, selectedPower, legalSet, phy, gameClonesPool, false
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

    private boolean notViolateCushionRule(FinalChoice.DefenseChoice choice) {
        if (game.getGameValues().rule.hasRule(Rule.HIT_CUSHION)) {
            if (choice.wp == null) return true;
            else return choice.wp.getWhiteCushionCountAfter() > 0 ||
                    choice.wp.getFirstBallCushionCount() > 0;
        } else {
            return true;
        }
    }

    private FinalChoice.DefenseChoice solveSnookerDefense(List<Ball> legalBalls,
                                                          double degreesTick,
                                                          double powerTick,
                                                          Phy phy,
                                                          boolean smallPower,
                                                          boolean allowPocketCorner) {
        Set<Ball> legalSet = new HashSet<>(legalBalls);
//        DefenseChoice best = null;

        Ball cueBall = game.getCueBall();
        double[] whitePos = new double[]{cueBall.getX(), cueBall.getY()};

//        List<DefenseThread> defenseThreads = new ArrayList<>();
        Game[] gameClonesPool = new Game[nThreads];
        for (int i = 0; i < gameClonesPool.length; i++) {
            gameClonesPool[i] = game.clone();
        }

        double realPowerTick = smallPower ? powerTick / 3.0 : powerTick;
        double powerLimit = smallPower ?
                Math.min(45.0, aiPlayer.getPlayerPerson().getControllablePowerPercentage()) :
                aiPlayer.getPlayerPerson().getControllablePowerPercentage();
        List<AngleSnookerSolver> angleSolvers = new ArrayList<>();

        for (double deg = 0.0; deg < 360; deg += degreesTick) {
            AngleSnookerSolver ass = new AngleSnookerSolver(deg, smallPower);
            for (double selectedPower = 5.0;
                 selectedPower < powerLimit;
                 selectedPower += realPowerTick) {

                DefenseThread thread = new DefenseThread(
                        Math.toRadians(deg),
                        1.0,  // 这里我们根本无法判断，只能给1.0了
                        whitePos,
                        selectedPower,
                        legalSet,
                        phy,
                        gameClonesPool,
                        allowPocketCorner
                );
                ass.threadsOfAngle.add(thread);
            }
            angleSolvers.add(ass);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        for (AngleSnookerSolver thread : angleSolvers) {
            executorService.execute(thread);
        }

        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(1, TimeUnit.MINUTES))
                throw new RuntimeException("AI thread not terminated.");  // Wait for all threads complete.
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<FinalChoice.DefenseChoice> legalChoices = new ArrayList<>();
        for (AngleSnookerSolver ass : angleSolvers) {
            for (FinalChoice.DefenseChoice result : ass.results) {
                if (notViolateCushionRule(result)) {
                    if (smallPower && result.wp.getWhiteSpeedWhenHitFirstBall() > realPowerTick * 25) {
                        continue;
                    }
                    legalChoices.add(result);
                }
            }
        }
        if (legalChoices.isEmpty()) return null;
        Collections.sort(legalChoices);
        Collections.reverse(legalChoices);

        if (legalChoices.size() == 1) return legalChoices.get(0);
        else if (legalChoices.size() == 2)
            return Math.random() > 0.3 ? legalChoices.get(0) : legalChoices.get(1);

        List<FinalChoice.DefenseChoice> bests = legalChoices.subList(0, Math.min(3, legalChoices.size()));
        double rnd = Math.random();
        if (rnd > 0.4) return bests.get(0);
        else if (rnd > 0.1) return bests.get(1);
        else return bests.get(2);
    }

    protected FinalChoice.DefenseChoice getBestDefenseChoice(Phy phy) {
        return getBestDefenseChoice(
                5.0,
                CueParams.selectedPowerToActualPower(
                        game,
                        aiPlayer.getInGamePlayer(),
                        aiPlayer.getPlayerPerson().getControllablePowerPercentage(),
                        0, 0, null),
                phy
        );
    }

    protected FinalChoice.DefenseChoice getBestDefenseChoice(double actualPowerLow, double actualPowerHigh, Phy phy) {
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

    public static class AttackParam {

        double potProb;  // 正常情况下能打进的概率
        double price;  // 对于球手来说的价值
        AttackChoice attackChoice;

        CueParams cueParams;

        private AttackParam(AttackParam base, AttackChoice replacement) {
            this.attackChoice = replacement;
            this.price = base.price;
            this.potProb = base.potProb;
            this.cueParams = base.cueParams;
        }

        public AttackParam(AttackChoice attackChoice,
                           Game<?, ?> game,
                           Phy phy,
                           CueParams cueParams) {
            this.attackChoice = attackChoice;
            this.cueParams = cueParams;

            GameValues gameValues = game.getGameValues();

            PlayerPerson playerPerson = attackChoice.attackingPlayer.getPlayerPerson();
            AiPlayStyle aps = playerPerson.getAiPlayStyle();
            double handSdMul = PlayerPerson.HandBody.getSdOfHand(attackChoice.handSkill);

//            double[] muSigXy = playerPerson.getCuePointMuSigmaXY();
//            double sideSpinSd = muSigXy[1];  // 左右打点的标准差，mm
            double powerErrorFactor = playerPerson.getErrorMultiplierOfPower(cueParams.selectedPower());
            double powerSd = (100.0 - playerPerson.getPowerControl()) / 100.0;
            powerSd *= attackChoice.attackingPlayer.getInGamePlayer()
                    .getCueSelection().getSelected().getNonNullInstance().getPowerMultiplier();
            powerSd *= handSdMul;
            powerSd *= powerErrorFactor;  // 力量的标准差

            // 和杆还是有关系的，拿着大头杆打斯诺克就不会去想很难的球
            double actualSideSpin = cueParams.actualSideSpin();

            // dev=deviation, 由于力量加上塞造成的1倍标准差偏差角，应为小于PI的正数
            double[] devOfLowPower = CuePlayParams.unitXYWithSpins(actualSideSpin,
                    cueParams.actualPower() * (1 - powerSd), 1, 0);
            double radDevOfLowPower = Algebra.thetaOf(devOfLowPower);
            if (radDevOfLowPower > Math.PI)
                radDevOfLowPower = Algebra.TWO_PI - radDevOfLowPower;

            double[] devOfHighPower = CuePlayParams.unitXYWithSpins(actualSideSpin,
                    cueParams.actualPower() * (1 + powerSd), 1, 0);
            double radDevOfHighPower = Algebra.thetaOf(devOfHighPower);
            if (radDevOfHighPower > Math.PI)
                radDevOfHighPower = Algebra.TWO_PI - radDevOfHighPower;

            double sideDevRad = (radDevOfHighPower - radDevOfLowPower) / 2;

            // 太小的力有惩罚
//            if (actualPower < 15.0) {
//                double penalty = Algebra.shiftRange(0, 15, 2, 1, actualPower);
//                sideDevRad *= penalty;
//            }

            // 瞄准的1倍标准差偏差角
            double aimingSd = (105 - aps.precision) * handSdMul /
                    AiCueResult.DEFAULT_AI_PRECISION;  // 这里用default是因为，我们不希望把AI精确度调低之后它就觉得打不进，一直防守

            double totalDt = attackChoice.targetHoleDistance + attackChoice.whiteCollisionDistance;
            double whiteInitSpeed = CuePlayParams.getSpeedOfPower(cueParams.actualPower(), 0);
            double totalMove = gameValues.estimatedMoveDistance(phy, whiteInitSpeed);

            // 预估台泥变线偏差
            double moveT = gameValues.estimateMoveTime(phy, whiteInitSpeed, totalDt);
//            double whiteT = gameValues.estimateMoveTime(phy, )
            double pathChange = moveT * phy.cloth.goodness.errorFactor * TableCloth.RANDOM_ERROR_FACTOR;  // 变线
//            System.out.println("Path change " + pathChange);  

            // 白球的偏差标准差
            double whiteBallDevRad = sideDevRad + aimingSd;
            // 白球在撞击点时的偏差标准差，毫米
            // 这里sin和tan应该差不多，都不准确，tan稍微好一点点
            double sdCollisionMm = Math.tan(whiteBallDevRad) * attackChoice.whiteCollisionDistance +
                    pathChange;  // todo: 这里把白球+目标球的变线全算给白球了

            // 目标球出发角的大致偏差，标准差。 todo: 目前的算法导致了AI认为近乎90度的薄球不难
            double tarDevSdRad = Math.asin(sdCollisionMm / gameValues.ball.ballDiameter);

            // todo: 1 / cos是权宜之计
            tarDevSdRad *= 1 / Math.cos(attackChoice.angleRad);
            if (tarDevSdRad > Algebra.HALF_PI) {
                tarDevSdRad = Algebra.HALF_PI;
            }

            // 目标球到袋时的大致偏差标准差，mm
            double tarDevHoleSdMm = Math.tan(tarDevSdRad) * attackChoice.targetHoleDistance;

            // 角度球的瞄准难度：从白球处看目标球和袋，在视线背景上的投影距离
            double targetAimingOffset =
                    Math.cos(Math.PI / 2 - attackChoice.angleRad) * attackChoice.targetHoleDistance;

            double allowedDev;
            NormalDistribution nd;
            if (attackChoice instanceof AttackChoice.DirectAttackChoice dac) {
                // 举个例子，瞄准为90的AI，白球在右顶袋打蓝球右底袋时，offset差不多1770，下面这个值在53毫米左右
                double targetDifficultyMm = targetAimingOffset * (105 - aps.precision) / 500;

                tarDevHoleSdMm += targetDifficultyMm;

                // 从这个角度看袋允许的偏差
                allowedDev = AttackChoice.DirectAttackChoice.allowedDeviationOfHole(
                        gameValues,
                        attackChoice.isMidHole(),
                        dac.targetHoleVec,
                        totalMove - totalDt  // 真就大概了，意思也就是不鼓励AI低搓去沙中袋
                );
                nd = new NormalDistribution(0.0, Math.max(tarDevHoleSdMm * 2, 0.00001));
            } else if (attackChoice instanceof AttackChoice.DoubleAttackChoice doubleAc) {
                // 稍微给高点
                // 除数越大，AI越倾向打翻袋
                double targetDifficultyMm = targetAimingOffset * (105 - aps.doubleAbility) / 400;

                tarDevHoleSdMm += targetDifficultyMm;

                allowedDev = AttackChoice.DirectAttackChoice.allowedDeviationOfHole(
                        gameValues,
                        attackChoice.isMidHole(),
                        doubleAc.lastCushionToPocket,
                        totalMove - totalDt  // 真就大概了，意思也就是不鼓励AI低搓去沙中袋
                );
//                System.out.println("Double sd: " + tarDevHoleSdMm * 2 + ", allow dev: " + allowedDev);
//                System.out.println(targetDifficultyMm + " " + tarDevSdRad + " " + attackChoice.targetHoleDistance);
                nd = new NormalDistribution(0.0, Math.max(tarDevHoleSdMm * 2, 0.00001));
            } else {
                EventLogger.error("Unknown attack choice: " + attackChoice);
                potProb = 0.0;
                price = 0.0;
                return;
            }

            potProb = nd.cumulativeProbability(allowedDev) - nd.cumulativeProbability(-allowedDev);
            if (potProb < 0) potProb = 0.0;  // 虽然我不知道为什么prob会是负的
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

        public double getPotProb() {
            return potProb;
        }

        public double getPrice() {
            return price;
        }
    }

    private class AngleSnookerSolver implements Runnable {

        final List<DefenseThread> threadsOfAngle = new ArrayList<>();  // 力量必须从小到大
        final List<FinalChoice.DefenseChoice> results = new ArrayList<>();
        double angleDeg;
        boolean smallPower;

        AngleSnookerSolver(double angleDeg, boolean smallPower) {
            this.angleDeg = angleDeg;
            this.smallPower = smallPower;
        }

        @Override
        public void run() {
            for (DefenseThread dt : threadsOfAngle) {
                dt.run();
                if (dt.result != null) {
                    results.add(dt.result);
                    if (smallPower) return;  // 刚好够碰到，可以了
                }
            }
        }
    }

    protected static class AttackThread implements Runnable {

        final AttackParam attackParams;
        final GameValues values;
        final int nextTarget;
        final List<Ball> nextStepLegalBalls;
        final Phy phy;
        final GamePlayStage stage;
        final Game[] gameClonesPool;
        final Player aiPlayer;
        final KickPriceCalculator kickPriceCalculator;
//        Game<?, ?> game2;  // todo: 这里的clone可以用线程池优化

        FinalChoice.IntegratedAttackChoice result;

        protected AttackThread(AttackParam attackParams,
                               GameValues values,
                               int nextTarget,
                               List<Ball> nextStepLegalBalls,
                               Phy phy,
                               Game[] gameClonesPool,
                               Player aiPlayer,
                               GamePlayStage stage,
                               KickPriceCalculator kickPriceCalculator) {
            this.attackParams = attackParams;
            this.values = values;
            this.nextTarget = nextTarget;
            this.nextStepLegalBalls = nextStepLegalBalls;
            this.phy = phy;
            this.gameClonesPool = gameClonesPool;
            this.stage = stage;
            this.aiPlayer = aiPlayer;
            this.kickPriceCalculator = kickPriceCalculator;
        }

        @Override
        public void run() {
            int threadIndex = (int) (Thread.currentThread().getId() % gameClonesPool.length);
            Game<?, ?> copy = gameClonesPool[threadIndex];
            //        System.out.print(selectedPower);
            double actualFbSpin = attackParams.cueParams.actualFrontBackSpin();
            double actualSideSpin = attackParams.cueParams.actualSideSpin();
            double actualPower = attackParams.cueParams.actualPower();

            double[] correctedDirection = CuePlayParams.aimingUnitXYIfSpin(
                    actualSideSpin,
                    actualPower,
                    attackParams.attackChoice.cueDirectionUnitVector[0],
                    attackParams.attackChoice.cueDirectionUnitVector[1]
            );

            // 考虑上塞之后的修正出杆
            AttackChoice correctedChoice =
                    attackParams.attackChoice.copyWithNewDirection(correctedDirection);

            CuePlayParams params = CuePlayParams.makeIdealParams(
                    correctedChoice.cueDirectionUnitVector[0],
                    correctedChoice.cueDirectionUnitVector[1],
                    attackParams.cueParams,
                    0.0
            );
            boolean checkPot = attackParams.attackChoice instanceof AttackChoice.DoubleAttackChoice;
//            game2 = checkPot ? game.clone() : game;

            // 直接能打到的球，必不会在打到目标球之前碰库
            WhitePrediction wp = copy.predictWhite(params, phy, 0.0,
                    true,
                    true,
                    checkPot,
                    true,
                    false);
            if (wp == null) {
                // 这情况非同寻常
                EventLogger.warning("White prediction is null");
                return;
            }
            if (wp.getFirstCollide() == null) {
                // 连球都碰不到，没吃饭？
//            System.out.println("too less");
                return;
            }
            if (checkPot && (!wp.willFirstBallPot() || wp.isCueBallFirstBallTwiceColl())) {
                // 进不了的翻袋，或是母球与目标球二次碰撞
                return;
            }
//            if (wp.getWhiteCushionCountAfter() == 0 && attackParams.selectedSideSpin != 0.0) {
//                // 不吃库的球加个卵的塞
//                return;
//            }

            double estBallSpeed = wp.getBallInitSpeed();
            double errorLow = aiPlayer.getPlayerPerson().getPowerSd(
                    attackParams.cueParams.selectedPower(),
                    attackParams.attackChoice.handSkill) * 1.96;
            double estBallSpeedLow = estBallSpeed * (1 - errorLow);  //  95%置信区间下界，没有考虑旋转这些

            double targetCanMove = values.estimatedMoveDistance(phy, estBallSpeedLow) * 0.95;  // 补偿：球的初始旋转是0，一开始的滑动摩擦会让目标球比预期的少跑一点
            if (targetCanMove - values.ball.ballDiameter * 2.0 <= correctedChoice.targetHoleDistance) {
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
            if (copy instanceof AbstractSnookerGame asg) {
                asg.pickupPottedBallsLast(correctedChoice.attackTarget);
            }
            List<AttackChoice> nextStepDirectAttackChoices =
                    getAttackChoices(copy,
                            nextTarget,
                            aiPlayer,
                            wp.getFirstCollide(),
                            nextStepLegalBalls,
                            whiteStopPos,
                            true,
                            true);
            AttackParam correctedParams = attackParams.copyWithCorrectedChoice(
                    correctedChoice
            );
            result = new FinalChoice.IntegratedAttackChoice(
                    copy,
                    correctedParams,
                    nextStepDirectAttackChoices,
                    nextTarget,
                    params,
                    wp,
                    stage,
                    kickPriceCalculator
            );
        }
    }

    protected class DefenseThread implements Runnable {

        double rad;
        double nativePrice;
        double[] whitePos;
        double selectedPower;
        Set<Ball> legalSet;
        Phy phy;
        Game[] gameClonesPool;
        boolean allowPocketCorner;

        FinalChoice.DefenseChoice result;

        protected DefenseThread(double rad,
                                double nativePrice,
                                double[] whitePos,
                                double selectedPower,
                                Set<Ball> legalSet,
                                Phy phy,
                                Game[] gameClonesPool,
                                boolean allowPocketCorner) {
            this.rad = rad;
            this.nativePrice = nativePrice;
            this.whitePos = whitePos;
            this.selectedPower = selectedPower;
            this.legalSet = legalSet;
            this.phy = phy;
            this.gameClonesPool = gameClonesPool;
            this.allowPocketCorner = allowPocketCorner;
        }

        @Override
        public void run() {
            int threadIndex = (int) (Thread.currentThread().getId() % gameClonesPool.length);
            Game<?, P> copy = gameClonesPool[threadIndex];

            double[] unitXY = Algebra.unitVectorOfAngle(rad);

            PlayerPerson.HandSkill handSkill = CuePlayParams.getPlayableHand(
                    whitePos[0],
                    whitePos[1],
                    unitXY[0],  // fixme: 这里存疑
                    unitXY[1],
                    0.0,
                    copy.getGameValues().table,
                    copy.getCuingPlayer().getPlayerPerson()
            );

            CueParams cueParams = CueParams.createBySelected(
                    selectedPower,
                    0.0,
                    0.0,
                    game,
                    aiPlayer.getInGamePlayer(),
                    handSkill
            );

            CuePlayParams cpp = CuePlayParams.makeIdealParams(
                    unitXY[0],
                    unitXY[1],
                    cueParams,
                    0.0
            );
            result = analyseDefense(
                    AiCue.this,
                    cpp,
                    cueParams,
                    phy,
                    copy,
                    legalSet,
                    aiPlayer,
                    unitXY,
                    false,
                    nativePrice,
                    allowPocketCorner
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

        FinalChoice.DefenseChoice result;
        double targetCanMove;

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
            int threadIndex = (int) (Thread.currentThread().getId() % gameClonesPool.length);
            Game<?, P> copy = gameClonesPool[threadIndex];

            double actualFbSpin = attackParam.cueParams.actualFrontBackSpin();
            double actualSideSpin = attackParam.cueParams.actualSideSpin();
            double actualPower = attackParam.cueParams.actualPower();

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
                    attackParam.cueParams,
                    0.0
            );

            result = analyseDefense(
                    AiCue.this,
                    params,
                    attackParam.cueParams,
                    phy,
                    copy,
                    legalSet,
                    aiPlayer,
                    correctedChoice.cueDirectionUnitVector,
                    true,
                    1.0,  // 进攻杆，AI应该不会吃屎去擦最薄边
                    false
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
