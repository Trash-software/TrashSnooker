package trashsoftware.trashSnooker.core.metrics;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.phy.TableCloth;

import java.util.Arrays;

public class GameValues {

    public final GameRule rule;
    public final TableMetrics table;
    public final BallMetrics ball;
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

        JSONObject tableObj = jsonObject.getJSONObject("table");
        TableMetrics.TableBuilderFactory factory = 
                TableMetrics.fromOrdinal(tableObj.getInt("tableOrdinal"));
        TableMetrics.HoleSize holeSize = factory.supportedHoles[tableObj.getInt("holeSizeOrdinal")];
        TableMetrics tableMetrics = factory
                .create()
                .holeSize(holeSize)
                .pocketGravityMultiplier(cloth.goodness.holeExtraGravityWidthMul)
                .build();
        
        return new GameValues(rule, tableMetrics, ballMetrics);
    }
    
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        
        jsonObject.put("gameRule", rule.name());
        jsonObject.put("ball", ball.name());
        
        JSONObject tableObj = new JSONObject();
        tableObj.put("tableOrdinal", table.getOrdinal());
        tableObj.put("holeSizeOrdinal", table.getHoleSizeOrdinal());
        jsonObject.put("table", tableObj);
        
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
                botLeftHoleOpenCenter,
                topRightHoleOpenCenter,
                botRightHoleOpenCenter,
                topMidHoleOpenCenter,
                botMidHoleOpenCenter
        };
        // open center 和 hole顺序必须一致
    }

    public boolean isStandard() {
        return (rule == GameRule.SNOOKER && TableMetrics.SNOOKER.equals(table.tableName) && ball == BallMetrics.SNOOKER_BALL) ||
                (rule == GameRule.MINI_SNOOKER && table.tableName.equals(TableMetrics.CHINESE_EIGHT) && ball == BallMetrics.SNOOKER_BALL) ||
                (rule == GameRule.CHINESE_EIGHT && table.tableName.equals(TableMetrics.CHINESE_EIGHT) && ball == BallMetrics.POOL_BALL) ||
                (rule == GameRule.LIS_EIGHT && table.tableName.equals(TableMetrics.CHINESE_EIGHT) && ball == BallMetrics.POOL_BALL) ||
                (rule == GameRule.SIDE_POCKET && table.tableName.equals(TableMetrics.SIDE_POCKET) && ball == BallMetrics.POOL_BALL);
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

    @Override
    public String toString() {
        return "GameValues{" +
                "rule=" + rule +
                ", table=" + table +
                ", ball=" + ball +
                '}';
    }
}
