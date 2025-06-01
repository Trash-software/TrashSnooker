package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;
import trashsoftware.trashSnooker.core.person.CuePlayerHand;

import java.util.*;

public abstract class TargetOrientedCueBallPlacer<G extends Game<?, ?>, P extends Player>
        extends AiCueBallPlacer<G, P> {
    
    public static final int NUM_ANGLES = 7;
    protected Ball specifiedBall;

    public TargetOrientedCueBallPlacer(G game, P player) {
        super(game, player);
    }

    @Override
    public Ball getBallSpecified() {
        return specifiedBall;
    }

    @Override
    public double[] getPositionToPlaceCueBall() {
        if (game.isBreaking()) {
            double[] breakPos = breakPosition();
            if (breakPos != null) return breakPos;
        }
        ALLOWED_POS allowedPos = currentAllowedPos();
        List<Ball> targetList = targetsSortedByPrivilege();

        int targetRep = game.getCurrentTarget();
        Ball cueBall = game.getCueBall();  // 肯定是pot了

        double maxPower = player.getPlayerPerson().getPrimaryHand().getControllablePowerPercentage();

        double attackProbThreshold = Analyzer.attackProbThreshold(AiCue.PURE_ATTACK_PROB,
                player.getPlayerPerson().getAiPlayStyle());

        Game[] pool = new Game[]{game};

        for (Ball target : targetList) {
            int nextTarget = game.getTargetAfterPotSuccess(target, false);
            List<Ball> nextStepLegalBalls = game.getAllLegalBalls(nextTarget,
                    false,
                    false
            );
            GamePlayStage stage = game.getGamePlayStage(target, false);

            SortedMap<FinalChoice.IntegratedAttackChoice, double[]> choicePlace = new TreeMap<>();

            double[] targetPos = new double[]{target.getX(), target.getY()};
            List<double[][]> dirHolePoints = game.directionsToAccessibleHoles(target);
            for (double[][] dirHole : dirHolePoints) {
//                System.out.println(target.getValue() + " " + game.getGameValues().getHoleByOpenCenter(dirHole[1]));
                List<double[]> availPlaces = switch (allowedPos) {
                    case FULL_TABLE -> availPosesFullTable(target, dirHole[0], dirHole[2]);
                    case BEFORE_LINE -> availPosesBeforeLine(target, dirHole[0], dirHole[2]);
                    case TWO_POINTS -> availPosesTwoPoints();
                };

                for (double[] pos : availPlaces) {
                    AttackChoice ac = AttackChoice.DirectAttackChoice.createChoice(
                            game,
                            game.getEntireGame().predictPhy,
                            player,
                            pos,
                            target,
                            null,
                            targetRep,
                            false,
                            dirHole,
                            targetPos
                    );
                    if (ac != null) {
//                        System.out.println("coll point: " + Arrays.toString(dirHole[2]) + ", pos: " + Arrays.toString(pos));
                        for (double selPower = 10; selPower <= maxPower; selPower += 5) {
                            CueParams cueParams = CueParams.createBySelected(
                                    selPower,
                                    0,
                                    0,
                                    5.0,
                                    game,
                                    player.getInGamePlayer(),
                                    CuePlayerHand.makeDefault(player.getInGamePlayer())
                            );
                            AttackParam attackParam = new AttackParam(
                                    ac,
                                    game,
                                    game.getEntireGame().predictPhy,
                                    cueParams
                            );
                            AiCue.AttackThread attackThread = new AiCue.AttackThread(
                                    attackParam,
                                    game.getGameValues(),
                                    nextTarget,
                                    nextStepLegalBalls,
                                    game.getEntireGame().predictPhy,
                                    pool,
                                    player,
                                    stage,
                                    null
                            );
                            game.forcePlaceWhiteNoRecord(pos[0], pos[1]);
                            attackThread.run();
                            cueBall.pot();
                            if (attackThread.result != null) {
                                choicePlace.put(attackThread.result, pos);
                            }
                        }
                    }
                }
            }
            if (!choicePlace.isEmpty()) {
                FinalChoice.IntegratedAttackChoice best = choicePlace.firstKey();
                if (best.attackParams.potProb > attackProbThreshold) {
                    specifiedBall = best.attackParams.attackChoice.ball;
                    return choicePlace.get(best);
                }
            }
        }
        
//        return null;
        return super.getPositionToPlaceCueBall();
    }

    private List<double[]> availPosesFullTable(Ball target,
                                               double[] targetDir,
                                               double[] collisionPoint) {
        List<double[]> results = new ArrayList<>();
        
        double targetOutDeg = Math.toDegrees(Algebra.thetaOf(targetDir));
        
        for (double deg = 0; deg < 90; deg += 5) {
            for (int sign : new int[]{-1, 1}) {
                if (sign == -1 && deg == 0) continue;  // -0哈，日你妈
                
                double[] posAtAng = findFarthestPosAtAng(target,
                        collisionPoint,
                        Algebra.normalizeAngleDeg(targetOutDeg + deg * sign),
                        500.0
                        );
                if (posAtAng != null) {
                    results.add(posAtAng);
                }
            }
            if (results.size() >= NUM_ANGLES) break;
        }
        return results;
    }

    private List<double[]> availPosesBeforeLine(Ball target,
                                                double[] targetDir,
                                                double[] collisionPoint) {
        List<double[]> results = new ArrayList<>();

        GameValues values = game.getGameValues();
        
        double[] xs = new double[]{
                game.getTable().breakLineX(),
                game.getTable().breakLineX() - values.ball.ballDiameter
        };
        double sep = 16;
        double yTick = (values.table.innerHeight - values.ball.ballDiameter) / sep;
        for (double x : xs) {
            for (double y = values.table.topY + values.ball.ballRadius; y < values.table.botY; y += yTick) {
                if (game.canPlaceWhite(x, y)) {
                    results.add(new double[]{x, y});
                }
            }
        }
        
        // 这里有nlogn次比较，比较内部其实可以优化，但懒得
        results.sort((a, b) -> {
            double[] aToColl = new double[]{collisionPoint[0] - a[0], collisionPoint[1] - a[1]};
            double[] bToColl = new double[]{collisionPoint[0] - b[0], collisionPoint[1] - b[1]};
            
            double aDt = Math.hypot(aToColl[0], aToColl[1]);
            double bDt = Math.hypot(bToColl[0], bToColl[1]);
            if (aDt < values.ball.ballDiameter * 0.33) return 1;
            if (bDt < values.ball.ballDiameter * 0.33) return -1;
            
            double aTheta = Algebra.thetaBetweenVectors(aToColl, targetDir);
            double bTheta = Algebra.thetaBetweenVectors(bToColl, targetDir);
            
            return Double.compare(aTheta, bTheta);
        });
        if (results.size() > NUM_ANGLES) {
            results = new ArrayList<>(results.subList(0, NUM_ANGLES));
        }
        return results;
    }

    private List<double[]> availPosesTwoPoints() {
        // 只有李式台球
        List<double[]> results = new ArrayList<>();
        TableMetrics metrics = game.getGameValues().table;

        double y = metrics.midY;
        for (double x : new double[]{(metrics.leftX + metrics.midX) / 2, (metrics.midX + metrics.rightX) / 2}) {
            while (game.isOccupied(x, y)) {
                x += 5.0;
            }
            if (!game.isInTable(x, y)) continue;
            results.add(new double[]{x, y});
        }
        return results;
    }

    private double[] findFarthestPosAtAng(Ball target,
                                          double[] collisionPoint,
                                          double absDeg,  // 是从假想白球看向目标进球点的方向
                                          double preferredDt) {
        double[] unitVec = Algebra.unitVectorOfAngle(Math.toRadians(absDeg));
        
        double dt = preferredDt;
        
        double[] pos = new double[2];
        do {
            if (dt < game.getGameValues().ball.ballDiameter * 0.33) return null;  // 摆不了
            pos[0] = collisionPoint[0] - unitVec[0] * dt;
            pos[1] = collisionPoint[1] - unitVec[1] * dt;
            dt *= 0.75;
        } while (!game.canPlaceWhite(pos[0], pos[1]) ||
                !game.pointToPointCanPassBall(
                pos[0],
                pos[1],
                collisionPoint[0],
                collisionPoint[1],
                null,
                target,
                true,
                true
        ));
//        System.out.println(Arrays.toString(pos) + " " + game.isInTable(pos[0], pos[1]));
        return pos;
    }

    protected abstract ALLOWED_POS currentAllowedPos();

    /**
     * @return 所有可以击打的目标，以优先级排序，最优先的目标在最前
     */
    protected abstract List<Ball> targetsSortedByPrivilege();

    public enum ALLOWED_POS {
        FULL_TABLE,
        BEFORE_LINE,
        TWO_POINTS
    }
}
