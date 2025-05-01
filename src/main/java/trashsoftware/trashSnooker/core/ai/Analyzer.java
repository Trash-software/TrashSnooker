package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.metrics.BallMetrics;
import trashsoftware.trashSnooker.core.movement.WhitePrediction;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.fxml.projection.ObstacleProjection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Analyzer {

    static final double STD_LENGTH_AFTER_WALL = 50000.0;

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
        if (!AiCue.aiOnlyDouble) {
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
        double room = 1 - AiCue.DEFENSIVE_ATTACK_PROB;
        double realProb = AiCue.DEFENSIVE_ATTACK_PROB + (1 - aps.attackPrivilege / 100) * room;
        double mul = Math.pow(aps.precision / 100.0, 1.5);  // 补偿由于AI打不准造成的进球概率低，进而不进攻的问题
        double res = realProb * mul;
        return Math.max(AiCue.DEFENSIVE_ATTACK_PROB / 5, res);
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
            boolean isSolving,
            double nativePrice,
            boolean allowPocketCorner,
            boolean considerTolerance
    ) {

        WhitePrediction wp = copy.predictWhite(cpp,
                phy,
                STD_LENGTH_AFTER_WALL,
                true,
                true,
                true,
                false,
                false,
                false);  // 这里不用clone，因为整个game都是clone的
        double[] whiteStopPos = wp.stopPoint();

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
            penalty *= 1 + ((cueParams.getCueAngleDeg() - Values.DEFAULT_CUE_ANGLE) / 30.0);
            double opponentAttackPrice = AttackChoice.DirectAttackChoice.priceOfDistance(seeAble.avgTargetDistance);
            double snookerPrice = 1.0;

//            if (wp.isFirstBallCollidesOther()) {
//                penalty *= 1.1;
//            }
            if (wp.getSecondCollide() != null) {
                penalty *= 10;
            }
            if (wp.isCueBallFirstBallTwiceColl()) {
                penalty *= 2;  // 二次碰撞
            }

            AttackChoice oppoEasiest = null;
            if (seeAble.seeAbleTargets == 0) {
                // 这个权重如果太大，AI会不计惩罚地去做斯诺克
                // 如果太小，AI会不做斯诺克
                snookerPrice = Math.sqrt(seeAble.maxShadowAngle) * 50.0;
                if (isSolving) snookerPrice /= 5;
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
                        opponentAttackPrice += 25.0 * potProb;  // 大卖
                    } else if (potProb > aiCue.opponentDefAtkProb) {
                        opponentAttackPrice += 10.0 * potProb;  // 小卖
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
                penalty /= AiCue.WHITE_HIT_CORNER_PENALTY;
            }
            if (snookerPrice > 1.01) {
                if (wp.isFirstBallCollidesOther()) {  // 在做斯诺克
                    penalty *= 8.0;
                }
            } else {
                Ball firstBallCol = wp.getFirstBallCollidesOther();
                if (firstBallCol != null) {
                    if (firstBallCol.getValue() != firstCollide.getValue()) {
                        // todo: 可能是检查目标球是否是同一种
                        penalty *= 2.0;
                    }
                }
            }
            wp.resetToInit();
//            System.out.printf("%f %f %f\n", snookerPrice, opponentAttackPrice, penalty);

            double tolerancePenalty = 1.0;
            // analyze tolerance
            if (considerTolerance) {
                WhitePrediction[] tolerances = toleranceAnalysis(
                        copy,
                        aiPlayer,
                        cpp,
                        phy,
                        STD_LENGTH_AFTER_WALL,
                        true,
                        true,
                        true,
                        false,
                        false
                );
                for (WhitePrediction devOne : tolerances) {
                    if (devOne == null) {
                        System.err.println("Deviation is null");
                        tolerancePenalty *= 10.0;
                        continue;
                    }
                    if (wp.getFirstCollide() != devOne.getFirstCollide()) {
                        tolerancePenalty *= 10.0;
                    }
                    if (wp.isFirstBallCollidesOther() != devOne.isFirstBallCollidesOther()) {
                        tolerancePenalty *= 1.2;
                    }
                    if (wp.getSecondCollide() != devOne.getSecondCollide()) {
                        tolerancePenalty *= 2.0;
                    }
                    double[] devStopPos = devOne.stopPoint();
                    double dt = Algebra.distanceToPoint(whiteStopPos, devStopPos);
                    double allowed = copy.getGameValues().table.maxLength * 0.1;
                    if (dt > allowed) {
                        tolerancePenalty *= (dt / allowed);
                    }
                }
            }

            return new FinalChoice.DefenseChoice(
                    firstCollide,
                    nativePrice,
                    snookerPrice,
                    opponentAttackPrice,
                    penalty,
                    tolerancePenalty,
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

    /**
     * @return {sideDevRad, aimingSd}
     */
    static double[] aiStandardDeviation(
            CueParams cueParams,
            Player aiPlayer,
            boolean isAttack
    ) {

        PlayerHand playerHand = cueParams.getHandSkill();
        PlayerPerson playerPerson = aiPlayer.getPlayerPerson();
        AiPlayStyle aps = playerPerson.getAiPlayStyle();
//            double handSdMul = PlayerPerson.HandBody.getSdOfHand(attackChoice.handSkill);
        double handSdMul = 1.0;

//            double[] muSigXy = playerPerson.getCuePointMuSigmaXY();
//            double sideSpinSd = muSigXy[1];  // 左右打点的标准差，mm
        double powerErrorFactor = playerHand.getErrorMultiplierOfPower(cueParams.selectedPower());
        double powerSd = (100.0 - playerHand.getPowerControl()) / 100.0;
        powerSd *= aiPlayer.getInGamePlayer()
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
        sideDevRad *= 1.25;  // 稍微多惩罚一点

        // 若球手不喜欢加塞，加大sideDevRad
        double likeSideMul = 110 / (aps.likeSide + 10);
        sideDevRad *= likeSideMul;

        // 瞄准的1倍标准差偏差角
        double aimingSd;
        if (isAttack) {
            aimingSd = (105 - aps.precision) * handSdMul /
                    AiCueResult.DEFAULT_AI_PRECISION;  // 这里用default是因为，我们不希望把AI精确度调低之后它就觉得打不进，一直防守
        } else {
            aimingSd = (105 - aps.defense) * handSdMul /
                    AiCueResult.DEFAULT_AI_PRECISION;
        }

        if (cueParams.getCueAngleDeg() > 5.0) {
            // 抬高杆尾导致瞄准困难
            aimingSd *= Algebra.shiftRangeSafe(5.0, 45.0, 1.0, 3.0,
                    cueParams.getCueAngleDeg());
        }

        return new double[]{sideDevRad, aimingSd};
    }

    static WhitePrediction[] toleranceAnalysis(
            Game<?, ?> game,
            Player aiPlayer,
            CuePlayParams origCpp,
            Phy phy,
            double lengthAfterWall,
            boolean predictPath,
            boolean checkCollisionAfterFirst,
            boolean predictTargetBall,
            boolean wipe,
            boolean useClone) {
        double[] devs = aiStandardDeviation(
                origCpp.cueParams,
                aiPlayer,
                false
        );

        double whiteBallDevRad = (devs[0] + devs[1]) * 1.0;  // 这个值越小，AI越愿意尝试走钢丝
        double[] leftDir = Algebra.rotateVector(origCpp.vx, origCpp.vy, -whiteBallDevRad);
        double[] rightDir = Algebra.rotateVector(origCpp.vx, origCpp.vy, whiteBallDevRad);

//        System.out.println("Center: " + Arrays.toString(new double[]{origCpp.vx, origCpp.vy}) +
//                " left: " + Arrays.toString(leftDir) + " right: " + Arrays.toString(rightDir));

        CuePlayParams leftCpp = origCpp.deviated(
                leftDir[0], leftDir[1]
        );
        CuePlayParams rightCpp = origCpp.deviated(
                rightDir[0], rightDir[1]
        );

        WhitePrediction left = game.predictWhite(leftCpp,
                phy,
                STD_LENGTH_AFTER_WALL,
                predictPath,
                checkCollisionAfterFirst,
                predictTargetBall,
                false,
                wipe,
                useClone);
        left.resetToInit();
        WhitePrediction right = game.predictWhite(rightCpp,
                phy,
                lengthAfterWall,
                predictPath,
                checkCollisionAfterFirst,
                predictTargetBall,
                false,
                wipe,
                useClone);
        right.resetToInit();

        return new WhitePrediction[]{
                left, right
        };
    }

    public static double[] estimateRealCueDirWithCurve(
            Game<?, ?> game,
            Phy phy,
            CueParams cueParams,
            double[] whiteToCollisionDir,
            boolean useClone) {

        double[] whitePos = game.getCueBall().getPositionArray();

        double preciseRad = Algebra.thetaOf(whiteToCollisionDir);
        double radHigh = Algebra.normalizeAngle(preciseRad + 0.1);
        double radLow = Algebra.normalizeAngle(preciseRad - 0.1);
        double tolerance = Math.atan2(1.0, game.getGameValues().table.maxLength);  // 一毫米误差
        int counter = 0;
        while (radHigh - radLow > tolerance) {
            double radMid = Algebra.normalizeAngle((radHigh + radLow) / 2);
            double[] unit = Algebra.unitVectorOfAngle(radMid);
            CuePlayParams cpp = CuePlayParams.makeIdealParams(
                    unit[0],
                    unit[1],
                    cueParams
            );
            WhitePrediction wp = game.predictWhite(cpp,
                    phy, 0.0,
                    false, false, false, true,
                    true, useClone);

            double[] currentDir;
            if (wp.getFirstCollide() != null) {
                currentDir = new double[]{
                        wp.getWhiteCollisionX() - whitePos[0],
                        wp.getWhiteCollisionY() - whitePos[1]
                };
            } else {
                // todo: 最好是让白球到一定距离就停止，不然万一画个巨大弧线，可能会反
                double[] whiteLastPos = wp.getWhitePath().getLast();
                currentDir = new double[]{
                        whiteLastPos[0] - whitePos[0],
                        whiteLastPos[1] - whitePos[1]
                };
            }
            double dirRad = Algebra.thetaOf(currentDir);
            int leftOrRight = Algebra.compareAngleDirection(dirRad, preciseRad);
            if (leftOrRight == 0) return currentDir;
            else if (leftOrRight < 0) {
                // 瞄得太左
                radLow = radMid;
            } else {
                // 瞄得太右
                radHigh = radMid;
            }
            counter++;
            if (counter > 16) {
                System.err.println("Max binary search step exceed.");
                return whiteToCollisionDir;
            }
        }

        return Algebra.unitVectorOfAngle(Algebra.normalizeAngle((radHigh + radLow) / 2));
    }

    /**
     * @param theoreticalCuePoints 纯的从-1到1的{高低杆，左右赛}
     * @return 考虑了避免呲杆打点的{高低杆，左右赛，角度deg}
     */
    public static double[] findCueAblePointAndAngle(
            Game<?, ?> game,
            Cue cue,
            double[] directionXY,
            double[][] theoreticalCuePoints
    ) {
        BallMetrics ballMetrics = game.getGameValues().ball;
        double cueTipBallRatio = cue.getCueTipWidth() / ballMetrics.ballDiameter;

        CueBackPredictor.Result backPre = game.getObstacleDtHeight(
                directionXY[0],
                directionXY[1],
                cue.getCueTipWidth()
        );

        double[] centerPoint = cue.aiCuePoint(theoreticalCuePoints[0], ballMetrics);

        for (double cueAngle = Values.DEFAULT_CUE_ANGLE; cueAngle <= 60; cueAngle += 5.0) {
            ObstacleProjection op = ObstacleProjection.createProjection(
                    backPre,
                    directionXY[0],
                    directionXY[1],
                    cueAngle,
                    game.getCueBall(),
                    game.getGameValues(),
                    cue.getCueTipWidth()
            );
            if (op == null) {
                return new double[]{centerPoint[0], centerPoint[1], cueAngle};
            }
            for (double[] cuePoint : theoreticalCuePoints) {
                double[] realPoint = cue.aiCuePoint(cuePoint, ballMetrics);
                if (op.cueAble(realPoint[1], -realPoint[0], cueTipBallRatio)) {
                    return new double[]{realPoint[0], realPoint[1], cueAngle};
                }
            }
        }
        System.err.println("Cannot find a cue able point and angle!");
        return new double[]{centerPoint[0], centerPoint[1], Values.DEFAULT_CUE_ANGLE};
    }
}
