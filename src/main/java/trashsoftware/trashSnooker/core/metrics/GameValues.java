package trashsoftware.trashSnooker.core.metrics;

import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.SubRule;
import trashsoftware.trashSnooker.core.Values;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.phy.TableCloth;
import trashsoftware.trashSnooker.core.training.Challenge;
import trashsoftware.trashSnooker.core.training.TrainType;
import trashsoftware.trashSnooker.util.DataLoader;

import java.util.Arrays;
import java.util.Collection;

public class GameValues {

    public final GameRule rule;
    public Collection<SubRule> subRules;
    public final TableMetrics table;
    public final BallMetrics ball;
    private TablePreset tablePreset;  // 不管太多，只管画
    private BallsGroupPreset ballsGroupPreset;
    // 袋的正中央，最易进球的位置
    public double[] topLeftHoleOpenCenter;
    public double[] botLeftHoleOpenCenter;
    public double[] topRightHoleOpenCenter;
    public double[] botRightHoleOpenCenter;
    public double[] topMidHoleOpenCenter;
    public double[] botMidHoleOpenCenter;
    public double[][] allHoleOpenCenters;
    public double cornerHoleAngleRatio;  // 打底袋最差的角度和最好的角度差多少
    public double midHoleBestAngleWidth;  // 中袋对正的容错空间
    public double cornerHoldBestAngleWidth;  // 底袋对正的容错空间
    double ballHoleRatio;

    private double maxPowerMoveDistance;
    private TrainType trainType;
    private Challenge trainChallenge;
    private boolean devMode = false;

    public GameValues(GameRule rule,
                      Collection<SubRule> subRules,
                      TableMetrics tableMetrics,
                      BallMetrics ballMetrics) {
        this.rule = rule;
        this.subRules = subRules;
        this.table = tableMetrics;
        this.ball = ballMetrics;

        build();
    }

    public static GameValues fromJson(JSONObject jsonObject) {
        GameRule rule = GameRule.valueOf(jsonObject.getString("gameRule"));
        BallMetrics ballMetrics = BallMetrics.valueOf(jsonObject.getString("ball"));

        TableMetrics tableMetrics;
        TablePreset tablePreset = null;
        if (jsonObject.has("tablePreset")) {
            tablePreset = DataLoader.getInstance().getTablePresetById(jsonObject.getString("tablePreset"));
            tableMetrics = tablePreset.tableSpec.tableMetrics;
        } else {
            JSONObject tableObj = jsonObject.getJSONObject("table");
            TableMetrics.TableBuilderFactory factory =
                    TableMetrics.fromOrdinal(tableObj.getInt("tableOrdinal"));
            PocketSize pocketSize = factory.supportedHoles[tableObj.getInt("holeSizeOrdinal")];

            int diffOrd = factory.supportedDifficulties.length / 2;
            if (tableObj.has("pocketDifficultyOrdinal")) {
                diffOrd = tableObj.getInt("pocketDifficultyOrdinal");
            }

            PocketDifficulty pocketDifficulty = factory.supportedDifficulties[diffOrd];
            tableMetrics = factory
                    .create()
                    .pocketDifficulty(pocketDifficulty)
                    .holeSize(pocketSize)
                    .build();
        }

        JSONArray subRulesArray = jsonObject.getJSONArray("subRules");
        GameValues gameValues = new GameValues(rule, 
                SubRule.jsonToSubRules(subRulesArray), 
                tableMetrics, 
                ballMetrics);
        gameValues.setTablePreset(tablePreset);

        boolean devMode = jsonObject.has("devMode") && jsonObject.getBoolean("devMode");
        gameValues.setDevMode(devMode);

        return gameValues;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();

        // 似乎不需要考虑training/challenge的问题：谁存这个啊

        jsonObject.put("gameRule", rule.name());
        jsonObject.put("subRules", SubRule.subRulesToJson(subRules));
        jsonObject.put("ball", ball.name());
        jsonObject.put("devMode", devMode);

        if (tablePreset == null) {
            JSONObject tableObj = new JSONObject();
            tableObj.put("tableOrdinal", table.getOrdinal());
            tableObj.put("holeSizeOrdinal", table.getHoleSizeOrdinal());
            tableObj.put("pocketDifficultyOrdinal", table.getPocketDifficultyOrdinal());
            jsonObject.put("table", tableObj);
        } else {
            jsonObject.put("tablePreset", tablePreset.id);
        }
        
        if (ballsGroupPreset != null) {
            jsonObject.put("ballsGroupPreset", ballsGroupPreset.id);
        }

        return jsonObject;
    }

    private void build() {
        ballHoleRatio = ball.ballDiameter / table.cornerHoleDiameter;
        double bestSpace = table.cornerHoleDiameter - ball.ballRadius;
        double worstSpace = table.cornerHoleDiameter * Math.sqrt(2) / 2 - ball.ballRadius;
        cornerHoleAngleRatio = worstSpace / bestSpace;
        midHoleBestAngleWidth = table.midHoleDiameter - ball.ballRadius;
        cornerHoldBestAngleWidth = table.cornerHoleDiameter - ball.ballRadius;

        topLeftHoleOpenCenter = new double[]{table.leftX + ball.ballRadius, table.topY + ball.ballRadius};
        botLeftHoleOpenCenter = new double[]{table.leftX + ball.ballRadius, table.botY - ball.ballRadius};
        topRightHoleOpenCenter = new double[]{table.rightX - ball.ballRadius, table.topY + ball.ballRadius};
        botRightHoleOpenCenter = new double[]{table.rightX - ball.ballRadius, table.botY - ball.ballRadius};
        topMidHoleOpenCenter = new double[]{table.midX, table.topY - ball.ballRadius * 0.25};  // 特殊：中心点其实在台外
        botMidHoleOpenCenter = new double[]{table.midX, table.botY + ball.ballRadius * 0.25};

        allHoleOpenCenters = new double[][]{
                topLeftHoleOpenCenter,
                topMidHoleOpenCenter,
                topRightHoleOpenCenter,
                botRightHoleOpenCenter,
                botMidHoleOpenCenter,
                botLeftHoleOpenCenter
        };
        // open center 和 hole顺序必须一致
    }

    public void setTablePreset(TablePreset tablePreset) {
        this.tablePreset = tablePreset;
    }

    public void setBallsGroupPreset(BallsGroupPreset ballsGroupPreset) {
        this.ballsGroupPreset = ballsGroupPreset;
    }

    public void setTrain(TrainType trainType, Challenge challenge) {
        this.trainType = trainType;
        this.trainChallenge = challenge;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public boolean isDevMode() {
        return devMode;
    }

    public boolean isTraining() {
        return trainType != null;
    }

    public Challenge getTrainChallenge() {
        return trainChallenge;
    }

    public TrainType getTrainType() {
        return trainType;
    }

    public boolean isStandard() {
        return !isTraining() &&
                !devMode &&
                (
                        (rule == GameRule.SNOOKER && TableMetrics.SNOOKER.equals(table.tableName) && ball == BallMetrics.SNOOKER_BALL) ||
                                (rule == GameRule.MINI_SNOOKER && table.tableName.equals(TableMetrics.CHINESE_EIGHT) && ball == BallMetrics.SNOOKER_BALL) ||
                                (rule == GameRule.SNOOKER_TEN && TableMetrics.CHINESE_EIGHT.equals(table.tableName) && ball == BallMetrics.SNOOKER_BALL) ||
                                (rule == GameRule.CHINESE_EIGHT && table.tableName.equals(TableMetrics.CHINESE_EIGHT) && ball == BallMetrics.POOL_BALL) ||
                                (rule == GameRule.LIS_EIGHT && table.tableName.equals(TableMetrics.CHINESE_EIGHT) && ball == BallMetrics.POOL_BALL) ||
                                (rule == GameRule.AMERICAN_NINE && table.tableName.equals(TableMetrics.AMERICAN_NINE) && ball == BallMetrics.POOL_BALL));
    }

    public void setSubRules(Collection<SubRule> subRules) {
        this.subRules = subRules;
    }

    public Collection<SubRule> getSubRules() {
        return subRules;
    }

    public boolean hasSubRule(SubRule subRule) {
        return subRules.contains(subRule);
    }
    
    public boolean hasSubRuleDetail(SubRule.Detail detail) {
        for (SubRule subRule : subRules) {
            if (subRule.hasDetail(detail)) return true;
        }
        return false;
    }
    
    public int nBalls() {
        return switch (rule) {
            case SNOOKER -> {
                if (hasSubRule(SubRule.SNOOKER_GOLDEN)) yield 23;
                else yield 22;
            } 
            case SNOOKER_TEN -> 17;
            case MINI_SNOOKER -> 13;
            case CHINESE_EIGHT, LIS_EIGHT -> 16;
            case AMERICAN_NINE -> 10;
        };
    }

    public boolean isInTable(double x, double y, double r) {
        if (x >= table.leftX + r &&
                x <= table.rightX - r &&
                y >= table.topY + r &&
                y <= table.botY - r) {
            return true;
        }
        if (x < table.leftX - table.cushionClothWidth ||
                x > table.rightX + table.cushionClothWidth ||
                y < table.topY - table.cushionClothWidth ||
                y > table.botY + table.cushionClothWidth) {
            return false;
        }

        double[] pos = new double[]{x, y};

        if (y < r + table.topY) {
            if (x < table.midHoleAreaRightX && x >= table.midHoleAreaLeftX) {
                // 上方中袋在袋角范围内
                if (Algebra.distanceToPoint(pos, table.topMidHoleLeftArcXy.getCenter()) < table.midArcRadius + r) {
                    // 击中上方中袋左侧
                    return false;
                } else if (Algebra.distanceToPoint(pos, table.topMidHoleRightArcXy.getCenter()) < table.midArcRadius + r) {
                    // 击中上方中袋右侧
                    return false;
                } else if (x >= table.midHoleLineLeftX && x < table.midHoleLineRightX) {
                    // 疑似上方中袋直线
                    Cushion.CushionLine line = table.topMidHoleLeftLine;
                    if (Algebra.distanceToLine(pos, line.getPosition()) < r) {
                        return false;
                    }
                    line = table.topMidHoleRightLine;
                    if (Algebra.distanceToLine(pos, line.getPosition()) < r) {
                        return false;
                    }
                    return true;
                } else {
                    return true;
                }
            }
        } else if (y >= table.botY - r) {
            if (x < table.midHoleAreaRightX && x >= table.midHoleAreaLeftX) {
                // 下方中袋袋角范围内
                if (Algebra.distanceToPoint(pos, table.botMidHoleLeftArcXy.getCenter()) < table.midArcRadius + r) {
                    // 击中下方中袋左侧
                    return false;
                } else if (Algebra.distanceToPoint(pos, table.botMidHoleRightArcXy.getCenter()) < table.midArcRadius + r) {
                    // 击中下方中袋右侧
                    return false;
                } else if (x >= table.midHoleLineLeftX && x < table.midHoleLineRightX) {
                    // 疑似下方中袋直线
                    Cushion.CushionLine line = table.botMidHoleLeftLine;
                    if (Algebra.distanceToLine(pos, line.getPosition()) < r) {
                        return false;
                    }
                    line = table.botMidHoleRightLine;
                    if (Algebra.distanceToLine(pos, line.getPosition()) < r) {
                        return false;
                    }
                    return true;
                } else {
                    return true;
                }
            }
        }

        double[] probHole = null;
        if (y < table.topCornerHoleAreaDownY) {
            if (x < table.leftCornerHoleAreaRightX) probHole = table.topLeft.fallCenter;  // 左上底袋
            else if (x >= table.rightCornerHoleAreaLeftX)
                probHole = table.topRight.fallCenter;  // 右上底袋
        } else if (y >= table.botCornerHoleAreaUpY) {
            if (x < table.leftCornerHoleAreaRightX) probHole = table.botLeft.fallCenter;  // 左下底袋
            else if (x >= table.rightCornerHoleAreaLeftX)
                probHole = table.botRight.fallCenter;  // 右下底袋
        }

        if (probHole != null) {
            for (int i = 0; i < table.allCornerLines.length; ++i) {
                Cushion.CushionLine line = table.allCornerLines[i];

                if (Algebra.distanceToLine(pos, line.getPosition()) < r) {
                    return false;
                }
            }
//            if (!table.isStraightHole()) {
            for (Cushion.CushionArc cornerArc : table.allCornerArcs) {
                if (Algebra.distanceToPoint(pos, cornerArc.getCenter()) < table.cornerArcRadius + r) {
                    return false;
                }
            }
//            }

            return true;
        }
        return false;
    }

//    public double speedReducerPerInterval(Phy phy) {
//        // todo
//        double slippingFriction = ball.frictionRatio * phy.slippingFrictionTimed;
//        return slippingFriction;
////        return (phy.speedReducer * table.speedReduceMultiplier / ball.ballWeightRatio);  // 重的球减速慢
//    }

    /**
     * @param speed 初始速度，mm/s
     * @return 预估的直线移动距离，mm。
     */
    public double estimatedMoveDistance(Phy phy, double speed) {
        double slippingFriction = ball.frictionRatio * table.slipResistanceRatio * phy.slippingFrictionTimed;
        double rollingFriction = ball.frictionRatio * table.rollResistanceRatio * phy.rollingFrictionTimed;

        double efficiency = TableCloth.SLIP_ACCELERATE_EFFICIENCY;
        double initSpeed = speed / phy.calculationsPerSec;
        // Step 1: Slipping Phase
        double slipEndTime = initSpeed / (slippingFriction * (1 + efficiency));
        double slipDistance = initSpeed * slipEndTime
                - 0.5 * (slippingFriction * efficiency) * slipEndTime * slipEndTime;

        double speedAtRollStart = initSpeed - (slippingFriction * efficiency) * slipEndTime;

        // Step 2: Rolling Phase
        double rollDistance = (speedAtRollStart * speedAtRollStart) / (2.0 * rollingFriction);

//        System.out.println("Slip: " + slipDistance + ", roll: " + rollDistance);

        // Total Distance
        return slipDistance + rollDistance;
        
//        double acceleration = speedReducerPerInterval(phy) * phy.calculationsPerSecSqr;
//        double t = speed / acceleration;  // 加速时间，秒
//        return acceleration / 2 * t * t;  // S = 1/2at^2
    }

    /**
     * @return 预估：以给定的初速度，跑一定距离需要多久。时间单位是秒
     */
    public double estimateMoveTime(Phy phy, double initSpeed, double distance) {
        double slippingFriction = ball.frictionRatio * table.slipResistanceRatio * phy.slippingFrictionTimed;
        double rollingFriction = ball.frictionRatio * table.rollResistanceRatio * phy.rollingFrictionTimed;
        double efficiency = TableCloth.SLIP_ACCELERATE_EFFICIENCY;

        double speed = initSpeed / phy.calculationsPerSec;

        // Step 1: Slipping Phase
        double slipEndTime = speed / (slippingFriction * (1 + efficiency));
        double slipDistance = speed * slipEndTime
                - 0.5 * (slippingFriction * efficiency) * slipEndTime * slipEndTime;

        double speedAtRollStart = speed - (slippingFriction * efficiency) * slipEndTime;

        double rollDistance = (speedAtRollStart * speedAtRollStart) / (2.0 * rollingFriction);

        double totalDistance = slipDistance + rollDistance;

        if (distance > totalDistance) {
//            throw new IllegalArgumentException("Ball cannot travel the given distance before stopping.");
            distance = totalDistance;
        }

        // Now find time:
        if (distance <= slipDistance) {
            // Still inside slipping phase
            // Solve quadratic: distance = speed * t - 0.5 * (slippingFriction * efficiency) * t^2
            double a = -0.5 * slippingFriction * efficiency;
            double b = speed;
            double c = -distance;

            double discriminant = b * b - 4 * a * c;
            if (discriminant < 0) {
                throw new IllegalArgumentException("No real solution for time in slipping phase.");
            }

            double tSlipPartial = (-b + Math.sqrt(discriminant)) / (2 * a);
            // usually positive root (-b + sqrt(discriminant)) / (2a), because a < 0

            return tSlipPartial / phy.calculationsPerSec;
        } else {
            // Distance spans both slipping and rolling phases
            double remainingDistance = distance - slipDistance;

            // In rolling phase:
            // Using: distance = v0 * t - 0.5 * rollingFriction * t^2
            double a = -0.5 * rollingFriction;
            double b = speedAtRollStart;
            double c = -remainingDistance;

            double discriminant = b * b - 4 * a * c;
            if (discriminant < 0) {
                throw new IllegalArgumentException("No real solution for time in rolling phase.");
            }

            double tRolling = (-b + Math.sqrt(discriminant)) / (2 * a);

            return (slipEndTime + tRolling) / phy.calculationsPerSec;
        }
        
        
//        double acceleration = speedReducerPerInterval(phy) * phy.calculationsPerSecSqr;
//        double fullT = initSpeed / acceleration;
//        fullT *= 0.85;  // 因为种种原因，这个算出来总是偏大，于是强行减小
//        double fullDt = acceleration / 2 * fullT * fullT;
//        double ratio = fullDt / distance;
//        double timeRatio = ratio * ratio;
//        double t0 = fullT / timeRatio + distance / initSpeed;
////        System.out.println("Full stop t: " + fullT + ", pocket t: " + t0 + ", full dt: " + fullDt + ", speed: " + initSpeed);
//        return t0;  // 我不确定这对不对
    }

//    public double estimateMaxPowerMoveDistance(Phy phy) {
//        if (maxPowerMoveDistance == 0) {
//            maxPowerMoveDistance = estimatedMoveDistance(phy, Values.MAX_POWER_SPEED);
//        }
//        return maxPowerMoveDistance;
//    }

    public double estimateSpeedNeeded(Phy phy, double distance) {
        double low = 0;
        double high = Values.MAX_POWER_SPEED * 1.2;  // 虽然可能有更快的球速，但这里真不需要再快了，不会用到的
        double dtTolerance = ball.ballRadius;

        while (high - low > 1e-6) {
            double midSpeed = (low + high) / 2.0;
            double estimatedDistance = estimatedMoveDistance(phy, midSpeed);

            if (estimatedDistance > distance + dtTolerance) {
                high = midSpeed;
            } else if (estimatedDistance < distance - dtTolerance) {
                low = midSpeed;
            } else {
                return midSpeed;
            }
        }

        return (low + high) / 2.0;
        
//        double acceleration = speedReducerPerInterval(phy) * phy.calculationsPerSecSqr;
//        double t2 = distance * 2 / acceleration;
//        return Math.sqrt(t2);
    }

    public double[] getOpenCenter(TableMetrics.Hole hole) {
        return switch (hole) {
            case TOP_LEFT -> topLeftHoleOpenCenter;
            case TOP_MID -> topMidHoleOpenCenter;
            case TOP_RIGHT -> topRightHoleOpenCenter;
            case BOT_RIGHT -> botRightHoleOpenCenter;
            case BOT_MID -> botMidHoleOpenCenter;
            case BOT_LEFT -> botLeftHoleOpenCenter;
        };
    }

    public TableMetrics.Hole getHoleByOpenCenter(double[] pos) {
        if (Arrays.equals(pos, topLeftHoleOpenCenter)) return TableMetrics.Hole.TOP_LEFT;
        else if (Arrays.equals(pos, topMidHoleOpenCenter)) return TableMetrics.Hole.TOP_MID;
        else if (Arrays.equals(pos, topRightHoleOpenCenter)) return TableMetrics.Hole.TOP_RIGHT;
        else if (Arrays.equals(pos, botLeftHoleOpenCenter)) return TableMetrics.Hole.BOT_LEFT;
        else if (Arrays.equals(pos, botMidHoleOpenCenter)) return TableMetrics.Hole.BOT_MID;
        else if (Arrays.equals(pos, botRightHoleOpenCenter)) return TableMetrics.Hole.BOT_RIGHT;
        else return null;
    }

    public TablePreset getTablePreset() {
        return tablePreset;
    }

    public BallsGroupPreset getBallsGroupPreset() {
        return ballsGroupPreset;
    }

    @Override
    public String toString() {
        return "GameValues{" +
                "rule=" + rule +
                ", table=" + table +
                ", ball=" + ball +
                '}';
    }
}
