package trashsoftware.trashSnooker.core.metrics;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.Values;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.phy.TableCloth;
import trashsoftware.trashSnooker.core.training.Challenge;
import trashsoftware.trashSnooker.core.training.TrainType;
import trashsoftware.trashSnooker.util.DataLoader;

import java.util.Arrays;

public class GameValues {

    public final GameRule rule;
    public final TableMetrics table;
    public final BallMetrics ball;
    private TablePreset tablePreset;  // 不管太多，只管画
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

    public GameValues(GameRule rule,
                      TableMetrics tableMetrics,
                      BallMetrics ballMetrics) {
        this.rule = rule;
        this.table = tableMetrics;
        this.ball = ballMetrics;

        build();
    }
    
    public static GameValues fromJson(JSONObject jsonObject, TableCloth cloth) {
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
        
        GameValues gameValues = new GameValues(rule, tableMetrics, ballMetrics);
        gameValues.setTablePreset(tablePreset);
        
        return gameValues;
    }
    
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        
        // 似乎不需要考虑training/challenge的问题：谁存这个啊
        
        jsonObject.put("gameRule", rule.name());
        jsonObject.put("ball", ball.name());
        
        if (tablePreset == null) {
            JSONObject tableObj = new JSONObject();
            tableObj.put("tableOrdinal", table.getOrdinal());
            tableObj.put("holeSizeOrdinal", table.getHoleSizeOrdinal());
            tableObj.put("pocketDifficultyOrdinal", table.getPocketDifficultyOrdinal());
            jsonObject.put("table", tableObj);
        } else {
            jsonObject.put("tablePreset", tablePreset.id);
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

    public void setTrain(TrainType trainType, Challenge challenge) {
        this.trainType = trainType;
        this.trainChallenge = challenge;
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
                ((rule == GameRule.SNOOKER && TableMetrics.SNOOKER.equals(table.tableName) && ball == BallMetrics.SNOOKER_BALL) ||
                (rule == GameRule.MINI_SNOOKER && table.tableName.equals(TableMetrics.CHINESE_EIGHT) && ball == BallMetrics.SNOOKER_BALL) ||
                (rule == GameRule.CHINESE_EIGHT && table.tableName.equals(TableMetrics.CHINESE_EIGHT) && ball == BallMetrics.POOL_BALL) ||
                (rule == GameRule.LIS_EIGHT && table.tableName.equals(TableMetrics.CHINESE_EIGHT) && ball == BallMetrics.POOL_BALL) ||
                (rule == GameRule.AMERICAN_NINE && table.tableName.equals(TableMetrics.AMERICAN_NINE) && ball == BallMetrics.POOL_BALL));
    }
    
    public boolean isInTable(double x, double y, double r) {
        if (x >= table.leftX + r && 
                x < table.rightX - r && 
                y >= table.topY + r && 
                y < table.botY - r) {
            return true;
        }
        
        double[] pos = new double[]{x, y};

        if (y < r + table.topY) {
            if (x < table.midHoleAreaRightX && x >= table.midHoleAreaLeftX) {
                // 上方中袋在袋角范围内
                if (Algebra.distanceToPoint(pos, table.topMidHoleLeftArcXy) < table.midArcRadius + r) {
                    // 击中上方中袋左侧
                    return false;
                } else if (Algebra.distanceToPoint(pos, table.topMidHoleRightArcXy) < table.midArcRadius + r) {
                    // 击中上方中袋右侧
                    return false;
                } else if (x >= table.midHoleLineLeftX && x < table.midHoleLineRightX) {
                    // 疑似上方中袋直线
                    double[][] line = table.topMidHoleLeftLine;
                    if (Algebra.distanceToLine(pos, line) < r) {
                        return false;
                    }
                    line = table.topMidHoleRightLine;
                    if (Algebra.distanceToLine(pos, line) < r) {
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
                if (Algebra.distanceToPoint(pos, table.botMidHoleLeftArcXy) < table.midArcRadius + r) {
                    // 击中下方中袋左侧
                    return false;
                } else if (Algebra.distanceToPoint(pos, table.botMidHoleRightArcXy) < table.midArcRadius + r) {
                    // 击中下方中袋右侧
                    return false;
                } else if (x >= table.midHoleLineLeftX && x < table.midHoleLineRightX) {
                    // 疑似下方中袋直线
                    double[][] line = table.botMidHoleLeftLine;
                    if (Algebra.distanceToLine(pos, line) < r) {
                        return false;
                    }
                    line = table.botMidHoleRightLine;
                    if (Algebra.distanceToLine(pos, line) < r) {
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
            else if (x >= table.rightCornerHoleAreaLeftX) probHole = table.topRight.fallCenter;  // 右上底袋
        } else if (y >= table.botCornerHoleAreaUpY) {
            if (x < table.leftCornerHoleAreaRightX) probHole = table.botLeft.fallCenter;  // 左下底袋
            else if (x >= table.rightCornerHoleAreaLeftX) probHole = table.botRight.fallCenter;  // 右下底袋
        }

        if (probHole != null) {
            for (int i = 0; i < table.allCornerLines.length; ++i) {
                double[][] line = table.allCornerLines[i];

                if (Algebra.distanceToLine(pos, line) < r) {
                    return false;
                }
            }
//            if (!table.isStraightHole()) {
                for (double[] cornerArc : table.allCornerArcs) {
                    if (Algebra.distanceToPoint(pos, cornerArc) < table.cornerArcRadius + r) {
                        return false;
                    }
                }
//            }
            
            return true;
        }
        
//        // 检测袋角弧线
//        for (double[] cornerArc : table.allCornerArcs) {
//            if (Algebra.distanceToPoint(x, y, cornerArc[0], cornerArc[1]) < table.cornerArcRadius + r) {
//                return false;
//            }
//        }
//        for (double[] midArc : table.allMidArcs) {
//            if (Algebra.distanceToPoint(x, y, midArc[0], midArc[1]) < table.midArcRadius + r) {
//                return false;
//            }
//        }
//        
//        // 检测袋角直线
        
        return false;
    }

    public double speedReducerPerInterval(Phy phy) {
        return (phy.speedReducer * table.speedReduceMultiplier / ball.ballWeightRatio);  // 重的球减速慢
    }

    /**
     * @param speed 初始速度，mm/s
     * @return 预估的直线移动距离，mm。
     */
    public double estimatedMoveDistance(Phy phy, double speed) {
        double acceleration = speedReducerPerInterval(phy) * phy.calculationsPerSecSqr;
        double t = speed / acceleration;  // 加速时间，秒
        return acceleration / 2 * t * t;  // S = 1/2at^2
    }

    /**
     * @return 预估：以给定的初速度，跑一定距离需要多久
     */
    public double estimateMoveTime(Phy phy, double initSpeed, double distance) {
        double acceleration = speedReducerPerInterval(phy) * phy.calculationsPerSecSqr;
        double fullT = initSpeed / acceleration;
        fullT *= 0.85;  // 因为种种原因，这个算出来总是偏大，于是强行减小
        double fullDt = acceleration / 2 * fullT * fullT;
        double ratio = fullDt / distance;
        double timeRatio = ratio * ratio;
        double t0 = fullT / timeRatio + distance / initSpeed;
//        System.out.println("Full stop t: " + fullT + ", pocket t: " + t0 + ", full dt: " + fullDt + ", speed: " + initSpeed);
        return t0;  // 我不确定这对不对
    }
    
    public double estimateMaxPowerMoveDistance(Phy phy) {
        if (maxPowerMoveDistance == 0) {
            maxPowerMoveDistance = estimatedMoveDistance(phy, Values.MAX_POWER_SPEED);
        }
        return maxPowerMoveDistance;
    }

    public double estimateSpeedNeeded(Phy phy, double distance) {
        double acceleration = speedReducerPerInterval(phy) * phy.calculationsPerSecSqr;
        double t2 = distance * 2 / acceleration;
        return Math.sqrt(t2);
    }

    public TableMetrics.Hole getHoleOpenCenter(double[] pos) {
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

    @Override
    public String toString() {
        return "GameValues{" +
                "rule=" + rule +
                ", table=" + table +
                ", ball=" + ball +
                '}';
    }
}
