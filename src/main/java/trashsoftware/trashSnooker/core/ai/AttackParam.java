package trashsoftware.trashSnooker.core.ai;

import org.apache.commons.math3.distribution.NormalDistribution;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.phy.TableCloth;
import trashsoftware.trashSnooker.util.EventLogger;

public class AttackParam {
    double potProb;  // 正常情况下能打进的概率
    double price;  // 对于球手来说的价值
    AttackChoice attackChoice;

    CueParams cueParams;

    AttackParam(AttackParam base, AttackChoice replacement) {
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

        double[] devs = Analyzer.aiStandardDeviation(
                cueParams,
                attackChoice.attackingPlayer,
                true
        );
        double sideDevRad = devs[0];
        double aimingSd = devs[1];

        double totalDt = attackChoice.targetHoleDistance + attackChoice.whiteCollisionDistance;
        double whiteInitSpeed = CuePlayParams.getSpeedOfPower(cueParams.actualPower(), 0);
        double totalMove = gameValues.estimatedMoveDistance(phy, whiteInitSpeed);

        // 预估台泥变线偏差
        double moveT = 0;
        try {
            moveT = gameValues.estimateMoveTime(phy, whiteInitSpeed, totalDt);
        } catch (IllegalArgumentException iae) {
            // do nothing
        }
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
            double targetDifficultyMm = targetAimingOffset * (105 - aps.doubleAbility) / 150;

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
