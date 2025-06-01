package trashsoftware.trashSnooker.core.ai;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.attempt.CueType;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.metrics.Rule;
import trashsoftware.trashSnooker.core.movement.WhitePrediction;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.snooker.AbstractSnookerGame;
import trashsoftware.trashSnooker.fxml.projection.ObstacleProjection;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Util;
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

    //    public static final double NO_DIFFICULTY_ANGLE_RAD = 0.3;
//    public static final double EACH_BALL_SEE_PRICE = 0.5;
    public static final double WHITE_HIT_CORNER_PENALTY = 0.05;
    public static final double KICK_USELESS_BALL_MUL = 0.5;
    public static final double POWER_TICK_EXP = 1.35;
    //    private static final double[] FRONT_BACK_SPIN_POINTS =
//            {0.0, -0.27, 0.27, -0.54, 0.54, -0.81, 0.81};
    protected static final double[] ATTACK_CUE_ANGLES = {
            5.0, 15.0, 30.0
    };
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
    protected static final double[][] DEFENSE_SPIN_POINTS = {  // 防守的打点
            {0.0, 0.0}, {0.45, 0.0}, {0.9, 0.0}
    };
    public static boolean aiOnlyDefense = false;
    public static boolean aiOnlyDouble = false;
    protected Ball presetTarget;

    static {
        Arrays.sort(ATTACK_SPIN_POINTS, Comparator.comparingDouble(a -> Math.abs(a[0]) + Math.abs(a[1])));
        Arrays.sort(DOUBLE_POT_SPIN_POINTS, Comparator.comparingDouble(a -> Math.abs(a[0]) + Math.abs(a[1])));
    }

    final double opponentPureAtkProb;  // 对手会直接进攻的界限
    final double opponentDefAtkProb;
    protected int nThreads;
    protected G game;
    protected P aiPlayer;

    protected final Map<AttackChoice, List<AttackParam>> lastResortAttackChoices = new TreeMap<>();
    protected FinalChoice.IntegratedAttackChoice bestAttack;  // 记录一下，不管最后有没有用它

    public AiCue(G game, P aiPlayer) {
        this.game = game;
        this.aiPlayer = aiPlayer;

        this.nThreads = Math.max(1,
                Math.min(32,
                        ConfigLoader.getInstance().getInt("nThreads", 4)));

        P opponent = game.getAnotherPlayer(aiPlayer);
        opponentPureAtkProb = Analyzer.attackProbThreshold(0.4, opponent.getPlayerPerson().getAiPlayStyle());
        opponentDefAtkProb = Analyzer.defensiveAttackProbThreshold(opponent.getPlayerPerson().getAiPlayStyle());
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
        System.out.println(legalBalls);

        PlayerPerson aps = aiPlayer.getPlayerPerson();
        double degreesTick = 100.0 / 2 / aps.getSolving();
//        double powerTick = 1000.0 / aps.getSolving();

        int continuousFoulAndMiss = game.getContinuousFoulAndMiss();
        double chanceOfSmallPower = Math.pow(continuousFoulAndMiss, 1.35) / 18 + 0.5;  // 到第6杆时必定小力了
        if (Math.random() < chanceOfSmallPower) {
            System.out.println("AI solving snooker small power!");
            return solveSnookerDefense(legalBalls, degreesTick, phy, true, allowPocketCorner);
        } else {
            System.out.println("AI solving snooker!");
            return solveSnookerDefense(legalBalls, degreesTick, phy, false, allowPocketCorner);
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

    private double[] powerValuesOfCueAngle(double[] origValues, double origLimit, double cueAngleDeg) {
        List<Double> availValues = new ArrayList<>();
        double ctrlPower = CuePlayParams.powerWithCueAngle(
                aiPlayer.getPlayerPerson().handBody,
                aiPlayer.getInGamePlayer().getCueSelection().getSelected().brand,
                origLimit,
                cueAngleDeg
        );
        for (double ov : origValues) {
            if (ov <= ctrlPower) availValues.add(ov);
            else {
                availValues.add(ctrlPower);
                break;
            }
        }
        double[] res = new double[availValues.size()];
        for (int i = 0; i < res.length; i++) res[i] = availValues.get(i);
        return res;
    }

    private FinalChoice.IntegratedAttackChoice tryLastResortAttack(Phy phy) {
        if (lastResortAttackChoices.isEmpty()) return null;

        FinalChoice.IntegratedAttackChoice best = null;

        for (var entry : lastResortAttackChoices.entrySet()) {
            System.out.println(entry.getValue().size() + " Last resort attacks");
            AttackChoice choice = entry.getKey();
            if (aiOnlyDouble && !(choice instanceof AttackChoice.DoubleAttackChoice)) continue;
            int nextTarget = game.getTargetAfterPotSuccess(choice.ball, game.isDoingSnookerFreeBll());
            
            FinalChoice.IntegratedAttackChoice iac = pureAttack(
                    entry.getValue(),
                    nextTarget,
                    nextStepLegalBalls(nextTarget, choice.ball),
                    phy,
                    game.getGamePlayStage(choice.ball, false),
                    true,
                    null
            );
            if (best == null || iac != null) {
                if (best == null || iac.price > best.price) {
                    best = iac;
                }
            }
        }
        return best;
    }

    @SuppressWarnings("unchecked")
    private FinalChoice.IntegratedAttackChoice pureAttack(
            List<AttackParam> pureAttacks,
            int nextTarget,
            List<Ball> nextStepLegalBalls,
            Phy phy,
            GamePlayStage stage,
            boolean mustAttack,
            @Nullable Game<?, P>[] gameClonesPool) {
        long t1 = System.currentTimeMillis();
        if (gameClonesPool == null) {
            gameClonesPool = (Game<?, P>[]) new Game[nThreads];
            for (int i = 0; i < gameClonesPool.length; i++) {
                gameClonesPool[i] = game.clone();
            }
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

        try (ExecutorService executorService = Executors.newFixedThreadPool(nThreads)) {
            for (AttackThread thread : attackThreads) {
                executorService.execute(thread);
            }
            executorService.shutdown();

            if (!executorService.awaitTermination(1, TimeUnit.MINUTES))
                throw new RuntimeException("AI thread not terminated.");  // Wait for all threads complete.
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        long t2 = System.currentTimeMillis();

        List<FinalChoice.IntegratedAttackChoice> choiceList = new ArrayList<>();
        for (AttackThread thread : attackThreads) {
            if (thread.result != null) choiceList.add(thread.result);
        }
        System.out.println("Ai calculated attacks of given choice in " + (t2 - t1) + " ms, " + choiceList.size() + " valid results");

        AiPlayStyle aps = aiPlayer.getPlayerPerson().getAiPlayStyle();

        if (!choiceList.isEmpty()) {
            double pureAttackThresh = Analyzer.attackProbThreshold(PURE_ATTACK_PROB, aps);

            choiceList.sort(FinalChoice.IntegratedAttackChoice::normalCompareTo);
            for (FinalChoice.IntegratedAttackChoice iac : choiceList) {
                if (!iac.nextStepAttackChoices.isEmpty()) {
                    AttackChoice bestNextStep = iac.nextStepAttackChoices.getFirst();
                    if (bestNextStep.defaultRef.potProb >= pureAttackThresh) {
                        System.out.println("Penalty, tor = " + iac.penalty + ", " + iac.positionErrorTolerance);
                        return iac;  // 我纯进攻下一杆也得走个纯进攻的位撒
                    }
                }
            }

            FinalChoice.IntegratedAttackChoice iac = choiceList.getFirst();
            if (iac.nextStepTarget == Game.END_REP || mustAttack) {
                System.out.println("Attack a must attack one");
                return iac;
            }

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
        }
        return null;
    }

    private FinalChoice.IntegratedAttackChoice attack(AttackChoice choice,
                                                      int nextTarget,
                                                      List<Ball> nextStepLegalBalls,
                                                      Phy phy,
                                                      boolean mustAttack) {
        double powerLimit = choice.handSkill.getControllablePowerPercentage();

//        double minPower;
//        double tick;
        double[][] availSpins;
        double[] powerValues;

        int totalTicks = (int) Math.round(aiPlayer.getPlayerPerson().getAiPlayStyle().position / 5);

        if (choice instanceof AttackChoice.DirectAttackChoice) {
            powerValues = Algebra.generateSkewedRange(100.0 / totalTicks, powerLimit, totalTicks, POWER_TICK_EXP);
            availSpins = ATTACK_SPIN_POINTS;
        } else if (choice instanceof AttackChoice.DoubleAttackChoice) {
            totalTicks /= 2;
            powerValues = Algebra.generateSkewedRange(12.0, powerLimit, totalTicks, POWER_TICK_EXP);
            availSpins = DOUBLE_POT_SPIN_POINTS;
        } else {
            throw new RuntimeException();
        }

        AiPlayStyle aps = aiPlayer.getPlayerPerson().getAiPlayStyle();
//        double likeShow = aiPlayer.getPlayerPerson().getAiPlayStyle().likeShow;  // 喜欢大力及杆法的程度
        GamePlayStage stage = game.getGamePlayStage(choice.ball, false);

        long t0 = System.currentTimeMillis();

        double pureAttackThreshold = Analyzer.attackProbThreshold(PURE_ATTACK_PROB, aps);  // 进球概率高于这个值，AI就纯进攻
        double defensiveAttackThreshold = Analyzer.defensiveAttackProbThreshold(aps);

        List<AttackParam> pureAttacks = new ArrayList<>();
        List<AttackParam> defensiveAttacks = new ArrayList<>();  // 连打带防
        Cue cue = aiPlayer.getInGamePlayer().getCueSelection().getSelected().getNonNullInstance();
        double cueTipBallRatio = cue.getCueTipWidth() / game.getGameValues().ball.ballDiameter;

        double easiest = 0.0;
        for (double cueAngle : ATTACK_CUE_ANGLES) {
            double[] dirVec;
            if (choice instanceof AttackChoice.DoubleAttackChoice dou) {
                dirVec = new double[]{
                        dou.collisionPos[0] - dou.whitePos[0],
                        dou.collisionPos[1] - dou.whitePos[1]
                };
                dirVec = Algebra.unitVector(dirVec);
            } else {
                dirVec = choice.cueDirectionUnitVector;
            }

            // 这里其实只是近似值，因为后面有让点、弧线那些情况，会导致这个稍有不同
            CueBackPredictor.Result backPre =
                    game.getObstacleDtHeight(dirVec[0], dirVec[1],
                            cue.getCueTipWidth());
            ObstacleProjection op = ObstacleProjection.createProjection(
                    backPre,
                    dirVec[0], dirVec[1],
                    cueAngle,
                    game.getCueBall(),
                    game.getGameValues(),
                    cue.getCueTipWidth()
            );
//            System.out.println("Obstacle: " + op);
            int availSpinCount = 0;

            double[] availPowers = powerValuesOfCueAngle(powerValues,
                    powerLimit,
                    cueAngle);

            for (double selectedPower : availPowers) {
                for (double[] spins : availSpins) {
                    double[] realSpins = cue.aiCuePoint(spins, game.getGameValues().ball);

                    if (op != null && !op.cueAble(realSpins[1], -realSpins[0], cueTipBallRatio)) {
                        // op是null就说明没有障碍，可以打，不进这个分支
                        // 注意是反过来的
                        continue;
                    }
                    if (selectedPower == powerValues[0]) availSpinCount++;  // 只记一遍就行了，力量应该不影响打点

                    CueParams cueParams = CueParams.createBySelected(
                            selectedPower,
                            realSpins[0],
                            realSpins[1],
                            cueAngle,
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

//            System.out.println("Avail points of " + cueAngle + ": " + availSpinCount + ", thresh " + (availSpins.length * 0.4));
            if (availSpinCount == availSpins.length) {
                break;
            }
        }

        long t1 = System.currentTimeMillis();
        System.out.println("Ai list attack params in " + (t1 - t0) + " ms, " +
                pureAttacks.size() + " pure, " + defensiveAttacks.size() + " defensive.");

        if (pureAttacks.isEmpty() && defensiveAttacks.isEmpty()) {
            System.out.println("Not attack because the highest prob is " + easiest);
            return null;
        }

        pureAttacks.sort((a, b) -> -Double.compare(a.potProb, b.potProb));
        defensiveAttacks.sort((a, b) -> -Double.compare(a.potProb, b.potProb));

        // 只留简单的
        pureAttacks = new ArrayList<>(pureAttacks.subList(0, Math.min(64, pureAttacks.size())));
        defensiveAttacks = new ArrayList<>(defensiveAttacks.subList(0, Math.min(64, defensiveAttacks.size())));

        List<AttackParam> backups = new ArrayList<>(pureAttacks);
        backups.addAll(defensiveAttacks);
        lastResortAttackChoices.put(choice, backups);

        @SuppressWarnings("unchecked")
        Game<?, P>[] gameClonesPool = (Game<?, P>[]) new Game[nThreads];
        for (int i = 0; i < gameClonesPool.length; i++) {
            gameClonesPool[i] = game.clone();
        }

        FinalChoice.IntegratedAttackChoice pureIac = pureAttack(pureAttacks,
                nextTarget,
                nextStepLegalBalls,
                phy,
                stage,
                mustAttack,
                gameClonesPool);
        if (pureIac != null) return pureIac;

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

        try (ExecutorService executorService2 = Executors.newFixedThreadPool(nThreads)) {
            for (DefensiveAttackThread thread : defensiveThreads) {
                executorService2.execute(thread);
            }
            executorService2.shutdown();

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
                    phy,
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

    protected AiCueResult makeAttackCue(FinalChoice.IntegratedAttackChoice iac, CueType cueType) {
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
                iac,
                game.frameImportance(aiPlayer.getInGamePlayer().getPlayerNumber()));
        List<double[]> whitePath;
        if (iac.whitePrediction != null) {
            whitePath = iac.whitePrediction.getWhitePath();
        } else {
            WhitePrediction wp = game.predictWhite(iac.params,
                    game.getEntireGame().predictPhy,
                    0.0,
                    true,
                    true, false, false,
                    true, true);
            whitePath = wp.getWhitePath();
        }

        acr.setWhitePath(whitePath);
        return acr;
    }

    protected AiCueResult makeDefenseCue(FinalChoice.DefenseChoice choice, CueType cueType) {
        AiCueResult acr = new AiCueResult(aiPlayer.getInGamePlayer(),
                game.getGamePlayStage(choice.ball, true),
                cueType,
                null,
                null,
                choice.ball,
                choice.cueDirectionUnitVector[0],
                choice.cueDirectionUnitVector[1],
                choice.cueParams,
                choice,
                game.frameImportance(aiPlayer.getInGamePlayer().getPlayerNumber()));
        acr.setWhitePath(choice.wp != null ? choice.wp.getWhitePath() : null);
        return acr;
    }

    protected AiCueResult regularCueDecision(Phy phy) {
        if (game.isBreaking()) {
            FinalChoice.DefenseChoice breakChoice = breakCue(phy);
            System.out.println("AI break: " + breakChoice);
            if (breakChoice != null) return makeDefenseCue(breakChoice, CueType.BREAK);
        }

        if (!aiOnlyDefense) {
            FinalChoice.IntegratedAttackChoice attackChoice = standardAttack(phy, currentMustAttack());
            if (attackChoice != null) {
                System.out.println("AI attack");
                return makeAttackCue(attackChoice);
            }
        }
        FinalChoice.DefenseChoice stdDefense = standardDefense();
        if (stdDefense != null) {
            System.out.println("AI standard defense");
            return makeDefenseCue(stdDefense, CueType.DEFENSE);
        }
        FinalChoice.DefenseChoice defenseChoice = getBestDefenseChoice(phy);
        if (defenseChoice != null) {
            System.out.println("AI defense");
            System.out.println(defenseChoice);
//            System.out.printf("Best defense choice: %f %f %f %f %f\n", 
//                    defenseChoice.price, defenseChoice.snookerPrice, defenseChoice.opponentAttackPrice,
//                    defenseChoice.penalty, defenseChoice.tolerancePenalty);
            if (!aiOnlyDefense && 
                    defenseChoice.opponentCanPureAttack(game.getAnotherPlayer(aiPlayer).getPlayerPerson().getAiPlayStyle())) {
                System.out.println("Defense not good, try last resort");
                FinalChoice.IntegratedAttackChoice iac = tryLastResortAttack(phy);
                if (iac != null) {
                    return makeAttackCue(iac);
                }
            }
            return makeDefenseCue(defenseChoice, CueType.DEFENSE);
        }
        FinalChoice.DefenseChoice solveSnooker = solveSnooker(phy, false);
        if (solveSnooker != null) {
            System.out.println("AI solve snooker");
            System.out.println(solveSnooker);
            return makeDefenseCue(solveSnooker, CueType.SOLVE);
        }
        FinalChoice.DefenseChoice solveSnooker2 = solveSnooker(phy, true);  // 只能说是逼急了，来个袋角解斯诺克
        System.out.println("Cannot solve snooker! Try pocket arc!");
        if (solveSnooker2 != null) {
            System.out.println("AI solve snooker by pocket arc");
            return makeDefenseCue(solveSnooker2, CueType.SOLVE);
        }
        System.out.println("Ai random angry cue");
        return randomAngryCue();
    }

    protected FinalChoice.IntegratedAttackChoice standardAttack(Phy phy, boolean mustAttack) {
        if (aiOnlyDefense) return null;
        List<AttackChoice> all;
        if (presetTarget == null) {
            all = getCurrentAttackChoices();
        } else {
            all = Analyzer.getAttackChoices(game,
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
    
    private List<Ball> nextStepLegalBalls(int nextTarget, @Nullable Ball selfBall) {
        List<Ball> nextStepLegalBalls =
                game.getAllLegalBalls(nextTarget,
                        false,
                        false);  // 这颗进了下一颗怎么可能是自由球/手中球

        if (game.getGameType() == GameRule.CHINESE_EIGHT || game.getGameType() == GameRule.LIS_EIGHT) {
            // 避免AI打自己较自己的可能（并不确定会发生）
            nextStepLegalBalls.remove(selfBall);
        }
        return nextStepLegalBalls;
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
                List<Ball> nextStepLegalBalls = nextStepLegalBalls(nextTargetIfThisSuccess, choice.ball);

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
        double[] directionVec = Algebra.unitVectorOfAngle(directionRad);

        Cue cue = aiPlayer.getInGamePlayer().getCueSelection().getSelected().getNonNullInstance();

        double[] cuePointAngle = Analyzer.findCueAblePointAndAngle(
                game,
                cue,
                directionVec,
                DEFENSE_SPIN_POINTS
        );

        PlayerHand handSkill = CuePlayParams.getPlayableHand(
                game.getCueBall().getX(), game.getCueBall().getY(),
                directionVec[0], directionVec[1],
                cuePointAngle[2],
                game.getGameValues().table,
                game.getCuingPlayer().getPlayerPerson()
        );
//        double power = random.nextDouble() *
//                (handSkill.getMaxPowerPercentage() - 20.0) + 20.0;
        double power = CuePlayParams.powerWithCueAngle(
                aiPlayer.getPlayerPerson().handBody,
                cue.getBrand(),
                handSkill.getMaxPowerPercentage(),
                cuePointAngle[2]
        );
        CueParams cueParams = CueParams.createBySelected(
                power,
                cuePointAngle[0],
                cuePointAngle[1],
                cuePointAngle[2],
                game,
                aiPlayer.getInGamePlayer(),
                handSkill
        );
        return new AiCueResult(
                aiPlayer.getInGamePlayer(),
                GamePlayStage.NORMAL,
                CueType.DEFENSE,
                null,
                null,
                null,
                directionVec[0],
                directionVec[1],
                cueParams,
                null,
                game.frameImportance(aiPlayer.getInGamePlayer().getPlayerNumber())
        );
    }

    List<AttackChoice> getAttackChoices(int attackTarget,
                                        Ball lastPottingBall,
                                        boolean isSnookerFreeBall,
                                        double[] whitePos,
                                        boolean isPositioning,
                                        boolean isLineInHandBall,
                                        boolean considerDoublePot) {
        return Analyzer.getAttackChoices(game,
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
                                                    double actualPowerLow,
                                                    double actualPowerHigh,
                                                    Phy phy) {
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
            if (!game.pointToPointCanPassBall(whitePos[0], whitePos[1],
                    ball.getX(), ball.getY(), game.getCueBall(), ball,
                    false, false)) {
                continue;
            }

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
        @SuppressWarnings("unchecked")
        Game<?, P>[] gameClonesPool = (Game<?, P>[]) new Game[nThreads];
        for (int i = 0; i < gameClonesPool.length; i++) {
            gameClonesPool[i] = game.clone();
        }

        int totalTicks = (int) Math.round(aiPlayer.getPlayerPerson().getAiPlayStyle().defense / 5);
        double[] selectedPowerArray = Algebra.generateSkewedRange(
                selPowLow,
                selPowHigh,
                totalTicks,
                POWER_TICK_EXP
        );

        if (availableRads.size() > 180) {
            Util.randomShrinkTo(availableRads, 180);
        }

        for (double selectedPower : selectedPowerArray) {
            for (DefenseAngle da : availableRads) {
                DefenseThread thread = new DefenseThread(
                        da.rad, da.price, whitePos, selectedPower, false,
                        legalSet, phy, gameClonesPool, false
                );
                defenseThreads.add(thread);
            }
        }

        System.out.println(availableRads.size() + " defense angles, " + defenseThreads.size() + " defenses");

        try (ExecutorService executorService = Executors.newFixedThreadPool(nThreads)) {
            for (DefenseThread thread : defenseThreads) {
                executorService.execute(thread);
            }
            executorService.shutdown();

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
                                                          Phy phy,
                                                          boolean smallPower,
                                                          boolean allowPocketCorner) {
        Set<Ball> legalSet = new HashSet<>(legalBalls);
//        DefenseChoice best = null;

        Ball cueBall = game.getCueBall();
        double[] whitePos = new double[]{cueBall.getX(), cueBall.getY()};

        @SuppressWarnings("unchecked")
        Game<?, P>[] gameClonesPool = (Game<?, P>[]) new Game[nThreads];
        for (int i = 0; i < gameClonesPool.length; i++) {
            gameClonesPool[i] = game.clone();
        }

        double[] selectedPowerArray = getSelectedPowerArray(smallPower);

        List<AngleSnookerSolver> angleSolvers = new ArrayList<>();

        for (double deg = 0.0; deg < 360; deg += degreesTick) {
            AngleSnookerSolver ass = new AngleSnookerSolver(deg, smallPower);
            for (double selectedPower : selectedPowerArray) {
                DefenseThread thread = new DefenseThread(
                        Math.toRadians(deg),
                        1.0,  // 这里我们根本无法判断，只能给1.0了
                        whitePos,
                        selectedPower,
                        true,
                        legalSet,
                        phy,
                        gameClonesPool,
                        allowPocketCorner
                );
                ass.threadsOfAngle.add(thread);
            }
            angleSolvers.add(ass);
        }

        try (ExecutorService executorService = Executors.newFixedThreadPool(nThreads)) {
            for (AngleSnookerSolver thread : angleSolvers) {
                executorService.execute(thread);
            }
            executorService.shutdown();

            if (!executorService.awaitTermination(1, TimeUnit.MINUTES))
                throw new RuntimeException("AI thread not terminated.");  // Wait for all threads complete.
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<FinalChoice.DefenseChoice> legalChoices = new ArrayList<>();
        for (AngleSnookerSolver ass : angleSolvers) {
            for (FinalChoice.DefenseChoice result : ass.results) {
                if (notViolateCushionRule(result)) {
                    if (smallPower && result.wp.getWhiteSpeedWhenHitFirstBall() > 100) {
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

    private double @NotNull [] getSelectedPowerArray(boolean smallPower) {
        double ctrlPower = aiPlayer.getPlayerPerson().handBody.getPrimary().getControllablePowerPercentage();

        double powerLimit = smallPower ?
                Math.min(45.0, ctrlPower) :
                ctrlPower;

        int totalTicks = (int) Math.round(aiPlayer.getPlayerPerson().getSolving() / 10.0);
        if (smallPower) totalTicks *= 2;
        return Algebra.generateSkewedRange(
                5.0,
                powerLimit,
                totalTicks,
                POWER_TICK_EXP
        );
    }

    protected FinalChoice.DefenseChoice getBestDefenseChoice(Phy phy) {
        return getBestDefenseChoice(
                5.0,
                CueParams.selectedPowerToActualPower(
                        game,
                        aiPlayer.getInGamePlayer(),
                        aiPlayer.getPlayerPerson().handBody.getPrimary().getControllablePowerPercentage(),
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
        return directDefense(legalBalls, degreesTick,
                actualPowerLow, actualPowerHigh, phy);
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
        final Game<?, ?>[] gameClonesPool;
        final Player aiPlayer;
        final KickPriceCalculator kickPriceCalculator;
//        Game<?, ?> game2;  // todo: 这里的clone可以用线程池优化

        FinalChoice.IntegratedAttackChoice result;

        protected AttackThread(AttackParam attackParams,
                               GameValues values,
                               int nextTarget,
                               List<Ball> nextStepLegalBalls,
                               Phy phy,
                               Game<?, ?>[] gameClonesPool,
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
            int threadIndex = (int) (Thread.currentThread().threadId() % gameClonesPool.length);
            Game<?, ?> copy = gameClonesPool[threadIndex];
            //        System.out.print(selectedPower);

            // todo: 有可能出现加了弧线就绕不过去那种情况
            double[] dirWithAngleCurve = Analyzer.estimateRealCueDirWithCurve(
                    copy,
                    copy.getEntireGame().predictPhy,
                    attackParams.cueParams,
                    attackParams.attackChoice.cueDirectionUnitVector,
                    false
            );

            // 考虑上塞之后的修正出杆
            AttackChoice correctedChoice =
                    attackParams.attackChoice.copyWithNewDirection(dirWithAngleCurve);

            CuePlayParams params = CuePlayParams.makeIdealParams(
                    correctedChoice.cueDirectionUnitVector[0],
                    correctedChoice.cueDirectionUnitVector[1],
                    attackParams.cueParams
            );
            boolean checkPot = attackParams.attackChoice instanceof AttackChoice.DoubleAttackChoice;
//            game2 = checkPot ? game.clone() : game;

            // 直接能打到的球，必不会在打到目标球之前碰库
            WhitePrediction wp = copy.predictWhite(params, phy, 0.0,
                    true,
                    true,
                    checkPot,
                    false,
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
            double errorLow = attackParams.attackChoice.handSkill.getPowerSd(
                    attackParams.cueParams.selectedPower()) * 1.96;
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
            double[] whiteStopPos = wp.stopPoint();
            if (copy instanceof AbstractSnookerGame asg) {
                asg.pickupPottedBallsLast(correctedChoice.attackTarget);
            }
            List<AttackChoice> nextStepDirectAttackChoices =
                    Analyzer.getAttackChoices(copy,
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
                    phy,
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
        boolean solving;
        Set<Ball> legalSet;
        Phy phy;
        Game<?, P>[] gameClonesPool;
        boolean allowPocketCorner;

        FinalChoice.DefenseChoice result;

        protected DefenseThread(double rad,
                                double nativePrice,
                                double[] whitePos,
                                double selectedPower,
                                boolean isSolving,
                                Set<Ball> legalSet,
                                Phy phy,
                                Game<?, P>[] gameClonesPool,
                                boolean allowPocketCorner) {
            this.rad = rad;
            this.nativePrice = nativePrice;
            this.whitePos = whitePos;
            this.selectedPower = selectedPower;
            this.solving = isSolving;
            this.legalSet = legalSet;
            this.phy = phy;
            this.gameClonesPool = gameClonesPool;
            this.allowPocketCorner = allowPocketCorner;
        }

        @Override
        public void run() {
            int threadIndex = (int) (Thread.currentThread().threadId() % gameClonesPool.length);
            Game<?, P> copy = gameClonesPool[threadIndex];

            double[] unitXY = Algebra.unitVectorOfAngle(rad);

            PlayerPerson playerPerson = aiPlayer.getPlayerPerson();
            Cue cue = aiPlayer.getInGamePlayer().getCueSelection().getSelected().getNonNullInstance();
            double[] cuePointAngle = Analyzer.findCueAblePointAndAngle(
                    copy,
                    cue,
                    unitXY,
                    DEFENSE_SPIN_POINTS
            );

            PlayerHand handSkill = CuePlayParams.getPlayableHand(
                    whitePos[0],
                    whitePos[1],
                    unitXY[0],  // fixme: 这里存疑
                    unitXY[1],
                    cuePointAngle[2],
                    copy.getGameValues().table,
                    playerPerson
            );

            double ctrlPowerLimit = CuePlayParams.powerWithCueAngle(
                    playerPerson.handBody,
                    cue.getBrand(),
                    handSkill.getControllablePowerPercentage(),
                    cuePointAngle[2]
            );
            if (selectedPower > ctrlPowerLimit) {
                // 发不了这个力
                return;
            }

            CueParams cueParams = CueParams.createBySelected(
                    selectedPower,
                    cuePointAngle[0],
                    cuePointAngle[1],
                    cuePointAngle[2],
                    game,
                    aiPlayer.getInGamePlayer(),
                    handSkill
            );

            CuePlayParams cpp = CuePlayParams.makeIdealParams(
                    unitXY[0],
                    unitXY[1],
                    cueParams
            );
            result = Analyzer.analyseDefense(
                    AiCue.this,
                    cpp,
                    cueParams,
                    phy,
                    copy,
                    legalSet,
                    aiPlayer,
                    unitXY,
                    false,
                    solving,
                    nativePrice,
                    allowPocketCorner,
                    true
            );
        }
    }

    class DefensiveAttackThread implements Runnable {

        double[] whitePos;
        Set<Ball> legalSet;
        Phy phy;
        Game<?, P>[] gameClonesPool;

        AttackParam attackParam;
        GameValues values;

        FinalChoice.DefenseChoice result;
//        double targetCanMove;

        protected DefensiveAttackThread(
                AttackParam attackParam,
                double[] whitePos,
                Set<Ball> legalSet,
                Phy phy,
                Game<?, P>[] gameClonesPool,
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
            int threadIndex = (int) (Thread.currentThread().threadId() % gameClonesPool.length);
            Game<?, P> copy = gameClonesPool[threadIndex];

            // todo: 有可能出现加了弧线就绕不过去那种情况
            double[] dirWithAngleCurve = Analyzer.estimateRealCueDirWithCurve(
                    copy,
                    copy.getEntireGame().predictPhy,
                    attackParam.cueParams,
                    attackParam.attackChoice.cueDirectionUnitVector,
                    false
            );

            // 考虑上塞之后的修正出杆
            AttackChoice correctedChoice = attackParam.attackChoice.copyWithNewDirection(dirWithAngleCurve);
            attackParam = new AttackParam(attackParam, correctedChoice);

            CuePlayParams params = CuePlayParams.makeIdealParams(
                    correctedChoice.cueDirectionUnitVector[0],
                    correctedChoice.cueDirectionUnitVector[1],
                    attackParam.cueParams
            );

            result = Analyzer.analyseDefense(
                    AiCue.this,
                    params,
                    attackParam.cueParams,
                    phy,
                    copy,
                    legalSet,
                    aiPlayer,
                    correctedChoice.cueDirectionUnitVector,
                    true,
                    false,
                    1.0,  // 进攻杆，AI应该不会吃屎去擦最薄边
                    false,
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
