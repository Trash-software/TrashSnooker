package trashsoftware.trashSnooker.core.ai;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.attempt.CueType;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.metrics.Pocket;
import trashsoftware.trashSnooker.core.phy.Phy;

import java.util.Arrays;

public abstract class AttackChoice implements Comparable<AttackChoice> {

    protected Ball ball;
    protected Game<?, ?> game;
    protected Player attackingPlayer;
    protected boolean isPositioning;  // 是走位预测还是进攻

    protected double angleRad;  // 范围 [0, PI/2)

    protected double whiteCollisionDistance;
    protected double targetHoleDistance;  // 对于翻袋，这个是 目标球-库点-袋口 的距离，可能乘以某个惩罚系数

    protected double[] whitePos;
    protected double[] targetOrigPos;
    protected double[] cueDirectionUnitVector;
    protected double[] collisionPos;
    protected double[] holeOpenPos;  // 洞口瞄准点的坐标，非洞底
    protected PlayerHand handSkill;

    int attackTarget;
    double targetPrice;
    protected AttackParam defaultRef;

    protected abstract AttackChoice copyWithNewDirection(double[] newDirection);

    protected abstract CueType cueType();

    protected boolean isMidHole() {
        return holeOpenPos[0] == game.getGameValues().table.midX;
    }

    @Override
    public int compareTo(@NotNull AttackChoice o) {
        return -Double.compare(this.defaultRef.price, o.defaultRef.price);
    }

    public static class DoubleAttackChoice extends AttackChoice {

        double[] lastCushionToPocket;
        protected Pocket pocket;

        /**
         * @param lastAiPottedBall 如果这杆为走位预测，则该值为AI第一步想打的球。如这杆就是第一杆，则为null
         */
        public static DoubleAttackChoice createChoice(Game<?, ?> game,
                                                      Phy phy,
                                                      Player attackingPlayer,
                                                      double[] whitePos,
                                                      Game.DoublePotAiming aiming,
                                                      @Nullable Ball lastAiPottedBall,
                                                      int attackTarget,
                                                      boolean isPositioning) {
            double collisionPointX = aiming.collisionPos[0];
            double collisionPointY = aiming.collisionPos[1];
            double cueDirX = collisionPointX - whitePos[0];
            double cueDirY = collisionPointY - whitePos[1];
            double[] cueDirUnit = Algebra.unitVector(cueDirX, cueDirY);

            double[] holePos = game.getGameValues().getOpenCenter(aiming.pocket.hole);
            double[] firstCushionPoint = aiming.cushionPos.get(0);
            double[] lastCushionPoint = aiming.cushionPos.get(aiming.cushionPos.size() - 1);
            double[] lastCushionToHole = new double[]{
                    holePos[0] - lastCushionPoint[0],
                    holePos[1] - lastCushionPoint[1]
            };
            double[] lastCushionToHoleUnit = Algebra.unitVector(lastCushionToHole);

            double[] ballOrigPos = aiming.targetPos;

            PlayerHand handSkill = CuePlayParams.getPlayableHand(
                    whitePos[0], whitePos[1],
                    cueDirUnit[0], cueDirUnit[1],
                    Values.DEFAULT_CUE_ANGLE,  // todo
                    game.getGameValues().table,
                    attackingPlayer.getPlayerPerson()
            );

            double[] collisionPos = new double[]{collisionPointX, collisionPointY};
            double[] whiteToColl =
                    new double[]{collisionPointX - whitePos[0], collisionPointY - whitePos[1]};
            double[] ballToFirstCushion = new double[]{
                    firstCushionPoint[0] - aiming.targetPos[0],
                    firstCushionPoint[1] - aiming.targetPos[1]
            };

            // 已经检查过了
            double angle = Algebra.thetaBetweenVectors(whiteToColl, ballToFirstCushion);
//
//            if (angle >= Math.PI / 2) {
////                System.out.println("Impossible angle: " + Math.toDegrees(angle));
//                return null;  // 不可能打进的球
//            }
            double whiteDistance = Math.hypot(whiteToColl[0], whiteToColl[1]);
            if (whiteDistance < game.getGameValues().ball.ballDiameter) {
                return null;  // 白球和目标球挤在一起了
            }
            double targetHoleTotalDt = 0;
            double[] pos = ballOrigPos;
            for (double[] cu : aiming.cushionPos) {
                targetHoleTotalDt += Math.hypot(cu[0] - pos[0], cu[1] - pos[1]);
                pos = cu;
            }
            targetHoleTotalDt += Math.hypot(lastCushionToHole[0], lastCushionToHole[1]);

            DoubleAttackChoice ac = new DoubleAttackChoice();
            ac.game = game;
            ac.ball = aiming.target;
            ac.isPositioning = isPositioning;
            ac.lastCushionToPocket = lastCushionToHoleUnit;
//            ac.targetHoleVec = targetToHole;
            ac.holeOpenPos = holePos;
            ac.pocket = aiming.pocket;
            ac.angleRad = angle;
            ac.targetHoleDistance = targetHoleTotalDt;
            ac.whitePos = whitePos;
            ac.collisionPos = collisionPos;
            ac.whiteCollisionDistance = whiteDistance;
            ac.cueDirectionUnitVector = cueDirUnit;
//            ac.dirHole = dirHole;
            ac.targetOrigPos = ballOrigPos;
            ac.attackTarget = attackTarget;
            ac.attackingPlayer = attackingPlayer;
            ac.handSkill = handSkill;
//            attackChoice.calculateDifficulty();

            ac.targetPrice = game.priceOfTarget(attackTarget, aiming.target, attackingPlayer, lastAiPottedBall);

            // 随便创建一个，用于评估难度
            ac.defaultRef = new AttackParam(
                    ac,
                    game,
                    phy,
                    CueParams.createBySelected(
                            30,
                            0,
                            0,
                            5.0,
                            game, attackingPlayer.getInGamePlayer(),
                            handSkill
                    )
            );

            return ac;
        }

        @Override
        protected DoubleAttackChoice copyWithNewDirection(double[] newDirection) {
            DoubleAttackChoice copied = new DoubleAttackChoice();

            copied.game = game;
            copied.ball = ball;
            copied.isPositioning = isPositioning;
            copied.lastCushionToPocket = lastCushionToPocket;
            copied.holeOpenPos = holeOpenPos;
            copied.pocket = pocket;
            copied.angleRad = angleRad;
            copied.targetHoleDistance = targetHoleDistance;
            copied.whitePos = whitePos;
            copied.collisionPos = collisionPos;
            copied.whiteCollisionDistance = whiteCollisionDistance;
            copied.cueDirectionUnitVector = newDirection;
//            copied.dirHole = dirHole;
            copied.targetOrigPos = targetOrigPos;
            copied.attackTarget = attackTarget;
            copied.attackingPlayer = attackingPlayer;
//            copied.difficulty = difficulty;
//            copied.price = price;
            copied.targetPrice = targetPrice;
            copied.handSkill = handSkill;
            copied.defaultRef = defaultRef;

            return copied;
        }

        @Override
        protected CueType cueType() {
            return CueType.DOUBLE_POT;
        }
    }

    public static class DirectAttackChoice extends AttackChoice {
        //        protected double difficulty;

        protected double[][] dirHole;
        protected double[] targetHoleVec;

//        transient double difficulty;
//        transient double price;

        private DirectAttackChoice() {
        }

        /**
         * @param lastAiPottedBall 如果这杆为走位预测，则该值为AI第一步想打的球。如这杆就是第一杆，则为null
         */
        public static DirectAttackChoice createChoice(Game<?, ?> game,
                                                      Phy phy,
                                                      Player attackingPlayer,
                                                      double[] whitePos,
                                                      Ball ball,
                                                      @Nullable Ball lastAiPottedBall,
                                                      int attackTarget,
                                                      boolean isPositioning,
                                                      double[][] dirHole,
                                                      @Nullable double[] ballOrigPos) {
            double collisionPointX = dirHole[2][0];
            double collisionPointY = dirHole[2][1];
            double cueDirX = collisionPointX - whitePos[0];
            double cueDirY = collisionPointY - whitePos[1];
            double[] cueDirUnit = Algebra.unitVector(cueDirX, cueDirY);
            double[] targetToHole = dirHole[0];
            double[] holePos = dirHole[1];
            if (ballOrigPos == null) {
                ballOrigPos = new double[]{ball.getX(), ball.getY()};
            }

            PlayerHand handSkill = CuePlayParams.getPlayableHand(
                    whitePos[0], whitePos[1],
                    cueDirUnit[0], cueDirUnit[1],
                    Values.DEFAULT_CUE_ANGLE,  // todo
                    game.getGameValues().table,
                    attackingPlayer.getPlayerPerson()
            );

            double[] collisionPos = new double[]{collisionPointX, collisionPointY};
            double[] whiteToColl =
                    new double[]{collisionPointX - whitePos[0], collisionPointY - whitePos[1]};
            double angle = Algebra.thetaBetweenVectors(whiteToColl, targetToHole);

            if (angle >= Math.PI / 2) {
//                System.out.println("Impossible angle: " + Math.toDegrees(angle));
                return null;  // 不可能打进的球
            }
            double whiteDistance = Math.hypot(whiteToColl[0], whiteToColl[1]);
//            if (whiteDistance < game.getGameValues().ball.ballDiameter) {
//                return null;  // 白球和目标球挤在一起了
//            }
            double targetHoleDistance = Math.hypot(
                    ballOrigPos[0] - holePos[0],
                    ballOrigPos[1] - holePos[1]
            );

            DirectAttackChoice directAttackChoice = new DirectAttackChoice();
            directAttackChoice.game = game;
            directAttackChoice.ball = ball;
            directAttackChoice.isPositioning = isPositioning;
            directAttackChoice.targetHoleVec = targetToHole;
            directAttackChoice.holeOpenPos = holePos;
            directAttackChoice.angleRad = angle;
            directAttackChoice.targetHoleDistance = targetHoleDistance;
            directAttackChoice.whitePos = whitePos;
            directAttackChoice.collisionPos = collisionPos;
            directAttackChoice.whiteCollisionDistance = whiteDistance;
            directAttackChoice.cueDirectionUnitVector = cueDirUnit;
            directAttackChoice.dirHole = dirHole;
            directAttackChoice.targetOrigPos = ballOrigPos;
            directAttackChoice.attackTarget = attackTarget;
            directAttackChoice.attackingPlayer = attackingPlayer;
            directAttackChoice.handSkill = handSkill;
//            attackChoice.calculateDifficulty();

            directAttackChoice.targetPrice = game.priceOfTarget(attackTarget, ball, attackingPlayer, lastAiPottedBall);

            // 随便创建一个，用于评估难度
            directAttackChoice.defaultRef = new AttackParam(
                    directAttackChoice,
                    game,
                    phy,
                    CueParams.createBySelected(
                            30,
                            0,
                            0,
                            5.0,
                            game, attackingPlayer.getInGamePlayer(),
                            handSkill
                    )
            );

            return directAttackChoice;
        }

        static double priceOfDistance(double distance) {
            double difficulty = distance * 2;
            return 10000.0 / difficulty;  // Refer to the class's constructor
        }

        /**
         * @param remDtOfBall 球到达袋口时的速度还能让它跑多远，大概
         */
        protected static double holeProjWidth(GameValues values,
                                              boolean isMidHole,
                                              double[] targetHoleVec,
                                              double remDtOfBall) {
            double effectiveSlopeMul = Math.max(0, 1 - remDtOfBall / 48000);
            // 我们认为到达时35以上的力就不能往里滚了。根据计算，35的力能打差不多48000远。
            // 这里有一个问题，就是其实应该用speed，因为慢的桌子distance会很小
            if (isMidHole) {
                // 大力灌袋时，必须很准才能进。小力轻推时比较容易因为重力跑进去
                // 最大力时必须整颗球都在袋内侧
                // 最小力时只要球心在袋+重力范围内就可以了
                // mul范围在0-1之间
                // 这个应该和逃逸速度/俘获类似
                // 质量不变，速度与半径应为二次方关系
                // 但我们这里的参数是剩余运行距离，和速度也是二次方关系
                // 怼了，成线性的了
//                System.out.println(remDtOfBall + " " + effectiveSlopeMul);

                return Math.abs(targetHoleVec[1]) * values.table.midHoleDiameter
                        + (values.table.midPocketGravityRadius + values.ball.ballRadius) * effectiveSlopeMul;
            } else {
                // 底袋也有一点点重力影响，但比中袋小
                double holeMax = values.table.cornerHoleDiameter + values.ball.ballRadius / 2
                        + values.table.cornerPocketGravityRadius * effectiveSlopeMul;
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
                                                       double[] targetHoleVec,
                                                       double remDtOfBall) {
            double holeWidth = DirectAttackChoice.holeProjWidth(values,
                    isMidHole,
                    targetHoleVec,
                    remDtOfBall);
            // 从这个角度看袋允许的偏差
            return holeWidth - values.ball.ballDiameter * 0.9;  // 0.9是随手写的
        }

        protected static double holeDifficulty(Game<?, ?> game,
                                               boolean isMidHole,
                                               double[] targetHoleVec) {

            double allowedDev = allowedDeviationOfHole(
                    game.getGameValues(),
                    isMidHole,
                    targetHoleVec,
                    10);  // 接近最容易进的，毕竟是评估球形难度，只能轻推中袋的球也还算行
            if (isMidHole) {
                return game.getGameValues().midHoleBestAngleWidth / allowedDev;
            } else {
                return game.getGameValues().cornerHoldBestAngleWidth / allowedDev;
            }
        }

        private static double toleranceToOneDir(Game<?, ?> game,
                                                Ball cueBall,
                                                Ball targetBall,
                                                double eachTickMm,
                                                double ticks,
                                                double[] tanLineUnit,
                                                double[] whitePos,
                                                double[] collisionPos,
                                                double[] potDirection) {
            for (int i = 1; i < ticks; i++) {
                double dt = i * eachTickMm;
                double x = whitePos[0] + tanLineUnit[0] * dt;
                double y = whitePos[1] + tanLineUnit[1] * dt;
                double[] thisWhiteDir = new double[]{collisionPos[0] - x, collisionPos[1] - y};

//                System.out.println("#" + i + " pos: " + x + ", " + y);

                double angle = Algebra.thetaBetweenVectors(
                        thisWhiteDir,
                        potDirection
                );
                if (Math.abs(angle) >= Algebra.HALF_PI) {
                    // 这个大于90度了，返回上一个位置
                    return (i - 1) * eachTickMm;
                }
                if (!game.pointToPointCanPassBall(
                        x,
                        y,
                        collisionPos[0],
                        collisionPos[1],
                        cueBall,
                        targetBall,
                        true,
                        true
                )) {
                    return (i - 1) * eachTickMm;
                }
            }
            return ticks * eachTickMm;
        }

        /**
         * 返回这个attackChoice白球允许的最大偏差, mm。
         * 以 白球实际位置与目标球进球点连线 的 垂线 为基准，在左为负，右为正。
         */
        public static double[] leftRightTolerance(Game<?, ?> game,
                                                  Ball cueBall,
                                                  Ball targetBall,
                                                  double[] whitePos,
                                                  double[] collisionPos,
                                                  double[] targetPos) {
            double[] orth = Algebra.unitVector(Algebra.normalVector(collisionPos[0] - whitePos[0],
                    collisionPos[1] - whitePos[1]));
            double[] potDirection = new double[]{targetPos[0] - collisionPos[0], targetPos[1] - collisionPos[1]};
            double tick = game.getGameValues().ball.ballRadius / 2;

//            System.out.println("White pos " + Arrays.toString(whitePos) + ", tan line " + Arrays.toString(orth));
//            System.out.println("left");
            double leftTor = toleranceToOneDir(
                    game,
                    cueBall,
                    targetBall,
                    tick,
                    40,
                    orth,
                    whitePos,
                    collisionPos,
                    potDirection
            );
//            System.out.println("right");
            double rightTor = toleranceToOneDir(
                    game,
                    cueBall,
                    targetBall,
                    -tick,
                    40,
                    orth,
                    whitePos,
                    collisionPos,
                    potDirection
            );
            ;

            return new double[]{Math.abs(leftTor), Math.abs(rightTor)};
        }

        @NotNull
        public AttackParam getDefaultRef() {
            return defaultRef;
        }

        public Ball getBall() {
            return ball;
        }

        public double[] leftRightTolerance() {
            return leftRightTolerance(game, game.getCueBall(), ball, whitePos, collisionPos, targetOrigPos);
        }

        protected DirectAttackChoice copyWithNewDirection(double[] newDirection) {
            DirectAttackChoice copied = new DirectAttackChoice();

            copied.game = game;
            copied.ball = ball;
            copied.isPositioning = isPositioning;
            copied.targetHoleVec = targetHoleVec;
            copied.holeOpenPos = holeOpenPos;
            copied.angleRad = angleRad;
            copied.targetHoleDistance = targetHoleDistance;
            copied.whitePos = whitePos;
            copied.collisionPos = collisionPos;
            copied.whiteCollisionDistance = whiteCollisionDistance;
            copied.cueDirectionUnitVector = newDirection;
            copied.dirHole = dirHole;
            copied.targetOrigPos = targetOrigPos;
            copied.attackTarget = attackTarget;
            copied.attackingPlayer = attackingPlayer;
//            copied.difficulty = difficulty;
//            copied.price = price;
            copied.targetPrice = targetPrice;
            copied.handSkill = handSkill;
            copied.defaultRef = defaultRef;

            return copied;
        }

        @Override
        protected CueType cueType() {
            return CueType.ATTACK;
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
                    ", holePos=" + Arrays.toString(holeOpenPos) +
                    ", cueDirectionUnitVector=" + Arrays.toString(cueDirectionUnitVector) +
                    '}';
        }
    }
}
